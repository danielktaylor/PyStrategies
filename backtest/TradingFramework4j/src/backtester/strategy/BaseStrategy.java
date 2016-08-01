package backtester.strategy;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;

import backtester.common.SimpleLogger;
import backtester.common.StatisticsRegistry;
import backtester.quote.Ask;
import backtester.quote.Bid;
import backtester.quote.QuoteBook;
import backtester.quote.QuoteListener;
import backtester.quote.TradeTick;
import backtester.simulation.SimulationClOrdId;
import backtester.trade.ClOrdId;
import backtester.trade.Fill;
import backtester.trade.OrderSpecification;
import backtester.trade.OrderStatus;
import backtester.trade.Position;
import backtester.trade.TradeDestination;
import backtester.trade.TradeListener;
import backtester.trade.TradeType;
import backtester.trade.TransactionCost;
import backtester.update.OrderUpdate;
import backtester.update.STATS_UPDATE_TYPE;
import backtester.update.StatsUpdate;

public abstract class BaseStrategy extends Observable implements QuoteListener, TradeListener {
	protected static final SimpleLogger _log = SimpleLogger.getLogger(BaseStrategy.class);
	protected static final NumberFormat numberFormatter = NumberFormat.getNumberInstance();
	protected static final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance();
	protected static final String NEW_LINE = "\n";

	protected final MathContext mathContext = new MathContext(8, RoundingMode.HALF_EVEN);
	
	protected StatisticsRegistry stats = StatisticsRegistry.getInstance();
	protected AtomicLong nextClOrdId = new AtomicLong();
	protected QuoteBook quoteBook = new QuoteBook();

	protected String symbol;
	protected Deque<Position> positionHistory = new LinkedList<Position>();
	protected Deque<TransactionCost> transactionCostHistory = new LinkedList<TransactionCost>();
	protected Deque<Fill> fillHistory = new LinkedList<Fill>();
	protected Map<ClOrdId, OrderSpecification> openOrders = new HashMap<ClOrdId, OrderSpecification>();
	protected Map<ClOrdId, OrderSpecification> allPlacedOrders = new HashMap<ClOrdId, OrderSpecification>();
	protected Map<ClOrdId, ClOrdId> allPlacedCancelOrders = new HashMap<ClOrdId, ClOrdId>();
	protected Map<ClOrdId, OrderSpecification> openBidOrders = new HashMap<ClOrdId, OrderSpecification>();
	protected Map<ClOrdId, OrderSpecification> openAskOrders = new HashMap<ClOrdId, OrderSpecification>();
	protected TransactionCost transactionCostTotal = new TransactionCost(BigDecimal.ZERO);
	protected Integer orderCount = Integer.valueOf(0);
	protected BigDecimal maximumExposure = BigDecimal.ZERO;
	protected BigDecimal drawDown = BigDecimal.ZERO;
	protected BigDecimal maxDrawDown = BigDecimal.ZERO;

	protected boolean firstMarketAction = true;
	protected Calendar startOfTrading; // 9:30:00
	protected Date currentMarketDate = new Date(0);

	private final TradeDestination tradeDestination;
	private boolean hasAnyObservers;

	protected BaseStrategy(final BaseStrategy baseStrategy, final StrategyConfiguration strategyConfiguration) {
		this.tradeDestination = strategyConfiguration.getTradeDestination();
		
		startOfTrading = Calendar.getInstance();
		startOfTrading.setTime(strategyConfiguration.getTradingDate());
		startOfTrading.set(Calendar.HOUR_OF_DAY, 9);
		startOfTrading.set(Calendar.MINUTE, 30);
		startOfTrading.set(Calendar.SECOND, 0);
		startOfTrading.set(Calendar.MILLISECOND, 0);

		if (baseStrategy != null) {
			final Position previousDayPosition = baseStrategy.getCurrentPosition();
			positionHistory
					.push(new Position(previousDayPosition.getSymbol(), previousDayPosition.getShares(), BigDecimal.ZERO, BigDecimal.ZERO));
		} else {
			positionHistory.push(new Position());
		}
		
		registerVariables();
		registerStatistics();
	}

	protected void registerVariables() {

	}

