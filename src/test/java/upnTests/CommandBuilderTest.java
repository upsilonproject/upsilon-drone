package upnTests;

import org.junit.BeforeClass;
import org.junit.Test;

import junit.framework.Assert;
import upsilon.node.Configuration;
import upsilon.node.configuration.xml.XmlConfigurationLoader;
import upsilon.node.configuration.xml.XmlConfigurationValidator;
import upsilon.node.dataStructures.StructureCommand;
import upsilon.node.dataStructures.StructureService;
import upsilon.node.util.UPath;
import upsilon.node.util.Util;

public class CommandBuilderTest {

	@BeforeClass
	public static void setup() {
		Configuration.instance.clear();
	}

	@Test
	public void testBuildCommand() {
		final StructureCommand checkFoo = new StructureCommand();
		final StructureService service = new StructureService();
		service.setCommandWithOnlyPositionalArgs(checkFoo, "one", "two");

		final StructureCommand cmd = new StructureCommand();
		cmd.setIdentifier("check_foo");
		cmd.setCommandLine("/usr/bin/foo -w foo");

		Assert.assertEquals("/usr/bin/foo", cmd.getExecutable());
		Assert.assertEquals("/usr/bin/foo -w foo", Util.implode(cmd.getFinalCommandLinePieces(service)));

		cmd.setCommandLine("/usr/bin/foo $ARG1 $ARG2");
		Assert.assertEquals("/usr/bin/foo one two", Util.implode(cmd.getFinalCommandLinePieces(service)));
	}

	@Test
	public void testBuildCommandFromConfig() throws Exception {
		final XmlConfigurationValidator validator = new XmlConfigurationValidator(new UPath("src/test/resources/config.xml"));
		validator.parse();

		final XmlConfigurationLoader loader = new XmlConfigurationLoader();
		loader.load(validator.getValidatedConfiguration(), false);

		Assert.assertTrue(validator.isParseClean());

		// named args only
		final StructureService serviceNamed = Configuration.instance.services.get("ping_host_namedarg");
		final String finalCmdLineNamed = serviceNamed.getFinalCommandLine(serviceNamed);
		Assert.assertEquals("/usr/local/sbin/nix/check_ping_named host1.example.com 60", finalCmdLineNamed);

		// positional args only
		final StructureService service = Configuration.instance.services.get("mindstormPing");
		final String finalCmdLine = service.getFinalCommandLine(service);
		Assert.assertEquals("/usr/local/sbin/nix/check_ping mindstorm.teratan.lan", finalCmdLine);
	}

	@Test
	public void testServiceRegistered() {
		final StructureService ss = new StructureService();

		ss.setRegistered(true);
		Assert.assertTrue(ss.isRegistered());
	}
}
