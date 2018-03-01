package store.bitcoin.pojo;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a stored transaction.
 * 
 * @author milan
 *
 */
public class StoredTransaction implements Serializable, Comparable<StoredTransaction>{
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((txid == null) ? 0 : txid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StoredTransaction other = (StoredTransaction) obj;
		if (txid == null) {
			if (other.txid != null)
				return false;
		} else if (!txid.equals(other.txid))
			return false;
		return true;
	}

	@Override
	public int compareTo(StoredTransaction o) {
		if(this.equals(o))
			return 0;
		else
			return this.time>o.time?-1:1;
	}

}
