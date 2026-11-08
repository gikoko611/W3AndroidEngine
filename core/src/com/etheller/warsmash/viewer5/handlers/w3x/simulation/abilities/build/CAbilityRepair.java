package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.build;

import java.util.EnumSet;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.build.AbstractCBehaviorRepair;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.build.CBehaviorRepair;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public class CAbilityRepair extends AbstractCAbilityRepair {
	private static final War3ID RENEW = War3ID.fromString("Aren");
	private static final War3ID RESTORE = War3ID.fromString("Arst");
	private final int orderId;
	private final int orderOnId;
	private final int orderOffId;

	public CAbilityRepair(final int handleId, final War3ID code, final War3ID alias,
			final EnumSet<CTargetType> targetsAllowed, final float navalRangeBonus, final float repairCostRatio,
			final float repairTimeRatio, final float castRange) {
		super(handleId, code, alias, targetsAllowed, navalRangeBonus, repairCostRatio, repairTimeRatio, castRange);
		if (code.equals(RENEW)) {
			this.orderId = OrderIds.renew;
			this.orderOnId = OrderIds.renewon;
			this.orderOffId = OrderIds.renewoff;
		}
		else if (code.equals(RESTORE)) {
			this.orderId = OrderIds.restoration;
			this.orderOnId = OrderIds.restorationon;
			this.orderOffId = OrderIds.restorationoff;
		}
		else {
			this.orderId = OrderIds.repair;
			this.orderOnId = OrderIds.repairon;
			this.orderOffId = OrderIds.repairoff;
		}
	}

	@Override
	protected AbstractCBehaviorRepair createBehavior(final CUnit unit) {
		return new CBehaviorRepair(unit, this);
	}

	@Override
	protected void innerCheckCanTarget(final CSimulation game, final CUnit unit, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target.getLife() < target.getMaxLife()) {
			if (target.canBeTargetedBy(game, unit, this.targetsAllowed, receiver)) {
				final CUnit targetUnit = target.visit(AbilityTargetVisitor.UNIT);
				if ((targetUnit != null) && targetUnit.isConstructing()) {
					refuse(orderId, receiver, CommandStringErrorKeys.THAT_BUILDING_IS_CURRENTLY_UNDER_CONSTRUCTION);
				}
				else {
					receiver.targetOk(target);
				}
			}
			// else receiver called by canBeTargetedBy
		}
		else {
			refuse(orderId, receiver, CommandStringErrorKeys.TARGET_IS_NOT_DAMAGED);
		}
	}

	@Override
	public int getBaseOrderId() {
		return this.orderId;
	}

	@Override
	public int getAutoCastOnOrderId() {
		return this.orderOnId;
	}

	@Override
	public int getAutoCastOffOrderId() {
		return this.orderOffId;
	}
}
