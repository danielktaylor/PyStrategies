package backtester.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import backtester.simulation.SimulationOrderId;


public class SimulationOrderIdTest {

	private SimulationOrderId createASimulationOrderId() {
		return new SimulationOrderId(1L);
	}

	private SimulationOrderId createASimulationOrderId(final long orderId) {
		return new SimulationOrderId(orderId);
	}

	@Test
	public void compareIdenticalSimulationOrderIds() {
		final SimulationOrderId firstSimulationOrderId = createASimulationOrderId();
		final SimulationOrderId secondSimulationOrderId = createASimulationOrderId();
		assertEquals(0, firstSimulationOrderId.compareTo(secondSimulationOrderId));
	}

	@Test
	public void compareDifferentSimulationOrderIds() {
		final SimulationOrderId firstSimulationOrderId = createASimulationOrderId();
		final SimulationOrderId secondSimulationOrderId = createASimulationOrderId(2L);
		assertEquals(-1, firstSimulationOrderId.compareTo(secondSimulationOrderId));
	}

	@Test
	public void equateIdenticalSimulationOrderIds() {
		final SimulationOrderId firstSimulationOrderId = createASimulationOrderId();
		final SimulationOrderId secondSimulationOrderId = createASimulationOrderId();
		assertTrue(firstSimulationOrderId.equals(secondSimulationOrderId));
	}

	@Test
	public void equateDifferentSimulationOrderIds() {
		final SimulationOrderId firstSimulationOrderId = createASimulationOrderId();
		final SimulationOrderId secondSimulationOrderId = createASimulationOrderId(2L);
		assertFalse(firstSimulationOrderId.equals(secondSimulationOrderId));
	}
}
