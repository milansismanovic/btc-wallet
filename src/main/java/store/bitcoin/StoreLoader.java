package store.bitcoin;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._37coins.bcJsonRpc.BitcoindInterface;
import com._37coins.bcJsonRpc.pojo.BlockVerbose;
import com._37coins.bcJsonRpc.pojo.RawTransaction;
import com._37coins.bcJsonRpc.pojo.Vin;
import com._37coins.bcJsonRpc.pojo.Vout;

import store.bitcoin.pojo.StoredBlock;
import store.bitcoin.pojo.StoredTransaction;
import store.bitcoin.pojo.StoredVin;
import store.bitcoin.pojo.StoredVout;

public class StoreLoader {
	private static final Logger log = LoggerFactory.getLogger(StoreLoader.class);

	BitcoindInterface client;

	private BlockStore store;

	public StoreLoader(BlockStore store, BitcoindInterface client) {
		this.client = client;
		this.store = store;
	}

	/**
	 * Loads the store with the last/newest n blocks and stores them including the
	 * block and the transactions with their outputs.
	 * 
	 * @param lastNBlocks
	 * @throws BlockStoreException
	 */
	public void loadStore(int lastNBlocks) throws BlockStoreException {
		int lastBlockNumber = client.getblockcount();
		int prevChainHeadHeight = store.getChainHeadHeight();
//		assertTrue(store.getChainHeadHeight() <= lastBlockNumber);
		if (store.getChainHeadHeight() == lastBlockNumber) {
			return;
		}
		String blockHash = client.getblockhash(lastBlockNumber);
		BlockVerbose block = client.getblock(blockHash, 2);
		StoredBlock storedBlock = new StoredBlock(block.getHash(), block.getHeight(), block.getTime(),
				block.getPreviousblockhash(), createStoredTxList(block));
		for (int i = 0; i < lastNBlocks; i++) {
			try {
				store.put(storedBlock);
				log.info("put block {}", storedBlock);
			} catch (BlockStoreException e) {
				// ignore duplicate entry
				log.debug("duplicate block ignored with hash {}", block.getHash());
			}
			if (!(store.getChainHeadHeight() - i - 1 > prevChainHeadHeight)) {
				break;
			}
			blockHash = block.getPreviousblockhash();
			block = client.getblock(blockHash, 2);
			lastBlockNumber = block.getHeight();
			storedBlock = new StoredBlock(block.getHash(), block.getHeight(), block.getTime(),
					block.getPreviousblockhash(), createStoredTxList(block));
		}
	}

	List<StoredTransaction> createStoredTxList(BlockVerbose block) {
		List<StoredTransaction> stxs = new LinkedList<>();
		for (RawTransaction tx : block.getTx()) {
			// get all vins into svins without the parents tx outputs
			// this needs to be done with update BlockStore.updateTxOutputs
			List<StoredVin> svins = new LinkedList<>();
			for (Vin vin : tx.getVin()) {
				// TODO load outputs for this txid to new field in StoredVin
				StoredVin svin = new StoredVin(vin.getTxid(), null);
				svins.add(svin);
			}
			// get all vouts into svouts
			List<StoredVout> svouts = new LinkedList<>();
			for (Vout vout : tx.getVout()) {
				List<String> addressList = vout.getScriptPubKey().getAddresses();
				StoredVout svout = new StoredVout(addressList, vout.getValue());
				svouts.add(svout);
			}
			// create tx and add it to the list
			long time = block.getTime();
			StoredTransaction stx = new StoredTransaction(tx.getTxid(), tx.getHash(), time, svins, svouts);
			stxs.add(stx);
		}
		return stxs;
	}
}
