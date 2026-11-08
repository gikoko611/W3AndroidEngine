package com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.cargohold;

import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens.PrimaryTag;
import com.etheller.warsmash.viewer5.handlers.w3x.SequenceUtils;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityMove;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold.CAbilityCargoHold;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold.CAbilityLoad;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetStillAliveVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorMove;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CRangedBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrder;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrderTargetWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

/**
 * A unit going aboard a transport that moves (ship, zeppelin). Measured on 1.29 with a transport ship and a
 * footman: while the transport is on its way, the footman walks only up to 512 from the point it can reach nearest
 * to where the transport is heading and waits there; once the transport stands still, the footman walks up to it
 * and goes aboard within the cargo hold range (edge to edge). A footman already that near does not move until the
 * transport stops. Ordering either of them elsewhere ends the other's order too.
 */
public class CBehaviorBoard implements CRangedBehavior {
	private static final float WAIT_DISTANCE = 512f;
	// The unit heads off again when the transport it waits for has moved this far.
	private static final float FOLLOW_AGAIN_DISTANCE = 128f;

	private final CUnit unit;
	private CUnit transport;
	private CAbilityLoad loadAbility;
	private int highlightOrderId;
	private AbilityPointTarget walkPoint;
	private float plannedForX;
	private float plannedForY;
	private boolean plannedWhileTransportMoving;

	public CBehaviorBoard(final CUnit unit) {
		this.unit = unit;
	}

	public CBehavior reset(final CSimulation game, final int highlightOrderId, final CUnit transport,
			final CAbilityLoad loadAbility) {
		this.highlightOrderId = highlightOrderId;
		this.transport = transport;
		this.loadAbility = loadAbility;
		this.walkPoint = null;
		pair(game, this.unit, transport, loadAbility);
		if (isWithinCargoRange()) {
			return this;
		}
		return plan(game);
	}

	private boolean isTransportMoving() {
		return this.transport.isMoving();
	}

	private CBehavior plan(final CSimulation game) {
		final boolean transportMoving = isTransportMoving();
		float headingX = this.transport.getX();
		float headingY = this.transport.getY();
		if (transportMoving) {
			final AbilityPointTarget meetingPoint = transportMeetingPoint();
			if (meetingPoint != null) {
				headingX = meetingPoint.x;
				headingY = meetingPoint.y;
			}
		}
		this.plannedForX = headingX;
		this.plannedForY = headingY;
		this.plannedWhileTransportMoving = transportMoving;
		if (this.unit.isMovementDisabled()) {
			this.walkPoint = null;
			return this;
		}
		final float[] nearest = CargoReach.nearestReachable(game, this.unit, headingX, headingY);
		this.walkPoint = new AbilityPointTarget(nearest[0], nearest[1]);
		if (isAtWalkEnd()) {
			return this;
		}
		return this.unit.getMoveBehavior().reset(this.walkPoint, this, false);
	}

	private AbilityPointTarget transportMeetingPoint() {
		final CBehavior behavior = this.transport.getCurrentBehavior();
		CBehavior loading = behavior;
		if (behavior instanceof CBehaviorMove) {
			loading = ((CBehaviorMove) behavior).getRangedBehavior();
		}
		if (loading instanceof CBehaviorLoad) {
			return ((CBehaviorLoad) loading).getMeetingPoint();
		}
		return null;
	}

	private boolean isAtWalkEnd() {
		if (this.walkPoint == null) {
			return true;
		}
		final double distance = StrictMath.hypot(this.unit.getX() - this.walkPoint.x, this.unit.getY() - this.walkPoint.y);
		if (this.plannedWhileTransportMoving) {
			return distance <= WAIT_DISTANCE;
		}
		return this.unit.distance(this.walkPoint.x, this.walkPoint.y) <= 0;
	}

	private boolean isWithinCargoRange() {
		final CAbilityCargoHold cargoData = this.transport.getCargoData();
		return (cargoData != null) && this.transport.canReach(this.unit, cargoData.getCastRange());
	}

	private boolean needsNewPlan() {
		if (this.plannedWhileTransportMoving != isTransportMoving()) {
			return true;
		}
		if (this.plannedWhileTransportMoving) {
			final AbilityPointTarget meetingPoint = transportMeetingPoint();
			return (meetingPoint != null) && (StrictMath.hypot(meetingPoint.x - this.plannedForX,
					meetingPoint.y - this.plannedForY) > FOLLOW_AGAIN_DISTANCE);
		}
		return StrictMath.hypot(this.transport.getX() - this.plannedForX,
				this.transport.getY() - this.plannedForY) > FOLLOW_AGAIN_DISTANCE;
	}

	private boolean checkTargetStillValid(final CSimulation simulation) {
		return !this.transport.isHidden() && this.transport.visit(AbilityTargetStillAliveVisitor.INSTANCE)
				&& (this.transport.getCargoData() != null);
	}

