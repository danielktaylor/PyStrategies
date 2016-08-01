package backtester.update;

public class QuoteSourceUpdate {

	private Object value;
	private QUOTE_SOURCE_UPDATE_TYPE type;

	public static enum QUOTE_SOURCE_UPDATE_TYPE {
		CURRENT_LINE, TOTAL_LINES, CURRENT_TIME, END_TIME, SOURCE_LOADED, END_OF_DAY
	}

	public QuoteSourceUpdate(Object value, QUOTE_SOURCE_UPDATE_TYPE type) {
		this.value = value;
		this.type = type;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public QUOTE_SOURCE_UPDATE_TYPE getType() {
		return type;
	}

	public void setType(QUOTE_SOURCE_UPDATE_TYPE type) {
		this.type = type;
	}
}
