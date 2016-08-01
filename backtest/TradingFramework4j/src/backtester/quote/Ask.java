package backtester.quote;

import java.math.BigDecimal;
import java.util.Date;

import backtester.trade.ClOrdId;

public class Ask extends BookEntry implements Comparable<Ask> {
	
	public Ask() {
		super();
	}
	
	public Ask(final ClOrdId clOrdId, final Object uniqueId, final String symbol, final int size, final BigDecimal price, final Date timestamp,
			final boolean isSimulated, final int originalQuantity) {
		super(clOrdId, uniqueId, symbol, size, price, timestamp, isSimulated, originalQuantity);
	}

	public Ask(final Object uniqueId, final String symbol, final int size, final BigDecimal price, final Date timestamp,
			final boolean isSimulated, final int originalQuantity) {
		super(ClOrdId.getUnknownClOrdId(), uniqueId, symbol, size, price, timestamp, isSimulated, originalQuantity);
	}

	public Ask(final BookEntry other) {
		super(other);
	}

	@Override
	public int compareTo(final Ask other) {
		if (getId().equals(other.getId())) {
			return 0;
		}
		// Earliest time and LOWEST price comes first for ASKS
		if (getPrice().compareTo(other.getPrice()) == 0) {
			return getTimestamp().compareTo(other.getTimestamp());
		} else {
			return getPrice().compareTo(other.getPrice());
		}
	}
}
