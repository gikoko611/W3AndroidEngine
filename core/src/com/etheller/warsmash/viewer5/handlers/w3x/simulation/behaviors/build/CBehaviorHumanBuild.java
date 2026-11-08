package com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.build;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.build.CAbilityBuildInProgress;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.build.CAbilityHumanRepair;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public class CBehaviorHumanBuild extends AbstractCBehaviorBuild {
	private boolean unitCreated = false;

	public CBehaviorHumanBuild(final CUnit unit) {
		super(unit);
	}

	@Override
	protected void resetStructure() {
		this.unitCreated = false;
	}

	@Override
	protected CBehavior update(final CSimulation simulation, final boolean withinFacingWindow) {
		if (!this.unitCreated) {
			this.unitCreated = true;
			final CUnit constructedStructure = startStructure(simulation);
			if (constructedStructure != null) {
				for (final CAbility ability : this.unit.getAbilities()) {
					if (ability instanceof CAbilityHumanRepair) {
						final int baseOrderId = ((CAbilityHumanRepair) ability).getBaseOrderId();
						ability.checkCanUse(simulation, this.unit, this.unit.getPlayerIndex(), baseOrderId, false,
								BooleanAbilityActivationReceiver.INSTANCE.reset());
						if (BooleanAbilityActivationReceiver.INSTANCE.isOk()) {
							final BooleanAbilityTargetCheckReceiver<CWidget> targetCheckReceiver = BooleanAbilityTargetCheckReceiver
									.getInstance();
							ability.checkCanTarget(simulation, this.unit, this.unit.getPlayerIndex(), baseOrderId,
									false, constructedStructure, targetCheckReceiver.reset());
							if (targetCheckReceiver.isTargetable()) {
								return ability.begin(simulation, this.unit, this.unit.getPlayerIndex(), baseOrderId,
										false, constructedStructure);
							}
						}
					}
				}
			}
		}
		return this.unit.pollNextOrderBehavior(simulation);
	}

	@Override
	protected void assignWorker(final CUnit constructedStructure) {
		constructedStructure.setConstructingPaused(true);
	}

	@Override
	protected void onStructureStarted(final CSimulation simulation, final CUnit constructedStructure,
			final CUnitType unitTypeToCreate, final CAbilityBuildInProgress abilityBuildInProgress) {
		stepAside(simulation, unitTypeToCreate);
	}
}
