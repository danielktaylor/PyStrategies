package backtester.quote;

public interface QuoteListener {
	void onBid(Bid bid);

	void onAsk(Ask ask);

	void onTradeTick(TradeTick tradeTick);
}
