package jersey.bitcoin;

import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.TestNet3Params;
import org.consensusj.jsonrpc.JsonRPCStatusException;

import com.msgilligan.bitcoinj.rpc.BitcoinExtendedClient;
import com.msgilligan.bitcoinj.rpc.RPCURI;

@Path("bitcoin")
public class Bitcoin {
	// TODO
	// fixed keys/address for mocking
	// real keys/address' come from the DB in a later stage
	final static String clientPrivateKeys[] = { "92CqrxbHxU1nDQo2N9UkL6bGftcfbh929cYzPSubPshyERqhUK7" };
	final static String clientPublicKeys[] = { "03A48BED6D0C1FF608CFBC4F27D7831061A58C927055D0D74B3AD7351E3523D697" };
	final static String clientAddresses[] = { "mofhdVSgsUsVacWsf8QMNhDQqYnVXPtnZH" };

	final static String host = "localhost";
	String rpcuser = "test";
	String rpcpassword = "test";
	BitcoinExtendedClient client;
	NetworkParameters netParams = TestNet3Params.get();

	public Bitcoin() {
		URI server = RPCURI.getDefaultTestNetURI();
		client = new BitcoinExtendedClient(netParams, server, rpcuser, rpcpassword);
	}

	String[] getClientprivatekeys() {
		return clientPrivateKeys;
	}

	String[] getClientpublickeys() {
		return clientPublicKeys;
	}

	String[] getClientaddresses() {
		return clientAddresses;
	}

	// gets all user transactions
	@GET
	@Path("getTransactions")
	@Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
	public List<Transaction> getTransactions() throws JsonRPCStatusException, IOException, ParseException {
		String clientPrivateKeys[] = getClientaddresses();
		// TODO get all transactions for these addresses and put them in a List of
		// BitcoinJ transactions using ConsensusJ
		// go backwards, starting with the last mined block
		// FIXME return the actual transaction
		List<Transaction> txs = new LinkedList<Transaction>();
		// iterate backwards
		int blocks = client.getBlockChainInfo().getBlocks();
		int i = 10000; // max amount of blocks into the past to go through
		Date userRegistered = new SimpleDateFormat("dd.MM.yyyy").parse("01.01.2018");
		Block block = client.getBlock(blocks);
		while (i-- > 0 && userRegistered.compareTo(block.getTime()) <= 0) {
			txs.addAll(block.getTransactions());
			block = client.getBlock(blocks);
		}
		// txs.add(new Transaction(TestNet3Params.get()));
		return txs;
	}

	// gets the user balance
	@GET
	@Path("getBalance")
	@Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
	public Coin getBalance() throws JsonRPCStatusException, IOException {
		String clientPrivateKeys[] = getClientaddresses();
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
		return "34ca17378d8268631c94ca820ed9e88728dd505d72f2b51847ffdf1f3b1be668";
	}

}
