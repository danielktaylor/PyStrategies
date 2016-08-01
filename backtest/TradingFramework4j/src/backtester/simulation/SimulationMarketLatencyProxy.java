package backtester.simulation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.PriorityQueue;

import backtester.quote.BookEntry;
import backtester.quote.QuoteListener;
import backtester.quote.TradeTick;
import backtester.simulation.latency.DelayedMethodInvocation;
import backtester.trade.TradeListener;

public class SimulationMarketLatencyProxy implements InvocationHandler {
	private final SimulationMarket simulationMarket;
	private TradeListener tradeListener;
	private QuoteListener quoteListener;
	private final MarketTime marketTime = MarketTime.getInstance();
	private final LatencyProfile latencyProfile;

	private final PriorityQueue<DelayedMethodInvocation> invocationQueue = new PriorityQueue<DelayedMethodInvocation>();

	private SimulationMarketLatencyProxy(final SimulationMarket simulationMarket, final LatencyProfile latencyProfile) {
		this.simulationMarket = simulationMarket;
		this.latencyProfile = latencyProfile;

		//I can set these proxies now or lazily in invoke...
		final QuoteListener quoteProxy = (QuoteListener) Proxy.newProxyInstance(this.getClass().getClassLoader(),
				new Class[] { QuoteListener.class }, this);
		simulationMarket.setQuoteListener(quoteProxy);

		final TradeListener tradeProxy = (TradeListener) Proxy.newProxyInstance(this.getClass().getClassLoader(),
				new Class[] { TradeListener.class }, this);
		simulationMarket.setTradeListener(tradeProxy);
	}

	public static SimulationMarket createSimulationMarketLatencyProxy(final SimulationMarket simulationMarket,
			final LatencyProfile latencyProfile) {
		return (SimulationMarket) Proxy.newProxyInstance(simulationMarket.getClass().getClassLoader(), simulationMarket.getClass()
				.getInterfaces(), new SimulationMarketLatencyProxy(simulationMarket, latencyProfile));
	}

	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
		final String methodName = method.getName();

		Object targetObject;

		if (proxy instanceof SimulationMarket) {
			if ("onAsk".equals(methodName) || "onBid".equals(methodName)) {
				setCurrentTime(((BookEntry) args[0]).getTimestamp());
			} else if ("onTradeTick".equals(methodName)) {
				setCurrentTime(((TradeTick) args[0]).getTimestamp());
			} else if ("setCurrentTime".equals(methodName)) {
				setCurrentTime((Date) args[0]);
			} else if ("setQuoteListener".equals(methodName)) {
				this.quoteListener = (QuoteListener) args[0];
				return null;
			} else if ("setTradeListener".equals(methodName)) {
				this.tradeListener = (TradeListener) args[0];
				return null;
			} else if ("reset".equals(methodName)) {
				reset();
			}

			targetObject = simulationMarket;
		} else if (proxy instanceof QuoteListener) {
			targetObject = quoteListener;
		} else {
			targetObject = tradeListener;
		}

		//Not sure if this null check is correct, it prevents callbacks from being queued even
		//if the listener is set before latency expires
		if (targetObject != null) {
			final long latency = latencyProfile.getLatency(targetObject, methodName);
			if (latency != 0) {
				final Date executionTime = addLatencyToCurrentTime(latency);
				invocationQueue.add(new DelayedMethodInvocation(executionTime, targetObject, method, args));
			} else {
				try {
					final Object invocationReturnValue = method.invoke(targetObject, args);
					return invocationReturnValue;
				} catch (final InvocationTargetException e) {
					throw e.getTargetException();
				} catch (final Exception e) {
					throw new RuntimeException(e.getCause());
				}
			}
		}

		return null;
	}

	private Date addLatencyToCurrentTime(final Long latency) {
		return new Date(marketTime.getTime().getTime() + latency);
	}

	private void setCurrentTime(final Date newTime) {
		while (!invocationQueue.isEmpty() && !invocationQueue.peek().getExecutionTime().after(newTime)) {
			final DelayedMethodInvocation methodInvocation = invocationQueue.remove();
			marketTime.setTime(methodInvocation.getExecutionTime());
			methodInvocation.invoke();
		}
		marketTime.setTime(newTime);
	}

	private void reset() {
		marketTime.resetTime();
		invocationQueue.clear();
	}
}
