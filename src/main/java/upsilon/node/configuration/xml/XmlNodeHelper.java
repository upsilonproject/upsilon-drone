package upsilon.node.configuration.xml;

import org.w3c.dom.Node;
import org.w3c.dom.Text;

import upsilon.node.configuration.abstractDom.ConfigurationNode;

class XmlNodeHelper extends ConfigurationNode<Node> {
	private final Node node;

	private String source = "";

	public XmlNodeHelper(final Node node) {
		super(node);
		this.node = node;
	}

	@Override
	public ConfigurationNode<Node>.ConfigurationNodeCollection getAttributes() {
		final ConfigurationNodeCollection attrs = new ConfigurationNodeCollection();

		for (int i = 0; i < this.node.getAttributes().getLength(); i++) {
			final Node n = this.node.getAttributes().item(i);

			attrs.add(new XmlNodeHelper(n));
		}

		return attrs;
	}

	@Override
	public String getAttributeValueUnchecked(final String string) {
		return this.node.getAttributes().getNamedItem(string).getNodeValue();
	}

	@Override
	public ConfigurationNode<Node>.ConfigurationNodeCollection getChildNodes() {
		final ConfigurationNodeCollection col = new ConfigurationNodeCollection();

		for (int i = 0; i < this.node.getChildNodes().getLength(); i++) {
			final Node child = this.node.getChildNodes().item(i);

			if (child instanceof Text) {
				continue;
			}

			col.add(new XmlNodeHelper(child));
		}

		return col;
	}

	@Override
	public String getName() {
		return this.node.getNodeName();
	}

	@Override
	public String getNodeName() {
		return this.node.getNodeName();
	}

	@Override
	public String getNodeValue() {
		return this.node.getFirstChild().getNodeValue();
	}

	@Override
	public String getSource() {
		return this.source;
	}

	@Override
	public boolean hasAttribute(final String string) {
		return this.node.getAttributes().getNamedItem(string) != null;
	}

	@Override
	public boolean hasChildElement(final String string) {
		return this.getFirstChildElement(string) != null;
	}

	@Override
	public void setSource(final String source) {
		if (source == null) {
			throw new IllegalArgumentException();
		}

		this.source = source;
	}

	@Override
	public String toString() {
		return this.node.toString();
	}
}
