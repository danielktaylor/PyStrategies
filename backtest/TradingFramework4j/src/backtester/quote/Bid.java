package backtester.quote;

import java.math.BigDecimal;
import java.util.Date;

import backtester.trade.ClOrdId;

public class Bid extends BookEntry implements Comparable<Bid> {
	public Bid() {
		super();
	}
	
	public Bid(final ClOrdId clOrdId, final Object uniqueId, final String symbol, final int size, final BigDecimal price, final Date timestamp,
			final boolean isSimulated, final int originalQuantity) {
		super(clOrdId, uniqueId, symbol, size, price, timestamp, isSimulated, originalQuantity);
	}

	public Bid(final Object uniqueId, final String symbol, final int size, final BigDecimal price, final Date timestamp,
			final boolean isSimulated, final int originalQuantity) {
		super(ClOrdId.getUnknownClOrdId(), uniqueId, symbol, size, price, timestamp, isSimulated, originalQuantity);
	}

	public Bid(final BookEntry other) {
		super(other);
	}

	@Override
	public int compareTo(final Bid other) {
		if (getId().equals(other.getId())) {
			return 0;
		}
		// Earliest time and highest price comes first for BIDS
		if (getPrice().compareTo(other.getPrice()) == 0) {
			return getTimestamp().compareTo(other.getTimestamp());
		} else {
			return getPrice().compareTo(other.getPrice()) * -1;
		}
	}

}
