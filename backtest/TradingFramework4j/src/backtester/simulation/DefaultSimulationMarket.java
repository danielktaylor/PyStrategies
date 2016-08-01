package backtester.simulation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicLong;

import backtester.common.SimpleLogger;
import backtester.quote.Ask;
import backtester.quote.Bid;
import backtester.quote.BookEntry;
import backtester.quote.BookEntryFactory;
import backtester.quote.DummyQuoteListener;
import backtester.quote.QuoteBook;
import backtester.quote.QuoteEntry;
import backtester.quote.QuoteListener;
import backtester.quote.TradeTick;
import backtester.strategy.DoNothingStrategy;
import backtester.strategy.StrategyConfiguration;
import backtester.trade.ClOrdId;
import backtester.trade.Fill;
import backtester.trade.LiquidityFlag;
import backtester.trade.OrderId;
import backtester.trade.OrderSpecification;
import backtester.trade.TradeListener;
import backtester.trade.TradeType;
import backtester.trade.TradingException;
import backtester.trade.TransactionCost;

public class DefaultSimulationMarket extends Observable implements SimulationMarket {
	private static final double EQUAL_PRICE_FILL_PROBABILITY = .5;
	private static final BigDecimal LIQUIDITY_ADDED_REBATE_PER_SHARE = new BigDecimal("-0.0027"); // from BATS
	private static final BigDecimal LIQUIDITY_REMOVED_FEE_PER_SHARE = new BigDecimal("0.0028"); // from BATS

	private final SimpleLogger _log = SimpleLogger.getLogger(DefaultSimulationMarket.class);
	private final QuoteBook quoteBook = new QuoteBook();
	private final HashMap<ClOrdId, SimulationOrderId> clOrdIdToOrderIdMap = new HashMap<ClOrdId, SimulationOrderId>();
	private AtomicLong currentNumericOrderId = new AtomicLong();

	private final MarketTime marketTime = MarketTime.getInstance();

	// QuoteListener and tradeListener are never null so we don't need null checks everywhere
	private QuoteListener quoteListener = new DummyQuoteListener();
	private TradeListener tradeListener = new DoNothingStrategy(null, StrategyConfiguration.emptyStrategyConfiguration());

	private boolean longSaleValidationEnabled = true;
	private int currentPosition = 0;

	private boolean hasAnyObservers;

	public DefaultSimulationMarket(final DefaultSimulationMarket market) {
		if (null != market) {
			currentPosition = market.getPosition();
		}
	}

	private SimulationOrderId lookupOrderId(final ClOrdId clOrdId) throws TradingException {
		if (!clOrdIdToOrderIdMap.containsKey(clOrdId)) {
			throw new TradingException("No OrderId found for specified clOrdId");
		}
		return clOrdIdToOrderIdMap.get(clOrdId);
	}

	@Override
	public void cancelOrder(final ClOrdId clOrdId, final ClOrdId origClOrdId) {
		try {
			final SimulationOrderId origOrderId = lookupOrderId(origClOrdId);
			final BookEntry bookEntryToCancel = quoteBook.getBookEntryByOrderId(origOrderId);
			if (bookEntryToCancel == null) {
				_log.warn("Cancel rejected, orderId not found: " + origOrderId.toString());
				tradeListener.onCancelRejected(clOrdId);
				return;
			}

			internalCancelOrder(bookEntryToCancel);

			tradeListener.onCancelAccepted(clOrdId, origClOrdId);
		} catch (final TradingException e) {
			_log.warn("Cancel rejected, unknown clOrdId: " + clOrdId);
			tradeListener.onCancelRejected(clOrdId);
		}
	}

	private void internalCancelOrder(final BookEntry bookEntryToCancel) {
		final BookEntry synthesizedCancelBookEntry;
		if (bookEntryToCancel instanceof Bid) {
			synthesizedCancelBookEntry = BookEntryFactory.createCancelBid(bookEntryToCancel.getClOrdId(), bookEntryToCancel.getSymbol(),
					bookEntryToCancel.getId(), marketTime.getTime());
		} else {
			synthesizedCancelBookEntry = BookEntryFactory.createCancelAsk(bookEntryToCancel.getClOrdId(), bookEntryToCancel.getSymbol(),
					bookEntryToCancel.getId(), marketTime.getTime());
		}
		addBookEntryToQuoteBookAndSendCallback(synthesizedCancelBookEntry);
		clOrdIdToOrderIdMap.remove(bookEntryToCancel.getClOrdId());
	}

