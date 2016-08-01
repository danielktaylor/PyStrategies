package backtester.trade;

import java.math.BigDecimal;
import java.util.Date;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import backtester.simulation.SimulationClOrdId;
import backtester.simulation.SimulationOrderId;
import backtester.strategy.BaseStrategy;
import backtester.strategy.DoNothingStrategy;
import backtester.strategy.StrategyConfiguration;
import backtester.trade.Fill;
import backtester.trade.LiquidityFlag;



public class StrategyTest {

	private static final String TEST_SYMBOL = "";
	private BaseStrategy strategy;

	@Before
	public void setup() {
		strategy = new DoNothingStrategy(null, StrategyConfiguration.emptyStrategyConfiguration());
	}

	@Test
	public void simpleBuyBuySharesTraded() {
		final Fill fill1 = new Fill(new SimulationClOrdId(1L), new SimulationOrderId(1L), TEST_SYMBOL, 10, BigDecimal.TEN, 0, new Date(1),
				LiquidityFlag.ADDED);
		strategy.onFill(fill1);
		Assert.assertEquals(10, strategy.getSharesTraded());

		final Fill fill2 = new Fill(new SimulationClOrdId(2L), new SimulationOrderId(2L), TEST_SYMBOL, 10, BigDecimal.TEN, 0, new Date(2),
				LiquidityFlag.ADDED);
		strategy.onFill(fill2);

		Assert.assertEquals(20, strategy.getSharesTraded());
	}

	@Test
	public void simpleBuySellSharesTraded() {
		final Fill fill1 = new Fill(new SimulationClOrdId(1L), new SimulationOrderId(1L), TEST_SYMBOL, 10, BigDecimal.TEN, 0, new Date(1),
				LiquidityFlag.ADDED);
		strategy.onFill(fill1);
		Assert.assertEquals(10, strategy.getSharesTraded());

		final Fill fill2 = new Fill(new SimulationClOrdId(2L), new SimulationOrderId(2L), TEST_SYMBOL, 10, new BigDecimal(-10), 0, new Date(2),
				LiquidityFlag.ADDED);
		strategy.onFill(fill2);

		Assert.assertEquals(20, strategy.getSharesTraded());
	}

}
