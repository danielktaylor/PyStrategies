package backtester.strategy;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.jpy.PyLib;
import org.jpy.PyModule;
import org.jpy.PyObject;

import backtester.quote.Ask;
import backtester.quote.Bid;
import backtester.quote.TradeTick;
import backtester.simulation.SimulationClOrdId;
import backtester.trade.ClOrdId;
import backtester.trade.Fill;
import backtester.trade.OrderSpecification;
import backtester.trade.TradeType;

public class PythonStrategy extends BaseStrategy {
	
	PyObject strategy = null;
	
	public PythonStrategy(final PythonStrategy pythonStrategy, final StrategyConfiguration strategyConfiguration) {
		super(pythonStrategy, strategyConfiguration);
		
		PyLib.startPython();
		PyModule.extendSysPath(System.getProperty("user.dir"), true);
		
		String pyFile = strategyConfiguration.getVariable("pythonStrategy");
		PyModule pluginModule;
		if (pyFile != null) {
			File f = new File(pyFile);
			pluginModule = PyModule.importModule(f.getName().replace(".py", ""));
		} else {
			pluginModule = PyModule.importModule("pyStrategy");
		}
		
		strategy = pluginModule.call("StrategyPlugin");
		
		String pythonConfig = strategyConfiguration.getVariable("pythonConfig");
		if (pythonConfig != null) {
			strategy.call("_config", pythonConfig);
		}
	}

	public void handleActions(String[] actions) {
		int numOrders = actions.length / 6;
		for (int i = 0; i < numOrders; i++) {
			String action = actions[6*i + 0];
			String clOrdId = actions[6*i + 1];
			String origClOrdId = actions[6*i + 2];
			String price = actions[6*i + 3];
			String qty = actions[6*i + 4];
			String type = actions[6*i + 5];
			
			ClOrdId id, origId;
			OrderSpecification order;
			String symbol = getSymbol();
			
			TradeType t;
			switch (type) {
			case "buy":
				t = TradeType.BUY;
				break;
			case "sell":
				t = TradeType.SELL;
				break;
			case "cover":
				t = TradeType.COVER;
				break;
			case "short":
				t = TradeType.SHORT;
				break;
			default:
				t = null;
			}
			
			switch (action) {
			case "NO": // New Order
				order = new OrderSpecification(symbol, new BigDecimal(price), Integer.parseInt(qty), t);
				id = new SimulationClOrdId(Long.parseLong(clOrdId));
				placeOrder(id, order);
				break;
			case "CO": // Cancel Order
				id = new SimulationClOrdId(Long.parseLong(clOrdId));
				origId = new SimulationClOrdId(Long.parseLong(origClOrdId));
				cancelOrder(id, origId);
				break;
			case "CR": // Cancel Replace
				order = new OrderSpecification(symbol, new BigDecimal(price), Integer.parseInt(qty), t);
				id = new SimulationClOrdId(Long.parseLong(clOrdId));
				origId = new SimulationClOrdId(Long.parseLong(origClOrdId));
				cancelReplaceOrder(id, origId, order);
				break;
			case "CA": // Cancel All
				id = new SimulationClOrdId(Long.parseLong(clOrdId));
				cancelAll(id);
				break;
			default:
				throw new IllegalArgumentException("Unknown action type");
			}
		}
	}
	
	private String cid(ClOrdId id) {
		return id.toString().replace("clOrd-","");
	}
	
	@Override
	void doOnBid(final Bid bid) {
		String id = bid.getId().toString();
		long qty = bid.getRemainingQuantity();
		String price = bid.getPrice().setScale(4, RoundingMode.UNNECESSARY).toPlainString();
		PyObject ret = strategy.call("_bid", bid.getSymbol(), id, qty, price, bid.getMilliTimestamp());
		String[] actions = ret.getObjectArrayValue(String.class);
		handleActions(actions);
	}
	
	@Override
	void doOnAsk(final Ask ask) {
		String id = ask.getId().toString();
		long qty = ask.getRemainingQuantity();
		String price = ask.getPrice().setScale(4, RoundingMode.UNNECESSARY).toPlainString();
		PyObject ret = strategy.call("_ask", ask.getSymbol(), id, qty, price, ask.getMilliTimestamp());
		String[] actions = ret.getObjectArrayValue(String.class);
		handleActions(actions);
	}
	
	@Override
	void doOnTradeTick(final TradeTick tradeTick) {
		long qty = tradeTick.getSize();
		String price = tradeTick.getPrice().setScale(4, RoundingMode.DOWN).toPlainString();
		PyObject ret = strategy.call("_trade_tick", tradeTick.getSymbol(), qty, price, tradeTick.getMilliTimestamp());
		String[] actions = ret.getObjectArrayValue(String.class);
		handleActions(actions);
	};
	
	@Override	
	public void doOnFill(final Fill fill) {
		long qty = fill.getQuantity();
		long remainingQty = fill.getRemaining();
		String price = fill.getPrice().setScale(4, RoundingMode.UNNECESSARY).toPlainString();
		PyObject ret = strategy.call("_fill", fill.getSymbol(), cid(fill.getClOrdId()), qty, remainingQty, price, fill.getMilliTimestamp());
		String[] actions = ret.getObjectArrayValue(String.class);
		handleActions(actions);
	}
	
	@Override
	void doOnCancelAccepted(final ClOrdId clOrdId, final ClOrdId origClOrdId) {
		PyObject ret = strategy.call("_cancel_accepted", cid(clOrdId), cid(origClOrdId));
		String[] actions = ret.getObjectArrayValue(String.class);
		handleActions(actions);
	};
	
	@Override
	void doOnCancelRejected(final ClOrdId clOrdId) {
		PyObject ret = strategy.call("_cancel_rejected", cid(clOrdId));
		String[] actions = ret.getObjectArrayValue(String.class);
		handleActions(actions);
	};
	
	@Override
	void doOnCancelReplaceAccepted(final ClOrdId clOrdId, final OrderSpecification order, final ClOrdId origClOrdId) {
		PyObject ret = strategy.call("_cancel_replace_accepted", cid(clOrdId), cid(origClOrdId));
		String[] actions = ret.getObjectArrayValue(String.class);
		handleActions(actions);
	}
	
	@Override
	void doOnCancelReplaceRejected(final ClOrdId clOrdId) {
		PyObject ret = strategy.call("_cancel_replace_rejected", cid(clOrdId));
		String[] actions = ret.getObjectArrayValue(String.class);
		handleActions(actions);
	};
	
	@Override
	void doOnNewOrderRejected(final ClOrdId clOrdId) {
		PyObject ret = strategy.call("_new_order_rejected", cid(clOrdId));
		String[] actions = ret.getObjectArrayValue(String.class);
		handleActions(actions);
	};

	@Override
	void doOnNewOrderAccepted(final ClOrdId clOrdId, final OrderSpecification order) {
		PyObject ret = strategy.call("_new_order_accepted", cid(clOrdId));
		String[] actions = ret.getObjectArrayValue(String.class);
		handleActions(actions);
	}
	
	@Override
	void doOnPlaybackEnd() {
		strategy.call("_playback_end");
	}

	@Override
	public void reset() {
		super.reset();
		strategy.call("_reset");
	}
}
