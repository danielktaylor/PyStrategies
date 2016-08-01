package backtester.trade;

import java.math.BigDecimal;
import java.util.Date;

public class Fill {
	private final OrderId orderId;
	private final String symbol;
	private final int quantity; // negative quantity for sell/short, positive for buy/cover
	private final BigDecimal price;
	private final int remaining;
	private final Date timestamp;
	private final ClOrdId clOrdId;
	private final LiquidityFlag liquidityFlag;

	public Fill(final ClOrdId clOrdId, final OrderId orderId, final String symbol, final int quantity, final BigDecimal price,
			final int remaining, final Date timestamp, final LiquidityFlag liquidityFlag) {
		this.orderId = orderId;
		this.symbol = symbol;
		this.quantity = quantity;
		this.price = price;
		this.remaining = remaining;
		this.timestamp = timestamp;
		this.clOrdId = clOrdId;
		this.liquidityFlag = liquidityFlag;
	}

	@SuppressWarnings("deprecation")
	public long getMilliTimestamp() {
		final Date date = new Date(timestamp.getYear(), timestamp.getMonth(), timestamp.getDate());
		return timestamp.getTime() - date.getTime();
	}

	public OrderId getOrderId() {
		return orderId;
	}

	public String getSymbol() {
		return symbol;
	}

	public int getQuantity() {
		return quantity;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public int getRemaining() {
		return remaining;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public ClOrdId getClOrdId() {
		return clOrdId;
	}

	public LiquidityFlag getLiquidityFlag() {
	    return liquidityFlag;
	}
}
