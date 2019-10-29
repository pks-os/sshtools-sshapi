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
package net.sf.sshapi.impl.mavericksynergy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.sshtools.client.AbstractKeyboardInteractiveCallback;
import com.sshtools.client.BannerDisplay;
import com.sshtools.client.ClientAuthenticator;
import com.sshtools.client.ConnectionProtocolClient;
import com.sshtools.client.KeyboardInteractiveAuthenticator;
import com.sshtools.client.KeyboardInteractivePrompt;
import com.sshtools.client.KeyboardInteractivePromptCompletor;
import com.sshtools.client.PasswordAuthenticator;
import com.sshtools.client.PublicKeyAuthenticator;
import com.sshtools.client.SshClient;
import com.sshtools.client.SshClientContext;
import com.sshtools.common.command.ExecutableCommand;
import com.sshtools.common.forwarding.ForwardingPolicy;
import com.sshtools.common.knownhosts.HostKeyVerification;
import com.sshtools.common.nio.WriteOperationRequest;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.permissions.UnauthorizedException;
import com.sshtools.common.policy.FileSystemPolicy;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SshPrivateKeyFile;
import com.sshtools.common.publickey.SshPrivateKeyFileFactory;
import com.sshtools.common.shell.ShellPolicy;
import com.sshtools.common.ssh.CachingDataWindow;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.ChannelFactory;
import com.sshtools.common.ssh.ChannelNG;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.ChannelOutputStream;
import com.sshtools.common.ssh.SessionChannel;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshKeyFingerprint;
import com.sshtools.common.ssh.Subsystem;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.SshX509RsaSha1PublicKey;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPrivateKey;

import net.sf.sshapi.AbstractClient;
import net.sf.sshapi.AbstractSshStreamChannel;
import net.sf.sshapi.Logger.Level;
import net.sf.sshapi.SshChannel.ChannelData;
import net.sf.sshapi.SshChannelHandler;
import net.sf.sshapi.SshChannelListener;
import net.sf.sshapi.SshCommand;
import net.sf.sshapi.SshConfiguration;
import net.sf.sshapi.SshShell;
import net.sf.sshapi.auth.SshAgentAuthenticator;
import net.sf.sshapi.auth.SshAuthenticator;
import net.sf.sshapi.auth.SshKeyboardInteractiveAuthenticator;
import net.sf.sshapi.auth.SshPasswordAuthenticator;
import net.sf.sshapi.auth.SshPublicKeyAuthenticator;
import net.sf.sshapi.auth.SshX509PublicKeyAuthenticator;
import net.sf.sshapi.forwarding.AbstractPortForward;
import net.sf.sshapi.forwarding.SshPortForward;
import net.sf.sshapi.hostkeys.AbstractHostKey;
import net.sf.sshapi.hostkeys.SshHostKeyValidator;
import net.sf.sshapi.sftp.SftpClient;
import net.sf.sshapi.util.Util;

