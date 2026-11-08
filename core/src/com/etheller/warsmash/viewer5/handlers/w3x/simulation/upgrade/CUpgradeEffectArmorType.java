package com.etheller.warsmash.viewer5.handlers.w3x.simulation.upgrade;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CDefenseType;

/**
 * Moves the unit's armor type by the effect value along the damage table order (small, medium, large, fort,
 * normal, hero, divine, none), as the game does: Reinforced Defenses (1) turns large into fort, and several such
 * upgrades on one unit add up.
 */
public class CUpgradeEffectArmorType implements CUpgradeEffect {
	private final int shift;

	public CUpgradeEffectArmorType(final int shift) {
		this.shift = shift;
	}

	@Override
	public void apply(final CSimulation simulation, final CUnit unit, final int level) {
		change(unit, this.shift);
	}

	@Override
	public void unapply(final CSimulation simulation, final CUnit unit, final int level) {
		change(unit, -this.shift);
	}

	private static void change(final CUnit unit, final int shift) {
		if (shift == 0) {
			return;
		}
		// past the end the game reads outside its damage table; unarmored is the closest stable result
		final int index = Math.max(0,
				Math.min(CDefenseType.VALUES.length - 1, unit.getDefenseType().ordinal() + shift));
		unit.setDefenseType(CDefenseType.VALUES[index]);
		unit.notifyAttacksChanged();
	}
}
