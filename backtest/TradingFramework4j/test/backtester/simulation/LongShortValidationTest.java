package backtester.simulation;

import java.math.BigDecimal;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import backtester.simulation.DefaultSimulationMarket;
import backtester.simulation.SimulationClOrdId;
import backtester.trade.ClOrdId;
import backtester.trade.Fill;
import backtester.trade.OrderSpecification;
import backtester.trade.TradeListener;
import backtester.trade.TradeType;
import backtester.trade.TransactionCost;



public class LongShortValidationTest implements TradeListener {
	private static final String SYMBOL = "TEST";

	private DefaultSimulationMarket simulationMarket;
	private static long nextClOrdId = 0;

	private int acceptedCounterLatch;
	private int rejectCounterLatch;

	@Before
	public void runFirst() {
		simulationMarket = new DefaultSimulationMarket(null);
		simulationMarket.setTradeListener(this);
		acceptedCounterLatch = 0;
		rejectCounterLatch = 0;
	}

	@After
	public void runLast() {
		Assert.assertEquals("Not enough accepts.  Remaining accepts not zero", 0, acceptedCounterLatch);
		Assert.assertEquals("Not enough rejects.  Remaining rejects not zero", 0, rejectCounterLatch);

		simulationMarket.stop();
	}

	private SimulationClOrdId getNextClOrdId() {
		return new SimulationClOrdId(nextClOrdId++);
	}

