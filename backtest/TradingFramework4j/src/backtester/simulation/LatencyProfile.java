package backtester.simulation;

public interface LatencyProfile {
	long getLatency(Object targetObject, String methodName);
}
