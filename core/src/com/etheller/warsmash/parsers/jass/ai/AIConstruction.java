package com.etheller.warsmash.parsers.jass.ai;

import static com.etheller.warsmash.parsers.jass.ai.AIUnits.distance;
import static com.etheller.warsmash.parsers.jass.ai.AIUnits.distanceToSegment;

import java.util.Iterator;

import com.etheller.warsmash.util.ArgbImage;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitClassification;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.build.AbstractCAbilityBuild;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrder;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.BuildOnBuildingIntersector;

/**
 * Structures the computer player builds: choosing the worker, finding the place in a town and following the worker
 * until the structure is laid down.
 */
final class AIConstruction {
	private static final float BUILDING_GAP = 96f;
	private static final int BUILD_JOB_TIMEOUT_TICKS = CTimer.wholeSteps(90);
	private static final int BUILD_JOB_GRACE_TICKS = CTimer.wholeSteps(2);

	private AIConstruction() {
	}

	/**
	 * @param type null for any worker that can build
	 */
	static CUnit findBuilder(final AIController ai, final CUnitType type, final float x, final float y,
			final int preferredTown) {
		CUnit best = null;
		float bestScore = Float.MAX_VALUE;
		for (final CUnit unit : ai.simulation.getUnits()) {
			if (!ai.isOurs(unit) || unit.isHidden() || ai.isBuildJobWorker(unit) || AIUnits.isBuilding(unit)
					|| ai.attackCaptain.getMembers().contains(unit)) {
				continue;
			}
			final AbstractCAbilityBuild build = AIUnits.getBuildAbility(unit);
			if ((build == null) || ((type != null) && !build.getStructuresBuilt().contains(type.getTypeId()))) {
				continue;
			}
			float score = distance(x, y, unit.getX(), unit.getY());
			final int[] job = ai.harvestJobs.get(unit);
			if (job == null) {
				score -= 4000;
			}
			else if (job[0] == AIHarvest.WOOD) {
				score -= 2000;
			}
			if ((preferredTown != -1) && (AITowns.townOf(ai, unit.getX(), unit.getY()) != preferredTown)) {
				score += 3000;
			}
			if (score < bestScore) {
				bestScore = score;
				best = unit;
			}
		}
		return best;
	}

	static boolean buildStructures(final AIController ai, final int qty, final CUnitType type, final int townId) {
		if (!AIProduction.requirementsMet(ai, type)) {
			return false;
		}
		// Measured: one builder at a time; until it has laid its structure down, another structure is refused.
		if (AIProduction.canAfford(ai, type) && hasBuilderOnTheWay(ai)) {
			return false;
		}
		int started = 0;
		for (int i = 0; i < qty; i++) {
			if (!AIProduction.canAfford(ai, type)) {
				// Measured: short of money it is still true, with nothing started.
				return true;
			}
			AITown town = AITowns.getTown(ai, townId == -1 ? 0 : townId);
			if (type.isCanBeBuiltOnThem() && (townId == -1)) {
				town = AITowns.townNeedingOverlay(ai);
			}
			if (town == null) {
				break;
			}
			final CUnit builder = findBuilder(ai, type, town.getCenterX(), town.getCenterY(), town.getId());
			if (builder == null) {
				break;
			}
			final AbilityPointTarget location;
			if (type.isCanBeBuiltOnThem() || (type.getClassifications().contains(CUnitClassification.TOWNHALL)
					&& !AITowns.townHasHall(ai, town.getId()))) {
				location = findTownStructureLocation(ai, type, builder, town);
			}
			else {
				location = findBuildLocation(ai, type, builder, town);
			}
			if ((location == null) || !orderBuild(ai, builder, type, location, town.getId())) {
				break;
			}
			started++;
		}
		return started > 0;
	}

	static boolean orderBuild(final AIController ai, final CUnit builder, final CUnitType type,
			final AbilityPointTarget location, final int town) {
		if (!builder.order(ai.simulation, type.getTypeId().getValue(), location)) {
			return false;
		}
		ai.harvestJobs.remove(builder);
		ai.buildJobs.add(
				new AIBuildJob(builder, type, location.x, location.y, town, ai.simulation.getGameTurnTick()));
		ai.countCache.clear();
		return true;
	}

