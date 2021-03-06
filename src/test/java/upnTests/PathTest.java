package upnTests;

import java.net.MalformedURLException;

import org.junit.Test;

import junit.framework.Assert;
import upsilon.node.util.UPath;
 
public class PathTest {
	@Test
	public void testGetFilename() throws MalformedURLException {
		UPath path1 = new UPath("http://example.com/?foo=bar&bar=foo");
		Assert.assertEquals("/?foo=bar&bar=foo", path1.getFilename()); 
		
		UPath path2 = new UPath("/etc/sample.conf");
		Assert.assertEquals("sample.conf", path2.getFilename());
	} 
	
	@Test 
	public void testAbsolutePath() throws Exception {
		String absolutePath = "/opt/test/foo";
		UPath path = new UPath(absolutePath);
		 
		Assert.assertEquals(absolutePath, path.getAbsolutePath());
	}
}
