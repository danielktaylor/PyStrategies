package backtester.quote;

import java.math.BigDecimal;
import java.util.Date;

import backtester.trade.ClOrdId;

public class BookEntryFactory {

	public static Ask createActualAsk(final Object uniqueId, final String symbol, final int size, final BigDecimal price, final Date timestamp) {
		return createActualAsk(uniqueId, symbol, size, price, timestamp, size);
	}

	public static Ask createActualAsk(final Object uniqueId, final String symbol, final int size, final BigDecimal price, final Date timestamp,
			final int originalQuantity) {
		return new Ask(uniqueId, symbol, size, price, timestamp, false, originalQuantity);
	}

	public static Ask createSimulatedAsk(final ClOrdId clOrdId, final Object uniqueId, final String symbol, final int size,
			final BigDecimal price, final Date timestamp) {
		return createSimulatedAsk(clOrdId, uniqueId, symbol, size, price, timestamp, size);
	}

	public static Ask createSimulatedAsk(final ClOrdId clOrdId, final Object uniqueId, final String symbol, final int size,
			final BigDecimal price, final Date timestamp, final int originalQuantity) {
		return new Ask(clOrdId, uniqueId, symbol, size, price, timestamp, true, originalQuantity);
	}

	public static Ask createCancelAsk(final ClOrdId clOrdId, final String symbol, final Object uniqueId, final Date currentTime) {
		return createSimulatedAsk(clOrdId, uniqueId, symbol, 0, BigDecimal.ZERO, currentTime);
	}

	public static Bid createCancelBid(final ClOrdId clOrdId, final String symbol, final Object uniqueId, final Date currentTime) {
		return createSimulatedBid(clOrdId, uniqueId, symbol, 0, BigDecimal.ZERO, currentTime);
	}

	public static Bid createActualBid(final Object uniqueId, final String symbol, final int size, final BigDecimal price, final Date timestamp) {
		return createActualBid(uniqueId, symbol, size, price, timestamp, size);
	}

	public static Bid createActualBid(final Object uniqueId, final String symbol, final int size, final BigDecimal price, final Date timestamp,
			final int originalQuantity) {
		return new Bid(uniqueId, symbol, size, price, timestamp, false, originalQuantity);
	}

	public static Bid createSimulatedBid(final ClOrdId clOrdId, final Object uniqueId, final String symbol, final int size,
			final BigDecimal price, final Date timestamp) {
		return createSimulatedBid(clOrdId, uniqueId, symbol, size, price, timestamp, size);
	}

	public static Bid createSimulatedBid(final ClOrdId clOrdId, final Object uniqueId, final String symbol, final int size,
			final BigDecimal price, final Date timestamp, final int originalQuantity) {
		return new Bid(clOrdId, uniqueId, symbol, size, price, timestamp, true, originalQuantity);
	}

	public static BookEntry createActualBookEntry(final Class<? extends BookEntry> bookEntryClazz, final Object uniqueId, final String symbol,
			final int size, final BigDecimal price, final Date timestamp, final int originalQuantity) {
		if (Ask.class.equals(bookEntryClazz)) {
			return createActualAsk(uniqueId, symbol, size, price, timestamp, originalQuantity);
		} else if (Bid.class.equals(bookEntryClazz)) {
			return createActualBid(uniqueId, symbol, size, price, timestamp, originalQuantity);
		}
		throw createUnknownBookEntryTypeException(bookEntryClazz);
	}

	public static BookEntry createSimulatedBookEntry(final Class<? extends BookEntry> bookEntryClazz, final ClOrdId clOrdId,
			final Object uniqueId, final String symbol, final int size, final BigDecimal price, final Date timestamp) {
		if (Ask.class.equals(bookEntryClazz)) {
			return createSimulatedAsk(clOrdId, uniqueId, symbol, size, price, timestamp);
		} else if (Bid.class.equals(bookEntryClazz)) {
			return createSimulatedBid(clOrdId, uniqueId, symbol, size, price, timestamp);
		}
		throw createUnknownBookEntryTypeException(bookEntryClazz);
	}

	public static BookEntry createSimulatedBookEntry(final Class<? extends BookEntry> bookEntryClazz, final ClOrdId clOrdId,
			final Object uniqueId, final String symbol, final int size, final BigDecimal price, final Date timestamp, final int originalQuantity) {
		if (Ask.class.equals(bookEntryClazz)) {
			return createSimulatedAsk(clOrdId, uniqueId, symbol, size, price, timestamp, originalQuantity);
		} else if (Bid.class.equals(bookEntryClazz)) {
			return createSimulatedBid(clOrdId, uniqueId, symbol, size, price, timestamp, originalQuantity);
		}
		throw createUnknownBookEntryTypeException(bookEntryClazz);
	}

	private static IllegalArgumentException createUnknownBookEntryTypeException(final Class<? extends BookEntry> bookEntryClazz) {
		return new IllegalArgumentException(bookEntryClazz + " is unknown book entry type.");
	}
}