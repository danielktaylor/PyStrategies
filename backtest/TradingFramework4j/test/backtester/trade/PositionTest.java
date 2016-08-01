package backtester.trade;

import java.math.BigDecimal;

import junit.framework.Assert;

import org.junit.Test;

import backtester.trade.Position;


public class PositionTest {

	@Test(expected = IllegalArgumentException.class)
	public void nullShares() {
		new Position("", null, BigDecimal.ONE, BigDecimal.ONE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void fractionalShares() {
		new Position("", new BigDecimal("10.1"), BigDecimal.ONE, BigDecimal.ONE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullCost() {
		new Position("", BigDecimal.ONE, null, BigDecimal.ONE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullClosedPl() {
		new Position("", BigDecimal.ONE, BigDecimal.ONE, null);
	}

	@Test
	public void precisionTest() {
		final Position position = new Position("", new BigDecimal("10000"), new BigDecimal("1111123456789"), BigDecimal.ZERO);

		Assert.assertEquals(new BigDecimal("111112345.6789"), position.getAveragePrice());
	}
}
