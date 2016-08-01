package backtester.simulation;

/**
 * Latency profile that always returns zero
 */
public class ZeroLatencyProfile implements LatencyProfile {

	@Override
	public long getLatency(final Object targetObject, final String methodName) {
		return 0;
	}

}
