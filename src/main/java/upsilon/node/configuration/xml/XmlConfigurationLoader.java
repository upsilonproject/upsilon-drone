package upsilon.node.configuration.xml;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import upsilon.node.Configuration;
import upsilon.node.configuration.CollectionAlterationTransaction;
import upsilon.node.configuration.FileChangeWatcher;
import upsilon.node.configuration.ValidatedConfiguration;
import upsilon.node.dataStructures.CollectionOfStructures;
import upsilon.node.dataStructures.ConfigStructure;
import upsilon.node.util.DirectoryWatcher;
import upsilon.node.util.UPath;
import upsilon.node.util.Util;

public class XmlConfigurationLoader implements DirectoryWatcher.Listener, FileChangeWatcher.Listener {
	@XmlRootElement(name = "config")
	public static class ConfigStatus {
		private final String sourceTag;

		@XmlElement
		public boolean isParsed = false;

		@XmlElement
		public boolean isParseClean = false;

		@XmlElement
		public boolean isAux = false;

		private String remoteId = "unset";

		@XmlElementWrapper(name = "parseErrors")
		@XmlElement(name = "error")
		private final Vector<String> stringParseErrors = new Vector<String>();

		private final Instant lastParsed = Instant.now();

		public ConfigStatus() {
			this.sourceTag = "?";
		}

		public ConfigStatus(final String sourceTag) {
			this.sourceTag = sourceTag;
		}

		public void clearParseErrors() {
			this.stringParseErrors.clear();
		}

		@XmlElement
		public String getLastParsed() {
			return this.lastParsed.toDateTime().toString();
		}

		@XmlElement
		public String getSourceTag() {
			return this.sourceTag;
		}

		public void setParseErrors(final Vector<SAXParseException> parseErrors) {
			for (final SAXParseException e : parseErrors) {
				this.stringParseErrors.add("Line: " + e.getLineNumber() + " Message: " + e.getMessage());
			}
		}

		public boolean hasErrors() {
			return !this.stringParseErrors.isEmpty();
		}

		public void setRemoteId(String remoteId) {
			this.remoteId = remoteId;
		}

		@XmlElement
		public String getRemoteId() {
			return this.remoteId;
		}

		public String toString() {
			return this.getSourceTag() + ":" + this.getRemoteId() +  ":" + this.lastParsed.toDateTime().getMillis() + ":" + this.hasErrors();
		}
	}

	private static final transient Logger LOG = LoggerFactory.getLogger(XmlConfigurationLoader.class);

	@XmlElement
	public static HashMap<String, ConfigStatus> configStatuses = new HashMap<String, ConfigStatus>();

	private final HashMap<UPath, FileChangeWatcher> fileWatchers = new HashMap<UPath, FileChangeWatcher>();

	private void buildAndRunConfigurationTransaction(final String sourceTag, final String xpath, final CollectionOfStructures<?> col, final Document d) throws XPathExpressionException, JAXBException {
		final CollectionAlterationTransaction<?> cat = col.newTransaction(sourceTag);

		final XPathExpression xpe = XPathFactory.newInstance().newXPath().compile(xpath);
		final NodeList els = (NodeList) xpe.evaluate(d, XPathConstants.NODESET);
		final Vector<XmlNodeHelper> list = this.parseNodelist(els);
		this.parseNodeParents(list);

		for (final XmlNodeHelper node : list) {
			XmlConfigurationLoader.LOG.trace("Node found. xpath: " + xpath + ". name: " + node.getNodeName());
			node.setSource(sourceTag);

			cat.considerFromConfig(node);
		}

		cat.print();
		col.processTransaction(cat);
	}

