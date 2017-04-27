package upsilon.node;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.security.Security;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.LogManager;

import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.xml.sax.SAXParseException;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import upsilon.node.configuration.FileChangeWatcher;
import upsilon.node.configuration.xml.XmlConfigurationLoader;
import upsilon.node.configuration.xml.XmlConfigurationValidator;
import upsilon.node.dataStructures.CollectionOfStructures;
import upsilon.node.dataStructures.StructureNode;
import upsilon.node.dataStructures.StructurePeer;
import upsilon.node.management.amqp.DaemonAmqpHeatbeater;
import upsilon.node.management.amqp.DaemonAmqpServiceUpdater;
import upsilon.node.management.amqp.DaemonConnectionHandler;
import upsilon.node.util.DirectoryWatcher;
import upsilon.node.util.ResourceResolver;
import upsilon.node.util.SslUtil;
import upsilon.node.util.UPath;
import upsilon.node.util.Util;

public class Main implements UncaughtExceptionHandler {
	public static final Main instance = new Main();
	private static File configurationOverridePath;
	private static String releaseVersion;

	private static final XmlConfigurationLoader configurationLoader = new XmlConfigurationLoader();

	private static transient final Logger LOG = (Logger) LoggerFactory.getLogger(Main.class);

	public static File getConfigurationOverridePath() {
		return Main.configurationOverridePath;
	}

	public static String getVersion() {
		if (Main.releaseVersion == null) {
			Main.releaseVersion = Main.class.getPackage().getImplementationVersion();

			try {
				final Properties props = new Properties();
				final InputStream buildIdFile = Main.class.getResourceAsStream("/.buildid");

				if (buildIdFile == null) { 
					Main.LOG.debug("buildid file could not be got as a stream.");
				} else {
					props.load(buildIdFile);
					Main.releaseVersion = props.getProperty("tag");
				}
			} catch (IOException | NullPointerException e) {
				Main.LOG.warn("Could not get release version from jar.", e);
			}

			if ((Main.releaseVersion == null) || Main.releaseVersion.isEmpty()) {
				Main.releaseVersion = "?";
			}
		}

		return Main.releaseVersion;
	}

	public static void main(final String[] args) throws Exception {
		if (args.length > 0) {
			Main.configurationOverridePath = new File(args[0]);
		}

		Main.instance.startup();
	}

	private void setupDnsCaching() {
		Main.LOG.info("Before dns setup; networkaddress.cache[.negative].ttl = " + Security.getProperty("networkaddress.cache.ttl") + " / " + Security.getProperty("networkaddress.cache.negative.ttl"));

        Security.setProperty("networkaddress.cache.ttl" , "60");   
        Security.setProperty("networkaddress.cache.negative.ttl" , "60");
		
		Main.LOG.info("After dns setup; networkaddress.cache[.negative].ttl = " + Security.getProperty("networkaddress.cache.ttl") + " / " + Security.getProperty("networkaddress.cache.negative.ttl"));
	}

	private static void setupLogging() {
		LogManager.getLogManager().getLogger("").setLevel(Level.FINEST);
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		final File loggingConfiguration = new File(ResourceResolver.getInstance().getConfigDir(), "logging.xml");

		try {
			if (loggingConfiguration.exists()) {
				for (final Logger logger : ((LoggerContext) LoggerFactory.getILoggerFactory()).getLoggerList()) {
					logger.detachAndStopAllAppenders();
				}

				final JoranConfigurator loggerConfigurator = new JoranConfigurator();
				loggerConfigurator.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
				loggerConfigurator.doConfigure(loggingConfiguration);

				Main.LOG.debug("Logging override configuration exists, parsing: " + loggingConfiguration.getAbsolutePath());
			}
		} catch (final Exception e) {
			Main.LOG.warn("Could not set up logging config.", e);
		}
	}

	public final StructureNode node = new StructureNode();

	private final Vector<Daemon> daemons = new Vector<Daemon>();

	public DaemonScheduler queueMaintainer;

	public XmlConfigurationLoader getConfigurationLoader() {
		return Main.configurationLoader;
	}

	public Vector<Daemon> getDaemons() {
		return this.daemons;
	}

	public String guessNodeType() {
		return this.guessNodeType(Database.instance, Configuration.instance.peers);
	}

	public String guessNodeType(final Database db, final CollectionOfStructures<StructurePeer> peers) {
		Vector<String> elements = new Vector<>();

		if (Configuration.instance.daemonAmqpEnabled) {
			elements.add("amqp");
		}

		if (Configuration.instance.daemonRestEnabled) {
			elements.add("rest");
		}

		if (db != null) {
			elements.add("db");
		}

		if (!peers.isEmpty()) {
			elements.add("peers");
		}

		return String.join(", ", elements);
	}

