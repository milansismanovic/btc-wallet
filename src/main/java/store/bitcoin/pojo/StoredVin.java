package store.bitcoin.pojo;

import java.io.Serializable;
import java.util.List;

public class StoredVin implements Serializable{
	private static final long serialVersionUID = -6444408196368573277L;
	String inputtxid; // coinbase or txid
	List<StoredVout> parentOuts; // null if coinbase

	public StoredVin(String inputtxid, StoredVout[] parentOuts) {
		super();
		this.inputtxid = inputtxid;
	}

	public String getInputtxid() {
		return inputtxid;
	}

	public List<StoredVout> getParentOuts() {
		return parentOuts;
	}

	public void setParentOuts(List<StoredVout> parentOuts) {
		this.parentOuts = parentOuts;
	}

	@Override
	public String toString() {
		return "StoredVin [inputtxid=" + inputtxid + "]";
	}

	// coinbase input
	// "vin": [
	// {
	// "coinbase":
	// "0362971304e3cc8b5a726567696f6e312f50726f6a65637420425443506f6f6c2f0100000a3bf8000000000000",
	// "sequence": 4294967295
	// }
	// ]
	// tx input
	// "vin": [
	// {
	// "txid": "a8c520d36178e35cb8adfc7beb02fc7ad317a27d1cdcb6025f426d767ce0e5ce",
	// "vout": 0,
	// "scriptSig": {
	// "asm":
	// "304402203fcad658ce69549c1ae53522ea2d3e8c80c4dcff5410779247798c68aaf475a802205667919a4a2fba46acce5d35f30ba4623757b00c4fc0c618f2a5f5dd35158a5f[ALL]
	// 032ced6c8df9eebfd18f7ef97deac874e4855d5e2864d395e77d1d009fd9922454",
	// "hex":
	// "47304402203fcad658ce69549c1ae53522ea2d3e8c80c4dcff5410779247798c68aaf475a802205667919a4a2fba46acce5d35f30ba4623757b00c4fc0c618f2a5f5dd35158a5f0121032ced6c8df9eebfd18f7ef97deac874e4855d5e2864d395e77d1d009fd9922454"
	// },
	// "sequence": 4294967295
	// },
	// {
	// "txid": "1d2d79a1f050eef1904406c7d437af4e5b28725a5a78288a183075e7a58b6bea",
	// "vout": 0,
	// "scriptSig": {
	// "asm":
	// "3044022058b41940ce829418e61f194b7dbfe18fecd4cb3eec28d0bbca2c18698da71ab4022043d6c3bb814c2c8d6e436cae5b5bb72bdb711555596bcedc1c5a7d62ab288c5f[ALL]
	// 026261a48149511faa64c087ffe5c07834d5ff18191b5a094e6d13b9f9978d6d6e",
	// "hex":
	// "473044022058b41940ce829418e61f194b7dbfe18fecd4cb3eec28d0bbca2c18698da71ab4022043d6c3bb814c2c8d6e436cae5b5bb72bdb711555596bcedc1c5a7d62ab288c5f0121026261a48149511faa64c087ffe5c07834d5ff18191b5a094e6d13b9f9978d6d6e"
	// },
	// "sequence": 4294967295
	// }
	// ]

	// tx input with txinwitness
	// "vin": [
	// {
	// "txid": "7a2195e9bf0d327dfe40a7e913ea0c59f627a38e8494c9381e83d7a3b5b0b760",
	// "vout": 1,
	// "scriptSig": {
	// "asm": "0014b934c0fa9d9cab05ed77b6fa3a6dcb1df5ede7cd",
	// "hex": "160014b934c0fa9d9cab05ed77b6fa3a6dcb1df5ede7cd"
	// },
	// "txinwitness": [
	// "3044022075125bb098a285224ad6b193116973344ce301cae4317079d6d6483648ec57ce0220038bb7390b81abd708b514cee8c2726f9c3c8cd6b60b2cc2cf9d98c48c2520e201",
	// "03e33944a148f672524deee01bb0e1cd7cef75133534649d1696baf293c96434e9"
	// ],
	// "sequence": 4294967295
	// }
	// ]

	// "vin" : [ (array of json objects)
	// {
	// "txid": "id", (string) The transaction id
	// "vout": n, (numeric)
	// "scriptSig": { (json object) The script
	// "asm": "asm", (string) asm
	// "hex": "hex" (string) hex
	// },
	// "sequence": n (numeric) The script sequence number
	// "txinwitness": ["hex", ...] (array of string) hex-encoded witness data (if
	// any)
	// }
	// ,...
	// ],
}
