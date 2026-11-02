package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.forsakenpaladin;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;

/** The holy ground of Consecration: refreshed every second while a unit stands on it. */
public class CBuffHealingModifier extends CBuffTimed {
	private final float healingFactor;
	private final boolean positive;

	public CBuffHealingModifier(final int handleId, final War3ID alias, final float duration, final float healingFactor,
			final boolean positive) {
		super(handleId, alias, alias, duration);
		this.healingFactor = healingFactor;
		this.positive = positive;
	}

	@Override
	protected void onBuffAdd(final CSimulation game, final CUnit unit) {
		unit.multiplyHealingReceived(this.healingFactor);
	}

	@Override
	protected void onBuffRemove(final CSimulation game, final CUnit unit) {
		unit.multiplyHealingReceived(1f / this.healingFactor);
	}

	@Override
	public boolean isTimedLifeBar() {
		return false;
	}

	@Override
	public boolean isPositive() {
		return this.positive;
	}

	@Override
	public boolean isDispellable() {
		return false;
	}

	@Override
	public boolean isMagic() {
		return true;
	}
}
