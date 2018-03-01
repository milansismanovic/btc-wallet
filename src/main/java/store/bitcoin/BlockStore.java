package store.bitcoin;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.SortedSet;

import store.bitcoin.pojo.StoredBlock;
import store.bitcoin.pojo.StoredTransaction;
import store.bitcoin.pojo.StoredVout;

public abstract class BlockStore {
	StoredBlock chainHeadBlock;

	/**
	 * <code>chainhead</code> is the latest/newest block in this storage
	 */
	public StoredBlock getChainHead() throws BlockStoreException {
		return chainHeadBlock;
	}

	/**
	 * update chainhead if block newer than current chainhead
	 * 
	 * @param block
	 * @throws BlockStoreException
	 */
	public void updateChainHead(StoredBlock block) throws BlockStoreException {
		if (getChainHeadHeight() < block.getHeight()) {
			chainHeadBlock = block;
		}
	}

	/**
	 * gets the chain head height or -1 if the chain head is null
	 * 
	 * @return
	 * @throws BlockStoreException
	 */
	public int getChainHeadHeight() throws BlockStoreException {
		int chainHeadHeigth = getChainHead() != null ? getChainHead().getHeight() : -1;
		return chainHeadHeigth;
	}

	public BigDecimal getBalance(Collection<String> addresses) throws BlockStoreException{
		SortedSet<StoredVout> utxos = getUnspentVouts(addresses);
		BigDecimal balance = new BigDecimal(0);
		for (StoredVout vout : utxos) {
			balance = balance.add(vout.getAmount());
		}
		return balance;
	}

	public abstract StoredBlock get(String hash) throws BlockStoreException;

	public abstract StoredBlock get(int height) throws BlockStoreException;

	public abstract void put(StoredBlock block) throws BlockStoreException;

	public abstract SortedSet<StoredTransaction> getTx(Collection<String> addresses) throws BlockStoreException;

	public abstract SortedSet<StoredVout> getUnspentVouts(Collection<String> addresses) throws BlockStoreException;

	public abstract void resetStore() throws BlockStoreException;

	/**
	 * Set the vout to spend for all given addresses.
	 * @param tx
	 * @param parentTx 
	 * @param addresses
	 * @throws BlockStoreException
	 */
	public abstract void updateUTXO(StoredTransaction tx, StoredTransaction parentTx, int parentVoutIndex)
			throws BlockStoreException;

	public abstract StoredTransaction getTx(String inputTxid);
}
