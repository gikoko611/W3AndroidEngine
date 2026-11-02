package com.etheller.warsmash.viewer5.handlers.w3x.save;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponent;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentLightning;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentModel;

/**
 * Stand-ins for what the simulation holds of the drawing side: an ability that ends later calls remove on them.
 */
final class SavedRenderStubs {
	static final Component COMPONENT = new Component();
	static final Model MODEL = new Model();
	static final Lightning LIGHTNING = new Lightning();

	private SavedRenderStubs() {
	}

	static final class Component implements SimulationRenderComponent {
		@Override
		public void remove() {
		}
	}

	static final class Model implements SimulationRenderComponentModel {
		@Override
		public void remove() {
		}

		@Override
		public void setHeight(final float height) {
		}
	}

	static final class Lightning implements SimulationRenderComponentLightning {
		@Override
		public void remove() {
		}

		@Override
		public void setColor(final float r, final float g, final float b, final float a) {
		}

		@Override
		public boolean isRemoved() {
			return true;
		}
	}
}
