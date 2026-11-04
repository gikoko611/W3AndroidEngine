package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.bloodmage;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CDamageFlags;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackFinalDamageTakenModificationListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;

/**
 * Life and mana a drain gave its caster above the maximum. The unit's life and mana stay at the maximum while the
 * bonus is spent first: damage takes it before life and spent mana is refilled from it. It lasts as long as the
 * drain and then decays.
 */
public class CBuffDrainBonus extends CBuffTimed implements CUnitAttackFinalDamageTakenModificationListener {
	private float lifeFactor;
	private float lifeDecay;
	private float manaFactor;
	private float manaDecay;
	private float lifeBonus;
	private float manaBonus;
	private boolean channeling;

	public CBuffDrainBonus(final int handleId, final War3ID alias) {
		super(handleId, alias, alias, 0);
	}

	public void startChannel(final float lifeFactor, final float lifeDecay, final float manaFactor,
			final float manaDecay) {
		this.lifeFactor = lifeFactor;
		this.lifeDecay = lifeDecay;
		this.manaFactor = manaFactor;
		this.manaDecay = manaDecay;
		this.channeling = true;
	}

	public void endChannel(final CSimulation game, final CUnit unit) {
		this.channeling = false;
		removeIfSpent(game, unit);
	}

	public void addLife(final CUnit unit, final float life) {
		this.lifeBonus = Math.min(this.lifeBonus + life, this.lifeFactor * unit.getMaximumLife());
	}

	public void addMana(final CUnit unit, final float mana) {
		this.manaBonus = Math.min(this.manaBonus + mana, this.manaFactor * unit.getMaximumMana());
	}

	@Override
	protected void onBuffAdd(final CSimulation game, final CUnit unit) {
		unit.addFinalDamageTakenModificationListener(this);
	}

	@Override
	protected void onBuffRemove(final CSimulation game, final CUnit unit) {
		unit.removeFinalDamageTakenModificationListener(this);
	}

	@Override
	public void onTick(final CSimulation game, final CUnit unit) {
		if ((this.manaBonus > 0) && (unit.getMana() < unit.getMaximumMana())) {
			final float refill = Math.min(this.manaBonus, unit.getMaximumMana() - unit.getMana());
			unit.setMana(unit.getMana() + refill);
			this.manaBonus -= refill;
		}
		if ((this.lifeBonus > 0) && (unit.getLife() < unit.getMaximumLife())) {
			final float refill = Math.min(this.lifeBonus, unit.getMaximumLife() - unit.getLife());
			unit.setLife(game, unit.getLife() + refill);
			this.lifeBonus -= refill;
		}
		if (!this.channeling) {
			this.manaBonus = Math.max(0, this.manaBonus - (this.manaDecay * WarsmashConstants.SIMULATION_STEP_TIME));
			this.lifeBonus = Math.max(0, this.lifeBonus - (this.lifeDecay * WarsmashConstants.SIMULATION_STEP_TIME));
			removeIfSpent(game, unit);
		}
	}

	private void removeIfSpent(final CSimulation game, final CUnit unit) {
		if ((this.manaBonus <= 0) && (this.lifeBonus <= 0) && unit.getAbilities().contains(this)) {
			unit.remove(game, this);
		}
	}

	@Override
	public float onDamage(final CSimulation simulation, final CUnit attacker, final CUnit target,
			final CDamageFlags flags, final CAttackType attackType, final CDamageType damageType,
			final float previousDamage) {
		if ((this.lifeBonus <= 0) || (previousDamage <= 0)) {
			return previousDamage;
		}
		final float absorbed = Math.min(previousDamage, this.lifeBonus);
		this.lifeBonus -= absorbed;
		return previousDamage - absorbed;
	}

	@Override
	public boolean isTimedLifeBar() {
		return false;
	}
}
