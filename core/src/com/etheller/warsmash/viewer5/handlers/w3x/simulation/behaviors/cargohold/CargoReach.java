package com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.cargohold;

import java.util.Arrays;

import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid.MovementType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;

/**
 * Where a transport and a unit boarding it can meet: the point a unit can walk (or sail) to that is nearest to a
 * goal, searched over the pathing cells around the unit. A ship and a footman on its shore never stand on the same
 * ground, so each heads for its own nearest point and the loading range bridges the shore.
 */
public final class CargoReach {
	// Searching for a point to unload from tests every cell, so that search stays near the transport.
	private static final int DROP_SEARCH_RADIUS_CELLS = 96;
	// How far around itself a unit looks for the ground nearest to a transport it is to meet. Measured on 1.29: a
	// footman 8000 from a ship heads for water 1750 away rather than water nearer the ship 3000 away.
	public static final int MEETING_SEARCH_RADIUS_CELLS = 56;
	private static final float CELL_SIZE = 32f;
	private static final int[] NEIGHBOR_X = { 1, -1, 0, 0, 1, 1, -1, -1 };
	private static final int[] NEIGHBOR_Y = { 0, 0, 1, -1, 1, -1, 1, -1 };

	private static boolean[] visited = new boolean[0];
	private static int[] queue = new int[0];
	private static boolean[] transportReach = new boolean[0];

	private CargoReach() {
	}

	/** A test on the centre of a pathing cell. */
	public interface PointTest {
		boolean test(float x, float y);
	}

	/**
	 * The point nearest to (goalX, goalY) that the unit can reach from where it stands; the goal itself when the
	 * unit can stand there. A flying unit reaches everything.
	 */
	public static float[] nearestReachable(final CSimulation game, final CUnit unit, final float goalX,
			final float goalY) {
		return nearestReachable(game, unit, goalX, goalY, Integer.MAX_VALUE);
	}

	/** As above, looking only within radiusCells pathing cells of the unit. */
	public static float[] nearestReachable(final CSimulation game, final CUnit unit, final float goalX,
			final float goalY, final int radiusCells) {
		final MovementType movementType = unit.getMovementType();
		if ((movementType == null) || (movementType == MovementType.FLY) || (movementType == MovementType.DISABLED)) {
			return new float[] { goalX, goalY };
		}
		final float[] result = search(game, unit, goalX, goalY, null, radiusCells, false);
		return result != null ? result : new float[] { unit.getX(), unit.getY() };
	}

	/**
	 * Where a unit boarding a transport that cannot stand on its ground heads: the point it can reach, within the
	 * meeting search radius, that has water (or ground) the transport can reach within range and lies nearest to
	 * the transport. The unit's own position when there is none.
	 */
	public static float[] nearestShore(final CSimulation game, final CUnit unit, final CUnit transport,
			final float range) {
		final MovementType movementType = unit.getMovementType();
		final MovementType transportMovementType = transport.getMovementType();
		if ((movementType == null) || (movementType == MovementType.FLY) || (movementType == MovementType.DISABLED)
				|| (transportMovementType == null) || (transportMovementType == MovementType.FLY)
				|| (transportMovementType == MovementType.DISABLED)) {
			return nearestReachable(game, unit, transport.getX(), transport.getY(), MEETING_SEARCH_RADIUS_CELLS);
		}
		final PathingGrid grid = game.getPathingGrid();
		if (!floodTransportReach(grid, transport)) {
			return new float[] { unit.getX(), unit.getY() };
		}
		final int width = grid.getWidth();
		final int height = grid.getHeight();
		final int rangeCells = (int) Math.ceil(range / CELL_SIZE);
		final PointTest nearReach = (x, y) -> {
			final int cellX = grid.getCellX(x);
			final int cellY = grid.getCellY(y);
			for (int dy = -rangeCells; dy <= rangeCells; dy++) {
				for (int dx = -rangeCells; dx <= rangeCells; dx++) {
					final int nx = cellX + dx;
					final int ny = cellY + dy;
					if ((nx < 0) || (ny < 0) || (nx >= width) || (ny >= height)) {
						continue;
					}
					final int packed = (ny * width) + nx;
					if (transportReach[packed]
							&& (StrictMath.hypot(grid.getWorldX(nx) - x, grid.getWorldY(ny) - y) <= range)) {
						return true;
					}
				}
			}
			return false;
		};
		final float[] result = search(game, unit, transport.getX(), transport.getY(), nearReach,
				MEETING_SEARCH_RADIUS_CELLS, false);
		return result != null ? result : new float[] { unit.getX(), unit.getY() };
	}

