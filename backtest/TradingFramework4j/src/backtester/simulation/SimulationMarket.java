package backtester.simulation;

import java.util.Date;
import java.util.Observer;

import backtester.quote.QuoteListener;
import backtester.quote.QuoteSource;
import backtester.trade.TradeDestination;




public interface SimulationMarket extends TradeDestination, QuoteListener, QuoteSource {
	void setCurrentTime(final Date newTime);

	// SimulationMarket interface cannot extend Observable.  This is why this method is defined here.
	void addObserver(Observer o);

	void reset();
}