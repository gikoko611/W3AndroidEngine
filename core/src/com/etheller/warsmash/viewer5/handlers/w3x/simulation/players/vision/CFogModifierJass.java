package com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;

public interface CFogModifierJass {
	void setEnabled(boolean flag);

	void destroy(CSimulation game);

	void applyNow(CSimulation game);

	/**
	 * Measured on 1.29: stopping a modifier fogs everything its player sees until the next fog update.
	 */
	void fogVisibleCells(CSimulation game);
}
