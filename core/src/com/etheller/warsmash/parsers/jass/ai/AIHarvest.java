package com.etheller.warsmash.parsers.jass.ai;

import static com.etheller.warsmash.parsers.jass.ai.AIUnits.canHarvestGold;
import static com.etheller.warsmash.parsers.jass.ai.AIUnits.canHarvestLumber;
import static com.etheller.warsmash.parsers.jass.ai.AIUnits.distance;
import static com.etheller.warsmash.parsers.jass.ai.AIUnits.isBuilding;
import static com.etheller.warsmash.parsers.jass.ai.AIUnits.isIdle;
import static com.etheller.warsmash.parsers.jass.ai.AIUnits.isWorkerType;
import static com.etheller.warsmash.parsers.jass.ai.AIUnits.minesFromOverlay;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitClassification;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.harvest.CAbilityHarvest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.harvest.CAbilityWispHarvest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIdUtils;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;

/**
 * The computer player's workers: the gold and lumber jobs the scripts ask for, per town, and the repairs.
 */
final class AIHarvest {
	static final int GOLD = 0;
	static final int WOOD = 1;
	private static final int TREE_CACHE_TICKS = CTimer.wholeSteps(20);

	private AIHarvest() {
	}

	static void harvest(final AIController ai, final int resource, final int town, final int peons) {
		if (peons > 0) {
			ai.harvestRequests.add(new int[] { resource, town, peons });
		}
	}

	private static boolean canDo(final AIController ai, final CUnit worker, final int resource, final int town) {
		if (resource == WOOD) {
			return canHarvestLumber(worker);
		}
		if (!canHarvestGold(worker)) {
			return false;
		}
		final AITown aiTown = AITowns.getTown(ai, town);
		if ((aiTown == null) || (aiTown.getMine() == null) || (aiTown.getGold() <= 0)) {
			return false;
		}
		if (minesFromOverlay(worker)) {
			return AITowns.ownsOverlay(ai, aiTown) || (worker.getFirstAbilityOfType(CAbilityWispHarvest.class) != null);
		}
		return aiTown.getOverlay() == null;
	}

	static void assignHarvesters(final AIController ai) {
		final List<CUnit> workers = new ArrayList<>();
		for (final CUnit unit : ai.simulation.getUnits()) {
			if (ai.isOurs(unit) && !ai.isBuildJobWorker(unit) && !ai.attackCaptain.getMembers().contains(unit)
					&& (canHarvestGold(unit) || canHarvestLumber(unit))) {
				workers.add(unit);
			}
		}
		ai.harvestJobs.keySet().retainAll(workers);
		final List<int[]> slots = new ArrayList<>();
		for (final int[] request : ai.harvestRequests) {
			for (int i = 0; i < request[2]; i++) {
				slots.add(new int[] { request[0], request[1] });
			}
		}
		// Slots are filled in the order the script asked for them, so with few workers the first requests (gold)
		// win over later ones even when the workers already hold a job for a later one.
		final Set<CUnit> claimed = new HashSet<>();
		final CUnit[] slotWorkers = new CUnit[slots.size()];
		for (int s = 0; s < slots.size(); s++) {
			final int[] slot = slots.get(s);
			for (final CUnit worker : workers) {
				final int[] job = ai.harvestJobs.get(worker);
				if (!claimed.contains(worker) && (job != null) && (job[0] == slot[0]) && (job[1] == slot[1])) {
					claimed.add(worker);
					slotWorkers[s] = worker;
					break;
				}
			}
			if (slotWorkers[s] != null) {
				continue;
			}
			final AITown town = AITowns.getTown(ai, slot[1]);
			if (town == null) {
				continue;
			}
			CUnit best = null;
			float bestScore = Float.MAX_VALUE;
			for (final CUnit worker : workers) {
				if (claimed.contains(worker) || worker.isHidden() || isBuilding(worker)
						|| !canDo(ai, worker, slot[0], slot[1])) {
					continue;
				}
				float score = distance(worker.getX(), worker.getY(), town.getCenterX(), town.getCenterY());
				if (ai.harvestJobs.containsKey(worker)) {
					score += 5000;
				}
				if (!isWorkerType(worker) && !isIdle(worker)) {
					score += 10000;
				}
				if (score < bestScore) {
					bestScore = score;
					best = worker;
				}
			}
			if (best != null) {
				claimed.add(best);
				slotWorkers[s] = best;
				ai.harvestJobs.put(best, slot);
				orderHarvest(ai, best, slot[0], slot[1]);
			}
		}
		for (int s = 0; s < slots.size(); s++) {
			final CUnit worker = slotWorkers[s];
			if ((worker != null) && !worker.isHidden() && isIdle(worker)) {
				orderHarvest(ai, worker, slots.get(s)[0], slots.get(s)[1]);
			}
		}
		for (final CUnit worker : workers) {
			if (claimed.contains(worker)) {
				continue;
			}
			if (!isWorkerType(worker)) {
				ai.harvestJobs.remove(worker);
			}
			else if (!worker.isHidden() && isIdle(worker)) {
				// Measured: with no harvest request at all (StopGathering included) idle workers still mine gold.
				final int[] job;
				if (ai.harvestRequests.isEmpty()) {
					job = new int[] { GOLD, canDo(ai, worker, GOLD, 0) ? 0 : Math.max(AITowns.townWithMine(ai), 0) };
				}
				else {
					job = canDo(ai, worker, WOOD, 0) ? new int[] { WOOD, 0 } : new int[] { GOLD, 0 };
				}
				if (canDo(ai, worker, job[0], job[1])) {
					ai.harvestJobs.put(worker, job);
					orderHarvest(ai, worker, job[0], job[1]);
				}
			}
		}
		if (ai.peonsRepair) {
			assignRepairs(ai, workers);
		}
	}

