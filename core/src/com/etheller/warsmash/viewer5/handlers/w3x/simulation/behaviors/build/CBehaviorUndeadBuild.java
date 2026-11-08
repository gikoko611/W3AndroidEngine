package com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.build;

import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens.PrimaryTag;
import com.etheller.warsmash.viewer5.handlers.w3x.SequenceUtils;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.build.CAbilityBuildInProgress;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;

public class CBehaviorUndeadBuild extends AbstractCBehaviorBuild {

	private static final int delayAnimationTicks = CTimer.wholeSteps(2.267f);
	private boolean unitCreated = false;
	private int doneTick = 0;

	public CBehaviorUndeadBuild(final CUnit unit) {
		super(unit);
	}

	@Override
	protected void resetStructure() {
		this.unitCreated = false;
		this.doneTick = 0;
	}

	@Override
	public boolean isWithinRange(final CSimulation simulation) {
		if (this.doneTick != 0) {
			return true;
		}
		return super.isWithinRange(simulation);
	}

	@Override
	protected float getRangeWithoutPathing(final CUnitType unitType) {
		return unitType.getCollisionSize();
	}

	@Override
	protected CBehavior update(final CSimulation simulation, final boolean withinFacingWindow) {
		if (this.doneTick != 0) {
			if (simulation.getGameTurnTick() > this.doneTick) {
				return this.unit.pollNextOrderBehavior(simulation);
			}
		}
		else if (!this.unitCreated) {
			this.unitCreated = true;
			if (startStructure(simulation) == null) {
				return this.unit.pollNextOrderBehavior(simulation);
			}
			this.doneTick = simulation.getGameTurnTick() + delayAnimationTicks;
		}
		this.unit.getUnitAnimationListener().playAnimation(false, PrimaryTag.STAND, SequenceUtils.WORK, 1.0f, true);
		return this;
	}

	@Override
	protected void assignWorker(final CUnit constructedStructure) {
		constructedStructure.setWorker(this.unit, false);
	}

	@Override
	protected void onStructureStarted(final CSimulation simulation, final CUnit constructedStructure,
			final CUnitType unitTypeToCreate, final CAbilityBuildInProgress abilityBuildInProgress) {
		stepAside(simulation, unitTypeToCreate);
	}
}
