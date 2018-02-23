package store.bitcoin.pojo;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a stored transaction.
 * 
 * @author milan
 *
 */
public class StoredTransaction implements Serializable{
	private static final long serialVersionUID = -7228504376094471841L;
	String txid;
	String blockHash;
	long time;
	List<StoredVin> vins;
	List<StoredVout> vouts;

	public StoredTransaction(String txid, String blockHash, long time, List<StoredVin> vins, List<StoredVout> vouts) {
		super();
		this.txid = txid;
		this.blockHash = blockHash;
		this.time = time;
		this.vins = vins;
		this.vouts = vouts;
	}

	public String getTxid() {
		return txid;
	}

	public String getBlockHash() {
		return blockHash;
	}

	public long getTime() {
		return time;
	}

	public List<StoredVin> getVins() {
		return vins;
	}

	public List<StoredVout> getVouts() {
		return vouts;
	}

	@Override
	public String toString() {
		return "StoredTransaction [txid=" + txid + ", blockHash=" + blockHash + ", time=" + time + ", vins=" + vins
				+ ", vouts=" + vouts + "]";
	}
}
