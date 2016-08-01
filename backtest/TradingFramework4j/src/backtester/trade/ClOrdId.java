package backtester.trade;

import backtester.simulation.UnknownClOrdId;

public abstract class ClOrdId {
	private static final UnknownClOrdId unknownClOrdId = new UnknownClOrdId();

	public static ClOrdId getUnknownClOrdId() {
		return unknownClOrdId;
	}
}