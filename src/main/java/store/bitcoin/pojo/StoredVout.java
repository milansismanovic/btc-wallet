package store.bitcoin.pojo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public class StoredVout implements Serializable{
	private static final long serialVersionUID = 4412574968461493796L;
	List<String> addresses;
	BigDecimal value;

	public StoredVout(List<String> addresses, BigDecimal value) {
		this.addresses = addresses;
		this.value = value;
	}

	public List<String> getAddresses() {
		return addresses;
	}

	public BigDecimal getAmount() {
		return value;
	}

	@Override
	public String toString() {
		return "StoredVout [addresses=" + addresses + ", value=" + value + "]";
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
