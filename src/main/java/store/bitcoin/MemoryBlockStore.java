package store.bitcoin;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import store.bitcoin.pojo.StoredBlock;
import store.bitcoin.pojo.StoredTransaction;
import store.bitcoin.pojo.StoredVout;

/**
 * Implements {@link BlockStore} into Memory using {@link Collection} classes.
 * 
 * @author milan
 *
 */
public class MemoryBlockStore extends BlockStore {
	private static final Logger log = LoggerFactory.getLogger(MemoryBlockStore.class);

	Map<String, StoredBlock> blockMap = new HashMap<String, StoredBlock>();
	Map<String, StoredTransaction> txMap = new HashMap<String, StoredTransaction>();
	Map<String, SortedSet<StoredTransaction>> addressTransactions = new HashMap<String, SortedSet<StoredTransaction>>();
	Map<String, List<StoredVout>> addressVouts = new HashMap<String, List<StoredVout>>();

	@Override
	public StoredBlock get(String hash) throws BlockStoreException {
		return blockMap.get(hash);
	}

	@Override
	public void put(StoredBlock block) throws BlockStoreException {
		blockMap.put(block.getHash(), block);
		for (StoredTransaction tx : block.getTxs()) {
			txMap.put(tx.getTxid(), tx);
			// Map<String,StoredVout> addressTxVouts = new HashMap<>();
			for (StoredVout vout : tx.getVouts()) {
				List<String> addresses = vout.getAddresses();
				if (addresses == null)
					continue;
				// store the addressTransactions from the vouts
				for (String address : vout.getAddresses()) {
					// ensures addressTransactions.get(address) will not be null
					addAddressTransactions(address, tx);
					// store the addressVouts from the vouts
					if (addressVouts.get(address) == null) {
						addressVouts.put(address, new LinkedList<StoredVout>());
					}
					addressVouts.get(address).add(vout);
				}
			}
		}
		updateChainHead(block);
	}

	StoredTransaction doubleTx;

	/**
	 * Add a transaction to the addressTransaction map. If there is no entry for
	 * this address, create one.
	 * 
	 * @param address
	 * @param tx
	 */
	void addAddressTransactions(String address, StoredTransaction tx) {
		SortedSet<StoredTransaction> thisaddressTransactions = addressTransactions.get(address);
		if (thisaddressTransactions == null) {
			thisaddressTransactions = new TreeSet<StoredTransaction>();
			addressTransactions.put(address, thisaddressTransactions);
		}
		// FIXME check what is wrong with tx
		// "538185dd71bbbab9e2f8fb9da0c89c77f065393d92482629b4af940cb2bcc09a"
		// it is added twice to the addressTransaction set. Thus this funny code below.
		// otherwise only
		// thisaddressTransactions.add(tx); would be needed.
		final String debugStopAddress = "538185dd71bbbab9e2f8fb9da0c89c77f065393d92482629b4af940cb2bcc09a";
		if (tx.getTxid().equals(debugStopAddress)) {
			log.info("debugTx: set():{}", thisaddressTransactions);
			if (thisaddressTransactions.contains(tx))
				log.info("already there");
			if (doubleTx != null) {
				for (StoredTransaction stx : thisaddressTransactions) {
					if (stx.getTxid().equals(debugStopAddress)) {
						log.info("stx hashcodes {}", stx.hashCode());
						log.info("tx hashcodes {}", tx.hashCode());
						if (tx.equals(stx)) {
							// evidence 1 of erroneous set behavior: contains returns false even though we
							// are iterating through
							// the set's content.
							log.info("set.contains(tx):{}", thisaddressTransactions.contains(tx));
							// evidence 2: the hashcode and equals show stx and tx are the same
							log.info("tx.equals(stx): {}", tx.equals(stx));
							log.info("stx.hashCode()=tx.hashCode(): {} ", (stx.hashCode() == tx.hashCode()));
							log.info("set size:{} ", thisaddressTransactions.size());
							thisaddressTransactions.add(tx);
							log.info("set size:{}", thisaddressTransactions.size());
						} else {
							log.info("different");
						}
						break;
					}
				}
			}
		}
		thisaddressTransactions.add(tx);
		if (tx.getTxid().equals(debugStopAddress)) {
			doubleTx = tx;
			StoredTransaction stx = getTx(debugStopAddress);
			if (tx.equals(stx))
				log.info("same");
			// weird this set contains duplicate transactions which must not happen
			log.info("debugTx: set(): {}", thisaddressTransactions);
		}
	}

	@Override
	public SortedSet<StoredTransaction> getTx(Collection<String> addresses) throws BlockStoreException {
		SortedSet<StoredTransaction> stxs = new TreeSet<>();
		for (String address : addresses) {
			Set<StoredTransaction> thisaddressTxs = addressTransactions.get(address);
			if (thisaddressTxs != null) {
				stxs.addAll(thisaddressTxs);
			}
		}
		return stxs;
	}

	@Override
	public StoredTransaction getTx(String inputTxid) {
		return txMap.get(inputTxid);
	}

	@Override
	public StoredBlock get(int height) throws BlockStoreException {
		for (StoredBlock block : blockMap.values()) {
			if (block.getHeight() == height) {
				return block;
			}
		}
		return null;
	}

	/**
	 * Does two things: 1. Updates all vouts unspent of this <code>parentTx</code>
	 * to false. 2. Adds this <code>parentTx</code> to the
	 * <code>addressTransactions</code> map for the parentTx.vout.addresses.
	 */
	@Override
	public void updateUTXO(StoredTransaction tx, StoredTransaction parentTx, int parentVoutIndex)
			throws BlockStoreException {
		StoredVout parentVout = parentTx.getVouts().get(parentVoutIndex);
		parentVout.setUnspent(false);
		List<String> addresses = parentVout.getAddresses();
		if (addresses != null) {
			for (String voutAdress : addresses) {
				addAddressTransactions(voutAdress, tx);
			}
		}
	}

	@Override
	public void resetStore() throws BlockStoreException {
		blockMap.clear();
		addressTransactions.clear();
		txMap.clear();
		addressVouts.clear();
	}

	@Override
	public SortedSet<StoredVout> getUnspentVouts(Collection<String> addresses) throws BlockStoreException {
		SortedSet<StoredTransaction> allTx = getTx(addresses);
		SortedSet<StoredVout> utxos = new TreeSet<>();
		for (StoredTransaction tx : allTx) {
			if (tx.getVouts() != null) {
				for (StoredVout vout : tx.getVouts()) {
					if (vout.isUnspent()) {
						utxos.add(vout);
						continue;
					}
				}
			}
		}
		return utxos;
	}
}
