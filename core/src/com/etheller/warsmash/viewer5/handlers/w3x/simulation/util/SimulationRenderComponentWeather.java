package com.etheller.warsmash.viewer5.handlers.w3x.simulation.util;

public interface SimulationRenderComponentWeather {
	void setEnabled(boolean enabled);

	void remove();

	boolean isRemoved();

	SimulationRenderComponentWeather DO_NOTHING = new SimulationRenderComponentWeather() {
		@Override
		public void setEnabled(final boolean enabled) {
		}

		@Override
		public void remove() {
		}

		@Override
		public boolean isRemoved() {
			return true;
		}
	};
}
