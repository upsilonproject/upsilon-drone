package upnTests.configurationTests;

import java.io.File;
import java.net.MalformedURLException;

import org.junit.Test;

import junit.framework.Assert;
import upsilon.node.configuration.abstractDom.ConfigurationNode;
import upsilon.node.configuration.xml.XmlConfigurationValidator;
import upsilon.node.dataStructures.StructureService;
import upsilon.node.util.UPath;

public class TestCreateConfigNodesFromString {
	@Test
	public void testCreateService() throws MalformedURLException, Exception {
		final File f = new File("src/test/resources/fragments/service.xml");

		final XmlConfigurationValidator validator = new XmlConfigurationValidator(new UPath(f), true);
		validator.parse();

		System.out.println(validator.getParseErrors());
		Assert.assertFalse(validator.hasErrors());

		final ConfigurationNode<?> cnConfig = validator.getValidatedConfiguration().getRoot();

		Assert.assertNotNull(cnConfig);
		Assert.assertEquals("config", cnConfig.getName());

		System.out.println(cnConfig.getChildNodes());

		Assert.assertEquals(1, cnConfig.getChildNodes().size());

		final ConfigurationNode<?> cnService = cnConfig.getChildNodes().firstElement();

		Assert.assertEquals("service", cnService.getName());

		final StructureService structureService = new StructureService();
		structureService.update(cnService);

		Assert.assertNotNull(structureService);
		Assert.assertEquals("foo", structureService.getIdentifier());
	}
}
