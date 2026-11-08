package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.build;

import java.util.EnumSet;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.AutocastType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.CAutocastAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.AbstractGenericSingleIconActiveAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.build.AbstractCBehaviorRepair;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

/** Repairing (also Renew and Restoration) at the cost and rate of the ability's ratios. */
public abstract class AbstractCAbilityRepair extends AbstractGenericSingleIconActiveAbility
		implements CAutocastAbility {
	protected EnumSet<CTargetType> targetsAllowed;
	private float navalRangeBonus;
	private float repairCostRatio;
	private float repairTimeRatio;
	private float castRange;
	private AbstractCBehaviorRepair behaviorRepair;
	private boolean autocasting = false;

	public AbstractCAbilityRepair(final int handleId, final War3ID code, final War3ID alias,
			final EnumSet<CTargetType> targetsAllowed, final float navalRangeBonus, final float repairCostRatio,
			final float repairTimeRatio, final float castRange) {
		super(handleId, code, alias);
		this.targetsAllowed = targetsAllowed;
		this.targetsAllowed.add(CTargetType.REPAIRABLE);
		this.navalRangeBonus = navalRangeBonus;
		this.repairCostRatio = repairCostRatio;
		this.repairTimeRatio = repairTimeRatio;
		this.castRange = castRange;
	}

	protected abstract AbstractCBehaviorRepair createBehavior(CUnit unit);

	/** Refuses a target, quietly for a right-click. */
	protected static void refuse(final int orderId, final AbilityTargetCheckReceiver<CWidget> receiver,
			final String errorKey) {
		if (orderId == OrderIds.smart) {
			receiver.orderIdNotAccepted();
		}
		else {
			receiver.targetCheckFailed(errorKey);
		}
	}

	@Override
	protected void innerCheckCanSmartTarget(final CSimulation game, final CUnit unit, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		innerCheckCanTarget(game, unit, orderId, target, receiver);
	}

	@Override
	protected void innerCheckCanTarget(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityPointTarget target, final AbilityTargetCheckReceiver<AbilityPointTarget> receiver) {
		receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_UNIT_WITH_THIS_ACTION);
	}

	@Override
	protected void innerCheckCanSmartTarget(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityPointTarget target, final AbilityTargetCheckReceiver<AbilityPointTarget> receiver) {
		innerCheckCanTarget(game, unit, orderId, target, receiver);
	}

	@Override
	protected void innerCheckCanTargetNoTarget(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityTargetCheckReceiver<Void> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	protected void innerCheckCanUse(final CSimulation game, final CUnit unit, final int playerIndex, final int orderId,
			final AbilityActivationReceiver receiver) {
		receiver.useOk();
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		this.behaviorRepair = createBehavior(unit);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {

	}

	@Override
	public void onTick(final CSimulation game, final CUnit unit) {

	}

	@Override
	public void onCancelFromQueue(final CSimulation game, final CUnit unit, final int playerIndex, final int orderId) {

	}

	@Override
	public CBehavior begin(final CSimulation game, final CUnit caster, final int playerIndex, final int orderId,
			final boolean autoOrder, final CWidget target) {
		return this.behaviorRepair.reset(game, target);
	}

	@Override
	public CBehavior begin(final CSimulation game, final CUnit caster, final int playerIndex, final int orderId,
			final boolean autoOrder, final AbilityPointTarget point) {
		return null;
	}

	@Override
	public CBehavior beginNoTarget(final CSimulation game, final CUnit caster, final int playerIndex, final int orderId,
			final boolean autoOrder) {
		return null;
	}

	@Override
	public boolean isToggleOn() {
		return false;
	}

	public EnumSet<CTargetType> getTargetsAllowed() {
		return this.targetsAllowed;
	}

	public float getNavalRangeBonus() {
		return this.navalRangeBonus;
	}

	public float getRepairCostRatio() {
		return this.repairCostRatio;
	}

	public float getRepairTimeRatio() {
		return this.repairTimeRatio;
	}

	public float getCastRange() {
		return this.castRange;
	}

	public void setTargetsAllowed(final EnumSet<CTargetType> targetsAllowed) {
		this.targetsAllowed = targetsAllowed;
	}

	public void setNavalRangeBonus(final float navalRangeBonus) {
		this.navalRangeBonus = navalRangeBonus;
	}

	public void setRepairCostRatio(final float repairCostRatio) {
		this.repairCostRatio = repairCostRatio;
	}

	public void setRepairTimeRatio(final float repairTimeRatio) {
		this.repairTimeRatio = repairTimeRatio;
	}

	public void setCastRange(final float castRange) {
		this.castRange = castRange;
	}

	@Override
	public AutocastType getAutocastType() {
		return AutocastType.NEARESTVALID;
	}

	@Override
	public void setAutoCastOn(final CSimulation simulation, final CUnit caster, final boolean autoCastOn,
			final boolean notify) {
		this.autocasting = autoCastOn;
		if (notify) {
			caster.setAutocastAbility(simulation, autoCastOn ? this : null);
		}
	}

	@Override
	public boolean isAutoCastOn() {
		return this.autocasting;
	}

	@Override
	public void checkCanAutoTarget(final CSimulation game, final CUnit unit, final int playerIndex, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		this.checkCanTarget(game, unit, playerIndex, orderId, false, target, receiver);
	}

	@Override
	public void checkCanAutoTarget(final CSimulation game, final CUnit unit, final int playerIndex, final int orderId,
			final AbilityPointTarget target, final AbilityTargetCheckReceiver<AbilityPointTarget> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	public void checkCanAutoTargetNoTarget(final CSimulation game, final CUnit unit, final int playerIndex,
			final int orderId, final AbilityTargetCheckReceiver<Void> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	public boolean isPhysical() {
		return true;
	}

	@Override
	public boolean isMagic() {
		return false;
	}

	@Override
	public boolean isUniversal() {
		return false;
	}

	@Override
	public CAbilityCategory getAbilityCategory() {
		return CAbilityCategory.CORE;
	}

}
