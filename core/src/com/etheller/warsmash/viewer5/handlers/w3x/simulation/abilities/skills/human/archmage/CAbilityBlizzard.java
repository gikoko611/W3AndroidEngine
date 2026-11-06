package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.archmage;

import com.etheller.warsmash.util.RandomBounds;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.math.Rectangle;
import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitEnumFunction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPointTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.GetAbilityByRawcodeVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CSpellDamageFlags;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CWeaponSoundTypeJass;

public class CAbilityBlizzard extends CAbilityPointTargetSpellBase {
	private static final float WAVE_DAMAGE_DELAY = 0.9f;
	// Measured on 1.29: the hit buff lasts more than 0.9 and less than 1 second.
	private static final float HIT_BUFF_DURATION = 0.95f;

	private float buildingReduction;
	private float damage;
	private float damagePerSecond;
	private float maximumDamagePerWave;
	private int shardCount;
	private int waveCount;
	private float waveDelay;
	private float areaOfEffect;
	private War3ID effectId;
	private War3ID hitBuffId;

	private int currentWave;
	private int nextWaveTick;
	private int damageTick = -1;
	private int channelEndTick;
	private final Rectangle recycleRect = new Rectangle();

	public CAbilityBlizzard(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public void populateData(final GameObject worldEditorAbility, final int level) {
		this.buildingReduction = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_D + level, 0);
		this.damage = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.damagePerSecond = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_E + level, 0);
		this.maximumDamagePerWave = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_F + level, 0);
		this.shardCount = worldEditorAbility.getFieldAsInteger(AbilityFields.DATA_C + level, 0);
		this.waveCount = worldEditorAbility.getFieldAsInteger(AbilityFields.DATA_A + level, 0);

		this.waveDelay = getCastingTime();
		setCastingTime(0); // dont use the casting time field normally
		this.areaOfEffect = worldEditorAbility.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		this.effectId = AbstractCAbilityTypeDefinition.getEffectId(worldEditorAbility, level);
		// The second buff (BHbz) marks the units a wave hit; the first (BHbd) is not seen on the caster in 1.29.
		this.hitBuffId = AbstractCAbilityTypeDefinition.getBuffId(worldEditorAbility, level, 1);
		if ((this.hitBuffId == null) || War3ID.NONE.equals(this.hitBuffId)) {
			this.hitBuffId = AbstractCAbilityTypeDefinition.getBuffId(worldEditorAbility, level, 0);
		}
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.blizzard;
	}

	@Override
	public boolean doEffect(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		final int tick = simulation.getGameTurnTick();
		this.currentWave = 0;
		this.damageTick = -1;
		this.nextWaveTick = tick;
		this.channelEndTick = tick + ticks(this.waveDelay * this.waveCount);
		return true;
	}

	/**
	 * Measured on 1.29: the first wave falls on the effect, each wave hits 0.9 seconds
	 * after it falls, a new wave falls every "Cast" seconds and the channel ends "Cast" seconds after the last one.
	 */
	@Override
	public boolean doChannelTick(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		final int tick = simulation.getGameTurnTick();
		if ((this.currentWave < this.waveCount) && (tick >= this.nextWaveTick)) {
			this.currentWave++;
			spawnShards(simulation, caster, target);
			this.damageTick = tick + ticks(WAVE_DAMAGE_DELAY);
			this.nextWaveTick = tick + ticks(this.waveDelay);
		}
		if ((this.damageTick >= 0) && (tick >= this.damageTick)) {
			this.damageTick = -1;
			damageWave(simulation, caster, target);
		}
		return tick < this.channelEndTick;
	}

	private void spawnShards(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		final Random seededRandom = simulation.getSeededRandom();
		for (int i = 0; i < this.shardCount; i++) {
			final float randomAngle = RandomBounds.nextFloat(seededRandom, (float) (StrictMath.PI * 2));
			/* (1 - StrictMath.pow(seededRandom.nextFloat(), 2)) */
			final float randomDistance = seededRandom.nextFloat() * this.areaOfEffect;
			simulation.spawnSpellEffectOnPoint(target.getX() + ((float) StrictMath.cos(randomAngle) * randomDistance),
					target.getY() + ((float) StrictMath.sin(randomAngle) * randomDistance), 0, this.effectId,
					CEffectType.EFFECT, 0).remove();
			simulation.unitSoundEffectEvent(caster, this.effectId);
		}
	}

	private void damageWave(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		final List<CUnit> damageTargets = new ArrayList<>();
		simulation.getWorldCollision().enumUnitsInRect(
				this.recycleRect.set(target.getX() - this.areaOfEffect, target.getY() - this.areaOfEffect,
						this.areaOfEffect * 2, this.areaOfEffect * 2),
				new CUnitEnumFunction() {
					@Override
					public boolean call(final CUnit possibleTarget) {
						if (possibleTarget.canReach(target, CAbilityBlizzard.this.areaOfEffect)
								&& possibleTarget.canBeTargetedBy(simulation, caster, getTargetsAllowed())
								&& !hasHitBuff(possibleTarget)) {
							damageTargets.add(possibleTarget);
						}
						return false;
					}
				});
		float damagePerTarget = this.damage;
		if ((damagePerTarget * damageTargets.size()) > this.maximumDamagePerWave) {
			damagePerTarget = this.maximumDamagePerWave / damageTargets.size();
		}
		final float damagePerTargetBuilding = damagePerTarget * (this.buildingReduction);
		for (final CUnit damageTarget : damageTargets) {
			final float thisTargetDamage = damageTarget.isBuilding() ? damagePerTargetBuilding : damagePerTarget;
			damageTarget.damage(simulation, caster, DAMAGE_FLAGS, CAttackType.SPELLS, CDamageType.COLD,
					CWeaponSoundTypeJass.WHOKNOWS.name(), thisTargetDamage);
			if (!damageTarget.isDead() && (this.hitBuffId != null) && !War3ID.NONE.equals(this.hitBuffId)) {
				damageTarget.add(simulation, new CBuffBlizzard(simulation.getHandleIdAllocator().createId(),
						this.hitBuffId, HIT_BUFF_DURATION));
			}
		}
	}

	private boolean hasHitBuff(final CUnit unit) {
		return (this.hitBuffId != null)
				&& (unit.getAbility(GetAbilityByRawcodeVisitor.getInstance().reset(this.hitBuffId)) != null);
	}

	private static int ticks(final float seconds) {
		return StrictMath.round(seconds / WarsmashConstants.SIMULATION_STEP_TIME);
	}

	@Override
	public float getUIAreaOfEffect() {
		return this.areaOfEffect;
	}

}
