package rest.bitcoin;

import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.KeyChain;

public enum UserHDKeys {
	INSTANCE;
	private final static byte[] ENTROPY = Sha256Hash.hash("don't use a string seed like this in real life".getBytes());
	private final static byte[] ENTROPY2 = Sha256Hash
			.hash("don't use a string seed like this in real life - really".getBytes());
	private final static KeyChain userKeyChain = new DeterministicKeyChain(new SecureRandom(ENTROPY));
	private final static KeyChain backupKeyChain = new DeterministicKeyChain(new SecureRandom(ENTROPY2));
	
	private final static List<Address> addresses = new LinkedList<Address>();
	
	int lastKeyCount = 0;

	public KeyChain getUserKeyChain() {
		return userKeyChain;
	}

	public static KeyChain getBackupkeychain() {
		return backupKeyChain;
	}
	
	public List<Address> getAddresses() {
		return addresses;
	}
}
