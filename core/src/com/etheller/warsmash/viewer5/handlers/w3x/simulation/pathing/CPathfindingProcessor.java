package com.etheller.warsmash.viewer5.handlers.w3x.simulation.pathing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;

import com.badlogic.gdx.math.Rectangle;
import com.etheller.warsmash.util.PerfStats;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid.MovementType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWorldCollision;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorMove;

public class CPathfindingProcessor {
	private final Rectangle tempRect = new Rectangle();
	private int gameTurnTick;
	private final PathingGrid pathingGrid;
	private final CWorldCollision worldCollision;
	private final LinkedList<PathfindingJob> moveQueue = new LinkedList<>();
	private final SharedGraph graph;
	// things with modified state per current job:
	private final Node[][] nodes;
	private final Node[][] cornerNodes;
	private final Node[] goalSet = new Node[9];
	private int goals = 0;
	private int pathfindJobId = 0;
	private static final int NEAREST_PATHABLE_RINGS = 32;
	private static final int MIN_SEARCH_ITERATIONS = 4096;
	private int totalIterations = 0;
	private int totalJobLoops = 0;
	private final int pathingGridCellCount;

	public CPathfindingProcessor(final PathingGrid pathingGrid, final CWorldCollision worldCollision,
			final SharedGraph graph) {
		this.pathingGrid = pathingGrid;
		this.worldCollision = worldCollision;
		this.graph = graph;
		this.nodes = graph.nodes;
		this.cornerNodes = graph.cornerNodes;
		this.pathingGridCellCount = pathingGrid.getWidth() * pathingGrid.getHeight();
	}

	/**
	 * One node grid for all players' processors: a grid per player filled during long cross-map searches ran the
	 * device out of memory. A search may pause between ticks with its state in the nodes, so only one processor at a
	 * time holds a started search; the others wait, and the first one that waited goes next.
	 */
	public static final class SharedGraph {
		private final Node[][] nodes;
		private final Node[][] cornerNodes;
		private int lastJobId = 0;
		private CPathfindingProcessor owner;
		private CPathfindingProcessor reservedFor;

		public SharedGraph(final PathingGrid pathingGrid) {
			this.nodes = new Node[pathingGrid.getHeight()][pathingGrid.getWidth()];
			this.cornerNodes = new Node[pathingGrid.getHeight() + 1][pathingGrid.getWidth() + 1];
		}
	}

	private void finishJob() {
		this.moveQueue.poll();
		if (this.graph.owner == this) {
			this.graph.owner = null;
		}
	}

	/**
	 * Nodes are created on first use: a search touches a small part of the map, while a node for every cell
	 * of every processor took most of the simulation's memory.
	 */
	private Node node(final Node[][] searchGraph, final int y, final int x) {
		Node node = searchGraph[y][x];
		if (node == null) {
			node = new Node(searchGraph == this.cornerNodes
					? new CPathPoint(this.pathingGrid.getWorldXFromCorner(x), this.pathingGrid.getWorldYFromCorner(y))
					: new CPathPoint(this.pathingGrid.getWorldX(x), this.pathingGrid.getWorldY(y)));
			searchGraph[y][x] = node;
		}
		return node;
	}

	/**
	 * Finds the path to a point using a naive, slow, and unoptimized algorithm.
	 * Does not have optimizations yet, do this for a bunch of units and it will
	 * probably lag like a walrus. The implementation here was created by reading
	 * the wikipedia article on A* to jog my memory from data structures class back
	 * in college, and is meant only as a first draft to get things working.
	 *
	 * @param collisionSize
	 *
	 *
	 * @param start
	 * @param goal
	 * @param playerIndex
	 * @param queueItem
	 * @return
	 */
	public void findNaiveSlowPath(final CUnit ignoreIntersectionsWithThisUnit,
			final CUnit ignoreIntersectionsWithThisSecondUnit, final float startX, final float startY,
			final CPathPoint goal, final PathingGrid.MovementType movementType, final float collisionSize,
			final boolean allowSmoothing, final CBehaviorMove queueItem) {
		this.moveQueue.offer(new PathfindingJob(ignoreIntersectionsWithThisUnit, ignoreIntersectionsWithThisSecondUnit,
				startX, startY, goal, movementType, collisionSize, allowSmoothing, queueItem));
	}

