package store.bitcoin.pojo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Stores the vouts for this tx. Note that this class is not inmutable, as it
 * allows the unspent flag to be changed. This is the only exception for the
 * Stored??? classes.
 * 
 * @author milan
 *
 */
public class StoredVout implements Serializable, Comparable<StoredVout> {
	// TODO add an id per vout for UTXO handling
	private static final long serialVersionUID = 4412574968461493796L;
	String txID;
	List<String> addresses;
	BigDecimal value;
	Boolean unspent;

	public StoredVout(String txId, List<String> addresses, BigDecimal value, Boolean unspent) {
		this.txID = txId;
		this.addresses = addresses;
		this.value = value;
		this.unspent = unspent;
	}

	public List<String> getAddresses() {
		return addresses;
	}

	public BigDecimal getAmount() {
		return value;
	}

	public Boolean isUnspent() {
		return unspent;
	}

	public void setUnspent(Boolean unspent) {
		this.unspent = unspent;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((addresses == null) ? 0 : addresses.hashCode());
		result = prime * result + ((txID == null) ? 0 : txID.hashCode());
		result = prime * result + ((unspent == null) ? 0 : unspent.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		StoredVout other = (StoredVout) obj;
		if (addresses == null) {
			if (other.addresses != null)
				return false;
		} else if (!addresses.equals(other.addresses))
			return false;
		if (txID == null) {
			if (other.txID != null)
				return false;
		} else if (!txID.equals(other.txID))
			return false;
		if (unspent == null) {
			if (other.unspent != null)
				return false;
		} else if (!unspent.equals(other.unspent))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "StoredVout [txID=" + txID + ", addresses=" + addresses + ", value=" + value + ", unspent=" + unspent
				+ "]";
	}

	@Override
	public int compareTo(StoredVout o) {
		if(this.equals(o))
			return 0;
		if(this.unspent & !o.unspent)
			return 1;
		return this.value.compareTo(o.value);
	}


	// std vout
	// "vout": [
	// {
	// "value": 0.00005000,
	// "n": 0,
	// "scriptPubKey": {
	// "asm": "OP_DUP OP_HASH160 629fc5c8b0961e31feee492c85597951d9d4eb99
	// OP_EQUALVERIFY OP_CHECKSIG",
	// "hex": "76a914629fc5c8b0961e31feee492c85597951d9d4eb9988ac",
	// "reqSigs": 1,
	// "type": "pubkeyhash",
	// "addresses": [
	// "mpWRv3j7VmxjTraJPPEnhf8MGAr2eteeiE"
	// ]
	// }
	// },
	// {
	// "value": 3.23585679,
	// "n": 1,
	// "scriptPubKey": {
	// "asm": "OP_HASH160 b52b7bfcd637b1edaed9f6430d232d2fdc3123e6 OP_EQUAL",
	// "hex": "a914b52b7bfcd637b1edaed9f6430d232d2fdc3123e687",
	// "reqSigs": 1,
	// "type": "scripthash",
	// "addresses": [
	// "2N9mAPMD823Zt5LJgGD31MjAbeN9LpRP6xw"
	// ]
	// }
	// }
	// ],
	// segwit vouts
	// "vout": [
	// {
	// "value": 0.16777216,
	// "n": 0,
	// "scriptPubKey": {
	// "asm": "0 aa6a5e68608091c738db21030596de387324814aa5b1cdf1111fd91b1cf79acf",
	// "hex":
	// "0020aa6a5e68608091c738db21030596de387324814aa5b1cdf1111fd91b1cf79acf",
	// "type": "witness_v0_scripthash"
	// }
	// },
	// {
	// "value": 0.41596058,
	// "n": 1,
	// "scriptPubKey": {
	// "asm": "0 d844507cdf95975456c2ad1deb0acf3717230f55",
	// "hex": "0014d844507cdf95975456c2ad1deb0acf3717230f55",
	// "type": "witness_v0_keyhash"
	// }
	// }
	// ],
}
