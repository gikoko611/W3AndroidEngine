package com.etheller.warsmash.parsers.jass.ai;

import static com.etheller.warsmash.parsers.jass.ai.AIUnits.isReady;
import static com.etheller.warsmash.parsers.jass.ai.AIUnits.queueLength;

import java.util.ArrayList;
import java.util.List;

import com.etheller.interpreter.ast.execution.JassThread;
import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.value.IntegerJassValue;
import com.etheller.interpreter.ast.value.JassValue;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit.QueueItemType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUpgradeType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.hero.CAbilityHero;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.queue.CAbilityQueue;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.queue.CAbilityReviveHero;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.upgrade.CAbilityUpgrade;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIdUtils;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;

/**
 * What the computer player has and makes: unit counts as the AI sees them, training units and heroes, research,
 * structure upgrades, hero skills and the unit transformations the scripts order.
 */
final class AIProduction {
	// Measured on 1.29: the AI only trains on an idle producer; SetProduce is false while all of them are busy, which
	// holds back the rest of the build priorities.
	private static final int MAX_QUEUED_PER_PRODUCER = 1;

	private AIProduction() {
	}

	static int getUnitCount(final AIController ai, final int typeId, final boolean done) {
		if (ai.countCacheTick != ai.simulation.getGameTurnTick()) {
			ai.countCacheTick = ai.simulation.getGameTurnTick();
			ai.countCache.clear();
		}
		final War3ID id = new War3ID(typeId);
		int[] counts = ai.countCache.get(id);
		if (counts == null) {
			counts = new int[] { countUnits(ai, ai.playerIndex, id, false, -1),
					countUnits(ai, ai.playerIndex, id, true, -1) };
			ai.countCache.put(id, counts);
		}
		return done ? counts[1] : counts[0];
	}

	/**
	 * Counts units of a type like the AI sees them: a structure being upgraded counts as its new type until done,
	 * queued units and heroes being revived count as not done, and so do the structures our workers are on
	 * their way to build.
	 */
	static int countUnits(final AIController ai, final int ownerIndex, final War3ID id, final boolean done,
			final int town) {
		int count = 0;
		for (final CUnit unit : ai.simulation.getUnits()) {
			if ((unit.getPlayerIndex() != ownerIndex) || unit.isDead()) {
				continue;
			}
			if ((town != -1) && (AITowns.townOf(ai, unit.getX(), unit.getY()) != town)) {
				continue;
			}
			if (unit.isUpgrading()) {
				if (done ? id.equals(unit.getTypeId()) : id.equals(unit.getUpgradeIdType())) {
					count++;
				}
			}
			else if (id.equals(unit.getTypeId()) && (!done || !unit.isConstructing())) {
				count++;
			}
			if (!done) {
				final War3ID[] queue = unit.getBuildQueue();
				final QueueItemType[] types = unit.getBuildQueueTypes();
				for (int i = 0; i < queue.length; i++) {
					if (queue[i] == null) {
						continue;
					}
					if ((types[i] == QueueItemType.UNIT) && id.equals(queue[i])) {
						count++;
					}
					else if (types[i] == QueueItemType.HERO_REVIVE) {
						final CUnit hero = ai.simulation.getUnit(queue[i].getValue());
						if ((hero != null) && id.equals(hero.getTypeId())) {
							count++;
						}
					}
				}
			}
		}
		if (!done && (ownerIndex == ai.playerIndex)) {
			for (final AIBuildJob job : ai.buildJobs) {
				if (id.equals(job.getTypeId()) && ((town == -1) || (job.getTown() == town))
						&& !AIConstruction.isLaidDown(ai, job)) {
					count++;
				}
			}
		}
		return count;
	}

	static boolean requirementsMet(final AIController ai, final CUnitType type) {
		final CPlayer player = ai.player;
		if (!player.isTechtreeAllowedByMax(type.getTypeId())) {
			return false;
		}
		if (!AIUnits.requirementsUnlocked(player, type.getRequirements())) {
			return false;
		}
		if (type.isHero()) {
			if (!player.isHeroCountAllowedByMax(ai.simulation)) {
				return false;
			}
			if (!AIUnits.requirementsUnlocked(player,
					type.getRequirementsTier(player.getHeroCount(ai.simulation, true)))) {
				return false;
			}
		}
		return true;
	}

	static boolean canAfford(final AIController ai, final CUnitType type) {
		return (ai.player.getGold() >= type.getGoldCost()) && (ai.player.getLumber() >= type.getLumberCost());
	}

