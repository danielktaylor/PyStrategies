package backtester.simulation;

import org.junit.Test;

import backtester.simulation.DefaultSimulationMarket;


public class SimulationMarketTest {

	@Test(expected = IllegalArgumentException.class)
	public void nullQuoteListenerNotAllowed() {
		final DefaultSimulationMarket market = new DefaultSimulationMarket(null);
		market.setQuoteListener(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullTradeListenerNotAllowed() {
		final DefaultSimulationMarket market = new DefaultSimulationMarket(null);
		market.setTradeListener(null);
	}
}
