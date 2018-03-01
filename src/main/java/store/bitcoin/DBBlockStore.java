package store.bitcoin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.bitcoinj.core.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import store.bitcoin.pojo.StoredBlock;
import store.bitcoin.pojo.StoredTransaction;
import store.bitcoin.pojo.StoredVout;

/**
 * Implements the BlockStore to persist the data in the db.
 * 
 * TODO very unfinished: needs proper schema for the db to efficiently implement
 * getTx and getBalance for a user.
 * 
 * @author milan
 *
 */
public class DBBlockStore extends BlockStore {
	private static final Logger log = LoggerFactory.getLogger(DBBlockStore.class);
	// private static final String DB_HOSTNAME = "localhost";
	// private static final String DB_NAME = "bitcoin_test";
	// private static final String DB_USERNAME = "bitcoin";
	// private static final String DB_PASSWORD = "password";

	// private static final String DATABASE_DRIVER_CLASS = "com.mysql.jdbc.Driver";
	private static final String DATABASE_DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
	private static final String DATABASE_CONNECTION_URL_PREFIX = "jdbc:mysql://";
	// private static final int blockCount = 100000;

	// table name list
	private static final String[] TABLE_NAMES = { "addresstransactions", "blockheaders", "transactions" };

	// CREATE TABLE SQL scripts
	private static final String CREATE_ADDRESSTRANSACTIONS_TABLE = "CREATE TABLE addresstransactions (\r\n"
			+ "  id int(11) NOT NULL AUTO_INCREMENT,\r\n" + "  txid varbinary(32) DEFAULT NULL,\r\n"
			+ "  address varchar(35) DEFAULT NULL,\r\n" + "  PRIMARY KEY (id),\r\n"
			+ "  KEY `index` (id,txid,address)\r\n" + ");";
	private static final String CREATE_blockheaders_TABLES = "CREATE TABLE `blockheaders` (\r\n"
			+ "  `hash` varbinary(32) NOT NULL,\r\n" + "  `height` int(11) NOT NULL,\r\n"
			+ "  `time` datetime NOT NULL,\r\n" + "  `previousblockhash` varbinary(32) NOT NULL,\r\n"
			+ "  PRIMARY KEY (`hash`),\r\n" + "  KEY `hash` (`hash`,`height`,`time`)\r\n" + ");";
	private static final String CREATE_transactions_TABLES = "CREATE TABLE `transactions` (\r\n"
			+ "  `txid` varbinary(32) NOT NULL,\r\n" + "  `blockhash` varbinary(32) NOT NULL,\r\n"
			+ "  `txblob` blob,\r\n" + "  PRIMARY KEY (`txid`),\r\n" + "  KEY `hash` (`txid`,`blockhash`)\r\n" + ");";

	private static final String[] CREATE_TABLE_SCRIPTS = { CREATE_ADDRESSTRANSACTIONS_TABLE, CREATE_blockheaders_TABLES,
			CREATE_transactions_TABLES };

	// DROP statements
	private static final String DROP_TABLES = "DROP TABLE `addresstransactions`, `blockheaders`, `transactions`;";

	// INSERT statements
	private static final String INSERT_BLOCKHEADER = "INSERT INTO `blockheaders`\r\n" + "(`hash`,\r\n" + "`height`,\r\n"
			+ "`time`,\r\n" + "`previousblockhash`)\r\n" + "VALUES\r\n" + "(?,\r\n" + "?,\r\n" + "?,\r\n" + "?);\r\n"
			+ "";

	private static final String INSERT_TX = "INSERT INTO `transactions`"
			+ "(`txid`,`blockhash`,`txblob`) VALUES (?,?,?);";

	private static final String INSERT_ADDRESSTRANSACTIONS = "INSERT INTO addresstransactions "
			+ "(txid,address) VALUES(?,?);";

	// SELECT statements
	private static final String SELECT_BLOCKHEADER = "SELECT \r\n" + "    hash,\r\n" + "    height,\r\n"
			+ "    time,\r\n" + "    previousblockhash\r\n" + "FROM blockheaders\r\n" + "WHERE hash = ?;";
	private static final String SELECT_ADDRESSTRANSACTIONS = "SELECT `id`,`txid`,`address` FROM `addresstransactions` ";

	protected String hostname;
	protected String username;
	protected String password;
	protected String schemaName;
	protected String connectionURL;