	@Override
	public void cancelReplaceOrder(final ClOrdId clOrdId, final ClOrdId origClOrdId, final OrderSpecification newOrder) {
		try {
		    if (newOrder.getQuantity() <= 0) {
		        _log.warn("Cancel-replace rejected, order quantity must be greater than zero.");
		        tradeListener.onCancelReplaceRejected(clOrdId);
		        return;
		    } else if (newOrder.getPrice().compareTo(BigDecimal.ZERO) < 0) {
		        _log.warn("Cancel-replace rejected, order price must be zero or greater.");
		        tradeListener.onCancelReplaceRejected(clOrdId);
		        return;
		    }

			final SimulationOrderId origOrderId = lookupOrderId(origClOrdId);
			final BookEntry bookEntryToCancel = quoteBook.getBookEntryByOrderId(origOrderId);

	        if (bookEntryToCancel == null) {
				_log.warn("Cancel-replace rejected, unknown clOrdId: " + origClOrdId);
				tradeListener.onCancelReplaceRejected(clOrdId);
				return;
			} else if (!bookEntryToCancel.isSimulated()) {
				_log.warn("Cancel-replace rejected, cannot replace non-simulated order (ClOrdId: " + clOrdId + ")");
				tradeListener.onCancelReplaceRejected(clOrdId);
				return;
			} else if (!bookEntryToCancel.getSymbol().equals(newOrder.getSymbol())) {
				_log.warn("Cancel-replace rejected, cannot change symbol (ClOrdId: " + clOrdId + ")");
				tradeListener.onCancelReplaceRejected(clOrdId);
				return;
			} else if (!bookEntryToCancel.getClass().equals(newOrder.getBookEntryClass())) {
				_log.warn("Cancel-replace rejected, cannot change trade type.  Existing book entry is " + bookEntryToCancel.getClass().getName()
						+ " and replacement order is " + newOrder.getBookEntryClass().getName() + ".");
				tradeListener.onCancelReplaceRejected(clOrdId);
				return;
			} else if (newOrder.getQuantity() <= bookEntryToCancel.getFilledQuantity()) {
				_log.warn("Cancel-replace rejected, replacement order quantity is less than or equal to existing order's filled quantity.  Existing book entry filled quantity is"
						+ bookEntryToCancel.getFilledQuantity() + " and replacement order quantity is " + newOrder.getQuantity() + ".");
				tradeListener.onCancelReplaceRejected(clOrdId);
				return;
			}
	        
			/* BATS rule for cancel replacing:
			 * 
			 * "In the event an order has been cancelled or replaced in accordance with Rule 11.9(e) above, 
			 * such order only retains time priority if such modification involves a decrease in the size
			 * of the order, a change to Max Floor of a Reserve Order, a change to the stop price of a
			 * Stop Order or Stop Limit Order or a change in position from sell long to sell short or
			 * vice-versa. Any other modification to an order, including an increase in the size of the
			 * order and/or price change, will result in such order losing time priority as compared to
			 * other orders in the BZX Book and the timestamp for such order being revised to reflect
			 * the time of the modification."
			 * 
			 */
			
	        Date insertionTimestamp = null;
			if (bookEntryToCancel.getPrice().compareTo(newOrder.getPrice()) == 0 &&
					newOrder.getQuantity() <= bookEntryToCancel.getOriginalQuantity()) {
				// Maintains time priority
				insertionTimestamp = bookEntryToCancel.getTimestamp();
			}
			
			final int filledQty = bookEntryToCancel.getFilledQuantity();
			internalCancelOrder(bookEntryToCancel);
			tradeListener.onCancelReplaceAccepted(clOrdId, newOrder, origClOrdId);
			newOrder.setAmountFilled(filledQty);
			internalPlaceOrder(clOrdId, newOrder, marketTime.getTime(), insertionTimestamp);
			
		} catch (final TradingException e) {
			_log.warn("Cancel-replace rejected, unknown clOrdId: " + clOrdId);
			tradeListener.onCancelReplaceRejected(clOrdId);
		}
	}

