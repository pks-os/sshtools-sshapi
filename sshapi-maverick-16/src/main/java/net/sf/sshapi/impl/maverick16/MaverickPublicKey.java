package net.sf.sshapi.impl.maverick16;

import java.io.IOException;

import com.sshtools.publickey.SshPublicKeyFile;

import net.sf.sshapi.SshException;
import net.sf.sshapi.SshPublicKey;

public class MaverickPublicKey implements SshPublicKey {

	private byte[] key;
	private String fingerPrint;
	private String algorithm;
	private int bitLength;
	private com.maverick.ssh.components.SshPublicKey publicKey;

	public MaverickPublicKey(SshPublicKeyFile publicKeyFile) throws com.maverick.ssh.SshException,
			IOException {
		init(publicKeyFile.toPublicKey());
	}

	private void init(com.maverick.ssh.components.SshPublicKey publicKey) throws com.maverick.ssh.SshException {
		this.publicKey = publicKey;
		key = publicKey.getEncoded();
		algorithm = publicKey.getAlgorithm();
		fingerPrint = publicKey.getFingerprint();
		bitLength = publicKey.getBitLength();
	}

	public MaverickPublicKey(com.maverick.ssh.components.SshPublicKey publicKey) throws com.maverick.ssh.SshException {
		init(publicKey);
	}

	public MaverickPublicKey(SshPublicKey publicKey) throws SshException {
		key = publicKey.getEncodedKey();
		fingerPrint = publicKey.getFingerprint();
		algorithm = publicKey.getAlgorithm();
		bitLength = publicKey.getBitLength();
	}

	public com.maverick.ssh.components.SshPublicKey getPublicKey() {
		return publicKey;
	}

	@Override
	public String getAlgorithm() {
		return algorithm;
	}

	@Override
	public String getFingerprint() {
		return fingerPrint;
	}

	@Override
	public byte[] getEncodedKey() {
		return key;
	}

	@Override
	public int getBitLength() {
		return bitLength;
	}
}