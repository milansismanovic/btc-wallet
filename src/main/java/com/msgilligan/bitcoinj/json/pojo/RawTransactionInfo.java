package com.msgilligan.bitcoinj.json.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.msgilligan.bitcoinj.json.conversion.TransactionHexSerializer;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;

import java.util.ArrayList;
import java.util.List;

/**
 * RawTransaction POJO
 */
@JsonIgnoreProperties(ignoreUnknown=true)
// "hash" property added (present in Bitcoin 0.13)
public class RawTransactionInfo {
    public final String hex;
    public final Sha256Hash txid;
    public final long version;
    public final long locktime;
    public final VinList vin;
    public final VoutList vout;
    public final Sha256Hash blockhash;
    public final int confirmations;
    public final long time;
    public final long blocktime;

    @JsonCreator
    public RawTransactionInfo(@JsonProperty("hex")              String hex,
                              @JsonProperty("txid")             Sha256Hash txid,
                              @JsonProperty("version")          long version,
                              @JsonProperty("locktime")         long locktime,
                              @JsonProperty("vin")              VinList vin,
                              @JsonProperty("vout")             VoutList vout,
                              @JsonProperty("blockhash")        Sha256Hash blockhash,
                              @JsonProperty("confirmations")    int confirmations,
                              @JsonProperty("time")             long time,
                              @JsonProperty("blocktime")        long blocktime) {
        this.hex = hex;
        this.txid = txid;
        this.version = version;
        this.locktime = locktime;
        this.vin = vin;
        this.vout = vout;
        this.blockhash = blockhash;
        this.confirmations = confirmations;
        this.time = time;
        this.blocktime = blocktime;
    }

    /**
     * Construct from a bitcoinj transaction
     * @param transaction A bitcoinj confirmed or unconfirmed transaction
     */
    public RawTransactionInfo(Transaction transaction) {
        this.hex = TransactionHexSerializer.bytesToHexString(transaction.bitcoinSerialize());
        this.txid = transaction.getHash();
        this.version = transaction.getVersion();
        this.locktime = transaction.getLockTime();
        this.blockhash = null;  // For now
        this.confirmations = transaction.getConfidence().getDepthInBlocks();
        this.time = 0; // TODO: block header time of block including transaction
        this.blocktime = this.time; // same as time (see API doc)
        vin = new VinList();
        for (TransactionInput input : transaction.getInputs()) {
            vin.add(new Vin(input.getHash(),
                            input.getOutpoint().getIndex(),
                            input.getScriptSig().toString(),
                            input.getSequenceNumber(),"0"));
        }
        vout = new VoutList();
        for (TransactionOutput output : transaction.getOutputs()) {
            vout.add(new Vout(output.getValue(),
                                output.getIndex(),
                                null,
                                output.getScriptPubKey().toString()));
        }
    }

    public String getHex() {
        return hex;
    }

    public Sha256Hash getTxid() {
        return txid;
    }

    public long getVersion() {
        return version;
    }

    public long getLocktime() {
        return locktime;
    }

    public List<Vin> getVin() {
        return vin;
    }

    public List<Vout> getVout() {
        return vout;
    }

    public Sha256Hash getBlockhash() {
        return blockhash;
    }

    public int getConfirmations() {
        return confirmations;
    }

    public long getTime() {
        return time;
    }

    public long getBlocktime() {
        return blocktime;
    }

    public static class VinList extends ArrayList<Vin> {
    }

    public static class VoutList extends ArrayList<Vout> {
    }

    public static class Vin {
        public final Sha256Hash txid;
        public final  long vout;
        public final  Object scriptSig;
        public final  long sequence;
        public final  String coinbase;

        public Vin(@JsonProperty("txid") Sha256Hash txid,
                   @JsonProperty("vout") long vout,
                   @JsonProperty("scriptSig") Object scriptSig,
                   @JsonProperty("sequence") long sequence,
                   @JsonProperty("coinbase") String coinbase) {
            this.txid = txid;
            this.vout = vout;
            this.scriptSig = scriptSig;
            this.sequence = sequence;
            this.coinbase = coinbase;
        }

        public Sha256Hash getTxid() {
            return txid;
        }

        public long getVout() {
            return vout;
        }

        public Object getScriptSig() {
            return scriptSig;
        }

        public long getSequence() {
            return sequence;
        }

		public String getCoinbase() {
			return coinbase;
		}
    }

    public static class ScriptPubKey {
    	public final  String asm;
        public final  String hex;
        public final  int reqSigs;
        public final  String type;
        public final List<String> addresses;
        
        public ScriptPubKey(@JsonProperty("asm")  String asm,
        		@JsonProperty("hex")  String hex,
                @JsonProperty("reqSigs") int reqSigs,
                @JsonProperty("type")  String type,
                @JsonProperty("addresses") List<String> addresses) {
        this.asm = asm;
        this.hex = hex;
        this.reqSigs = reqSigs;
        this.type = type;
        this.addresses = addresses;
    }

		public String getAsm() {
			return asm;
		}

		public String getHex() {
			return hex;
		}

		public int getReqSigs() {
			return reqSigs;
		}

		public String getType() {
			return type;
		}

		public List<String> getAddresses() {
			return addresses;
		}
    }
    public static class Vout {
        public final  Coin value;
        public final  int n;
        public final  ScriptPubKey scriptPubKey;
        public final  Object scriptPubKeyRaw;

        public Vout(@JsonProperty("value")          Coin value,
                    @JsonProperty("n")              int n,
                    @JsonProperty("scriptPubKey")   ScriptPubKey scriptPubKey,
                    @JsonProperty(value="scriptPubKeyRaw", required=false)   Object scriptPubKeyRaw) {
            this.value = value;
            this.n = n;
            this.scriptPubKey = scriptPubKey;
            this.scriptPubKeyRaw = null;
        }

        public Coin getValue() {
            return value;
        }

        public int getN() {
            return n;
        }

        public ScriptPubKey getScriptPubKey() {
            return scriptPubKey;
        }
    }
}