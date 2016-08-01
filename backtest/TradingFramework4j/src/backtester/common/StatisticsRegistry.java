package backtester.common;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import backtester.update.STATS_UPDATE_TYPE;

public class StatisticsRegistry {
	private static final SimpleLogger _log = SimpleLogger.getLogger(StatisticsRegistry.class);

	private static StatisticsRegistry instance;

	private final Map<STATS_UPDATE_TYPE, StatMethodWrapper> statistics;

	private StatisticsRegistry() {
		statistics = new HashMap<STATS_UPDATE_TYPE, StatMethodWrapper>();
	}

	public static StatisticsRegistry getInstance() {
		if (null == instance) {
			instance = new StatisticsRegistry();
		}

		return instance;
	}

	public void addStatistic(final STATS_UPDATE_TYPE type, final Object classInstance, final String methodName) {
		addStatistic(type, classInstance, methodName, null);
	}

	public void addStatistic(final STATS_UPDATE_TYPE type, final Object classInstance, final String getterMethodName,
			final String updateMethodName, final Class<?>... parameterTypes) {
		statistics.put(type, new StatMethodWrapper(classInstance, getterMethodName, updateMethodName, parameterTypes));
	}

	public Object getValueForStatistic(final STATS_UPDATE_TYPE type) {
		Object returnValue = null;

		final StatMethodWrapper methodWrapper = statistics.get(type);

		if (null != methodWrapper) {
			returnValue = invokeMethod(methodWrapper.getClassInstance(), methodWrapper.getGetterMethodName(), null);
		}

		return returnValue;
	}

	public Object getFormattedValueForStatistic(final STATS_UPDATE_TYPE type) {
		final Object value = getValueForStatistic(type);

		if (null != value) {
			return type.getFormatter().format(value);
		} else {
			return "N/A";
		}
	}

	public Object getUpdatedValueForStatistic(final STATS_UPDATE_TYPE type, final Object value) {
		final StatMethodWrapper methodWrapper = statistics.get(type);

		Object returnValue = null;

		if (null != methodWrapper) {
			returnValue = invokeMethod(methodWrapper.getClassInstance(), methodWrapper.getUpdateMethodName(), value,
					methodWrapper.getParameterTypes());
		}

		return returnValue;
	}

	public Object getUpdatedAndFormattedValueForStatistic(final STATS_UPDATE_TYPE type, final Object value) {
		final Object returnValue = getUpdatedValueForStatistic(type, value);

		if (null != returnValue) {
			return type.getFormatter().format(returnValue);
		} else {
			return "N/A";
		}
	}

	private Object invokeMethod(Object classInstance, final String methodName, final Object value, final Class<?>... parameterTypes) {
		try {
			if (methodName.contains(".")) {
				final String innerObject = methodName.substring(0, methodName.indexOf("."));
				final String innerProperty = methodName.substring(methodName.indexOf(".") + 1);
				final Method innerMethod = classInstance.getClass().getMethod(innerObject, parameterTypes);
				classInstance = innerMethod.invoke(classInstance);

				return invokeMethod(classInstance, innerProperty, value, parameterTypes);
			}

			final Method method = classInstance.getClass().getMethod(methodName, parameterTypes);

			if (null != value) {
				return method.invoke(classInstance, value);
			} else {
				return method.invoke(classInstance);
			}
		} catch (final Exception e) {
			_log.error(e);
		}

		return null;
	}

	private class StatMethodWrapper {
		private final Object classInstance;
		private final String getterMethodName;
		private final String updateMethodName;
		private final Class<?>[] parameterTypes;

		public StatMethodWrapper(final Object classInstance, final String getterMethodName, final String updateMethodName,
				final Class<?>... parameterTypes) {
			this.classInstance = classInstance;
			this.getterMethodName = getterMethodName;
			this.updateMethodName = updateMethodName;
			this.parameterTypes = parameterTypes;
		}

		public Object getClassInstance() {
			return classInstance;
		}

		public String getGetterMethodName() {
			return getterMethodName;
		}

		public String getUpdateMethodName() {
			return updateMethodName;
		}

		public Class<?>[] getParameterTypes() {
			return parameterTypes;
		}
	}
}
