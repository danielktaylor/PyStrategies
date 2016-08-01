package backtester.common;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;

public class SimpleLogger {
	private final Logger logger;

	public static SimpleLogger getLogger(final Class<?> cls) {
		return new SimpleLogger(Logger.getLogger(cls.getName()));
	}

	private SimpleLogger(final Logger logger) {
		this.logger = logger;
	}

	public void debug(final Throwable t) {
		logger.debug(t.getMessage(), t);
	}

	public void debug(final String message) {
		logger.debug(message);
	}

	public void error(final Throwable t) {
		logger.error(t.getMessage(), t);
	}

	public void error(final String message) {
		logger.error(message);
	}

	public void fatal(final Throwable t) {
		logger.fatal(t.getMessage(), t);
	}

	public void fatal(final String message) {
		logger.fatal(message);
	}

	public void info(final String message) {
		logger.info(message);
	}

	public void warn(final String message) {
		logger.warn(message);
	}

	public void warn(final Throwable t) {
		logger.warn(t.getMessage(), t);
	}
	
	public void addAppender(Appender appender) {
		logger.addAppender(appender);
	}
}
