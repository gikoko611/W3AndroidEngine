package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.deathknight;

import java.util.ArrayList;
import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.creeps.CCreepCastAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimedLife;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;

/**
 * Animate Dead: raises corpses of any owner around the caster as its own units for the duration, invulnerable when
 * the spell says so (the creep version's are not).
 */
public class CAbilityAnimateDead extends CAbilityNoTargetSpellBase implements CCreepCastAbility {
	// Measured on 1.29 (Satyr Hellcaller): a creep raises the dead once there are as many corpses as the spell raises
	// (5 of 6 do not), or with fewer corpses at 36 % life or less (37 % does not).
	private static final float CREEP_CAST_LIFE_RATIO = 0.36f;

	private int numberOfCorpsesRaised;
	private boolean raisedUnitsInvulnerable;
	private float areaOfEffect;
	private War3ID buffId;
	private final int orderId;

	public CAbilityAnimateDead(final int handleId, final War3ID alias) {
		this(handleId, alias, OrderIds.animatedead);
	}

	/**
	 * The creep version answers its own order.
	 */
	public CAbilityAnimateDead(final int handleId, final War3ID alias, final int orderId) {
		super(handleId, alias);
		this.orderId = orderId;
	}

	@Override
	public int getBaseOrderId() {
		return this.orderId;
	}

	@Override
	public void populateData(final GameObject worldEditorAbility, final int level) {
		this.numberOfCorpsesRaised = worldEditorAbility.getFieldAsInteger(AbilityFields.DATA_A + level, 0);
		this.raisedUnitsInvulnerable = worldEditorAbility.getFieldAsInteger(AbilityFields.DATA_B + level, 0) != 0;
		this.areaOfEffect = worldEditorAbility.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		this.buffId = AbstractCAbilityTypeDefinition.getBuffId(worldEditorAbility, level);
	}

	private List<CUnit> findCorpses(final CSimulation simulation, final CUnit caster) {
		final List<CUnit> corpses = new ArrayList<>();
		simulation.getWorldCollision().enumCorpsesInRange(caster.getX(), caster.getY(), this.areaOfEffect,
				(enumUnit) -> {
					if ((corpses.size() < this.numberOfCorpsesRaised) && (enumUnit.getHeroData() == null)
							&& enumUnit.canBeTargetedBy(simulation, caster, getTargetsAllowed())) {
						corpses.add(enumUnit);
					}
					return corpses.size() >= this.numberOfCorpsesRaised;
				});
		return corpses;
	}

	@Override
	public boolean tryCreepCast(final CSimulation game, final CUnit creep, final CUnit attackTarget,
			final boolean inAttackRange) {
		if ((getCooldownRemaining(game, creep) > 0) || (creep.getMana() < getManaCost())) {
			return false;
		}
		if ((this.orderId != OrderIds.creepanimatedead) || !inAttackRange) {
			return false;
		}
		final int corpses = findCorpses(game, creep).size();
		if ((corpses == 0) || ((corpses < this.numberOfCorpsesRaised)
				&& (creep.getLife() > (CREEP_CAST_LIFE_RATIO * creep.getMaximumLife())))) {
			return false;
		}
		return creep.order(game, this.orderId, null, true);
	}

	@Override
	public boolean doEffect(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		final List<CUnit> corpses = findCorpses(simulation, caster);
		for (final CUnit corpse : corpses) {
			corpse.resurrectAsAnimatedCorpse(simulation);
			if (corpse.getPlayerIndex() != caster.getPlayerIndex()) {
				corpse.setPlayerIndex(simulation, caster.getPlayerIndex(), true);
			}
			if (this.raisedUnitsInvulnerable) {
				corpse.setInvulnerableSource(simulation, CUnit.ANIMATED_INVULNERABLE, true);
			}
			corpse.add(simulation, new CBuffTimedLife(simulation.getHandleIdAllocator().createId(), this.buffId,
					getDuration(), false));
			simulation.createTemporarySpellEffectOnUnit(corpse, getAlias(), CEffectType.TARGET);
		}
		simulation.createTemporarySpellEffectOnUnit(caster, getAlias(), CEffectType.CASTER);
		return false;
	}
}
