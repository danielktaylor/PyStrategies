package backtester.trade;


public interface TradeListener {

	void onFill(Fill fill);
	
	void onTransactionCost(TransactionCost cost);

	void onCancelReplaceAccepted(ClOrdId clOrdId, OrderSpecification order, ClOrdId origClOrdId);

	void onCancelReplaceRejected(ClOrdId clOrdId);

	void onCancelAccepted(ClOrdId clOrdId, ClOrdId origClOrdId);

	void onCancelRejected(ClOrdId clOrdId);

	void onNewOrderAccepted(ClOrdId clOrdId, OrderSpecification order);

	void onNewOrderRejected(ClOrdId clOrdId);
}
