package upsilon.node.configuration.abstractDom;

import java.util.Vector;

import org.joda.time.Duration;

public abstract class ConfigurationNode<C> {
	public class ConfigurationNodeCollection extends Vector<ConfigurationNode<C>> {
		private static final long serialVersionUID = 1L;

		public ConfigurationNode<C> getItemAt(final int index) {
			return this.elementAt(index);
		}

		public int getLength() {
			return this.size();
		}

		public ConfigurationNode<C> getNamedItem(final String key) {
			for (final ConfigurationNode<C> node : this) {
				if (node.getName().equals(key)) {
					return node;
				}
			}

			return null;
		}
	}

	protected C node;

	private ConfigurationNode<C> parent;

	public ConfigurationNode(final C node) {
		this.node = node;
	}

	public abstract ConfigurationNodeCollection getAttributes();

	public <T> T getAttributeValue(final String key, final T def) {
		return this.getAttributeValueOrParentOrDefault(key, def);
	}

	@SuppressWarnings("unchecked")
	public <T> T getAttributeValueOrDefault(final String key, final T def) {
		final ConfigurationNode<C> attr = this.getAttributes().getNamedItem(key);

		if (attr == null) {
			return def;
		} else {
			final String value = attr.getNodeValue();

			if (def instanceof Integer) {
				return (T) (Integer) Integer.parseInt(value);
			} else if (def instanceof Boolean) {
				return (T) (Boolean) Boolean.parseBoolean(value);
			} else {
				return (T) attr.getNodeValue();
			}
		}
	}

	public <T> T getAttributeValueOrParentOrDefault(final String key, final T def) {
		if (this.hasAttribute(key)) {
			final String val = this.getAttributeValueUnchecked(key);

			if (def instanceof Duration) {
				return (T) Duration.parse(val);
			} else if (def instanceof Integer) {
				return (T) new Integer(Integer.parseInt(val));
			} else {
				return (T) val;
			}
		} else {
			if (this.parent == null) {
				return def;
			} else {
				return this.parent.getAttributeValueOrParentOrDefault(key, def);
			}
		}
	}

	public String getAttributeValueUnchecked(final String string) {
		return this.getAttributes().getNamedItem(string).getNodeValue();
	}

	public Vector<ConfigurationNode<C>> getChildElements(final String string) {
		final Vector<ConfigurationNode<C>> xmlHelperChildren = new Vector<ConfigurationNode<C>>();
		final ConfigurationNodeCollection children = this.getChildNodes();

		for (final ConfigurationNode<C> n : children) {
			if (n.getNodeName().equals(string)) {
				xmlHelperChildren.add(n);
			}
		}

		return xmlHelperChildren;
	}

	public abstract ConfigurationNodeCollection getChildNodes();

	public ConfigurationNode<C> getFirstChildElement(final String string) {
		for (int i = 0; i < this.getChildNodes().getLength(); i++) {
			final ConfigurationNode<C> n = this.getChildNodes().getItemAt(i);

			if (n.getNodeName().equals(string)) {
				return n;
			}
		}

		return null;
	}

	public abstract String getName();

	public abstract String getNodeName();

	public abstract String getNodeValue();

	public abstract String getSource();

	public boolean hasAttribute(final String string) {
		return this.getAttributes().getNamedItem(string) != null;
	}

	public boolean hasChildElement(final String string) {
		return this.getFirstChildElement(string) != null;
	}

	public void setParent(final ConfigurationNode search) {
		this.parent = search;
	}

	public void setSource(final String source) {
	}
}
