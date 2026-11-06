package com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors;

import java.util.List;

import com.badlogic.gdx.math.Rectangle;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens.PrimaryTag;
import com.etheller.warsmash.viewer5.handlers.w3x.SequenceUtils;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid.MovementType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWorldCollision;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.harvest.CBehaviorWispHarvest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrder;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrderTargetWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.pathing.CPathPoint;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.pathing.CPathfindingProcessor;

public class CBehaviorMove implements CBehavior {
	private static boolean ALWAYS_INTERRUPT_MOVE = false;
	/**
	 * Measured on 1.29: a unit turns its heading by turnRate radians every 0.029 s and walks once the
	 * heading is inside the propulsion window, straight toward its next point; the drawn facing turns slower and on its
	 * own.
	 */
	private static final float ORIGINAL_TURN_PERIOD = 0.029f;
	/** Ticks a unit waits behind walking units before it searches a way around them. */
	private static final int WAIT_FOR_WALKERS_TICKS = 10;
	private static final float MAX_DETOUR_AROUND_UNITS = 320f;
	private static final float GIVE_UP_NEAR_TARGET = 384f;
	private static final int START_TICKS = 4;

	private static final Rectangle tempRect = new Rectangle();
	private final CUnit unit;
	private int highlightOrderId;
	private final TargetVisitingResetter targetVisitingResetter;

	public CBehaviorMove(final CUnit unit) {
		this.unit = unit;
		this.targetVisitingResetter = new TargetVisitingResetter();
	}

	private boolean wasWithinPropWindow = false;
	private List<CPathPoint> path = null;
	private CPathfindingProcessor.GridMapping gridMapping;
	private CPathPoint target;
	private int searchCycles = 0;
	private CUnit followUnit;
	private CRangedBehavior rangedBehavior;
	private boolean firstUpdate = true;
	private boolean disableCollision = false;
	private boolean pathfindingActive = false;
	private boolean firstPathfindJob = false;
	private boolean pathfindingFailedGiveUp;
	private int giveUpUntilTurnTick;
	private int prevSpeed = 0;
	private boolean pathReachesGoal = true;
	private boolean followSearch = false;
	private int waitForWalkersTicks = 0;
	private boolean searchAfterBlock = false;
	private double pathLengthWhenBlocked;
	private double pathStartDistance;
	private CGroupMove group;

	public CBehaviorMove reset(final int highlightOrderId, final AbilityTarget target) {
		if (!continuesSameMove(highlightOrderId, target)) {
			target.visit(this.targetVisitingResetter.reset(highlightOrderId));
		}
		this.rangedBehavior = null;
		this.group = null;
		resetDisableCollision(false);
		return this;
	}

	/**
	 * Measured on 1.29: a unit given the same move order again and again (a player clicking on, a script repeating its
	 * order) walks on along its way. Here a long path search spans many ticks, so starting it over threw away the path
	 * and walked the unit straight at the target, into the nearest pocket, at every repeat.
	 */
	private boolean continuesSameMove(final int highlightOrderId, final AbilityTarget target) {
		if (!(target instanceof AbilityPointTarget) || (this.unit.getCurrentBehavior() != this)
				|| (this.rangedBehavior != null) || (this.followUnit != null) || (this.target == null)
				|| (highlightOrderId != this.highlightOrderId) || this.pathfindingFailedGiveUp) {
			return false;
		}
		return isUnderwayTo((AbilityPointTarget) target);
	}

	/**
	 * Whether an attack-move given again to the same point can walk on along this move (see continuesSameMove).
	 */
	public boolean continuesAttackMove(final CBehaviorAttackMove attackMove, final AbilityPointTarget point) {
		return (this.rangedBehavior == attackMove) && (this.followUnit == null) && (this.target != null)
				&& !this.pathfindingFailedGiveUp && isUnderwayTo(point);
	}