	public void removeFromPathfindingQueue(final CBehaviorMove behaviorMove) {
		// TODO because of silly java things, this remove is O(N) for now,
		// we could do some refactors to make it O(1) but do we care?
		final Iterator<PathfindingJob> iterator = this.moveQueue.iterator();
		while (iterator.hasNext()) {
			final PathfindingJob job = iterator.next();
			if (job.queueItem == behaviorMove) {
				iterator.remove();
			}
		}

	}

	private boolean pathableBetween(final CUnit ignoreIntersectionsWithThisUnit,
			final CUnit ignoreIntersectionsWithThisSecondUnit, final float startX, final float startY,
			final PathingGrid.MovementType movementType, final float collisionSize, final float x, final float y) {
		return this.pathingGrid.isPathable(x, y, movementType, collisionSize)
				&& this.pathingGrid.isPathable(startX, y, movementType, collisionSize)
				&& this.pathingGrid.isPathable(x, startY, movementType, collisionSize)
				&& isPathableDynamically(x, y, ignoreIntersectionsWithThisUnit, ignoreIntersectionsWithThisSecondUnit,
						movementType)
				&& isPathableDynamically(x, startY, ignoreIntersectionsWithThisUnit,
						ignoreIntersectionsWithThisSecondUnit, movementType)
				&& isPathableDynamically(startX, y, ignoreIntersectionsWithThisUnit,
						ignoreIntersectionsWithThisSecondUnit, movementType);
	}

	private boolean isPathableDynamically(final float x, final float y, final CUnit ignoreIntersectionsWithThisUnit,
			final CUnit ignoreIntersectionsWithThisSecondUnit, final PathingGrid.MovementType movementType) {
		return !this.worldCollision.intersectsStandingUnitOtherThan(tempRect.setCenter(x, y),
				ignoreIntersectionsWithThisUnit, ignoreIntersectionsWithThisSecondUnit, movementType, this.gameTurnTick);
	}

	public static boolean isCollisionSizeBetterSuitedForCorners(final float collisionSize) {
		return (((2 * (int) collisionSize) / 32) % 2) == 1;
	}

	/**
	 * Whether the job's unit fits at a grid point, the same test pathableBetween makes for each of its three points. A
	 * point is tested by up to eight of its neighbors, so the answer is kept for the rest of the job's work in this
	 * tick (units do not move within a tick, so it cannot change).
	 */
	private boolean nodePathable(final PathfindingJob job, final CSimulation simulation, final float x, final float y) {
		final Node node = node(job.searchGraph, job.gridMapping.getY(this.pathingGrid, y),
				job.gridMapping.getX(this.pathingGrid, x));
		final int tick = simulation.getGameTurnTick();
		if ((node.pathableJobId != this.pathfindJobId) || (node.pathableTick != tick)) {
			node.pathableJobId = this.pathfindJobId;
			node.pathableTick = tick;
			node.pathable = this.pathingGrid.isPathable(x, y, job.movementType, job.collisionSize)
					&& isPathableDynamically(x, y, job.ignoreIntersectionsWithThisUnit,
							job.ignoreIntersectionsWithThisSecondUnit, job.movementType);
		}
		return node.pathable;
	}

	private static void openAdd(final PathfindingJob job, final Node node) {
		node.open = true;
		job.openSet.add(node);
	}

	public double f(final Node n) {
		return n.g + h(n);
	}

	public double g(final Node n) {
		return n.g;
	}

	private boolean isGoal(final Node n) {
		for (int i = 0; i < this.goals; i++) {
			if (n == this.goalSet[i]) {
				return true;
			}
		}
		return false;
	}

