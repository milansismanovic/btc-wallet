package store.bitcoin;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import store.bitcoin.pojo.StoredBlock;
import store.bitcoin.pojo.StoredTransaction;
import store.bitcoin.pojo.StoredVout;

public class MemoryBlockStore extends BlockStore {
	Map<String, StoredBlock> blockMap = new HashMap<String, StoredBlock>();
	Map<String, List<StoredTransaction>> addressTransactions = new HashMap<String, List<StoredTransaction>>();

	@Override
	public StoredBlock get(String hash) throws BlockStoreException {
		return blockMap.get(hash);
	}

	@Override
	public void put(StoredBlock block) throws BlockStoreException {
		blockMap.put(block.getHash(), block);
		for (StoredTransaction tx : block.getTxs()) {
			for (StoredVout vout : tx.getVouts()) {
				List<String> addresses = vout.getAddresses();
				if (addresses == null)
					continue;
				for (String address : vout.getAddresses()) {
					List<StoredTransaction> thisaddressTransactions = addressTransactions.get(address);
					if (thisaddressTransactions == null) {
						thisaddressTransactions = new LinkedList<StoredTransaction>();
						addressTransactions.put(address, thisaddressTransactions);
					}
					thisaddressTransactions.add(tx);
				}
			}
		}
		updateChainHead(block);
	}

	@Override
	public List<StoredTransaction> getTx(List<String> addresses) throws BlockStoreException {
		List<StoredTransaction> stxs = new LinkedList<>();
		for (String address : addresses) {
			List<StoredTransaction> thisaddressTxs = addressTransactions.get(address);
			if (thisaddressTxs != null) {
				stxs.addAll(thisaddressTxs);
			}
		}
		return stxs;
	}

	@Override
	public List<StoredTransaction> getUnspentTx(List<String> addresses) throws BlockStoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigInteger getBalance(List<String> addresses) throws BlockStoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void resetStore() throws BlockStoreException {
		blockMap.clear();
		addressTransactions.clear();
	}

}
