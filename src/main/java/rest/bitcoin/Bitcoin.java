package rest.bitcoin;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;

import javax.servlet.Servlet;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._37coins.bcJsonRpc.BitcoindClientFactory;
import com._37coins.bcJsonRpc.BitcoindInterface;

import store.bitcoin.BlockStore;
import store.bitcoin.BlockStoreException;
import store.bitcoin.MemoryBlockStore;
import store.bitcoin.StoreLoader;
import store.bitcoin.pojo.StoredTransaction;
import store.bitcoin.pojo.StoredVout;

/**
 * This component that exposes all the services with regard to Bitcoin
 * transactions and transfers.
 * 
 * This class exposes the services through Jersey as a {@link Servlet} to the
 * client.
 * 
 * @author milan
 *
 */
@Path("bitcoin")
public class Bitcoin {
	private static Logger log = LoggerFactory.getLogger(Bitcoin.class);
	// TODO
	// fixed keys/address for mocking
	// real keys/address' come from the DB in a later stage
	final static String clientPrivateKeys1[] = { "92CqrxbHxU1nDQo2N9UkL6bGftcfbh929cYzPSubPshyERqhUK7" };
	final static String clientPublicKeys1[] = { "03A48BED6D0C1FF608CFBC4F27D7831061A58C927055D0D74B3AD7351E3523D697" };
	final static String clientAddresses1[] = { "mofhdVSgsUsVacWsf8QMNhDQqYnVXPtnZH",
			"miQxg3AVaLxZtGEgTbk4YgqU2hu5PoEFDj", "miSnWcTZteByQyJuz4XhVswhHuA5LcFD1i" };
	final static String clientPrivateKeys2[] = { "KySuyZ6jnGbGW2Qc9VpCk7F8z2rqfaS5EfTsqLxycczaZSuPxJwp" };
	final static String clientPublicKeys2[] = { "021AD207A99E408F840C0911BD8BCDDA9C6089B23AC0CFBB62D76961018E59C282" };
	final static String clientAddresses2[] = { "1FYU384h3Y7quk1bYy9BZzCFdFA7ojiCfc" };
	// FIXME add fresh 3rd key set
	final static String clientPrivateKeys3[] = { "KySuyZ6jnGbGW2Qc9VpCk7F8z2rqfaS5EfTsqLxycczaZSuPxJwp" };
	final static String clientPublicKeys3[] = { "02B7EC7437DC90F5F1BE5F963E97B81538AD87C81B4DC1C6E311140F22759D9C46" };
	final static String clientAddresses3[] = { "n1NBeDoig6XWgSYLs1mgDwh27YccgiDxv9" };

	static BitcoindInterface client;
	static BlockStore store;
	static Configuration config;
	static StoreLoader storeLoader;
	static NetworkParameters params;

	public Bitcoin() throws MalformedURLException, IOException, BlockStoreException, ConfigurationException {
		if (config == null)
			config = new Configurations().properties(new File("bitcoin.properties"));
		BitcoindClientFactory clientFactory = new BitcoindClientFactory(new URL(config.getString("bitcoin.rpc.URL")),
				config.getString("bitcoin.rpc.rpcuser"), config.getString("bitcoin.rpc.rpcpassword"));
		if (client == null)
			client = clientFactory.getClient();
		// store = new DBBlockStore();
		if (store == null)
			store = new MemoryBlockStore();
		if (storeLoader == null)
			storeLoader = new StoreLoader(store, client);
		storeLoader.loadStore(6 * 24 * 20); // 28 days in the past

		if (params == null) {
			// params = config.getBoolean("bitcoin.testnet") ? TestNet3Params.get() :
			// MainNetParams.get();
			if (config.getBoolean("bitcoin.testnet"))
				params = TestNet3Params.get();
			else
				params = MainNetParams.get();
		}
	}

	Collection<String> getClientprivatekeys1() {
		Collection<String> privateKeys = new LinkedList<>();
		privateKeys.addAll(Arrays.asList(clientPrivateKeys1));
		return privateKeys;
	}

	Collection<String> getClientpublickeys1() {
		Collection<String> publicKeys = new LinkedList<>();
		publicKeys.addAll(Arrays.asList(clientPublicKeys1));
		return publicKeys;
	}

	Collection<String> getClientaddresses1() {
		Collection<String> addresses = new LinkedList<>();
		addresses.addAll(Arrays.asList(clientAddresses1));
		return addresses;
	}

	static long lastRefresh = -1;

	// // TODO implement a clever sync from store to the bitcoin network
	// // suggestion: create anonymous inner class to run the sync to update the db
	// // as a singleton to ensure the thread is only spanned once.
	// // i.e. use BlockListener from the BitcoindClient4J
	// class DBRefresher {
	//
	// }