	private boolean isUnderwayTo(final AbilityPointTarget point) {
		if ((point.x != this.target.x) || (point.y != this.target.y)) {
			return false;
		}
		return this.pathfindingActive || ((this.path != null) && !this.path.isEmpty() && this.pathReachesGoal);
	}

	public CBehaviorMove reset(final AbilityTarget target, final CRangedBehavior rangedBehavior,
			final boolean disableCollision) {
		final int highlightOrderId = rangedBehavior.getHighlightOrderId();
		target.visit(this.targetVisitingResetter.reset(highlightOrderId));
		this.rangedBehavior = rangedBehavior;
		this.group = null;
		resetDisableCollision(disableCollision);
		return this;
	}

	public CGroupMove getGroup() {
		return this.group;
	}

	public void setGroup(final CGroupMove group) {
		this.group = group;
	}

	private void resetDisableCollision(final boolean disableCollision) {
		final boolean previousCollisionState = this.disableCollision;
		this.disableCollision = disableCollision;
		if ((previousCollisionState != this.disableCollision) && (this.unit.getCurrentBehavior() == this)) {
			// begin() / end() are sometimes not called on behavior when it resets
			// while it was already the active behavior. Until that design is fixed,
			// need to ensure we can't leave unit stuck in "no collision" mode here
			this.unit.setNoCollisionMovementType(this.disableCollision);
		}
	}

	private void internalResetMove(final int highlightOrderId, final float targetX, final float targetY) {
		this.highlightOrderId = highlightOrderId;
		this.wasWithinPropWindow = false;
		this.gridMapping = CPathfindingProcessor.isCollisionSizeBetterSuitedForCorners(
				this.unit.getUnitType().getCollisionSize()) ? CPathfindingProcessor.GridMapping.CORNERS
						: CPathfindingProcessor.GridMapping.CELLS;
		this.target = new CPathPoint(targetX, targetY);
		this.path = null;
		this.searchCycles = 0;
		this.followUnit = null;
		this.firstUpdate = true;
		this.pathfindingFailedGiveUp = false;
		this.giveUpUntilTurnTick = 0;
		this.followSearch = false;
		this.waitForWalkersTicks = 0;
		this.searchAfterBlock = false;
	}

	private void internalResetMove(final int highlightOrderId, final CUnit followUnit) {
		this.highlightOrderId = highlightOrderId;
		this.wasWithinPropWindow = false;
		this.gridMapping = CPathfindingProcessor.isCollisionSizeBetterSuitedForCorners(
				this.unit.getUnitType().getCollisionSize()) ? CPathfindingProcessor.GridMapping.CORNERS
						: CPathfindingProcessor.GridMapping.CELLS;
		this.target = new CPathPoint(followUnit.getX(), followUnit.getY());
		this.path = null;
		this.searchCycles = 0;
		this.followUnit = followUnit;
		this.firstUpdate = true;
		this.pathfindingFailedGiveUp = false;
		this.giveUpUntilTurnTick = 0;
		this.followSearch = false;
		this.waitForWalkersTicks = 0;
		this.searchAfterBlock = false;
	}

	@Override
	public int getHighlightOrderId() {
		return this.highlightOrderId;
	}

