package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.AbstractCBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CExactExpiry;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponent;

public abstract class CBuffTimed extends AbstractCBuff {
	private SimulationRenderComponent fx;
	private final float duration;
	private final CExactExpiry expiry = new CExactExpiry();
	private double carriedRemaining = -1;
	private boolean timerPaused;

	public CBuffTimed(final int handleId, final War3ID code, final War3ID alias, final float duration) {
		super(handleId, code, alias);
		this.duration = duration;
	}

	protected abstract void onBuffAdd(final CSimulation game, final CUnit unit);

	protected abstract void onBuffRemove(final CSimulation game, final CUnit unit);

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		onBuffAdd(game, unit);
		this.fx = game.createPersistentSpellEffectOnUnit(unit, getAlias(), CEffectType.TARGET);
		if (this.carriedRemaining >= 0) {
			this.expiry.startRemaining(game, this.carriedRemaining);
		}
		else {
			this.expiry.start(game, this.duration);
		}
		this.carriedRemaining = -1;
	}

	/**
	 * Keeps the time left when this buff is next added, for a buff moved to another unit (Spell Steal).
	 */
	public void carryRemainingTime(final CSimulation game) {
		this.carriedRemaining = this.expiry.getRemaining(game);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		this.expiry.cancel(game);
		onBuffRemove(game, unit);
		this.fx.remove();
	}

	public void setTimerPaused(final boolean timerPaused) {
		this.timerPaused = timerPaused;
	}

	@Override
	public void onTick(final CSimulation game, final CUnit caster) {
		if (this.timerPaused) {
			this.expiry.postpone(game, WarsmashConstants.SIMULATION_STEP_TIME);
			return;
		}
		if (this.expiry.check(game, simulation -> caster.remove(simulation, this))) {
			caster.remove(game, this);
		}
	}

	@Override
	public void onDeath(final CSimulation game, final CUnit cUnit) {
		cUnit.remove(game, this);
	}

	@Override
	public void onCancelFromQueue(final CSimulation game, final CUnit unit, final int playerIndex, final int orderId) {
	}

	@Override
	public CBehavior begin(final CSimulation game, final CUnit caster, final int playerIndex, final int orderId,
			final boolean autoOrder, final CWidget target) {
		return null;
	}

	@Override
	public CBehavior begin(final CSimulation game, final CUnit caster, final int playerIndex, final int orderId,
			final boolean autoOrder, final AbilityPointTarget point) {
		return null;
	}

	@Override
	public CBehavior beginNoTarget(final CSimulation game, final CUnit caster, final int playerIndex, final int orderId,
			final boolean autoOrder) {
		return null;
	}

	@Override
	public void checkCanTarget(final CSimulation game, final CUnit unit, final int playerIndex, final int orderId,
			final boolean autoOrder, final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	public void checkCanTarget(final CSimulation game, final CUnit unit, final int playerIndex, final int orderId,
			final boolean autoOrder, final AbilityPointTarget target,
			final AbilityTargetCheckReceiver<AbilityPointTarget> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	public void checkCanTargetNoTarget(final CSimulation game, final CUnit unit, final int playerIndex,
			final int orderId, final boolean autoOrder, final AbilityTargetCheckReceiver<Void> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	protected void innerCheckCanUse(final CSimulation game, final CUnit unit, final int playerIndex, final int orderId,
			final AbilityActivationReceiver receiver) {
		receiver.notAnActiveAbility();
	}

	@Override
	public float getDurationMax() {
		return this.duration;
	}

	@Override
	public float getDurationRemaining(final CSimulation game, final CUnit unit) {
		return (float) this.expiry.getRemaining(game);
	}
}
