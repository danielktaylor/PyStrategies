package backtester.simulation;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import backtester.quote.BookEntryFactory;
import backtester.quote.TradeTick;
import backtester.simulation.DefaultLatencyProfile;
import backtester.simulation.DefaultSimulationMarket;
import backtester.simulation.SimulationClOrdId;
import backtester.simulation.SimulationMarket;
import backtester.simulation.SimulationMarketLatencyProxy;
import backtester.trade.ClOrdId;
import backtester.trade.Fill;
import backtester.trade.OrderSpecification;
import backtester.trade.TradeListener;
import backtester.trade.TradeType;
import backtester.trade.TradingException;
import backtester.trade.TransactionCost;

public class SimulationMarketFillTest implements TradeListener {
    private static final String SYMBOL = "TEST";

    private static final Date ALMOST_END_OF_TIME = new Date(Long.MAX_VALUE - 1);
    private static final Date END_OF_TIME = new Date(Long.MAX_VALUE);

    private SimulationMarket simulationMarket;
    private DefaultSimulationMarket innerMarket;
    private int fillCounterLatch;
    private int acceptedCounterLatch;
    private int cancelReplaceRejectedLatch;
    private int newOrderRejectedLatch;
    private List<Fill> fills;
    private long dateCounter;
    private static long nextClOrdId = 0;

    @Before
    public void runFirst() throws NoSuchMethodException {
        dateCounter = 10;

        innerMarket = new DefaultSimulationMarket(null);
        innerMarket.setLongSaleValidationEnabled(false);
        simulationMarket = SimulationMarketLatencyProxy.createSimulationMarketLatencyProxy(innerMarket, new DefaultLatencyProfile());
        simulationMarket.setTradeListener(this);
        fills = new LinkedList<Fill>();
        acceptedCounterLatch = 0;
        fillCounterLatch = 0;
        cancelReplaceRejectedLatch = 0;
    }

    @After
    public void runLast() {
        Assert.assertEquals("Not enough accepts.  Remaining accepts not zero", 0, acceptedCounterLatch);
        Assert.assertEquals("Not enough fills.  Remaining fills not zero", 0, fillCounterLatch);

        simulationMarket.stop();
        simulationMarket.reset();
    }

    @Override
    public void onFill(final Fill fill) {
        Assert.assertFalse("Received more fills than expected", fillCounterLatch == 0);
        fills.add(fill);
        fillCounterLatch--;
    }

    @Override
    public void onTransactionCost(final TransactionCost cost) {
        // Do nothing for fees
    }

    private Date getNextDate() {
        dateCounter += DefaultLatencyProfile.SIMULATED_ORDER_LATENCY_MILLIS + 1;
        return new Date(dateCounter);
    }

    private SimulationClOrdId getNextClOrdId() {
        return new SimulationClOrdId(nextClOrdId++);
    }

    private void endSimulation() {
        simulationMarket.setCurrentTime(ALMOST_END_OF_TIME);
        simulationMarket.setCurrentTime(END_OF_TIME);
    }

    @Test
    public void testAtAloneTopOfBid() throws InterruptedException, TradingException {
        fillCounterLatch = 1;
        acceptedCounterLatch = 1;

        final BigDecimal bidPrice = new BigDecimal(10);
        final int bidQuantity = 10;
        final SimulationClOrdId clOrdId = getNextClOrdId();
        simulationMarket.placeOrder(clOrdId, new OrderSpecification(SYMBOL, bidPrice, bidQuantity, TradeType.BUY));

        simulationMarket.onAsk(BookEntryFactory.createActualAsk("FillingAsk", SYMBOL, 10, new BigDecimal(9), getNextDate()));

        endSimulation();

        Assert.assertEquals(bidQuantity, fills.get(0).getQuantity());
        Assert.assertEquals(bidPrice, fills.get(0).getPrice());
        Assert.assertEquals(clOrdId, fills.get(0).getClOrdId());

        Assert.assertEquals(bidQuantity, innerMarket.getPosition());
    }