	public float h(final Node n) {
		float bestDistance = 0;
		for (int i = 0; i < this.goals; i++) {
			final float possibleDistance = (float) n.point.distance(this.goalSet[i].point);
			if (possibleDistance > bestDistance) {
				bestDistance = possibleDistance; // always overestimate
			}
		}
		return bestDistance;
	}

	public static final class Node {
		public Direction cameFromDirection;
		private final CPathPoint point;
		private double f;
		private double g;
		private Node cameFrom;
		private int pathfindJobId;
		private boolean open;
		private int pathableJobId;
		private int pathableTick = -1;
		private boolean pathable;

		private Node(final CPathPoint point) {
			this.point = point;
		}

		private void touch(final int pathfindJobId) {
			if (pathfindJobId != this.pathfindJobId) {
				this.g = Float.POSITIVE_INFINITY;
				this.f = Float.POSITIVE_INFINITY;
				this.cameFrom = null;
				this.cameFromDirection = null;
				this.open = false;
				this.pathfindJobId = pathfindJobId;
			}
		}
	}

	private static enum Direction {
		NORTH_WEST(-1, 1), NORTH(0, 1), NORTH_EAST(1, 1), EAST(1, 0), SOUTH_EAST(1, -1), SOUTH(0, -1),
		SOUTH_WEST(-1, -1), WEST(-1, 0);

		public static final Direction[] VALUES = values();

		private final int xOffset;
		private final int yOffset;
		private final double length;

		private static Direction of(final float dx, final float dy) {
			final int xOffset = (int) Math.signum(dx);
			final int yOffset = (int) Math.signum(dy);
			for (final Direction direction : VALUES) {
				if ((direction.xOffset == xOffset) && (direction.yOffset == yOffset)) {
					return direction;
				}
			}
			return null;
		}

		private Direction(final int xOffset, final int yOffset) {
			this.xOffset = xOffset;
			this.yOffset = yOffset;
			final double sqrt = Math.sqrt((xOffset * xOffset) + (yOffset * yOffset));
			this.length = sqrt;
		}
	}

	public static interface GridMapping {
		int getX(PathingGrid grid, float worldX);

		int getY(PathingGrid grid, float worldY);

		public static final GridMapping CELLS = new GridMapping() {
			@Override
			public int getX(final PathingGrid grid, final float worldX) {
				return grid.getCellX(worldX);
			}

			@Override
			public int getY(final PathingGrid grid, final float worldY) {
				return grid.getCellY(worldY);
			}

		};

		public static final GridMapping CORNERS = new GridMapping() {
			@Override
			public int getX(final PathingGrid grid, final float worldX) {
				return grid.getCornerX(worldX);
			}

			@Override
			public int getY(final PathingGrid grid, final float worldY) {
				return grid.getCornerY(worldY);
			}

		};
	}

