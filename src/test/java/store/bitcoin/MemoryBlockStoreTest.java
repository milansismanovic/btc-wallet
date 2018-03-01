package store.bitcoin;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._37coins.bcJsonRpc.BitcoindClientFactory;
import com._37coins.bcJsonRpc.BitcoindInterface;

import store.bitcoin.pojo.StoredBlock;
import store.bitcoin.pojo.StoredTransaction;
import store.bitcoin.pojo.StoredVin;
import store.bitcoin.pojo.StoredVout;

public class MemoryBlockStoreTest {
	private static final Logger log = LoggerFactory.getLogger(MemoryBlockStoreTest.class);
	private static MemoryBlockStore store;
	private static BitcoindInterface client;
	private static StoreLoader storeLoader;

	@BeforeClass
	public static void setUpBeforeClass()
			throws BlockStoreException, MalformedURLException, IOException, ConfigurationException {
		store = new MemoryBlockStore();
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
		store.resetStore();
		int n = 6 * 24 * 30; // 30 days in the past
		log.info("blockCount: {}. Loading {} blocks.", client.getblockcount(), n);
		storeLoader.loadStore(n);
		// iterate through the blocks in the store
		StoredBlock block = store.getChainHead();
		while (block != null) {
			log.info("block: {}", block);
			block = store.get(block.getPreviousblockhash());
		}
		log.info("chainhead before update: {}", store.getChainHead());
		storeLoader.loadStore(n); // updates store if new blocks arrived
		log.info("chainhead after update: {}", store.getChainHead());
		List<String> addresses = new LinkedList<>();
		addresses.add("mofhdVSgsUsVacWsf8QMNhDQqYnVXPtnZH");
		List<StoredTransaction> txs = store.getTx(addresses);
		log.info("number of txs with adr: 'mofhdVSgsUsVacWsf8QMNhDQqYnVXPtnZH' {}", txs.size());
		for (StoredTransaction tx : txs) {
			log.info("tx: {}", tx);
			if (tx.getVins() != null) {
				for (StoredVin vin : tx.getVins()) {
					StoredTransaction parentTx = store.getTx(vin.getInputtxid());
					if (parentTx == null || parentTx.getVouts() == null)
						continue;
					for (StoredVout parentVout : parentTx.getVouts()) {
						log.info("\tvins:{}", parentVout);
					}
				}
			}
			if (tx.getVouts() != null) {
				for (StoredVout vout : tx.getVouts()) {
					log.info("\tvouts:{}", vout);
				}
			}
		}
		// check for duplicates
		for (int i = 0; i < txs.size(); i++) {
			log.info("tx: {}", txs.get(i));
			for (int j = i + 1; j < txs.size(); j++) {
				if(txs.get(i).equals(txs.get(j))){
					log.info("\tduplicate tx: {}", txs.get(i));
				}
			}
		}
	}
}
