package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.shadowhunter;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff.CPausedTickingBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;

/**
 * Big Bad Voodoo's buffs: the protected units' one makes them invulnerable, the caster's one only marks the channel.
 * Both last until the ability takes them off. Measured on 1.29: a protected unit keeps the buff, and its
 * invulnerability, for about 0.8 s after it leaves the area or the channel ends, paused or not; the caster's goes
 * at once.
 */
public class CBuffBigBadVoodoo extends CBuffTimed implements CPausedTickingBuff {
	private static final int LINGER_TICKS = (int) (0.8f / WarsmashConstants.SIMULATION_STEP_TIME);
	private final StateModBuff invulnerableBuff;
	private int lingerEndTick;

	public CBuffBigBadVoodoo(final int handleId, final War3ID alias, final boolean protecting) {
		super(handleId, alias, alias, 0);
		this.invulnerableBuff = protecting ? new StateModBuff(StateModBuffType.INVULNERABLE, 1) : null;
	}

	@Override
	protected void onBuffAdd(final CSimulation game, final CUnit unit) {
		if (this.invulnerableBuff != null) {
			unit.addStateModBuff(this.invulnerableBuff);
			unit.computeUnitState(game, StateModBuffType.INVULNERABLE);
		}
	}

	@Override
	protected void onBuffRemove(final CSimulation game, final CUnit unit) {
		if (this.invulnerableBuff != null) {
			unit.removeStateModBuff(this.invulnerableBuff);
			unit.computeUnitState(game, StateModBuffType.INVULNERABLE);
		}
	}

	public void startLinger(final CSimulation game) {
		if (this.lingerEndTick == 0) {
			this.lingerEndTick = game.getGameTurnTick() + LINGER_TICKS;
		}
	}

	public void stopLinger() {
		this.lingerEndTick = 0;
	}

	@Override
	public void onTick(final CSimulation game, final CUnit unit) {
		if ((this.lingerEndTick != 0) && (game.getGameTurnTick() >= this.lingerEndTick)) {
			unit.remove(game, this);
		}
	}

	@Override
	public void onDeath(final CSimulation game, final CUnit cUnit) {
	}

	@Override
	public boolean isTimedLifeBar() {
		return false;
	}

	@Override
	public boolean isDispellable() {
		return false;
	}
}
