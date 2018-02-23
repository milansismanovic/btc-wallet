package store.bitcoin.pojo;

import java.util.List;

//import com._37coins.bcJsonRpc.pojo.BlockVerbose;

/**
 * POJO that represents a block from the blockchain. It is possible that the
 * block from the constructor is null if the block is
 * 
 * @author milan
 *
 */
public class StoredBlock {
	String hash;
	int height;
	long time;
	String previousblockhash;
	List<StoredTransaction> txs;

	// public StoredBlock(BlockVerbose block) {
	// hash = block.getHash();
	// height = block.getHeight();
	// time = block.getTime();
	// previousblockhash = block.getPreviousblockhash();
	// }

	public StoredBlock(String hash, int height, long time, String previousblockhash, List<StoredTransaction> txs) {
		super();
		this.hash = hash;
		this.height = height;
		this.time = time;
		this.previousblockhash = previousblockhash;
		this.txs = txs;
	}

	public String getHash() {
		return hash;
	}

	public int getHeight() {
		return height;
	}

	public long getTime() {
		return time;
	}

	public String getPreviousblockhash() {
		return previousblockhash;
	}

	public List<StoredTransaction> getTxs() {
		return txs;
	}

	@Override
	public String toString() {
		return "StoredBlock [hash=" + hash + ", height=" + height + ", time=" + time + ", previousblockhash="
				+ previousblockhash + "]";
	}
}
