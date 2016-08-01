package backtester.quote.sources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import backtester.common.SimpleLogger;
import backtester.quote.QuoteListener;
import backtester.quote.QuoteSource;
import backtester.update.QuoteSourceUpdate;
import backtester.update.QuoteSourceUpdate.QUOTE_SOURCE_UPDATE_TYPE;

public class FileDumpQuoteSource extends Observable implements QuoteSource {

	private static SimpleLogger _log = SimpleLogger.getLogger(FileDumpQuoteSource.class);

	private QuoteListener quoteListener;
	private final List<String[]> listOfLines = new ArrayList<String[]>();
	private final List<String> listOfLinesUnsplit = new ArrayList<String>();
	private boolean initialized = false;
	private boolean initializing = false;
	private boolean running = false;
	private final File quoteDumpFile;
	private final Date currentDate;
	private long currentSimulationTime = -1;
	private long simulationEndTime = -1;
	private int currentLine = 0;
	private int totalLines = 0;
	private boolean hasAnyObservers;

	public String getUnsplitLine(int index) {
		return listOfLinesUnsplit.get(index);
	}
	
	public FileDumpQuoteSource(final File quoteDumpFile) throws ParseException {
		this.quoteDumpFile = quoteDumpFile;
		currentDate = FileDumpQuoteSourceUtil.parseDateFromFilename(quoteDumpFile.getName());

		currentLine = 0;
	}

	@Override
	public void addObserver(final Observer o) {
		super.addObserver(o);
		hasAnyObservers = true;
	}

	@Override
	public void setQuoteListener(final QuoteListener quoteListener) {
		this.quoteListener = quoteListener;
	}

	@Override
	public void playForTime(long howLongToPlayFor, final long delay) {
		running = true;
		if (currentSimulationTime + howLongToPlayFor > simulationEndTime) {
			howLongToPlayFor = simulationEndTime - currentSimulationTime;
		}

		final long endTime = currentSimulationTime + howLongToPlayFor;

		while (currentSimulationTime < endTime && running) {
			final String[] line = listOfLines.get(currentLine);
			parseLine(line);

			try {
				Thread.sleep(delay);
			} catch (final InterruptedException e) {
				_log.error(e);
			}
		}

		stop();
	}

	@Override
	public void playNumberOfLines(long numberOfLinesToPlay, final long delay) {
		running = true;
		if (currentLine + numberOfLinesToPlay > totalLines) {
			numberOfLinesToPlay = totalLines - currentLine;
		}

		final long endLine = currentLine + numberOfLinesToPlay;

		while (currentLine < endLine && running) {
			final String[] line = listOfLines.get(currentLine);
			parseLine(line);

			try {
				Thread.sleep(delay);
			} catch (final InterruptedException e) {
				_log.error(e);
			}
		}

		stop();
	}

	@Override
	public void playAll() {
		running = true;
		while (currentLine < totalLines && running) {
			final String[] line = listOfLines.get(currentLine);
			parseLine(line);
		}

		stop();
	}

	private void initializeQuoteSource() {
		initialized = false;
		initializing = true;

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(quoteDumpFile));

			String line = reader.readLine();
			
			listOfLinesUnsplit.clear();
			
			while (line != null) {
				listOfLinesUnsplit.add(line);
				
				final String[] parts = FileDumpQuoteSourceUtil.splitLine(line);

				listOfLines.add(parts);

				if (listOfLines.size() % 10000 == 0) {
					setTotalLines(listOfLines.size());
				}

				line = reader.readLine();
			}
		} catch (final FileNotFoundException e) {
			_log.fatal(e);
		} catch (final IOException e) {
			_log.fatal(e);
		} finally {
			try {
				reader.close();
			} catch (final IOException e) {
				// Swallow.
			}
		}

		if (listOfLines.size() > 0) {
			final String[] lastLine = listOfLines.get(listOfLines.size() - 1);
			setSimulationEndTime(FileDumpQuoteSourceUtil.parseDateFromLineArray(lastLine, currentDate).getTime());
		}

		setTotalLines(listOfLines.size());

		sendUpdate(simulationEndTime, QUOTE_SOURCE_UPDATE_TYPE.SOURCE_LOADED);

		initializing = false;
		initialized = true;
	}

	public void initialize() {
		if (!initialized && !initializing) {
			if (hasAnyObservers) {
				new Thread() {
					@Override
					public void run() {
						initializeQuoteSource();
					}
				}.start();
			} else {
				initializeQuoteSource();
			}
		}
	}

	@Override
	public void stop() {
		running = false;
	}

	public boolean isRunning() {
		return running;
	}

	protected void parseLine(final String[] parts) {
		if (parts != null) {
			final Date currentSimulationTime;

			currentSimulationTime = FileDumpQuoteSourceUtil.parseDateFromLineArray(parts, currentDate);
			setCurrentSimulationTime(currentSimulationTime.getTime());

			FileDumpQuoteSourceUtil.createAndSendCallback(parts, currentSimulationTime, quoteListener);

			setCurrentLine(++currentLine);
		}
	}

	public int getCurrentLine() {
		return currentLine;
	}

	public int getLinesProcessed() {
		return currentLine;
	}

	public void setCurrentLine(final int currentLine) {
		this.currentLine = currentLine;

		sendUpdate(currentLine, QUOTE_SOURCE_UPDATE_TYPE.CURRENT_LINE);

		if (currentLine == totalLines) {
			sendUpdate(null, QUOTE_SOURCE_UPDATE_TYPE.END_OF_DAY);
		}
	}

	private void setTotalLines(final int totalLines) {
		this.totalLines = totalLines;

		sendUpdate(totalLines, QUOTE_SOURCE_UPDATE_TYPE.TOTAL_LINES);
	}

	public long getCurrentSimulationTime() {
		return currentSimulationTime;
	}

	public void setCurrentSimulationTime(final long currentSimulationTime) {
		if (this.currentSimulationTime > currentSimulationTime) {
			_log.warn("Time went backwards at time " + currentSimulationTime + ", not updating FileDumpQuoteSource time.");
			return;
		}

		this.currentSimulationTime = currentSimulationTime;

		sendUpdate(currentSimulationTime, QUOTE_SOURCE_UPDATE_TYPE.CURRENT_TIME);
	}

	public long getSimulationEndTime() {
		return simulationEndTime;
	}

	public void setSimulationEndTime(final long simulationEndTime) {
		this.simulationEndTime = simulationEndTime;

		sendUpdate(simulationEndTime, QUOTE_SOURCE_UPDATE_TYPE.END_TIME);
	}

	public void plugMemoryLeak() {
		listOfLines.clear();
	}

	public void reset() {
		stop();
		currentSimulationTime = -1;
		currentLine = 0;

		setSimulationEndTime(getSimulationEndTime());
		setTotalLines(totalLines);
	}

	private void sendUpdate(final Object value, final QUOTE_SOURCE_UPDATE_TYPE type) {
		if (hasAnyObservers) {
			setChanged();
			notifyObservers(new QuoteSourceUpdate(value, type));
		}
	}
}
