package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.malganis;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;

/**
 * Dark Conversion on its target: stunned until the time runs out, then turned into the new type. Removed early
 * (death, dispel) the villager stays as it is.
 */
public class CBuffDarkConversion extends CBuffTimed {
	private final StateModBuff stunBuff = new StateModBuff(StateModBuffType.STUN, 1);
	private final float duration;
	private final CUnit caster;
	private final War3ID convertedUnitId;
	private int convertTick;

	public CBuffDarkConversion(final int handleId, final War3ID alias, final float duration, final CUnit caster,
			final War3ID convertedUnitId) {
		super(handleId, alias, alias, duration);
		this.duration = duration;
		this.caster = caster;
		this.convertedUnitId = convertedUnitId;
	}

	@Override
	protected void onBuffAdd(final CSimulation game, final CUnit unit) {
		this.convertTick = game.getGameTurnTick() + (int) (this.duration / WarsmashConstants.SIMULATION_STEP_TIME);
		unit.addStateModBuff(this.stunBuff);
		unit.computeUnitState(game, StateModBuffType.STUN);
	}

	@Override
	protected void onBuffRemove(final CSimulation game, final CUnit unit) {
		final CUnitType newType = game.getUnitData().getUnitType(this.convertedUnitId);
		if (!unit.isDead() && (game.getGameTurnTick() >= this.convertTick) && (newType != null)) {
			// Measured on 1.29: the same unit (handle, owner, facing) becomes the new type at the same life share;
			// the summon event sees it without the buff but still stunned.
			unit.setTypeId(game, this.convertedUnitId, true);
			final CPlayer player = game.getPlayer(unit.getPlayerIndex());
			player.setUnitFoodUsed(unit, newType.getFoodUsed());
			player.setUnitFoodMade(unit, newType.getFoodMade());
			game.createTemporarySpellEffectOnUnit(unit, getAlias(), CEffectType.EFFECT);
			game.unitSummoned(this.caster, unit);
		}
		unit.removeStateModBuff(this.stunBuff);
		unit.computeUnitState(game, StateModBuffType.STUN);
	}

	@Override
	public boolean isTimedLifeBar() {
		return false;
	}

	@Override
	public boolean isPositive() {
		return false;
	}
}
