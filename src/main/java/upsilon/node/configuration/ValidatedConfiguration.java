package upsilon.node.configuration;

import org.w3c.dom.Document;

import upsilon.node.configuration.abstractDom.ConfigurationNode;
import upsilon.node.util.UPath;

public class ValidatedConfiguration {
	private final Document document;
	private UPath sourceFile;
	private final String sourceTag;

	public ConfigurationNode<?> root;

	public ValidatedConfiguration(final Document document) {
		this.document = document;
		this.sourceTag = "input";
	}

	public ValidatedConfiguration(final Document document, final UPath source) {
		this.document = document;
		this.sourceFile = source;
		this.sourceTag = source.getAbsolutePath();
	}

	public Document getDocument() {
		return this.document;
	}

	public ConfigurationNode<?> getRoot() {
		return this.root;
	}

	public UPath getSourceFile() {
		return this.sourceFile;
	}

	public String getSourceTag() {
		return this.sourceTag;
	}

	public boolean isSourceFile() {
		return this.sourceFile != null;
	}

	public void setRoot(final ConfigurationNode<?> root) {
		this.root = root;
	}
}