	protected void registerStatistics() {
		stats.addStatistic(STATS_UPDATE_TYPE.CLOSED_PL, this, "getClosedPL");
		stats.addStatistic(STATS_UPDATE_TYPE.SHARES_TRADED, this, "getSharesTraded");
		stats.addStatistic(STATS_UPDATE_TYPE.TOTAL_TRANSACTION_COST, this, "getTotalTransactionCost");
		stats.addStatistic(STATS_UPDATE_TYPE.OPEN_ORDER_COUNT, this, "getOpenOrderCount");
		stats.addStatistic(STATS_UPDATE_TYPE.OPEN_ORDER_VALUE, this, "getOpenOrderValue");
		stats.addStatistic(STATS_UPDATE_TYPE.ORDER_COUNT, this, "getOrderCount");
		stats.addStatistic(STATS_UPDATE_TYPE.FILL_COUNT, this, "getFillCount");
		stats.addStatistic(STATS_UPDATE_TYPE.MAXIMUM_EXPOSURE, this, "getMaximumExposure");
		stats.addStatistic(STATS_UPDATE_TYPE.MAXIMUM_DRAWDOWN, this, "getMaxDrawDown");
		stats.addStatistic(STATS_UPDATE_TYPE.SHARES_OWNED, this, "getCurrentPosition.getShares");
	}
	
	/**
	 * Callback triggered on major market actions, including receiving an ask, a bid, a trade tick, and a fill. This is a convenience method
	 * provided to each strategy to avoid overriding each individual callback. This callback is designed to be used for time-driven events, such
	 * as, determining when a particular time period has passed, which requires your strategy to take action.
	 */
	protected void onMarketAction() {
	}

	protected void onFirstMarketAction() {
	}
	
	private void updateCurrentMarketDateAndTriggerMarketActionCallback(final Date currentMarketDate) {
		if (this.currentMarketDate.compareTo(currentMarketDate) < 0) {
			this.currentMarketDate = currentMarketDate;
		}
		if (firstMarketAction) {
			firstMarketAction = false;
			onFirstMarketAction();
		}
		onMarketAction();
	}

	///----------------Quote Listener Methods----------------///
	@Override
	public final void onAsk(final Ask ask) {
		if (symbol == null) {
			symbol = ask.getSymbol();
		}
		
		quoteBook.addAsk(ask);
		updateCurrentMarketDateAndTriggerMarketActionCallback(ask.getTimestamp());

		doOnAsk(ask);

	}

	@Override
	public final void onBid(final Bid bid) {
		if (symbol == null) {
			symbol = bid.getSymbol();
		}
		
		quoteBook.addBid(bid);
		updateCurrentMarketDateAndTriggerMarketActionCallback(bid.getTimestamp());
		doOnBid(bid);

	}

	@Override
	public final void onTradeTick(final TradeTick tradeTick) {
		if (symbol == null) {
			symbol = tradeTick.getSymbol();
		}
		
		quoteBook.addTradeTick(tradeTick);
		updateCurrentMarketDateAndTriggerMarketActionCallback(tradeTick.getTimestamp());
		doOnTradeTick(tradeTick);

	}

	///----------------Trade Listener Methods----------------///
	@Override
	public final void onFill(final Fill fill) {
		updateCurrentMarketDateAndTriggerMarketActionCallback(fill.getTimestamp());

		final Position currentPosition = new Position(getCurrentPosition());

		final BigDecimal oldPL = getClosedPL();

		currentPosition.applyFill(fill);
		positionHistory.push(currentPosition);
		fillHistory.push(fill);		
		
		final BigDecimal currentPL = getClosedPL();
		if (currentPL.compareTo(oldPL) == -1) {
			drawDown = drawDown.add(oldPL.subtract(currentPL));
			if (drawDown.compareTo(maxDrawDown) == 1) {
				maxDrawDown = drawDown;
				sendChangedEvent(new StatsUpdate(getMaxDrawDown(), STATS_UPDATE_TYPE.MAXIMUM_DRAWDOWN));
			}
		} else if (currentPL.compareTo(oldPL) == 1) {
			drawDown = BigDecimal.ZERO;
		}

		if (fill.getRemaining() == 0) {
			final OrderSpecification wasOpenOrder = openOrders.remove(fill.getClOrdId());
			if (null == wasOpenOrder) {
				_log.warn("You got a fill for an order you didn't think was open to begin with!");
			} else {
				wasOpenOrder.setOrderStatus(OrderStatus.COMPLETED);
			}
			sendChangedEvent(OrderUpdate.createCompletedOrderUpdate(fill.getClOrdId(), fill));
		} else {
			final OrderSpecification partiallyFilledOrder = openOrders.get(fill.getClOrdId());
			if (null != partiallyFilledOrder) {
				partiallyFilledOrder.setOrderStatus(OrderStatus.PARTIALLY_FILLED);
			}
			sendChangedEvent(OrderUpdate.createPartiallyFilledOrderUpdate(fill.getClOrdId(), fill));
		}

		sendPositionChangedEvent();
		sendChangedEvent(new StatsUpdate(getSharesTraded(), STATS_UPDATE_TYPE.SHARES_TRADED));
		sendChangedEvent(new StatsUpdate(currentPL, STATS_UPDATE_TYPE.CLOSED_PL));
		sendChangedEvent(new StatsUpdate(fillHistory.size(), STATS_UPDATE_TYPE.FILL_COUNT));
		sendChangedEvent(new StatsUpdate(openOrders.size(), STATS_UPDATE_TYPE.OPEN_ORDER_COUNT));
		sendChangedEvent(new StatsUpdate(getCurrentPosition().getShares(), STATS_UPDATE_TYPE.SHARES_OWNED));

		doOnFill(fill);
	}