	Connection connection;

	public DBBlockStore() throws BlockStoreException, ConfigurationException {
		Configuration config = new Configurations().properties(new File("bitcoin.properties"));
		this.hostname = config.getString("db.host");
		this.schemaName = config.getString("db.name");
		this.username = config.getString("db.user");
		this.password = config.getString("db.password");
		if (hostname == null || schemaName == null || username == null || password == null) {
			throw new ConfigurationException(
					"one of the configuration items: 'db.host', 'db.name', 'db.user', 'db.password' is missing");
		}
		this.connectionURL = DATABASE_CONNECTION_URL_PREFIX + hostname + "/" + schemaName;
		try {
			Class.forName(DATABASE_DRIVER_CLASS);
			log.info(DATABASE_DRIVER_CLASS + " loaded. ");
		} catch (ClassNotFoundException e) {
			log.error("check CLASSPATH for database driver jar ", e);
		}
		try {
			this.connect();
		} catch (Exception e) {
			throw new BlockStoreException("error connecting to the db : " + connectionURL, e);
		}
		// if schema not available create tables
		boolean tableExists = false;
		try {
			tableExists = tablesExist();
		} catch (SQLException e) {
			throw new BlockStoreException("error checking db tables exist", e);
		}
		if (!tableExists) {
			try {
				createTables();
			} catch (SQLException e) {
				throw new BlockStoreException("error creating db tables", e);
			}
		} else {
			// load store chainhead
			String sql = "SELECT `hash` FROM blockheaders ORDER BY height DESC LIMIT 1";
			PreparedStatement s;
			try {
				s = connection.prepareStatement(sql);
				ResultSet rs = s.executeQuery();
				if (rs.next()) {
					byte[] blockChainHeadHashBytes = rs.getBytes(1);
					String blockChainHeadHash = Utils.HEX.encode(blockChainHeadHashBytes);
					StoredBlock head = this.get(blockChainHeadHash);
					updateChainHead(head);
				}
			} catch (SQLException e) {
				throw new BlockStoreException("error getting newest block", e);
			}

		}
	}

	private void connect() throws SQLException {
		Properties props = new Properties();
		props.setProperty("user", this.username);
		props.setProperty("password", this.password);
		connection = DriverManager.getConnection(connectionURL, props);
	}

	boolean tablesExist() throws SQLException {
		DatabaseMetaData meta = connection.getMetaData();
		ResultSet res = meta.getTables(null, null, TABLE_NAMES[0], new String[] { "TABLE" });
		return res.next();
	}

	void createTables() throws SQLException {
		Statement s = connection.createStatement();
		for (String sql : CREATE_TABLE_SCRIPTS) {
			if (log.isDebugEnabled()) {
				log.debug("DatabaseFullPrunedBlockStore : CREATE table [SQL= {}]", sql);
			}
			s.executeUpdate(sql);
		}
	}

	/**
	 * Resets the store by swiping all data.
	 * 
	 * @throws BlockStoreException
	 */
	public void resetStore() throws BlockStoreException {
		try {
			Statement s = connection.createStatement();
			String sql = DROP_TABLES;
			if (log.isDebugEnabled()) {
				log.debug("DatabaseFullPrunedBlockStore : DROP table [SQL= {0}]", sql);
			}
			s.executeUpdate(sql);
		} catch (SQLException e) {
			// no need to stop here
			// throw new BlockStoreException("error dropping tables.", e);
		}
		try {
			createTables();
		} catch (Exception e) {
			throw new BlockStoreException("error creating tables.", e);
		}
	}

	/**
	 * Gets a block from the store with the given hash. Returns null if no such
	 * block is stored.
	 */
	public StoredBlock get(String hash) throws BlockStoreException {
		try {
			PreparedStatement s = connection.prepareStatement(SELECT_BLOCKHEADER);
			byte[] hashBytes = Utils.HEX.decode(hash);
			s.setBytes(1, hashBytes);
			ResultSet rs = s.executeQuery();
			if (rs.next()) {
				byte[] hashbytes = rs.getBytes(1);
				String hash2 = Utils.HEX.encode(hashbytes);
				int height = rs.getInt(2);
				Timestamp timeStamp = rs.getTimestamp(3);
				long time = timeStamp.getTime() / 1000;
				byte[] previousHash = rs.getBytes(4);
				String previousblockhash = Utils.HEX.encode(previousHash);
				StoredBlock block = new StoredBlock(hash2, height, time, previousblockhash, null);
				// TODO get txs here
				return block;
			} else {
				return null;
			}
		} catch (SQLException e) {
			log.debug("couldn't find block with this hash: {}", hash);
		}
		return null;
	}

