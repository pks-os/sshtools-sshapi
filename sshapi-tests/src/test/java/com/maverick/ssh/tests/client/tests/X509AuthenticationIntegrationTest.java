package com.maverick.ssh.tests.client.tests;

import static org.junit.Assert.assertTrue;

import org.junit.Assume;
import org.junit.Test;

import com.maverick.ssh.tests.ServerCapability;
import com.maverick.ssh.tests.client.AbstractClientConnecting;

import net.sf.sshapi.Capability;
import net.sf.sshapi.auth.SshPublicKeyAuthenticator;
import net.sf.sshapi.auth.SshX509PublicKeyAuthenticator;
import net.sf.sshapi.util.DefaultX509PublicKeyAuthenticator;
import net.sf.sshapi.util.SimplePasswordAuthenticator;

/**
 * <p>
 * Tests for X509 certificate authentication. To generate test keystores :-
 * <blockquote>
 * 
 * <pre>
 * 
 * keytool -genkeypair -keystore keystore -storepass changeit -storetype PKCS12 -keyalg rsa
 * </pre>
 * 
 * </blockquote>
 * </p>
 */
public class X509AuthenticationIntegrationTest extends AbstractClientConnecting {
	public final static String PASSPHRASE = "changeit";

	/**
	 * Ensures a valid X509 certificate authenticates.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testX509Valid() throws Exception {
		assertServerCapabilities(ServerCapability.X509);
		Assume.assumeTrue("Must support X509", ssh.getProvider().getCapabilities().contains(Capability.X509_PUBLIC_KEY));
		SshX509PublicKeyAuthenticator pk = new DefaultX509PublicKeyAuthenticator("mykey", new SimplePasswordAuthenticator(X509AuthenticationIntegrationTest.PASSPHRASE.toCharArray()),
				getClass().getResourceAsStream("/x509-valid/keystore"));
		boolean result = ssh.authenticate(pk);
		assertTrue("Authentication must be complete.", result);
		assertTrue("Must be connected", ssh.isConnected());
	}

}