	@Test
	public void sellWhileLongGoingLessLong() {
		acceptedCounterLatch = 1;

		final BigDecimal price = new BigDecimal(10);
		final int quantity = 10;

		simulationMarket.setPosition(50);
		simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, price, quantity, TradeType.SELL));
	}

	@Test
	public void sellWhileLongGoingFlat() {
		acceptedCounterLatch = 1;

		final BigDecimal price = new BigDecimal(10);
		final int quantity = 50;

		simulationMarket.setPosition(quantity);
		simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, price, quantity, TradeType.SELL));
	}

	@Test
	public void sellWhileLongGoingShort() {
		rejectCounterLatch = 1;

		final BigDecimal price = new BigDecimal(10);
		final int quantity = 50;

		simulationMarket.setPosition(20);
		simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, price, quantity, TradeType.SELL));
	}

	@Test
	public void sellWhileFlat() {
		rejectCounterLatch = 1;

		final BigDecimal price = new BigDecimal(10);
		final int quantity = 50;
		simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, price, quantity, TradeType.SELL));
	}

	@Test
	public void sellWhileShort() {
		rejectCounterLatch = 1;

		final BigDecimal price = new BigDecimal(10);
		final int quantity = 50;

		simulationMarket.setPosition(-10);
		simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, price, quantity, TradeType.SELL));
	}

	@Test
	public void sellWhileLongGoingShortLSVDisabled() {
		simulationMarket.setLongSaleValidationEnabled(false);

		acceptedCounterLatch = 1;

		final BigDecimal price = new BigDecimal(10);
		final int quantity = 50;

		simulationMarket.setPosition(20);
		simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, price, quantity, TradeType.SELL));
	}

	@Test
	public void sellWhileFlatLSVDisabled() {
		simulationMarket.setLongSaleValidationEnabled(false);

		acceptedCounterLatch = 1;

		final BigDecimal price = new BigDecimal(10);
		final int quantity = 50;
		simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, price, quantity, TradeType.SELL));
	}

	@Test
	public void sellWhileShortLSVDisabled() {
		simulationMarket.setLongSaleValidationEnabled(false);

		acceptedCounterLatch = 1;

		final BigDecimal price = new BigDecimal(10);
		final int quantity = 50;

		simulationMarket.setPosition(-10);
		simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, price, quantity, TradeType.SELL));
	}

	@Test
	public void shortWhileLongGoingLessLong() {
		acceptedCounterLatch = 1;

		final BigDecimal price = new BigDecimal(10);
		final int quantity = 10;
		simulationMarket.setPosition(50);
		simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, price, quantity, TradeType.SHORT));
	}

	@Test
	public void shortWhileLongGoingFlat() {
		acceptedCounterLatch = 1;

		final BigDecimal price = new BigDecimal(10);
		final int quantity = 50;

		simulationMarket.setPosition(quantity);
		simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, price, quantity, TradeType.SHORT));
	}

	@Test
	public void shortWhileLongGoingShort() {
		acceptedCounterLatch = 1;

		final BigDecimal price = new BigDecimal(10);
		final int quantity = 50;

		simulationMarket.setPosition(10);
		simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, price, quantity, TradeType.SHORT));
	}

	@Test
	public void shortWhileFlat() {
		acceptedCounterLatch = 1;

		final BigDecimal price = new BigDecimal(10);
		final int quantity = 50;

		simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, price, quantity, TradeType.SHORT));
	}

	@Test
	public void shortWhileShort() {
		acceptedCounterLatch = 1;

		final BigDecimal price = new BigDecimal(10);
		final int quantity = 50;

		simulationMarket.setPosition(-10);
		simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, price, quantity, TradeType.SHORT));
	}

	@Test
	public void buyWhileShortGoingLessShort() {
		acceptedCounterLatch = 1;

		final BigDecimal price = new BigDecimal(10);
		final int quantity = 10;

		simulationMarket.setPosition(-50);
		simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, price, quantity, TradeType.BUY));
	}

	@Test
	public void buyWhileShortGoingFlat() {
		acceptedCounterLatch = 1;

		final BigDecimal price = new BigDecimal(10);
		final int quantity = 10;

		simulationMarket.setPosition(-10);
		simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, price, quantity, TradeType.BUY));
	}

	@Test
	public void buyWhileShortGoingLong() {
		acceptedCounterLatch = 1;

		final BigDecimal price = new BigDecimal(10);
		final int quantity = 20;

		simulationMarket.setPosition(-10);
		simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, price, quantity, TradeType.BUY));
	}

	@Test
	public void buyWhileFlat() {
		acceptedCounterLatch = 1;

		final BigDecimal price = new BigDecimal(10);
		final int quantity = 20;

		simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, price, quantity, TradeType.BUY));
	}

	@Test
	public void buyWhileLong() {
		acceptedCounterLatch = 1;

		final BigDecimal price = new BigDecimal(10);
		final int quantity = 20;

		simulationMarket.setPosition(10);
		simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, price, quantity, TradeType.BUY));
	}

	@Test
	public void coverWhileShortGoingLessShort() {
		acceptedCounterLatch = 1;

		final BigDecimal price = new BigDecimal(10);
		final int quantity = 10;

		simulationMarket.setPosition(-50);
		simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, price, quantity, TradeType.COVER));
	}

	@Test
	public void coverWhileShortGoingFlat() {
		acceptedCounterLatch = 1;

		final BigDecimal price = new BigDecimal(10);
		final int quantity = 10;

		simulationMarket.setPosition(-10);
		simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, price, quantity, TradeType.COVER));
	}

	@Test
	public void coverWhileShortGoingLong() {
		acceptedCounterLatch = 1;

		final BigDecimal price = new BigDecimal(10);
		final int quantity = 20;

		simulationMarket.setPosition(-10);
		simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, price, quantity, TradeType.COVER));
	}

	@Test
	public void coverWhileFlat() {
		acceptedCounterLatch = 1;

		final BigDecimal price = new BigDecimal(10);
		final int quantity = 20;

		simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, price, quantity, TradeType.COVER));
	}

	@Test
	public void coverWhileLong() {
		acceptedCounterLatch = 1;

		final BigDecimal price = new BigDecimal(10);
		final int quantity = 20;

		simulationMarket.setPosition(10);
		simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, price, quantity, TradeType.COVER));
	}

	@Override
	public void onNewOrderAccepted(final ClOrdId clOrdId, final OrderSpecification order) {
		Assert.assertFalse("Received more accepts than expected", acceptedCounterLatch == 0);
		acceptedCounterLatch--;
	}

	@Override
	public void onCancelReplaceAccepted(final ClOrdId clOrdId, final OrderSpecification order, final ClOrdId origClOrdId) {
		Assert.assertFalse("Received more accepts than expected", acceptedCounterLatch == 0);
		acceptedCounterLatch--;
	}

	@Override
	public void onCancelReplaceRejected(final ClOrdId clOrdId) {
		Assert.assertFalse("Received more rejects than expected", rejectCounterLatch == 0);
		rejectCounterLatch--;
	}

	@Override
	public void onNewOrderRejected(final ClOrdId clOrdId) {
		Assert.assertFalse("Received more rejects than expected", rejectCounterLatch == 0);
		rejectCounterLatch--;
	}

	@Override
	public void onCancelAccepted(final ClOrdId clOrdId, final ClOrdId origClOrdId) {
	}

	@Override
	public void onCancelRejected(final ClOrdId clOrdId) {
	}

	@Override
	public void onFill(final Fill fill) {
	}

	@Override
	public void onTransactionCost(final TransactionCost cost) {
	}

}
