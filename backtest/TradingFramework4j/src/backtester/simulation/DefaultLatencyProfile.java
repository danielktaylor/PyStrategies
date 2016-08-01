package backtester.simulation;

import java.util.HashMap;
import java.util.Map;

import backtester.quote.QuoteListener;
import backtester.trade.TradeListener;

/**
 * Latency profile of a general market
 */
public class DefaultLatencyProfile implements LatencyProfile {
	public static final Long SIMULATED_LINK_LATENCY_MILLIS = 4L;
	public static final Long SIMULATED_STRATEGY_PROCESSING_LATENCY = 1L;
	public static final Long SIMULATED_ORDER_LATENCY_MILLIS = SIMULATED_LINK_LATENCY_MILLIS + SIMULATED_STRATEGY_PROCESSING_LATENCY;

	private final Map<String, Long> marketLatencyMap = new HashMap<String, Long>();
	private final Map<String, Long> quoteListenerLatencyMap = new HashMap<String, Long>();
	private final Map<String, Long> tradeListenerLatencyMap = new HashMap<String, Long>();

	public DefaultLatencyProfile() {
		//Latencies to the Market (Strategy->Market)
		marketLatencyMap.put("placeOrder", SIMULATED_ORDER_LATENCY_MILLIS);
		marketLatencyMap.put("cancelOrder", SIMULATED_ORDER_LATENCY_MILLIS);
		marketLatencyMap.put("cancelReplaceOrder", SIMULATED_ORDER_LATENCY_MILLIS);
		marketLatencyMap.put("cancelAll", SIMULATED_ORDER_LATENCY_MILLIS);

		//Latencies to the QuoteListener (Market->Strategy)
		quoteListenerLatencyMap.put("onBid", SIMULATED_LINK_LATENCY_MILLIS);
		quoteListenerLatencyMap.put("onAsk", SIMULATED_LINK_LATENCY_MILLIS);
		quoteListenerLatencyMap.put("onTradeTick", SIMULATED_LINK_LATENCY_MILLIS);

		//Latencies to trade listener (Market->Strategy)
		tradeListenerLatencyMap.put("onFill", SIMULATED_LINK_LATENCY_MILLIS);
		tradeListenerLatencyMap.put("onTransactionCost", SIMULATED_LINK_LATENCY_MILLIS);
		tradeListenerLatencyMap.put("onCancelReplaceAccepted", SIMULATED_LINK_LATENCY_MILLIS);
		tradeListenerLatencyMap.put("onCancelReplaceRejected", SIMULATED_LINK_LATENCY_MILLIS);
		tradeListenerLatencyMap.put("onCancelAccepted", SIMULATED_LINK_LATENCY_MILLIS);
		tradeListenerLatencyMap.put("onCancelRejected", SIMULATED_LINK_LATENCY_MILLIS);
		tradeListenerLatencyMap.put("onCancelAllAccepted", SIMULATED_LINK_LATENCY_MILLIS);
		tradeListenerLatencyMap.put("onCancelAllRejected", SIMULATED_LINK_LATENCY_MILLIS);
		tradeListenerLatencyMap.put("onNewOrderAccepted", SIMULATED_LINK_LATENCY_MILLIS);
		tradeListenerLatencyMap.put("onNewOrderRejected", SIMULATED_LINK_LATENCY_MILLIS);
	}

	@Override
	public long getLatency(final Object targetObject, final String methodName) {
		Map<String, Long> latencyMap = null;

		if (targetObject instanceof SimulationMarket) {
			latencyMap = marketLatencyMap;
		} else if (targetObject instanceof QuoteListener) {
			latencyMap = quoteListenerLatencyMap;
		} else if (targetObject instanceof TradeListener) {
			latencyMap = tradeListenerLatencyMap;
		}

		if (latencyMap != null && latencyMap.containsKey(methodName)) {
			return latencyMap.get(methodName);
		}

		return 0;
	}
}