	@Override
	public void fileChanged(final UPath url, final boolean isAux) {
		try {
			this.load("change", url, false, isAux);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private void findParent(final XmlNodeHelper node, final Vector<XmlNodeHelper> linage, final Vector<XmlNodeHelper> availableNodes) {
		if (node.hasAttribute("parent")) {
			final String parentId = node.getAttributeValueUnchecked("parent");

			for (final XmlNodeHelper search : availableNodes) {
				if (search.getAttributeValueUnchecked("id").equals(parentId)) {
					node.setParent(search);

					this.findParent(search, linage, availableNodes);

					return;
				}
			}
		}
	}

	public ConfigStatus getConfigStatus(final String sourceTag) {
		if (!XmlConfigurationLoader.configStatuses.containsKey(sourceTag)) {
			XmlConfigurationLoader.configStatuses.put(sourceTag, new ConfigStatus(sourceTag));
		}

		return XmlConfigurationLoader.configStatuses.get(sourceTag);
	}

	public Collection<ConfigStatus> getStatuses() {
		return XmlConfigurationLoader.configStatuses.values();
	}

	public String getStatusesString() {
		return getStatuses().toString();
	}

	public FileChangeWatcher load(String remoteId, final UPath path, final boolean watch, final boolean isAux) throws Exception {
		ValidatedConfiguration vcfg = null;
		XmlConfigurationValidator validator = null;

		validator = new XmlConfigurationValidator(path, isAux);
		vcfg = validator.getValidatedConfiguration();

		final ConfigStatus configStatus = this.getConfigStatus(path.getAbsolutePath());
		configStatus.setRemoteId(remoteId);

		XmlConfigurationLoader.LOG.info("Reparse of configuration of file {}. Schema: {}. Validation status: {}", new Object[] { validator.getSource(), Util.bool2s(validator.isAux(), "AUX", "MAIN"), Util.bool2s(validator.isParseClean(), "VALID", "INVALID") });
		configStatus.isAux = validator.isAux();
		configStatus.isParsed = validator.isParsed();
		configStatus.isParseClean = validator.isParseClean();
		configStatus.clearParseErrors();

		if (!validator.isParsed()) {
			XmlConfigurationLoader.LOG.warn("Configuration file could not be loaded for parser: " + validator.getSource());
		} else if (!validator.isParseClean()) {
			XmlConfigurationLoader.LOG.warn("Configuration file has parse {} errors. It will NOT be reloaded: {}", new Object[] { validator.getParseErrors().size(), validator.getSource() });
			configStatus.setParseErrors(validator.getParseErrors());

			for (final SAXParseException e : validator.getParseErrors()) {
				XmlConfigurationLoader.LOG.warn("Configuration file parse error: {}:{} - {}", new Object[] { validator.getSource(), e.getLineNumber(), e.getMessage() });
			}
		}

		return this.load(remoteId, vcfg, watch, isAux);
	}

	public void load(String remoteId, final ValidatedConfiguration validatedConfiguration, final boolean b) {
		this.load(remoteId, validatedConfiguration, b, false);
	}

	public FileChangeWatcher load(String remoteId, final ValidatedConfiguration vcfg, final boolean watch, final boolean isAux) {
		XmlConfigurationLoader.LOG.info("XMLConfigurationLoader is loading file: " + vcfg.getSourceTag());
		FileChangeWatcher fcw = null;

		if (vcfg.isSourceFile() && watch) {
			final UPath path = vcfg.getSourceFile();

			if (fileWatchers.containsKey(path)) {
				fcw = fileWatchers.get(path);
			} else {
				fcw = new FileChangeWatcher(path, this, isAux);
				this.fileWatchers.put(path, fcw);
			} 

			if (watch) {
				fcw.start();
			}
		}

		this.parse(vcfg);

		return fcw;
	}

	@Override
	public void onNewFile(final File f) {
		try {
			this.load("local", new UPath(f), true, true);
		} catch (final Exception e) {
			XmlConfigurationLoader.LOG.warn("Informed of new file in a directory, but the configuration loader encountered an exception: " + e.getMessage());
		}
	}

	public synchronized void parse(final ValidatedConfiguration vcfg) {
		this.getConfigStatus(vcfg.getSourceTag());

		try {
			final Document d = vcfg.getDocument();

			this.parseTrusts(d);
			this.parseSystem(d);
			this.parseIncludedConfiguration(d);
			this.buildAndRunConfigurationTransaction(vcfg.getSourceTag(), "config/command", Configuration.instance.commands, d);
			this.buildAndRunConfigurationTransaction(vcfg.getSourceTag(), "config/service", Configuration.instance.services, d);
			this.buildAndRunConfigurationTransaction(vcfg.getSourceTag(), "config/peer", Configuration.instance.peers, d);
		} catch (final Exception e) {
			XmlConfigurationLoader.LOG.error("Could not reparse configuration: " + e.getMessage(), e);
		}
	}

	private void parseIncludedConfiguration(final Document d) throws XPathExpressionException, MalformedURLException, URISyntaxException {
		final XPathExpression xpe = XPathFactory.newInstance().newXPath().compile("config/include");
		final NodeList nl = (NodeList) xpe.evaluate(d, XPathConstants.NODESET);

		final Vector<XmlNodeHelper> nodes = this.parseNodelist(nl);

		for (final XmlNodeHelper node : nodes) {
			final UPath path = new UPath(node.getAttributeValue("path", ""));

			if (!path.isAbsolute()) {
				XmlConfigurationLoader.LOG.warn("Path is not absolute. Relative paths should not be used: " + path);
			} else {
				if (DirectoryWatcher.canMonitor(path)) {
					new DirectoryWatcher(path, this);
				} else if (FileChangeWatcher.isAlreadyMonitoring(path)) {
					XmlConfigurationLoader.LOG.info("Already monitoring included configuration file: " + path);
				} else {
					XmlConfigurationLoader.LOG.info("Loading included configuration file: " + path);

					try {
						final XmlConfigurationValidator validator = new XmlConfigurationValidator(path, true);
						this.load("config.xml", validator.getValidatedConfiguration(), false, true);
					} catch (final Exception e) {
						XmlConfigurationLoader.LOG.error("Could not parse included configuration:" + e);
					}
				}
			}
		}
	}

	private Vector<XmlNodeHelper> parseNodelist(final NodeList nl) {
		final Vector<XmlNodeHelper> list = new Vector<XmlNodeHelper>();

		for (int i = 0; i < nl.getLength(); i++) {
			list.add(new XmlNodeHelper(nl.item(i)));
		}

		return list;
	}

	private void parseNodeParents(final Vector<XmlNodeHelper> list) {
		final Vector<XmlNodeHelper> linage = new Vector<XmlNodeHelper>();

		for (final XmlNodeHelper node : list) {
			this.findParent(node, linage, list);
			linage.clear();
		}
	}

	private void parseSystem(final Document d) throws XPathExpressionException {
		final XPathExpression xpe = XPathFactory.newInstance().newXPath().compile("config/system");
		final NodeList nl = (NodeList) xpe.evaluate(d, XPathConstants.NODESET);

		if (nl.getLength() == 1) {
			Configuration.instance.update(new XmlNodeHelper(nl.item(0)));
		}
	}

	private void parseTrusts(final Document d) throws XPathExpressionException {
		final XPathExpression xpe = XPathFactory.newInstance().newXPath().compile("config/trust");
		final NodeList nl = (NodeList) xpe.evaluate(d, XPathConstants.NODESET);

		final Vector<XmlNodeHelper> nodes = this.parseNodelist(nl);

		for (final XmlNodeHelper node : nodes) {
			final String fingerprint = node.getAttributeValueUnchecked("certSha1Fingerprint");

			Configuration.instance.parseTrustFingerprint(fingerprint);
		}
	}

	private void replaceSource(final CollectionOfStructures<? extends ConfigStructure> col, final String before, final String after) {
		for (final ConfigStructure struct : col) {
			if (struct.getSource().equals(before.toString())) {
				struct.setSource(after.toString());
			}
		}
	}

	public void replaceSources(final UPath beforePath, final UPath afterPath) {
		final String before = beforePath.getAbsolutePath();
		final String after = afterPath.getAbsolutePath();

		this.replaceSource(Configuration.instance.commands, before, after);
		this.replaceSource(Configuration.instance.peers, before, after);
		this.replaceSource(Configuration.instance.remoteNodes, before, after);
		this.replaceSource(Configuration.instance.services, before, after);
	}

	public void stopFileWatchers() {
		synchronized (this.fileWatchers) {
			for (final FileChangeWatcher fcw : this.fileWatchers.values()) {
				fcw.stop();
			}
		}
	}
}
