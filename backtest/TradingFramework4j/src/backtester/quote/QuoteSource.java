package backtester.quote;

import java.util.Observer;

public interface QuoteSource {
	void setQuoteListener(QuoteListener quoteListener);

	void addObserver(final Observer o);

	void initialize();

	void stop();

	void playForTime(long howLongToPlayFor, long delay);

	void playNumberOfLines(long numberOfLinesToPlay, long delay);

	void playAll();

	void reset();

	void plugMemoryLeak();
}
