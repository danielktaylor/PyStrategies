package backtester.trade;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class Position {

	private final String symbol;
	private BigDecimal closedPL;
	private BigDecimal totalCost;
	private BigDecimal shares;
	final static MathContext MATH_CONTEXT = new MathContext(16, RoundingMode.HALF_EVEN);

	public Position() {
		symbol = "";
		closedPL = BigDecimal.ZERO;
		totalCost = BigDecimal.ZERO;
		shares = BigDecimal.ZERO;
	}

	public Position(final Position other) {
		symbol = other.symbol;
		closedPL = other.closedPL;
		totalCost = other.totalCost;
		shares = other.shares;
	}

	public Position(final String symbol, final BigDecimal shares, final BigDecimal totalCost, final BigDecimal closedPl) {
		if (symbol == null || shares == null || totalCost == null || closedPl == null) {
			throw new IllegalArgumentException();
		}
		try {
			shares.toBigIntegerExact();
		} catch (final ArithmeticException e) {
			throw new IllegalArgumentException("Fraction share count not allowed:" + shares);
		}

		this.symbol = symbol;
		this.closedPL = closedPl;
		this.totalCost = totalCost;
		this.shares = shares;
	}

	public String getSymbol() {
		return symbol;
	}

	public BigDecimal getClosedPL() {
		return closedPL;
	}

	public BigDecimal getTotalCost() {
		return totalCost;
	}

	public BigDecimal getShares() {
		return shares;
	}

	public BigDecimal getAveragePrice() {
		if (shares.compareTo(BigDecimal.ZERO) != 0) {
			return totalCost.divide(shares, MATH_CONTEXT);
		} else {
			return BigDecimal.ZERO;
		}
	}

	private BigDecimal getAmountClosed(final BigDecimal fillQty) {

		if (fillQty.signum() == shares.signum()) {
			return BigDecimal.ZERO;
		}

		if (shares.abs().compareTo(fillQty.abs()) == -1) {
			return shares.abs();
		} else {
			return fillQty.abs();
		}
	}

	public void applyFill(final Fill fill) {
		final BigDecimal fillQty = new BigDecimal(fill.getQuantity()); //positive for buy/negative for sells
		final BigDecimal fillSign = new BigDecimal(fillQty.signum());

		final BigDecimal amountClosed = getAmountClosed(fillQty); //always positive
		final BigDecimal amountOpened = fillQty.abs().subtract(amountClosed); //always positive

		// update profit
		final BigDecimal rateDifferential = fill.getPrice().subtract(getAveragePrice());
		final BigDecimal additionalProfitOrLoss = amountClosed.multiply(rateDifferential);

		closedPL = closedPL.add(additionalProfitOrLoss.multiply(new BigDecimal(shares.signum())));

		// update cost
		if (amountOpened.compareTo(BigDecimal.ZERO) > 0 && amountClosed.compareTo(BigDecimal.ZERO) > 0 || shares.compareTo(BigDecimal.ZERO) == 0) {
			totalCost = fill.getPrice().multiply(amountOpened).multiply(fillSign);
		} else if (amountOpened.compareTo(BigDecimal.ZERO) > 0) {
			totalCost = totalCost.add(fill.getPrice().multiply(amountOpened).multiply(fillSign));
		} else if (amountClosed.compareTo(BigDecimal.ZERO) > 0) {
			final BigDecimal avgPrice = getAveragePrice();
			final BigDecimal costOfSoldShares = avgPrice.multiply(amountClosed).multiply(fillSign).negate();

			totalCost = totalCost.subtract(costOfSoldShares);
		}

		shares = shares.add(fillQty);
	}

	public boolean isFlat() {
		return shares.compareTo(BigDecimal.ZERO) == 0;
	}

	@Override
	public int hashCode() {
		return 3 * closedPL.hashCode() + 31 * shares.hashCode() + 11 * totalCost.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof Position)) {
			return false;
		}
		final Position other = (Position) obj;

		// Using compareTo() instead of equals() since we want bigDecimals of difference scale to be equal (ie '2.0' == '2.00')
		// for the purposes of this method
		return symbol.equals(other.symbol) && closedPL.compareTo(other.closedPL) == 0 && shares.compareTo(other.shares) == 0
				&& totalCost.compareTo(other.totalCost) == 0;

	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();

		sb.append("\n**Position for: " + symbol + "\n");
		sb.append("**Total cost: " + totalCost + "\n");
		sb.append("**Average Price: " + getAveragePrice() + "\n");
		sb.append("**Total shares: " + shares + "\n");
		sb.append("**Closed PL: " + closedPL + "\n");
		sb.append("**End Position**\n");

		return sb.toString();
	}
}
