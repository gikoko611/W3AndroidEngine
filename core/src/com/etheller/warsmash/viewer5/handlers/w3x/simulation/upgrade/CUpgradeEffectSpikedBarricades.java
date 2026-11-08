package com.etheller.warsmash.viewer5.handlers.w3x.simulation.upgrade;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;

/**
 * Damage returned to melee attackers (rspi). The upgrade only sets the amount: the damage is dealt by the unit's
 * Spiked Barricades ability, so a unit without that ability returns nothing.
 */
public class CUpgradeEffectSpikedBarricades implements CUpgradeEffect {
	private final float base;
	private final float mod;

	public CUpgradeEffectSpikedBarricades(final float base, final float mod) {
		this.base = base;
		this.mod = mod;
	}

	public float getDamage(final int level) {
		return Util.levelValue(this.base, this.mod, level - 1);
	}

	@Override
	public void apply(final CSimulation simulation, final CUnit unit, final int level) {
	}

	@Override
	public void unapply(final CSimulation simulation, final CUnit unit, final int level) {
	}
}