	static boolean setProduce(final AIController ai, final int qty, final int typeIdValue, final int town) {
		final War3ID typeId = new War3ID(typeIdValue);
		final CUnitType type = ai.simulation.getUnitData().getUnitType(typeId);
		if ((type == null) || (qty <= 0)) {
			return false;
		}
		if (type.isHero()) {
			return produceHero(ai, type);
		}
		if (AIConstruction.canBeBuiltByUs(ai, type)) {
			return AIConstruction.buildStructures(ai, qty, type, town);
		}
		final List<CUnit> bases = new ArrayList<>();
		final List<CUnit> producers = new ArrayList<>();
		boolean anyBase = false;
		for (final CUnit unit : ai.simulation.getUnits()) {
			if (!ai.isOurs(unit) || ((town != -1) && (AITowns.townOf(ai, unit.getX(), unit.getY()) != town))) {
				continue;
			}
			final CAbilityUpgrade upgrade = unit.getFirstAbilityOfType(CAbilityUpgrade.class);
			if ((upgrade != null) && upgrade.getUpgradesTo().contains(typeId)) {
				anyBase = true;
				if (isReady(unit)) {
					bases.add(unit);
				}
			}
			final CAbilityQueue queue = unit.getFirstAbilityOfType(CAbilityQueue.class);
			if ((queue != null) && queue.getUnitsTrained().contains(typeId) && isReady(unit)) {
				producers.add(unit);
			}
		}
		if (anyBase) {
			if (!requirementsMet(ai, type)) {
				return false;
			}
			int started = 0;
			for (final CUnit base : bases) {
				if (started >= qty) {
					break;
				}
				if ((base.getBuildQueue()[0] == null) && base.order(ai.simulation, typeIdValue, null)) {
					started++;
				}
			}
			return started > 0;
		}
		if (producers.isEmpty() || !requirementsMet(ai, type)) {
			return false;
		}
		if (!canAfford(ai, type)) {
			// Measured: short of money it is true while a producer is idle, and nothing is queued.
			for (final CUnit producer : producers) {
				if (queueLength(producer) < MAX_QUEUED_PER_PRODUCER) {
					return true;
				}
			}
			return false;
		}
		return train(ai, producers, typeIdValue, qty) > 0;
	}

	private static int train(final AIController ai, final List<CUnit> producers, final int typeIdValue,
			final int qty) {
		int started = 0;
		for (int i = 0; i < qty; i++) {
			CUnit best = null;
			int bestQueue = MAX_QUEUED_PER_PRODUCER;
			for (final CUnit producer : producers) {
				final int length = queueLength(producer);
				if (length < bestQueue) {
					bestQueue = length;
					best = producer;
				}
			}
			if ((best == null) || !best.order(ai.simulation, typeIdValue, null)) {
				break;
			}
			started++;
		}
		if (started > 0) {
			ai.countCache.clear();
		}
		return started;
	}

	private static boolean produceHero(final AIController ai, final CUnitType type) {
		CUnit altar = null;
		CUnit deadHero = null;
		final List<CUnit> producers = new ArrayList<>();
		for (final CUnit unit : ai.simulation.getUnits()) {
			if (unit.getPlayerIndex() != ai.playerIndex) {
				continue;
			}
			if (unit.isDead()) {
				final CAbilityHero heroData = unit.getHeroData();
				if ((heroData != null) && unit.getTypeId().equals(type.getTypeId()) && heroData.isAwaitingRevive()
						&& !heroData.isReviving()) {
					deadHero = unit;
				}
				continue;
			}
			if (!isReady(unit)) {
				continue;
			}
			if ((altar == null) && (unit.getFirstAbilityOfType(CAbilityReviveHero.class) != null)
					&& (queueLength(unit) < MAX_QUEUED_PER_PRODUCER)) {
				altar = unit;
			}
			final CAbilityQueue queue = unit.getFirstAbilityOfType(CAbilityQueue.class);
			if ((queue != null) && queue.getUnitsTrained().contains(type.getTypeId())) {
				producers.add(unit);
			}
		}
		if (deadHero != null) {
			if (altar == null) {
				return false;
			}
			altar.order(ai.simulation, deadHero.getHandleId(), null);
			return true;
		}
		if (!ai.newHeroes || producers.isEmpty() || !requirementsMet(ai, type)) {
			return false;
		}
		return train(ai, producers, type.getTypeId().getValue(), 1) > 0;
	}

	static int getUpgradeLevel(final AIController ai, final int typeId) {
		final War3ID id = new War3ID(typeId);
		return ai.player.getTechtreeUnlocked(id) + ai.player.getTechtreeInProgress(id);
	}

	static int getUpgradeGoldCost(final AIController ai, final int typeId) {
		final War3ID id = new War3ID(typeId);
		final CUpgradeType upgrade = ai.simulation.getUpgradeData().getType(id);
		return upgrade == null ? 0 : upgrade.getGoldCost(ai.player.getTechtreeUnlocked(id));
	}

	static int getUpgradeWoodCost(final AIController ai, final int typeId) {
		final War3ID id = new War3ID(typeId);
		final CUpgradeType upgrade = ai.simulation.getUpgradeData().getType(id);
		return upgrade == null ? 0 : upgrade.getLumberCost(ai.player.getTechtreeUnlocked(id));
	}