    @Test
    public void placingNewOrderWithZeroQuantityShouldBeRejected() {
        newOrderRejectedLatch = 1;

        simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, BigDecimal.ZERO, 0, TradeType.BUY));

        endSimulation();
        Assert.assertEquals(0, newOrderRejectedLatch);
    }

    @Test
    public void placingNewOrderWithNegativePriceShouldBeRejected() {
        newOrderRejectedLatch = 1;

        simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, new BigDecimal(-1), 1, TradeType.BUY));

        endSimulation();
        Assert.assertEquals(0, newOrderRejectedLatch);
    }

    @Test
    public void placingCancelReplaceOrderWithZeroQuantityShouldBeRejected() {
        cancelReplaceRejectedLatch = 1;

        simulationMarket.cancelReplaceOrder(getNextClOrdId(), getNextClOrdId(), new OrderSpecification(SYMBOL, BigDecimal.ZERO, 0, TradeType.BUY));

        endSimulation();
        Assert.assertEquals(0, cancelReplaceRejectedLatch);
    }

    @Test
    public void placingCancelReplaceOrderWithNegativePriceShouldBeRejected() {
        cancelReplaceRejectedLatch = 1;

        simulationMarket.cancelReplaceOrder(getNextClOrdId(), getNextClOrdId(), new OrderSpecification(SYMBOL, new BigDecimal(-1), 1, TradeType.BUY));

        endSimulation();
        Assert.assertEquals(0, cancelReplaceRejectedLatch);
    }

    @Test
    public void testAtAloneTopOfBidMultipleFills() throws InterruptedException, TradingException {
        fillCounterLatch = 4;
        acceptedCounterLatch = 1;

        final BigDecimal bidPrice = new BigDecimal(10);
        final int bidQuantity = 100;
        final SimulationClOrdId clOrdId = getNextClOrdId();
        simulationMarket.placeOrder(clOrdId, new OrderSpecification(SYMBOL, bidPrice, bidQuantity, TradeType.BUY));

        simulationMarket.onAsk(BookEntryFactory.createActualAsk("Ask1", SYMBOL, 30, new BigDecimal(9), getNextDate()));
        simulationMarket.onAsk(BookEntryFactory.createActualAsk("Ask2", SYMBOL, 20, new BigDecimal(8), getNextDate()));
        simulationMarket.onAsk(BookEntryFactory.createActualAsk("Ask3", SYMBOL, 10, new BigDecimal(7), getNextDate()));
        simulationMarket.onAsk(BookEntryFactory.createActualAsk("Ask4", SYMBOL, 40, new BigDecimal(6), getNextDate()));
        simulationMarket.onAsk(BookEntryFactory.createActualAsk("Ask5", SYMBOL, 1, new BigDecimal(6), getNextDate())); // this one should not cause a fill

        endSimulation();

        Assert.assertEquals(30, fills.get(0).getQuantity());
        Assert.assertEquals(bidPrice, fills.get(0).getPrice());
        Assert.assertEquals(clOrdId, fills.get(0).getClOrdId());

        Assert.assertEquals(20, fills.get(1).getQuantity());
        Assert.assertEquals(bidPrice, fills.get(1).getPrice());
        Assert.assertEquals(clOrdId, fills.get(1).getClOrdId());

        Assert.assertEquals(10, fills.get(2).getQuantity());
        Assert.assertEquals(bidPrice, fills.get(2).getPrice());
        Assert.assertEquals(clOrdId, fills.get(2).getClOrdId());

        Assert.assertEquals(40, fills.get(3).getQuantity());
        Assert.assertEquals(bidPrice, fills.get(3).getPrice());
        Assert.assertEquals(clOrdId, fills.get(3).getClOrdId());

        Assert.assertEquals(100, innerMarket.getPosition());
    }

    @Test
    public void testAtAloneTopOfBidALotOfQty() throws InterruptedException, TradingException {
        fillCounterLatch = 1;
        acceptedCounterLatch = 1;

        final BigDecimal bidPrice = new BigDecimal(10);
        final int bidQuantity = 100;
        final SimulationClOrdId clOrdId = getNextClOrdId();
        simulationMarket.placeOrder(clOrdId, new OrderSpecification(SYMBOL, bidPrice, bidQuantity, TradeType.BUY));

        simulationMarket.onAsk(BookEntryFactory.createActualAsk("Ask1", SYMBOL, 3000, new BigDecimal(9), getNextDate()));

        endSimulation();

        Assert.assertEquals(100, fills.get(0).getQuantity());
        Assert.assertEquals(bidPrice, fills.get(0).getPrice());
        Assert.assertEquals(clOrdId, fills.get(0).getClOrdId());

        Assert.assertEquals(bidQuantity, innerMarket.getPosition());
    }
    
    @Test
    public void testAtTopAloneBidThenBumpedDown() throws InterruptedException, TradingException {
        fillCounterLatch = 1;
        acceptedCounterLatch = 1;

        final BigDecimal bidPrice = new BigDecimal(10);
        final int bidQuantity = 100;
        final SimulationClOrdId clOrdId = getNextClOrdId();
        simulationMarket.placeOrder(clOrdId, new OrderSpecification(SYMBOL, bidPrice, bidQuantity, TradeType.BUY));

        simulationMarket.onAsk(BookEntryFactory.createActualAsk("Ask1", SYMBOL, 50, new BigDecimal(9), getNextDate()));
        simulationMarket.onBid(BookEntryFactory.createActualBid("BetterBid", SYMBOL, 10, new BigDecimal(11), getNextDate()));
        simulationMarket.onAsk(BookEntryFactory.createActualAsk("Ask2", SYMBOL, 50, new BigDecimal(9), getNextDate()));

        endSimulation();

        Assert.assertEquals(50, fills.get(0).getQuantity());
        Assert.assertEquals(bidPrice, fills.get(0).getPrice());
        Assert.assertEquals(clOrdId, fills.get(0).getClOrdId());

        Assert.assertEquals(50, innerMarket.getPosition());
    }
    
    @Test
    public void testAtAloneTopOfAsk() throws InterruptedException, TradingException {
        fillCounterLatch = 1;
        acceptedCounterLatch = 1;

        final BigDecimal askPrice = new BigDecimal(10);
        final int askQuantity = 10;
        final SimulationClOrdId clOrdId = getNextClOrdId();
        simulationMarket.placeOrder(clOrdId, new OrderSpecification(SYMBOL, askPrice, askQuantity, TradeType.SELL));

        simulationMarket.onBid(BookEntryFactory.createActualBid("FillingBid", SYMBOL, 10, new BigDecimal(11), getNextDate()));

        endSimulation();

        Assert.assertEquals(askQuantity * -1, fills.get(0).getQuantity());
        Assert.assertEquals(askPrice, fills.get(0).getPrice());
        Assert.assertEquals(clOrdId, fills.get(0).getClOrdId());

        Assert.assertEquals(askQuantity * -1, innerMarket.getPosition());
    }

    @Test
    public void testAtAloneTopOfAskMultipleFills() throws InterruptedException, TradingException {
        fillCounterLatch = 4;
        acceptedCounterLatch = 1;

        final BigDecimal askPrice = new BigDecimal(5);
        final int askQuantity = 100;
        final SimulationClOrdId clOrdId = getNextClOrdId();
        simulationMarket.placeOrder(clOrdId, new OrderSpecification(SYMBOL, askPrice, askQuantity, TradeType.SELL));

        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 30, new BigDecimal(9), getNextDate()));
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid2", SYMBOL, 20, new BigDecimal(8), getNextDate()));
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid3", SYMBOL, 10, new BigDecimal(7), getNextDate()));
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid4", SYMBOL, 40, new BigDecimal(6), getNextDate()));
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid5", SYMBOL, 1, new BigDecimal(6), getNextDate())); // this one should not cause a fill

        endSimulation();

        Assert.assertEquals(-30, fills.get(0).getQuantity());
        Assert.assertEquals(askPrice, fills.get(0).getPrice());
        Assert.assertEquals(clOrdId, fills.get(0).getClOrdId());

        Assert.assertEquals(-20, fills.get(1).getQuantity());
        Assert.assertEquals(askPrice, fills.get(1).getPrice());
        Assert.assertEquals(clOrdId, fills.get(1).getClOrdId());

        Assert.assertEquals(-10, fills.get(2).getQuantity());
        Assert.assertEquals(askPrice, fills.get(2).getPrice());
        Assert.assertEquals(clOrdId, fills.get(2).getClOrdId());

        Assert.assertEquals(-40, fills.get(3).getQuantity());
        Assert.assertEquals(askPrice, fills.get(3).getPrice());
        Assert.assertEquals(clOrdId, fills.get(3).getClOrdId());

        Assert.assertEquals(-100, innerMarket.getPosition());
    }

    @Test
    public void testAtAloneTopOfAskALotOfQty() throws InterruptedException, TradingException {
        fillCounterLatch = 1;
        acceptedCounterLatch = 1;

        final BigDecimal askPrice = new BigDecimal(9);
        final int askQuantity = 100;
        final SimulationClOrdId clOrdId = getNextClOrdId();
        simulationMarket.placeOrder(clOrdId, new OrderSpecification(SYMBOL, askPrice, askQuantity, TradeType.SELL));

        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 3000, new BigDecimal(10), getNextDate()));

        endSimulation();

        Assert.assertEquals(-100, fills.get(0).getQuantity());
        Assert.assertEquals(askPrice, fills.get(0).getPrice());
        Assert.assertEquals(clOrdId, fills.get(0).getClOrdId());

        Assert.assertEquals(-100, innerMarket.getPosition());
    }

    @Test
    public void testAtTopAloneAskThenBumpedDown() throws InterruptedException, TradingException {
        fillCounterLatch = 1;
        acceptedCounterLatch = 1;

        final BigDecimal askPrice = new BigDecimal(10);
        final int askQuantity = 100;
        final SimulationClOrdId clOrdId = getNextClOrdId();
        simulationMarket.placeOrder(clOrdId, new OrderSpecification(SYMBOL, askPrice, askQuantity, TradeType.SELL));

        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 50, new BigDecimal(11), getNextDate()));
        simulationMarket.onAsk(BookEntryFactory.createActualAsk("BetterAsk", SYMBOL, 10, new BigDecimal(9), getNextDate()));
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid2", SYMBOL, 50, new BigDecimal(11), getNextDate()));

        endSimulation();

        Assert.assertEquals(-50, fills.get(0).getQuantity());
        Assert.assertEquals(askPrice, fills.get(0).getPrice());
        Assert.assertEquals(clOrdId, fills.get(0).getClOrdId());

        Assert.assertEquals(-50, innerMarket.getPosition());
    }

    @Test
    public void testIdentifiedTradeAsk() throws TradingException, InterruptedException {
        fillCounterLatch = 1;
        acceptedCounterLatch = 1;

        final BigDecimal bidPrice = new BigDecimal(10);
        final int bidQuantity = 50;
        final SimulationClOrdId clOrdId = getNextClOrdId();
        simulationMarket.placeOrder(clOrdId, new OrderSpecification(SYMBOL, bidPrice, bidQuantity, TradeType.BUY));

        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 50, new BigDecimal(9), getNextDate()));
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 0, new BigDecimal(9), getNextDate()));

        simulationMarket.onTradeTick(new TradeTick(SYMBOL, 50, new BigDecimal(9), getNextDate()));

        endSimulation();

        Assert.assertEquals(bidQuantity, fills.get(0).getQuantity());
        Assert.assertEquals(bidPrice, fills.get(0).getPrice());
        Assert.assertEquals(clOrdId, fills.get(0).getClOrdId());

        Assert.assertEquals(bidQuantity, innerMarket.getPosition());
    }

    @Test
    public void testRemoveLiquidityOnBid() throws TradingException, InterruptedException {
        fillCounterLatch = 1;
        acceptedCounterLatch = 1;

        final BigDecimal fillPrice = BigDecimal.TEN;
        simulationMarket.onAsk(BookEntryFactory.createActualAsk("Ask1", SYMBOL, 100, fillPrice, getNextDate()));

        final BigDecimal bidPrice = new BigDecimal(11);
        final int bidQuantity = 100;
        final SimulationClOrdId clOrdId = getNextClOrdId();
        simulationMarket.placeOrder(clOrdId, new OrderSpecification(SYMBOL, bidPrice, bidQuantity, TradeType.BUY));

        endSimulation();

        Assert.assertEquals(bidQuantity, fills.get(0).getQuantity());
        Assert.assertEquals(fillPrice, fills.get(0).getPrice());
        Assert.assertEquals(clOrdId, fills.get(0).getClOrdId());

        Assert.assertEquals(bidQuantity, innerMarket.getPosition());
    }

    @Test
    public void testRemoveLiquidityOnAsk() throws TradingException, InterruptedException {
        fillCounterLatch = 1;
        acceptedCounterLatch = 1;

        final BigDecimal fillPrice = BigDecimal.TEN;
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 100, BigDecimal.TEN, getNextDate()));

        final BigDecimal askPrice = new BigDecimal(9);
        final int askQuantity = 100;
        final SimulationClOrdId clOrdId = getNextClOrdId();
        simulationMarket.placeOrder(clOrdId, new OrderSpecification(SYMBOL, askPrice, askQuantity, TradeType.SELL));

        endSimulation();

        Assert.assertEquals(askQuantity * -1, fills.get(0).getQuantity());
        Assert.assertEquals(fillPrice, fills.get(0).getPrice());
        Assert.assertEquals(clOrdId, fills.get(0).getClOrdId());

        Assert.assertEquals(askQuantity * -1, innerMarket.getPosition());
    }

    @Test
    public void testIdentifiedTradeAskWillFill() throws TradingException, InterruptedException {
        fillCounterLatch = 1;
        acceptedCounterLatch = 1;

        final BigDecimal bidPrice = new BigDecimal(10);
        final int bidQuantity = 50;
        simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, bidPrice, bidQuantity, TradeType.BUY));

        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 50, new BigDecimal(9), getNextDate()));
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 0, new BigDecimal(9), getNextDate()));

        simulationMarket.onTradeTick(new TradeTick(SYMBOL, 50, new BigDecimal(9), getNextDate()));

        endSimulation();
        Assert.assertEquals(50, innerMarket.getPosition());
    }

    @Test
    public void testIdentifiedTradeAskWillNotFill() throws TradingException, InterruptedException {
        fillCounterLatch = 0;
        acceptedCounterLatch = 1;

        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 50, new BigDecimal(9), getNextDate()));

        final BigDecimal bidPrice = new BigDecimal(9);
        final int bidQuantity = 50;
        simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, bidPrice, bidQuantity, TradeType.BUY));

        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 0, new BigDecimal(9), getNextDate()));

        simulationMarket.onTradeTick(new TradeTick(SYMBOL, 50, new BigDecimal(10), getNextDate()));

        endSimulation();
        Assert.assertEquals(0, innerMarket.getPosition());
    }

    @Test
    public void testCancelOrderMakeSureNoFills() throws TradingException, InterruptedException {
        fillCounterLatch = 0;
        acceptedCounterLatch = 1;

        final BigDecimal bidPrice = new BigDecimal(9);
        final int bidQuantity = 50;
        final ClOrdId clOrdId = getNextClOrdId();
        simulationMarket.placeOrder(clOrdId, new OrderSpecification(SYMBOL, bidPrice, bidQuantity, TradeType.BUY));

        simulationMarket.cancelOrder(getNextClOrdId(), clOrdId);

        simulationMarket.onAsk(BookEntryFactory.createActualAsk("Ask1", SYMBOL, 50, new BigDecimal(8), getNextDate()));

        endSimulation();
        Assert.assertEquals(0, innerMarket.getPosition());
    }

    @Test
    public void testReplacePartiallyFilledBidThenCompletelyFilling() throws TradingException, InterruptedException {
        fillCounterLatch = 2;
        acceptedCounterLatch = 2;

        final BigDecimal bidPrice = new BigDecimal(9);
        final int bidQuantity = 50;
        // Place and partially fill order such that it has one share remaining open.
        final SimulationClOrdId clOrdId = getNextClOrdId();
        simulationMarket.placeOrder(clOrdId, new OrderSpecification(SYMBOL, bidPrice, bidQuantity, TradeType.BUY));
        simulationMarket.onAsk(BookEntryFactory.createActualAsk("Ask1", SYMBOL, 49, new BigDecimal(7), getNextDate()));

        // Cancel-replace open order
        final SimulationClOrdId cancelReplaceClOrdId = getNextClOrdId();
        simulationMarket.cancelReplaceOrder(cancelReplaceClOrdId, clOrdId, new OrderSpecification(SYMBOL, bidPrice, 52, TradeType.BUY));
        //order now has 3 remaining shares 52-49 = 3;

        simulationMarket.onAsk(BookEntryFactory.createActualAsk("Ask2", SYMBOL, 25, new BigDecimal(7), getNextDate()));

        endSimulation();

        Assert.assertEquals(49, fills.get(0).getQuantity());
        Assert.assertEquals(1, fills.get(0).getRemaining());
        Assert.assertEquals(3, fills.get(1).getQuantity());
        Assert.assertEquals(0, fills.get(1).getRemaining());
        Assert.assertEquals(bidPrice, fills.get(0).getPrice());
        Assert.assertEquals(clOrdId, fills.get(0).getClOrdId());
        Assert.assertEquals(cancelReplaceClOrdId, fills.get(1).getClOrdId());

        Assert.assertEquals(52, innerMarket.getPosition());
    }

    @Test
    public void testReplacePartiallyFilledBidThenPartiallyFillingMore() throws TradingException, InterruptedException {
        fillCounterLatch = 2;
        acceptedCounterLatch = 2;

        final BigDecimal bidPrice = new BigDecimal(9);
        final int bidQuantity = 50;
        // Place and partially fill order such that it has one share remaining open.
        final SimulationClOrdId clOrdId = getNextClOrdId();
        simulationMarket.placeOrder(clOrdId, new OrderSpecification(SYMBOL, bidPrice, bidQuantity, TradeType.BUY));
        simulationMarket.onAsk(BookEntryFactory.createActualAsk("Ask1", SYMBOL, 49, new BigDecimal(7), getNextDate()));

        // Cancel-replace open order
        final SimulationClOrdId cancelReplaceClOrdId = getNextClOrdId();
        simulationMarket.cancelReplaceOrder(cancelReplaceClOrdId, clOrdId, new OrderSpecification(SYMBOL, bidPrice, 100, TradeType.BUY));
        //order now has 51 remaining shares 100-49 = 51;

        simulationMarket.onAsk(BookEntryFactory.createActualAsk("Ask2", SYMBOL, 25, new BigDecimal(7), getNextDate()));

        endSimulation();

        Assert.assertEquals(49, fills.get(0).getQuantity());
        Assert.assertEquals(1, fills.get(0).getRemaining());
        Assert.assertEquals(25, fills.get(1).getQuantity());
        Assert.assertEquals(26, fills.get(1).getRemaining());
        Assert.assertEquals(bidPrice, fills.get(0).getPrice());
        Assert.assertEquals(clOrdId, fills.get(0).getClOrdId());
        Assert.assertEquals(cancelReplaceClOrdId, fills.get(1).getClOrdId());

        Assert.assertEquals(74, innerMarket.getPosition());
    }

    @Test
    public void testReplacePartiallyFilledBidWhenReplacementQuantityIsLessThanFilledQuantity() throws TradingException, InterruptedException {
        fillCounterLatch = 1;
        acceptedCounterLatch = 1;
        cancelReplaceRejectedLatch = 1;

        final BigDecimal bidPrice = new BigDecimal(9);
        int bidQuantity = 50;
        // Place and partially fill order
        final SimulationClOrdId clOrdId = getNextClOrdId();
        simulationMarket.placeOrder(clOrdId, new OrderSpecification(SYMBOL, bidPrice, bidQuantity, TradeType.BUY));
        simulationMarket.onAsk(BookEntryFactory.createActualAsk("Ask1", SYMBOL, 25, new BigDecimal(7), getNextDate()));
        //25 shares remaining
        // Cancel-replace open order
        bidQuantity = 24;
        final SimulationClOrdId cancelReplaceClOrdId = getNextClOrdId();
        simulationMarket.cancelReplaceOrder(cancelReplaceClOrdId, clOrdId, new OrderSpecification(SYMBOL, bidPrice, bidQuantity, TradeType.BUY));

        endSimulation();

        Assert.assertEquals(25, innerMarket.getPosition());
    }

    @Test
    public void testReplacePartiallyFilledAskThenCompletelyFilling() throws TradingException, InterruptedException {
        fillCounterLatch = 2;
        acceptedCounterLatch = 2;

        final BigDecimal askPrice = new BigDecimal(7);
        final int askQuantity = 50;
        // Place and partially fill order such that it has one share remaining open.
        final SimulationClOrdId clOrdId = getNextClOrdId();
        simulationMarket.placeOrder(clOrdId, new OrderSpecification(SYMBOL, askPrice, askQuantity, TradeType.SELL));
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 49, new BigDecimal(9), getNextDate()));

        // Cancel-replace open order
        final SimulationClOrdId cancelReplaceClOrdId = getNextClOrdId();
        simulationMarket.cancelReplaceOrder(cancelReplaceClOrdId, clOrdId, new OrderSpecification(SYMBOL, askPrice, 52, TradeType.SELL));
        //order now has 3 remaining shares 52-49 = 3;

        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid2", SYMBOL, 25, new BigDecimal(9), getNextDate()));
        endSimulation();

        Assert.assertEquals(-49, fills.get(0).getQuantity());
        Assert.assertEquals(1, fills.get(0).getRemaining());
        Assert.assertEquals(-3, fills.get(1).getQuantity());
        Assert.assertEquals(0, fills.get(1).getRemaining());
        Assert.assertEquals(askPrice, fills.get(0).getPrice());
        Assert.assertEquals(clOrdId, fills.get(0).getClOrdId());
        Assert.assertEquals(cancelReplaceClOrdId, fills.get(1).getClOrdId());

        Assert.assertEquals(-52, innerMarket.getPosition());
    }

    @Test
    public void testReplacePartiallyFilledAskThenPartiallyFillingMore() throws TradingException, InterruptedException {
        fillCounterLatch = 2;
        acceptedCounterLatch = 2;

        final BigDecimal askPrice = new BigDecimal(7);
        final int askQuantity = 50;
        // Place and partially fill order such that it has one share remaining open.
        final SimulationClOrdId clOrdId = getNextClOrdId();
        simulationMarket.placeOrder(clOrdId, new OrderSpecification(SYMBOL, askPrice, askQuantity, TradeType.SELL));
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 49, new BigDecimal(9), getNextDate()));

        // Cancel-replace open order
        final SimulationClOrdId cancelReplaceClOrdId = getNextClOrdId();
        simulationMarket.cancelReplaceOrder(cancelReplaceClOrdId, clOrdId, new OrderSpecification(SYMBOL, askPrice, 100, TradeType.SELL));
        //order now has 51 remaining shares 100-49 = 51;

        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid2", SYMBOL, 25, new BigDecimal(9), getNextDate()));
        endSimulation();

        Assert.assertEquals(-49, fills.get(0).getQuantity());
        Assert.assertEquals(1, fills.get(0).getRemaining());
        Assert.assertEquals(-25, fills.get(1).getQuantity());
        Assert.assertEquals(26, fills.get(1).getRemaining());
        Assert.assertEquals(askPrice, fills.get(0).getPrice());
        Assert.assertEquals(clOrdId, fills.get(0).getClOrdId());
        Assert.assertEquals(cancelReplaceClOrdId, fills.get(1).getClOrdId());

        Assert.assertEquals(-74, innerMarket.getPosition());
    }

    @Test
    public void testReplacePartiallyFilledAskWhenReplacementQuantityIsLessThanFilledQuantity() throws TradingException, InterruptedException {
        fillCounterLatch = 1;
        acceptedCounterLatch = 1;
        cancelReplaceRejectedLatch = 1;

        final BigDecimal askPrice = new BigDecimal(7);
        int askQuantity = 50;
        // Place and partially fill order such that it has one share remaining open.
        final SimulationClOrdId clOrdId = getNextClOrdId();
        simulationMarket.placeOrder(clOrdId, new OrderSpecification(SYMBOL, askPrice, askQuantity, TradeType.SELL));
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 25, new BigDecimal(9), getNextDate()));
        // Cancel-replace open order and partially fill it.
        askQuantity = 24;
        final SimulationClOrdId cancelReplaceClOrdId = getNextClOrdId();
        simulationMarket
        .cancelReplaceOrder(cancelReplaceClOrdId, clOrdId, new OrderSpecification(SYMBOL, askPrice, askQuantity, TradeType.SELL));

        endSimulation();

        Assert.assertEquals(-25, innerMarket.getPosition());
    }

    @Test
    public void testMultipleIdentifiedTradesFilling() throws TradingException, InterruptedException {
        fillCounterLatch = 3;
        acceptedCounterLatch = 1;

        final BigDecimal bidPrice = new BigDecimal(10);
        final int bidQuantity = 200;
        simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, bidPrice, bidQuantity, TradeType.BUY));

        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 50, new BigDecimal(9), getNextDate()));
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 0, new BigDecimal(9), getNextDate()));
        simulationMarket.onTradeTick(new TradeTick(SYMBOL, 50, new BigDecimal(9), getNextDate()));

        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid2", SYMBOL, 50, new BigDecimal(9), getNextDate()));
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid2", SYMBOL, 0, new BigDecimal(9), getNextDate()));
        simulationMarket.onTradeTick(new TradeTick(SYMBOL, 50, new BigDecimal(9), getNextDate()));

        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid3", SYMBOL, 50, new BigDecimal(9), getNextDate()));
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid3", SYMBOL, 0, new BigDecimal(9), getNextDate()));
        simulationMarket.onTradeTick(new TradeTick(SYMBOL, 50, new BigDecimal(9), getNextDate()));

        endSimulation();
        
        Assert.assertEquals(150, innerMarket.getPosition());
    }

    @Test
    public void testMultipleIdentifiedTradesFillingWorsePrice() throws TradingException, InterruptedException {
        fillCounterLatch = 3;
        acceptedCounterLatch = 1;

        final BigDecimal bidPrice = new BigDecimal(9);
        final int bidQuantity = 200;
        final SimulationClOrdId clOrdId = getNextClOrdId();
        simulationMarket.placeOrder(clOrdId, new OrderSpecification(SYMBOL, bidPrice, bidQuantity, TradeType.BUY));

        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 50, new BigDecimal(8), getNextDate()));
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 0, new BigDecimal(8), getNextDate()));
        simulationMarket.onTradeTick(new TradeTick(SYMBOL, 50, new BigDecimal(8), getNextDate()));

        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid2", SYMBOL, 50, new BigDecimal(7), getNextDate()));
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid2", SYMBOL, 0, new BigDecimal(7), getNextDate()));
        simulationMarket.onTradeTick(new TradeTick(SYMBOL, 50, new BigDecimal(7), getNextDate()));

        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid3", SYMBOL, 50, new BigDecimal(6), getNextDate()));
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid3", SYMBOL, 0, new BigDecimal(6), getNextDate()));
        simulationMarket.onTradeTick(new TradeTick(SYMBOL, 50, new BigDecimal(6), getNextDate()));

        endSimulation();

        Assert.assertEquals(50, fills.get(0).getQuantity());
        Assert.assertEquals(bidPrice, fills.get(0).getPrice());
        Assert.assertEquals(clOrdId, fills.get(0).getClOrdId());

        Assert.assertEquals(50, fills.get(1).getQuantity());
        Assert.assertEquals(bidPrice, fills.get(1).getPrice());
        Assert.assertEquals(clOrdId, fills.get(1).getClOrdId());

        Assert.assertEquals(50, fills.get(2).getQuantity());
        Assert.assertEquals(bidPrice, fills.get(2).getPrice());
        Assert.assertEquals(clOrdId, fills.get(2).getClOrdId());

        Assert.assertEquals(150, innerMarket.getPosition());
    }

    @Test
    public void testIncomingAskFillingMultipleSimulatedBids() throws TradingException, InterruptedException {
        fillCounterLatch = 2;
        acceptedCounterLatch = 2;
        final BigDecimal bidPrice = new BigDecimal(9);
        final int bidQuantity = 50;

        // Place two simulated orders such that incoming ask fills both orders.
        final SimulationClOrdId firstClOrdId = getNextClOrdId();
        final SimulationClOrdId secondClOrdId = getNextClOrdId();
        simulationMarket.placeOrder(firstClOrdId, new OrderSpecification(SYMBOL, bidPrice, bidQuantity, TradeType.BUY));
        simulationMarket.placeOrder(secondClOrdId, new OrderSpecification(SYMBOL, bidPrice, bidQuantity, TradeType.BUY));
        simulationMarket.onAsk(BookEntryFactory.createActualAsk("Ask1", SYMBOL, 120, new BigDecimal(7), getNextDate()));

        endSimulation();

        Assert.assertEquals(2, fills.size());
        Assert.assertEquals(50, fills.get(0).getQuantity());
        Assert.assertEquals(50, fills.get(1).getQuantity());
        Assert.assertEquals(firstClOrdId, fills.get(0).getClOrdId());
        Assert.assertEquals(secondClOrdId, fills.get(1).getClOrdId());

        Assert.assertEquals(100, innerMarket.getPosition());
    }

    @Test
    public void testIncomingBidFillingMultipleSimulatedAsks() throws TradingException, InterruptedException {
        fillCounterLatch = 2;
        acceptedCounterLatch = 2;
        final BigDecimal askPrice = new BigDecimal(7);
        final int bidQuantity = 50;

        // Place two simulated orders such that incoming bid fills both orders.
        final SimulationClOrdId firstClOrdId = getNextClOrdId();
        final SimulationClOrdId secondClOrdId = getNextClOrdId();
        simulationMarket.placeOrder(firstClOrdId, new OrderSpecification(SYMBOL, askPrice, bidQuantity, TradeType.SELL));
        simulationMarket.placeOrder(secondClOrdId, new OrderSpecification(SYMBOL, askPrice, bidQuantity, TradeType.SELL));
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 120, new BigDecimal(9), getNextDate()));

        endSimulation();

        Assert.assertEquals(2, fills.size());
        Assert.assertEquals(-50, fills.get(0).getQuantity());
        Assert.assertEquals(-50, fills.get(1).getQuantity());
        Assert.assertEquals(firstClOrdId, fills.get(0).getClOrdId());
        Assert.assertEquals(secondClOrdId, fills.get(1).getClOrdId());

        Assert.assertEquals(-100, innerMarket.getPosition());
    }

    @Test
    public void testIncomingTradeTickFillingMultipleSimulatedBids() throws TradingException, InterruptedException {
        fillCounterLatch = 2;
        acceptedCounterLatch = 2;
        final BigDecimal bidPrice = new BigDecimal(9);
        final int bidQuantity = 50;

        // Place two simulated orders such that incoming trade tick fills both orders.
        final SimulationClOrdId firstClOrdId = getNextClOrdId();
        final SimulationClOrdId secondClOrdId = getNextClOrdId();
        simulationMarket.placeOrder(firstClOrdId, new OrderSpecification(SYMBOL, bidPrice, bidQuantity, TradeType.BUY));
        simulationMarket.placeOrder(secondClOrdId, new OrderSpecification(SYMBOL, bidPrice, bidQuantity, TradeType.BUY));
        // Set bid to have lower price than simulated orders to ensure simulated orders are filled.
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 120, new BigDecimal(8), getNextDate()));
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 0, new BigDecimal(8), getNextDate()));

        simulationMarket.onTradeTick(new TradeTick(SYMBOL, 120, new BigDecimal(8), getNextDate()));
        endSimulation();

        Assert.assertEquals(2, fills.size());
        Assert.assertEquals(50, fills.get(0).getQuantity());
        Assert.assertEquals(50, fills.get(1).getQuantity());
        Assert.assertEquals(firstClOrdId, fills.get(0).getClOrdId());
        Assert.assertEquals(secondClOrdId, fills.get(1).getClOrdId());

        Assert.assertEquals(100, innerMarket.getPosition());
    }

    @Test
    public void testIncomingTradeTickFillingMultipleSimulatedAsks() throws TradingException, InterruptedException {
        fillCounterLatch = 2;
        acceptedCounterLatch = 2;
        final BigDecimal askPrice = new BigDecimal(7);
        final int bidQuantity = 50;

        // Place two simulated orders such that incoming trade tick fills both orders.
        final SimulationClOrdId firstClOrdId = getNextClOrdId();
        final SimulationClOrdId secondClOrdId = getNextClOrdId();
        simulationMarket.placeOrder(firstClOrdId, new OrderSpecification(SYMBOL, askPrice, bidQuantity, TradeType.SELL));
        simulationMarket.placeOrder(secondClOrdId, new OrderSpecification(SYMBOL, askPrice, bidQuantity, TradeType.SELL));

        // Set ask to have higher price than simulated orders to ensure simulated orders are filled.
        simulationMarket.onAsk(BookEntryFactory.createActualAsk("Ask1", SYMBOL, 120, new BigDecimal(8), getNextDate()));
        simulationMarket.onAsk(BookEntryFactory.createActualAsk("Ask1", SYMBOL, 0, new BigDecimal(8), getNextDate()));

        simulationMarket.onTradeTick(new TradeTick(SYMBOL, 120, new BigDecimal(8), getNextDate()));
        endSimulation();

        Assert.assertEquals(2, fills.size());
        Assert.assertEquals(-50, fills.get(0).getQuantity());
        Assert.assertEquals(-50, fills.get(1).getQuantity());
        Assert.assertEquals(firstClOrdId, fills.get(0).getClOrdId());
        Assert.assertEquals(secondClOrdId, fills.get(1).getClOrdId());

        Assert.assertEquals(-100, innerMarket.getPosition());
    }

    @Test
    public void simulatedMarketAskShouldRemoveLiquidity() throws TradingException, InterruptedException {
        fillCounterLatch = 1;
        acceptedCounterLatch = 1;
        final BigDecimal askPrice = BigDecimal.ZERO;
        final int askQuantity = 50;

        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 50, new BigDecimal(8), getNextDate()));
        simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, askPrice, askQuantity, TradeType.SELL));
        endSimulation();

        Assert.assertEquals(1, fills.size());
        Assert.assertEquals(askQuantity * -1, fills.get(0).getQuantity());

        Assert.assertEquals(askQuantity * -1, innerMarket.getPosition());
    }

    @Test
    public void simulatedMarketAskShouldFillAllIncomingBids() throws TradingException, InterruptedException {
        fillCounterLatch = 2;
        acceptedCounterLatch = 1;
        final BigDecimal askPrice = BigDecimal.ZERO;
        final int askQuantity = 100;

        simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, askPrice, askQuantity, TradeType.SELL));
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 50, new BigDecimal(8), getNextDate()));
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 50, new BigDecimal(1), getNextDate()));
        endSimulation();

        Assert.assertEquals(2, fills.size());
        Assert.assertEquals(-50, fills.get(0).getQuantity());
        Assert.assertEquals(-50, fills.get(1).getQuantity());

        Assert.assertEquals(-100, innerMarket.getPosition());
    }

    @Test
    public void simulatedMarketAskShouldFillAtTopOfBookPrice() {
        fillCounterLatch = 1;
        acceptedCounterLatch = 1;

        final BigDecimal fillPrice = BigDecimal.TEN;
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 50, fillPrice, getNextDate()));

        simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, BigDecimal.ZERO, 100, TradeType.SELL));

        endSimulation();

        Assert.assertEquals(fillPrice, fills.get(0).getPrice());

        Assert.assertEquals(-50, innerMarket.getPosition());
    }

    @Test
    public void simulatedMarketBidShouldRemoveLiquidity() throws TradingException, InterruptedException {
        fillCounterLatch = 1;
        acceptedCounterLatch = 1;
        final BigDecimal bidPrice = BigDecimal.ZERO;
        final int bidQuantity = 50;

        simulationMarket.onAsk(BookEntryFactory.createActualAsk("Ask1", SYMBOL, 50, new BigDecimal(8), getNextDate()));
        simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, bidPrice, bidQuantity, TradeType.BUY));
        endSimulation();

        Assert.assertEquals(1, fills.size());
        Assert.assertEquals(bidQuantity, fills.get(0).getQuantity());

        Assert.assertEquals(bidQuantity, innerMarket.getPosition());
    }

    @Test
    public void simulatedMarketBidShouldFillAllIncomingAsks() throws TradingException, InterruptedException {
        fillCounterLatch = 2;
        acceptedCounterLatch = 1;

        final BigDecimal bidPrice = BigDecimal.ZERO;
        final int bidQuantity = 100;

        simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, bidPrice, bidQuantity, TradeType.BUY));
        simulationMarket.onAsk(BookEntryFactory.createActualAsk("Ask1", SYMBOL, 50, new BigDecimal(8), getNextDate()));
        simulationMarket.onAsk(BookEntryFactory.createActualAsk("Ask2", SYMBOL, 50, new BigDecimal(8), getNextDate()));
        endSimulation();

        Assert.assertEquals(2, fills.size());
        Assert.assertEquals(50, fills.get(0).getQuantity());
        Assert.assertEquals(50, fills.get(1).getQuantity());

        Assert.assertEquals(100, innerMarket.getPosition());
    }

    @Test
    public void simulatedMarketBidShouldFillAtTopOfBookPrice() {
        fillCounterLatch = 1;
        acceptedCounterLatch = 1;

        final BigDecimal fillPrice = BigDecimal.TEN;
        simulationMarket.onAsk(BookEntryFactory.createActualAsk("Ask1", SYMBOL, 100, fillPrice, getNextDate()));

        simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, BigDecimal.ZERO, 100, TradeType.BUY));

        endSimulation();

        Assert.assertEquals(fillPrice, fills.get(0).getPrice());

        Assert.assertEquals(100, innerMarket.getPosition());
    }

    @Test
    public void sendOrderThenCancelThenSendOrder() throws TradingException, InterruptedException {
        fillCounterLatch = 0;
        acceptedCounterLatch = 2;

        final int bidQuantity = 100;
        final ClOrdId firstClOrdId = getNextClOrdId();

        simulationMarket.placeOrder(firstClOrdId, new OrderSpecification(SYMBOL, new BigDecimal(10), bidQuantity, TradeType.BUY));
        simulationMarket.cancelOrder(getNextClOrdId(), firstClOrdId);

        simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, new BigDecimal(9), bidQuantity, TradeType.SELL));
        endSimulation();
    }

    @Test
    public void loadPositionAcrossMultipleTradingSessions() {
        fillCounterLatch = 1;
        acceptedCounterLatch = 1;

        final BigDecimal bidPrice = BigDecimal.ZERO;
        final int bidQuantity = 100;

        simulationMarket.placeOrder(getNextClOrdId(), new OrderSpecification(SYMBOL, bidPrice, bidQuantity, TradeType.BUY));
        simulationMarket.onAsk(BookEntryFactory.createActualAsk("Ask1", SYMBOL, 100, new BigDecimal(8), getNextDate()));
        endSimulation();

        innerMarket = new DefaultSimulationMarket(innerMarket);
        simulationMarket = SimulationMarketLatencyProxy.createSimulationMarketLatencyProxy(innerMarket, new DefaultLatencyProfile());

        Assert.assertEquals(100, innerMarket.getPosition());
    }
    
    @Test
    public void multiplePartialFillsMaintainTimePriority() {
    	fillCounterLatch = 2;
        acceptedCounterLatch = 2;
        final BigDecimal askPrice = new BigDecimal(10);
        final int askQuantity = 50;
        
        // Place two simulated orders
        final SimulationClOrdId firstClOrdId = getNextClOrdId();
        final SimulationClOrdId secondClOrdId = getNextClOrdId();
        simulationMarket.placeOrder(firstClOrdId, new OrderSpecification(SYMBOL, askPrice, askQuantity, TradeType.SELL));
        simulationMarket.placeOrder(secondClOrdId, new OrderSpecification(SYMBOL, askPrice, askQuantity, TradeType.SELL));
        
        // Generate fills
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 20, new BigDecimal(11), getNextDate()));
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid2", SYMBOL, 15, new BigDecimal(11), getNextDate()));
        endSimulation();
        
        // Assert only first order gets fills
        Assert.assertEquals(2, fills.size());
        Assert.assertEquals(-20, fills.get(0).getQuantity());
        Assert.assertEquals(-15, fills.get(1).getQuantity());
        Assert.assertEquals(firstClOrdId, fills.get(0).getClOrdId());
        Assert.assertEquals(firstClOrdId, fills.get(1).getClOrdId());
        
        Assert.assertEquals(-35, innerMarket.getPosition());
    }
    
    @Test
    public void cancelReplacingLessQuantityRetainsPriority() {
    	fillCounterLatch = 1;
        acceptedCounterLatch = 3;
        final BigDecimal askPrice = new BigDecimal(10);
        final int askQuantity = 50;
        
        // Place simulated orders
        final SimulationClOrdId firstClOrdId = getNextClOrdId();
        final SimulationClOrdId secondClOrdId = getNextClOrdId();
        simulationMarket.placeOrder(firstClOrdId, new OrderSpecification(SYMBOL, askPrice, askQuantity, TradeType.SELL));
        simulationMarket.setCurrentTime(getNextDate()); // necessary because we don't correctly handle the case where all of the times are exactly the same
        simulationMarket.placeOrder(secondClOrdId, new OrderSpecification(SYMBOL, askPrice, askQuantity, TradeType.SELL));
        
        // Cancel replace firstClOrdId with less quantity
        final SimulationClOrdId cancelReplaceClOrdId = getNextClOrdId();
        simulationMarket.cancelReplaceOrder(cancelReplaceClOrdId, firstClOrdId, new OrderSpecification(SYMBOL, askPrice, askQuantity - 1, TradeType.SELL));
        
        // Generate fill
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 20, new BigDecimal(11), getNextDate()));
        endSimulation();
        
        // Assert only first order gets fills
        Assert.assertEquals(1, fills.size());
        Assert.assertEquals(-20, fills.get(0).getQuantity());
        Assert.assertEquals(cancelReplaceClOrdId, fills.get(0).getClOrdId());
        
        Assert.assertEquals(-20, innerMarket.getPosition());
    }
    
    @Test
    public void cancelReplacingLessQuantityDoesntOverfill() {
    	fillCounterLatch = 1;
        acceptedCounterLatch = 2;
        final BigDecimal askPrice = new BigDecimal(10);
        final int askQuantity = 50;
        
        // Place simulated order
        final SimulationClOrdId firstClOrdId = getNextClOrdId();
        simulationMarket.placeOrder(firstClOrdId, new OrderSpecification(SYMBOL, askPrice, askQuantity, TradeType.SELL));
        
        // Cancel replace firstClOrdId with less quantity
        final SimulationClOrdId cancelReplaceClOrdId = getNextClOrdId();
        simulationMarket.cancelReplaceOrder(cancelReplaceClOrdId, firstClOrdId, new OrderSpecification(SYMBOL, askPrice, askQuantity - 1, TradeType.SELL));
        
        // Generate fill
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, askQuantity + 100, new BigDecimal(11), getNextDate()));
        endSimulation();
        
        // Assert only first order gets fills
        Assert.assertEquals(1, fills.size());
        Assert.assertEquals(-1 * (askQuantity - 1), fills.get(0).getQuantity());
        Assert.assertEquals(cancelReplaceClOrdId, fills.get(0).getClOrdId());
        
        Assert.assertEquals(-1 * (askQuantity - 1), innerMarket.getPosition());
    }
    
    @Test
    public void cancelReplacingGreaterQuantityLosesPriority() {
    	fillCounterLatch = 1;
        acceptedCounterLatch = 3;
        final BigDecimal askPrice = new BigDecimal(10);
        final int askQuantity = 50;
        
        // Place simulated order, then cancel replace
        final SimulationClOrdId firstClOrdId = getNextClOrdId();
        final SimulationClOrdId secondClOrdId = getNextClOrdId();
        simulationMarket.placeOrder(firstClOrdId, new OrderSpecification(SYMBOL, askPrice, askQuantity, TradeType.SELL));
        simulationMarket.setCurrentTime(getNextDate()); // necessary because we don't correctly handle the case where all of the times are exactly the same
        simulationMarket.placeOrder(secondClOrdId, new OrderSpecification(SYMBOL, askPrice, askQuantity, TradeType.SELL));
        
        // Cancel replace firstClOrdId with greater quantity
        final SimulationClOrdId cancelReplaceClOrdId = getNextClOrdId();
        simulationMarket.cancelReplaceOrder(cancelReplaceClOrdId, firstClOrdId, new OrderSpecification(SYMBOL, askPrice, askQuantity + 10, TradeType.SELL));
                
        // Generate fill
        simulationMarket.onBid(BookEntryFactory.createActualBid("Bid1", SYMBOL, 20, new BigDecimal(11), getNextDate()));
        endSimulation();
        
        // Assert second order gets fills
        Assert.assertEquals(1, fills.size());
        Assert.assertEquals(-20, fills.get(0).getQuantity());
        Assert.assertEquals(secondClOrdId, fills.get(0).getClOrdId());
        
        Assert.assertEquals(-20, innerMarket.getPosition());
    }
    
    @Override
    public void onCancelReplaceAccepted(final ClOrdId clOrdId, final OrderSpecification order, final ClOrdId origClOrdId) {
        Assert.assertFalse("Received more accepts than expected", acceptedCounterLatch == 0);
        acceptedCounterLatch--;
    }

    @Override
    public void onCancelReplaceRejected(final ClOrdId clOrdId) {
        Assert.assertFalse("Received more cancel-replace rejects than expected", cancelReplaceRejectedLatch == 0);
        cancelReplaceRejectedLatch--;
    }

    @Override
    public void onNewOrderAccepted(final ClOrdId clOrdId, final OrderSpecification order) {
        Assert.assertFalse("Received more accepts than expected", acceptedCounterLatch == 0);
        acceptedCounterLatch--;
    }

    @Override
    public void onNewOrderRejected(final ClOrdId clOrdId) {
        Assert.assertFalse("Received more new order rejects than expected", newOrderRejectedLatch == 0);
        newOrderRejectedLatch--;
    }
    
    @Override
    public void onCancelAccepted(final ClOrdId clOrdId, final ClOrdId origClOrdId) {
        
    }

    @Override
    public void onCancelRejected(final ClOrdId clOrdId) {
        
    }
}
