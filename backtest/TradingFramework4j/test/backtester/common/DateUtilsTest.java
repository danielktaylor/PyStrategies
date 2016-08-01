package backtester.common;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import backtester.common.DateUtils;


public class DateUtilsTest {

	@Before
	public void runfirst() {

	}

	@Test
	public void timeTest() {
		String testResult = DateUtils.getBestHumanTimeFromMillis(2000);
		Assert.assertEquals("2 seconds 0 milliseconds", testResult);
		
		testResult = DateUtils.getBestHumanTimeFromMillis(123456);
		Assert.assertEquals("2 minutes 3 seconds 456 milliseconds", testResult);
		
		testResult = DateUtils.getBestHumanTimeFromMillis(789000);
		Assert.assertEquals("13 minutes 9 seconds 0 milliseconds", testResult);
		
		testResult = DateUtils.getBestHumanTimeFromMillis(2000000);
		Assert.assertEquals("33 minutes 20 seconds 0 milliseconds", testResult);
	}
}
