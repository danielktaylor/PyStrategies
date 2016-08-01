package backtester.trade;

public class TradingException extends Exception {
	private static final long serialVersionUID = -7881921749192048635L;

	public TradingException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public TradingException(final String message) {
		super(message);
	}

	public TradingException(final Throwable cause) {
		super(cause);
	}

}
