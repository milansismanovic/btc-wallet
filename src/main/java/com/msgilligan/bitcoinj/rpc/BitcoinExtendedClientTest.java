package com.msgilligan.bitcoinj.rpc;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.params.TestNet3Params;
import org.consensusj.jsonrpc.JsonRPCStatusException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class BitcoinExtendedClientTest {
	private static final Logger log = Logger.getLogger(BitcoinExtendedClientTest.class.getName());

	static BitcoinExtendedClient client;

	final static String host = "localhost";
	static String rpcuser = "test";
	static String rpcpassword = "test";
	String faucetAddress = "mofhdVSgsUsVacWsf8QMNhDQqYnVXPtnZH";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		if (client == null) {
			NetworkParameters netParams = TestNet3Params.get();

			URI server = RPCURI.getDefaultTestNetURI();
			// URI server = new URI("http://" + host + ":18332/");

			client = new BitcoinExtendedClient(netParams, server, rpcuser, rpcpassword);

			NetworkParameters params = client.getNetParams();
			log.info(params.toString());

			// NetworkParameters netIdString= TestNet3Params.get();;
			RPCConfig config = new RPCConfig(TestNet3Params.get(), server, rpcuser, rpcpassword);
			client = new BitcoinExtendedClient(config);

			log.info(client.getCommands().toString());
			log.info(client.getBlockChainInfo().toString());
		}
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
	}

	@Test
	public void testGetBitcoinBalanceAddress() throws Exception {
		if (client == null) {
			setUp();
		}
		Address address = Address.fromBase58(TestNet3Params.get(), faucetAddress);
		Coin coin = client.getBitcoinBalance(address);

		log.info("***********coin: " + coin.toFriendlyString());
	}

	@Test
	public void testGetBlockCount() throws Exception {
		if (client == null) {
			setUp();
		}
		log.info("getBlockCount(): " + client.getBlockCount());
		int blockCount = client.getBlockCount();
		log.info("block count: " + blockCount);
		log.info(client.getBlockChainInfo().toString());
		log.info(client.listUnspent().toString());

		Block block = client.getBlock(657810);
		log.info(block.toString());

		// Sha256Hash hash =
		// Sha256Hash.wrap("000000000933ea01ad0ee984209779baaec3ced90fa3f408719526f8d77f4943");
		// log.info(client.getTransaction(hash).toString());
		// log.info(client.getRawTransaction(hash).toString());

	}

	@Test
	public void testGetBlockInteger() throws Exception {
		if (client == null) {
			setUp();
		}
		int N = 1000;
		List<Transaction> txs = new LinkedList<Transaction>();
		for (int i = 1; i <= N; i++) {
			Block block = client.getBlock(i);
			txs.addAll(block.getTransactions());
		}
		String title = ">>>>>>>>>>>>>>>>>all transactions " + txs.size() + " from the first " + N + " blocks";
		log.info(title);
		log.info("current dir: " + System.getProperty("user.dir"));
		FileOutputStream outf = new FileOutputStream("testnet-txs.txt");
		PrintStream out = new PrintStream(outf);
		// out = System.out;
		out.println(title);
		int i = 0;
		for (Transaction t : txs) {
			log.info("" + i++);
			writeTransaction(t, out);
			// out.println(">>in >>" + t.getInputs().toString());
			// out.println(">>out>>" + t.getOutputs().toString());
		}
		out.close();
		outf.close();
	}

	void writeBlock(int blockNumber) throws JsonRPCStatusException, IOException {
		log.info("block " + blockNumber + ":" + client.getBlock(blockNumber).toString());
	}

	@Test
	public void testTransactions() throws JsonRPCStatusException, IOException {
		// iterate backwards
		int blocks = client.getBlockChainInfo().getBlocks();
		writeBlock(1);
		writeBlock(blocks / 8);
		writeBlock(blocks / 4);
		writeBlock(blocks / 2);
		writeBlock(blocks * 6 / 10);
		writeBlock(blocks * 7 / 10);
		writeBlock(blocks * 76 / 100);
		writeBlock(blocks * 79 / 100);
		writeBlock(blocks * 81 / 100);
		writeBlock(blocks * 9 / 10);
		writeBlock(blocks * 901 / 1000);
		writeBlock(blocks * 969 / 1000);
		writeBlock(blocks * 992 / 1000);
		writeBlock(blocks * 993 / 1000);
		writeBlock(blocks);
		Block blockLast = client.getBlock(blocks);

		int i = 10;
		while (i-- > 0) {
			Block block = client.getBlock(blocks - 1);
			List<Transaction> txs = block.getTransactions();
			for (Transaction t : txs) {
				log.info(t.toString());
			}
		}
	}

	@Test
	public void testGetTransaction() throws Exception {
		if (client == null) {
			setUp();
		}
		Sha256Hash txid = Sha256Hash.wrap("f1114ac0d88a47daa8f28573cf539496c69720e4bf4ff42b60b8d8f6902b30ce");
		// TxOutInfo txo0 = client.getTxOut(txid, 0);
		// TxOutInfo txo1 = client.getTxOut(txid, 1);
		Transaction t = client.getRawTransaction(txid);
		System.out.println(t);
		// TransactionConfidence confidence = t.getConfidence();
		// WalletTransactionInfo wti = client.getTransaction(txid);
		// Block block = client.getBlock(wti.getBlockhash());
	}

	private void writeTransaction(Transaction t, PrintStream out) throws JsonRPCStatusException, IOException {
		for (TransactionInput ti : t.getInputs()) {
			TransactionOutPoint outpoint = ti.getOutpoint();
			log.info(outpoint.toString());
			Sha256Hash hash = outpoint.getHash();
			Transaction tiFrom = client.getRawTransaction(hash);
			TransactionOutput tiFromTo = tiFrom.getOutput(0);
			String fromAddress = ti.isCoinBase() ? "COINBASE" : ti.getFromAddress().toString();
			String value = ti.getValue() == null ? "" : ti.getValue().toPlainString();
			out.println(t.getHashAsString() + ";" + fromAddress + ";" + value);
		}
		for (TransactionOutput to : t.getOutputs()) {
			out.println(t.getHashAsString() + ";;" + to.getSpentBy() + ";" + to.getValue());
		}
	}

}
