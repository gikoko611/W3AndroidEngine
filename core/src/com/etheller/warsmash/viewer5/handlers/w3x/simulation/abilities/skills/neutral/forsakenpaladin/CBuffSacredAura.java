package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.forsakenpaladin;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffAuraBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackDamageTakenModificationListener;

public class CBuffSacredAura extends CBuffAuraBase {
	private final float healingFactor;
	private final CUnitAttackDamageTakenModificationListener resistance;

	public CBuffSacredAura(final int handleId, final War3ID alias, final float magicResistance,
			final float healingFactor) {
		super(handleId, alias, alias);
		this.healingFactor = healingFactor;
		this.resistance = (simulation, attacker, target, flags, attackType, damageType, result) -> {
			if (attackType == CAttackType.SPELLS) {
				result.setDamageMultiplier(result.getDamageMultiplier() * (1f - magicResistance));
			}
			return result;
		};
	}

	@Override
	public void onBuffAdd(final CSimulation game, final CUnit unit) {
		unit.multiplyHealingReceived(this.healingFactor);
		unit.addDamageTakenModificationListener(this.resistance);
	}

	@Override
	public void onBuffRemove(final CSimulation game, final CUnit unit) {
		unit.multiplyHealingReceived(1f / this.healingFactor);
		unit.removeDamageTakenModificationListener(this.resistance);
	}
}
