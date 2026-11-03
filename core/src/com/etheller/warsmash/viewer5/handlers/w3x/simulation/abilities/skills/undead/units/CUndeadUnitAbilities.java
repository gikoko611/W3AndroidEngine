package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.units;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.CBuff;

public final class CUndeadUnitAbilities {
	private CUndeadUnitAbilities() {
	}

	public static <T extends CBuff> T findBuff(final CUnit unit, final Class<T> buffClass, final War3ID alias) {
		for (final CAbility ability : unit.getAbilities()) {
			if (buffClass.isInstance(ability) && alias.equals(ability.getAlias())) {
				return buffClass.cast(ability);
			}
		}
		return null;
	}

	public static boolean hasBuff(final CUnit unit, final War3ID alias) {
		return findBuff(unit, CBuff.class, alias) != null;
	}
}
