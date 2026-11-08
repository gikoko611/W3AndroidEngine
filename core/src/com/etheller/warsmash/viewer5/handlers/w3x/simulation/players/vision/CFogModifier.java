package com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision;

import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;

public abstract class CFogModifier {
	public static final float FOG_UPDATE_TIME = 1f;

	/** A vision radius squared, in fog grid cells. */
	protected static float gridRadiusSq(final float radius) {
		return (radius * radius) / (CPlayerFogOfWar.GRID_STEP * CPlayerFogOfWar.GRID_STEP);
	}

	public void setEnabled(final boolean enabled) {
	}

	/**
	 * Measured on 1.29: a modifier sets its state once when it is created, started or not.
	 */
	public void applyNow(final CSimulation game, final CPlayer player) {
	}

	public void onAdd(final CSimulation game, final CPlayer player) {
	}

	public void onRemove(final CSimulation game, final CPlayer player) {
	}

	public abstract void update(final CSimulation game, final CPlayer player, final PathingGrid pathingGrid,
			final CPlayerFogOfWar fogOfWar);
}