	private static boolean floodTransportReach(final PathingGrid grid, final CUnit transport) {
		final int width = grid.getWidth();
		final int height = grid.getHeight();
		final int startX = grid.getCellX(transport.getX());
		final int startY = grid.getCellY(transport.getY());
		if ((startX < 0) || (startY < 0) || (startX >= width) || (startY >= height)) {
			return false;
		}
		if (transportReach.length != (width * height)) {
			transportReach = new boolean[width * height];
			queue = new int[width * height];
			visited = new boolean[width * height];
		}
		else {
			Arrays.fill(transportReach, false);
		}
		final MovementType movementType = transport.getMovementType();
		final float collisionSize = transport.getUnitType().getCollisionSize();
		int head = 0;
		int tail = 0;
		transportReach[(startY * width) + startX] = true;
		queue[tail++] = (startY * width) + startX;
		while (head < tail) {
			final int packed = queue[head++];
			final int cellX = packed % width;
			final int cellY = packed / width;
			for (int i = 0; i < NEIGHBOR_X.length; i++) {
				final int nextX = cellX + NEIGHBOR_X[i];
				final int nextY = cellY + NEIGHBOR_Y[i];
				if ((nextX < 0) || (nextY < 0) || (nextX >= width) || (nextY >= height)) {
					continue;
				}
				final int next = (nextY * width) + nextX;
				if (!transportReach[next] && grid.isCellPathable(nextX, nextY, movementType, collisionSize)) {
					transportReach[next] = true;
					queue[tail++] = next;
				}
			}
		}
		return true;
	}

	/**
	 * The reachable point where the test holds that lies best on the way from the unit to (goalX, goalY): the
	 * least distance from the unit plus distance on to the goal. Null when there is none within the search area.
	 */
	public static float[] reachableOnTheWay(final CSimulation game, final CUnit unit, final float goalX,
			final float goalY, final PointTest test) {
		final MovementType movementType = unit.getMovementType();
		if ((movementType == null) || (movementType == MovementType.DISABLED)) {
			return null;
		}
		return search(game, unit, goalX, goalY, test, DROP_SEARCH_RADIUS_CELLS, true);
	}

	private static float[] search(final CSimulation game, final CUnit unit, final float goalX, final float goalY,
			final PointTest test, final int radiusCells, final boolean onTheWay) {
		final MovementType movementType = unit.getMovementType();
		final PathingGrid grid = game.getPathingGrid();
		final float collisionSize = unit.getUnitType().getCollisionSize();
		final int startX = grid.getCellX(unit.getX());
		final int startY = grid.getCellY(unit.getY());
		final int goalCellX = grid.getCellX(goalX);
		final int goalCellY = grid.getCellY(goalY);
		final boolean flying = movementType == MovementType.FLY;
		final int width = grid.getWidth();
		final int height = grid.getHeight();
		if ((startX < 0) || (startY < 0) || (startX >= width) || (startY >= height)) {
			return null;
		}
		if (visited.length != (width * height)) {
			visited = new boolean[width * height];
			queue = new int[width * height];
			transportReach = new boolean[width * height];
		}
		else {
			Arrays.fill(visited, false);
		}
		int head = 0;
		int tail = 0;
		visited[(startY * width) + startX] = true;
		queue[tail++] = (startY * width) + startX;
		int bestX = startX;
		int bestY = startY;
		double bestDistance = distanceSquared(unit.getX(), unit.getY(), goalX, goalY);
		double bestScore = Double.MAX_VALUE;
		boolean found = false;
		while (head < tail) {
			final int packed = queue[head++];
			final int cellX = packed % width;
			final int cellY = packed / width;
			if (test != null) {
				final float worldX = grid.getWorldX(cellX);
				final float worldY = grid.getWorldY(cellY);
				final double score = (onTheWay ? StrictMath.hypot(worldX - unit.getX(), worldY - unit.getY()) : 0)
						+ StrictMath.hypot(goalX - worldX, goalY - worldY);
				if ((score < bestScore) && test.test(worldX, worldY)) {
					bestScore = score;
					bestX = cellX;
					bestY = cellY;
					found = true;
				}
			}
			else {
				if ((cellX == goalCellX) && (cellY == goalCellY)) {
					return new float[] { goalX, goalY };
				}
				final double distance = distanceSquared(grid.getWorldX(cellX), grid.getWorldY(cellY), goalX, goalY);
				if (distance < bestDistance) {
					bestDistance = distance;
					bestX = cellX;
					bestY = cellY;
				}
			}
			for (int i = 0; i < NEIGHBOR_X.length; i++) {
				final int nextX = cellX + NEIGHBOR_X[i];
				final int nextY = cellY + NEIGHBOR_Y[i];
				if ((nextX < 0) || (nextY < 0) || (nextX >= width) || (nextY >= height)
						|| (Math.abs(nextX - startX) > radiusCells) || (Math.abs(nextY - startY) > radiusCells)) {
					continue;
				}
				final int next = (nextY * width) + nextX;
				if (visited[next]) {
					continue;
				}
				visited[next] = true;
				if (flying || grid.isCellPathable(nextX, nextY, movementType, collisionSize)) {
					queue[tail++] = next;
				}
			}
		}
		if (test != null) {
			return found ? new float[] { grid.getWorldX(bestX), grid.getWorldY(bestY) } : null;
		}
		if ((bestX == startX) && (bestY == startY)) {
			return null;
		}
		return new float[] { grid.getWorldX(bestX), grid.getWorldY(bestY) };
	}

	private static double distanceSquared(final float x, final float y, final float goalX, final float goalY) {
		final double dx = goalX - x;
		final double dy = goalY - y;
		return (dx * dx) + (dy * dy);
	}
}
