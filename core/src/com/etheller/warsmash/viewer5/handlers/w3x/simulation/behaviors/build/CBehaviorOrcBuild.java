package com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.build;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.build.CAbilityBuildInProgress;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;

public class CBehaviorOrcBuild extends AbstractCBehaviorBuild {
	private CUnit constructedStructure = null;

	public CBehaviorOrcBuild(final CUnit unit) {
		super(unit);
	}

	@Override
	protected void resetStructure() {
		this.constructedStructure = null;
	}

	@Override
	protected CBehavior update(final CSimulation simulation, final boolean withinFacingWindow) {
		if (this.constructedStructure == null) {
			this.constructedStructure = startStructure(simulation);
			if (this.constructedStructure == null) {
				return this.unit.pollNextOrderBehavior(simulation);
			}
		}
		if (this.constructedStructure.isConstructing()) {
			return this;
		}
		else {
			return this.unit.pollNextOrderBehavior(simulation);
		}
	}

	@Override
	protected void assignWorker(final CUnit constructedStructure) {
		constructedStructure.setWorker(this.unit, true);
	}

	@Override
	protected void onStructureStarted(final CSimulation simulation, final CUnit constructedStructure,
			final CUnitType unitTypeToCreate, final CAbilityBuildInProgress abilityBuildInProgress) {
		this.unit.setHidden(true);
		this.unit.setAcceptingOrders(false);
		this.unit.setInvulnerableSource(simulation, CUnit.INSIDE_INVULNERABLE, true);
		onStructureCreated(simulation, constructedStructure, abilityBuildInProgress);
	}

	protected void onStructureCreated(final CSimulation game, final CUnit constructedStructure,
			final CAbilityBuildInProgress abilityBuildInProgress) {
	}
}
