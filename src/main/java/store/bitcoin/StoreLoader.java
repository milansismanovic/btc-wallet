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

/**
 * Loads the underlying {@link BlockStore} using the {@link BitcoindInterface}.
 * It is responsible that after loading the store is consistent for our user's
 * addresses. I.e. has all the transactions.
 * 
 * @author milan
 *
 */
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
	 * block and the transactions with their outputs. Since we only have a certain
	 * amount of blocks, not all vout's unspent are known. For those who's blocks we
	 * have, the unspent flag will be correctly set.
	 * 
	 * @param lastNBlocks
	 * @throws BlockStoreException
	 */
	public void loadStore(int lastNBlocks) throws BlockStoreException {
		int lastBlockNumber = client.getblockcount();
		int prevChainHeadHeight = store.getChainHeadHeight();
		// assertTrue(store.getChainHeadHeight() <= lastBlockNumber);
		if (store.getChainHeadHeight() == lastBlockNumber) {
			return;
		}
		String blockHash = client.getblockhash(lastBlockNumber);
		BlockVerbose block = client.getblock(blockHash, 2);
		StoredBlock storedBlock = new StoredBlock(block.getHash(), block.getHeight(), block.getTime(),
				block.getPreviousblockhash(), createStoredTxList(block));
		int firstBlockStoredHeight = storedBlock.getHeight();
		int lastBlockStoredHeight = 0;
		for (int i = 0; i < lastNBlocks; i++) {
			try {
				store.put(storedBlock);
				lastBlockStoredHeight = storedBlock.getHeight();
				log.info("put block {}", storedBlock.getHash());
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
		// second pass through all the blocks, now from the store, to:
		// 1. update the unspent flag for the new blocks going through all the blocks
		// previously put into the db block chain
		// 2. add the address to transaction associations for input transactions
		for (int blockheight = firstBlockStoredHeight; blockheight >= lastBlockStoredHeight; blockheight--) {
			storedBlock = store.get(blockheight);
			for (StoredTransaction tx : storedBlock.getTxs()) {
				// Set all vouts' unspent flag to true if null.
				for (StoredVout vout : tx.getVouts()) {
					// all vout's unspent flag that is null can be set to true
					// as we know there are no newer transactions that could have
					// spent it
					if (vout.isUnspent() == null) {
						vout.setUnspent(true);
					}
				}
				for (StoredVin vin : tx.getVins()) {
					try {
						String inputTxid = vin.getInputtxid();
						StoredTransaction parentTx = store.getTx(inputTxid);
						// TODO manage what should happen if the input tx to this is missing
						// Note: it could be OK, as we can choose to handle only our user's tx properly
						// and accept other addresses to have missing UTXOs. For this need to be sure,
						// that we are loading all blocks from the start date of the first user's
						// wallet.
						if (parentTx == null) {
							log.debug("missing input tx {} for vin.tx {}", tx.getTxid(), inputTxid);
							continue;
						}
						// set spent to true for all parent outputs that match these output addresses
						store.updateUTXO(tx, parentTx, vin.getVout());
					} catch (Exception e) {
						throw new BlockStoreException("error adding vin: " + vin, e);
					}
				}
			}
		}
	}

	List<StoredTransaction> createStoredTxList(BlockVerbose block) {
		List<StoredTransaction> stxs = new LinkedList<>();
		for (RawTransaction tx : block.getTx()) {
			// get all vins into svins without the parents tx outputs
			// this needs to be done with update BlockStore.updateTxOutputs
			List<StoredVin> svins = new LinkedList<>();
			for (Vin vin : tx.getVin()) {
				// Initial plan was to load outputs for this txid to new field in StoredVin
				// called parentouts. But this would mean to hav another call to the BC Node
				// and getting the parent tx, which would take like forever.
				int vout = vin.getVout() == null ? -1 : vin.getVout();
				StoredVin svin = new StoredVin(vin.getTxid(), vout);
				svins.add(svin);
			}
			// get all vouts into svouts
			List<StoredVout> svouts = new LinkedList<>();
			int index = 0;
			for (Vout vout : tx.getVout()) {
				List<String> addressList = vout.getScriptPubKey().getAddresses();
				// the check if the tx has been spent must be done in a second run
				// therefore we set its unspent value to null meaning unknown
				StoredVout svout = new StoredVout(tx.getTxid(), index++, addressList, vout.getValue(), null);
				svouts.add(svout);
			}
			// create tx and add it to the list
			long time = block.getTime();
			StoredTransaction stx = new StoredTransaction(tx.getTxid(), block.getHash(), time, svins, svouts);
			stxs.add(stx);
		}
		return stxs;
	}
}
