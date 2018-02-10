package com.msgilligan.bitcoinj.rpc;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.TestNet3Params;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BitcoinExtendedClientTest2 {
	private static final Logger log = LoggerFactory.getLogger(BitcoinExtendedClientTest2.class);

	static BitcoinExtendedClient client;

	final static String host = "localhost";
	static String rpcuser = "test";
	static String rpcpassword = "test";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		if (client == null) {
			NetworkParameters netParams = TestNet3Params.get();
			// NetworkParameters netParams = MainNetParams.get();
			URI server = RPCURI.getDefaultTestNetURI();
			client = new BitcoinExtendedClient(netParams, server, rpcuser, rpcpassword);
			RPCConfig config = new RPCConfig(TestNet3Params.get(), server, rpcuser, rpcpassword);
			client = new BitcoinExtendedClient(config);
			log.info(client.getBlockChainInfo().toString());
		}
	}

	// error block for height = 1280329
	Sha256Hash blockHashParseErrorNegativeArraySizeException = Sha256Hash
			.wrap("0000000000000a4013ee34b098e9731b232c268609c9308965b1159ba2b83a6c");
	// error block for height = 1280327
	Sha256Hash blockHashInvalidBlock = Sha256Hash
			.wrap("0000000000001daf392e4596e931f5bd076f41c6bc63c9f4fa0c41ac3edb2881");

	@Test
	public void testInvalidBlock() throws Exception {
		testBlock(blockHashInvalidBlock);
	}

	@Test
	public void testBlockHashParseErrorNegativeArraySizeException() throws Exception {
		testBlock(blockHashParseErrorNegativeArraySizeException);
	}

	void testBlock(Sha256Hash hash) throws Exception {
		Block block;
		try {
			block = client.getBlock(hash);
			log.info("block: " + block.toString());
		} catch (Exception e) {
			log.error("error getting block with hash: " + hash.toString() + ", exception type: "
					+ e.getClass().toString() + ", errormessage: " + e.getMessage());
			throw e;
		}
	}

	// block that works with the current setup for the transaction test
	Sha256Hash[] blockHashWorks = { Sha256Hash.wrap("0000000000001552580a96192eb3418f58add901e517e11221e42d77af1533e6"),
			Sha256Hash.wrap("0000000000001e1e908ab1fb88e7a8ee84963eb553830a5b43d8397462a2f69c"),
			Sha256Hash.wrap("0000000000000e7ac7d4c9b5f1d0f53fd8870546386ce06559d5b8eca90361b3"),
			Sha256Hash.wrap("00000000000010b37abbe5a714d411b9d4afd6d768470bdae5c9c887ba760f77"),
			Sha256Hash.wrap("000000000000118b1c39232e9d2d8dc2b953b45739c66c1f8ebe137de9d64553"),
			Sha256Hash.wrap("0000000000001c3f03cc2add324d274fefca3a014217a3d98956264b80bae30f"),
			Sha256Hash.wrap("0000000000000af0042aef6df10aef0d5d84cc6887b852f243956304c4fd3793") };

	@Test
	public void testGetTransaction() throws Exception {
		for (Sha256Hash blockHash : blockHashWorks) {
			Block block = client.getBlock(blockHash);
			log.info(block.toString());
			List<Transaction> txs = block.getTransactions();
			for (Transaction t : txs) {
//				log.info("transaction: " + t.toString());
				log.info(client.getRawTransaction(t.getHash()).toString());
			}
		}
	}

	@Test
	public void testGetBlock() throws Exception {
		// get last N blocks
		// log the block and their transactions
		int currentBlockDepth = client.getBlockCount();
		int N = 100;
		int successfulBlockRetrieves = 0;
		List<String> successfulBlockHashes = new LinkedList<>();
		List<String> unsuccessfulBlockHashes = new LinkedList<>();
		for (int i = currentBlockDepth; i > currentBlockDepth - N; i--) {
			Block block;
			try {
				block = client.getBlock(i);
				successfulBlockRetrieves++;
				successfulBlockHashes.add(block.getHashAsString());
			} catch (Exception e) {
				Sha256Hash hash = client.getBlockHash(i);
				unsuccessfulBlockHashes.add("["+hash.toString()+","+e.getClass().getSimpleName()+"]");
				log.error("error getting block: " + i + ", hash: " + hash.toString() + ", exception type: "
						+ e.getClass().toString() + ", errormessage: " + e.getMessage());
				continue;
			}
			log.info("block: " + i + ": " + block.toString());
		}
		log.info("successfulBlockHashes: {}", successfulBlockHashes);
		log.info("unsuccessfulBlockHashes: {}", unsuccessfulBlockHashes);
		log.info("successfulBlockRetrieve: " + successfulBlockRetrieves + " of " + N);
	}
}