	public void update(final CSimulation simulation) {
		this.gameTurnTick = simulation.getGameTurnTick();
		final SharedGraph graph = this.graph;
		if ((graph.owner == this) && (this.moveQueue.isEmpty() || !this.moveQueue.peek().jobStarted)) {
			graph.owner = null;
		}
		if ((graph.reservedFor == this) && this.moveQueue.isEmpty()) {
			graph.reservedFor = null;
		}
		if ((graph.owner != null) && (graph.owner != this)) {
			if (!this.moveQueue.isEmpty() && (graph.reservedFor == null)) {
				graph.reservedFor = this;
			}
			return;
		}
		int workIterations = 0;
		JobsLoop: while (!this.moveQueue.isEmpty()) {
			this.totalJobLoops++;
			final PathfindingJob job = this.moveQueue.peek();
			this.tempRect.set(0, 0, job.collisionSize * 2, job.collisionSize * 2);
			if (!job.jobStarted) {
				if ((graph.reservedFor != null) && (graph.reservedFor != this)) {
					break JobsLoop;
				}
				graph.owner = this;
				graph.reservedFor = null;
				this.pathfindJobId = ++graph.lastJobId;
				this.totalIterations = 0;
				this.totalJobLoops = 0;
				job.jobStarted = true;
				PerfStats.pathJob();
				workIterations += 5; // setup of job predicted cost
				job.goalX = job.goal.x;
				job.goalY = job.goal.y;
				job.goalMoved = false;
				job.weightForHittingWalls = 1E9f;
				job.bestNode = null;
				job.bestDistance = Float.MAX_VALUE;
				job.bestGLimit = Double.MAX_VALUE;
				if (!this.pathingGrid.isPathable(job.goalX, job.goalY, job.movementType, job.collisionSize)) {
					if (!moveGoalToNearest(job, true)) {
						moveGoalToNearest(job, false);
					}
				}
				else if (!isPathableDynamically(job.goalX, job.goalY, job.ignoreIntersectionsWithThisUnit,
						job.ignoreIntersectionsWithThisSecondUnit, job.movementType)) {
					moveGoalToNearest(job, true);
				}
				if (!this.pathingGrid.isPathable(job.goalX, job.goalY, job.movementType, job.collisionSize)
						|| !isPathableDynamically(job.goalX, job.goalY, job.ignoreIntersectionsWithThisUnit,
								job.ignoreIntersectionsWithThisSecondUnit, job.movementType)) {
					job.weightForHittingWalls = 5E2f;
				}
				final double straightCells = StrictMath.hypot(job.goalX - job.startX, job.goalY - job.startY) / 32;
				job.iterationLimit = (int) Math.min(this.pathingGridCellCount,
						Math.max(MIN_SEARCH_ITERATIONS, 4 * straightCells * straightCells));
				if ((job.startX == job.goalX) && (job.startY == job.goalY)) {
					finishJob();
					job.queueItem.pathFound(new LinkedList<>(), !job.goalMoved, simulation);
					continue JobsLoop;
				}
				if (isCollisionSizeBetterSuitedForCorners(job.collisionSize)) {
					job.searchGraph = this.cornerNodes;
					job.gridMapping = GridMapping.CORNERS;
				}
				else {
					job.searchGraph = this.nodes;
					job.gridMapping = GridMapping.CELLS;
				}
				final int goalCellY = Math.min(Math.max(0, job.gridMapping.getY(this.pathingGrid, job.goalY)),
						job.searchGraph.length - 1);
				final int goalCellX = Math.min(Math.max(0, job.gridMapping.getX(this.pathingGrid, job.goalX)),
						job.searchGraph[goalCellY].length - 1);
				final Node mostLikelyGoal = node(job.searchGraph, goalCellY, goalCellX);
				mostLikelyGoal.touch(this.pathfindJobId);
				final double bestGoalDistance = mostLikelyGoal.point.distance(job.goalX, job.goalY);
				Arrays.fill(this.goalSet, null);
				this.goals = 0;
				for (int i = goalCellX - 1; i <= (goalCellX + 1); i++) {
					for (int j = goalCellY - 1; j <= (goalCellY + 1); j++) {
						if ((j >= 0) && (j < job.searchGraph.length)) {
							if ((i >= 0) && (i < job.searchGraph[j].length)) {
								final Node possibleGoal = node(job.searchGraph, j, i);
								possibleGoal.touch(this.pathfindJobId);
								if (possibleGoal.point.distance(job.goalX, job.goalY) <= bestGoalDistance) {
									this.goalSet[this.goals++] = possibleGoal;
								}
							}
						}
					}
				}
				final int startGridY = job.gridMapping.getY(this.pathingGrid, job.startY);
				final int startGridX = job.gridMapping.getX(this.pathingGrid, job.startX);
				job.openSet = new PriorityQueue<>(new Comparator<Node>() {
					@Override
					public int compare(final Node a, final Node b) {
						return Double.compare(a.f, b.f);
					}
				});

				job.start = node(job.searchGraph, startGridY, startGridX);
				job.start.touch(this.pathfindJobId);
				if (job.startX > job.start.point.x) {
					job.startGridMinX = startGridX;
					job.startGridMaxX = startGridX + 1;
				}
				else if (job.startX < job.start.point.x) {
					job.startGridMinX = startGridX - 1;
					job.startGridMaxX = startGridX;
				}
				else {
					job.startGridMinX = startGridX;
					job.startGridMaxX = startGridX;
				}
				if (job.startY > job.start.point.y) {
					job.startGridMinY = startGridY;
					job.startGridMaxY = startGridY + 1;
				}
				else if (job.startY < job.start.point.y) {
					job.startGridMinY = startGridY - 1;
					job.startGridMaxY = startGridY;
				}
				else {
					job.startGridMinY = startGridY;
					job.startGridMaxY = startGridY;
				}
				for (int cellX = job.startGridMinX; cellX <= job.startGridMaxX; cellX++) {
					for (int cellY = job.startGridMinY; cellY <= job.startGridMaxY; cellY++) {
						if ((cellX >= 0) && (cellX < this.pathingGrid.getWidth()) && (cellY >= 0)
								&& (cellY < this.pathingGrid.getHeight())) {
							final Node possibleNode = node(job.searchGraph, cellY, cellX);
							possibleNode.touch(this.pathfindJobId);
							final float x = possibleNode.point.x;
							final float y = possibleNode.point.y;
							if (pathableBetween(job.ignoreIntersectionsWithThisUnit,
									job.ignoreIntersectionsWithThisSecondUnit, job.startX, job.startY, job.movementType,
									job.collisionSize, x, y)) {

								final double tentativeScore = possibleNode.point.distance(job.startX, job.startY);
								job.bestGLimit = Math.min(job.bestGLimit, tentativeScore + job.weightForHittingWalls);
								possibleNode.g = tentativeScore;
								possibleNode.f = tentativeScore + h(possibleNode);
								openAdd(job, possibleNode);

							}
							else {
								final double tentativeScore = job.weightForHittingWalls;
								// A unit squeezed among others at its start still looks for the nearest point it
								// can walk to.
								job.bestGLimit = Math.min(job.bestGLimit, tentativeScore + job.weightForHittingWalls);
								possibleNode.g = tentativeScore;
								possibleNode.f = tentativeScore + h(possibleNode);
								openAdd(job, possibleNode);

							}
						}
					}
				}
			}

			while (!job.openSet.isEmpty()) {
				Node current = job.openSet.poll();
				current.touch(this.pathfindJobId);
				current.open = false;
				if (isGoal(current)) {
					final LinkedList<CPathPoint> totalPath = buildPath(job, current, true);
					finishJob();
					job.queueItem.pathFound(totalPath, !job.goalMoved, simulation);
					continue JobsLoop;
				}
				if (current.g < job.bestGLimit) {
					final float distanceToGoal = (float) current.point.distance(job.goalX, job.goalY);
					if (distanceToGoal < job.bestDistance) {
						job.bestDistance = distanceToGoal;
						job.bestNode = current;
					}
				}

				for (final Direction direction : Direction.VALUES) {
					final float x = current.point.x + (direction.xOffset * 32);
					final float y = current.point.y + (direction.yOffset * 32);
					if (this.pathingGrid.contains(x, y)) {
						double turnCost;
						if ((current.cameFromDirection != null) && (direction != current.cameFromDirection)) {
							turnCost = 0.25;
						}
						else {
							turnCost = 0;
						}
						double tentativeScore = current.g + ((direction.length + turnCost) * 32);
						if (!nodePathable(job, simulation, x, y) || !nodePathable(job, simulation, current.point.x, y)
								|| !nodePathable(job, simulation, x, current.point.y)) {
							tentativeScore += (direction.length) * job.weightForHittingWalls;
						}
						final Node neighbor = node(job.searchGraph, job.gridMapping.getY(this.pathingGrid, y),
								job.gridMapping.getX(this.pathingGrid, x));
						neighbor.touch(this.pathfindJobId);
						if (tentativeScore < neighbor.g) {
							neighbor.cameFrom = current;
							neighbor.cameFromDirection = direction;
							neighbor.g = tentativeScore;
							neighbor.f = tentativeScore + h(neighbor);
							if (!neighbor.open) {
								openAdd(job, neighbor);
							}
						}
					}
				}
				workIterations++;
				this.totalIterations++;
				PerfStats.pathIteration();
				// Measured: the original walks across a whole melee map; a fixed cap stopped such paths after a few
				// steps. A short chase into a crowd of units has no way through and searched the whole map each time
				// a unit in a fight moved, so the limit grows with the distance instead.
				if (this.totalIterations > job.iterationLimit) {
					break;
				}
				if (workIterations >= 1500) {
					// breaking jobs loop will implicitly exit without calling pathFound() below
					break JobsLoop;
				}
			}
			// Measured: the original walks to the reachable point nearest the target and stops there.
			final LinkedList<CPathPoint> partialPath;
			if ((job.bestNode != null)
					&& (job.bestDistance < (StrictMath.hypot(job.goalX - job.startX, job.goalY - job.startY) - 16))) {
				partialPath = buildPath(job, job.bestNode, false);
			}
			else {
				partialPath = new LinkedList<>();
				partialPath.add(new CPathPoint(job.startX, job.startY));
			}
			finishJob();
			job.queueItem.pathFound(partialPath, false, simulation);
		}
	}

