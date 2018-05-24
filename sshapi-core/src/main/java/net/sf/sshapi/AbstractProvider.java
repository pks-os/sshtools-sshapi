/* 
 * Copyright (c) 2010 The JavaSSH Project
 * All rights reserved.
 * 
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 * 
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 * 
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package net.sf.sshapi;

import java.util.List;

import net.sf.sshapi.agent.SshAgent;
import net.sf.sshapi.hostkeys.SshHostKeyManager;
import net.sf.sshapi.identity.SshIdentityManager;

/**
 * Abstract implementation of an {@link SshProvider}, providing some common
 * methods. All providers will probably want to extend this.
 */
public abstract class AbstractProvider implements SshProvider {

	private boolean supportsDefaultConfiguration = true;
	private final String name;

	protected AbstractProvider(String name) {
		this.name = name;
	}

	protected void setSupportsDefaultConfiguration(boolean supportsDefaultConfiguration) {
		this.supportsDefaultConfiguration = supportsDefaultConfiguration;
	}

	public String getName() {
		return name;
	}

	public final boolean supportsConfiguration(SshConfiguration configuration) {
		try {
			doSupportsConfiguration(configuration);
			checkDefaultConfiguration(configuration);
			return true;
		} catch (UnsupportedOperationException ueo) {
			return false;
		}
	}

	public final SshClient createClient(SshConfiguration configuration) {
		doSupportsConfiguration(configuration);
		checkDefaultConfiguration(configuration);
		SshClient client = doCreateClient(configuration);
		client.init(this);
		return client;
	}

	protected abstract void doSupportsConfiguration(SshConfiguration configuration);

	protected abstract SshClient doCreateClient(SshConfiguration configuration);

	private void checkDefaultConfiguration(SshConfiguration configuration) {
		if (configuration == null) {
			if (supportsDefaultConfiguration) {
				return;
			} else {
				throw new UnsupportedOperationException("Default configuration is not supported. You must supply a configuration.");
			}
		}

		configuration.providerHasCapabilities(this);

		check(configuration.getPreferredClientToServerCipher(), getSupportedCiphers(configuration.getProtocolVersion()));
		check(configuration.getPreferredServerToClientCipher(), getSupportedCiphers(configuration.getProtocolVersion()));
		check(configuration.getPreferredClientToServerMAC(), getSupportedMAC());
		check(configuration.getPreferredServerToClientMAC(), getSupportedMAC());
		check(configuration.getPreferredClientToServerCompression(), getSupportedCompression());
		check(configuration.getPreferredServerToClientCompression(), getSupportedCompression());

		if (configuration.getProxyServer() != null) {
			SshProxyServerDetails proxy = configuration.getProxyServer();
			if (proxy.getType().equals(SshProxyServerDetails.Type.HTTP) && !getCapabilities().contains(Capability.HTTP_PROXY)) {
				throw new UnsupportedOperationException("HTTP proxy is not supported.");
			}
			if (proxy.getType().equals(SshProxyServerDetails.Type.SOCKS4) && !getCapabilities().contains(Capability.SOCKS4_PROXY)) {
				throw new UnsupportedOperationException("SOCKS4 proxy is not supported.");
			}
			if (proxy.getType().equals(SshProxyServerDetails.Type.SOCKS5) && !getCapabilities().contains(Capability.SOCKS5_PROXY)) {
				throw new UnsupportedOperationException("SOCKS5 proxy is not supported.");
			}
		}
	}

	public SshIdentityManager createIdentityManager(SshConfiguration configuration) {
		throw new UnsupportedOperationException();
	}

	public SshHostKeyManager createHostKeyManager(SshConfiguration configuration) throws SshException {
		throw new UnsupportedOperationException();
	}

	public SshAgent connectToLocalAgent(String application, String location, int socketType) throws SshException {
		throw new UnsupportedOperationException();
	}

	static boolean check(String name, List list) {
		boolean ok = name == null || list.contains(name) || name.equals("none");
		if (!ok) {
			throw new UnsupportedOperationException("Capability " + name + " is not one of " + list);
		}
		return ok;
	}

}
