package backtester.simulation;

import java.util.Date;

import backtester.common.SimpleLogger;

public class MarketTime {
	private static final SimpleLogger _log = SimpleLogger.getLogger(MarketTime.class);
	private static final MarketTime instance = new MarketTime();
	private Date currentTime;

	public MarketTime() {
		currentTime = new Date(0);
	}

	public void setTime(final Date _date) {
		if (currentTime.compareTo(_date) == 1) {
			_log.warn("Time went backwards at time " + _date.getTime() + ", not updating simulator time.");
			return;
		}
		currentTime = _date;
	}

	public void resetTime() {
		currentTime = new Date(0);
	}

	public Date getTime() {
		return currentTime;
	}

	public static MarketTime getInstance() {
		return instance;
	}
}