	private static void orderHarvest(final AIController ai, final CUnit worker, final int resource,
			final int townId) {
		final AITown town = AITowns.getTown(ai, townId);
		if (town == null) {
			return;
		}
		if (resource == GOLD) {
			if (minesFromOverlay(worker)) {
				if (AITowns.ownsOverlay(ai, town)) {
					orderHarvestTarget(ai, worker, town.getOverlay());
				}
				else if ((town.getOverlay() == null) && (town.getMine() != null)) {
					entangle(ai, town);
				}
			}
			else if (town.getMine() != null) {
				orderHarvestTarget(ai, worker, town.getMine());
			}
			return;
		}
		final List<CDestructable> trees = getTrees(ai, town);
		int tries = 0;
		for (final Iterator<CDestructable> iterator = trees.iterator(); iterator.hasNext() && (tries < 6);) {
			final CDestructable tree = iterator.next();
			if (tree.isDead()) {
				iterator.remove();
				continue;
			}
			tries++;
			if (orderHarvestTarget(ai, worker, tree)) {
				return;
			}
		}
	}

	/**
	 * A worker that carries resources only takes the return order, so it returns them first and goes on to the
	 * target from there.
	 */
	private static boolean orderHarvestTarget(final AIController ai, final CUnit worker, final CWidget target) {
		if (worker.order(ai.simulation, OrderIds.harvest, target)) {
			return true;
		}
		final CAbilityHarvest harvest = worker.getFirstAbilityOfType(CAbilityHarvest.class);
		if ((harvest == null) || (harvest.getCarriedResourceAmount() <= 0)) {
			return false;
		}
		harvest.setLastHarvestTarget(target);
		return worker.order(ai.simulation, OrderIds.returnresources, null);
	}

	private static void entangle(final AIController ai, final AITown town) {
		final int entangleOrder = OrderIdUtils.getOrderId("entangleinstant");
		for (final CUnit unit : ai.simulation.getUnits()) {
			if (ai.isOurs(unit) && AIUnits.isReady(unit)
					&& unit.getClassifications().contains(CUnitClassification.TOWNHALL)
					&& (distance(unit.getX(), unit.getY(), town.getMine().getX(), town.getMine().getY()) < 1200)) {
				unit.order(ai.simulation, entangleOrder, town.getMine());
				return;
			}
		}
	}

	private static List<CDestructable> getTrees(final AIController ai, final AITown town) {
		final Integer cachedTick = ai.townTreesTick.get(town.getId());
		List<CDestructable> trees = ai.townTrees.get(town.getId());
		if ((trees == null) || (cachedTick == null) || ((ai.ticks - cachedTick) > TREE_CACHE_TICKS)
				|| trees.isEmpty()) {
			trees = new ArrayList<>();
			final float cx = town.getCenterX();
			final float cy = town.getCenterY();
			for (final CDestructable destructable : ai.simulation.getDestructables()) {
				if (!destructable.isDead() && destructable.getDestType().getTargetedAs().contains(CTargetType.TREE)
						&& (distance(cx, cy, destructable.getX(), destructable.getY()) < 2500)) {
					trees.add(destructable);
				}
			}
			trees.sort((a, b) -> Float.compare(distance(cx, cy, a.getX(), a.getY()),
					distance(cx, cy, b.getX(), b.getY())));
			ai.townTrees.put(town.getId(), trees);
			ai.townTreesTick.put(town.getId(), ai.ticks);
		}
		return trees;
	}

	private static void assignRepairs(final AIController ai, final List<CUnit> workers) {
		if (ai.threat != null) {
			return;
		}
		for (final CUnit worker : workers) {
			if (isBuilding(worker) && !ai.isBuildJobWorker(worker)) {
				return;
			}
		}
		for (final CUnit unit : ai.simulation.getUnits()) {
			if (!ai.isOurs(unit) || !unit.isBuilding() || unit.isConstructing() || unit.isUpgrading()
					|| (unit.getLife() > (unit.getMaxLife() * 0.6f))) {
				continue;
			}
			CUnit best = null;
			float bestDistance = 1500f;
			for (final CUnit worker : workers) {
				final int[] job = ai.harvestJobs.get(worker);
				if (worker.isHidden() || (job == null) || (job[0] != WOOD)) {
					continue;
				}
				final float d = distance(worker.getX(), worker.getY(), unit.getX(), unit.getY());
				if (d < bestDistance) {
					bestDistance = d;
					best = worker;
				}
			}
			if ((best != null) && best.order(ai.simulation, AIUnits.repairOrderId(best), unit)) {
				return;
			}
		}
	}
}
