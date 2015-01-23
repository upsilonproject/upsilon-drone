package upsilon.dataStructures;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import upsilon.Configuration;
import upsilon.Main;
import upsilon.RobustProcessExecutor;
import upsilon.util.ResourceResolver;

@XmlRootElement
public class StructureNode extends ConfigStructure {
	private static final transient Logger LOG = LoggerFactory.getLogger(StructureNode.class);
	
	private String type = "???";
	private int serviceCount;
	private String identifier = "unidentifiedNode";
	private String instanceApplicationVersion = "???";

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

	public void refresh() {
		this.refreshType();
		this.refreshIdentifier();
		this.instanceApplicationVersion = Main.getVersion();
		this.setServiceCount(Configuration.instance.services.size());
		this.setDatabaseUpdateRequired(true);
		this.setPeerUpdateRequired(true);
	}
	
	private void regenerateLocalIdentifierFile(File identifierFile, String newIdentifier) {
		try {
			identifierFile.createNewFile();
			
			FileWriter writer = new FileWriter(identifierFile);
			writer.write(newIdentifier);
			writer.close();
		} catch (Exception e ) { 
			LOG.error("Could not save identifier to identity file: " + identifierFile.getAbsolutePath() + ". This identity will not persist across restarts.", e);
		}
	}

	private void refreshIdentifier() {
		String newIdentifier = "unknownIdentifier";
		
		try {
			File configDir = ResourceResolver.getInstance().getConfigDir();
			File identifierFile = new File(configDir, "indentifier.txt");
			
			if (!identifierFile.exists()) {
				newIdentifier = UUID.randomUUID().toString();
				regenerateLocalIdentifierFile(identifierFile, newIdentifier);
			} else {
				BufferedReader reader = new BufferedReader(new FileReader(identifierFile));
				newIdentifier = reader.readLine().trim();
				reader.close();
			}
		} catch (IOException e) {
			LOG.error("Could not get a valid identifier for this node, using 'unknownIdentifier': ", e); 
		} 

		if (!this.identifier.equals(newIdentifier)) {
			this.setDatabaseUpdateRequired(true);
			this.identifier = newIdentifier;
		}
	}

	private void refreshType() {
		String newType = Main.instance.guessNodeType();

		if (this.type.equals(newType)) {
			return;
		} else {
			this.type = newType;
			this.setPeerUpdateRequired(true);
		}
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public void setInstanceApplicationVersion(String versionString) {
		this.instanceApplicationVersion = versionString;
	}

	public void setServiceCount(int serviceCount) {
		if (this.serviceCount != serviceCount) {
			this.serviceCount = serviceCount;

			this.setDatabaseUpdateRequired(true);
			this.setPeerUpdateRequired(true);
		}
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return String.format("%s = {identifier: %s, type: %s, service count: %s}", this.getClass().getSimpleName(), this.getIdentifier(), this.type, this.getServiceCount());
	}
}