	@Override
	public final void onTransactionCost(final TransactionCost transactionCost) {
		transactionCostHistory.push(transactionCost);
		this.transactionCostTotal = new TransactionCost(transactionCostTotal.getCost().add(transactionCost.getCost()));
		sendChangedEvent(new StatsUpdate(getTotalTransactionCost(), STATS_UPDATE_TYPE.TOTAL_TRANSACTION_COST));

		doOnTransactionCost(transactionCost);
	}

	@Override
	public final void onCancelAccepted(final ClOrdId clOrdId, final ClOrdId origClOrdId) {
		final OrderSpecification wasOpenOrder = openOrders.remove(origClOrdId);
		if (null == wasOpenOrder) {
			_log.warn("Something is not right - open order not present in map waay too early. Someone is doing something wrong.");
		} else {
			wasOpenOrder.setOrderStatus(OrderStatus.CANCELED);
		}

		openBidOrders.remove(origClOrdId);
		openAskOrders.remove(origClOrdId);

		sendChangedEvent(OrderUpdate.createCancelOrderUpdate(origClOrdId));

		sendChangedEvent(new StatsUpdate(openOrders.size(), STATS_UPDATE_TYPE.OPEN_ORDER_COUNT));
		sendChangedEvent(new StatsUpdate(getOpenOrderValue(), STATS_UPDATE_TYPE.OPEN_ORDER_VALUE));

		doOnCancelAccepted(clOrdId, origClOrdId);
	}

	@Override
	public final void onCancelRejected(final ClOrdId clOrdId) {
		_log.warn("Received onCancelRejected callback.");

		final ClOrdId origOrderTriedToCancel = allPlacedCancelOrders.get(clOrdId);
		if (null != origOrderTriedToCancel) {
			final OrderSpecification openOrder = openOrders.get(origOrderTriedToCancel);
			final OrderSpecification openBidOrder = openBidOrders.get(origOrderTriedToCancel);
			final OrderSpecification openAskOrder = openAskOrders.get(origOrderTriedToCancel);

			if (null != openOrder) {
				_log.warn("The order you were trying to cancel (" + origOrderTriedToCancel + ") was found in openOrderMap with status: "
						+ openOrder.getOrderStatus() + " and quantity: " + openOrder.getQuantity());
			}

			if (null != openBidOrder) {
				_log.warn("The order you were trying to cancel (" + origOrderTriedToCancel + ") was found in openBidOrderMap with status: "
						+ openBidOrder.getOrderStatus() + " and quantity: " + openBidOrder.getQuantity());
			}

			if (null != openAskOrder) {
				_log.warn("The order you were trying to cancel (" + origOrderTriedToCancel + ") was found in openAskOrderMap with status: "
						+ openAskOrder.getOrderStatus() + " and quantity: " + openAskOrder.getQuantity());
			}

			final OrderSpecification placedOrder = allPlacedOrders.get(origOrderTriedToCancel);
			if (null != placedOrder) {
				_log.warn("The order you were trying to cancel (" + origOrderTriedToCancel + ")had a status of: " + placedOrder.getOrderStatus());
			}
		} else {
			_log.warn("I have no fucking idea where you got that cancelReject message from but it is bullshit.");
		}

		openBidOrders.remove(clOrdId);
		openAskOrders.remove(clOrdId);

		doOnCancelRejected(clOrdId);
	}

