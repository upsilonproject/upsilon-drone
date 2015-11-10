package upsilon.node.configuration.xml;

import java.io.StringReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Vector;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import upsilon.node.configuration.ConfigurationValidator;
import upsilon.node.configuration.ValidatedConfiguration;
import upsilon.node.util.ResourceResolver;
import upsilon.node.util.UPath;

public class XmlConfigurationValidator implements ErrorHandler, ConfigurationValidator {
	private static final Logger LOG = LoggerFactory.getLogger(XmlConfigurationValidator.class);

	static {
		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
	}

	private final Vector<SAXParseException> parseErrors = new Vector<SAXParseException>();
	private final DocumentBuilder builder;

	private boolean isParsed = false;

	private boolean isAux = false;

	private final InputSource is;

	private final String source;
	private UPath sourcePath;

	private ValidatedConfiguration vcfg = null;

	public XmlConfigurationValidator(final String xmlInput, final boolean isAux) throws Exception {
		this.isAux = isAux;

		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setSchema(this.selectSchema());

		this.builder = dbf.newDocumentBuilder();
		this.builder.setErrorHandler(this);
		this.is = new InputSource(new StringReader(xmlInput));

		this.source = "input";
	}

	public XmlConfigurationValidator(final UPath path) throws Exception {
		this(path, false);
	}

	public XmlConfigurationValidator(final UPath path, final boolean isAux) throws Exception {
		this.isAux = isAux;

		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setSchema(this.selectSchema());

		if (!path.exists()) {
			XmlConfigurationValidator.LOG.warn("Wont parse non existant configuration file: " + path);
		} else if (!path.isFile()) {
			XmlConfigurationValidator.LOG.warn("Wont parse thing on filesystem, it does not look like a file: " + path);
		}

		this.builder = dbf.newDocumentBuilder();
		this.builder.setErrorHandler(this);

		this.is = new InputSource(path.getInputStream());

		this.source = path.toString();
		this.sourcePath = path;
	}

	@Override
	public void error(final SAXParseException exception) throws SAXException {
		this.parseErrors.add(exception);
	}

	@Override
	public void fatalError(final SAXParseException exception) throws SAXException {
		this.parseErrors.add(exception);
	}

	public Vector<SAXParseException> getParseErrors() throws Exception {
		if (!this.isParsed) {
			throw new Exception("Parse not attempted, so cannot get parse errors.");
		}

		return this.parseErrors;
	}

	public String getSource() {
		return this.source;
	}

	public UPath getSourcePath() {
		return this.sourcePath;
	}

	public ValidatedConfiguration getValidatedConfiguration() throws Exception {
		if (!this.isParsed) {
			this.parse();

			if (this.hasErrors()) {
				throw new Exception("Cannot get ValidatedConfiguration because it has parse errors.");
			}
		}

		return this.vcfg;
	}

	public boolean hasErrors() throws Exception {
		return !this.getParseErrors().isEmpty();
	}

	public boolean isAux() {
		return this.isAux;
	}

	public boolean isParseClean() throws Exception {
		if (!this.isParsed) {
			throw new Exception("Parse not even attempted");
		}

		return this.parseErrors.isEmpty();
	}

	public boolean isParsed() {
		return this.isParsed;
	}

	@Override
	public void parse() throws Exception {
		if (this.isParsed) {
			throw new Exception("Cannot parse the configuration twice. ");
		}

		final Document doc = this.builder.parse(this.is);

		this.isParsed = true;

		ValidatedConfiguration vcfg;

		if (this.sourcePath == null) {
			vcfg = new ValidatedConfiguration(doc);
		} else {
			vcfg = new ValidatedConfiguration(doc, this.sourcePath);
		}

		vcfg.setRoot(new XmlNodeHelper(doc.getFirstChild()));

		this.vcfg = vcfg;

	}

	private Schema selectSchema() throws Exception {
		StreamSource source;

		if (this.isAux) {
			source = new StreamSource(ResourceResolver.getInstance().getInternalFromFilename("upsilon.aux.xsd"));
		} else {
			source = new StreamSource(ResourceResolver.getInstance().getInternalFromFilename("upsilon.xsd"));
		}

		return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(source);
	}

	@Override
	public void warning(final SAXParseException exception) throws SAXException {
		this.parseErrors.add(exception);
	}
}
