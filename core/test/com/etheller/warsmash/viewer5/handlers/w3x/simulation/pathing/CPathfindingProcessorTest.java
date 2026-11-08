package com.etheller.warsmash.viewer5.handlers.w3x.simulation.pathing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.badlogic.gdx.math.Rectangle;
import com.etheller.warsmash.parsers.w3x.wpm.War3MapWpm;
import com.etheller.warsmash.testing.TestObjects;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid.MovementType;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid.PathingFlags;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWorldCollision;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorMove;

/** Searches on a small terrain grid with no units on it. */
class CPathfindingProcessorTest {
	private static final int SIZE = 24;
	private static final float CELL = 32f;
	/** Uses the cell centers. */
	private static final float CELL_COLLISION = 8f;
	/** Uses the cell corners. */
	private static final float CORNER_COLLISION = 16f;

	private static final class Result {
		List<CPathPoint> waypoints;
		boolean reachesGoal;
	}

	private static final class RecordingMove extends CBehaviorMove {
		private final Result result;

		RecordingMove(final Result result) {
			super(null);
			this.result = result;
		}

		@Override
		public void pathFound(final CPathPoint searchedGoal, final List<CPathPoint> waypoints,
				final boolean reachesGoal, final CSimulation simulation) {
			this.result.waypoints = new ArrayList<>(waypoints);
			this.result.reachesGoal = reachesGoal;
		}
	}

	private static PathingGrid grid(final boolean[][] blocked) throws IOException {
		final War3MapWpm wpm = new War3MapWpm(null);
		wpm.getSize()[0] = SIZE;
		wpm.getSize()[1] = SIZE;
		final short[] pathing = new short[SIZE * SIZE];
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				if (blocked[y][x]) {
					pathing[(y * SIZE) + x] = (short) (PathingFlags.UNWALKABLE | PathingFlags.UNBUILDABLE);
				}
			}
		}
		wpm.setPathing(pathing);
		return new PathingGrid(wpm, new float[] { 0, 0 });
	}

	private static Result search(final PathingGrid grid, final float startX, final float startY, final float goalX,
			final float goalY, final float collisionSize) {
		final CSimulation simulation = TestObjects.allocate(CSimulation.class);
		final CWorldCollision collision = new CWorldCollision(new Rectangle(0, 0, SIZE * CELL, SIZE * CELL), 64f);
		final CPathfindingProcessor processor = new CPathfindingProcessor(grid, collision,
				new CPathfindingProcessor.SharedGraph(grid));
		final Result result = new Result();
		processor.findNaiveSlowPath(null, null, startX, startY, new CPathPoint(goalX, goalY), MovementType.FOOT,
				collisionSize, true, new RecordingMove(result));
		for (int i = 0; (i < 100) && (result.waypoints == null); i++) {
			TestObjects.set(simulation, "gameTurnTick", i);
			processor.update(simulation);
		}
		assertNotNull(result.waypoints, "search did not finish");
		return result;
	}

	/** Every point along the path, every few units, is walkable terrain. */
	private static void assertWalkable(final PathingGrid grid, final float startX, final float startY,
			final List<CPathPoint> waypoints) {
		float x = startX;
		float y = startY;
		for (final CPathPoint point : waypoints) {
			final float length = (float) Math.hypot(point.x - x, point.y - y);
			final int samples = Math.max(1, (int) (length / 4));
			for (int i = 0; i <= samples; i++) {
				final float sx = x + (((point.x - x) * i) / samples);
				final float sy = y + (((point.y - y) * i) / samples);
				assertTrue(grid.isPathable(sx, sy, MovementType.FOOT, 0f),
						"path crosses blocked terrain at " + sx + "," + sy);
			}
			x = point.x;
			y = point.y;
		}
	}

	private static CPathPoint last(final List<CPathPoint> waypoints) {
		return waypoints.get(waypoints.size() - 1);
	}

	private static float maxY(final List<CPathPoint> waypoints) {
		float max = -Float.MAX_VALUE;
		for (final CPathPoint point : waypoints) {
			max = Math.max(max, point.y);
		}
		return max;
	}

	/** A wall across column {@code wallX} from the bottom up to (not including) row {@code gapFromY}. */
	private static boolean[][] wall(final int wallX, final int gapFromY) {
		final boolean[][] blocked = new boolean[SIZE][SIZE];
		for (int y = 0; y < gapFromY; y++) {
			blocked[y][wallX] = true;
		}
		return blocked;
	}

	@Test
	void openGroundReachesTheGoal() throws IOException {
		for (final float collision : new float[] { CELL_COLLISION, CORNER_COLLISION }) {
			final PathingGrid grid = grid(new boolean[SIZE][SIZE]);
			final Result result = search(grid, 80, 80, 600, 400, collision);
			assertTrue(result.reachesGoal);
			assertEquals(600, last(result.waypoints).x, 1e-3);
			assertEquals(400, last(result.waypoints).y, 1e-3);
			assertWalkable(grid, 80, 80, result.waypoints);
		}
	}

	@Test
	void goesRoundAWall() throws IOException {
		for (final float collision : new float[] { CELL_COLLISION, CORNER_COLLISION }) {
			final PathingGrid grid = grid(wall(12, 18));
			final Result result = search(grid, 200, 80, 600, 80, collision);
			assertTrue(result.reachesGoal);
			assertEquals(600, last(result.waypoints).x, 1e-3);
			assertEquals(80, last(result.waypoints).y, 1e-3);
			assertTrue(maxY(result.waypoints) >= (18 * CELL), "path does not go through the gap");
			assertWalkable(grid, 200, 80, result.waypoints);
		}
	}

	@Test
	void sameSearchGivesTheSamePath() throws IOException {
		final PathingGrid grid = grid(wall(12, 18));
		final Result first = search(grid, 200, 80, 600, 80, CELL_COLLISION);
		final Result second = search(grid, 200, 80, 600, 80, CELL_COLLISION);
		assertEquals(first.waypoints.size(), second.waypoints.size());
		for (int i = 0; i < first.waypoints.size(); i++) {
			assertEquals(first.waypoints.get(i).x, second.waypoints.get(i).x);
			assertEquals(first.waypoints.get(i).y, second.waypoints.get(i).y);
		}
	}

	@Test
	void walledOffGoalStopsAtTheNearestReachablePoint() throws IOException {
		final PathingGrid grid = grid(wall(12, SIZE));
		final Result result = search(grid, 200, 300, 600, 300, CELL_COLLISION);
		assertFalse(result.reachesGoal);
		final CPathPoint end = last(result.waypoints);
		assertTrue(end.x < (12 * CELL), "path ends past the wall");
		assertTrue(end.x > 300, "path does not walk towards the goal");
		assertWalkable(grid, 200, 300, result.waypoints);
	}
}
