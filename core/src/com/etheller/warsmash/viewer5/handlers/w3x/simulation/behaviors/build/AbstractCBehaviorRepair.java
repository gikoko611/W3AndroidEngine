package com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.build;

import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens;
import com.etheller.warsmash.viewer5.handlers.w3x.SequenceUtils;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.build.AbstractCAbilityRepair;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetStillAliveAndTargetableVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CAbstractRangedBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;

/**
 * Repairing a unit or destructable: the cost is paid step by step while the life goes up, and the work stops when the
 * player cannot pay.
 */
public abstract class AbstractCBehaviorRepair extends CAbstractRangedBehavior {
	private final AbstractCAbilityRepair ability;
	private final AbilityTargetStillAliveAndTargetableVisitor stillAliveVisitor;

	private int nextNotifyTick = 0;

	private float totalGoldCharged = 0;
	private float totalLumberCharged = 0;

	protected float goldPerUpdate = 0;
	protected float lumberPerUpdate = 0;
	protected float hpPerUpdate = 0;

	public AbstractCBehaviorRepair(final CUnit unit, final AbstractCAbilityRepair ability) {
		super(unit);
		this.ability = ability;
		this.stillAliveVisitor = new AbilityTargetStillAliveAndTargetableVisitor();
	}

	public CBehavior reset(final CSimulation game, final CWidget target) {
		this.nextNotifyTick = 0;
		this.totalGoldCharged = 0;
		this.totalLumberCharged = 0;
		if (target instanceof CUnit) {
			final CUnit taru = (CUnit) target;
			if (!setBuildRates(taru)) {
				this.goldPerUpdate = taru.getUnitType().getGoldRepairCost()
						* (WarsmashConstants.SIMULATION_STEP_TIME / taru.getUnitType().getRepairTime())
						* this.ability.getRepairCostRatio() / this.ability.getRepairTimeRatio();
				this.lumberPerUpdate = taru.getUnitType().getLumberRepairCost()
						* (WarsmashConstants.SIMULATION_STEP_TIME / taru.getUnitType().getRepairTime())
						* this.ability.getRepairCostRatio() / this.ability.getRepairTimeRatio();
				this.hpPerUpdate = taru.getMaxLife()
						* (WarsmashConstants.SIMULATION_STEP_TIME / taru.getUnitType().getRepairTime())
						/ this.ability.getRepairTimeRatio();
			}
		}
		else if (target instanceof CDestructable) {
			final CDestructable tard = (CDestructable) target;
			this.goldPerUpdate = tard.getDestType().getGoldRepairCost()
					* (WarsmashConstants.SIMULATION_STEP_TIME / tard.getDestType().getRepairTime())
					* this.ability.getRepairCostRatio() / this.ability.getRepairTimeRatio();
			this.lumberPerUpdate = tard.getDestType().getLumberRepairCost()
					* (WarsmashConstants.SIMULATION_STEP_TIME / tard.getDestType().getRepairTime())
					* this.ability.getRepairCostRatio() / this.ability.getRepairTimeRatio();
			this.hpPerUpdate = tard.getMaxLife()
					* (WarsmashConstants.SIMULATION_STEP_TIME / tard.getDestType().getRepairTime())
					/ this.ability.getRepairTimeRatio();
		}
		else {
			this.goldPerUpdate = 0;
			this.lumberPerUpdate = 0;
			this.hpPerUpdate = 0;
		}
		return innerReset(game, target, false);
	}

	protected AbstractCAbilityRepair getAbility() {
		return this.ability;
	}

	/** Sets the rates for helping to build the target instead of repairing it; false when this is a repair. */
	protected boolean setBuildRates(final CUnit target) {
		return false;
	}

	/** Whole gold or lumber paid for a running repair cost: a started unit is paid at once. */
	static int chargedSoFar(final float cost) {
		return (int) Math.ceil(cost);
	}

	@Override
	public boolean isWithinRange(final CSimulation simulation) {
		float castRange = this.ability.getCastRange();
		if (this.target instanceof CUnit) {
			final CUnit unitTarget = (CUnit) this.target;
			if (unitTarget.getMovementType() == PathingGrid.MovementType.FLOAT) {
				castRange += this.ability.getNavalRangeBonus();
			}
		}
		return this.unit.canReach(this.target, castRange);
	}

