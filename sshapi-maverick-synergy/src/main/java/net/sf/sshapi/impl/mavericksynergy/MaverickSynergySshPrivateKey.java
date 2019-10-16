package net.sf.sshapi.impl.mavericksynergy;

import java.io.IOException;
import java.security.PrivateKey;

import net.sf.sshapi.SshException;
import net.sf.sshapi.SshPrivateKey;

public class MaverickSynergySshPrivateKey implements com.sshtools.common.ssh.components.SshPrivateKey {
	private final SshPrivateKey key;

	public MaverickSynergySshPrivateKey(SshPrivateKey key) {
		this.key = key;
	}

	public byte[] sign(byte[] data) throws IOException {
		try {
			return key.sign(data);
		} catch (SshException e) {
			throw new IOException(
					String.format("Failed to sign %d bytes.", new Object[] { String.valueOf(data.length) }), e);
		}
	}

	public PrivateKey getJCEPrivateKey() {
		throw new UnsupportedOperationException();
	}

	public String getAlgorithm() {
		return key.getAlgorithm();
	}

	@Override
	public byte[] sign(byte[] data, String signingAlgorithm) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
}