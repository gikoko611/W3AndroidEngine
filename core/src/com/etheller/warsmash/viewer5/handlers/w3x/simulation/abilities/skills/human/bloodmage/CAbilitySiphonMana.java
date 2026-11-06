package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.bloodmage;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffSpellArt;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponent;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentLightning;

/**
 * Channeled drain shared by the drain abilities: every interval it takes life and mana from an enemy target and
 * gives the caster what it took, or gives the caster's life and mana to a friendly target. What the caster gets while already
 * at its maximum is kept as a bonus above it (up to the bonus factor of the maximum) that decays after the channel.
 * The channel breaks when the target dies or moves beyond the break distance.
 */
public class CAbilitySiphonMana extends CAbilityTargetSpellBase {
	private static final int KIND_BOTH = 0;
	private static final int KIND_LIFE = 1;
	private static final int KIND_MANA = 2;
	// The buff list holds the caster, target and bonus buffs, each for life and mana, life only and mana only.
	private static final int TARGET_BUFF_OFFSET = 3;
	private static final int BONUS_BUFF_OFFSET = 6;

	private float lifeDrainedPerSecond;
	private float manaDrainedPerSecond;
	private float interval;
	private float lifeTransferredPerSecond;
	private float manaTransferredPerSecond;
	private float bonusLifeFactor;
	private float bonusLifeDecay;
	private float bonusManaFactor;
	private float bonusManaDecay;
	private float breakDistance;
	private final War3ID[] buffIds = new War3ID[9];

	private int channelEndTick;
	private SimulationRenderComponentLightning beam;
	private SimulationRenderComponent loopSound;
	private int nextDrainTick;
	private CBuffSpellArt casterBuff;
	private CBuffSpellArt targetBuff;
	private CUnit buffedTarget;
	private CBuffDrainBonus bonus;

	public CAbilitySiphonMana(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.drain;
	}

