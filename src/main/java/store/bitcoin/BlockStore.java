package store.bitcoin;

import java.math.BigInteger;
import java.util.List;

import store.bitcoin.pojo.StoredBlock;
import store.bitcoin.pojo.StoredTransaction;

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
	 * @return
	 * @throws BlockStoreException
	 */
	public int getChainHeadHeight() throws BlockStoreException {
		int chainHeadHeigth = getChainHead() != null ? getChainHead().getHeight() : -1;
		return chainHeadHeigth;
	}

	public abstract StoredBlock get(String hash) throws BlockStoreException;

	public abstract void put(StoredBlock block) throws BlockStoreException;

	public abstract List<StoredTransaction> getTx(List<String> addresses) throws BlockStoreException;

	public abstract List<StoredTransaction> getUnspentTx(List<String> addresses) throws BlockStoreException;

	public abstract BigInteger getBalance(List<String> addresses) throws BlockStoreException;

	public abstract void resetStore() throws BlockStoreException;
}
