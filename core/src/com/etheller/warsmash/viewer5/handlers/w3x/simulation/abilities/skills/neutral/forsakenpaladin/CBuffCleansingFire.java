package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.forsakenpaladin;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttack;

public class CBuffCleansingFire extends CBuffTimed {
	private final float damageBonus;
	private int applied;

	public CBuffCleansingFire(final int handleId, final War3ID alias, final float duration, final float damageBonus) {
		super(handleId, alias, alias, duration);
		this.damageBonus = damageBonus;
	}

	@Override
	protected void onBuffAdd(final CSimulation game, final CUnit unit) {
		for (final CUnitAttack attack : unit.getUnitSpecificAttacks()) {
			this.applied = Math.round(attack.getDamageBase() * this.damageBonus);
			attack.setTemporaryDamageBonus(attack.getTemporaryDamageBonus() + this.applied);
		}
	}

	@Override
	protected void onBuffRemove(final CSimulation game, final CUnit unit) {
		for (final CUnitAttack attack : unit.getUnitSpecificAttacks()) {
			attack.setTemporaryDamageBonus(attack.getTemporaryDamageBonus() - this.applied);
		}
	}

	@Override
	public boolean isTimedLifeBar() {
		return false;
	}

	@Override
	public boolean isPositive() {
		return true;
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
