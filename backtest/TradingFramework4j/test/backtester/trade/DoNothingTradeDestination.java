package backtester.trade;

import backtester.trade.ClOrdId;
import backtester.trade.OrderSpecification;
import backtester.trade.TradeDestination;
import backtester.trade.TradeListener;

public class DoNothingTradeDestination implements TradeDestination {

	@Override
	public void setTradeListener(final TradeListener tradeListener) {
		
	}

	@Override
	public void cancelOrder(final ClOrdId clOrdId, final ClOrdId origClOrdId) {
		
	}

	@Override
	public void cancelReplaceOrder(final ClOrdId clOrdId, final ClOrdId origClOrdId, final OrderSpecification newOrder) {
		
	}

	@Override
	public void placeOrder(final ClOrdId clOrdId, final OrderSpecification newOrder) {
		
	}

	@Override
	public void start() {
		
	}

	@Override
	public void stop() {
		
	}

	@Override
	public void cancelAll(final ClOrdId clOrdId) {
		
	}

}
