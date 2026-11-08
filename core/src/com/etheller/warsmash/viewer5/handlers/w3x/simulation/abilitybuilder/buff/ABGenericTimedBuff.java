package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff;

import java.util.Map;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CExactExpiry;

public abstract class ABGenericTimedBuff extends ABBuff {
	private final float duration;
	private final CExactExpiry expiry = new CExactExpiry();
	private int lastTick;

	public ABGenericTimedBuff(int handleId, War3ID alias, Map<String, Object> localStore, CAbility sourceAbility,
			CUnit sourceUnit, float duration, boolean showTimedLifeBar, boolean leveled, boolean positive,
			boolean dispellable) {
		super(handleId, alias, alias, localStore, sourceAbility, sourceUnit);
		this.setTimedLifeBar(showTimedLifeBar);
		this.duration = duration;
		this.setLeveled(leveled);
		this.setPositive(positive);
		this.setDispellable(dispellable);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ABGenericTimedBuff other = (ABGenericTimedBuff) obj;
		return this.getAlias() == other.getAlias() && this.getLevel() == other.getLevel() && !this.isStacks() && !other.isStacks();
	}

	@Override
	public void onAdd(CSimulation game, CUnit unit) {
		this.onBuffAdd(game, unit);
		startExpiry(game);
	}

	private void startExpiry(final CSimulation game) {
		this.lastTick = game.getGameTurnTick();
		if (this.duration == 0) {
			this.expiry.never(game);
		} else {
			this.expiry.start(game, this.duration);
		}
	}

	protected abstract void onBuffAdd(CSimulation game, CUnit unit);

	@Override
	public void onRemove(CSimulation game, CUnit unit) {
		this.expiry.cancel(game);
		this.onBuffRemove(game, unit);
		this.cleanUpUniqueValues();
	}

	protected abstract void onBuffRemove(CSimulation game, CUnit unit);

	protected abstract void onBuffExpire(CSimulation game, CUnit unit);

	@Override
	public float getDurationMax() {
		return this.duration;
	}

	@Override
	public float getDurationRemaining(final CSimulation game, final CUnit unit) {
		return (float) this.expiry.getRemaining(game);
	}

	@Override
	public void onTick(final CSimulation game, final CUnit caster) {
		// Steps its unit spent paused, without ticking its buffs, do not count.
		final int tick = game.getGameTurnTick();
		final int skippedSteps = tick - this.lastTick - 1;
		this.lastTick = tick;
		if (skippedSteps > 0) {
			this.expiry.postpone(game, skippedSteps * (double) WarsmashConstants.SIMULATION_STEP_TIME);
		}
		if (this.expiry.check(game, simulation -> expire(simulation, caster))) {
			expire(game, caster);
		}
	}

	private void expire(final CSimulation game, final CUnit caster) {
		this.onBuffExpire(game, caster);
		caster.remove(game, this);
	}

	@Override
	public void onDeath(final CSimulation game, final CUnit cUnit) {
		cUnit.remove(game, this);
	}

	public void updateExpiration(final CSimulation game, final CUnit unit) {
		startExpiry(game);
	}
}
