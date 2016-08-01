package backtester.update;

public class StatsUpdate {
	private Object value;
	private STATS_UPDATE_TYPE type;

	public StatsUpdate(final Object value, final STATS_UPDATE_TYPE type) {
		super();
		this.value = value;
		this.type = type;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(final Object value) {
		this.value = value;
	}

	public STATS_UPDATE_TYPE getType() {
		return type;
	}

	public void setType(final STATS_UPDATE_TYPE type) {
		this.type = type;
	}

	public String getFormattedValue() {
		return type.getFormatter().format(value);
	}
}