	@Override
	public void placeOrder(final ClOrdId clOrdId, final OrderSpecification newOrder) {
	    if (longSaleValidationEnabled && newOrder.getTradeType().equals(TradeType.SELL) && newOrder.getQuantity() > currentPosition) {
	        _log.warn("New order rejected, long sale causes short position (must short instead): " + clOrdId);
	        tradeListener.onNewOrderRejected(clOrdId);
	        return;
	    } else if (newOrder.getQuantity() <= 0) {
	        _log.warn("New order rejected, order quantity must be greater than zero.");
	        tradeListener.onNewOrderRejected(clOrdId);
	        return;
	    } else if (newOrder.getPrice().compareTo(BigDecimal.ZERO) < 0) {
	        _log.warn("New order rejected, order price must be zero or greater.");
	        tradeListener.onNewOrderRejected(clOrdId);
	        return;
	    }

		tradeListener.onNewOrderAccepted(clOrdId, newOrder);

		internalPlaceOrder(clOrdId, newOrder);
	}
	
	private void internalPlaceOrder(final ClOrdId clOrdId, final OrderSpecification newOrder) {
		internalPlaceOrder(clOrdId, newOrder, marketTime.getTime(), null);
	}

	private void internalPlaceOrder(final ClOrdId clOrdId, final OrderSpecification newOrder, Date timestamp, Date insertionTimestamp) {
		final SimulationOrderId orderId = new SimulationOrderId(currentNumericOrderId.getAndIncrement());

		BookEntry simulatedBookEntry = BookEntryFactory.createSimulatedBookEntry(newOrder.getBookEntryClass(), clOrdId, orderId,
				newOrder.getSymbol(), newOrder.getQuantity() - newOrder.getAmountFilled(), newOrder.getPrice(), timestamp,
				newOrder.getQuantity());
		simulatedBookEntry.setInsertionTimestamp(insertionTimestamp);
		simulatedBookEntry = checkForLiquidityRemoval(simulatedBookEntry);

		if (simulatedBookEntry.getRemainingQuantity() > 0) {
			// Add to the book now because liquidity removal check is done and the book entry has positive remaining quantity.
			addBookEntryToQuoteBookAndSendCallback(simulatedBookEntry);
			clOrdIdToOrderIdMap.put(clOrdId, orderId);
		}
	}

	@Override
	public void setTradeListener(final TradeListener tradeListener) {
		if (tradeListener == null) {
			throw new IllegalArgumentException("TradeListener can't be null");
		}

		this.tradeListener = tradeListener;
	}

	@Override
	public void onAsk(final Ask ask) {
		addBookEntryToQuoteBookAndSendCallback(ask);

		bidOrAskFillCheck(ask);
	}

	@Override
	public void onBid(final Bid bid) {
		addBookEntryToQuoteBookAndSendCallback(bid);

		bidOrAskFillCheck(bid);
	}

	private void bidOrAskFillCheck(final BookEntry receivedBookEntry) {
		if (receivedBookEntry.getRemainingQuantity() == 0 || receivedBookEntry.isSimulated()) {
			return; // Do not evaluate bids and asks with size of zero, also make sure received book entry is not simulated.
		}

		final int directionMultiplier = receivedBookEntry instanceof Ask ? 1 : -1;
		int fillQuantity = 0;
		BookEntry opposingTopOfBookBookEntry = null;
		do {
			opposingTopOfBookBookEntry = quoteBook.getOppositeTopOfBookBookEntry(receivedBookEntry);
			if (null == opposingTopOfBookBookEntry || !opposingTopOfBookBookEntry.isSimulated()) {
				return;
			}

			int compareResult = -1;
			// Do not compute compare result for market orders because they always fill.
			if (!opposingTopOfBookBookEntry.isMarketOrder()) {
				compareResult = receivedBookEntry.getPrice().compareTo(opposingTopOfBookBookEntry.getPrice()) * directionMultiplier;
			}
			if (compareResult < 0) {
				fillQuantity = createOnTickFill(receivedBookEntry, opposingTopOfBookBookEntry, directionMultiplier, true);
			} else if (compareResult == 0) {
				if (Math.random() >= EQUAL_PRICE_FILL_PROBABILITY) {
					fillQuantity = createOnTickFill(receivedBookEntry, opposingTopOfBookBookEntry, directionMultiplier, true);
					// System.out.println("probability fill occurred " + ++yes);
				} else {
					// System.out.println("probability fill did not occur " + ++no);
				}
			} else if (compareResult > 0) {
				return;
			}
		} while (fillQuantity == opposingTopOfBookBookEntry.getRemainingQuantity()); // Continue checking for fills while the simulated book entry is totally filled.
	}

