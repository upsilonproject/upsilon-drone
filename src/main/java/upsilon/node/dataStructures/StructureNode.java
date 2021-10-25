package upsilon.node.dataStructures;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import upsilon.node.Configuration;
import upsilon.node.Main;
import upsilon.node.util.ResourceResolver;
import upsilon.node.util.Util;

@XmlRootElement
public class StructureNode extends ConfigStructure {
	private static final transient Logger LOG = LoggerFactory.getLogger(StructureNode.class);

	private String type = "???";
	private int serviceCount;
	private String identifier = "unidentifiedNode";
	private String instanceApplicationVersion = "???";
	private String configs = "";

	private boolean triedCreatingIdentifierFile = false;

	@Override
	@XmlElement
	public String getIdentifier() {
		return this.identifier;
	}

	@XmlAttribute
	public String getInstanceApplicationVersion() {
		return this.instanceApplicationVersion;
	}

	@XmlAttribute
	public int getServiceCount() {
		return this.serviceCount;
	}

	@XmlAttribute
	public String getType() {
		return this.type;
	}

	@XmlAttribute
	public String getConfigs() {
		return this.configs;
	}

	public void refresh() {
		this.refreshType();
		this.refreshIdentifier();
		this.instanceApplicationVersion = Main.getVersion();
		this.setServiceCount(Configuration.instance.services.size());
		this.setDatabaseUpdateRequired(true);
		this.setPeerUpdateRequired(true);
	}

	private void refreshIdentifier() {
		String newIdentifier = "unknownIdentifier";

		if (!Util.isBlank(System.getenv("UPSILON_IDENTIFIER"))) {
			newIdentifier = System.getenv("UPSILON_IDENTIFIER");
		} else {
			try {
				final File configDir = ResourceResolver.getInstance().getConfigDir();
				final File identifierFile = new File(configDir, "identifier.txt");

				if (!identifierFile.exists() && !this.triedCreatingIdentifierFile) {
					this.triedCreatingIdentifierFile = true;

					newIdentifier = UUID.randomUUID().toString();
					this.regenerateLocalIdentifierFile(identifierFile, newIdentifier);
				} else {
					final BufferedReader reader = new BufferedReader(new FileReader(identifierFile));
					newIdentifier = reader.readLine().trim();
					reader.close();
				}
			} catch (final IOException e) {
				StructureNode.LOG.error("Could not get a valid identifier for this node, using 'unknownIdentifier': ", e);
			}
		}

		if (!this.identifier.equals(newIdentifier)) {
			this.identifier = newIdentifier;
			this.setDatabaseUpdateRequired(true);
		}
	}

	private void refreshType() {
		final String newType = Main.instance.guessNodeType();

		if (this.type.equals(newType)) {
			return;
		} else {
			this.type = newType;
			this.setPeerUpdateRequired(true);
		}
	}

	private void regenerateLocalIdentifierFile(final File identifierFile, final String newIdentifier) {
		try {
			identifierFile.createNewFile();

			final FileWriter writer = new FileWriter(identifierFile);
			writer.write(newIdentifier);
			writer.close();
		} catch (final Exception e) {
			StructureNode.LOG.warn("Could not save identifier to identity file: " + identifierFile.getAbsolutePath() + ". This identity will not persist across restarts.", e);
		}
	}

	public void setIdentifier(final String identifier) {
		this.identifier = identifier;
	}

	public void setInstanceApplicationVersion(final String versionString) {
		this.instanceApplicationVersion = versionString;
	}

	public void setServiceCount(final int serviceCount) {
		if (this.serviceCount != serviceCount) {
			this.serviceCount = serviceCount;

			this.setDatabaseUpdateRequired(true);
			this.setPeerUpdateRequired(true);
		}
	}

	public void setType(final String type) {
		this.type = type;
	}

	public void setConfigs(final String configs) {
		this.configs = configs;
	}

	@Override
	public String toString() {
		return String.format("%s = {identifier: %s, type: %s, service count: %s}", this.getClass().getSimpleName(), this.getIdentifier(), this.type, this.getServiceCount());
	}
}
