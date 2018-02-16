package rest.bitcoin;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.TestNet3Params;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._37coins.bcJsonRpc.BitcoindClientFactory;
import com._37coins.bcJsonRpc.BitcoindInterface;
import com._37coins.bcJsonRpc.pojo.RawTransaction;
import com._37coins.bcJsonRpc.pojo.Vin;
import com._37coins.bcJsonRpc.pojo.Vout;

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
	final static String clientPrivateKeys3[] = { "KySuyZ6jnGbGW2Qc9VpCk7F8z2rqfaS5EfTsqLxycczaZSuPxJwp" };
	final static String clientPublicKeys3[] = { "021AD207A99E408F840C0911BD8BCDDA9C6089B23AC0CFBB62D76961018E59C282" };
	final static String clientAddresses3[] = { "1FYU384h3Y7quk1bYy9BZzCFdFA7ojiCfc" };

	final static String host = "localhost";
	String rpcuser = "test";
	String rpcpassword = "test";
	BitcoindInterface client;

	public Bitcoin() throws MalformedURLException, IOException {
		BitcoindClientFactory clientFactory = new BitcoindClientFactory(new URL("http://" + host + ":18332/"), rpcuser,
				rpcpassword);
		client = clientFactory.getClient();
	}

	String[] getClientprivatekeys1() {
		return clientPrivateKeys1;
	}

	String[] getClientpublickeys1() {
		return clientPublicKeys1;
	}

	String[] getClientaddresses1() {
		return clientAddresses1;
	}

	// gets all user transactions
	@GET
	@Path("getTransactions")
	@Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
	public List<RawTransaction> getTransactions() throws ParseException {
		String clientAddresses[] = getClientaddresses1();
		Set<String> addresses = new HashSet<String>();
		addresses.addAll(Arrays.asList(clientAddresses));
		// TODO get all transactions for these addresses and put them in a List of
		// BitcoinJ transactions using ConsensusJ
		// go backwards, starting with the last mined block
		// FIXME return the actual transaction
		Set<RawTransaction> txs = new HashSet<RawTransaction>();
		int lastBlockNumber = client.getblockcount();
		// look one week in the past. assume 1 block per 10 minutes.
		int deepestBlockNumber = Math.max(0, lastBlockNumber - 6 * 24 * 2);
		// FIXME get actual user registration date
		Date userRegistered = new SimpleDateFormat("dd.MM.yyyy").parse("01.01.2018");
		for (int blockNumber = lastBlockNumber; blockNumber >= deepestBlockNumber; blockNumber--) {
			com._37coins.bcJsonRpc.pojo.Block block = null;
			try {
				String blockHash = client.getblockhash(blockNumber);
				block = client.getblock(blockHash);
			} catch (Exception e) {
				log.error("error getting block: " + blockNumber);
				throw e;
			}
			long timeSecs = block.getTime();
			Date blockTime = new Date(timeSecs * 1000);
			log.info(blockTime.toString());
			if (userRegistered.compareTo(blockTime) <= 0) {
				List<String> transactions = block.getTx();
				if (transactions != null) {
					for (String transactionHash : transactions) {
						String rawTransaction = client.getrawtransaction(transactionHash);
						// TODO decode the rawtransaction with BitcoinJ
						// code below is failing at reading. works in a JUnit at 
						// the freshly checked out BitcoinJ Code
//						byte[] rawTransactionBytes = Utils.HEX.decode(rawTransaction);
//						try {
//							Transaction bitcoinjTx = new Transaction(TestNet3Params.get(), rawTransactionBytes);
//							log.info(bitcoinjTx.toString());
//						} catch (Exception e) {
//							log.info("failed tx deser: {}, bytes: {}", e.getMessage(), rawTransaction);
//						}
						RawTransaction tx = client.decoderawtransaction(rawTransaction);
						log.debug("transaction: {}", tx);
						// get the tx matching the inputs from the input txs in vin
						List<Vin> vins = tx.getVin();
						for (Vin vin : vins) {
							String vinTxHash = vin.getTxid();
							if (vinTxHash == null)
								continue;
							RawTransaction vinTx = client.decoderawtransaction(client.getrawtransaction(vinTxHash));
							List<Vout> vouts = vinTx.getVout();
							for (Vout vout : vouts) {
								List<String> txAddresses = vout.getScriptPubKey().getAddresses();
								if (txAddresses == null)
									continue;
								for (String txAddress : txAddresses) {
									if (addresses.contains(txAddress)) {
										txs.add(tx);
									}
								}
							}
						}
						// get the tx matching the outputs
						List<Vout> vouts = tx.getVout();
						for (Vout vout : vouts) {
							List<String> txAddresses = vout.getScriptPubKey().getAddresses();
							if (txAddresses == null)
								continue;
							for (String txAddress : txAddresses) {
								if (addresses.contains(txAddress)) {
									txs.add(tx);
								}
							}
						}
					}
				}
			}
		}
		return new LinkedList<RawTransaction>(txs);
	}

	// gets the user balance
	@GET
	@Path("getBalance")
	@Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
	public BigDecimal getBalance() throws IOException {
		// TODO get the balance for these addresses and put them in a List of
		// BitcoinJ transactions using ConsensusJ
		BigDecimal balance = client.getbalance();
		return balance;
	}

	// create a 2 out of 2 multi sign address
	@GET
	@Path("createAddress")
	@Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
	public String createAddress(@QueryParam("publicKey") String publicKey) {
		// TODO requires the usage of the 3 public keys and creates a 2 out of 3 multisig address
		// one public key is from the client. The rest from the backend.
		return "32gaYRAvxFgsBZB3LuegK4W4wbx8rNdNX9";
	}

	// returns array of UTXOs to sign
	@GET
	@Path("startTransaction")
	@Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
	public List<Transaction> startTransaction(@QueryParam("toAddress") String toAddress,
			@QueryParam("satoshiAmount") int satoshiAmount, @QueryParam("changeAddress") String changeAddress) {
		// TODO create new list of transactions to be signed with key 1
		List<Transaction> txs = new LinkedList<>();
		txs.add(new Transaction(TestNet3Params.get()));
		txs.add(new Transaction(TestNet3Params.get()));
		txs.add(new Transaction(TestNet3Params.get()));
		return txs;
	}

	// returns transaction id
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