	static boolean isLaidDown(final AIController ai, final AIBuildJob job) {
		for (final CUnit unit : ai.simulation.getUnits()) {
			if (ai.isOurs(unit) && unit.getTypeId().equals(job.getTypeId())
					&& (distance(unit.getX(), unit.getY(), job.getX(), job.getY()) < 64)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasBuilderOnTheWay(final AIController ai) {
		updateBuildJobs(ai);
		return !ai.buildJobs.isEmpty();
	}

	static boolean canBeBuiltByUs(final AIController ai, final CUnitType type) {
		for (final CUnit unit : ai.simulation.getUnits()) {
			if (ai.isOurs(unit)) {
				final AbstractCAbilityBuild build = AIUnits.getBuildAbility(unit);
				if ((build != null) && build.getStructuresBuilt().contains(type.getTypeId())) {
					return true;
				}
			}
		}
		return false;
	}

	static void updateBuildJobs(final AIController ai) {
		final int tick = ai.simulation.getGameTurnTick();
		for (final Iterator<AIBuildJob> iterator = ai.buildJobs.iterator(); iterator.hasNext();) {
			final AIBuildJob job = iterator.next();
			final CUnit worker = job.getWorker();
			boolean finished = worker.isDead() || isLaidDown(ai, job)
					|| ((tick - job.getIssuedTick()) > BUILD_JOB_TIMEOUT_TICKS);
			if (!finished && ((tick - job.getIssuedTick()) > BUILD_JOB_GRACE_TICKS)) {
				final COrder order = worker.getCurrentOrder();
				finished = (order == null) || (order.getOrderId() != job.getTypeId().getValue());
				finished &= !AIUnits.isBuilding(worker);
			}
			if (finished) {
				iterator.remove();
			}
		}
	}

	private static int halfWidth(final CUnitType type) {
		final ArgbImage map = type.getBuildingPathingPixelMap();
		return map == null ? 64 : (map.getWidth() * 16);
	}

	private static int halfHeight(final CUnitType type) {
		final ArgbImage map = type.getBuildingPathingPixelMap();
		return map == null ? 64 : (map.getHeight() * 16);
	}

	private static boolean isObstructed(final AIController ai, final CUnitType type, final CUnit builder,
			final float x, final float y) {
		return AbstractCAbilityBuild.isBuildLocationObstructed(ai.simulation, type, type.getBuildingPathingPixelMap(),
				type.isCanBeBuiltOnThem(), x, y, builder, BuildOnBuildingIntersector.INSTANCE.reset(x, y));
	}

	private static boolean overlapsBuildJob(final AIController ai, final CUnitType type, final float x,
			final float y) {
		for (final AIBuildJob job : ai.buildJobs) {
			if ((Math.abs(job.getX() - x) < (halfWidth(type) + halfWidth(job.getType()) + 64))
					&& (Math.abs(job.getY() - y) < (halfHeight(type) + halfHeight(job.getType()) + 64))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Haunted mines go on the gold mine, town halls at a mining distance from it; the start town's hall goes back
	 * to the start location.
	 */
	static AbilityPointTarget findTownStructureLocation(final AIController ai, final CUnitType type,
			final CUnit builder, final AITown town) {
		if (type.isCanBeBuiltOnThem()) {
			return town.getMine() == null ? null : new AbilityPointTarget(town.getMine().getX(), town.getMine().getY());
		}
		if (town.getId() == 0) {
			final AbilityPointTarget start = new AbilityPointTarget(town.getX(), town.getY());
			AbstractCAbilityBuild.roundTargetPoint(start, type);
			if (!isObstructed(ai, type, builder, start.x, start.y)) {
				return start;
			}
		}
		final CUnit mine = town.getMine();
		if (mine == null) {
			return null;
		}
		final float minRadius = halfWidth(mine.getUnitType()) + halfWidth(type) + 160;
		final float awayAngle = (float) StrictMath.atan2(mapCenterY(ai) - mine.getY(), mapCenterX(ai) - mine.getX());
		return searchLocation(ai, type, builder, mine.getX(), mine.getY(), minRadius, minRadius + 384, awayAngle,
				null);
	}

	private static float mapCenterX(final AIController ai) {
		final PathingGrid grid = ai.simulation.getPathingGrid();
		return (grid.getWorldX(0) + grid.getWorldX(grid.getWidth() - 1)) / 2;
	}

	private static float mapCenterY(final AIController ai) {
		final PathingGrid grid = ai.simulation.getPathingGrid();
		return (grid.getWorldY(0) + grid.getWorldY(grid.getHeight() - 1)) / 2;
	}

	private static AbilityPointTarget findBuildLocation(final AIController ai, final CUnitType type,
			final CUnit builder, final AITown town) {
		final float cx = town.getCenterX();
		final float cy = town.getCenterY();
		final CUnit mine = town.getMine();
		float preferredAngle = (float) StrictMath.atan2(mapCenterY(ai) - cy, mapCenterX(ai) - cx);
		if (mine != null) {
			preferredAngle = (float) StrictMath.atan2(cy - mine.getY(), cx - mine.getX());
		}
		final float minRadius = Math.max(256f, 160f + halfWidth(type) + 128f);
		return searchLocation(ai, type, builder, cx, cy, minRadius, 1800f, preferredAngle, mine);
	}

	/**
	 * Rings outward from the center, starting each ring at the preferred angle, for the first spot that is free
	 * with a cell of room around it, keeps clear of the gold mine and of the path between mine and hall, and is
	 * not taken by another worker's pending building.
	 */
	private static AbilityPointTarget searchLocation(final AIController ai, final CUnitType type,
			final CUnit builder, final float cx, final float cy, final float minRadius, final float maxRadius,
			final float preferredAngle, final CUnit avoidMine) {
		final AbilityPointTarget point = new AbilityPointTarget(0, 0);
		final float size = Math.max(halfWidth(type), halfHeight(type));
		for (float radius = minRadius; radius <= maxRadius; radius += 64f) {
			final int steps = Math.max(8, (int) ((2 * Math.PI * radius) / 96f));
			for (int step = 0; step < steps; step++) {
				final int side = ((step & 1) == 0) ? 1 : -1;
				final double angle = preferredAngle + (side * ((step + 1) / 2) * ((2 * Math.PI) / steps));
				point.x = cx + (float) (StrictMath.cos(angle) * radius);
				point.y = cy + (float) (StrictMath.sin(angle) * radius);
				AbstractCAbilityBuild.roundTargetPoint(point, type);
				if (avoidMine != null) {
					final float mineDistance = distance(point.x, point.y, avoidMine.getX(), avoidMine.getY());
					if ((mineDistance < (size + 320f))
							|| (distanceToSegment(point.x, point.y, cx, cy, avoidMine.getX(), avoidMine.getY()) < (size
									+ 160f))) {
						continue;
					}
				}
				if (ai.attackCaptain.isHomeSet() && (distance(point.x, point.y, ai.attackCaptain.getHomeX(),
						ai.attackCaptain.getHomeY()) < (size + 400f))) {
					continue;
				}
				if (overlapsBuildJob(ai, type, point.x, point.y) || isObstructed(ai, type, builder, point.x, point.y)) {
					continue;
				}
				if (!type.isCanBeBuiltOnThem() && (isObstructed(ai, type, builder, point.x + BUILDING_GAP, point.y)
						|| isObstructed(ai, type, builder, point.x - BUILDING_GAP, point.y)
						|| isObstructed(ai, type, builder, point.x, point.y + BUILDING_GAP)
						|| isObstructed(ai, type, builder, point.x, point.y - BUILDING_GAP))) {
					continue;
				}
				return new AbilityPointTarget(point.x, point.y);
			}
		}
		return null;
	}
}