	int yes = 0;
	int no = 0;

	@Override
	public void onTradeTick(final TradeTick tradeTick) {
		quoteBook.addTradeTick(tradeTick);

		updateUI(tradeTick);

		quoteListener.onTradeTick(tradeTick);

		final BookEntry lastReceivedBookEntry = quoteBook.getLastReceivedNonSimulatedBookEntry();
		if (lastReceivedBookEntry != null) {
			if (isTradeTickAnIdentifiableTrade(tradeTick, lastReceivedBookEntry)) {
				// Get book entry removed from quote book because it has original book entry size.
				final BookEntry removedBookEntry = quoteBook.getRemovedBookEntry(lastReceivedBookEntry.getId());
				if (removedBookEntry != null) {
					tradeTickFillCheck(removedBookEntry);
				}
			}
		}
	}

	private void tradeTickFillCheck(final BookEntry lastReceivedBookEntry) {
		final int directionMultiplier = lastReceivedBookEntry instanceof Ask ? -1 : 1;
		BookEntry topOfBookBookEntry = null;
		int fillQuantity = 0;
		do {
			topOfBookBookEntry = quoteBook.getSameSideTopOfBookBookEntry(lastReceivedBookEntry);
			if (topOfBookBookEntry == null || !topOfBookBookEntry.isSimulated()) {
				return;
			}

			final int compareResult = lastReceivedBookEntry.getPrice().compareTo(topOfBookBookEntry.getPrice()) * directionMultiplier;
			if (compareResult < 0) {
				fillQuantity = createOnTickFill(lastReceivedBookEntry, topOfBookBookEntry, directionMultiplier, false);
			} else if (compareResult == 0) {
				if (Math.random() >= EQUAL_PRICE_FILL_PROBABILITY) {
					fillQuantity = createOnTickFill(lastReceivedBookEntry, topOfBookBookEntry, directionMultiplier, false);
				}
			} else if (compareResult > 0) {
				return;
			}
		} while (fillQuantity == topOfBookBookEntry.getRemainingQuantity());
	}

	private int createOnTickFill(final BookEntry receivedBookEntry, final BookEntry simulatedBookEntry, final int directionMultiplier,
			final boolean isDirtyQuantityUpdateRequired) {
		final int fillQuantity = Math.min(receivedBookEntry.getRemainingQuantity() - receivedBookEntry.getDirtyQuantity(),
				simulatedBookEntry.getRemainingQuantity());
		if (fillQuantity == 0) {
			return 0;
		}
		// remember -1 direction means selling
		final Fill fill = new Fill(simulatedBookEntry.getClOrdId(), (OrderId) simulatedBookEntry.getId(), simulatedBookEntry.getSymbol(),
				directionMultiplier * fillQuantity, simulatedBookEntry.getPrice(), simulatedBookEntry.getRemainingQuantity() - fillQuantity,
				marketTime.getTime(), LiquidityFlag.ADDED);

		currentPosition += fill.getQuantity();
		tradeListener.onFill(fill);

		// Update simulated book entry with fill information.
		final BookEntry postFillSimulatedBookEntry = BookEntryFactory.createSimulatedBookEntry(simulatedBookEntry.getClass(),
				simulatedBookEntry.getClOrdId(), simulatedBookEntry.getId(), simulatedBookEntry.getSymbol(),
				simulatedBookEntry.getRemainingQuantity() - fillQuantity, simulatedBookEntry.getPrice(), marketTime.getTime(),
				simulatedBookEntry.getOriginalQuantity());

		// Update the book with new simulated book entry.
		addBookEntryToQuoteBookAndSendCallback(postFillSimulatedBookEntry);

		// Update received book entry with fill information.
		final BookEntry postFillReceivedBookEntry = BookEntryFactory.createActualBookEntry(receivedBookEntry.getClass(),
				receivedBookEntry.getId(), receivedBookEntry.getSymbol(), receivedBookEntry.getRemainingQuantity(),
				receivedBookEntry.getPrice(), marketTime.getTime(), receivedBookEntry.getOriginalQuantity());

		if (isDirtyQuantityUpdateRequired) {
			postFillReceivedBookEntry.setDirtyQuantity(receivedBookEntry.getDirtyQuantity() + fillQuantity);
		}

		// Update the book with new received book entry.
		addBookEntryToQuoteBookAndSendCallback(postFillReceivedBookEntry);

		final TransactionCost transactCost = new TransactionCost(LIQUIDITY_ADDED_REBATE_PER_SHARE.multiply(new BigDecimal(fillQuantity)));
		tradeListener.onTransactionCost(transactCost);

		return fillQuantity;
	}

