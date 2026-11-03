package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.darkranger;


import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public class CAbilityCharm extends CAbilityTargetSpellBase {
	private int maximumCreepLevel;

	public CAbilityCharm(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public void populateData(final GameObject worldEditorAbility, final int level) {
		this.maximumCreepLevel = worldEditorAbility.getFieldAsInteger(AbilityFields.DATA_A + level, 0);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.charm;
	}

	@Override
	protected void innerCheckCanTarget(final CSimulation game, final CUnit unit, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		final CUnit targetUnit = target.visit(AbilityTargetVisitor.UNIT);
		if (targetUnit != null) {
			if (targetUnit.getUnitType().getLevel() <= maximumCreepLevel) {
				super.innerCheckCanTarget(game, unit, orderId, target, receiver);
			}
			else {
				receiver.targetCheckFailed(CommandStringErrorKeys.THAT_CREATURE_IS_TOO_POWERFUL);
			}
		}
		else {
			receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_UNIT_WITH_THIS_ACTION);
		}
	}

	@Override
	public boolean doEffect(final CSimulation simulation, final CUnit unit, final AbilityTarget target) {
		final CUnit targetUnit = target.visit(AbilityTargetVisitor.UNIT);
		simulation.createTemporarySpellEffectOnUnit(targetUnit, getAlias(), CEffectType.TARGET);
		targetUnit.changeOwner(simulation, unit.getPlayerIndex(), true);
		return false;
	}

	public int getMaximumCreepLevel() {
		return maximumCreepLevel;
	}

	public void setMaximumCreepLevel(final int maximumCreepLevel) {
		this.maximumCreepLevel = maximumCreepLevel;
	}
}
