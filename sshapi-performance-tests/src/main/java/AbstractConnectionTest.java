import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.slf4j.LoggerFactory;

import net.sf.sshapi.DefaultProviderFactory;
import net.sf.sshapi.Logger;
import net.sf.sshapi.SshClient;
import net.sf.sshapi.SshConfiguration;
import net.sf.sshapi.SshException;
import net.sf.sshapi.SshProvider;
import net.sf.sshapi.util.DumbHostKeyValidator;
import net.sf.sshapi.util.SimplePasswordAuthenticator;

abstract class AbstractConnectionTest {

	private static final Logger LOG = SshConfiguration.getLogger();

	protected final static org.slf4j.Logger log;

	protected static Properties PROPERTIES = new Properties();
	static {
		File propertyFile = new File("ssh-test.properties");
		try {
			FileInputStream fin = new FileInputStream(propertyFile);
			try {
				PROPERTIES.load(fin);
			} finally {
				fin.close();
			}
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
		BasicConfigurator.configure();
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.INFO);
		SshConfiguration.setLogger(new Logger() {
			@Override
			public void log(Level level, String message, Throwable exception, Object... args) {
				switch (level) {
				case TRACE:
					log.trace(MessageFormat.format(message, args), exception);
					break;
				case INFO:
					log.info(MessageFormat.format(message, args), exception);
					break;
				case ERROR:
					log.error(MessageFormat.format(message, args), exception);
					break;
				case DEBUG:
					log.debug(MessageFormat.format(message, args), exception);
					break;
				case WARN:
					log.warn(MessageFormat.format(message, args), exception);
					break;
				default:
					break;
				}
			}

			@Override
			public void log(Level level, String message, Object... args) {
				switch (level) {
				case INFO:
					log.info(MessageFormat.format(message, args));
					break;
				case ERROR:
					log.error(MessageFormat.format(message, args));
					break;
				case DEBUG:
					log.debug(MessageFormat.format(message, args));
					break;
				case TRACE:
					log.trace(MessageFormat.format(message, args));
					break;
				case WARN:
					log.warn(MessageFormat.format(message, args));
					break;
				default:
					break;
				}
			}

			@Override
			public boolean isLevelEnabled(Level required) {
				return true;
			}
		});
		log = LoggerFactory.getLogger(AbstractConnectionTest.class);
	}
	protected SshConfiguration configuration;
	protected Map<String, List<Long>> times = new HashMap<>();
	protected Map<String, Throwable> exceptions = new HashMap<>();
	protected Map<SshProvider, Long> connectionTime = Collections.synchronizedMap(new HashMap<>());
	protected int iterations;

	public AbstractConnectionTest() throws IOException {
		this("ConnectionTest");
	}

	public AbstractConnectionTest(String propertyPrefix) throws IOException {
		configuration = new SshConfiguration();
		configuration.setHostKeyValidator(new DumbHostKeyValidator());
		iterations = Integer.parseInt(PROPERTIES.getProperty("iterations", "100"));
		if (PROPERTIES.getProperty("fair", "true").equals("true")) {
			configuration.setPreferredClientToServerCipher(PROPERTIES.getProperty("cipher.cs", "aes256-ctr"));
			configuration.setPreferredServerToClientCipher(PROPERTIES.getProperty("cipher.sc", "aes256-ctr"));
			configuration.setPreferredClientToServerMAC(PROPERTIES.getProperty("mac", "hmac-sha1"));
			configuration.setPreferredServerToClientMAC(PROPERTIES.getProperty("mac", "hmac-sha1"));
			configuration.setPreferredClientToServerCompression(PROPERTIES.getProperty("compress", "none"));
			configuration.setPreferredServerToClientCompression(PROPERTIES.getProperty("compress", "none"));
			configuration.setPreferredKeyExchange(PROPERTIES.getProperty("kex", "diffie-hellman-group14-sha1"));
			configuration.setPreferredPublicKey(PROPERTIES.getProperty("pubkey", SshConfiguration.PUBLIC_KEY_SSHRSA));
			configuration.setSftpPacketSize(Long.parseLong(PROPERTIES.getProperty("sftppacketsize", "16384")));
			configuration.setSftpWindowSize(Long.parseLong(PROPERTIES.getProperty("sftpwindowsize", "32768")));
			configuration.setSftpWindowSizeMax(Long.parseLong(PROPERTIES.getProperty("sftpmaxwindowsize", "10485760")));
		}

	}

	public SshProvider[] getAllConfiguredProviders() {
		List<String> include = parseList(PROPERTIES.getProperty("include", ""));
		List<String> exclude = parseList(PROPERTIES.getProperty("exclude", ""));
		List<SshProvider> providers = new ArrayList<>();
		SshProvider[] alLProviders = DefaultProviderFactory.getAllProviders();
		for (int i = 0; i < alLProviders.length; i++) {
			SshProvider provider = alLProviders[i];
			if (!exclude.contains(provider.getName())
					&& (include.size() == 0 || include.contains(provider.getName()))) {
				providers.add(provider);
			}
		}
		SshProvider[] array = (SshProvider[]) providers.toArray(new SshProvider[0]);
		if (array.length == 0)
			throw new IllegalStateException("No providers.");
		return array;
	}

