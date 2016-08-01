package backtester;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

import backtester.common.SimpleLogger;
import backtester.quote.QuoteSource;
import backtester.quote.sources.FileDumpQuoteSource;
import backtester.quote.sources.FileDumpQuoteSourceUtil;
import backtester.simulation.DefaultLatencyProfile;
import backtester.simulation.DefaultSimulationMarket;
import backtester.simulation.SimulationMarket;
import backtester.simulation.SimulationMarketLatencyProxy;
import backtester.strategy.BaseStrategy;
import backtester.strategy.PythonStrategy;
import backtester.strategy.StrategyConfiguration;

public class Backtester implements Observer {
	private static SimpleLogger _log = SimpleLogger.getLogger(Backtester.class);
	private Constructor<? extends BaseStrategy> strategyConstructor;
	
	private SimulationMarket simulationMarket;
	private DefaultSimulationMarket innerMarket;
	private QuoteSource quoteSource;
	private BaseStrategy strategy;
	private String quoteFile;
	private static String pythonStrategy;
	private static String presetConfiguration;
	
	public Backtester(final Observer gui, final Class<? extends BaseStrategy> strategyClass, final String quoteFile) {
		this.quoteFile = quoteFile;
		
		try {
			strategyConstructor = strategyClass.getConstructor(strategyClass, StrategyConfiguration.class);
		} catch (final SecurityException e) {
			_log.error(e);
		} catch (final NoSuchMethodException e) {
			_log.error(e);
		}
	}
	
	public void playForTime(final long time, final long delay) throws Exception {
		quoteSource.playForTime(time, delay);
	}

	public void playNumberOfLines(final long lines, final long delay) throws Exception {
		quoteSource.playNumberOfLines(lines, delay);
	}

	public void reset() {
		quoteSource.reset();
		strategy.reset();
		simulationMarket.reset();
	}

	public BaseStrategy getStrategy() {
		return strategy;
	}
	
	public void testStrategy() throws Exception {
		_log.info("Starting test run at " + new Date());
		
		final File quoteSourceFile = new File(quoteFile);
		final Date tradingDate = FileDumpQuoteSourceUtil.parseDateFromFilename(quoteSourceFile.getName());
		quoteSource = new FileDumpQuoteSource(quoteSourceFile);

		innerMarket = new DefaultSimulationMarket(innerMarket) ;
		
		simulationMarket = SimulationMarketLatencyProxy.createSimulationMarketLatencyProxy(innerMarket, new DefaultLatencyProfile());
		final StrategyConfiguration strategyConfiguration = new StrategyConfiguration(simulationMarket, tradingDate);
		
		if (pythonStrategy != null) {
			strategyConfiguration.setVariable("pythonStrategy", pythonStrategy);
		}
		
		if (presetConfiguration != null) {
			strategyConfiguration.setVariable("pythonConfig", presetConfiguration);
		}
		
		strategy = strategyConstructor.newInstance(strategy, strategyConfiguration);
		
		quoteSource.setQuoteListener(simulationMarket);
		simulationMarket.setQuoteListener(strategy);
		simulationMarket.setTradeListener(strategy);
		quoteSource.initialize();
		quoteSource.playAll();
		quoteSource.stop();
		quoteSource.plugMemoryLeak();
		
		_log.info(strategy.getEndOfDayReport());
		strategy.onPlaybackEnd();
	}

	public static void main(final String[] args) throws Exception {
		Backtester backtester = null;
		if (args.length == 1) {
			backtester = new Backtester(null, PythonStrategy.class, args[0]);
		} else if (args.length == 2) {
			pythonStrategy = args[1];
			backtester = new Backtester(null, PythonStrategy.class, args[0]);
		} else if (args.length == 3) {
			pythonStrategy = args[1];
			presetConfiguration = args[2]; // e.g. "var1=1.0;var2=5.0"
			backtester = new Backtester(null, PythonStrategy.class, args[0]);
		} else {
			System.out.println("Usage: Backtester quotes.csv <optional python strategy file> <optional python strategy config>");
			System.exit(0);
		}
		
		backtester.testStrategy();
	}

	@Override
	public void update(final Observable o, final Object arg) {
		
	}
}