	@Override
	protected CBehavior update(final CSimulation simulation, final boolean withinFacingWindow) {
		final CUnit targetUnit = this.target.visit(AbilityTargetVisitor.UNIT);
		final boolean building = isBuildTarget(targetUnit);
		if (this.target instanceof CWidget) {
			final CWidget targetWidget = (CWidget) this.target;
			if ((targetWidget.getLife() >= targetWidget.getMaxLife()) && !building && stopsWhenUndamaged()) {
				return this.unit.pollNextOrderBehavior(simulation);
			}
		}
		if (!building || (targetUnit.getWorker() != this.unit)) {
			final float ng = this.totalGoldCharged + this.goldPerUpdate
					+ extraCharge(targetUnit, this.goldPerUpdate);
			final float nl = this.totalLumberCharged + this.lumberPerUpdate
					+ extraCharge(targetUnit, this.lumberPerUpdate);
			final int gdx = chargedSoFar(ng) - chargedSoFar(this.totalGoldCharged);
			final int ldx = chargedSoFar(nl) - chargedSoFar(this.totalLumberCharged);
			this.totalGoldCharged = ng;
			this.totalLumberCharged = nl;
			if (!simulation.getPlayer(this.unit.getPlayerIndex()).charge(gdx, ldx)) {
				return this.unit.pollNextOrderBehavior(simulation);
			}
		}
		this.unit.getUnitAnimationListener().playAnimation(false, AnimationTokens.PrimaryTag.STAND, SequenceUtils.WORK,
				1.0f, true);
		if ((this.nextNotifyTick == 0) || (simulation.getGameTurnTick() >= this.nextNotifyTick)) {
			if (this.nextNotifyTick == 0) {
				this.nextNotifyTick = simulation.getGameTurnTick()
						+ WarsmashConstants.ONGOING_BEHAVIOR_NOTIFICATION_TICKS;
			}
			else {
				this.unit.fireBehaviorChangeEvent(simulation, this, true);
			}
		}
		if (this.target instanceof CWidget) {
			if (building) {
				return buildStep(simulation, targetUnit);
			}
			final CWidget targetWidget = (CWidget) this.target;
			float newLifeValue = targetWidget.getLife() + this.hpPerUpdate;
			final boolean done = newLifeValue > targetWidget.getMaxLife();
			if (done) {
				newLifeValue = targetWidget.getMaxLife();
			}
			targetWidget.setLife(simulation, newLifeValue);
			if (done) {
				return this.unit.pollNextOrderBehavior(simulation);
			}
		}
		return this;
	}

	/** Whether the target is a building this worker helps to build rather than repairs. */
	protected boolean isBuildTarget(final CUnit targetUnit) {
		return false;
	}

	/** Whether the work ends without a charge when the target is found at full life. */
	protected boolean stopsWhenUndamaged() {
		return false;
	}

	/** What a step costs on top of the repair rate. */
	protected float extraCharge(final CUnit targetUnit, final float perUpdate) {
		return 0;
	}

	/** One step of helping to build the target. */
	protected CBehavior buildStep(final CSimulation simulation, final CUnit targetUnit) {
		return this;
	}

	@Override
	protected CBehavior updateOnInvalidTarget(final CSimulation simulation) {
		return this.unit.pollNextOrderBehavior(simulation);
	}

	@Override
	protected boolean checkTargetStillValid(final CSimulation simulation) {
		return this.target.visit(this.stillAliveVisitor.reset(simulation, this.unit, this.ability.getTargetsAllowed()));
	}

	@Override
	protected void resetBeforeMoving(final CSimulation simulation) {
	}

	@Override
	public void begin(final CSimulation game) {
	}

	@Override
	public void end(final CSimulation game, final boolean interrupted) {
	}

	@Override
	public void endMove(final CSimulation game, final boolean interrupted) {
	}

	@Override
	public int getHighlightOrderId() {
		return this.ability.getBaseOrderId();
	}

	@Override
	public boolean interruptable() {
		return true;
	}

	@Override
	public CBehaviorCategory getBehaviorCategory() {
		return CBehaviorCategory.BUILD;
	}
}
