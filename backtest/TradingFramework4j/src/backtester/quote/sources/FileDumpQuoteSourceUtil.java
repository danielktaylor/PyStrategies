package backtester.quote.sources;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Pattern;

import backtester.common.SimpleLogger;
import backtester.quote.Ask;
import backtester.quote.Bid;
import backtester.quote.QuoteListener;
import backtester.quote.TradeTick;

public class FileDumpQuoteSourceUtil {
	private static final SimpleLogger _log = SimpleLogger.getLogger(FileDumpQuoteSourceUtil.class);
	private static final Pattern LINE_SPLITTER = Pattern.compile(",");
	
	private static final int TICK_TYPE = 0;
	private static final int SYMBOL = 1;
	private static final int ID = 3;
	private static final int SIZE = 4;
	private static final int PRICE = 5;
	private static final int TIME = 6;
	
	private static final String BID = "B";
	private static final String ASK = "A";
	private static final String TRADE = "T";

	private FileDumpQuoteSourceUtil() {
	}

	public static Date parseDateFromFilename(final String filename) throws ParseException {
		// Sample filename: XOM_BATS_2010-07-16.csv
		Date fileDate;
		final String[] symbolAndDate = filename.split("_");
		final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		fileDate = format.parse(symbolAndDate[2].split("\\.")[0]);
		return fileDate;
	}

	public static Date parseDateFromLineArray(final String[] parts, final Date currentDate) {
		Date lineDate = null;
		try {
			lineDate = timestampToDate(parts[TIME], currentDate);
		} catch (final IndexOutOfBoundsException e) {
		    _log.error("Malformed line:  " + Arrays.toString(parts));
		}

		return lineDate;
	}

	private static Bid createBid(final String[] parts, final Date currentSimulationDate) {
		return new Bid(parts[ID], parts[SYMBOL], Integer.parseInt(parts[SIZE]), new BigDecimal(parts[PRICE]), currentSimulationDate, false, Integer
				.parseInt(parts[SIZE]));
	}

	private static Ask createAsk(final String[] parts, final Date currentSimulationDate) {
		return new Ask(parts[FileDumpQuoteSourceUtil.ID], parts[FileDumpQuoteSourceUtil.SYMBOL], Integer
				.parseInt(parts[FileDumpQuoteSourceUtil.SIZE]), new BigDecimal(parts[FileDumpQuoteSourceUtil.PRICE]), currentSimulationDate, false, Integer
				.parseInt(parts[FileDumpQuoteSourceUtil.SIZE]));
	}

	private static TradeTick createTradeTick(final String[] parts, final Date currentSimulationDate) {
		return new TradeTick(parts[SYMBOL], Integer.parseInt(parts[SIZE]), new BigDecimal(parts[PRICE]), currentSimulationDate);
	}

	private static Date timestampToDate(final String dateString, final Date currentDate) {
		final Long millisecondsSinceMidnight = Long.parseLong(dateString);
		return new Date(currentDate.getTime() + millisecondsSinceMidnight);
	}

	public static void createAndSendCallback(final String[] parts, final Date currentSimulateDate, final QuoteListener quoteListener) {
		if (parts[FileDumpQuoteSourceUtil.TICK_TYPE].equals(FileDumpQuoteSourceUtil.ASK)) {
			final Ask ask = FileDumpQuoteSourceUtil.createAsk(parts, currentSimulateDate);
			quoteListener.onAsk(ask);
		} else if (parts[FileDumpQuoteSourceUtil.TICK_TYPE].equals(FileDumpQuoteSourceUtil.BID)) {
			final Bid bid = FileDumpQuoteSourceUtil.createBid(parts, currentSimulateDate);
			quoteListener.onBid(bid);
		} else if (parts[FileDumpQuoteSourceUtil.TICK_TYPE].equals(FileDumpQuoteSourceUtil.TRADE)) {
			final TradeTick tradeTick = FileDumpQuoteSourceUtil.createTradeTick(parts, currentSimulateDate);
			quoteListener.onTradeTick(tradeTick);
		} else {
			_log.error("Invalid tick type in file: " + Arrays.toString(parts));
		}
	}

    public static Object createCallback(final String[] parts, final Date currentSimulateDate) {
        if (parts[FileDumpQuoteSourceUtil.TICK_TYPE].equals(FileDumpQuoteSourceUtil.ASK)) {
            final Ask ask = FileDumpQuoteSourceUtil.createAsk(parts, currentSimulateDate);
            return ask;
        } else if (parts[FileDumpQuoteSourceUtil.TICK_TYPE].equals(FileDumpQuoteSourceUtil.BID)) {
            final Bid bid = FileDumpQuoteSourceUtil.createBid(parts, currentSimulateDate);
            return bid;
        } else if (parts[FileDumpQuoteSourceUtil.TICK_TYPE].equals(FileDumpQuoteSourceUtil.TRADE)) {
            final TradeTick tradeTick = FileDumpQuoteSourceUtil.createTradeTick(parts, currentSimulateDate);
            return tradeTick;
        } else {
            _log.error("Invalid tick type in file: " + Arrays.toString(parts));
            return null;
        }
    }

	public static String[] splitLine(final String line) {
		final String[] parts = LINE_SPLITTER.split(line, 0);

		// Resize internal char[] of each String to be the size of String.size() instead of line.size().
		// See http://blog.xebia.com/2007/10/04/leaking-memory-in-java/ for more information.
		for (int partsIndex = 0; partsIndex < parts.length; partsIndex++) {
			final String individualPart = new String(parts[partsIndex]);
			parts[partsIndex] = individualPart;
		}

		return parts;
	}
}
