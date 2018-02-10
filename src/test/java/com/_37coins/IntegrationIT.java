package com._37coins;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._37coins.bcJsonRpc.BitcoindClientFactory;
import com._37coins.bcJsonRpc.BitcoindInterface;
import com._37coins.bcJsonRpc.pojo.Block;
import com._37coins.bcJsonRpc.pojo.Info;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class IntegrationIT {
	Logger log = LoggerFactory.getLogger(IntegrationIT.class);

	static BitcoindInterface client;

	@BeforeClass
	static public void before() throws MalformedURLException, IOException {
		BitcoindClientFactory clientFactory = new BitcoindClientFactory(new URL("http://localhost:18332/"), "test",
				"test");
		client = clientFactory.getClient();
	}

	@Test
	public void testInfo() throws JsonProcessingException {
		Info info = client.getinfo();
		System.out.println(new ObjectMapper().writeValueAsString(info));
	}

	@Test
	public void testBlock() {
		// get last N blocks
		// log the block and their transactions
		Info info = client.getinfo();
		int currentBlockDepth = (int) info.getBlocks();
		int N = 6; // all transactions of the last 1h
		int successfulBlockRetrieves = 0;
		for (int i = currentBlockDepth; i > currentBlockDepth - N; i--) {
			Block block = null;
			String blockHash = null;
			try {
				blockHash = client.getblockhash(i);
				block = client.getblock(blockHash);
				successfulBlockRetrieves++;
			} catch (Exception e) {
				log.error("error getting block: " + i + " with hash: " + blockHash);
				continue;
			}
			log.info("block: " + i + ": " + block.toString());
			List<String> txs = block.getTx();
			for (String txHash : txs) {
				log.info("retrieving tx " + txHash);
				String txScript = client.getrawtransaction(txHash);
				com._37coins.bcJsonRpc.pojo.RawTransaction tx = client.decoderawtransaction(txScript);
				log.info("transaction: " + tx);
			}
		}
		log.info("successfulBlockRetrieve: " + successfulBlockRetrieves + " of " + N);
	}

	@Test
	public void testRawTransaction() {
		String txHash = "b8f3c72fb4ba9c25e8e070dc5785856ef17a4fc6ad4870a8ce41127a8c0f090e";
		String txScript = client.getrawtransaction(txHash);
		log.info("txScript " + txScript );
	}
	
	@Test
	public void testDecodeRawTransaction() {
		String txHash = "b8f3c72fb4ba9c25e8e070dc5785856ef17a4fc6ad4870a8ce41127a8c0f090e";
		String txScript = client.getrawtransaction(txHash);
		com._37coins.bcJsonRpc.pojo.RawTransaction tx = client.decoderawtransaction(txScript);
		log.info("tx " + tx);
	}

	@Test
	public void testTransaction() {
		String txHash = "f1114ac0d88a47daa8f28573cf539496c69720e4bf4ff42b60b8d8f6902b30ce";
		com._37coins.bcJsonRpc.pojo.Transaction tx = client.gettransaction(txHash);
		log.info("tx " + txHash + ": " + tx.toString());
	}

	@Test
	public void testBalance() {
		BigDecimal balance = client.getbalance();

		log.info("balance = " + balance);

		assertTrue(balance.compareTo(BigDecimal.ZERO) >= 0);
	}

}
