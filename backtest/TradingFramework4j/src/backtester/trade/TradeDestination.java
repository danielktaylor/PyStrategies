package backtester.trade;

public interface TradeDestination {

	/**
	 * Send an order to the destination
	 *
	 * @param clOrdid
	 * @param newOrder
	 * @return an OrderId. A unique identifier for the order
	 * @throws TradingException
	 *             if the Order could not be placed
	 */
	void placeOrder(ClOrdId clOrdId, OrderSpecification newOrder);

	/**
	 * Cancels the order with the specified original client order id
	 *
	 * @param clOrdId
	 * @param origClOrdId
	 * @throws TradingException
	 */
	void cancelOrder(ClOrdId clOrdId, ClOrdId origClOrdId);

	/**
	 * Cancel/replaces an order with oldOrderId. The cancel/replaceable attributes in the old order will be replaced by those of the new order
	 *
	 * @param clOrdId
	 * @param origClOrdId
	 * @param newOrder
	 * @return
	 * @throws TradingException
	 */
	void cancelReplaceOrder(ClOrdId clOrdId, ClOrdId origClOrdId, OrderSpecification newOrder);

	void setTradeListener(TradeListener tradeListener);

	void start();

	void stop();

	/**
	 * Cancels all open orders
	 */
	void cancelAll(ClOrdId clOrdId);
}
