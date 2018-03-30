package bitcoin.transaction;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.UTXO;
import org.bitcoinj.core.Utils;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
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
import store.bitcoin.MemoryBlockStore;
import store.bitcoin.StoreLoader;

public class BitcoinTransactionTest {
	private static Logger log = LoggerFactory.getLogger(BitcoinTransactionTest.class);

	final static String clientPrivateKeys1 = "92CqrxbHxU1nDQo2N9UkL6bGftcfbh929cYzPSubPshyERqhUK7";
	final static String clientPublicKeys1 = "03A48BED6D0C1FF608CFBC4F27D7831061A58C927055D0D74B3AD7351E3523D697";
	final static String clientAddresses1 = "mofhdVSgsUsVacWsf8QMNhDQqYnVXPtnZH";

	static BitcoindInterface client;
	static BlockStore store;
	static Configuration config;
	static StoreLoader storeLoader;
	static NetworkParameters params;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		config = new Configurations().properties(new File("bitcoin.properties"));
		BitcoindClientFactory clientFactory = new BitcoindClientFactory(new URL(config.getString("bitcoin.rpc.URL")),
				config.getString("bitcoin.rpc.rpcuser"), config.getString("bitcoin.rpc.rpcpassword"));
		client = clientFactory.getClient();
		params = config.getBoolean("bitcoin.testnet") ? TestNet3Params.get() : MainNetParams.get();
		// store = new DBBlockStore();
		store = new MemoryBlockStore();
		storeLoader = new StoreLoader(store, client);
		// storeLoader.loadStore(6 * 24 * 2); // 28 days in the past
		storeLoader.loadStore(30); // 28 days in the past
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