	@Override
	public CBehavior update(final CSimulation simulation) {
		if ((this.rangedBehavior != null) && (this.rangedBehavior.isWithinRange(simulation)
				|| isUnorderedChaseOfLostTarget(simulation))) {
			if (this.pathfindingActive) {
				simulation.removeFromPathfindingQueue(this);
				this.pathfindingActive = false;
			}
			return this.unit.finishMoveBehavior(simulation, this.rangedBehavior);
		}
		if (this.firstUpdate) {
			// when units start moving, if they're on top of other units, maybe push them to
			// the side
			this.unit.setPointAndCheckUnstuck(this.unit.getX(), this.unit.getY(), simulation);
			this.firstUpdate = false;
			// a unit about to walk off is no obstacle for the paths of the others ordered with it
			this.unit.markWalked(simulation.getGameTurnTick() + START_TICKS);
		}
		if (this.pathfindingFailedGiveUp) {
			onMoveGiveUp(simulation);
			return this.unit.pollNextOrderBehavior(simulation);
		}
		final float prevX = this.unit.getX();
		final float prevY = this.unit.getY();

		MovementType movementType = this.unit.getMovementType();
		if (movementType == null) {
			movementType = MovementType.DISABLED;
		}
		else if ((movementType == MovementType.FOOT) && this.disableCollision) {
			movementType = MovementType.FOOT_NO_COLLISION;
		}
		final PathingGrid pathingGrid = simulation.getPathingGrid();
		final CWorldCollision worldCollision = simulation.getWorldCollision();
		final float collisionSize = this.unit.getUnitType().getCollisionSize();
		final float startFloatingX = prevX;
		final float startFloatingY = prevY;
		if (this.path == null) {
			if (!this.pathfindingActive) {
				if (this.followUnit != null) {
					this.target.x = this.followUnit.getX();
					this.target.y = this.followUnit.getY();
				}
				simulation.findNaiveSlowPath(this.unit, this.followUnit, startFloatingX, startFloatingY, this.target,
						movementType, collisionSize, true, this);
				this.pathfindingActive = true;
				this.firstPathfindJob = true;
			}
		}
		else if ((this.followUnit != null) && (this.path.size() > 1) && (this.target.distance(this.followUnit.getX(),
				this.followUnit.getY()) > (0.1 * this.target.distance(this.unit.getX(), this.unit.getY())))) {
			this.target.x = this.followUnit.getX();
			this.target.y = this.followUnit.getY();
			if (this.pathfindingActive) {
				simulation.removeFromPathfindingQueue(this);
			}
			simulation.findNaiveSlowPath(this.unit, this.followUnit, startFloatingX, startFloatingY, this.target,
					movementType, collisionSize, this.searchCycles < 4, this);
			this.pathfindingActive = true;
			this.followSearch = true;
		}
		if ((this.path != null) && this.path.isEmpty() && !this.pathReachesGoal && !this.pathfindingActive) {
			return endOfPartialPath(simulation, movementType, collisionSize);
		}
		float currentTargetX;
		float currentTargetY;
		if ((this.path == null) || this.path.isEmpty()) {
			if (this.followUnit != null) {
				currentTargetX = this.followUnit.getX();
				currentTargetY = this.followUnit.getY();
			}
			else {
				currentTargetX = this.target.x;
				currentTargetY = this.target.y;
			}
		}
		else {
			if ((this.followUnit != null) && (this.path.size() == 1)) {
				currentTargetX = this.followUnit.getX();
				currentTargetY = this.followUnit.getY();
			}
			else {
				final CPathPoint nextPathElement = this.path.get(0);
				currentTargetX = nextPathElement.x;
				currentTargetY = nextPathElement.y;
			}
		}

		float deltaX = currentTargetX - prevX;
		float deltaY = currentTargetY - prevY;
		double goalAngleRad = Math.atan2(deltaY, deltaX);
		float goalAngle = (float) Math.toDegrees(goalAngleRad);
		if (goalAngle < 0) {
			goalAngle += 360;
		}
		float facing = this.unit.getFacing();
		float delta = goalAngle - facing;
		final float propulsionWindow = this.unit.getPropWindow();
		final float turnRate = this.unit.getTurnRate();
		final int speed = this.group == null ? this.unit.getSpeed()
				: this.group.limitSpeed(this.unit, this.unit.getSpeed());
		final boolean speedChange = speed != prevSpeed;
		this.prevSpeed = speed;

		if (delta < -180) {
			delta = 360 + delta;
		}
		if (delta > 180) {
			delta = -360 + delta;
		}
		float absDelta = Math.abs(delta);
		final float turnStep = (float) Math.toDegrees(turnRate)
				* (WarsmashConstants.SIMULATION_STEP_TIME / ORIGINAL_TURN_PERIOD);
		// part of this tick left for walking once the heading comes inside the window
		final float walkFraction;
		if (absDelta < propulsionWindow) {
			walkFraction = 1;
		}
		else if ((turnStep > 0) && ((absDelta - turnStep) < propulsionWindow)) {
			walkFraction = 1 - ((absDelta - propulsionWindow) / turnStep);
		}
		else {
			walkFraction = 0;
		}

		if ((absDelta <= Math.max(1.0f, turnStep)) && (absDelta != 0)) {
			this.unit.setFacing(goalAngle);
		}
		else if (absDelta != 0) {
			facing += Math.signum(delta) * turnStep;
			this.unit.setFacing(facing);
		}
		final boolean blockedByGiveUpUntilTickDelay = simulation.getGameTurnTick() < this.giveUpUntilTurnTick;
		// While a first path or a new path toward a moving target is searched, the unit keeps walking toward the target
		// or along its old path (1.29 starts walking at once; the search takes a tick).
		final boolean walkWhileSearching = this.pathfindingActive
				&& ((this.path == null) ? this.firstPathfindJob : (this.followSearch && !this.path.isEmpty()));
		if (!blockedByGiveUpUntilTickDelay && ((this.path != null) || walkWhileSearching)
				&& (!this.pathfindingActive || walkWhileSearching) && (walkFraction > 0)) {
			final float speedTick = speed * WarsmashConstants.SIMULATION_STEP_TIME * walkFraction;
			double continueDistance = speedTick;
			float walkX = prevX;
			float walkY = prevY;
			do {
				boolean done;
				float nextX, nextY;
				deltaX = currentTargetX - walkX;
				deltaY = currentTargetY - walkY;
				final double travelDistance = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
				if (travelDistance <= continueDistance) {
					nextX = currentTargetX;
					nextY = currentTargetY;
					continueDistance = continueDistance - travelDistance;
					done = true;
				}
				else {
					nextX = walkX + (float) ((deltaX / travelDistance) * continueDistance);
					nextY = walkY + (float) ((deltaY / travelDistance) * continueDistance);
					continueDistance = 0;
//					done = (this.gridMapping.getX(pathingGrid, nextX) == this.gridMapping.getX(pathingGrid,
//							currentTargetX))
//							&& (this.gridMapping.getY(pathingGrid, nextY) == this.gridMapping.getY(pathingGrid,
//									currentTargetY));
					done = false;
				}
				if (this.unit.getCollisionRectangle() != null) {
					tempRect.set(this.unit.getCollisionRectangle());
				}
				else {
					tempRect.set(nextX, nextY, 0, 0);
				}
				tempRect.setCenter(nextX, nextY);
				if ((movementType == null) || (pathingGrid.isPathableFrom(walkX, walkY, nextX, nextY, movementType,
						collisionSize)
						&& !worldCollision.intersectsAnythingOtherThan(tempRect, this.unit, movementType, false))) {
					this.unit.setPoint(nextX, nextY, worldCollision, simulation.getRegionManager());
					this.unit.markWalked(simulation.getGameTurnTick());
					this.waitForWalkersTicks = 0;
					walkX = nextX;
					walkY = nextY;
					if (done) {
						// if we're making headway along the path then it's OK to start thinking fast
						// again
						if (travelDistance > 0) {
							this.searchCycles = 0;
						}
						if (this.path == null) {
							// reached the target before the first search finished
							simulation.removeFromPathfindingQueue(this);
							this.pathfindingActive = false;
							onMoveGiveUp(simulation);
							return this.unit.pollNextOrderBehavior(simulation);
						}
						if (this.path.isEmpty()) {
							if (!this.pathReachesGoal) {
								return endOfPartialPath(simulation, movementType, collisionSize);
							}
							onMoveGiveUp(simulation);
							return this.unit.pollNextOrderBehavior(simulation);
						}
						else {
							this.path.remove(0);
							final boolean emptyPath = this.path.isEmpty();
							if (emptyPath && !this.pathReachesGoal) {
								return endOfPartialPath(simulation, movementType, collisionSize);
							}
							if (emptyPath) {
								if (this.followUnit != null) {
									currentTargetX = this.followUnit.getX();
									currentTargetY = this.followUnit.getY();
								}
								else {
									currentTargetX = this.target.x;
									currentTargetY = this.target.y;
								}
							}
							else {
								if ((this.followUnit != null) && (this.path.size() == 1)) {
									currentTargetX = this.followUnit.getX();
									currentTargetY = this.followUnit.getY();
								}
								else {
									final CPathPoint firstPathElement = this.path.get(0);
									currentTargetX = firstPathElement.x;
									currentTargetY = firstPathElement.y;
								}
							}
							deltaY = currentTargetY - nextY;
							deltaX = currentTargetX - nextX;
							if ((deltaX == 0.000f) && (deltaY == 0.000f) && this.path.isEmpty()) {
								onMoveGiveUp(simulation);
								return this.unit.pollNextOrderBehavior(simulation);
							}
							goalAngleRad = Math.atan2(deltaY, deltaX);
							goalAngle = (float) Math.toDegrees(goalAngleRad);
							if (goalAngle < 0) {
								goalAngle += 360;
							}
							facing = this.unit.getFacing();
							delta = goalAngle - facing;

							if (delta < -180) {
								delta = 360 + delta;
							}
							if (delta > 180) {
								delta = -360 + delta;
							}
							absDelta = Math.abs(delta);
							if (absDelta >= propulsionWindow) {
								if (this.wasWithinPropWindow) {
									playStandAnimation();
								}
								this.wasWithinPropWindow = false;
								return this;
							}
						}
					}
				}
				else {
					// As in 1.29, a unit stopped by group-mates walking across its way waits for them instead of
					// searching a way around units that will have moved on.
					if ((this.waitForWalkersTicks < WAIT_FOR_WALKERS_TICKS)
							&& pathingGrid.isPathable(nextX, nextY, movementType, collisionSize)
							&& !worldCollision.intersectsStandingUnitOtherThan(tempRect, this.unit, null, movementType,
									simulation.getGameTurnTick())) {
						this.waitForWalkersTicks++;
						break;
					}
					this.waitForWalkersTicks = 0;
					if (this.followUnit != null) {
						this.target.x = this.followUnit.getX();
						this.target.y = this.followUnit.getY();
					}
					// A point taken by other units is never reached; arriving next to it ends the move, otherwise
					// the unit searches a path every tick and starves the pathfinding queue of other units.
					else if (((this.rangedBehavior == null) || (this.rangedBehavior instanceof CBehaviorAttackMove))
							&& (this.unit.distance(this.target.x, this.target.y) <= Math.max(128f, collisionSize * 4))) {
						onMoveGiveUp(simulation);
						return this.unit.pollNextOrderBehavior(simulation);
					}
					if (!this.pathfindingActive) {
						simulation.findNaiveSlowPath(this.unit, this.followUnit, startFloatingX, startFloatingY,
								this.target, movementType, collisionSize, this.searchCycles < 4, this);
						this.pathfindingActive = true;
						this.searchAfterBlock = true;
						// The way the unit was on is the way around the terrain; only what the units add to it counts as
						// a detour.
						this.pathLengthWhenBlocked = (this.path == null) ? 0 : pathLength(this.path);
						this.searchCycles++;
						return this;
					}
				}
				if (this.unit.isScriptAnimationHeld()) {
					this.unit.getUnitAnimationListener().replayEndedAnimation();
				}
				else if (speed > 0) {
					this.unit.getUnitAnimationListener().playWalkAnimation(speedChange, speed, true);
				}
				else {
					playStandAnimation();
				}
				this.wasWithinPropWindow = true;
			}
			while (continueDistance > 0);
		}
		else {
			// If this happens, the unit is facing the wrong way, and has to turn before
			// moving.
			if (this.wasWithinPropWindow) {
				playStandAnimation();
			}
			this.wasWithinPropWindow = false;
		}

		return this;
	}