	private static final float MAX_SMOOTHED_SEGMENT = 1024f;

	/**
	 * The path from the start through the searched nodes to endNode (and on to the goal when it was reached). With
	 * smoothing, a node is skipped whenever the unit can walk straight past it, so units walk any-angle lines instead
	 * of the grid's zigzag.
	 */
	private LinkedList<CPathPoint> buildPath(final PathfindingJob job, final Node endNode, final boolean reachedGoal) {
		final ArrayList<CPathPoint> chain = new ArrayList<>();
		Node node = endNode;
		while (node != null) {
			chain.add(node.point);
			node = node.cameFrom;
			if (chain.size() > this.pathingGridCellCount) {
				new IllegalStateException("PATHING SYSTEM ERROR: cycle in the path near " + endNode.point + ", unit "
						+ CUnit.maybeMeaningfulName(job.ignoreIntersectionsWithThisUnit)).printStackTrace();
				return new LinkedList<>();
			}
		}
		Collections.reverse(chain);
		if (reachedGoal) {
			if (!job.goalMoved) {
				chain.add(job.goal);
			}
			else {
				chain.add(new CPathPoint(job.goalX, job.goalY));
			}
		}
		final LinkedList<CPathPoint> path = new LinkedList<>();
		float anchorX = job.startX;
		float anchorY = job.startY;
		Direction lastDirection = null;
		for (int i = 0; i < chain.size(); i++) {
			final CPathPoint point = chain.get(i);
			if (i < (chain.size() - 1)) {
				final CPathPoint next = chain.get(i + 1);
				if (job.allowSmoothing) {
					if ((next.distance(anchorX, anchorY) <= MAX_SMOOTHED_SEGMENT)
							&& segmentClear(job, anchorX, anchorY, next.x, next.y)) {
						continue;
					}
				}
				else {
					final Direction direction = Direction.of(next.x - point.x, next.y - point.y);
					if ((direction != null) && (direction == lastDirection)) {
						lastDirection = direction;
						continue;
					}
					lastDirection = direction;
				}
			}
			path.add(point);
			anchorX = point.x;
			anchorY = point.y;
		}
		return path;
	}

