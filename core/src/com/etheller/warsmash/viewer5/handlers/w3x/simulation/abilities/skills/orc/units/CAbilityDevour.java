package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.units;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.creeps.CCreepCastAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

/**
 * Devour (also the creep version, which answers its own order): swallows a land unit into the caster's Cargo Hold
 * (Devour), one at a time.
 */
public class CAbilityDevour extends CAbilityTargetSpellBase implements CCreepCastAbility {
	private int maximumCreepLevel;
	private War3ID digestingBuffId;
	private final int orderId;

	public CAbilityDevour(final int handleId, final War3ID alias) {
		this(handleId, alias, OrderIds.devour);
	}

	public CAbilityDevour(final int handleId, final War3ID alias, final int orderId) {
		super(handleId, alias);
		this.orderId = orderId;
	}

	@Override
	public int getBaseOrderId() {
		return this.orderId;
	}

	@Override
	public void populateData(final GameObject worldEditorAbility, final int level) {
		this.maximumCreepLevel = worldEditorAbility.getFieldAsInteger(AbilityFields.DATA_A + level, 0);
		this.digestingBuffId = AbstractCAbilityTypeDefinition.getBuffId(worldEditorAbility, level, 0);
	}

	@Override
	protected void innerCheckCanUseSpell(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityActivationReceiver receiver) {
		final CAbilityDevourCargo cargo = unit.getFirstAbilityOfType(CAbilityDevourCargo.class);
		if ((cargo == null) || !cargo.isEmpty()) {
			receiver.activationCheckFailed(CommandStringErrorKeys.UNABLE_TO_LOAD_TARGET);
		}
		else {
			receiver.useOk();
		}
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit unit, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		final CUnit targetUnit = target.visit(AbilityTargetVisitor.UNIT);
		if ((targetUnit == null) || targetUnit.isBuilding()) {
			receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_UNIT_WITH_THIS_ACTION);
		}
		else if ((this.maximumCreepLevel > 0) && game.isNeutralAggressive(targetUnit.getPlayerIndex())
				&& (targetUnit.getUnitType().getLevel() > this.maximumCreepLevel)) {
			receiver.targetCheckFailed(CommandStringErrorKeys.THAT_CREATURE_IS_TOO_POWERFUL);
		}
		else {
			receiver.targetOk(target);
		}
	}

	@Override
	public boolean doEffect(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		final CUnit targetUnit = target.visit(AbilityTargetVisitor.UNIT);
		final CAbilityDevourCargo cargo = caster.getFirstAbilityOfType(CAbilityDevourCargo.class);
		if ((targetUnit != null) && !targetUnit.isDead() && (cargo != null) && cargo.isEmpty()) {
			simulation.createTemporarySpellEffectOnUnit(caster, getAlias(), CEffectType.SPECIAL);
			cargo.devour(simulation, caster, targetUnit, this.digestingBuffId);
		}
		return false;
	}

	/**
	 * Measured on 1.29 (Dragon Turtle): the creep devours the unit it attacks once that unit is within its attack
	 * range and the spell's reach, not while they close in.
	 */
	@Override
	public boolean tryCreepCast(final CSimulation game, final CUnit creep, final CUnit attackTarget,
			final boolean inAttackRange) {
		if ((getCooldownRemaining(game, creep) > 0) || (creep.getMana() < getManaCost())) {
			return false;
		}
		if ((this.orderId != OrderIds.creepdevour) || !inAttackRange || !creep.canReach(attackTarget, getCastRange())) {
			return false;
		}
		return creep.order(game, this.orderId, attackTarget, true);
	}

	@Override
	public boolean isMagic() {
		return false;
	}

	@Override
	public boolean isPhysical() {
		return true;
	}
}