	private void playStandAnimation() {
		if (this.unit.isScriptAnimationHeld()) {
			this.unit.getUnitAnimationListener().replayEndedAnimation();
		}
		else {
			this.unit.getUnitAnimationListener().playAnimation(false, PrimaryTag.STAND, SequenceUtils.EMPTY, 1.0f,
					true);
		}
	}

	/**
	 * Measured on 1.29: a unit chasing a target it picked itself (acquired, attacked back)
	 * drops the chase as soon as the target dies or is removed, while one ordered onto the target walks on to where it
	 * was and stops there.
	 */
	private boolean isUnorderedChaseOfLostTarget(final CSimulation simulation) {
		if (!(this.rangedBehavior instanceof CBehaviorAttack)) {
			return false;
		}
		final COrder order = this.unit.getCurrentOrder();
		if (order instanceof COrderTargetWidget) {
			return false;
		}
		return !((CBehaviorAttack) this.rangedBehavior).checkTargetStillValid(simulation);
	}

	private void onMoveGiveUp(final CSimulation simulation) {
		// An attack-move whose point cannot be reached ends where the walk ended (1.29), instead of starting again
		// as the unit's default behaviour.
		if (this.rangedBehavior instanceof CBehaviorAttackMove) {
			this.unit.setDefaultBehavior(this.unit.getStopBehavior());
		}
		if (this.rangedBehavior != null) {
			this.rangedBehavior.endMove(simulation, true);
		}
	}