	/** Whether the job's unit can walk the straight line between two points: the same tests its movement makes. */
	private boolean segmentClear(final PathfindingJob job, final float x0, final float y0, final float x1,
			final float y1) {
		final float dx = x1 - x0;
		final float dy = y1 - y0;
		final int steps = Math.max(1, (int) Math.ceil(StrictMath.hypot(dx, dy) / 8));
		for (int step = 1; step <= steps; step++) {
			final float x = x0 + ((dx * step) / steps);
			final float y = y0 + ((dy * step) / steps);
			if (!this.pathingGrid.isPathable(x, y, job.movementType, job.collisionSize)
					|| !isPathableDynamically(x, y, job.ignoreIntersectionsWithThisUnit,
							job.ignoreIntersectionsWithThisSecondUnit, job.movementType)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Measured: ordered onto ground it cannot stand on, or onto a point other units stand on, the original walks to the
	 * free point nearest the target (around a wall of units if that is where it is). Searching toward the blocked point
	 * itself explored nearly the whole map, or made walls of units cheap to cross so the unit stopped in front of them.
	 */
	private boolean moveGoalToNearest(final PathfindingJob job, final boolean freeOfUnits) {
		float bestX = 0;
		float bestY = 0;
		double bestDistance = Double.MAX_VALUE;
		double bestStartDistance = Double.MAX_VALUE;
		int foundRing = -1;
		for (int ring = 1; ring <= NEAREST_PATHABLE_RINGS; ring++) {
			// a point on the next ring can still be nearer than the corners of this one
			if ((foundRing != -1) && (ring > (foundRing + 1))) {
				break;
			}
			for (int i = -ring; i <= ring; i++) {
				for (int j = -ring; j <= ring; j++) {
					if ((Math.abs(i) != ring) && (Math.abs(j) != ring)) {
						continue;
					}
					final float x = job.goalX + (i * 32);
					final float y = job.goalY + (j * 32);
					final double distance = StrictMath.hypot(i * 32, j * 32);
					if (distance > bestDistance) {
						continue;
					}
					final double startDistance = StrictMath.hypot(x - job.startX, y - job.startY);
					if ((distance == bestDistance) && (startDistance >= bestStartDistance)) {
						continue;
					}
					if (this.pathingGrid.contains(x, y)
							&& this.pathingGrid.isPathable(x, y, job.movementType, job.collisionSize)
							&& (!freeOfUnits || isPathableDynamically(x, y, job.ignoreIntersectionsWithThisUnit,
									job.ignoreIntersectionsWithThisSecondUnit, job.movementType))) {
						bestDistance = distance;
						bestStartDistance = startDistance;
						bestX = x;
						bestY = y;
						if (foundRing == -1) {
							foundRing = ring;
						}
					}
				}
			}
		}
		if (foundRing == -1) {
			return false;
		}
		job.goalX = bestX;
		job.goalY = bestY;
		job.goalMoved = true;
		return true;
	}

	public static final class PathfindingJob {
		private final CUnit ignoreIntersectionsWithThisUnit;
		private final CUnit ignoreIntersectionsWithThisSecondUnit;
		private final float startX;
		private final float startY;
		private final CPathPoint goal;
		private final MovementType movementType;
		private final float collisionSize;
		private final boolean allowSmoothing;
		private final CBehaviorMove queueItem;
		private boolean jobStarted;
		public float goalY;
		public float goalX;
		public float weightForHittingWalls;
		int iterationLimit;
		boolean goalMoved;
		Node bestNode;
		float bestDistance;
		double bestGLimit;
		Node[][] searchGraph;
		GridMapping gridMapping;
		PriorityQueue<Node> openSet;
		Node start;
		int startGridMinX;
		int startGridMinY;
		int startGridMaxX;
		int startGridMaxY;

		public PathfindingJob(final CUnit ignoreIntersectionsWithThisUnit,
				final CUnit ignoreIntersectionsWithThisSecondUnit, final float startX, final float startY,
				final CPathPoint goal, final PathingGrid.MovementType movementType, final float collisionSize,
				final boolean allowSmoothing, final CBehaviorMove queueItem) {
			this.ignoreIntersectionsWithThisUnit = ignoreIntersectionsWithThisUnit;
			this.ignoreIntersectionsWithThisSecondUnit = ignoreIntersectionsWithThisSecondUnit;
			this.startX = startX;
			this.startY = startY;
			this.goal = goal;
			this.movementType = movementType;
			this.collisionSize = collisionSize;
			this.allowSmoothing = allowSmoothing;
			this.queueItem = queueItem;
			this.jobStarted = false;
		}
	}
}
