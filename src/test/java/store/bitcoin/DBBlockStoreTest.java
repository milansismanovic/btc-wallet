package store.bitcoin;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.dbutils.ResultSetHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._37coins.bcJsonRpc.BitcoindClientFactory;
import com._37coins.bcJsonRpc.BitcoindInterface;
import com.mysql.cj.jdbc.result.ResultSetMetaData;

import store.bitcoin.pojo.StoredBlock;
import store.bitcoin.pojo.StoredTransaction;

public class DBBlockStoreTest {
	private static final Logger log = LoggerFactory.getLogger(DBBlockStoreTest.class);
	private static DBBlockStore store;
	private static BitcoindInterface client;
	private static StoreLoader storeLoader;

	@BeforeClass
	public static void setUpBeforeClass()
			throws BlockStoreException, MalformedURLException, IOException, ConfigurationException {
		store = new DBBlockStore();
		Configuration config = new Configurations().properties(new File("bitcoin.properties"));
		BitcoindClientFactory clientFactory = new BitcoindClientFactory(new URL(config.getString("bitcoin.rpc.URL")),
				config.getString("bitcoin.rpc.rpcuser"), config.getString("bitcoin.rpc.rpcpassword"));
		client = clientFactory.getClient();
		storeLoader = new StoreLoader(store, client);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() throws Exception {
		// store.resetStore();
		int n = 6 * 24; // load data from the last 24h
		log.info("blockCount: {}. Loading {} blocks.", client.getblockcount(), n);
		storeLoader.loadStore(n);
		// iterate through the blocks in the store
		StoredBlock block = store.getChainHead();
		while (block != null) {
			log.info("block: {}", block);
			block = store.get(block.getPreviousblockhash());
		}
		// get tx from known address of a user
		List<String> addresses = new LinkedList<String>();
		addresses.add("mofhdVSgsUsVacWsf8QMNhDQqYnVXPtnZH");
		List<StoredTransaction> txs = store.getTx(addresses);
		for (StoredTransaction tx : txs) {
			log.info(tx.toString());
		}
	}

	@Test
	public void testTransactions() throws BlockStoreException {
		List<String> addresses = new LinkedList<String>();
		addresses.add("mofhdVSgsUsVacWsf8QMNhDQqYnVXPtnZH");
		List<StoredTransaction> txs = store.getTx(addresses);
		log.info("tx for address: {}, count={}, tx={}", "mofhdVSgsUsVacWsf8QMNhDQqYnVXPtnZH", txs.size(),
				txs.toString());

		// String sql = "SELECT `txblob` FROM `transactions` WHERE txid=?;";
	}
}