	private final class TargetVisitingResetter implements AbilityTargetVisitor<Void> {
		private int highlightOrderId;

		private TargetVisitingResetter reset(final int highlightOrderId) {
			this.highlightOrderId = highlightOrderId;
			return this;
		}

		@Override
		public Void accept(final AbilityPointTarget target) {
			internalResetMove(this.highlightOrderId, target.x, target.y);
			return null;
		}

		@Override
		public Void accept(final CUnit target) {
			internalResetMove(this.highlightOrderId, target);
			return null;
		}

		@Override
		public Void accept(final CDestructable target) {
			internalResetMove(this.highlightOrderId, target.getX(), target.getY());
			return null;
		}

		@Override
		public Void accept(final CItem target) {
			internalResetMove(this.highlightOrderId, target.getX(), target.getY());
			return null;
		}
	}

	@Override
	public void begin(final CSimulation game) {
		if (this.disableCollision) {
			this.unit.setNoCollisionMovementType(true);
		}
	}

	@Override
	public void end(final CSimulation game, final boolean interrupted) {
		if (this.disableCollision) {
			this.unit.setNoCollisionMovementType(false);
		}
		if (ALWAYS_INTERRUPT_MOVE) {
			game.removeFromPathfindingQueue(this);
			this.pathfindingActive = false;
		}
		if (this.rangedBehavior != null) {
			this.rangedBehavior.endMove(game, interrupted);
		}
	}

