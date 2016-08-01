package backtester.trade;

import java.math.BigDecimal;

public class TransactionCost {
	private final BigDecimal cost;

	public TransactionCost(final BigDecimal cost) {
		this.cost = cost;
	}

	/**
	 * Cost of the transaction. Negative cost indicates a rebate
	 * 
	 * @return
	 */
	public BigDecimal getCost() {
		return cost;
	}

}