	public void shutdown() {
		for (final Daemon t : this.daemons) {
			t.stop();
		}

		if (Database.instance != null) {
			try {
				Database.instance.disconnect();
			} catch (final Exception e) {
				Main.LOG.error("SQL Error during disconnect: " + e.getMessage());
			}
		}

		DirectoryWatcher.stopAll();
		RobustProcessExecutor.executingThreadPool.shutdown();
		RobustProcessExecutor.monitoringThreadPool.shutdown();
		FileChangeWatcher.stopAll();

		Main.LOG.warn("All daemons have been requested to stop. Main application should now shutdown.");
	}

	private void startDaemon(final Daemon r) {
		String daemonName = r.getClass().getSimpleName();
		Main.LOG.info("Starting daemon: " + daemonName);

		final Thread t = new Thread(r, daemonName);
		this.daemons.add(r);

		t.start();
		t.setUncaughtExceptionHandler(this);
		Thread.setDefaultUncaughtExceptionHandler(this);
	}

	private void parseInitialConfiguration() throws Exception {
		XmlConfigurationValidator validator = null;

		try {
			final UPath mainConfigPath = new UPath(ResourceResolver.getInstance().getConfigDir(), "config.xml");

			if (!mainConfigPath.exists()) {
				Main.LOG.info("Configuration file does not exist, configuration will only be possible via AMQP. ");
				return;
			}

			validator = new XmlConfigurationValidator(mainConfigPath, false);
			
			Main.configurationLoader.load("config.xml", validator.getValidatedConfiguration(), true);
		} catch (final Exception e) {
			Main.configurationLoader.stopFileWatchers();

			if (validator != null) {
				for (SAXParseException parseError : validator.getParseErrors()) {
					Main.LOG.error(parseError.toString()); 
				} 
			}
			
			throw new Exception("Could not parse the initial configuration file. Upsilon cannot ever have a good configuration if it does not start off with a good configuration. Exiting.");
		}
	}

	private void parseStartupDirectory(String startupConfigDirectoryName) {
		try {
			UPath startupConfigDirectory = new UPath(ResourceResolver.getInstance().getConfigDir(), startupConfigDirectoryName);

			if (startupConfigDirectory.exists()) {
				new DirectoryWatcher(startupConfigDirectory, Main.configurationLoader);
			}
		} catch (Exception e) {
			Main.LOG.warn("Could not start monitoring startup config directory", e);
		}
	}
	
	private void parseConfigurationEnvironmentVariables() {
		if (!Util.isBlank(System.getenv("UPSILON_CONFIG_SYSTEM_AMQPHOST"))) {
			Main.LOG.info("Setting AMQPHost from ENV: " + System.getenv("UPSILON_CONFIG_SYSTEM_AMQPHOST"));
			Configuration.instance.amqpHostname = System.getenv("UPSILON_CONFIG_SYSTEM_AMQPHOST");
		}
	}

	private void startup() throws Exception {
		Main.setupLogging();
		SslUtil.init();

		this.node.refresh();

		Main.LOG.info("Upsilon " + Main.getVersion());
		Main.LOG.info("----------");
		Main.LOG.info("Identifier: " + this.node.getIdentifier());
		Main.LOG.trace("CP: " + System.getProperty("java.class.path"));
		Main.LOG.trace("OS: " + System.getProperty("os.name"));

		this.parseConfigurationEnvironmentVariables();
		this.parseInitialConfiguration();
		this.parseStartupDirectory("includes.d/");
		this.parseStartupDirectory("remotes.d/");
		
		this.setupDnsCaching();

		if (Configuration.instance.daemonRestEnabled) {
			this.startDaemon(new DaemonRest());
		}

		if (Configuration.instance.daemonAmqpEnabled) {
			this.startDaemon(DaemonConnectionHandler.instance);
			this.startDaemon(new DaemonAmqpHeatbeater());
			this.startDaemon(new DaemonAmqpServiceUpdater());  
		}

		this.startDaemon(new DaemonScheduler());

		Main.LOG.debug("Best guess at node type: " + this.guessNodeType());
	}

	@Override
	public void uncaughtException(final Thread t, final Throwable e) {
		Main.LOG.error("Exception on a critical thread [" + t.getName() + "], will now shutdown.", e);
		this.shutdown();
	}
}