	void refreshDB() {
		if (System.currentTimeMillis() - lastRefresh < 1000 * 30) {
			return;
		}
		Thread refreshDBRunnable = new Thread() {
			public void run() {
				try {
					storeLoader.loadStore(1000);
					lastRefresh = System.currentTimeMillis();
				} catch (BlockStoreException e) {
					log.error("error refreshing block store", e);
				}
			}
		};
		refreshDBRunnable.start();
	}

	/**
	 * Gets all user transactions. FIXME: use BitcoinJ Transaction instead of
	 * BitcoinClient4J RawTransaction.
	 * 
	 * @return
	 * @throws ParseException
	 *             Thrown if the JSON cannot be read.
	 * @throws BlockStoreException
	 */
	@GET
	@Path("getTransactions")
	@Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
	public SortedSet<StoredTransaction> getTransactions() throws BlockStoreException {
		refreshDB();
		log.info("getting stored txs");
		SortedSet<StoredTransaction> stxs = store.getTx(getClientaddresses1());
		log.info("user's stored tx({}): {}", stxs.size(), stxs.toString());
		return stxs;
	}

	/**
	 * Gets the user balance.
	 * 
	 * @return
	 * @throws BlockStoreException
	 */
	@GET
	@Path("getBalance")
	@Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
	public BigDecimal getBalance() throws BlockStoreException {
		BigDecimal balance = store.getBalance(getClientaddresses1());
		return balance;
	}

	/**
	 * Create a 2 out of 3 multi sign address using {@code userPublicKey} as the
	 * first public key. Take 2nd and 3rd from the server.
	 * 
	 * @param userPublicKey
	 * @return
	 */
	@GET
	@Path("createAddress")
	@Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
	public String createAddress(@QueryParam("publicKey") String userPublicKey) {
		// TODO requires the usage of the 3 public keys and creates a 2 out of 3
		// multisig address
		// one public key is from the client. The rest from the backend.
		return "32gaYRAvxFgsBZB3LuegK4W4wbx8rNdNX9";
	}

	/**
	 * Creates a list of user's UTXOs for the client to sign.
	 * 
	 * @param toAddress
	 * @param satoshiAmount
	 * @param changeAddress
	 * @return array of UTXOs to sign
	 * @throws BlockStoreException
	 */
	@GET
	@Path("startTransaction")
	@Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
	public List<Transaction> startTransaction(@QueryParam("toAddress") String toAddress,
			@QueryParam("satoshiAmount") int satoshiAmount, @QueryParam("changeAddress") String changeAddress)
			throws BlockStoreException {
		// TODO create new list of transactions to be signed with key 1
		SortedSet<StoredVout> utxos = store.getUnspentVouts(getClientaddresses1());
		// create the tx
		// create the inputs
		// create the outputs

		// String to a private key
		String privKey = getClientprivatekeys1().iterator().next(); // get the first key as all money is with it
		String address = getClientaddresses1().iterator().next(); // get the first address

		DumpedPrivateKey dumpedPrivateKey = DumpedPrivateKey.fromBase58(params, privKey);
		ECKey key = dumpedPrivateKey.getKey();

		// String to an address
		Address address2 = Address.fromBase58(params, address);

		Transaction tx = new Transaction(params);
		int amount = 14013;
		// value is a sum of all inputs, fee is 4013
		tx.addOutput(Coin.valueOf(amount - 4013), address2);

//		UTXO utxo;
//		for(vout:store.get)
		
		// //utxos is an array of inputs from my wallet
		// for(UTXO utxo : utxos)
		// {
		// TransactionOutPoint outPoint = new TransactionOutPoint(params,
		// utxo.getIndex(), utxo.getHash());
		// //YOU HAVE TO CHANGE THIS
		// tx.addSignedInput(outPoint, utxo.getScript(), key, Transaction.SigHash.ALL,
		// true);
		// }
		//
		// tx.getConfidence().setSource(TransactionConfidence.Source.SELF);
		// tx.setPurpose(Transaction.Purpose.USER_PAYMENT);
		//
		// System.out.println(tx.getHashAsString());
		// b_peerGroup.GetPeerGroup().broadcastTransaction(tx);

		List<Transaction> txs = new LinkedList<>();
		txs.add(new Transaction(TestNet3Params.get()));
		txs.add(new Transaction(TestNet3Params.get()));
		txs.add(new Transaction(TestNet3Params.get()));
		return txs;
	}

	/**
	 * Sign the transaction from the server with the user's server key and broadcast
	 * it.
	 * 
	 * @param signedTX
	 * @return transaction id
	 */
	@GET
	@Path("executeTransaction")
	@Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
	public String executeTransaction(List<String> signedTX) {
		// TODO sign transactions with key 2
		// TODO broadcast tx
		// TODO return transaction id
		return "34ca17378d8268631c94ca820ed9e88728dd505d72f2b51847ffdf1f3b1be668";
	}
}