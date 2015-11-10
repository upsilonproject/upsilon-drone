package upnTests.configurationChanged;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import junit.framework.Assert;
import upsilon.node.Configuration;
import upsilon.node.configuration.FileChangeWatcher;
import upsilon.node.configuration.xml.XmlConfigurationLoader;
import upsilon.node.configuration.xml.XmlConfigurationValidator;

public class BasicReloadTest extends AbstractConfigurationChangeTest {
	@BeforeClass
	@AfterClass
	public static void clearConfig() {
		Configuration.instance.clear();
	}

	public BasicReloadTest() throws Exception {
		super("basicReload");
	}

	@Test
	public void testConfig() throws Exception {
		final XmlConfigurationValidator validator = new XmlConfigurationValidator(this.before, false);
		validator.parse();
		System.out.println(validator.getParseErrors());
		Assert.assertTrue(validator.isParseClean());

		final XmlConfigurationLoader loader = new XmlConfigurationLoader();
		final FileChangeWatcher fcw = loader.load(this.before, true, false);

		Assert.assertTrue(Configuration.instance.services.containsId("baseService"));
		Assert.assertTrue(Configuration.instance.services.containsId("mindstormPing"));
		Assert.assertEquals(Configuration.instance.services.size(), 2);

		loader.replaceSources(this.before, this.after);

		fcw.setWatchedFile(this.after);
		fcw.checkForModification();

		Assert.assertTrue(Configuration.instance.services.containsId("baseService"));
		Assert.assertFalse(Configuration.instance.services.containsId("mindstormPing"));
		Assert.assertEquals(Configuration.instance.services.size(), 1);
	}
}
