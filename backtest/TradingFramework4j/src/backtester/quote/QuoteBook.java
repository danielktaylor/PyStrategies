package backtester.quote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import backtester.quote.Ask;
import backtester.quote.Bid;

import backtester.common.SimpleLogger;
import backtester.common.ValueSortedMap;
import backtester.trade.OrderId;

public class QuoteBook {
	private final static int MAX_TRADE_TICKS = 50;
	private final static int MAX_REMOVED_BOOK_ENTRIES_TO_STORE = 50;
	private final static SimpleLogger _log = SimpleLogger.getLogger(QuoteBook.class);

	private final ValueSortedMap<Object, Bid> bids = new ValueSortedMap<Object, Bid>();
	private final ValueSortedMap<Object, Ask> asks = new ValueSortedMap<Object, Ask>();
	private final Queue<TradeTick> ticks = new ArrayDeque<TradeTick>();
	private final Queue<BookEntry> removedBookEntryQueue = new ArrayDeque<BookEntry>();
	private long bidVolume = 0;
	private long askVolume = 0;

	private Bid lastReceivedBid;
	private Ask lastReceivedAsk;
	@SuppressWarnings("unused")
	private Bid lastReceivedNonSimulatedBid;
	@SuppressWarnings("unused")
	private Ask lastReceivedNonSimulatedAsk;
	private BookEntry lastReceivedBidAsk;
	private BookEntry lastReceivedNonSimulatedBidAsk;
	
	public void addBid(final Bid bid) {
		if (null == lastReceivedBid || bid.getTimestamp().compareTo(lastReceivedBid.getTimestamp()) >= 0) {
			lastReceivedBid = bid;
			lastReceivedBidAsk = bid;

			if (!bid.isSimulated()) {
				lastReceivedNonSimulatedBidAsk = bid;
				lastReceivedNonSimulatedBid = bid;
			}
		}

		if (bid.getRemainingQuantity() == 0) {
			final Bid removed = bids.remove(bid.getId());
			if (removed == null) {
				_log.warn("Failed to remove bid with ID: " + bid.getId() + ".  This bid was simulated: " + bid.isSimulated() + ".");
			} else {
				addRemovedBookEntryToQueue(removed);
				bidVolume -= removed.getRemainingQuantity();
			}
		} else {
			if (bids.containsKey(bid.getId())) {
				final long diff = bid.getRemainingQuantity() - bids.get(bid.getId()).getRemainingQuantity();
				bidVolume += diff;
				
				Bid origBid = bids.get(bid.getId());
				if (origBid.getPrice().compareTo(bid.getPrice()) != 0
						|| origBid.getOriginalQuantity() < bid.getOriginalQuantity()) {
					bids.put(bid.getId(), bid);
				} else {
					Date incomingTimestamp = bid.getTimestamp();
					bid.setTimestamp(origBid.getTimestamp());
					bids.put(bid.getId(), bid);
					bid.setTimestamp(incomingTimestamp);
				}
			} else {
				if (bid.getInsertionTimestamp() != null) {
					Date incomingTimestamp = bid.getTimestamp();
					bid.setTimestamp(bid.getInsertionTimestamp());
					bidVolume += bid.getRemainingQuantity();
					bids.put(bid.getId(), bid);
					bid.setTimestamp(incomingTimestamp);
				} else {
					bidVolume += bid.getRemainingQuantity();
					bids.put(bid.getId(), bid);
				}
			}
		}
	}
	
	public void addAsk(final Ask ask) {
		if (null == lastReceivedAsk || ask.getTimestamp().compareTo(lastReceivedAsk.getTimestamp()) >= 0) {
			lastReceivedAsk = ask;
			lastReceivedBidAsk = ask;

			if (!ask.isSimulated()) {
				lastReceivedNonSimulatedBidAsk = ask;
				lastReceivedNonSimulatedAsk = ask;
			}
		}

		if (ask.getRemainingQuantity() == 0) {
			final Ask removed = asks.remove(ask.getId());
			if (removed == null) {
				_log.warn("Failed to remove ask with ID: " + ask.getId() + ".  This ask was simulated: " + ask.isSimulated() + ".");
			} else {
				addRemovedBookEntryToQueue(removed);
				askVolume -= removed.getRemainingQuantity();
			}
		} else {
			if (asks.containsKey(ask.getId())) {
				final long diff = ask.getRemainingQuantity() - asks.get(ask.getId()).getRemainingQuantity();
				askVolume += diff;

				Ask origAsk = asks.get(ask.getId());
				if (origAsk.getPrice().compareTo(ask.getPrice()) != 0
						|| origAsk.getOriginalQuantity() < ask.getOriginalQuantity()) {
					asks.put(ask.getId(), ask);
				} else {
					Date incomingTimestamp = ask.getTimestamp();
					ask.setTimestamp(origAsk.getTimestamp());
					asks.put(ask.getId(), ask);
					ask.setTimestamp(incomingTimestamp);
				}
			} else {
				if (ask.getInsertionTimestamp() != null) {
					Date incomingTimestamp = ask.getTimestamp();
					ask.setTimestamp(ask.getInsertionTimestamp());
					askVolume += ask.getRemainingQuantity();
					asks.put(ask.getId(), ask);
					ask.setTimestamp(incomingTimestamp);
				} else {
					askVolume += ask.getRemainingQuantity();
					asks.put(ask.getId(), ask);
				}
			}
		}
	}

	private void addRemovedBookEntryToQueue(final BookEntry bookEntry) {
		if (removedBookEntryQueue.size() == MAX_REMOVED_BOOK_ENTRIES_TO_STORE) {
			removedBookEntryQueue.poll();
		}

		removedBookEntryQueue.add(bookEntry);
	}

