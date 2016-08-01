package backtester.quote;

public class QuoteEntry {
	private final Bid bid;
	private final Ask ask;

	public QuoteEntry(final Bid bid, final Ask ask) {
		this.ask = ask;
		this.bid = bid;
	}

	public Bid getBid() {
		return bid;
	}

	public Ask getAsk() {
		return ask;
	}
}
