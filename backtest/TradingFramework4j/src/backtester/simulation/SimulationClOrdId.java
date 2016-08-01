package backtester.simulation;

import backtester.trade.ClOrdId;

public class SimulationClOrdId extends ClOrdId implements Comparable<SimulationClOrdId> {

	private final Long numericId;

	public SimulationClOrdId(final Long numericId) {
		this.numericId = numericId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((numericId == null) ? 0 : numericId.hashCode());
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
		final SimulationClOrdId other = (SimulationClOrdId) obj;
		if (numericId == null) {
			if (other.numericId != null) {
				return false;
			}
		} else if (!numericId.equals(other.numericId)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "clOrd-" + numericId;
	}

	@Override
	public int compareTo(final SimulationClOrdId o) {
		return numericId.compareTo(o.numericId); // Lower numericId first
	}

}
