package backtester.simulation.latency;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class DelayedMethodInvocation implements Comparable<DelayedMethodInvocation> {
	private static final AtomicLong orderingGenerator = new AtomicLong();

	private final Date executionTime;
	private final Long uniqueOrdering;

	private final Object targetObject;
	private final Method targetMethod;
	private final Object[] methodArgs;

	public DelayedMethodInvocation(final Date executionTime, final Object targetObject, final Method targetMethod, final Object[] methodArgs) {
		this.executionTime = executionTime;
		this.targetObject = targetObject;
		this.targetMethod = targetMethod;
		this.methodArgs = methodArgs;

		uniqueOrdering = orderingGenerator.getAndIncrement();
	}

	public void invoke() {
		try {
			targetMethod.invoke(targetObject, methodArgs);
		} catch (final IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (final IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (final InvocationTargetException e) {
			// Ensure failed assertions in unit tests cause tests to fail instead of just writing failed assertion to the log.
			if (e.getTargetException() instanceof AssertionError) {
				throw (AssertionError) e.getTargetException();
			}
			throw new RuntimeException(e.getTargetException());
		}
	}

	public Date getExecutionTime() {
		return executionTime;
	}

	@Override
	public int compareTo(final DelayedMethodInvocation o) {
		//We can't just use uniqueOrdering because if we use different latencies for different api methods,
		//an api invoked later could actually be executed first
		final int dateCompare = executionTime.compareTo(o.executionTime);
		if (dateCompare == 0) {
			return uniqueOrdering.compareTo(o.uniqueOrdering);
		} else {
			return dateCompare;
		}
	}

	@Override
	public String toString() {
		return "DelayedMethodInvocation [executionTime=" + executionTime + ", methodArgs=" + Arrays.toString(methodArgs) + ", targetMethod="
				+ targetMethod + ", targetObject=" + targetObject + ", uniqueOrdering=" + uniqueOrdering + "]";
	}

}