	private boolean isTradeTickAnIdentifiableTrade(final TradeTick tradeTick, final BookEntry lastReceivedBookEntry) {
		return lastReceivedBookEntry.getRemainingQuantity() == 0 && lastReceivedBookEntry.getPrice().equals(tradeTick.getPrice());
	}

	@Override
	public void setQuoteListener(final QuoteListener quoteListener) {
		if (quoteListener == null) {
			throw new IllegalArgumentException("QuoteListener can't be null");
		}
		this.quoteListener = quoteListener;
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	private BookEntry checkForLiquidityRemoval(BookEntry simulatedBookEntry) {
		if (!simulatedBookEntry.isSimulated()) {
			throw new IllegalArgumentException("Attempted to evaluate non-simulated book " + "entry with ID " + simulatedBookEntry.getId() + ".");
		}
		BookEntry topOpposingBookEntry;
		List<? extends BookEntry> opposingBookEntryList;
		int directionMultiplier = 0;
		final QuoteEntry topOfBook = quoteBook.getTopOfBook();
		if (topOfBook == null) {
			return simulatedBookEntry;
		}
		if (simulatedBookEntry instanceof Ask) {
			topOpposingBookEntry = topOfBook.getBid();
			directionMultiplier = -1;
			opposingBookEntryList = quoteBook.getBids();
		} else { // Bid
			topOpposingBookEntry = topOfBook.getAsk();
			directionMultiplier = 1;
			opposingBookEntryList = quoteBook.getAsks();
		}

		if (topOpposingBookEntry == null) {
			return simulatedBookEntry;
		}

		if (!simulatedBookEntry.isMarketOrder()
				&& topOpposingBookEntry.getPrice().compareTo(simulatedBookEntry.getPrice()) * directionMultiplier > 0) {
			return simulatedBookEntry; // Liquidity is not removed because simulated book entry is outside price range of opposing book entry.
		}

		// Iterate through opposing book entry list to find fills that remove liquidity.
		for (final BookEntry opposingBookEntry : opposingBookEntryList) {
			if (simulatedBookEntry.isMarketOrder()
					|| opposingBookEntry.getPrice().compareTo(simulatedBookEntry.getPrice()) * directionMultiplier <= 0) {
				int fillQuantity = 0;
				boolean isOpposingBookEntrySimulated = false;
				if (opposingBookEntry.isSimulated()) {
					fillQuantity = Math.min(opposingBookEntry.getRemainingQuantity(), simulatedBookEntry.getRemainingQuantity());
					isOpposingBookEntrySimulated = true;
				} else { // Real book entry
					fillQuantity = Math.min(opposingBookEntry.getRemainingQuantity() - opposingBookEntry.getDirtyQuantity(),
							simulatedBookEntry.getRemainingQuantity());
				}

				if (fillQuantity == 0) {
					continue;
				}

				if (!isOpposingBookEntrySimulated) {
					opposingBookEntry.setDirtyQuantity(opposingBookEntry.getDirtyQuantity() + fillQuantity);
				}

				final int simulatedBookEntryRemaining = simulatedBookEntry.getRemainingQuantity() - fillQuantity;

				// remember -1 direction means selling
				// The order removing liquidity receives price improvement.  This is why the opposing book entry's price is
				// used for fill.
				final Fill fill = new Fill(simulatedBookEntry.getClOrdId(), (OrderId) simulatedBookEntry.getId(),
						simulatedBookEntry.getSymbol(), directionMultiplier * fillQuantity, opposingBookEntry.getPrice(),
						simulatedBookEntryRemaining, marketTime.getTime(), LiquidityFlag.REMOVED);

				currentPosition += fill.getQuantity();
				tradeListener.onFill(fill);

				final TransactionCost transactCost = new TransactionCost(LIQUIDITY_REMOVED_FEE_PER_SHARE.multiply(new BigDecimal(fillQuantity)));
				tradeListener.onTransactionCost(transactCost);

				simulatedBookEntry = BookEntryFactory.createSimulatedBookEntry(simulatedBookEntry.getClass(), simulatedBookEntry.getClOrdId(),
						simulatedBookEntry.getId(), simulatedBookEntry.getSymbol(), simulatedBookEntryRemaining, simulatedBookEntry.getPrice(),
						marketTime.getTime(), simulatedBookEntry.getOriginalQuantity());

				if (simulatedBookEntryRemaining == 0) {
					break; // There is no need to continue evaluating fills when remaining quantity is zero.
				}
			}
		}

		return simulatedBookEntry;
	}

	@Override
	public synchronized void addObserver(final Observer o) {
		super.addObserver(o);

		hasAnyObservers = true;
	}

	private void addBookEntryToQuoteBookAndSendCallback(final BookEntry bookEntryToAdd) {
		if (bookEntryToAdd instanceof Ask) {
			quoteBook.addAsk((Ask) bookEntryToAdd);
			quoteListener.onAsk((Ask) bookEntryToAdd);
		} else {
			quoteBook.addBid((Bid) bookEntryToAdd);
			quoteListener.onBid((Bid) bookEntryToAdd);
		}

		updateUI(bookEntryToAdd);
	}

	@Override
	public void cancelAll(final ClOrdId clOrdId) {
		final List<ClOrdId> clOrdIdsToCancel = new ArrayList<ClOrdId>();

		for (final Ask askToBeCanceled : quoteBook.getSimulatedAsks()) {
			clOrdIdsToCancel.add(askToBeCanceled.getClOrdId());
		}

		for (final Bid bidToBeCanceled : quoteBook.getSimulatedBids()) {
			clOrdIdsToCancel.add(bidToBeCanceled.getClOrdId());
		}

		// updateUI(OrderChangedUpdate.createCancelAllOrdersUpdate());

		for (final ClOrdId clordIdToCancel : clOrdIdsToCancel) {
			cancelOrder(clOrdId, clordIdToCancel);
		}
	}

	private void updateUI(final Object arg) {
		if (hasAnyObservers) {
			setChanged();
			notifyObservers(arg);
		}
	}

	//package visible for unit tests only
	void setPosition(final int position) {
		currentPosition = position;
	}

	//package visible for unit tests only
	int getPosition() {
		return currentPosition;
	}

	void setLongSaleValidationEnabled(final boolean longSaleValidationEnabled) {
		this.longSaleValidationEnabled = longSaleValidationEnabled;
	}

	@Override
	public void reset() {
		quoteBook.reset();
		clOrdIdToOrderIdMap.clear();
		currentNumericOrderId = new AtomicLong();
	}

	@Override
	public void setCurrentTime(final Date newTime) {
		// Do nothing -- don't use this! Proxy keeps track of time, not market!
		_log.warn("setCurrentTime(...) in DefaultSimulationMarket should not be used.");
	}

	@Override
	public void initialize() {

	}

	@Override
	public void plugMemoryLeak() {

	}
	
	@Override
	public void playForTime(final long howLongToPlayFor, final long delay) {
		
	}

	@Override
	public void playNumberOfLines(final long numberOfLinesToPlay, final long delay) {
		
	}

	@Override
	public void playAll() {
		
	}
}
