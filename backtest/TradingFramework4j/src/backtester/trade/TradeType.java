package backtester.trade;

import backtester.quote.Ask;
import backtester.quote.Bid;
import backtester.quote.BookEntry;

public enum TradeType {
	BUY, SELL, COVER, SHORT;

	public boolean isOnSameSideOfBook(final BookEntry bookEntry) {
		if (bookEntry instanceof Ask) {
			return this.equals(SELL) || this.equals(SHORT);
		} else if (bookEntry instanceof Bid) {
			return this.equals(BUY) || this.equals(COVER);
		}
		throw new IllegalArgumentException("Unknown book entry type");
	}
}
