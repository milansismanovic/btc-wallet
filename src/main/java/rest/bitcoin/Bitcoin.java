package rest.bitcoin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.TestNet3Params;
import org.consensusj.jsonrpc.JsonRPCStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.msgilligan.bitcoinj.json.pojo.RawTransactionInfo;
import com.msgilligan.bitcoinj.json.pojo.RawTransactionInfo.Vout;
import com.msgilligan.bitcoinj.rpc.BitcoinExtendedClient;
import com.msgilligan.bitcoinj.rpc.RPCURI;

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
	BitcoinExtendedClient client;
	NetworkParameters netParams = TestNet3Params.get();

	public Bitcoin() {
		URI server = RPCURI.getDefaultTestNetURI();
		client = new BitcoinExtendedClient(netParams, server, rpcuser, rpcpassword);
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
	public List<RawTransactionInfo> getTransactions() throws JsonRPCStatusException, IOException, ParseException {
		String clientAddresses[] = getClientaddresses1();
		Set<String> addresses = new HashSet<String>();
		addresses.addAll(Arrays.asList(clientAddresses));
		// TODO get all transactions for these addresses and put them in a List of
		// BitcoinJ transactions using ConsensusJ
		// go backwards, starting with the last mined block
		// FIXME return the actual transaction
		List<RawTransactionInfo> txs = new LinkedList<RawTransactionInfo>();
		int lastBlockNumber = client.getBlockCount();
		// look half a year in the past. assume 1 block per 10 minutes.
		int deepestBlockNumber = Math.max(0, lastBlockNumber - 6 * 10 * 24 * 183);
		// TODO get actual user registration date
		Date userRegistered = new SimpleDateFormat("dd.MM.yyyy").parse("01.01.2018");
		for (int blockNumber = lastBlockNumber; blockNumber >= deepestBlockNumber; blockNumber--) {
			Block block = null;
			try {
				block = client.getBlock(blockNumber);
			} catch (Exception e) {
				log.error("error getting block: " + blockNumber);
				continue;
			}
			Date blockTime = block.getTime();
			if (userRegistered.compareTo(blockTime) <= 0) {
				List<Transaction> transactions = block.getTransactions();
				if (transactions != null) {
					for (Transaction transaction : transactions) {
						RawTransactionInfo transactionInfo = client.getRawTransactionInfo(transaction.getHash());
						List<Vout> vouts = transactionInfo.getVout();
						boolean found = false;
						for (Vout vout : vouts) {
							List<String> adds = vout.getScriptPubKey() == null ? null
									: vout.getScriptPubKey().getAddresses();
							if (adds != null) {
								for (String address : adds) {
									if (addresses.contains(address)) {
										if (!txs.contains(transactionInfo)) {
											txs.add(transactionInfo);
											found = true;
											break;
										}
									}
								}
							}
						}
						if (found) {
							break;
						}
					}
				}
			}
		}
		return txs;
	}

	// gets the user balance
	@GET
	@Path("getBalance")
	@Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
	public Coin getBalance() throws JsonRPCStatusException, IOException {
		String clientPrivateKeys[] = getClientaddresses1();
		// TODO get the balance for these addresses and put them in a List of
		// BitcoinJ transactions using ConsensusJ
		Coin balance = Coin.valueOf(0);
		for (String addressBase58 : clientPrivateKeys) {
			Address address = Address.fromBase58(netParams, addressBase58);
			Coin addressBalance = client.getBitcoinBalance(address);
			balance.add(addressBalance);
		}
		return balance;
	}

	// create a 2 out of 2 multi sign address
	@GET
	@Path("createAddress")
	@Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
	public String createAddress(@QueryParam("publicKey") String publicKey) {

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