	private List<String> parseList(String list) {
		list = list.trim();
		if (list.equals("")) {
			return new ArrayList<>();
		}
		return Arrays.asList(list.split(","));
	}

	public void start() throws Exception {

		System.in.read();
		int repeats = Integer.parseInt(PROPERTIES.getProperty("repeats", "10"));
		int warmUps = Integer.parseInt(PROPERTIES.getProperty("warmUps", "1"));
		LOG.info("Warming up");
		for (int i = 0; i < warmUps; i++) {
			singleRun();
		}
		LOG.info("Warmed up, starting actual run of {0} runs for each provider", repeats);
		resetStats();
		for (int i = 0; i < repeats; i++) {
			singleRun();
		}
		SshProvider[] providers = getAllConfiguredProviders();
		System.out.println();
		System.out.println("Test Properties :-");
		Properties p = new Properties(PROPERTIES);
		p.remove("password");
		p.list(System.out);
		System.out.println();
		System.out.println("Test Results:-");
		for (int i = 0; i < providers.length; i++) {
			SshProvider provider = providers[i];
			List<Long> providerTimes = times.get(provider.getName());
			StringBuilder sb = new StringBuilder();
			try (Formatter formatter = new Formatter(sb)) {
				if (providerTimes == null) {
					Throwable t = exceptions.get(provider.getName());
					formatter.format("%20s Failed. %s", new Object[] { provider.getName(),
							t == null || t.getMessage() == null ? "" : t.getMessage() });
				} else {
					long total = 0;
					for (Long t : providerTimes)
						total += t;
					long avg = total / providerTimes.size();
					long contime = connectionTime.get(provider);
					formatter.format(
							"%20s Total: %6d  Avg per run: %6d    Data Only: %6d  Data Only Avg per run: %6d    Connection time: %6d   Connection Avg: %6d",
							new Object[] { provider.getName(), new Long(total), new Long(avg),
									new Long(total - contime), new Long(total - contime) / providerTimes.size(),
									contime, contime / repeats });
				}
			}
			System.out.println(sb);
		}
	}

	void singleRun() throws Exception {
		SshProvider[] providers = getAllConfiguredProviders();
		for (int i = 0; i < providers.length; i++) {
			long started = System.currentTimeMillis();
			SshProvider provider = providers[i];
			try {
				doProvider(provider);
				long finished = System.currentTimeMillis();
				List<Long> providerTimes = times.get(provider.getName());
				if (providerTimes == null) {
					providerTimes = new ArrayList<>();
					times.put(provider.getName(), providerTimes);
				}
				providerTimes.add(new Long(finished - started));
			} catch (Exception e) {
				log.error(String.format("Provider %s failed.", provider.getName()), e);
				exceptions.put(provider.getName(), e);
			}
		}
	}

	void resetStats() {
		connectionTime.clear();
		times.clear();
	}

	protected void time(SshProvider provider, Runnable runnable) throws Exception {
		long started = System.currentTimeMillis();
		try {
			runnable.run();
		} catch (Error e) {
			if (e.getCause() != null && e.getCause() instanceof Exception) {
				throw (Exception) e.getCause();
			} else {
				throw e;
			}
		}
		long finished = System.currentTimeMillis();
		List<Long> providerTimes = times.get(provider.getName());
		if (providerTimes == null) {
			providerTimes = new ArrayList<>();
			times.put(provider.getName(), providerTimes);
		}
		providerTimes.add(new Long(finished - started));
	}

	protected void doProvider(SshProvider provider) throws Exception {
		long started = System.currentTimeMillis();
		try (SshClient client = connect(provider)) {
			time(provider, new Runnable() {
				public void run() {
					try {
						long connectionTook = System.currentTimeMillis() - started;
						synchronized (connectionTime) {
							connectionTime.put(provider, connectionTime.getOrDefault(provider, 0l) + connectionTook);
						}
						doConnection(client);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			});
			log.info(String.format("Provider %s finished.", provider.getName()));
		}
	}

	protected final SshClient connect(SshProvider provider) throws SshException {
		LOG.info("\tProvider {0}", provider.getName());
		String password = PROPERTIES.getProperty("password");
		if (password == null) {
			throw new IllegalArgumentException("Password must be provided in properties file.");
		}
		try {
			return provider.open(configuration, PROPERTIES.getProperty("username", System.getProperty("user.name")),
					PROPERTIES.getProperty("hostname", "localhost"),
					Integer.parseInt(PROPERTIES.getProperty("port", "22")),
					// new SimpleKeyboardInteractiveAuthenticator(password)
					new SimplePasswordAuthenticator(password.toCharArray()));
		} catch (SshException sshe) {
			throw sshe;
		}
	}

	protected void doConnection(SshClient client) throws Exception {
		// For sub-classes to do something useful with a client
	}
}
