package upnTests.configurationTests;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import upsilon.node.Configuration;
import upsilon.node.configuration.FileChangeWatcher;
import upsilon.node.configuration.xml.XmlConfigurationLoader;
import upsilon.node.configuration.xml.XmlConfigurationValidator;
import upsilon.node.util.UPath;

public class MultiConfigurationTest {
	@BeforeClass
	@AfterClass
	public static void setupConfig() {
		Configuration.instance.clear();
	}

	@Test
	@Ignore
	public void testMultiConfiguration() throws Exception {
		final UPath masterFile = new UPath("file://src/test/resources/multi/config.xml");
		final XmlConfigurationValidator masterValidator = new XmlConfigurationValidator(masterFile, false);
		masterValidator.parse();
		Assert.assertTrue(masterValidator.isParseClean());

		final UPath slaveFile = new UPath("file://src/test/resources/multi/slave.xml");
		final XmlConfigurationValidator slaveValidator = new XmlConfigurationValidator(slaveFile);
		slaveValidator.parse();

		final Configuration config = Configuration.instance;

		final XmlConfigurationLoader loader = new XmlConfigurationLoader();
		loader.load(masterValidator.getValidatedConfiguration(), false, false);

		MatcherAssert.assertThat(config.services.getImmutable(), Matchers.hasSize(1));
		MatcherAssert.assertThat(config.services.containsId("echo"), Matchers.is(true));

		MatcherAssert.assertThat(config.commands.getImmutable(), Matchers.hasSize(1));
		MatcherAssert.assertThat(config.commands.containsId("echo"), Matchers.is(true));

		final FileChangeWatcher fcwSlave = loader.load(slaveValidator.getValidatedConfiguration(), true, false);

		Thread.sleep(1000);

		MatcherAssert.assertThat(config.services.getImmutable(), Matchers.hasSize(2));
		MatcherAssert.assertThat(config.commands.getImmutable(), Matchers.hasSize(2));

		loader.load(slaveValidator.getValidatedConfiguration(), false, false);

		MatcherAssert.assertThat(config.services.getImmutable(), Matchers.hasSize(2));
		MatcherAssert.assertThat(config.commands.getImmutable(), Matchers.hasSize(2));

		final UPath emptyConfig = new UPath("src/test/resources/multi/config.empty.xml");

		loader.replaceSources(slaveFile, emptyConfig);

		fcwSlave.setWatchedFile(emptyConfig);
		fcwSlave.checkForModification();

		MatcherAssert.assertThat(config.services.getImmutable(), Matchers.hasSize(1));
		MatcherAssert.assertThat(config.services.containsId("hostname"), Matchers.is(true));

		MatcherAssert.assertThat(config.commands.getImmutable(), Matchers.hasSize(1));
		MatcherAssert.assertThat(config.commands.containsId("hostname"), Matchers.is(true));
	}
}
