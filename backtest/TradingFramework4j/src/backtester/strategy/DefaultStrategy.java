package backtester.strategy;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Calendar;

import backtester.quote.Ask;
import backtester.quote.Bid;
import backtester.quote.BookEntry;
import backtester.quote.QuoteEntry;
import backtester.trade.ClOrdId;
import backtester.trade.Fill;
import backtester.trade.OrderSpecification;
import backtester.trade.TradeType;

public class DefaultStrategy extends BaseStrategy {
	private boolean isPositionOpen = false;

	private ClOrdId lastPlacedBidClOrdId = null;
	private ClOrdId lastPlacedAskClOrdId = null;

	private final long MAX_TIME_TO_KEEP_ORDER_OPEN = 5000;
	private long currentTime = 0;
	private long lastOrderPlacedTime = 0;

	private final long GET_OUT_NOW_TIME = 10000;
	private final MathContext mathContext;

	private Calendar today;

	public DefaultStrategy(final DefaultStrategy defaultStrategy, final StrategyConfiguration strategyConfiguration) {
		super(defaultStrategy, strategyConfiguration);
		mathContext = new MathContext(4, RoundingMode.HALF_EVEN);
	}

	@Override
	public void doOnAsk(final Ask ask) {
		runAlgo(ask);
	}

	@Override
	public void doOnBid(final Bid bid) {
		runAlgo(bid);
	}

	@Override
	public void doOnFill(final Fill fill) {
		cancelAll(getNextClOrdId());
		if (!isPositionOpen) {
			lastPlacedBidClOrdId = null;
			isPositionOpen = true;
			_log.debug("Got Fill on Buy: " + fill.getSymbol() + " : " + fill.getPrice());
		} else {
			lastPlacedAskClOrdId = null;
			isPositionOpen = false;
			_log.debug("Got Fill on Sell: " + fill.getSymbol() + " : " + fill.getPrice());
		}

	}

	private void runAlgo(final BookEntry bookEntry) {
		if (currentTime == 0) {
			today = Calendar.getInstance();
			today.clear();
			today.setTime(bookEntry.getTimestamp());
			today.set(Calendar.HOUR_OF_DAY, 9);
			today.set(Calendar.MINUTE, 35);
		}
		currentTime = bookEntry.getTimestamp().getTime();
		if (currentTime > today.getTimeInMillis()) {
			if (!bookEntry.isSimulated()) {
				final long timeElapsedSinceLastOrder = currentTime - lastOrderPlacedTime;
				final QuoteEntry topOfBook = quoteBook.getTopOfBook();
				if (bookEntry instanceof Ask && topOfBook.getAsk() != null || bookEntry instanceof Bid && topOfBook.getBid() != null) {
					if (!isPositionOpen && timeElapsedSinceLastOrder > MAX_TIME_TO_KEEP_ORDER_OPEN) {
						lastOrderPlacedTime = currentTime;

						BigDecimal priceOfBid = topOfBook.getAsk() == null ? bookEntry.getPrice() : topOfBook.getAsk().getPrice();
						if (timeElapsedSinceLastOrder < GET_OUT_NOW_TIME) {
							priceOfBid = topOfBook.getBid().getPrice().subtract(topOfBook.getBid().getPrice().multiply(new BigDecimal(.03)));
						}

						priceOfBid = priceOfBid.round(mathContext);

						final OrderSpecification order = new OrderSpecification(bookEntry.getSymbol(), priceOfBid, 2, TradeType.BUY);
						if (null != lastPlacedBidClOrdId) {
							cancelOrder(getNextClOrdId(), lastPlacedBidClOrdId);
						}

						placeOrder(getNextClOrdId(), order);
					} else if (isPositionOpen && timeElapsedSinceLastOrder > MAX_TIME_TO_KEEP_ORDER_OPEN) {
						lastOrderPlacedTime = currentTime;

						BigDecimal priceOfAsk = topOfBook.getBid() == null ? bookEntry.getPrice() : topOfBook.getBid().getPrice();
						if (timeElapsedSinceLastOrder < GET_OUT_NOW_TIME) {
							priceOfAsk = topOfBook.getAsk().getPrice().add(topOfBook.getAsk().getPrice().multiply(new BigDecimal(.03)));
						}

						priceOfAsk = priceOfAsk.round(mathContext);

						final OrderSpecification order = new OrderSpecification(bookEntry.getSymbol(), priceOfAsk, 2, TradeType.SELL);

						if (null != lastPlacedAskClOrdId) {
							cancelOrder(getNextClOrdId(), lastPlacedAskClOrdId);
						}
						placeOrder(getNextClOrdId(), order);
					}
				}
			}
		}
	}

	@Override
	public void doOnNewOrderAccepted(final ClOrdId clOrdId, final OrderSpecification order) {
		switch (order.getTradeType()) {
		case BUY: {
			lastPlacedBidClOrdId = clOrdId;
			break;
		}
		case SELL: {
			lastPlacedAskClOrdId = clOrdId;
			break;
		}
		case COVER: {
			// Do nothing
		}
		case SHORT: {
			// Do nothing
		}
		}
	}

	@Override
	public void reset() {
		super.reset();

		isPositionOpen = false;
		lastPlacedAskClOrdId = null;
		lastPlacedBidClOrdId = null;
		currentTime = 0;
		lastOrderPlacedTime = 0;
	}

}
