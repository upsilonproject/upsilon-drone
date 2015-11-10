package upnTests;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import upnTests.customMatchers.RegexMatcher;
import upsilon.node.Database;
import upsilon.node.Main;
import upsilon.node.dataStructures.CollectionOfStructures;
import upsilon.node.dataStructures.StructurePeer;
import upsilon.node.util.SslUtil;

public class MainTest {
	private static final Logger LOG = LoggerFactory.getLogger(MainTest.class);
	
	@BeforeClass 
	public static void setupSsl() throws Exception {
		SslUtil.init(); 
	} 
	
    @Test
    public void testGetters() {
        Assert.assertNotNull(null, Main.instance.getDaemons());
    }
    
    @Test
    public void testConfigurationOverridePath() { 
    	Assert.assertNull(Main.getConfigurationOverridePath());
    }

    @Test
    public void testNodeType() throws Exception {
        final CollectionOfStructures<StructurePeer> peers = new CollectionOfStructures<>("testingStructure");

        Assert.assertEquals("useless-testing-node", Main.instance.guessNodeType(null, peers));

        Assert.assertEquals("super-node", Main.instance.guessNodeType(new Database(null, null, null, 0, null), peers));

        peers.register(new StructurePeer("localhost", 100));
        Assert.assertEquals("service-node", Main.instance.guessNodeType(null, peers));

        Assert.assertEquals("non-standard-node", Main.instance.guessNodeType(new Database(null, null, null, 0, null), peers));
    }

    @Ignore
    @Test
    public void testVersion() {
        final String releaseVersion = Main.getVersion();
          
        LOG.info("Testing version is well formed: " + releaseVersion);

        Assert.assertThat(releaseVersion, RegexMatcher.matches("\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?"));
    }
}