		Long satoshiAmount = 812L;
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
		fail("not yet implemented");
	}

	Transaction createSingleKeyTransaction(String privKey, long satoshiAmount, String address,
			List<ListUnspentTO> sutxos) {
		
		Collection<UTXO> utxos = convertUTXOs(sutxos);
		
		// String to a ECKey
		DumpedPrivateKey dumpedPrivateKey = DumpedPrivateKey.fromBase58(params, privKey);
		ECKey key = dumpedPrivateKey.getKey();
		
		// String to an address
		Address address2 = Address.fromBase58(params, address);

		Transaction tx = new Transaction(params);
		// add outputs
		// calculate total utxo amount
		Coin totalValue = Coin.ZERO;
		for (UTXO utxo : utxos) {
			log.info("utxo used: "+ store.getTx(utxo.getHash().toString()));
			totalValue = totalValue.add(utxo.getValue());
		}
		// calc fees
		Coin feeAmount = Coin.valueOf(4013);
		Coin sendAmount = Coin.valueOf(satoshiAmount);
		tx.addOutput(sendAmount, address2);
		Coin changeAmount = totalValue.minus(sendAmount).minus(feeAmount);
		assertTrue("UTXO must be greater than 0 for this address: " + address2 + ".",
				totalValue.compareTo(Coin.ZERO) > 0);
		tx.addOutput(changeAmount, key.toAddress(params));

		// add inputs
		// utxos is an array of inputs from my wallet
		for (UTXO utxo : utxos) {
			TransactionOutPoint outPoint = null;
			try {
				String rawPartentTx = client.getrawtransaction(utxo.getHash().toString());
				Transaction parent = new Transaction(params, Utils.HEX.decode(rawPartentTx));
				TransactionOutput from = new TransactionOutput(params,parent ,sendAmount,address2);
				tx.addInput(from);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("error signing script for outpoint" + outPoint);
			}
		}
		SendRequest request = SendRequest.forTx(tx);
		try {
			wallet.sendCoinsOffline(request );
		} catch (InsufficientMoneyException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return tx;
	}

	Collection<UTXO> convertUTXOs(List<ListUnspentTO> sutxos) {
		Collection<UTXO> utxos = new HashSet<>();
		for (ListUnspentTO sutxo : sutxos) {
			Sha256Hash hash = Sha256Hash.wrap(sutxo.getTxid());
			int index = sutxo.getVout();
			BigDecimal satoshis = sutxo.getAmount().multiply(new BigDecimal(100000000));
			// kids don't do this at home possible rounding error
			long satoshisLong = satoshis.longValue();
			Coin value = Coin.valueOf(satoshisLong);
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
		}
		return utxos;
	}

	// Transaction createTransaction(String privKey, int amount, String address,
	// SortedSet<StoredVout> sutxos)
	// throws BlockStoreException {
	// Collection<UTXO> utxos = convertUTXOs(sutxos);
	// // String to a private key
	// DumpedPrivateKey dumpedPrivateKey = DumpedPrivateKey.fromBase58(params,
	// privKey);
	// ECKey key = dumpedPrivateKey.getKey();
	//
	// // String to an address
	// Address address2 = Address.fromBase58(params, address);
	//
	// Transaction tx = new Transaction(params);
	//
	// // calculate total utxo amount
	// Coin totalValue = Coin.ZERO;
	// for (UTXO utxo : utxos) {
	// totalValue = totalValue.add(utxo.getValue());
	// }
	//
	// // add outputs
	// Coin feeAmount = Coin.valueOf(4013);
	// Coin sendAmount = Coin.valueOf(amount);
	// tx.addOutput(sendAmount, address2);
	// Coin changeAmount = totalValue.minus(sendAmount).minus(feeAmount);
	// tx.addOutput(changeAmount, key.toAddress(params));
	//
	// // sign and add inputs
	// // utxos is an array of inputs from my wallet
	// for (UTXO utxo : utxos) {
	// TransactionOutPoint outPoint = new TransactionOutPoint(params,
	// utxo.getIndex(), utxo.getHash());
	// tx.addSignedInput(outPoint, utxo.getScript(), key);
	// }
	// return tx;
	// }
	//
	// Collection<UTXO> convertUTXOs(Collection<StoredVout> sutxos) throws
	// BlockStoreException {
	// Collection<UTXO> utxos = new HashSet<>();
	// for (StoredVout sutxo : sutxos) {
	// Sha256Hash hash = Sha256Hash.wrap(sutxo.getTxID());
	// int index = sutxo.getIndex();
	// BigDecimal satoshis = sutxo.getValue().multiply(new BigDecimal("100000000"));
	// long satoshisLong = satoshis.longValueExact();
	// Coin value = Coin.valueOf(satoshisLong);
	// int height =
	// store.get(store.getTx(sutxo.getTxID()).getBlockHash()).getHeight();
	// boolean coinbase = false;
	// String rtxString = client.getrawtransaction(sutxo.getTxID());
	// Transaction inputTx;
	// // TODO migrate to latest segwit branch or main if already migrated there, so
	// the conversion always works
	// try {
	// inputTx = new Transaction(params, Utils.HEX.decode(rtxString));
	// } catch (Exception e) {
	// log.error("error reading transaction: {}", rtxString);
	// continue;
	// }
	// RawTransaction rtx = client.decoderawtransaction(rtxString);
	// Vout rtxVout = rtx.getVout().get(sutxo.getIndex());
	// log.info(rtxVout.toString());
	// byte[] scriptBytes =
	// inputTx.getOutputs().get(sutxo.getIndex()).getScriptBytes();
	// Script script = null;
	// try {
	// script = new Script(scriptBytes);
	// } catch (Exception e) {
	// log.error("error reading script: {}", Utils.HEX.encode(scriptBytes));
	// continue;
	// }
	// String address = sutxo.getAddresses().get(0);
	// UTXO utxo = new UTXO(hash, index, value, height, coinbase, script, address);
	// utxos.add(utxo);
	// }
	// return utxos;
	// }

	// @Test
	// public void signRawTransaction() {
	// RawTxInput rawTxInput = new
	// RawTxInput("6ab96bee2ce532737281eb98e9e3c132b593d936af02f23914ed451d0bd29921",
	// 0,
	// null);
	// List<RawTxInput> rawTxInputs = new LinkedList<>();
	// rawTxInputs.add(rawTxInput);
	//
	// RawTxOutput rawTxOutput = new RawTxOutput();
	// rawTxOutput.setAddressValuePairs("mmouiZHkpRd2uBUk3UYj2vTJ6NuupY4Agf",
	// 0.599);
	// rawTxOutput.setAddressValuePairs("mofhdVSgsUsVacWsf8QMNhDQqYnVXPtnZH", 0.5);
	//
	// String rawTxString = client.createrawtransaction(rawTxInputs, rawTxOutput);
	//
	// // sign it using key and address
	// String privKey = clientPrivateKeys1; // get the first key as all money is
	// with it
	// String pubKey = clientPublicKeys1; // get the first key as all money is with
	// it
	// String address = clientAddresses1; // get the first address
	//
	// BigInteger privKeyI = Base58.decodeToBigInteger(privKey);
	// ECKey key = ECKey.fromPrivate(privKeyI);
	// log.info("pkey: ", key.getPrivateKeyAsWiF(TestNet3Params.get()));
	// log.info("pubKey: {}", key.getPublicKeyAsHex());
	// // String sha256Hash = "";
	// // ECDSASignature signature = key.sign(Sha256Hash.wrap(sha256Hash));
	//
	// // String pk =
	// // "5192F341264DA502B07C196F9E12D542A0A717D41D03FBC68B55FD7C350F963D";
	// //
	// // byte[] pkBytes = Utils.HEX.base16().upperCase().decode(pk);
	// // ECKey key = ECKey.fromPrivate(pkBytes);
	// //// ECKey key = new ECKey(Utils.HEX.decode(privKey),
	// Utils.HEX.decode(pubKey));
	//
	// // org.bitcoinj.wallet.Wallet wallet;
	// // SendRequest req;
	// // wallet.signTransaction(req);
	//
	// String signedMessage64 = key.signMessage(rawTxString);
	// byte[] signedMessageBytes = Base64.decode(signedMessage64);
	// String signedMessageByteString = Utils.HEX.encode(signedMessageBytes);
	//
	// log.info("signed tx: {}", signedMessageByteString);
	// }
	//
	// @Test
	// public void test() throws BlockStoreException {
	// String privKey = clientPrivateKeys1; // get the first key as all money is
	// with it
	// String address = clientAddresses1; // get the first address
	// Set<String> addresses = new HashSet<>();
	// addresses.add(address);
	// SortedSet<StoredVout> utxos = store.getUnspentVouts(addresses);
	//
	// DumpedPrivateKey dumpedPrivateKey = DumpedPrivateKey.fromBase58(params,
	// privKey);
	// ECKey key = dumpedPrivateKey.getKey();
	//
	// // String to an address
	// Address address2 = Address.fromBase58(params, address);
	//
	// Transaction tx = new Transaction(params);
	//
	// String inputString =
	// "[{\"txid\":\"6ab96bee2ce532737281eb98e9e3c132b593d936af02f23914ed451d0bd29921\",\"vout\":0}]";
	// String outputString = "{\"mmouiZHkpRd2uBUk3UYj2vTJ6NuupY4Agf\":0.599,"
	// + "\"mofhdVSgsUsVacWsf8QMNhDQqYnVXPtnZH\":0.5}";
	//
	// // UTXO utxo = new UTXO(hash, index, value, height, coinbase, script)
	//
	// for (StoredVout vout : utxos) {
	// // UTXO utxo = new UTXO(null); // new UTXO(hash, index, value, height,
	// coinbase,
	// // script, address);
	// log.info("utxo to be used:{}", vout);
	// String inputRawTxString = client.getrawtransaction(vout.getTxID());
	// RawTransaction rawTx = client.decoderawtransaction(inputRawTxString);
	// // log.info("rawtx: {}", rawTxString);
	// byte[] inputRawTxBytes = Utils.HEX.decode(inputRawTxString);
	//
	// Sha256Hash txHash = Sha256Hash.wrap(vout.getTxID());
	// TransactionOutPoint outPoint = new TransactionOutPoint(params,
	// vout.getIndex(), txHash);
	// log.info("outPoint: {}", outPoint);
	// // Script script = new Script(out.getScriptBytes()); // utxo.getScript()
	// Transaction bitcoinjTx = new Transaction(params, inputRawTxBytes);
	// TransactionOutput out = bitcoinjTx.getOutput(vout.getIndex());
	// // tx.addSignedInput(outPoint, out.getScriptPubKey(), key,
	// // Transaction.SigHash.ALL, true);
	// }
	// int amount = 14013;// satoshiAmount;
	// // value is a sum of all inputs, fee is 4013
	// tx.addOutput(Coin.valueOf(amount - 4013), address2);
	//
	// tx.getConfidence().setSource(TransactionConfidence.Source.SELF);
	// tx.setPurpose(Transaction.Purpose.USER_PAYMENT);
	//
	// byte[] rawTx = tx.unsafeBitcoinSerialize();
	//
	// // log.info("tx to broadcast: {}", tx);
	// // b_peerGroup.GetPeerGroup().broadcastTransaction(tx);
	//
	// }
}
