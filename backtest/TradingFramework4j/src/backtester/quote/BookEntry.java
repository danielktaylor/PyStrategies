package backtester.quote;

import java.math.BigDecimal;
import java.util.Date;

import backtester.trade.ClOrdId;

public abstract class BookEntry {
	private final Object uniqueId;
	private final ClOrdId clOrdId;
	private final String symbol;
	private int remainingQuantity;
	private final BigDecimal price;
	private Date timestamp;
	private Date insertionTimestamp;
	private final boolean isSimulated;
	private int dirtyQuantity;
	private int originalQuantity;

	public BookEntry() {
		uniqueId = null;
		clOrdId = null;
		symbol = null;
		remainingQuantity = 0;
		price = BigDecimal.ZERO;
		timestamp = null;
		insertionTimestamp = null;
		isSimulated = false;
		dirtyQuantity = 0;
		originalQuantity = 0;
	}

	public BookEntry(final ClOrdId clOrdId, final Object uniqueId, final String symbol, final int remainingQuantity, final BigDecimal price,
			final Date timestamp, final boolean isSimulated, final int originalQuantity) {
		this.clOrdId = clOrdId;
		this.uniqueId = uniqueId;
		this.symbol = symbol;
		if (remainingQuantity < 0 || originalQuantity < 0) {
			throw new IllegalArgumentException("Original quantity cannot be negative.");
		}
		this.remainingQuantity = remainingQuantity;
		this.price = price;
		if (price.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Price cannot be negative.");
		}
		this.timestamp = timestamp;
		this.isSimulated = isSimulated;
		this.originalQuantity = originalQuantity;
	}

	public BookEntry(final BookEntry other) {
		this(other.getClOrdId(), other.getId(), other.getSymbol(), other.getRemainingQuantity(), other.getPrice(), other.getTimestamp(), other.isSimulated(),
				other.getOriginalQuantity());
	}

	public Object getId() {
		return uniqueId;
	}

	public int getFilledQuantity() {
		return originalQuantity - remainingQuantity;
	}
	
	public void setOriginalQuantity(int qty) {
		this.originalQuantity = qty;
	}
	
	public void setRemainingQuantity(int qty) {
		this.remainingQuantity = qty;
	}

	public int getOriginalQuantity() {
		return originalQuantity;
	}

	public String getSymbol() {
		return symbol;
	}

	public int getRemainingQuantity() {
		return remainingQuantity;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public Date getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	
	public Date getInsertionTimestamp() {
		return insertionTimestamp;
	}
	
	public void setInsertionTimestamp(Date insertionTimestamp) {
		this.insertionTimestamp = insertionTimestamp;
	}

	public boolean isSimulated() {
		return isSimulated;
	}

	public void setDirtyQuantity(final int dirtyQuantity) {
		this.dirtyQuantity = dirtyQuantity;
	}

	public int getDirtyQuantity() {
		return dirtyQuantity;
	}

	@SuppressWarnings("deprecation")
	public long getMilliTimestamp() {
		final Date date = new Date(timestamp.getYear(), timestamp.getMonth(), timestamp.getDate());
		return timestamp.getTime() - date.getTime();
	}

	public boolean isMarketOrder() {
		return price.compareTo(BigDecimal.ZERO) == 0;
	}

	@Override
	public String toString() {
		return "BookEntry [class=" + getClass().getSimpleName() + ", dirtyQuantity=" + dirtyQuantity + ", isSimulated=" + isSimulated
				+ ", price=" + price + ", size=" + remainingQuantity + ", symbol=" + symbol + ", timestamp=" + timestamp + ", uniqueId=" + uniqueId + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (uniqueId == null ? 0 : uniqueId.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final BookEntry other = (BookEntry) obj;
		if (uniqueId == null) {
			if (other.uniqueId != null) {
				return false;
			}
		} else if (!uniqueId.equals(other.uniqueId)) {
			return false;
		}
		return true;
	}

	public ClOrdId getClOrdId() {
		return clOrdId;
	}

}
