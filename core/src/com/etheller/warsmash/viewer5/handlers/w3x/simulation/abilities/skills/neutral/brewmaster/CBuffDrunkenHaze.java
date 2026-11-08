package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.brewmaster;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackEffectListenerStacking;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackPreDamageListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackPreDamageListenerPriority;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;

/**
 * Drunken Haze on a unit: a chance to miss with attacks, slower movement and attacks. Breath of Fire sets units with
 * this buff on fire.
 */
public class CBuffDrunkenHaze extends CBuffTimed {
	private static final String STACKING_KEY = "drunkenHaze";

	private final float missChance;
	private final NonStackingStatBuff moveSpeedDebuff;
	private final NonStackingStatBuff attackSpeedDebuff;
	private final CUnitAttackPreDamageListener missListener;

	public CBuffDrunkenHaze(final int handleId, final War3ID alias, final float duration, final float missChance,
			final float moveSpeedReduction, final float attackSpeedReduction) {
		super(handleId, alias, alias, duration);
		this.missChance = missChance;
		this.moveSpeedDebuff = new NonStackingStatBuff(NonStackingStatBuffType.MVSPDPCT, STACKING_KEY,
				-moveSpeedReduction);
		this.attackSpeedDebuff = new NonStackingStatBuff(NonStackingStatBuffType.ATKSPD, STACKING_KEY,
				-attackSpeedReduction);
		this.missListener = (simulation, attacker, target, impactLocation, attack, settings, damageResult) -> {
			if (simulation.getSeededRandom().nextFloat() < CBuffDrunkenHaze.this.missChance) {
				damageResult.setMiss(true);
				return new CUnitAttackEffectListenerStacking(false, true);
			}
			return new CUnitAttackEffectListenerStacking();
		};
	}

	@Override
	protected void onBuffAdd(final CSimulation game, final CUnit unit) {
		unit.addNonStackingStatBuff(game, this.moveSpeedDebuff);
		unit.addNonStackingStatBuff(game, this.attackSpeedDebuff);
		unit.addPreDamageListener(CUnitAttackPreDamageListenerPriority.ACCURACY, this.missListener);
	}

	@Override
	protected void onBuffRemove(final CSimulation game, final CUnit unit) {
		unit.removeNonStackingStatBuff(game, this.moveSpeedDebuff);
		unit.removeNonStackingStatBuff(game, this.attackSpeedDebuff);
		unit.removePreDamageListener(CUnitAttackPreDamageListenerPriority.ACCURACY, this.missListener);
	}

	public static boolean isHazed(final CUnit unit) {
		for (final CAbility ability : unit.getAbilities()) {
			if (ability instanceof CBuffDrunkenHaze) {
				return true;
			}
		}
		return false;
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
