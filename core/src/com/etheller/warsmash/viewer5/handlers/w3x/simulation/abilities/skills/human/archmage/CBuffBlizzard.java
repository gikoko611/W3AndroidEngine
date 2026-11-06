package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.archmage;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;

/**
 * Put on a unit a Blizzard wave hits; while it has it, no Blizzard wave hurts it. Its time stands still on a paused
 * unit, so a paused unit takes only the first hit.
 */
public class CBuffBlizzard extends CBuffTimed {

	public CBuffBlizzard(final int handleId, final War3ID alias, final float duration) {
		super(handleId, alias, alias, duration);
	}

	@Override
	protected void onBuffAdd(final CSimulation game, final CUnit unit) {
	}

	@Override
	protected void onBuffRemove(final CSimulation game, final CUnit unit) {
	}

	@Override
	public boolean isTimedLifeBar() {
		return false;
	}

	@Override
	public boolean isPositive() {
		return false;
	}

	@Override
	public boolean isDispellable() {
		return true;
	}

	@Override
	public boolean isMagic() {
		return true;
	}
}
