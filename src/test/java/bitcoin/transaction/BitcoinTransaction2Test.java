package bitcoin.transaction;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.UTXO;
import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._37coins.bcJsonRpc.BitcoindClientFactory;
import com._37coins.bcJsonRpc.BitcoindInterface;
import com._37coins.bcJsonRpc.ListUnspentTO;

import store.bitcoin.BlockStore;
import store.bitcoin.BlockStoreException;

/**
 * IMPORTANT PLEASE READ TO MAKE UNIT TEST WORK: - install the bitcoin Core Node
 * and run it with testnet - import the clientPrivateKeys1 into your Bitcoin
 * Core Node - configure the bitcoin.conf file to have the following entries
 * testnet=1 server=1 rpcuser=test rpcpassword=test rpcallowip=::/0 txindex=1
 * rpcserialversion=0
 * 
 * @author milan
 *
 */
public class BitcoinTransaction2Test {
	private static Logger log = LoggerFactory.getLogger(BitcoinTransactionTest.class);

	final static String clientPrivateKeys1 = "92CqrxbHxU1nDQo2N9UkL6bGftcfbh929cYzPSubPshyERqhUK7";
	final static String clientPublicKeys1 = "03A48BED6D0C1FF608CFBC4F27D7831061A58C927055D0D74B3AD7351E3523D697";
	final static String clientAddresses1 = "mofhdVSgsUsVacWsf8QMNhDQqYnVXPtnZH";

