package backtester.update;

import backtester.trade.ClOrdId;
import backtester.trade.Fill;
import backtester.trade.OrderSpecification;
import backtester.trade.OrderStatus;

public class OrderUpdate {
	private final ClOrdId updatedClOrdId;
	private final ClOrdId originalClOrdId;
	private final OrderSpecification updatedOrderSpecification;
	private final ORDER_UPDATE_TYPE orderChangedUpdateType;

	public static enum ORDER_UPDATE_TYPE {
		ALL_ORDERS_CANCELED, NEW_ORDER, NOTHING, ORDER_CANCELED, ORDER_CANCEL_REPLACED, ORDER_COMPLETED, ORDER_PARTIALLY_FILLED
	}

	public static OrderUpdate createNewOrderUpdate(final ClOrdId clOrdId, final OrderSpecification order) {
		order.setOrderStatus(OrderStatus.NEW);
		return new OrderUpdate(clOrdId, null, order, ORDER_UPDATE_TYPE.NEW_ORDER);
	}

	public static OrderUpdate createCancelOrderUpdate(final ClOrdId clOrdId) {
		return new OrderUpdate(clOrdId, null, null, ORDER_UPDATE_TYPE.ORDER_CANCELED);
	}

	public static OrderUpdate createCompletedOrderUpdate(final ClOrdId clOrdId, final Fill fill) {
		final OrderSpecification completedOrder = new OrderSpecification(null, fill.getPrice().abs(), 1, null);
		completedOrder.getFillHistory().add(fill);
		return new OrderUpdate(clOrdId, clOrdId, completedOrder, ORDER_UPDATE_TYPE.ORDER_COMPLETED);
	}

	public static OrderUpdate createCancelReplaceOrderUpdate(final ClOrdId originalOrderId, final ClOrdId newClOrdId,
			final OrderSpecification order) {
		order.setOrderStatus(OrderStatus.REPLACED);
		return new OrderUpdate(originalOrderId, newClOrdId, order, ORDER_UPDATE_TYPE.ORDER_CANCEL_REPLACED);
	}

	public static OrderUpdate createPartiallyFilledOrderUpdate(final ClOrdId originalOrderId, final Fill fill) {
		final OrderSpecification order = new OrderSpecification(null, fill.getPrice().abs(), 1, null);
		order.setAmountFilled(fill.getQuantity());
		order.getFillHistory().add(fill);
		order.setOrderStatus(OrderStatus.PARTIALLY_FILLED);
		return new OrderUpdate(originalOrderId, originalOrderId, order, ORDER_UPDATE_TYPE.ORDER_PARTIALLY_FILLED);
	}

	public static OrderUpdate createCancelAllOrdersUpdate() {
		return new OrderUpdate(null, null, null, ORDER_UPDATE_TYPE.ALL_ORDERS_CANCELED);
	}

	private OrderUpdate(final ClOrdId originalClOrdId, final ClOrdId updatedClOrdId, final OrderSpecification order,
			final ORDER_UPDATE_TYPE updateType) {
		this.originalClOrdId = originalClOrdId;
		this.updatedClOrdId = updatedClOrdId;

		updatedOrderSpecification = order;
		orderChangedUpdateType = updateType;
	}

	public ClOrdId getUpdatedClOrdId() {
		return updatedClOrdId;
	}

	public OrderSpecification getUpdatedOrderSpecification() {
		return updatedOrderSpecification;
	}

	public ORDER_UPDATE_TYPE getOrderChangedUpdateType() {
		return orderChangedUpdateType;
	}

	public ClOrdId getOriginalClOrdId() {
		return originalClOrdId;
	}
}
