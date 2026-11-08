package com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.cargohold;

import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens.PrimaryTag;
import com.etheller.warsmash.viewer5.handlers.w3x.SequenceUtils;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold.CAbilityCargoHold;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold.CAbilityLoad;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetStillAliveAndTargetableVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CRangedBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrder;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;

/**
 * A transport taking a unit aboard. Measured on 1.29 (transport ship and footman): the transport and the unit get
 * paired orders, load on the transport and board on the unit, whichever of them was ordered; the transport heads
 * for the point it can reach nearest to the unit and stays put when it is already there (a ship by the shore does
 * not move for a footman walking up); the unit goes aboard once it is within the cargo hold range (edge to edge) of
 * the transport. Ordering either of them elsewhere ends the other's order too.
 */
public class CBehaviorLoad implements CRangedBehavior {
	// The transport heads off again when the unit it waits for has walked this far.
	private static final float FOLLOW_AGAIN_DISTANCE = 128f;
	// The ship heads a little past the point where it stops, so that it does not slow down before it.
	private static final float APPROACH_OVERSHOOT = 32f;

	private final CUnit unit;
	private final CAbilityLoad ability;
	private final AbilityTargetStillAliveAndTargetableVisitor stillAliveVisitor;
	private CUnit target;
	private AbilityPointTarget meetingPoint;
	private float shoreX;
	private float shoreY;
	private double targetToShore;

	public CBehaviorLoad(final CUnit unit, final CAbilityLoad ability) {
		this.unit = unit;
		this.ability = ability;
		this.stillAliveVisitor = new AbilityTargetStillAliveAndTargetableVisitor();
	}

	public CBehavior reset(final CSimulation game, final CWidget target) {
		this.target = (CUnit) target;
		this.meetingPoint = null;
		if (isWithinCargoRange()) {
			return this;
		}
		if (!this.ability.isMovingTransport()) {
			// A transport that cannot move (Entangled Gold Mine) only takes in units already at it.
			return this.unit.pollNextOrderBehavior(game);
		}
		CBehaviorBoard.pair(game, this.target, this.unit, this.ability);
		return plan(game);
	}

	private CBehavior plan(final CSimulation game) {
		final float castRange = this.unit.getCargoData().getCastRange();
		// Measured on 1.29: the footman heads for the ground near water the ship can sail to that is nearest the
		// ship, looking around itself only; from 8000 away the ship then sails round the map to it.
		final float[] shore = CargoReach.nearestShore(game, this.target, this.unit, castRange);
		this.shoreX = shore[0];
		this.shoreY = shore[1];
		this.targetToShore = StrictMath.hypot(this.target.getX() - shore[0], this.target.getY() - shore[1]);
		if ((this.unit.distance(shore[0], shore[1])
				- this.target.getUnitType().getCollisionSize()) <= castRange) {
			// Measured on 1.29: a ship the footman can walk into range of stays where it is.
			this.meetingPoint = new AbilityPointTarget(this.unit.getX(), this.unit.getY());
			return this;
		}
		// Measured on 1.29: a ship far out heads for that ground and stops once it is within the cargo hold range of
		// the ship's centre, not as near the shore as the ship could go.
		final double toShore = StrictMath.hypot(this.unit.getX() - shore[0], this.unit.getY() - shore[1]);
		final float approach = castRange - APPROACH_OVERSHOOT;
		final float goalX = (float) (shore[0] + (((this.unit.getX() - shore[0]) * approach) / toShore));
		final float goalY = (float) (shore[1] + (((this.unit.getY() - shore[1]) * approach) / toShore));
		final float[] nearest = CargoReach.nearestReachable(game, this.unit, goalX, goalY);
		this.meetingPoint = new AbilityPointTarget(nearest[0], nearest[1]);
		if (isAtMeetingPoint()) {
			return this;
		}
		return this.unit.getMoveBehavior().reset(this.meetingPoint, this, false);
	}

	public AbilityPointTarget getMeetingPoint() {
		return this.meetingPoint;
	}

	private boolean isAtMeetingPoint() {
		if (this.meetingPoint == null) {
			return true;
		}
		if (StrictMath.hypot(this.unit.getX() - this.shoreX, this.unit.getY() - this.shoreY) <= this.unit
				.getCargoData().getCastRange()) {
			return true;
		}
		// Within its own collision size of the point.
		return this.unit.distance(this.meetingPoint.x, this.meetingPoint.y) <= 0;
	}

	private boolean isWithinCargoRange() {
		final CAbilityCargoHold cargoData = this.unit.getCargoData();
		return (cargoData != null) && this.unit.canReach(this.target, cargoData.getCastRange());
	}

	private boolean targetMovedOn() {
		// Measured on 1.29: the meeting point stays while the unit walks to it, though nearer ground would be
		// found from where it walked to.
		return this.ability.isMovingTransport() && (StrictMath.hypot(this.target.getX() - this.shoreX,
				this.target.getY() - this.shoreY) > (this.targetToShore + FOLLOW_AGAIN_DISTANCE));
	}

	@Override
	public boolean isWithinRange(final CSimulation simulation) {
		return !checkTargetStillValid(simulation) || isWithinCargoRange() || isAtMeetingPoint() || targetMovedOn();
	}

	@Override
	public CBehavior update(final CSimulation simulation) {
		if (!checkTargetStillValid(simulation)) {
			return this.unit.pollNextOrderBehavior(simulation);
		}
		if (isWithinCargoRange()) {
			CAbilityLoad.loadUnit(simulation, this.unit, this.target, false);
			return this.unit.pollNextOrderBehavior(simulation);
		}
		if (targetMovedOn() || ((this.meetingPoint != null) && !isAtMeetingPoint())) {
			return plan(simulation);
		}
		this.unit.getUnitAnimationListener().playAnimation(false, PrimaryTag.STAND, SequenceUtils.EMPTY, 1.0f, false);
		return this;
	}

	private boolean checkTargetStillValid(final CSimulation simulation) {
		if (this.target.isHidden() || this.target.isDead()) {
			return false;
		}
		return this.target
				.visit(this.stillAliveVisitor.reset(simulation, this.unit, this.unit.getCargoData().getTargetsAllowed()));
	}

	public CUnit getTargetUnit() {
		return this.target;
	}

	@Override
	public AbilityTarget getTarget() {
		return this.target;
	}

	@Override
	public void begin(final CSimulation game) {
	}

	@Override
	public void end(final CSimulation game, final boolean interrupted) {
		onInterrupted(game, interrupted);
	}

	@Override
	public void endMove(final CSimulation game, final boolean interrupted) {
		onInterrupted(game, interrupted);
	}

	private void onInterrupted(final CSimulation game, final boolean interrupted) {
		if (!interrupted || (this.target == null)) {
			return;
		}
		final COrder order = this.unit.getCurrentOrder();
		if ((order != null) && (order.getOrderId() == OrderIds.load)) {
			return;
		}
		// Measured on 1.29: a transport ordered elsewhere (hold position) leaves the unit standing too.
		CBehaviorBoard.unpair(game, this.target, this.unit);
	}

	@Override
	public int getHighlightOrderId() {
		return OrderIds.load;
	}

	@Override
	public boolean interruptable() {
		return true;
	}

	@Override
	public CBehaviorCategory getBehaviorCategory() {
		return CBehaviorCategory.SPELL;
	}

	@Override
	public <T> T visit(final CBehaviorVisitor<T> visitor) {
		return visitor.accept(this);
	}
}