class MaverickSynergySshClient extends AbstractClient implements ChannelFactory<SshClientContext>
// implements ForwardingClientListener
{

	protected final class MaverickSynergySshChannel extends ChannelNG<SshClientContext> {
		private CachingDataWindow cached;
		private final ChannelData channelData;
		private ChannelInputStream channelInputStream;
		private ChannelOutputStream channelOutputStream;

		protected MaverickSynergySshChannel(String channelType, SshConnection con, int maximumPacketSize, int initialWindowSize,
				int maximumWindowSpace, int minimumWindowSpace, ChannelData channelData) {
			super(channelType, con, maximumPacketSize, initialWindowSize, maximumWindowSpace, minimumWindowSpace);
			this.channelData = channelData;
			cached = new CachingDataWindow(initialWindowSize, maximumWindowSpace, minimumWindowSpace, maximumPacketSize);
			channelInputStream = new ChannelInputStream(cached);
			channelOutputStream = new ChannelOutputStream(this);
		}

		@Override
		protected boolean checkWindowSpace() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		protected byte[] createChannel() throws IOException {
			return channelData.create();
		}

		protected ChannelInputStream getChannelInputStream() {
			return channelInputStream;
		}

		protected ChannelOutputStream getChannelOutputStream() {
			return channelOutputStream;
		}

		@Override
		protected void onChannelClosed() {
			// TODO Auto-generated method stub
		}

		@Override
		protected void onChannelClosing() {
			// TODO Auto-generated method stub
		}

		@Override
		protected void onChannelData(ByteBuffer data) {
			synchronized (localWindow) {
				cached.put(data);
			}
			// TODO
			// fireData(SshDataListener.EXTENDED, buf, off, len);
		}

		@Override
		protected void onChannelFree() {
			// TODO Auto-generated method stub
		}

		@Override
		protected void onChannelOpen() {
			// TODO Auto-generated method stub
		}

		@Override
		protected void onChannelOpenConfirmation() {
			// TODO Auto-generated method stub
		}

		@Override
		protected void onChannelRequest(String type, boolean wantreply, byte[] requestdata) {
			// TODO Auto-generated method stub
		}

		@Override
		protected void onExtendedData(ByteBuffer data, int type) {
			// TODO
			// fireData(SshDataListener.EXTENDED, buf, off, len);
		}

		@Override
		protected void onLocalEOF() {
			// TODO Auto-generated method stub
		}

		@Override
		protected void onRemoteEOF() {
			// TODO Auto-generated method stub
		}

		@Override
		protected byte[] openChannel(byte[] requestdata) throws WriteOperationRequest, ChannelOpenException {
			// TODO Auto-generated method stub
			return null;
		}
	}

	class BannerDisplayBridge implements BannerDisplay {
		@Override
		public void displayBanner(String message) {
			getConfiguration().getBannerHandler().banner(message);
		}
	}

	class HostKeyVerificationBridge implements HostKeyVerification {
		@Override
		public boolean verifyHost(final String host, final SshPublicKey pk) throws SshException {
			if (getConfiguration().getHostKeyValidator() != null) {
				int status;
				try {
					status = getConfiguration().getHostKeyValidator().verifyHost(new AbstractHostKey() {
						@Override
						public String getComments() {
							return null;
						}

						@Override
						public String getFingerprint() {
							try {
								return stripAlgorithmFromFingerprint(SshKeyFingerprint.getFingerprint(getKey(), getMaverickFingerprintAlgo()));
							} catch (SshException e) {
								throw new RuntimeException(e);
							}
						}

						@Override
						public String getHost() {
							return host;
						}

						@Override
						public byte[] getKey() {
							try {
								return pk.getEncoded();
							} catch (SshException e) {
								throw new RuntimeException(e);
							}
						}

						@Override
						public String getType() {
							return pk.getAlgorithm();
						}
					});
					return status == SshHostKeyValidator.STATUS_HOST_KEY_VALID;
				} catch (net.sf.sshapi.SshException e) {
					SshConfiguration.getLogger().log(Level.ERROR, "Failed to verify host key.", e);
				}
			} else {
				System.out.println("The authenticity of host '" + host + "' can't be established.");
				System.out.println(pk.getAlgorithm() + " key fingerprint is " + pk.getFingerprint());
				return Util.promptYesNo("Are you sure you want to continue connecting?");
			}
			return false;
		}
	}

	class MaverickSshChannel
			extends AbstractSshStreamChannel<SshChannelListener<net.sf.sshapi.SshChannel>, net.sf.sshapi.SshChannel>
			implements net.sf.sshapi.SshChannel {
		private ChannelData channelData;
		private String name;
		private MaverickSynergySshChannel ssh2Channel;

		public MaverickSshChannel(String name, ChannelData channelData) {
			super(getProvider(), getConfiguration());
			this.channelData = channelData;
			this.name = name;
		}

		@Override
		public ChannelData getChannelData() {
			return channelData;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return ssh2Channel.getChannelInputStream();
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			return ssh2Channel.getChannelOutputStream();
		}

		@Override
		public boolean sendRequest(String requesttype, boolean wantreply, byte[] requestdata) throws net.sf.sshapi.SshException {
			// try {
			ssh2Channel.sendChannelRequest(requesttype, wantreply, requestdata);
			// TODO success?
			return true;
			// } catch (SshException e) {
			// throw new net.sf.sshapi.SshException("Failed to send request.",
			// e);
			// }
		}

		public void setSourceChannel(MaverickSynergySshChannel ssh2Channel) {
			this.ssh2Channel = ssh2Channel;
			ssh2Channel.addEventListener(new com.sshtools.common.ssh.ChannelEventListener() {
				@Override
				public void onChannelClose(Channel channel) {
					fireClosed();
				}
				// @Override
				// public void extendedDataReceived(SshChannel arg0, byte[] buf,
				// int off, int len, int arg4) {
				// fireData(SshDataListener.EXTENDED, buf, off, len);
				// }
				//
				// @Override
				// public void dataSent(SshChannel arg0, byte[] buf, int off,
				// int len) {
				// fireData(SshDataListener.SENT, buf, off, len);
				// }
				//
				// @Override
				// public void dataReceived(SshChannel arg0, byte[] buf, int
				// off, int len) {
				// fireData(SshDataListener.RECEIVED, buf, off, len);
				// }

				@Override
				public void onChannelClosing(Channel channel) {
					fireClosing();
				}

				@Override
				public void onChannelDisconnect(Channel channel) {
					// TODO Auto-generated method stub
				}

				@Override
				public void onChannelEOF(Channel channel) {
					fireEof();
				}

				@Override
				public void onChannelOpen(Channel channel) {
					fireOpened();
				}

				@Override
				public void onWindowAdjust(Channel channel, long currentWindowSpace) {
				}
			});
		}

		@Override
		protected void fireData(int direction, byte[] buf, int off, int len) {
			super.fireData(direction, buf, off, len);
		}

		/**
		 * Inform all listeners the channel has reached EOF.
		 */
		protected void fireEof() {
			if (getListeners() != null) {
				for (int i = getListeners().size() - 1; i >= 0; i--)
					getListeners().get(i).eof(this);
			}
		}

		/**
		 * Inform all listeners a request was received.
		 * 
		 * @param requestType request type
		 * @param wantReply remote side wanted reply
		 * @param data data
		 * @return send error reply
		 */
		protected boolean fireRequest(String requestType, boolean wantReply, byte[] data) {
			boolean send = false;
			if (getListeners() != null) {
				for (int i = getListeners().size() - 1; i >= 0; i--) {
					if (getListeners().get(i).request(this, requestType, wantReply, data)) {
						send = true;
					}
				}
			}
			return send;
		}

		@Override
		protected void onClose() throws net.sf.sshapi.SshException {
			ssh2Channel.close();
		}

		@Override
		protected void onOpen() throws net.sf.sshapi.SshException {
		}
	}

	protected SshClientContext sshContext;
	private Set<SshChannelHandler> channelFactories = new LinkedHashSet<>();
	private ChannelFactory<SshClientContext> originalChannelFactory;
	// private ForwardingClient forwarding;
	// private Map forwardingChannels = new HashMap();
	private SshClient sshClient;
	private int timeout = -1;
	private String username;

	MaverickSynergySshClient(SshConfiguration configuration) throws SshException, IOException {
		super(configuration);
	}

	@Override
	public void addChannelHandler(final SshChannelHandler channelFactory) throws net.sf.sshapi.SshException {
		channelFactories.add(channelFactory);
	}

	@Override
	public boolean authenticate(SshAuthenticator... authenticators) throws net.sf.sshapi.SshException {
		try {
			for (SshAuthenticator a : authenticators) {
				if (sshClient.authenticate(createAuthentication(a, ""), 600000)) {
					break;
				}
			}
		} catch (net.sf.sshapi.SshException sshe) {
			if (sshe.getCode() == net.sf.sshapi.SshException.AUTHENTICATION_ATTEMPTS_EXCEEDED)
				return false;
			else
				throw sshe;
		} catch (IOException sshe) {
			throw new net.sf.sshapi.SshException(net.sf.sshapi.SshException.IO_ERROR, sshe);
		} catch (SshException e) {
			throw new net.sf.sshapi.SshException(
					e.getReason() == SshException.BAD_API_USAGE ? net.sf.sshapi.SshException.AUTHENTICATION_CANCELLED
							: net.sf.sshapi.SshException.AUTHENTICATION_FAILED,
					e);
		}
		return isAuthenticated();
	}

	// public void channelClosed(int type, String key, SshTunnel tunnel) {
	// int sshapiType = getTypeForTunnel(type);
	// TunnelChannel channel = (TunnelChannel) forwardingChannels.get(tunnel);
	// if (channel != null) {
	// try {
	// firePortForwardChannelClosed(sshapiType, channel);
	// } finally {
	// forwardingChannels.remove(tunnel);
	// }
	// } else {
	// SshConfiguration.getLogger().log(Level.WARN,
	// "Got a channel closed event for a channel we don't know about (" + key +
	// ").");
	// }
	// }
	public void channelFailure(int type, String key, String host, int port, boolean isConnected, Throwable t) {
		// TODO?
	}

	public boolean checkLocalSourceAddress(SocketAddress arg0, String arg1, int arg2, String arg3, int arg4) {
		return true;
	}

	@Override
	public ChannelNG<SshClientContext> createChannel(String channeltype, SshConnection con)
			throws UnsupportedChannelException, PermissionDeniedException {
		for (SshChannelHandler ch : channelFactories) {
			if (Arrays.asList(ch.getSupportChannelNames()).contains(channeltype)) {
				// TODO requestData used to come through in ch.createChannel(),
				// is this gone?
				byte[] requestData = new byte[0];
				ChannelData channelData = ch.createChannel(channeltype, requestData);
				MaverickSshChannel msc = new MaverickSshChannel(channeltype, channelData);
				MaverickSynergySshChannel ssh2Channel = new MaverickSynergySshChannel(channeltype, con, channelData.getWindowSize(),
						channelData.getPacketSize(), channelData.getPacketSize(), channelData.getWindowSize() * 2, channelData);
				msc.setSourceChannel(ssh2Channel);
				try {
					ch.channelCreated(msc);
				} catch (IOException ioe) {
					throw new RuntimeException(ioe);
				}
				return ssh2Channel;
			}
		}
		return originalChannelFactory.createChannel(channeltype, con);
	}

	@Override
	public Subsystem createSubsystem(String name, SessionChannel session)
			throws UnsupportedChannelException, PermissionDeniedException {
		return originalChannelFactory.createSubsystem(name, session);
	}

	@Override
	public ExecutableCommand executeCommand(String[] args, Map<String, String> environment)
			throws PermissionDeniedException, UnsupportedChannelException {
		return originalChannelFactory.executeCommand(args, environment);
	}

	// public void channelOpened(int type, String key, SshTunnel tunnel) {
	// int sshapiType = getTypeForTunnel(type);
	// TunnelChannel channel = new TunnelChannel(tunnel);
	// forwardingChannels.put(tunnel, channel);
	// // The channel is actually already open, but this will set its state
	// // correctly in the wrapper
	// try {
	// channel.open();
	// } catch (net.sf.sshapi.SshException e) {
	// throw new RuntimeException(e);
	// }
	// firePortForwardChannelOpened(sshapiType, channel);
	// }
	//
	// private int getTypeForTunnel(int type) {
	// int sshapiType = SshPortForward.LOCAL_FORWARDING;
	// if (type == ForwardingClientListener.X11_FORWARDING) {
	// sshapiType = SshPortForward.X11_FORWARDING;
	// } else if (type == ForwardingClientListener.REMOTE_FORWARDING) {
	// sshapiType = SshPortForward.REMOTE_FORWARDING;
	// }
	// return sshapiType;
	// }
	public void forwardingStarted(int type, String key, String host, int port) {
	}

	public void forwardingStopped(int type, String key, String host, int port) {
	}

	@Override
	public int getChannelCount() {
		if (!isConnected()) {
			throw new IllegalStateException("Not connected.");
		}
		return sshClient.getConnection().getConnectionProtocol().getActiveChannels().size();
	}

	@Override
	public String getRemoteIdentification() {
		if (!isConnected()) {
			/*
			 * BUG: Maverick NG does not have a separate connection /
			 * authentication phase, so we can't do this
			 */
			return "Unknown";
		}
		return sshClient.getConnection().getRemoteIdentification();
	}

	@Override
	public int getRemoteProtocolVersion() {
		return SshConfiguration.SSH2_ONLY;
	}

	@Override
	public int getTimeout() throws IOException {
		return sshContext == null ? (timeout < 1 ? 0 : timeout) : sshContext.getIdleConnectionTimeoutSeconds();
	}

	@Override
	public String getUsername() {
		return sshClient == null ? null : username;
	}

	@Override
	public boolean isAuthenticated() {
		return sshClient != null && sshClient.isAuthenticated();
	}

	@Override
	public boolean isConnected() {
		return sshClient != null && sshClient.isConnected();
	}

	@Override
	public void setTimeout(int timeout) throws IOException {
		this.timeout = timeout;
		if (sshContext != null)
			sshContext.setIdleConnectionTimeoutSeconds(timeout);
	}

	@Override
	protected void doConnect(String username, String hostname, int port, SshAuthenticator... authenticators)
			throws net.sf.sshapi.SshException {
		this.username = username;
		try {
			sshClient = new SshClient(hostname, port, username) {
				@Override
				protected void configure(SshClientContext sshContext) throws SshException, IOException {
					MaverickSynergySshClient.this.sshContext = sshContext;
					if (timeout != -1)
						sshContext.setIdleAuthenticationTimeoutSeconds(timeout);
					originalChannelFactory = sshContext.getChannelFactory();
					// TODO not published yet
					// sshContext.setChannelFactory(MaverickSynergySshClient.this);
					sshContext.setHostKeyVerification(new HostKeyVerificationBridge());
					SshConfiguration configuration = getConfiguration();
					// Version
					if (configuration.getProtocolVersion() == SshConfiguration.SSH1_ONLY)
						throw new IllegalArgumentException("SSH1 is not supported by this provider.");
					if (configuration.getPreferredClientToServerCipher() != null) {
						sshContext.setPreferredCipherCS(configuration.getPreferredClientToServerCipher());
					}
					if (configuration.getPreferredServerToClientCipher() != null) {
						sshContext.setPreferredCipherSC(configuration.getPreferredServerToClientCipher());
					}
					if (configuration.getPreferredClientToServerMAC() != null) {
						sshContext.setPreferredMacCS(configuration.getPreferredClientToServerMAC());
					}
					if (configuration.getPreferredServerToClientMAC() != null) {
						sshContext.setPreferredMacSC(configuration.getPreferredServerToClientMAC());
					}
					if (configuration.getPreferredClientToServerCompression() != null) {
						sshContext.setPreferredCompressionCS(configuration.getPreferredClientToServerCompression());
					}
					if (configuration.getPreferredServerToClientCompression() != null) {
						sshContext.setPreferredCompressionCS(configuration.getPreferredServerToClientCompression());
					}
					if (configuration.getPreferredKeyExchange() != null) {
						sshContext.setPreferredKeyExchange(configuration.getPreferredKeyExchange());
					}
					if (configuration.getPreferredPublicKey() != null) {
						sshContext.setPublicKeyPreferredPosition(configuration.getPreferredPublicKey(), 0);
					}
					ShellPolicy shellPolicy = new ShellPolicy();
					if (configuration.getShellWindowSizeMax() > 0)
						shellPolicy.setSessionMaxWindowSize((int) configuration.getShellWindowSizeMax());
					if (configuration.getShellPacketSize() > 0)
						shellPolicy.setSessionMaxPacketSize((int) configuration.getShellPacketSize());
					if (configuration.getShellPacketSize() > 0)
						shellPolicy.setSessionMinWindowSize((int) configuration.getShellWindowSize());
					sshContext.setPolicy(ShellPolicy.class, shellPolicy);
					FileSystemPolicy fsPolicy = new FileSystemPolicy();
					if (configuration.getSftpWindowSizeMax() > 0)
						fsPolicy.setSftpMaxWindowSize((int) configuration.getSftpWindowSizeMax());
					if (configuration.getSftpPacketSize() > 0)
						fsPolicy.setSftpMaxPacketSize((int) configuration.getSftpPacketSize());
					if (configuration.getSftpWindowSize() > 0)
						fsPolicy.setSftpMinWindowSize((int) configuration.getSftpWindowSize());
					sshContext.setPolicy(FileSystemPolicy.class, fsPolicy);
					sshContext.setBannerDisplay(new BannerDisplayBridge());
					ForwardingPolicy forwardingPolicy = new ForwardingPolicy();
					forwardingPolicy.allowForwarding();
					forwardingPolicy.allowGatewayForwarding();
					sshContext.setPolicy(ForwardingPolicy.class, forwardingPolicy);
				}
			};
		} catch (IOException sshe) {
			throw new net.sf.sshapi.SshException(net.sf.sshapi.SshException.IO_ERROR, sshe);
		} catch (SshException e) {
			throw new net.sf.sshapi.SshException("Failed to authenticate.", e);
		}
	}

	@Override
	protected SshCommand doCreateCommand(final String command, String termType, int cols, int rows, int pixWidth, int pixHeight,
			byte[] terminalModes) throws net.sf.sshapi.SshException {
		synchronized (sshClient) {
			return new MaverickSynergySshCommand(getProvider(), getConfiguration(), sshClient.getConnection(), termType, command, cols, rows, pixWidth, pixHeight,
					terminalModes);
		}
	}

	@Override
	protected SshPortForward doCreateLocalForward(final String localAddress, final int localPort, final String remoteHost,
			final int remotePort) throws net.sf.sshapi.SshException {
		ConnectionProtocolClient client = (ConnectionProtocolClient) sshClient.getConnection().getConnectionProtocol();
		final String fLocalAddress = localAddress == null ? "0.0.0.0" : localAddress;
		return new AbstractPortForward(getProvider()) {
			private int boundPort;

			@Override
			public int getBoundPort() {
				return boundPort;
			}

			@Override
			protected void onClose() throws net.sf.sshapi.SshException {
				try {
					client.stopLocalForwarding(fLocalAddress + ":" + localPort);
				} finally {
					boundPort = 0;
				}
			}

			@Override
			protected void onOpen() throws net.sf.sshapi.SshException {
				try {
					boundPort = client.startLocalForwarding(fLocalAddress, localPort, remoteHost, remotePort);
				} catch (SshException e) {
					throw new net.sf.sshapi.SshException("Failed to start local forward.", e);
				} catch (UnauthorizedException e) {
					throw new net.sf.sshapi.SshException(net.sf.sshapi.SshException.UNAUTHORIZED,
							"Not authorized to start forward.", e);
				}
			}
		};
	}

	@Override
	protected SshPortForward doCreateRemoteForward(final String remoteHost, final int remotePort, final String localAddress,
			final int localPort) throws net.sf.sshapi.SshException {
		ConnectionProtocolClient client = (ConnectionProtocolClient) sshClient.getConnection().getConnectionProtocol();
		final String fRemoteHost = remoteHost == null ? "0.0.0.0" : remoteHost;
		return new AbstractPortForward(getProvider()) {
			private int boundPort;

			@Override
			public int getBoundPort() {
				return boundPort;
			}

			@Override
			protected void onClose() throws net.sf.sshapi.SshException {
				try {
					client.stopRemoteForwarding(fRemoteHost, boundPort);
				} catch (SshException e) {
					throw new net.sf.sshapi.SshException("Failed to stop remote forward.", e);
				} finally {
					boundPort = 0;
				}
			}

			@Override
			protected void onOpen() throws net.sf.sshapi.SshException {
				try {
					boundPort = client.startRemoteForwarding(fRemoteHost, remotePort, localAddress, localPort);
				} catch (SshException e) {
					throw new net.sf.sshapi.SshException("Failed to start remote forward.", e);
				}
			}
		};
	}

	@Override
	protected SftpClient doCreateSftp() throws net.sf.sshapi.SshException {
		if (!isConnected()) {
			throw new IllegalStateException("Not connected.");
		}
		return new MaverickSynergySftpClient(this);
	}

	// private String[] getAuthenticationMethods() throws SshException {
	// String[] authenticationMethods =
	// client.getAuthenticationMethods(client.getUsername());
	// return authenticationMethods;
	// }
	@Override
	protected SshShell doCreateShell(String termType, int cols, int rows, int pixWidth, int pixHeight, byte[] terminalModes)
			throws net.sf.sshapi.SshException {
		synchronized (sshClient) {
			return new MaverickSynergySshShell(getProvider(), getConfiguration(), sshClient.getConnection(), termType, cols, rows, pixWidth, pixHeight, terminalModes);
		}
	}

	@Override
	protected void onClose() throws net.sf.sshapi.SshException {
		if (!isConnected()) {
			throw new net.sf.sshapi.SshException(net.sf.sshapi.SshException.NOT_OPEN, "Not connected.");
		}
		try {
			sshClient.close();
			sshClient.getConnection().getDisconnectFuture().waitForever();
		} catch (IOException sshe) {
			throw new net.sf.sshapi.SshException(net.sf.sshapi.SshException.IO_ERROR, sshe);
		} finally {
			// if (forwarding != null) {
			// forwarding.removeListener(this);
			// }
		}
	}

	SshClient getNativeClient() {
		return sshClient;
	}
	
	static String stripAlgorithmFromFingerprint(String fingerprint) {
		int idx = fingerprint.indexOf(':');
		return idx == -1 ? fingerprint : fingerprint.substring(idx + 1);
	}

	String getMaverickFingerprintAlgo() {
		if(SshConfiguration.FINGERPRINT_SHA1.equals(getConfiguration().getFingerprintHashingAlgorithm()))
			return SshKeyFingerprint.SHA1_FINGERPRINT;
		else if(SshConfiguration.FINGERPRINT_SHA256.equals(getConfiguration().getFingerprintHashingAlgorithm()))
			return SshKeyFingerprint.SHA256_FINGERPRINT;
		else
			return SshKeyFingerprint.MD5_FINGERPRINT;
	}

	private ClientAuthenticator createAuthentication(final SshAuthenticator authenticator, String type)
			throws net.sf.sshapi.SshException, SshException {
		// PrivateKeyFileAuthenticator pfa;
		if (authenticator instanceof SshAgentAuthenticator) {
			SshAgentAuthenticator aa = (SshAgentAuthenticator) authenticator;
			return new PublicKeyAuthenticator(((MaverickSynergyAgent) aa.getAgent(getConfiguration())).getAgent());
		} else if (authenticator instanceof SshPasswordAuthenticator) {
			return new PasswordAuthenticator() {
				@Override
				public String getPassword() {
					char[] answer = ((SshPasswordAuthenticator) authenticator).promptForPassword(MaverickSynergySshClient.this,
							"Password");
					return answer == null ? null : new String(answer);
				}
			};
		} else if (authenticator instanceof SshX509PublicKeyAuthenticator) {
			SshX509PublicKeyAuthenticator pk = (SshX509PublicKeyAuthenticator) authenticator;
			try {
				KeyStore keystore = KeyStore.getInstance("PKCS12");
				char[] keystorePassphrase = pk.promptForKeyPassphrase(this, "Passphrase");
				if (keystorePassphrase == null)
					throw new net.sf.sshapi.SshException(net.sf.sshapi.SshException.AUTHENTICATION_CANCELLED);
				keystore.load(new ByteArrayInputStream(pk.getPrivateKey()), keystorePassphrase);
				char[] keyPassphrase = pk.promptForKeyPassphrase(this, "Passphrase");
				if (keyPassphrase == null)
					throw new net.sf.sshapi.SshException(net.sf.sshapi.SshException.AUTHENTICATION_CANCELLED);
				RSAPrivateKey prv = (RSAPrivateKey) keystore.getKey(pk.getAlias(), keyPassphrase);
				X509Certificate x509 = (X509Certificate) keystore.getCertificate(pk.getAlias());
				SshX509RsaSha1PublicKey pubkey = new SshX509RsaSha1PublicKey(x509);
				Ssh2RsaPrivateKey privkey = new Ssh2RsaPrivateKey(prv);
				throw new UnsupportedOperationException("Errrr");
//				return new PublicKeyAuthenticator(SshKeyPair.getKeyPair(pubkey, privkey));
			} catch (net.sf.sshapi.SshException sshe) {
				throw sshe;
			} catch (IOException ioe) {
				throw new net.sf.sshapi.SshException(net.sf.sshapi.SshException.IO_ERROR, ioe);
			} catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException kse) {
				throw new net.sf.sshapi.SshException(net.sf.sshapi.SshException.GENERAL, kse);
			}
		} else if (authenticator instanceof SshPublicKeyAuthenticator) {
			SshPublicKeyAuthenticator pk = (SshPublicKeyAuthenticator) authenticator;
			try {
				SshPrivateKeyFile pkf = SshPrivateKeyFileFactory.parse(pk.getPrivateKey());
				SshKeyPair pair = null;
				for (int i = 2; i >= 0; i--) {
					if (pkf.isPassphraseProtected()) {
						char[] pa = pk.promptForPassphrase(MaverickSynergySshClient.this, "Passphrase");
						if (pa == null) {
							throw new net.sf.sshapi.SshException(net.sf.sshapi.SshException.AUTHENTICATION_CANCELLED);
						}
						try {
							pair = pkf.toKeyPair(new String(pa));
						} catch (InvalidPassphraseException ipe) {
							if (i == 0) {
								throw new net.sf.sshapi.SshException(net.sf.sshapi.SshException.AUTHENTICATION_ATTEMPTS_EXCEEDED);
							}
						}
					} else {
						try {
							pair = pkf.toKeyPair("");
						} catch (InvalidPassphraseException ipe) {
							throw new net.sf.sshapi.SshException(net.sf.sshapi.SshException.AUTHENTICATION_FAILED, ipe);
						}
					}
				}
				return new PublicKeyAuthenticator(pair);
			} catch (net.sf.sshapi.SshException se) {
				throw se;
			} catch (IOException ioe) {
				throw new net.sf.sshapi.SshException(net.sf.sshapi.SshException.IO_ERROR, ioe);
			}
		} else if (authenticator instanceof SshKeyboardInteractiveAuthenticator) {
			final SshKeyboardInteractiveAuthenticator kbi = (SshKeyboardInteractiveAuthenticator) authenticator;
			return new KeyboardInteractiveAuthenticator(new AbstractKeyboardInteractiveCallback() {
				@Override
				public void showPrompts(String name, String instruction, KeyboardInteractivePrompt[] prompts,
						KeyboardInteractivePromptCompletor keyboardInteractivePromptCompletor) {
					String[] prompt = new String[prompts.length];
					boolean[] echo = new boolean[prompts.length];
					for (int i = 0; i < prompts.length; i++) {
						prompt[i] = prompts[i].getPrompt();
						echo[i] = prompts[i].echo();
					}
					String[] answers = kbi.challenge(name, instruction, prompt, echo);
					if (answers != null) {
						for (int i = 0; i < prompts.length; i++) {
							prompts[i].setResponse(answers[i]);
						}
						keyboardInteractivePromptCompletor.complete();
						return;
					}
					keyboardInteractivePromptCompletor.cancel();
				}
			});
		}
		throw new UnsupportedOperationException(
				String.format("Authenticators of type %s are not supported.", authenticator.getClass()));
	}
}
