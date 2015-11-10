package upnTests.configurationChanged;

import org.junit.Test;

import junit.framework.Assert;
import upsilon.node.Configuration;
import upsilon.node.configuration.FileChangeWatcher;
import upsilon.node.configuration.xml.XmlConfigurationLoader;
import upsilon.node.configuration.xml.XmlConfigurationValidator;

public class InvalidDependantCommand extends AbstractConfigurationChangeTest {
	public InvalidDependantCommand() throws Exception {
		super("invalidDependantCommand");
	}

	@Test
	public void testConfig() throws Exception {
		final XmlConfigurationValidator validator = new XmlConfigurationValidator(this.before, true);
		final XmlConfigurationLoader loader = new XmlConfigurationLoader();
		final FileChangeWatcher fcw = loader.load(validator.getValidatedConfiguration(), true, false);

		Assert.assertEquals(validator.getSourcePath(), this.before);
		Assert.assertTrue(validator.isParseClean());

		Assert.assertTrue(Configuration.instance.services.containsId("helloWorld"));
		Assert.assertEquals(Configuration.instance.services.size(), 1);

		fcw.setWatchedFile(this.after);
		fcw.checkForModification();

		// Check that no structured were tarnished
		Assert.assertTrue(Configuration.instance.services.containsId("helloWorld"));
		Assert.assertEquals(Configuration.instance.services.size(), 1);
	}
}
