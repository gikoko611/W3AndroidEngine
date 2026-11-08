package com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.build;

import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.build.CAbilityHumanRepair;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;

/**
 * Repair that also helps build: the Peasant that started a building builds it at the normal rate for free, every
 * other one adds the power build rate and pays the power build cost.
 */
public class CBehaviorHumanRepair extends AbstractCBehaviorRepair {
	public CBehaviorHumanRepair(final CUnit unit, final CAbilityHumanRepair ability) {
		super(unit, ability);
	}

	private CAbilityHumanRepair humanAbility() {
		return (CAbilityHumanRepair) getAbility();
	}

	private static boolean isPowerBuildable(final CUnit target) {
		return target.isConstructing() && target.isConstructingPaused();
	}

	@Override
	protected boolean setBuildRates(final CUnit taru) {
		if (!isPowerBuildable(taru)) {
			return false;
		}
		final CAbilityHumanRepair ability = humanAbility();
		this.goldPerUpdate = (taru.getUnitType().getGoldRepairCost() * ability.getPowerBuildCostRatio()
				* WarsmashConstants.SIMULATION_STEP_TIME) / (taru.getUnitType().getBuildTime());
		this.lumberPerUpdate = (taru.getUnitType().getLumberRepairCost() * ability.getPowerBuildCostRatio()
				* WarsmashConstants.SIMULATION_STEP_TIME) / (taru.getUnitType().getBuildTime());
		this.hpPerUpdate = taru.getMaxLife()
				* (WarsmashConstants.SIMULATION_STEP_TIME / taru.getUnitType().getBuildTime())
				* (1.0f - WarsmashConstants.BUILDING_CONSTRUCT_START_LIFE) * ability.getPowerBuildTimeRatio();
		return true;
	}

	@Override
	protected boolean isBuildTarget(final CUnit targetUnit) {
		return (targetUnit != null) && isPowerBuildable(targetUnit);
	}

	@Override
	protected boolean stopsWhenUndamaged() {
		return true;
	}

	@Override
	protected float extraCharge(final CUnit targetUnit, final float perUpdate) {
		// Repairing a destructable (a gate) or a finished unit has no power builders to pay for.
		if (!isBuildTarget(targetUnit)) {
			return 0;
		}
		return perUpdate * humanAbility().getPowerBuildTimeRatio() * targetUnit.getPowerWorkers();
	}

	@Override
	protected CBehavior buildStep(final CSimulation simulation, final CUnit targetUnit) {
		final int buildTime = targetUnit.getUnitType().getBuildTime();
		if (targetUnit.getWorker() == this.unit) {
			targetUnit.setConstructionProgress(targetUnit.getConstructionProgress()
					+ (WarsmashConstants.SIMULATION_STEP_TIME * simulation.getCheatBuildSpeed()));
			final float healthGain = ((WarsmashConstants.SIMULATION_STEP_TIME * simulation.getCheatBuildSpeed())
					/ buildTime) * (targetUnit.getMaximumLife() * (1.0f - WarsmashConstants.BUILDING_CONSTRUCT_START_LIFE));
			targetUnit.setLife(simulation, Math.min(targetUnit.getLife() + healthGain, targetUnit.getMaximumLife()));
		}
		else {
			targetUnit.setConstructionProgress(targetUnit.getConstructionProgress()
					+ (WarsmashConstants.SIMULATION_STEP_TIME * humanAbility().getPowerBuildTimeRatio()
							* simulation.getCheatBuildSpeed()));
			targetUnit.setLife(simulation, Math.min(
					targetUnit.getLife() + (this.hpPerUpdate * simulation.getCheatBuildSpeed()),
					targetUnit.getMaximumLife()));
		}
		if (targetUnit.getConstructionProgress() >= buildTime) {
			return this.unit.pollNextOrderBehavior(simulation);
		}
		return this;
	}

	@Override
	public void begin(final CSimulation game) {
		if (this.target instanceof CUnit) {
			final CUnit taru = (CUnit) this.target;
			if (isPowerBuildable(taru)) {
				if (taru.getWorker() == null) {
					taru.setWorker(this.unit, false);
				}
				else {
					taru.addPowerWorker();
				}
			}
		}
	}

	@Override
	public void end(final CSimulation game, final boolean interrupted) {
		if (this.target instanceof CUnit) {
			final CUnit taru = (CUnit) this.target;
			if (isPowerBuildable(taru)) {
				if (taru.getWorker() == this.unit) {
					taru.setWorker(null, false);
				}
				else {
					taru.removePowerWorker();
				}
			}
		}
	}
}
