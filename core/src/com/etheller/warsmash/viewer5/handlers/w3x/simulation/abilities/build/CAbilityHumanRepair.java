package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.build;

import java.util.EnumSet;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.build.AbstractCBehaviorRepair;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.build.CBehaviorHumanRepair;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

/** Repair that also helps build (power build) a building another Peasant is building. */
public class CAbilityHumanRepair extends AbstractCAbilityRepair {
	private float powerBuildCostRatio;
	private float powerBuildTimeRatio;

	public CAbilityHumanRepair(final int handleId, final War3ID code, final War3ID alias,
			final EnumSet<CTargetType> targetsAllowed, final float navalRangeBonus, final float powerBuildCostRatio,
			final float powerBuildTimeRatio, final float repairCostRatio, final float repairTimeRatio,
			final float castRange) {
		super(handleId, code, alias, targetsAllowed, navalRangeBonus, repairCostRatio, repairTimeRatio, castRange);
		this.powerBuildCostRatio = powerBuildCostRatio;
		this.powerBuildTimeRatio = powerBuildTimeRatio;
	}

	@Override
	protected AbstractCBehaviorRepair createBehavior(final CUnit unit) {
		return new CBehaviorHumanRepair(unit, this);
	}

	@Override
	protected void innerCheckCanTarget(final CSimulation game, final CUnit unit, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target.canBeTargetedBy(game, unit, this.targetsAllowed, receiver)) {
			final CUnit targetUnit = target.visit(AbilityTargetVisitor.UNIT);
			final boolean underConstruction = (targetUnit != null) && targetUnit.isConstructing();
			final boolean doHumanBuild = underConstruction && targetUnit.isConstructingPaused()
					&& (this.powerBuildTimeRatio != 0);
			if (doHumanBuild) {
				// you can human build something even if it is mysteriously 100% hp somehow
				receiver.targetOk(target);
			}
			else if (underConstruction) {
				refuse(orderId, receiver, CommandStringErrorKeys.THAT_BUILDING_IS_CURRENTLY_UNDER_CONSTRUCTION);
			}
			else if (target.getLife() < target.getMaxLife()) {
				receiver.targetOk(target);
			}
			else {
				refuse(orderId, receiver, CommandStringErrorKeys.TARGET_IS_NOT_DAMAGED);
			}
		}
		// else receiver called by canBeTargetedBy
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.repair;
	}

	@Override
	public int getAutoCastOnOrderId() {
		return OrderIds.repairon;
	}

	@Override
	public int getAutoCastOffOrderId() {
		return OrderIds.repairoff;
	}

	public float getPowerBuildCostRatio() {
		return this.powerBuildCostRatio;
	}

	public float getPowerBuildTimeRatio() {
		return this.powerBuildTimeRatio;
	}

	public void setPowerBuildCostRatio(final float powerBuildCostRatio) {
		this.powerBuildCostRatio = powerBuildCostRatio;
	}

	public void setPowerBuildTimeRatio(final float powerBuildTimeRatio) {
		this.powerBuildTimeRatio = powerBuildTimeRatio;
	}
}