	static boolean setUpgrade(final AIController ai, final int typeId) {
		final War3ID id = new War3ID(typeId);
		final CUpgradeType upgrade = ai.simulation.getUpgradeData().getType(id);
		if (upgrade == null) {
			return false;
		}
		if (ai.player.getTechtreeInProgress(id) > 0) {
			return true;
		}
		final CUpgradeType.UpgradeLevel level = upgrade.getLevel(ai.player.getTechtreeUnlocked(id));
		if ((level == null) || !AIUnits.requirementsUnlocked(ai.player, level.getRequirements())) {
			return false;
		}
		CUnit best = null;
		for (final CUnit unit : ai.simulation.getUnits()) {
			if (ai.isOurs(unit) && isReady(unit)) {
				final CAbilityQueue queue = unit.getFirstAbilityOfType(CAbilityQueue.class);
				if ((queue != null) && queue.getResearchesAvailable().contains(id)
						&& ((best == null) || (queueLength(unit) < queueLength(best)))) {
					best = unit;
				}
			}
		}
		if ((best == null) || (queueLength(best) >= MAX_QUEUED_PER_PRODUCER)) {
			return false;
		}
		return best.order(ai.simulation, typeId, null);
	}

	static void updateHeroSkills(final AIController ai) {
		if (ai.heroLevelsFunction == null) {
			return;
		}
		for (final CUnit unit : ai.simulation.getUnits()) {
			if (!ai.isOurs(unit)) {
				continue;
			}
			final CAbilityHero heroData = unit.getHeroData();
			if (heroData == null) {
				continue;
			}
			for (int guard = 0; (guard < 4) && (heroData.getSkillPoints() > 0); guard++) {
				final int before = heroData.getSkillPoints();
				ai.levelingHero = unit;
				ai.levelingHeroLevel = Math.max(1, (heroData.getHeroLevel() - before) + 1);
				int skill = 0;
				try {
					final JassThread thread = ai.globalScope.createThreadCapturingReturnValue(ai.heroLevelsFunction,
							TriggerExecutionScope.EMPTY);
					final JassValue value = ai.globalScope.runThreadUntilCompletionAndReadReturnValue(thread,
							"SetHeroLevels", IntegerJassValue.ZERO);
					if (value != null) {
						skill = value.visit(IntegerJassValueVisitor.getInstance());
					}
				}
				finally {
					ai.levelingHero = null;
				}
				if (skill != 0) {
					heroData.selectHeroSkill(ai.simulation, unit, new War3ID(skill));
				}
				if (heroData.getSkillPoints() == before) {
					for (final War3ID other : unit.getUnitType().getHeroAbilityList()) {
						heroData.selectHeroSkill(ai.simulation, unit, other);
						if (heroData.getSkillPoints() != before) {
							break;
						}
					}
				}
				if (heroData.getSkillPoints() == before) {
					break;
				}
			}
		}
	}

	static boolean mergeUnits(final AIController ai, final int qty, final int typeA, final int typeB,
			final int result) {
		if (getUnitCount(ai, result, false) >= qty) {
			return true;
		}
		final War3ID a = new War3ID(typeA);
		final War3ID b = new War3ID(typeB);
		CUnit first = null;
		CUnit second = null;
		for (final CUnit unit : ai.simulation.getUnits()) {
			if (ai.isArmyUnit(unit) && AIUnits.isIdle(unit) && !ai.attackCaptain.getMembers().contains(unit)) {
				if ((first == null) && unit.getTypeId().equals(a)) {
					first = unit;
				}
				else if ((second == null) && unit.getTypeId().equals(b)) {
					second = unit;
				}
			}
		}
		if ((first == null) || (second == null)) {
			return false;
		}
		return second.order(ai.simulation, OrderIdUtils.getOrderId("coupletarget"), first)
				|| first.order(ai.simulation, OrderIdUtils.getOrderId("coupletarget"), second);
	}

	static boolean convertUnits(final AIController ai, final int qty, final int typeIdValue) {
		final War3ID typeId = new War3ID(typeIdValue);
		int converted = 0;
		for (final CUnit unit : ai.simulation.getUnits()) {
			if ((converted < qty) && ai.isArmyUnit(unit) && unit.getTypeId().equals(typeId)
					&& unit.order(ai.simulation, OrderIdUtils.getOrderId("sphinxform"), null)) {
				converted++;
			}
		}
		return converted > 0;
	}

	static void unsummon(final AIController ai, final CUnit building) {
		if ((building == null) || !ai.isOurs(building)) {
			return;
		}
		final int unsummonOrder = OrderIdUtils.getOrderId("unsummon");
		for (final CUnit unit : ai.simulation.getUnits()) {
			if (ai.isOurs(unit) && !unit.isHidden() && unit.order(ai.simulation, unsummonOrder, building)) {
				return;
			}
		}
	}
}
