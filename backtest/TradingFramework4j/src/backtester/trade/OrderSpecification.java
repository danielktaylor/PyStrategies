package backtester.trade;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import backtester.quote.Ask;
import backtester.quote.Bid;
import backtester.quote.BookEntry;

public class OrderSpecification {

    private static AtomicLong uniqueId = new AtomicLong();
    private final Long id;
    private BigDecimal price;
    private int quantity;
    private int amountFilled;
    private String symbol;
    private TradeType tradeType;
    private OrderStatus orderStatus;
    private Long lastChange;
    private List<Fill> fillHistory;

    public OrderSpecification(final String symbol, final BigDecimal price, final int quantity, final TradeType tradeType) {
        this.id = uniqueId.getAndIncrement();
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.amountFilled = 0;
        this.tradeType = tradeType;
        this.lastChange = System.currentTimeMillis();
        this.fillHistory = new ArrayList<Fill>();
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
        final OrderSpecification otherOrderSpecification = (OrderSpecification) obj;
        return uniqueId.equals(otherOrderSpecification.getId());
    }

    public AtomicLong getId() {
        return uniqueId;
    }

    public int getAmountFilled() {
        return amountFilled;
    }

    public void setAmountFilled(final int amountFilled) {
        this.amountFilled = amountFilled;
    }

    public List<Fill> getFillHistory() {
        return fillHistory;
    }

    public void setFillHistory(final List<Fill> fillHistory) {
        this.fillHistory = fillHistory;
    }

    public Long getLastChange() {
        return lastChange;
    }

    public void setLastChange(final Long lastChange) {
        this.lastChange = lastChange;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(final OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(final BigDecimal price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(final int quantity) {
        this.quantity = quantity;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(final String symbol) {
        this.symbol = symbol;
    }

    public TradeType getTradeType() {
        return tradeType;
    }

    public void setTradeType(final TradeType tradeType) {
        this.tradeType = tradeType;
    }

    public static AtomicLong getUniqueId() {
        return uniqueId;
    }

    public static void setUniqueId(final AtomicLong uniqueId) {
        OrderSpecification.uniqueId = uniqueId;
    }

    public Class<? extends BookEntry> getBookEntryClass() {
        switch (tradeType) {
        case BUY:
        case COVER:
            return Bid.class;
        case SELL:
        case SHORT:
            return Ask.class;
        }
        throw new IllegalArgumentException("Unknown trade type " + tradeType + ".");
    }

    @Override
    public String toString() {
		return "OrderSpecification{" + "id=" + id + ", price=" + price + ", quantity=" + quantity + ", amountFilled=" + amountFilled
				+ ", symbol=" + symbol + ", tradeType=" + tradeType + ", orderStatus=" + orderStatus + ", lastChange=" + lastChange
				+ ", fillHistory=" + fillHistory + '}';
    }
}