	public BookEntry getRemovedBookEntry(final Object bookEntryId) {
		for (final BookEntry bookEntry : removedBookEntryQueue) {
			if (bookEntry.getId().equals(bookEntryId)) {
				return bookEntry;
			}
		}
		_log.warn("Cannot find book entry ID, " + bookEntryId + ", in list of removed book entries.  " + "Available book entries are "
				+ removedBookEntryQueue);
		return null;
	}

	public void addTradeTick(final TradeTick tradeTick) {
		ticks.add(tradeTick);

		if (ticks.size() > MAX_TRADE_TICKS) {
			ticks.remove();
		}
	}

	public ArrayDeque<TradeTick> getTrades() {
		final ArrayDeque<TradeTick> trades = (ArrayDeque<TradeTick>)ticks;
		return trades.clone();
	}
	
	public List<Bid> getBids() {
		final List<Bid> bidCopy = new LinkedList<Bid>();
		bidCopy.addAll(bids.values());

		return bidCopy;
	}

	public List<Ask> getAsks() {
		final List<Ask> askCopy = new LinkedList<Ask>();
		askCopy.addAll(asks.values());

		return askCopy;
	}

	public List<Ask> getSimulatedAsks() {
		final List<Ask> asks = getAsks();
		final List<Ask> simulatedAskList = new ArrayList<Ask>();
		for (final Ask ask : asks) {
			if (ask.isSimulated()) {
				simulatedAskList.add(ask);
			}
		}
		return simulatedAskList;
	}

	public List<Bid> getSimulatedBids() {
		final List<Bid> bids = getBids();
		final List<Bid> simulatedBidList = new ArrayList<Bid>();
		for (final Bid bid : bids) {
			if (bid.isSimulated()) {
				simulatedBidList.add(bid);
			}
		}
		return simulatedBidList;
	}

	public QuoteEntry getTopOfBook() {
		Bid bid = null;
		if (!bids.isEmpty()) {
			bid = bids.values().iterator().next();
		}
		Ask ask = null;
		if (!asks.isEmpty()) {
			ask = asks.values().iterator().next();
		}
		return new QuoteEntry(bid, ask);
	}

	public BigDecimal getSpread() {
		if (bids.isEmpty() || asks.isEmpty()) {
			return null;
		}
		final BigDecimal bidPrice = bids.values().iterator().next().getPrice();
		final BigDecimal askPrice = asks.values().iterator().next().getPrice();

		return askPrice.subtract(bidPrice);
	}

	public BigDecimal getMidpoint() {
		if (bids.isEmpty() || asks.isEmpty()) {
			return BigDecimal.ZERO;
		} else {
			return getTopOfBook().getBid().getPrice().add(getSpread().divide(new BigDecimal("2"), 4, RoundingMode.CEILING));
		}
	}

	public long getBidVolume() {
		return bidVolume;
	}

	public long getAskVolume() {
		return askVolume;
	}

	public BookEntry getBookEntryByOrderId(final OrderId orderId) {
		final BookEntry bookEntry = bids.get(orderId);
		return bookEntry != null ? bookEntry : asks.get(orderId);
	}

	public BookEntry getOppositeTopOfBookBookEntry(final BookEntry bookEntry) {
		return bookEntry instanceof Ask ? getTopOfBook().getBid() : getTopOfBook().getAsk();
	}

	public BookEntry getSameSideTopOfBookBookEntry(final BookEntry bookEntry) {
		return bookEntry instanceof Ask ? getTopOfBook().getAsk() : getTopOfBook().getBid();
	}

	public BookEntry getLastReceivedNonSimulatedBookEntry() {
		if (lastReceivedAsk != null && lastReceivedBid != null && lastReceivedNonSimulatedBidAsk != null) {
			final int compareResult = lastReceivedAsk.getTimestamp().compareTo(lastReceivedBid.getTimestamp());
			if (compareResult > 0 && !lastReceivedAsk.getId().equals(lastReceivedBidAsk.getId())) {
				_log.error("Expected the last received ask to be the last received bid-ask, but it is a different value.  The last received ask is ["
						+ lastReceivedAsk
						+ "].  The last received bid is ["
						+ lastReceivedBid
						+ "].  The last received bid-ask is ["
						+ lastReceivedBidAsk + "].");
			} else if (compareResult < 0 && !lastReceivedBid.getId().equals(lastReceivedBidAsk.getId())) {
				_log.error("Expected the last received bid to be the last received bid-ask, but it is a different value.  The last received ask is ["
						+ lastReceivedAsk
						+ "].  The last received bid is ["
						+ lastReceivedBid
						+ "].  The last received bid-ask is ["
						+ lastReceivedNonSimulatedBidAsk + "].");
			}
		}

		return lastReceivedNonSimulatedBidAsk;
	}

	public void reset() {
		bids.clear();
		asks.clear();
		ticks.clear();
		lastReceivedBid = null;
		lastReceivedAsk = null;
		lastReceivedBidAsk = null;
		removedBookEntryQueue.clear();
		lastReceivedNonSimulatedBidAsk = null;
		bidVolume = 0;
		askVolume = 0;
	}

	/**
	 * Exposed for unit testing purposes only.
	 */
	Ask getLastAsk() {
		return lastReceivedAsk;
	}

	/**
	 * Exposed for unit testing purposes only.
	 */
	Bid getLastBid() {
		return lastReceivedBid;
	}

	/**
	 * Exposed for unit testing purposes only.
	 */
	BookEntry getLastReceivedBookEntry() {
		return lastReceivedBidAsk;
	}
}