	@Override
	public final void onCancelReplaceAccepted(final ClOrdId clOrdId, final OrderSpecification order, final ClOrdId origClOrdId) {
		final OrderSpecification wasOpenOrder = openOrders.remove(origClOrdId);
		if (null == wasOpenOrder) {
			_log.warn("Something is not right - open order not present in map waay too early. Someone is doing something wrong.");
		} else {
			wasOpenOrder.setOrderStatus(OrderStatus.REPLACED);
		}
		openOrders.put(clOrdId, order);

		sendChangedEvent(OrderUpdate.createCancelReplaceOrderUpdate(origClOrdId, clOrdId, order));

		sendChangedEvent(new StatsUpdate(openOrders.size(), STATS_UPDATE_TYPE.OPEN_ORDER_COUNT));
		sendChangedEvent(new StatsUpdate(getOpenOrderValue(), STATS_UPDATE_TYPE.OPEN_ORDER_VALUE));

		doOnCancelReplaceAccepted(clOrdId, order, origClOrdId);
	}

	@Override
	public final void onCancelReplaceRejected(final ClOrdId clOrdId) {
		_log.warn("Received onCancelReplaceRejected callback.");
		doOnCancelReplaceRejected(clOrdId);
	}

	@Override
	public final void onNewOrderAccepted(final ClOrdId clOrdId, final OrderSpecification order) {
		openOrders.put(clOrdId, order);

		BigDecimal exposure = new BigDecimal(0);

		for (final OrderSpecification o : openOrders.values()) {
			exposure = exposure.add(BigDecimal.valueOf(o.getQuantity()).multiply(o.getPrice()));
		}

		if (exposure.compareTo(maximumExposure) > 0) {
			maximumExposure = exposure;
			sendChangedEvent(new StatsUpdate(maximumExposure, STATS_UPDATE_TYPE.MAXIMUM_EXPOSURE));
		}

		sendChangedEvent(OrderUpdate.createNewOrderUpdate(clOrdId, order));

		sendChangedEvent(new StatsUpdate(++orderCount, STATS_UPDATE_TYPE.ORDER_COUNT));
		sendChangedEvent(new StatsUpdate(openOrders.size(), STATS_UPDATE_TYPE.OPEN_ORDER_COUNT));
		sendChangedEvent(new StatsUpdate(getOpenOrderValue(), STATS_UPDATE_TYPE.OPEN_ORDER_VALUE));

		doOnNewOrderAccepted(clOrdId, order);
	}

	@Override
	public final void onNewOrderRejected(final ClOrdId clOrdId) {
		_log.warn("Received onNewOrderRejected callback.");
		final OrderSpecification rejectedOrder = allPlacedOrders.get(clOrdId);
		if (null != rejectedOrder) {
			rejectedOrder.setOrderStatus(OrderStatus.REJECTED);
		}
		doOnNewOrderRejected(clOrdId);
	}

	public boolean areAnyOrdersOpen() {
		return !openOrders.isEmpty();
	}

	public String getSymbol() {
		return symbol;
	}
	
	public Position getCurrentPosition() {
		return positionHistory.peek();
	}

	public Map<ClOrdId, OrderSpecification> getOpenOrders() {
		return openOrders;
	}

	public Integer getOpenOrderCount() {
		return openOrders.size();
	}

	public Integer getOrderCount() {
		return orderCount;
	}

	public Integer getFillCount() {
		return fillHistory.size();
	}

	public Collection<Position> getPositionHistory() {
		return positionHistory;
	}

	public Collection<TransactionCost> getTransactionCostHistory() {
		return transactionCostHistory;
	}

	public BigDecimal getTotalTransactionCost() {
		return transactionCostTotal.getCost();
	}

	public Collection<Fill> getFillHistory() {
		return fillHistory;
	}

	public BigDecimal getClosedPL() {
		return positionHistory.peek().getClosedPL();
	}

	public int getSharesTraded() {
		int sharesTraded = 0;

		for (final Fill fill : fillHistory) {
			sharesTraded += Math.abs(fill.getQuantity());
		}

		return sharesTraded;
	}

	public BigDecimal getOpenOrderValue() {
		BigDecimal openOrderValue = new BigDecimal(0);

		for (final OrderSpecification order : openOrders.values()) {
			final BigDecimal orderValue = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
			openOrderValue = openOrderValue.add(orderValue);
		}

		return openOrderValue;
	}

	public BigDecimal getMaxDrawDown() {
		return maxDrawDown;
	}
	