	@Override
	public void populateData(final GameObject worldEditorAbility, final int level) {
		this.lifeDrainedPerSecond = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.manaDrainedPerSecond = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.interval = Math.max(0.1f, worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_C + level, 0));
		this.lifeTransferredPerSecond = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_D + level, 0);
		this.manaTransferredPerSecond = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_E + level, 0);
		this.bonusLifeFactor = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_F + level, 0);
		this.bonusLifeDecay = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_G + level, 0);
		this.bonusManaFactor = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_H + level, 0);
		this.bonusManaDecay = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_I + level, 0);
		this.breakDistance = worldEditorAbility.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		for (int i = 0; i < this.buffIds.length; i++) {
			this.buffIds[i] = AbstractCAbilityTypeDefinition.getBuffId(worldEditorAbility, level, i);
		}
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit unit, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		final CUnit targetUnit = target.visit(AbilityTargetVisitor.UNIT);
		if ((targetUnit != null) && isFriendly(game, unit, targetUnit) && isFullForTransfer(targetUnit)) {
			receiver.targetCheckFailed(CommandStringErrorKeys.ALREADY_AT_FULL_MANA);
			return;
		}
		super.innerCheckCanTargetSpell(game, unit, orderId, target, receiver);
	}

	@Override
	public boolean doEffect(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		final CUnit targetUnit = target.visit(AbilityTargetVisitor.UNIT);
		if (targetUnit == null) {
			return false;
		}
		final int tick = simulation.getGameTurnTick();
		this.channelEndTick = tick + ticks(getDurationForTarget(targetUnit));
		this.nextDrainTick = tick + ticks(this.interval);
		final boolean friendly = isFriendly(simulation, caster, targetUnit);
		final int kind = friendly ? transferKind() : drainKind();
		removeBuffs(simulation, caster);
		this.casterBuff = addArtBuff(simulation, caster, this.buffIds[kind], true);
		this.targetBuff = addArtBuff(simulation, targetUnit, this.buffIds[TARGET_BUFF_OFFSET + kind], friendly);
		this.buffedTarget = targetUnit;
		if (!friendly) {
			startBonus(simulation, caster, kind);
			if (this.lifeDrainedPerSecond > 0) {
				// Measured on 1.29: Life Drain hits its target for 0 on the effect.
				targetUnit.damage(simulation, caster, DAMAGE_FLAGS, CAttackType.SPELLS, CDamageType.UNIVERSAL, null,
						0f);
			}
		}
		removeBeam();
		// A negative duration keeps the beam until the channel ends instead of the lightning type's own duration.
		this.beam = simulation.createAbilityLightning(caster, getAlias(), kind, targetUnit, -1f);
		this.loopSound = simulation.unitLoopSoundEffectEvent(caster, getAlias());
		return true;
	}

	private void startBonus(final CSimulation simulation, final CUnit caster, final int kind) {
		final boolean lifeBonus = (kind != KIND_MANA) && (this.bonusLifeFactor > 0);
		final boolean manaBonus = (kind != KIND_LIFE) && (this.bonusManaFactor > 0);
		if (!lifeBonus && !manaBonus) {
			return;
		}
		if ((this.bonus == null) || !caster.getAbilities().contains(this.bonus)) {
			final War3ID bonusId = this.buffIds[BONUS_BUFF_OFFSET + kind];
			if ((bonusId == null) || War3ID.NONE.equals(bonusId)) {
				return;
			}
			this.bonus = new CBuffDrainBonus(simulation.getHandleIdAllocator().createId(), bonusId);
			caster.add(simulation, this.bonus);
		}
		this.bonus.startChannel(lifeBonus ? this.bonusLifeFactor : 0, this.bonusLifeDecay,
				manaBonus ? this.bonusManaFactor : 0, this.bonusManaDecay);
	}

	private static CBuffSpellArt addArtBuff(final CSimulation simulation, final CUnit unit, final War3ID buffId,
			final boolean positive) {
		if ((buffId == null) || War3ID.NONE.equals(buffId)) {
			return null;
		}
		final CBuffSpellArt buff = new CBuffSpellArt(simulation.getHandleIdAllocator().createId(), buffId, positive,
				true);
		unit.add(simulation, buff);
		return buff;
	}

	@Override
	public void doChannelEnd(final CSimulation game, final CUnit unit, final AbilityTarget target,
			final boolean interrupted) {
		removeBeam();
		removeBuffs(game, unit);
		if ((this.bonus != null) && unit.getAbilities().contains(this.bonus)) {
			this.bonus.endChannel(game, unit);
		}
	}

	private void removeBuffs(final CSimulation game, final CUnit caster) {
		if ((this.casterBuff != null) && caster.getAbilities().contains(this.casterBuff)) {
			caster.remove(game, this.casterBuff);
		}
		if ((this.targetBuff != null) && (this.buffedTarget != null)
				&& this.buffedTarget.getAbilities().contains(this.targetBuff)) {
			this.buffedTarget.remove(game, this.targetBuff);
		}
		this.casterBuff = null;
		this.targetBuff = null;
		this.buffedTarget = null;
	}

	private void removeBeam() {
		if (this.beam != null) {
			this.beam.remove();
			this.beam = null;
		}
		if (this.loopSound != null) {
			this.loopSound.remove();
			this.loopSound = null;
		}
	}

	@Override
	public boolean doChannelTick(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		final CUnit targetUnit = target.visit(AbilityTargetVisitor.UNIT);
		if ((targetUnit == null) || targetUnit.isDead() || caster.isDead()
				|| ((this.breakDistance > 0) && (caster.distance(targetUnit) > this.breakDistance))) {
			return false;
		}
		final int tick = simulation.getGameTurnTick();
		if (tick >= this.nextDrainTick) {
			this.nextDrainTick = tick + ticks(this.interval);
			if (isFriendly(simulation, caster, targetUnit)) {
				final float given = Math.min(caster.getMana(), this.manaTransferredPerSecond * this.interval);
				caster.setMana(caster.getMana() - given);
				targetUnit.setMana(Math.min(targetUnit.getMaximumMana(), targetUnit.getMana() + given));
				if (this.lifeTransferredPerSecond > 0) {
					final float life = Math.min(caster.getLife() - 1,
							this.lifeTransferredPerSecond * this.interval);
					if (life > 0) {
						caster.setLife(simulation, caster.getLife() - life);
						targetUnit.setLife(simulation, Math.min(targetUnit.getMaximumLife(), targetUnit.getLife() + life));
					}
				}
				// Measured: the transfer stops once the ally is full, even with time left.
				if (isFullForTransfer(targetUnit)) {
					return false;
				}
			}
			else {
				final float mana = Math.min(targetUnit.getMana(), this.manaDrainedPerSecond * this.interval);
				if (mana > 0) {
					targetUnit.setMana(targetUnit.getMana() - mana);
					giveMana(caster, mana);
				}
				if (this.lifeDrainedPerSecond > 0) {
					final float lifeBefore = targetUnit.getLife();
					// Measured: the drain is spell damage, so heroes lose 70% of it and armor does not count; the
					// caster gets what the target lost.
					targetUnit.damage(simulation, caster, DAMAGE_FLAGS, CAttackType.SPELLS, CDamageType.UNIVERSAL,
							null, this.lifeDrainedPerSecond * this.interval);
					final float lifeTaken = lifeBefore - Math.max(0, targetUnit.getLife());
					if (lifeTaken > 0) {
						giveLife(simulation, caster, lifeTaken);
					}
				}
			}
		}
		return tick < this.channelEndTick;
	}

	/**
	 * Measured: only what arrives while the caster is already at its maximum becomes the bonus; the rest of a gain
	 * that tops it up is lost.
	 */
	private void giveMana(final CUnit caster, final float mana) {
		if (caster.getMana() >= caster.getMaximumMana()) {
			if (hasBonus(caster)) {
				this.bonus.addMana(caster, mana);
			}
		}
		else {
			caster.setMana(Math.min(caster.getMaximumMana(), caster.getMana() + mana));
		}
	}

	private void giveLife(final CSimulation simulation, final CUnit caster, final float life) {
		if (caster.getLife() >= caster.getMaximumLife()) {
			if (hasBonus(caster)) {
				this.bonus.addLife(caster, life);
			}
		}
		else {
			caster.setLife(simulation, Math.min(caster.getMaximumLife(), caster.getLife() + life));
		}
	}

	private boolean hasBonus(final CUnit caster) {
		return (this.bonus != null) && caster.getAbilities().contains(this.bonus);
	}

	private boolean isFullForTransfer(final CUnit ally) {
		final boolean manaFull = ally.getMana() >= ally.getMaximumMana();
		if (this.lifeTransferredPerSecond <= 0) {
			return manaFull;
		}
		final boolean lifeFull = ally.getLife() >= ally.getMaximumLife();
		return (this.manaTransferredPerSecond <= 0) ? lifeFull : (lifeFull && manaFull);
	}

	private int transferKind() {
		final boolean life = this.lifeTransferredPerSecond > 0;
		final boolean mana = this.manaTransferredPerSecond > 0;
		if (life && mana) {
			return KIND_BOTH;
		}
		return life ? KIND_LIFE : KIND_MANA;
	}

	private int drainKind() {
		final boolean life = this.lifeDrainedPerSecond > 0;
		final boolean mana = this.manaDrainedPerSecond > 0;
		if (life && mana) {
			return KIND_BOTH;
		}
		return life ? KIND_LIFE : KIND_MANA;
	}

	private static boolean isFriendly(final CSimulation simulation, final CUnit caster, final CUnit target) {
		return simulation.getPlayer(caster.getPlayerIndex()).hasAlliance(target.getPlayerIndex(),
				CAllianceType.SHARED_SPELLS);
	}

	private static int ticks(final float seconds) {
		return (int) StrictMath.ceil(seconds / WarsmashConstants.SIMULATION_STEP_TIME);
	}
}
