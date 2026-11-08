package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.brewmaster;

import java.util.ArrayList;
import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimedLife;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;

/**
 * Storm, Earth and Fire: the Brewmaster leaves the battle and splits into the elemental spirits of Data A (one per
 * listed unit type) around it, each with the timed life buff. When the duration ends the Brewmaster comes back with
 * its life and mana; when every spirit has been killed it dies.
 */
public class CAbilityStormEarthAndFire extends CAbilityNoTargetSpellBase {
	private static final StateModBuff HIDDEN_INVULNERABLE = new StateModBuff(StateModBuffType.INVULNERABLE, 1);

	private final List<War3ID> spiritTypes = new ArrayList<>();
	private War3ID buffId;
	private float areaOfEffect;

	private final List<CUnit> spirits = new ArrayList<>();
	private int endTick;
	private float lastX;
	private float lastY;
	private boolean allDied;

	public CAbilityStormEarthAndFire(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.elementalfury;
	}

	@Override
	public void populateData(final GameObject worldEditorAbility, final int level) {
		this.spiritTypes.clear();
		for (int i = 0; i < 3; i++) {
			final String type = worldEditorAbility.getFieldAsString(AbilityFields.DATA_A + level, i);
			if ((type != null) && (type.length() == 4)) {
				this.spiritTypes.add(War3ID.fromString(type));
			}
		}
		this.buffId = AbstractCAbilityTypeDefinition.getBuffId(worldEditorAbility, level);
		this.areaOfEffect = worldEditorAbility.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
	}

	@Override
	public boolean doEffect(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		this.spirits.clear();
		final float facingRad = (float) StrictMath.toRadians(caster.getFacing());
		final int count = this.spiritTypes.size();
		for (int i = 0; i < count; i++) {
			final double angle = (facingRad + SPIRIT_ANGLE_OFFSET) - ((2 * Math.PI * i) / count);
			final float x = caster.getX() + (float) (StrictMath.cos(angle) * this.areaOfEffect);
			final float y = caster.getY() + (float) (StrictMath.sin(angle) * this.areaOfEffect);
			final CUnit spirit = simulation.createUnitSimple(this.spiritTypes.get(i), caster.getPlayerIndex(), x, y,
					caster.getFacing());
			if (spirit == null) {
				continue;
			}
			spirit.add(simulation, new CBuffTimedLife(simulation.getHandleIdAllocator().createId(), this.buffId,
					getDuration(), false));
			simulation.createTemporarySpellEffectOnUnit(spirit, getAlias(), CEffectType.SPECIAL);
			this.spirits.add(spirit);
		}
		if (this.spirits.isEmpty()) {
			return false;
		}
		this.lastX = caster.getX();
		this.lastY = caster.getY();
		this.allDied = false;
		caster.setHidden(true);
		caster.addStateModBuff(HIDDEN_INVULNERABLE);
		caster.computeUnitState(simulation, StateModBuffType.INVULNERABLE);
		this.endTick = simulation.getGameTurnTick() + CTimer.wholeSteps(getDuration());
		return true;
	}

	@Override
	public boolean doChannelTick(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		boolean anyAlive = false;
		for (final CUnit spirit : this.spirits) {
			if (!spirit.isDead()) {
				anyAlive = true;
				// Measured on 1.29: the Brewmaster comes back where the last listed living spirit stands.
				this.lastX = spirit.getX();
				this.lastY = spirit.getY();
			}
		}
		if (!anyAlive) {
			this.allDied = true;
			return false;
		}
		return simulation.getGameTurnTick() < this.endTick;
	}

	@Override
	public void doChannelEnd(final CSimulation game, final CUnit caster, final AbilityTarget target,
			final boolean interrupted) {
		// An order to the hidden Brewmaster ends the split early, as on 1.29.
		for (final CUnit spirit : this.spirits) {
			if (!spirit.isDead()) {
				spirit.kill(game);
			}
		}
		this.spirits.clear();
		if (!caster.isHidden()) {
			return;
		}
		caster.removeStateModBuff(HIDDEN_INVULNERABLE);
		caster.computeUnitState(game, StateModBuffType.INVULNERABLE);
		caster.setHidden(false);
		// Measured on 1.29: exactly on the spirit's spot, which that spirit just made walkable.
		caster.setPoint(this.lastX, this.lastY, game.getWorldCollision(), game.getRegionManager());
		game.unitRepositioned(caster);
		if (this.allDied) {
			// Measured on 1.29: when every spirit has been killed the Brewmaster dies with them.
			caster.kill(game);
		}
	}

	private static final double SPIRIT_ANGLE_OFFSET = StrictMath.toRadians(90);
}