	public BigDecimal getMaximumExposure() {
		return maximumExposure;
	}

	public String getStrategySpecificCustomEndOfDayReport() {
		return "";
	}

	public String getEndOfDayReport() {
		final StringBuilder sb = new StringBuilder();

		sb.append(NEW_LINE + "-------------------- END OF DAY REPORT (" + startOfTrading.getTime() + ") --------------------" + NEW_LINE);

		for (final STATS_UPDATE_TYPE type : STATS_UPDATE_TYPE.getTodaysEnums()) {
			sb.append(StringUtils.rightPad(type.toString() + ":", 25) + stats.getFormattedValueForStatistic(type) + NEW_LINE);
		}
		
		sb.append(getStrategySpecificCustomEndOfDayReport());

		sb.append("------------------------------------------------------------------------------------------" + NEW_LINE);

		return sb.toString();

	}

	protected SimulationClOrdId getNextClOrdId() {
		return new SimulationClOrdId(nextClOrdId.getAndIncrement());
	}

	public void reset() {
		quoteBook.reset();
		positionHistory.clear();
		transactionCostHistory.clear();
		fillHistory.clear();
		openOrders.clear();
		nextClOrdId = new AtomicLong();
		orderCount = Integer.valueOf(0);
		maximumExposure = BigDecimal.ZERO;
		drawDown = BigDecimal.ZERO;
		maxDrawDown = BigDecimal.ZERO;

		positionHistory.push(new Position());
	}

	///------------UI Methods-----------------///
	@Override
	public void addObserver(final Observer o) {
		super.addObserver(o);

		hasAnyObservers = true;
	}

	public void sendPositionChangedEvent() {
		if (hasAnyObservers) {
			setChanged();
			notifyObservers(positionHistory.peek());
		}
	}

	public void sendChangedEvent(final Object update) {
		if (hasAnyObservers) {
			setChanged();
			notifyObservers(update);
		}
	}

	///------------New doOnAction Methods-----------------///

	protected void doOnFill(final Fill fill) {
	}

	void doOnTransactionCost(final TransactionCost cost) {
	}

	void doOnCancelReplaceAccepted(final ClOrdId clOrdId, final OrderSpecification order, final ClOrdId origClOrdId) {
	}

	void doOnCancelReplaceRejected(final ClOrdId clOrdId) {
	}

	void doOnCancelAccepted(final ClOrdId clOrdId, final ClOrdId origClOrdId) {
	}

	void doOnCancelRejected(final ClOrdId clOrdId) {
	}

	void doOnNewOrderAccepted(final ClOrdId clOrdId, final OrderSpecification order) {
	}

	void doOnNewOrderRejected(final ClOrdId clOrdId) {
	}

	void doOnBid(final Bid bid) {
	}

	void doOnAsk(final Ask ask) {
	}

	void doOnTradeTick(final TradeTick tradeTick) {
	}
	
	void doOnPlaybackEnd() {
	}

	///----------------Trade Destination Method Wrappers----------------///
	public void placeOrder(final ClOrdId clOrdId, final OrderSpecification newOrder) {
		allPlacedOrders.put(clOrdId, newOrder);
		newOrder.setOrderStatus(OrderStatus.NEW);
		if (newOrder.getTradeType().equals(TradeType.BUY) || newOrder.getTradeType().equals(TradeType.COVER)) {
			openBidOrders.put(clOrdId, newOrder);
		} else if (newOrder.getTradeType().equals(TradeType.SELL) || newOrder.getTradeType().equals(TradeType.SHORT)) {
			openAskOrders.put(clOrdId, newOrder);
		}
		tradeDestination.placeOrder(clOrdId, newOrder);
	}

	public void cancelOrder(final ClOrdId clOrdId, final ClOrdId origClOrdId) {
		allPlacedCancelOrders.put(clOrdId, origClOrdId);
		tradeDestination.cancelOrder(clOrdId, origClOrdId);
		openAskOrders.remove(origClOrdId);
		openBidOrders.remove(origClOrdId);
	}

	public void cancelReplaceOrder(final ClOrdId clOrdId, final ClOrdId origClOrdId, final OrderSpecification newOrder) {
		tradeDestination.cancelReplaceOrder(clOrdId, origClOrdId, newOrder);
	}

	public void cancelAll(final ClOrdId clOrdId) {
		tradeDestination.cancelAll(clOrdId);
	}

	public void onPlaybackEnd() {
		doOnPlaybackEnd();
	}
}
