package upsilon.node;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.LogManager;

import javax.xml.bind.Validator;

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
import upsilon.node.management.amqp.DaemonConnectionHandler;
import upsilon.node.util.DirectoryWatcher;
import upsilon.node.util.ResourceResolver;
import upsilon.node.util.SslUtil;
import upsilon.node.util.UPath;

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
					Main.LOG.warn("buildid file could not be got as a stream.");
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
		if ((db == null) && !peers.isEmpty()) {
			return "service-node";
		}

		if ((db != null) && peers.isEmpty()) {
			return "super-node";
		}

		if ((db == null) && peers.isEmpty()) {
			return "useless-testing-node";
		}

		return "non-standard-node";
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
		final Thread t = new Thread(r, r.getClass().getSimpleName());
		this.daemons.add(r);

		t.start();
		t.setUncaughtExceptionHandler(this);
		Thread.setDefaultUncaughtExceptionHandler(this);
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

		XmlConfigurationValidator validator = null;
		
		try {
			final UPath mainConfigPath = new UPath(ResourceResolver.getInstance().getConfigDir(), "config.xml");
			validator = new XmlConfigurationValidator(mainConfigPath, false);
			
			Main.configurationLoader.load(validator.getValidatedConfiguration(), true);
		} catch (final Exception e) {
			Main.configurationLoader.stopFileWatchers();
			
			for (SAXParseException parseError : validator.getParseErrors()) {
				Main.LOG.error(parseError.toString()); 
			} 
			
			Main.LOG.error("Could not parse the initial configuration file. Upsilon cannot ever have a good configuration if it does not start off with a good configuration. Exiting.");
			return;
		}

		if (Configuration.instance.daemonRestEnabled) {
			this.startDaemon(new DaemonRest());
		}

		if (Configuration.instance.daemonAmqpEnabled) {
			this.startDaemon(DaemonConnectionHandler.instance);
			this.startDaemon(new DaemonAmqpHeatbeater());
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