	public StoredBlock get(int height) {
		try {
			String sql = "SELECT `blockheaders`.`hash`,\r\n" + "    `blockheaders`.`height`,\r\n"
					+ "    `blockheaders`.`time`,\r\n" + "    `blockheaders`.`previousblockhash`\r\n"
					+ "FROM `bitcoin_test`.`blockheaders`;\r\n" + "WHERE height=?";
			PreparedStatement s = connection.prepareStatement(sql);
			s.setInt(1, height);
			ResultSet rs = s.executeQuery();
			if (rs.next()) {
				byte[] hashbytes = rs.getBytes(1);
				String hash2 = Utils.HEX.encode(hashbytes);
				Timestamp timeStamp = rs.getTimestamp(3);
				long time = timeStamp.getTime() / 1000;
				byte[] previousHash = rs.getBytes(4);
				String previousblockhash = Utils.HEX.encode(previousHash);
				StoredBlock block = new StoredBlock(hash2, height, time, previousblockhash, null);
				// TODO get txs here
				return block;
			} else {
				return null;
			}
		} catch (SQLException e) {
			log.debug("couldn't find block with this hash: {}", height);
		}
		return null;

	}

	/**
	 * Puts a block into the store.
	 */
	public void put(StoredBlock block) throws BlockStoreException {
		try {
			// put block data into the db
			PreparedStatement s = connection.prepareStatement(INSERT_BLOCKHEADER);
			byte[] hash;
			hash = Utils.HEX.decode(block.getHash());
			s.setBytes(1, hash);
			s.setInt(2, (int) block.getHeight());
			Timestamp blockDate = new Timestamp(block.getTime() * 1000);
			s.setTimestamp(3, blockDate);
			hash = Utils.HEX.decode(block.getPreviousblockhash());
			s.setBytes(4, hash);
			if (log.isDebugEnabled())
				log.debug(s.toString());
			s.executeUpdate();
			s.close();
			// put the tx into transaction table and the address to transaction into the
			// addresstransaction table of the db
			for (StoredTransaction tx : block.getTxs()) {
				// add tx to db (`txid`,`blockhash`,`txblob`) VALUES (?,?,?)
				PreparedStatement stx = connection.prepareStatement(INSERT_TX);
				byte[] txid;
				txid = Utils.HEX.decode(tx.getTxid());
				stx.setBytes(1, txid);
				stx.setBytes(2, hash);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(baos);
				oos.writeObject(tx);
				byte[] txBytes = baos.toByteArray();
				stx.setBytes(3, txBytes);
				if (log.isDebugEnabled())
					log.debug(stx.toString());
				stx.executeUpdate();
				stx.close();
				// put for each address in vout an entry in the addresstransaction table
				// consider putting only our clients' address-transaction combinations in
				// the future
				PreparedStatement satx = connection.prepareStatement(INSERT_ADDRESSTRANSACTIONS);
				for (StoredVout vout : tx.getVouts()) {
					List<String> addresses = vout.getAddresses();
					if (addresses == null)
						continue;
					for (String address : vout.getAddresses()) {
						// TODO add address to transaction records here
						// (txid,address) VALUES(?,?);
						satx.setBytes(1, txid);
						satx.setString(2, address);
						if (log.isDebugEnabled())
							log.debug(satx.toString());
						satx.addBatch();
					}
				}
				satx.executeBatch();
				satx.close();
			}
			// put txs vins to db
			// TODO implement - ACTUALLY CAN'T AS WE PROBABLY DON'T HAVE THE PREVIOUS
			// TRANSACTION YET LOADED/PUT INTO THE DB
		} catch (SQLException e) {
			throw new BlockStoreException("error inserting block", e);
		} catch (IOException e) {
			throw new BlockStoreException("error serializing tx", e);
		}
		updateChainHead(block);
	}

	void updateVins() {
		// go through the blockchain and update the vins for all tx

	}

