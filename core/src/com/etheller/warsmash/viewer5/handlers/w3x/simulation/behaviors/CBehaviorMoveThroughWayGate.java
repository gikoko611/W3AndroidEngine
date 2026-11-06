package com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityRanged;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.neutral.CAbilityWayGate;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorMoveIntoRangeFor.PairAbilityLocator;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;

/**
 * Measured on 1.29: a unit ordered to "move" onto an active Way Gate walks in and is teleported. The order still
 * aims at the gate, so when a gate near the drop point leads back to it the unit walks through that one too, and the
 * order ends there.
 */
public class CBehaviorMoveThroughWayGate extends CBehaviorMoveIntoRangeFor implements PairAbilityLocator {
	private CUnit goalGate;
	private boolean returning;

	public CBehaviorMoveThroughWayGate(final CUnit unit) {
		super(unit);
	}

	public CBehavior reset(final CSimulation game, final CUnit gate) {
		this.goalGate = gate;
		this.returning = false;
		return super.reset(game, OrderIds.move, gate, this);
	}

	@Override
	public CAbilityRanged getPartnerAbility(final CSimulation game, final CUnit caster, final CUnit transport,
			final boolean ignoreRange, final boolean ignoreDisabled) {
		return CAbilityWayGate.getWayGateAbility(game, caster, transport, ignoreRange, ignoreDisabled);
	}

	@Override
	protected CBehavior update(final CSimulation simulation, final boolean withinFacingWindow) {
		final CUnit gate = this.target.visit(AbilityTargetVisitor.UNIT);
		if (gate != null) {
			final CAbilityWayGate wayGate = CAbilityWayGate.getWayGateAbility(simulation, this.unit, gate, false,
					false);
			if ((wayGate != null) && wayGate.teleport(simulation, gate, this.unit) && !this.returning) {
				final CUnit returnGate = findReturnGate(simulation);
				if (returnGate != null) {
					this.returning = true;
					return innerReset(simulation, returnGate);
				}
			}
		}
		return this.unit.pollNextOrderBehavior(simulation);
	}

	private CUnit findReturnGate(final CSimulation simulation) {
		double best = distance(this.unit.getX(), this.unit.getY(), this.goalGate.getX(), this.goalGate.getY());
		CUnit bestGate = null;
		for (final CUnit candidate : simulation.getUnits()) {
			if ((candidate == this.goalGate) || candidate.isDead()) {
				continue;
			}
			final CAbilityWayGate wayGate = CAbilityWayGate.getWayGateAbility(simulation, this.unit, candidate, true,
					false);
			if (wayGate == null) {
				continue;
			}
			final AbilityPointTarget destination = wayGate.getDestination();
			if (destination == null) {
				continue;
			}
			final double viaGate = distance(this.unit.getX(), this.unit.getY(), candidate.getX(), candidate.getY())
					+ distance(destination.x, destination.y, this.goalGate.getX(), this.goalGate.getY());
			if (viaGate < best) {
				best = viaGate;
				bestGate = candidate;
			}
		}
		return bestGate;
	}

	private static double distance(final float x1, final float y1, final float x2, final float y2) {
		final double dx = x2 - x1;
		final double dy = y2 - y1;
		return StrictMath.sqrt((dx * dx) + (dy * dy));
	}
}