	static BitcoindInterface client;
	static BlockStore store;
	static Configuration config;
	// static StoreLoader storeLoader;
	static NetworkParameters params;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		config = new Configurations().properties(new File("bitcoin.properties"));
		BitcoindClientFactory clientFactory = new BitcoindClientFactory(new URL(config.getString("bitcoin.rpc.URL")),
				config.getString("bitcoin.rpc.rpcuser"), config.getString("bitcoin.rpc.rpcpassword"));
		client = clientFactory.getClient();
		params = config.getBoolean("bitcoin.testnet") ? TestNet3Params.get() : MainNetParams.get();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * send a small amount to oneself with change to oneself and a small fee
	 * 
	 * @throws BlockStoreException
	 */
	@Test
	public void testSimpleTransaction() throws BlockStoreException {
		String privKey = clientPrivateKeys1; // get the first key as all money is with it
		String address = clientAddresses1; // get the first address
		List<String> addresses = new ArrayList<>();
		addresses.add(address);

		List<ListUnspentTO> utxos = client.listunspent(0, 9999999, addresses, true, new HashMap<String, String>());
		log.info(utxos.toString());

		Long satoshiAmount = 42042L;
		Transaction tx = null;
		try {
			tx = createSingleKeyTransaction(privKey, satoshiAmount, address, utxos);
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		log.info("tx: {}", tx);
		String rawTransaction = Utils.HEX.encode(tx.unsafeBitcoinSerialize());
		log.info("rawtx: {}", rawTransaction);
		String newTxID = null;
		try {
			newTxID = client.sendrawtransaction(rawTransaction);
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		log.info("newTxID: {}", newTxID);
	}

	@Test
	public void testMultiSigTransaction() throws BlockStoreException {
		// take pbk1, 2, 3
		// create multisigaddress
		// get utxos
		// create rawtx
		// sign utxos with prk1 and prk2 and add to the rawtx
		// send rawtx
		//fail("not yet implemented");
	}

	private static final EnumSet<Script.VerifyFlag> MINIMUM_VERIFY_FLAGS = EnumSet.of(Script.VerifyFlag.P2SH,
			Script.VerifyFlag.NULLDUMMY);

	private List<UTXO> utxos = null;
	private List<Transaction> txIns = null;
	private List<TransactionOutput> txInsOuts = null;

	Transaction createSingleKeyTransaction(String privKey, long satoshiAmount, String address,
			List<ListUnspentTO> sutxos) {

		utxos = new ArrayList<>();
		txIns = new ArrayList<>();
		txInsOuts = new ArrayList<>();

		convertUTXOs(sutxos);
		
		// String to a private key
		DumpedPrivateKey dumpedPrivateKey = DumpedPrivateKey.fromBase58(params, privKey);
		ECKey key = dumpedPrivateKey.getKey();

		// String to an address
		Address address2 = Address.fromBase58(params, address);

		Transaction tx = new Transaction(params);

		// calculate total utxo amount
		Coin totalValue = Coin.ZERO;
		for (UTXO utxo : utxos) {
			totalValue = totalValue.add(utxo.getValue());
		}

		// add outputs
		Coin feeAmount = Coin.valueOf(8013);
		Coin sendAmount = Coin.valueOf(satoshiAmount);
		tx.addOutput(sendAmount, address2);
		Coin changeAmount = totalValue.minus(sendAmount).minus(feeAmount);

		assertTrue("UTXO must be greater than 0 for this address: " + address2 + ".",
				totalValue.compareTo(Coin.ZERO) > 0);
		tx.addOutput(changeAmount, key.toAddress(params));

		// First collect all transaction inputs
		// (We actually don't use utxos data structure for anything anymore)
		for (int i = 0; i < utxos.size(); i++) {
			Transaction txIn = txIns.get(i);
			TransactionOutPoint outPoint = null;
			try {
				outPoint = new TransactionOutPoint(params, txInsOuts.get(i));
				tx.addInput(new TransactionInput(params, txIn, new byte[0], outPoint));
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("error signing script for outpoint" + outPoint);
			}
		}

		// Only then sign them to get correct signatures for each input
		int numInputs = tx.getInputs().size();
		for (int i = 0; i < numInputs; i++) {
			TransactionInput txIn = tx.getInput(i);
			if (txIn.getConnectedOutput() == null) {
				log.warn("Missing connected output, assuming input {} is already signed.", i);
				continue;
			}

			Script scriptPubKey = txIn.getConnectedOutput().getScriptPubKey();
			txIn.setScriptSig(scriptPubKey.createEmptyInputScript(key, new Script(txInsOuts.get(i).getScriptBytes())));
			Script inputScript = txIn.getScriptSig();
			byte[] script = txInsOuts.get(i).getScriptBytes();

			try {
				// Calculate signature for the transaction input
				TransactionSignature signature = tx.calculateSignature(i, key, script, Transaction.SigHash.ALL, false);
				int sigIndex = 0;
				// inputScript must have OP_0 in place of the actual signature as it will get replaced on this point
				// That's why we call setScriptSig() with createEmptyInputScript() earlier on
				inputScript = scriptPubKey.getScriptSigWithSignature(inputScript, signature.encodeToBitcoin(), sigIndex);
				txIn.setScriptSig(inputScript);
			} catch (ECKey.KeyIsEncryptedException e) {
				throw e;
			} catch (ECKey.MissingPrivateKeyException e) {
				log.warn("No private key in keypair for input {}", i);
			}
		}

		return tx;
	}

	void convertUTXOs(List<ListUnspentTO> sutxos) {
		for (ListUnspentTO sutxo : sutxos) {
			Sha256Hash hash = Sha256Hash.wrap(sutxo.getTxid());
			int index = sutxo.getVout();
			BigDecimal satoshis = sutxo.getAmount().multiply(new BigDecimal(100000000));
			// kids don't do this at home possible rounding error
			long satoshisLong = satoshis.longValue();
			Coin value = Coin.valueOf(satoshisLong);
			log.info("utxo amount from listunspent = {}, from bitcoinj = {}", sutxo.getAmount(), value.toString());
			// get matching rawtransaction
			String rtxString = client.getrawtransaction(sutxo.getTxid());
			// RawTransaction rtx = client.decoderawtransaction(rtxString);
			Transaction inputTx;
			try {
				inputTx = new Transaction(params, Utils.HEX.decode(rtxString));
			} catch (Exception e) {
				log.error("error reading transaction: {}", rtxString);
				continue;
			}

			// Vout rtxVout = rtx.getVout().get(sutxo.getVout());
			// log.info(rtxVout.toString());
			byte[] scriptBytes = null;
			try {
				scriptBytes = inputTx.getOutputs().get(index).getScriptBytes();
			} catch (Exception e) {
				log.error("error getting output script from tx: {}", rtxString);
				continue;
			}
			Script script = null;
			try {
				script = new Script(scriptBytes);
			} catch (Exception e) {
				log.error("error reading script: from utxo: {}, {}", sutxo, Utils.HEX.encode(scriptBytes));
				continue;
			}

			long height = 1288710;
			String address = sutxo.getAddress();
			boolean coinbase = false;
			UTXO utxo = new UTXO(hash, index, value, (int) height, coinbase, script, address);
			utxos.add(utxo);
			txIns.add(inputTx);
			txInsOuts.add(inputTx.getOutputs().get(index));
		}
	}
}
