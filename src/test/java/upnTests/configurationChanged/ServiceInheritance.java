package upnTests.configurationChanged;

import org.joda.time.Duration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import junit.framework.Assert;
import upsilon.node.Configuration;
import upsilon.node.configuration.xml.XmlConfigurationLoader;
import upsilon.node.configuration.xml.XmlConfigurationValidator;
import upsilon.node.dataStructures.StructureService;
import upsilon.node.util.GlobalConstants;
import upsilon.node.util.UPath;

public class ServiceInheritance {
	@BeforeClass
	@AfterClass
	public static void clearConfig() {
		Configuration.instance.clear();
	}

	@Test
	public void testConfiguration() throws Exception {
		final UPath before = new UPath("src/test/resources/fragments/serviceInheritance/config.xml");
		final XmlConfigurationValidator validator = new XmlConfigurationValidator(before);
		final XmlConfigurationLoader loader = new XmlConfigurationLoader();
		loader.load(validator.getValidatedConfiguration(), false);

		Assert.assertEquals(validator.getSourcePath(), before);
		Assert.assertTrue(validator.isParseClean());

		Assert.assertTrue(Configuration.instance.services.containsId("baseService"));
		final StructureService baseService = Configuration.instance.services.get("baseService");

		Assert.assertEquals("baseService", baseService.getIdentifier());
		Assert.assertNotSame(GlobalConstants.DEF_TIMEOUT, baseService.getTimeout());
		Assert.assertEquals(Duration.standardSeconds(5), baseService.getTimeout());

		Assert.assertTrue(Configuration.instance.services.containsId("childService"));
		final StructureService childService = Configuration.instance.services.get("childService");
		Assert.assertEquals(2, Configuration.instance.services.size());

		Assert.assertEquals(Duration.standardSeconds(5), childService.getTimeout());
	}
}
