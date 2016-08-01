package backtester.trade;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import backtester.simulation.SimulationClOrdId;
import backtester.simulation.SimulationOrderId;
import backtester.strategy.BaseStrategy;
import backtester.strategy.DoNothingStrategy;
import backtester.strategy.StrategyConfiguration;
import backtester.trade.Fill;
import backtester.trade.LiquidityFlag;
import backtester.trade.OrderSpecification;
import backtester.trade.Position;
import backtester.trade.TradeType;

public class PositionHistoryTest {
	private static final String TEST_SYMBOL = "";
	private BaseStrategy strategy;
	private Deque<Position> positions;
	private long counter;

	@Before
	public void setup() {
		strategy = new DoNothingStrategy(null, StrategyConfiguration.emptyStrategyConfiguration());
		positions = new LinkedList<Position>();
		counter = 0;
		pushPosition("0", "0", "0");
	}

	@After
	public void checkModel() {
		final BigDecimal positionClosedPL = positions.peek().getClosedPL();
		final BigDecimal strategyClosedPL = strategy.getClosedPL();

		Assert.assertEquals("Closed PLs don't match (" + positionClosedPL + "!=" + strategyClosedPL + ")", 0,
				positionClosedPL.compareTo(strategyClosedPL));

		for (final Position position : strategy.getPositionHistory()) {
			Assert.assertEquals(positions.poll(), position);
		}
	}

	//Take in strings so tests can be more readable
	private void pushFill(final String qty, final String price) {
		final SimulationClOrdId clOrdId = new SimulationClOrdId(counter);
		final SimulationOrderId orderId = new SimulationOrderId(counter);
		final TradeType tradeType = Integer.valueOf(qty).compareTo(0) > 0 ? TradeType.BUY : TradeType.SELL;
		final OrderSpecification orderPlaced = new OrderSpecification(TEST_SYMBOL, new BigDecimal(price), Integer.valueOf(qty), tradeType);
		strategy.placeOrder(clOrdId, orderPlaced);
		strategy.onNewOrderAccepted(clOrdId, orderPlaced);
		final Fill fill = new Fill(clOrdId, orderId, TEST_SYMBOL, Integer.valueOf(qty),
				new BigDecimal(price), 0, new Date(counter), LiquidityFlag.ADDED);

		strategy.onFill(fill);
		counter++;
	}

	//Take in strings so tests can be more readable
	private void pushPosition(final String shares, final String totalCost, final String closedPl) {
		final Position position = new Position(TEST_SYMBOL, new BigDecimal(shares), new BigDecimal(totalCost), new BigDecimal(closedPl));
		positions.push(position);
	}

	@Test
	public void inOutSamePrice() {
		pushFill("10", "5");
		pushPosition("10", "50", "0");

		pushFill("-10", "5");
		pushPosition("0", "0", "0");
	}

	@Test
	public void inTwiceOutOnceSamePrice() {
		pushFill("10", "5");
		pushPosition("10", "50", "0");

		pushFill("10", "5");
		pushPosition("20", "100", "0");

		pushFill("-20", "5");
		pushPosition("0", "0", "0");
	}

	@Test
	public void inTwiceDifferentPricesOutOnceNoPL() {
		pushFill("10", "5");
		pushPosition("10", "50", "0");

		pushFill("10", "10");
		pushPosition("20", "150", "0");

		pushFill("-20", "7.5");
		pushPosition("0", "0", "0");
	}

	@Test
	public void inOutWithProfit() {
		pushFill("10", "5");
		pushPosition("10", "50", "0");

		pushFill("-10", "10");
		pushPosition("0", "0", "50");
	}

	@Test
	public void inOutWithLoss() {
		pushFill("10", "5");
		pushPosition("10", "50", "0");

		pushFill("-10", "1");
		pushPosition("0", "0", "-40");
	}

	@Test
	public void inOutTwiceWithLosses() {
		pushFill("10", "5");
		pushPosition("10", "50", "0");

		pushFill("-10", "1");
		pushPosition("0", "0", "-40");

		pushFill("10", "5");
		pushPosition("10", "50", "-40");

		pushFill("-10", "1");
		pushPosition("0", "0", "-80");
	}

	@Test
	public void shortToMoreShort() {
		pushFill("-10", "2");
		pushPosition("-10", "-20", "0");

		pushFill("-15", "2");
		pushPosition("-25", "-50", "0");
	}

	@Test
	public void shortToMoreShortThenNoProfit() {
		pushFill("-10", "2");
		pushPosition("-10", "-20", "0");

		pushFill("-15", "2");
		pushPosition("-25", "-50", "0");

		pushFill("25", "2");
		pushPosition("0", "0", "0");
	}

	@Test
	public void shortToFlatWithProfit() {
		pushFill("-10", "2");
		pushPosition("-10", "-20", "0");

		pushFill("10", "1");
		pushPosition("0", "0", "10");
	}

	@Test
	public void shortToFlatWithLoss() {
		pushFill("-10", "2");
		pushPosition("-10", "-20", "0");

		pushFill("10", "3");
		pushPosition("0", "0", "-10");
	}

	@Test
	public void shortToLongNoPl() {
		pushFill("-10", "2");
		pushPosition("-10", "-20", "0");

		pushFill("20", "2");
		pushPosition("10", "20", "0");
	}

	@Test
	public void shortToLongWithProfit() {
		pushFill("-10", "2");
		pushPosition("-10", "-20", "0");

		pushFill("20", "1");
		pushPosition("10", "10", "10");
	}

	@Test
	public void shortToLongWithLoss() {
		pushFill("-10", "2");
		pushPosition("-10", "-20", "0");

		pushFill("20", "3");
		pushPosition("10", "30", "-10");
	}

	@Test
	public void longToShortNoPL() {
		pushFill("10", "2");
		pushPosition("10", "20", "0");

		pushFill("-20", "2");
		pushPosition("-10", "-20", "0");
	}

	@Test
	public void longToShortWithProfit() {
		pushFill("10", "2");
		pushPosition("10", "20", "0");

		pushFill("-20", "3");
		pushPosition("-10", "-30", "10");
	}

	@Test
	public void longToShortWithLoss() {
		pushFill("10", "2");
		pushPosition("10", "20", "0");

		pushFill("-20", "1");
		pushPosition("-10", "-10", "-10");
	}

}