	public CUnit getUnit() {
		return this.unit;
	}

	public CRangedBehavior getRangedBehavior() {
		return this.rangedBehavior;
	}

	/**
	 * A search started for an earlier target (the order was replaced while it ran) brings nothing: the unit asks for
	 * a path to its current target on its next update. Measured on 1.29: a new move order given right after a long
	 * one goes straight to the new point.
	 */
	public void pathFound(final CPathPoint searchedGoal, final List<CPathPoint> waypoints, final boolean reachesGoal,
			final CSimulation simulation) {
		if (searchedGoal != this.target) {
			this.pathfindingActive = false;
			this.followSearch = false;
			return;
		}
		pathFound(waypoints, reachesGoal, simulation);
	}

	public void pathFound(final List<CPathPoint> waypoints, final boolean reachesGoal,
			final CSimulation simulation) {
		this.pathfindingActive = false;
		this.followSearch = false;
		this.path = waypoints;
		this.pathReachesGoal = reachesGoal;
		final boolean searchedAfterBlock = this.searchAfterBlock;
		this.searchAfterBlock = false;
		if (searchedAfterBlock && isPlainMove()
				&& (this.unit.distance(this.target.x, this.target.y) <= GIVE_UP_NEAR_TARGET) && !waypoints.isEmpty()
				&& (pathLength(waypoints) > ((2 * Math.max(this.pathLengthWhenBlocked,
						waypoints.get(waypoints.size() - 1).distance(this.unit.getX(), this.unit.getY())))
						+ MAX_DETOUR_AROUND_UNITS))) {
			// As in 1.29, a unit stopped by standing units on its way to a taken point stops near it instead of
			// walking far around them.
			this.pathfindingFailedGiveUp = true;
		}
		this.pathStartDistance = this.unit.distance(this.target.x, this.target.y);
		if (this.firstPathfindJob) {
			this.firstPathfindJob = false;
		}
		else if (this.searchCycles > 6) {
			if (this.searchCycles > 9) {
				this.pathfindingFailedGiveUp = true;
			}
			else {
				this.giveUpUntilTurnTick = simulation.getGameTurnTick()
						+ (int) (5 / WarsmashConstants.SIMULATION_STEP_TIME);
			}
		}
	}

