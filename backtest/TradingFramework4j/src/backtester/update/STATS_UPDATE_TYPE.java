package backtester.update;

import java.text.NumberFormat;

public enum STATS_UPDATE_TYPE {
	CLOSED_PL("Closed PnL (no fees)", true), SHARES_TRADED("Shares Traded"), TOTAL_TRANSACTION_COST("Total Cost", true), OPEN_ORDER_COUNT(
			"Open Order Count"), OPEN_ORDER_VALUE("Open Order Value", true), ORDER_COUNT("Orders Placed"), FILL_COUNT(
			"Fill Count"), MAXIMUM_EXPOSURE("Max Open Order Exposure", true), MAXIMUM_DRAWDOWN("Max Draw Down", true), SHARES_OWNED(
			"Shares Owned @ Close");

	private static final NumberFormat numberFormatter = NumberFormat.getNumberInstance();
	private static final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance();

	private String label;
	private Boolean isCurrency;

	STATS_UPDATE_TYPE(final String name) {
		this(name, false);
	}

	STATS_UPDATE_TYPE(final String label, final Boolean isCurrency) {
		this.label = label;
		this.isCurrency = isCurrency;
	}

	public Boolean isCurrency() {
		return isCurrency;
	}

	@Override
	public String toString() {
		return label;
	}

	public NumberFormat getFormatter() {
		if (isCurrency) {
			return currencyFormatter;
		} else {
			return numberFormatter;
		}
	}

	public static STATS_UPDATE_TYPE[] getTodaysEnums() {
		return new STATS_UPDATE_TYPE[] { ORDER_COUNT, FILL_COUNT, SHARES_TRADED, CLOSED_PL, TOTAL_TRANSACTION_COST, 
				OPEN_ORDER_COUNT, OPEN_ORDER_VALUE, MAXIMUM_EXPOSURE, MAXIMUM_DRAWDOWN, SHARES_OWNED};
	}
}