	@Override
	public boolean isWithinRange(final CSimulation simulation) {
		return !checkTargetStillValid(simulation) || isWithinCargoRange() || isAtWalkEnd() || needsNewPlan();
	}

	@Override
	public CBehavior update(final CSimulation simulation) {
		if (!checkTargetStillValid(simulation)) {
			return this.unit.pollNextOrderBehavior(simulation);
		}
		if (isWithinCargoRange()) {
			if (CAbilityLoad.loadUnit(simulation, this.transport, this.unit, true)) {
				return this.unit.pollNextOrderBehavior(simulation);
			}
			return this.unit.pollNextOrderBehavior(simulation);
		}
		if (needsNewPlan() || !isAtWalkEnd()) {
			final CBehavior next = plan(simulation);
			if (next != this) {
				return next;
			}
		}
		this.unit.getUnitAnimationListener().playAnimation(false, PrimaryTag.STAND, SequenceUtils.EMPTY, 1.0f, false);
		return this;
	}

	@Override
	public AbilityTarget getTarget() {
		return this.transport;
	}

	public CUnit getTransport() {
		return this.transport;
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
		if (!interrupted || (this.transport == null) || this.unit.isHidden()) {
			return;
		}
		final COrder order = this.unit.getCurrentOrder();
		if ((order != null) && (order.getTarget(game) == this.transport)
				&& ((order.getOrderId() == OrderIds.board) || (order.getOrderId() == OrderIds.smart))) {
			return;
		}
		final COrder transportOrder = this.transport.getCurrentOrder();
		if ((transportOrder != null) && (transportOrder.getOrderId() == OrderIds.load)
				&& (transportOrder.getTarget(game) == this.unit)) {
			this.transport.order(game, OrderIds.stop, null);
		}
	}

	@Override
	public int getHighlightOrderId() {
		return this.highlightOrderId;
	}

	@Override
	public boolean interruptable() {
		return true;
	}

	@Override
	public CBehaviorCategory getBehaviorCategory() {
		return CBehaviorCategory.MOVEMENT;
	}

	@Override
	public <T> T visit(final CBehaviorVisitor<T> visitor) {
		return visitor.accept(this);
	}

	/**
	 * Gives the unit the board order on the transport and the transport the load order on the unit, unless they have
	 * them already.
	 */
	public static void pair(final CSimulation game, final CUnit passenger, final CUnit transport,
			final CAbilityLoad loadAbility) {
		final COrder transportOrder = transport.getCurrentOrder();
		if ((transportOrder == null) || (transportOrder.getOrderId() != OrderIds.load)
				|| (transportOrder.getTarget(game) != passenger)) {
			transport.order(game, new COrderTargetWidget(transport.getPlayerIndex(), loadAbility.getHandleId(),
					OrderIds.load, passenger.getHandleId(), false), false);
		}
		final COrder passengerOrder = passenger.getCurrentOrder();
		if ((passengerOrder == null) || (passengerOrder.getTarget(game) != transport)
				|| ((passengerOrder.getOrderId() != OrderIds.board)
						&& (passengerOrder.getOrderId() != OrderIds.smart))) {
			final CAbilityMove move = passenger.getFirstAbilityOfType(CAbilityMove.class);
			if (move != null) {
				passenger.order(game, new COrderTargetWidget(passenger.getPlayerIndex(), move.getHandleId(),
						boardOrderId(game, passenger, move, transport), transport.getHandleId(), false), false);
			}
		}
	}

	private static int boardOrderId(final CSimulation game, final CUnit passenger, final CAbility move,
			final CUnit transport) {
		final BooleanAbilityTargetCheckReceiver<com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget> receiver = BooleanAbilityTargetCheckReceiver
				.<com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget>getInstance().reset();
		move.checkCanTarget(game, passenger, passenger.getPlayerIndex(), OrderIds.board, false, transport, receiver);
		return receiver.isTargetable() ? OrderIds.board : OrderIds.smart;
	}

	/** The transport no longer takes the unit aboard: the unit stops boarding it. */
	public static void unpair(final CSimulation game, final CUnit passenger, final CUnit transport) {
		if (passenger.isHidden()) {
			return;
		}
		final COrder order = passenger.getCurrentOrder();
		if ((order != null) && (order.getTarget(game) == transport)
				&& ((order.getOrderId() == OrderIds.board) || (order.getOrderId() == OrderIds.smart))
				&& isBoarding(passenger, transport)) {
			passenger.order(game, OrderIds.stop, null);
		}
	}

	public static boolean isBoarding(final CUnit passenger, final CUnit transport) {
		CBehavior behavior = passenger.getCurrentBehavior();
		if (behavior instanceof CBehaviorMove) {
			behavior = ((CBehaviorMove) behavior).getRangedBehavior();
		}
		return (behavior instanceof CBehaviorBoard) && (((CBehaviorBoard) behavior).getTransport() == transport);
	}
}