	private boolean isPlainMove() {
		return (this.followUnit == null)
				&& ((this.rangedBehavior == null) || (this.rangedBehavior instanceof CBehaviorAttackMove));
	}

	private double pathLength(final List<CPathPoint> waypoints) {
		double length = 0;
		float x = this.unit.getX();
		float y = this.unit.getY();
		for (final CPathPoint point : waypoints) {
			length += point.distance(x, y);
			x = point.x;
			y = point.y;
		}
		return length;
	}

	/**
	 * Measured: a unit whose target cannot be reached walks to the reachable point nearest to it and stops. A search
	 * cut short by its iteration limit also ends at such a point, so the unit searches again from there while that
	 * brought it closer. A unit on its way to act on something (attack, harvest, build) heads on toward it instead,
	 * since its own range decides when it has arrived.
	 */
	private CBehavior endOfPartialPath(final CSimulation simulation, final MovementType movementType,
			final float collisionSize) {
		final boolean plainMove = (this.rangedBehavior == null)
				|| (this.rangedBehavior instanceof CBehaviorAttackMove);
		if (!plainMove) {
			if (this.rangedBehavior instanceof CBehaviorWispHarvest) {
				final CBehavior next = ((CBehaviorWispHarvest) this.rangedBehavior).onPathEnd(simulation);
				if (next != null) {
					return next;
				}
			}
			this.pathReachesGoal = true;
			return this;
		}
		final double distance = this.unit.distance(this.target.x, this.target.y);
		if ((distance > (this.pathStartDistance - 64)) || (this.searchCycles > 9)) {
			onMoveGiveUp(simulation);
			return this.unit.pollNextOrderBehavior(simulation);
		}
		if (this.followUnit != null) {
			this.target.x = this.followUnit.getX();
			this.target.y = this.followUnit.getY();
		}
		simulation.findNaiveSlowPath(this.unit, this.followUnit, this.unit.getX(), this.unit.getY(), this.target,
				movementType, collisionSize, true, this);
		this.pathfindingActive = true;
		this.searchCycles++;
		return this;
	}

	public boolean isPathfindingActive() {
		return this.pathfindingActive;
	}

	@Override
	public boolean interruptable() {
		return true;
	}

	@Override
	public <T> T visit(final CBehaviorVisitor<T> visitor) {
		return visitor.accept(this);
	}

	@Override
	public CBehaviorCategory getBehaviorCategory() {
		return CBehaviorCategory.MOVEMENT;
	}
	
	public AbilityTarget getTarget() {
		if (this.rangedBehavior != null) {
			return this.rangedBehavior.getTarget();
		} else if (this.followUnit != null) {
			return this.followUnit;
		}
		return new AbilityPointTarget(this.target.x, this.target.y);
	}
}