	public StoredTransaction getTx(String txid) {
		// get all tx from the transactions table
		String txsql = "SELECT `txblob` FROM `transactions` WHERE txid=?;";
		PreparedStatement txstmt;
		byte[] txidBytes = Utils.HEX.decode(txid);
		StoredTransaction tx = null;
		try {
			txstmt = connection.prepareStatement(txsql);
			txstmt.setBytes(1, txidBytes);
			ResultSet txrs = txstmt.executeQuery();
			if (txrs.next()) {
				byte[] txBytes = txrs.getBytes(1);
				ByteArrayInputStream baip = new ByteArrayInputStream(txBytes);
				ObjectInputStream ois = new ObjectInputStream(baip);
				Object object = ois.readObject();
				tx = (StoredTransaction) object;
			}
		} catch (Exception e) {
			// don't care if it fails as we return null then
			// most likely because tx is not stored
		}
		return tx;
	}

	public SortedSet<StoredTransaction> getTx(List<String> addresses) throws BlockStoreException {
		SortedSet<StoredTransaction> txs = new TreeSet<>();
		// FIXME there is probably a more elegant way to have a multi value where
		// clause than to manually construct the sql select statement...
		StringBuffer sql = new StringBuffer();
		sql.append(SELECT_ADDRESSTRANSACTIONS);
		sql.append("WHERE address in (");

		for (int i = 0; i < addresses.size(); i++) {
			sql.append("\"");
			sql.append(addresses.get(i));
			// if last omit the comma
			if (i < addresses.size() - 1) {
				sql.append("\",");
			} else {
				sql.append("\"");
			}
		}
		sql.append(")");
		try {
			// get the txid list to rs
			PreparedStatement s = connection.prepareStatement(sql.toString());
			ResultSet rs = s.executeQuery();
			while (rs.next()) {
				byte[] txidByte = rs.getBytes(2);
				// get all tx from the transactions table
				String txsql = "SELECT `txblob` FROM `transactions` WHERE txid=?;";
				PreparedStatement txstmt = connection.prepareStatement(txsql);
				txstmt.setBytes(1, txidByte);
				ResultSet txrs = txstmt.executeQuery();
				while (txrs.next()) {
					byte[] txBytes = txrs.getBytes(1);
					ByteArrayInputStream baip = new ByteArrayInputStream(txBytes);
					ObjectInputStream ois = new ObjectInputStream(baip);
					Object object = ois.readObject();
					StoredTransaction tx = (StoredTransaction) object;
					// add tx to the returning txs
					txs.add(tx);
				}
			}
		} catch (SQLException e) {
			throw new BlockStoreException("db error fetching transactions for addresses " + addresses.toString(), e);
		} catch (IOException e) {
			throw new BlockStoreException(
					"serialization error converting transactions for addresses " + addresses.toString(), e);
		} catch (ClassNotFoundException e) {
			throw new BlockStoreException(
					"serialization error converting StoredTransaction for addresses " + addresses.toString(), e);
		}
		return txs;
	}

	public SortedSet<StoredTransaction> getUnspentTx(List<String> addresses) throws BlockStoreException {
		// TODO Auto-generated method stub
		return null;
	}

	public BigInteger getBalance(List<String> addresses) throws BlockStoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateUTXO(StoredTransaction tx, StoredTransaction parentTx, int parentVoutIndex)
			throws BlockStoreException {
		// TODO Auto-generated method stub
	}

	public void test(List<String> addresses) throws BlockStoreException {
		try {
			// get all tx for this address
			SortedSet<StoredTransaction> txs = getTx(addresses);
			for (StoredTransaction tx : txs) {
				log.info(tx.toString());
				// get
				String txsql = "SELECT txblob FROM `transactions`;";
				PreparedStatement txstmt = connection.prepareStatement(txsql);
				ResultSet txrs = txstmt.executeQuery();
				while (txrs.next()) {
					byte[] txBytes = txrs.getBytes(1);
					ByteArrayInputStream baip = new ByteArrayInputStream(txBytes);
					ObjectInputStream ois = new ObjectInputStream(baip);
					StoredTransaction txR = (StoredTransaction) ois.readObject();
					log.info(txR.toString());
				}
			}
		} catch (SQLException e) {
			throw new BlockStoreException("db error fetching transactions", e);
		} catch (IOException e) {
			throw new BlockStoreException("serialization error converting transactions", e);
		} catch (ClassNotFoundException e) {
			throw new BlockStoreException("serialization error converting StoredTransaction", e);
		}
	}


}
