package upnTests;

import junit.framework.Assert;
import org.junit.Test;

import java.time.Duration;
import java.time.Period;

public class JodaDurations {
	@Test
	public void testJodaDurations1() {
		Assert.assertEquals(Duration.ofMinutes(5), Duration.parse("PT5M"));
	}
} 
