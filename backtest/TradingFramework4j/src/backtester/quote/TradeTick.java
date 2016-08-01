package backtester.quote;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TradeTick {
    private static final String DELIMITER = ",";
    private final String symbol;
    private final int size;
    private final BigDecimal price;
    private final Date timestamp;

    public TradeTick(final String symbol, final int size, final BigDecimal price, final Date timestamp) {
        this.symbol = symbol;
        this.size = size;
        this.price = price;
        this.timestamp = timestamp;
    }

    @SuppressWarnings("deprecation")
    public long getMilliTimestamp() {
        final Date date = new Date(timestamp.getYear(), timestamp.getMonth(), timestamp.getDate());
        return timestamp.getTime() - date.getTime();
    }

    public String getSymbol() {
        return symbol;
    }

    public int getSize() {
        return size;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return formatDateForDisplay(timestamp) + DELIMITER + formatTimeForDisplay(timestamp) + DELIMITER + price + DELIMITER + size;
    }

    private String formatTimeForDisplay(final Date date) {
        final SimpleDateFormat sdf = new SimpleDateFormat("kk:mm:ss");
        return sdf.format(date);
    }

    private String formatDateForDisplay(final Date date) {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        return sdf.format(date);
    }

    public static String getCsvHeader() {
        return "Date" + DELIMITER + "Time" + DELIMITER + "Price" + DELIMITER + "Volume";
    }
}
