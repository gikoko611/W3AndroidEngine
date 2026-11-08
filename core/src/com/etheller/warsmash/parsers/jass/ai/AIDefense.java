package com.etheller.warsmash.parsers.jass.ai;

import static com.etheller.warsmash.parsers.jass.ai.AIUnits.distance;

import java.util.List;
import java.util.Map;

import com.etheller.warsmash.parsers.jass.ai.AIController.AIGuardPost;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorAttack;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;

/**
 * Units that stay home: guard posts and guard positions, the defense captain and the threats to our towns.
 */
final class AIDefense {
	private static final float THREAT_RADIUS = 1300f;
	private static final float GUARD_RETURN_DISTANCE = 100f;

	private AIDefense() {
	}

	private static AIGuardPost nearestFreePost(final AIController ai, final CUnit unit) {
		AIGuardPost best = null;
		float bestDistance = Float.MAX_VALUE;
		for (final AIGuardPost post : ai.guardPosts) {
			if (post.typeId.equals(unit.getTypeId()) && ((post.unit == null) || post.unit.isDead())) {
				final float d = distance(unit.getX(), unit.getY(), post.x, post.y);
				if (d < bestDistance) {
					bestDistance = d;
					best = post;
				}
			}
		}
		return best;
	}

	static void leavePost(final AIController ai, final CUnit unit) {
		for (final AIGuardPost post : ai.guardPosts) {
			if (post.unit == unit) {
				post.unit = null;
			}
		}
	}

	/**
	 * Measured: units the player has when the AI starts keep their place as their guard position. Later ones do the
	 * same until the script has called StopGathering (InitAI does); after that they go at once to a free guard post
	 * of their type, nearest first, and the rest join the defense captain at its home.
	 */
	static void placeNewUnits(final AIController ai) {
		ai.guardPositions.keySet().removeIf((unit) -> unit.isDead());
		for (final CUnit unit : ai.simulation.getUnits()) {
			if (!ai.isGuardCandidate(unit) || ai.guardPositions.containsKey(unit)) {
				continue;
			}
			ai.guardPositions.put(unit, new float[] { unit.getX(), unit.getY() });
			if (!ai.unitsSeen || !ai.gatheringStopped || ai.attackCaptain.getMembers().contains(unit)) {
				continue;
			}
			final AIGuardPost post = nearestFreePost(ai, unit);
			if (post != null) {
				post.unit = unit;
				ai.guardPositions.put(unit, new float[] { post.x, post.y });
				unit.order(ai.simulation, OrderIds.attack, new AbilityPointTarget(post.x, post.y));
			}
			else if (ai.isArmyUnit(unit)) {
				joinDefense(ai, unit);
			}
		}
		ai.unitsSeen = true;
	}

	/**
	 * Measured: every unit outside the captains that has wandered off walks back to its guard position, attack-moving,
	 * even if a script had ordered it away; fighting units finish first.
	 */
	static void returnGuardPosts(final AIController ai) {
		for (final Map.Entry<CUnit, float[]> entry : ai.guardPositions.entrySet()) {
			final CUnit unit = entry.getKey();
			final float[] position = entry.getValue();
			if (unit.isDead() || unit.isHidden() || ai.attackCaptain.getMembers().contains(unit)
					|| ai.defenseCaptain.getMembers().contains(unit)
					|| (unit.getCurrentBehavior() instanceof CBehaviorAttack)
					|| (distance(unit.getX(), unit.getY(), position[0], position[1]) < GUARD_RETURN_DISTANCE)) {
				continue;
			}
			unit.order(ai.simulation, OrderIds.attack, new AbilityPointTarget(position[0], position[1]));
		}
	}

	/**
	 * Measured: a defender leaves its guard post or place for the defense home of that moment; a later SetCaptainHome
	 * does not move it.
	 */
	private static void joinDefense(final AIController ai, final CUnit unit) {
		leavePost(ai, unit);
		ai.defenseCaptain.getMembers().add(unit);
		final float x = ai.defenseCaptain.getHomeX();
		final float y = ai.defenseCaptain.getHomeY();
		ai.guardPositions.put(unit, new float[] { x, y });
		unit.order(ai.simulation, OrderIds.move, new AbilityPointTarget(x, y));
	}

	/**
	 * Measured: AddDefenders claims units at once, also ones the player had from the start, and is false without one.
	 */
	static boolean addDefenders(final AIController ai, final int qty, final int typeIdValue) {
		final War3ID typeId = new War3ID(typeIdValue);
		final List<CUnit> members = ai.defenseCaptain.getMembers();
		int got = 0;
		for (final CUnit unit : members) {
			if (!unit.isDead() && unit.getTypeId().equals(typeId)) {
				got++;
			}
		}
		for (final CUnit unit : ai.simulation.getUnits()) {
			if (got >= qty) {
				break;
			}
			if (unit.getTypeId().equals(typeId) && ai.isArmyUnit(unit) && !unit.isConstructing()
					&& !members.contains(unit) && !ai.attackCaptain.getMembers().contains(unit)) {
				joinDefense(ai, unit);
				got++;
			}
		}
		return got >= qty;
	}

	private static CUnit findThreat(final AIController ai) {
		CUnit best = null;
		float bestDistance = Float.MAX_VALUE;
		for (final AITown town : ai.towns) {
			if (town.getHall() == null) {
				continue;
			}
			final float x = town.getCenterX();
			final float y = town.getCenterY();
			final CUnit[] found = new CUnit[1];
			final float[] foundDistance = { THREAT_RADIUS };
			ai.simulation.getWorldCollision().enumUnitsInRange(x, y, THREAT_RADIUS, (unit) -> {
				if (ai.isEnemy(unit) && !ai.simulation.isNeutralAggressive(unit.getPlayerIndex())
						&& unit.isVisible(ai.simulation, ai.playerIndex)) {
					final float d = distance(x, y, unit.getX(), unit.getY());
					if (d < foundDistance[0]) {
						foundDistance[0] = d;
						found[0] = unit;
					}
				}
				return false;
			});
			if ((found[0] != null) && (foundDistance[0] < bestDistance)) {
				bestDistance = foundDistance[0];
				best = found[0];
			}
		}
		return best;
	}

	static void updateDefense(final AIController ai) {
		ai.threat = findThreat(ai);
		final boolean attackIdle = !ai.attackCaptain.hasGoal() && !ai.attackCaptain.isRetreating();
		final List<CUnit> defenders = ai.defenseCaptain.getMembers();
		defenders.removeIf((unit) -> !ai.isArmyUnit(unit));
		if (ai.threat != null) {
			final AbilityPointTarget target = new AbilityPointTarget(ai.threat.getX(), ai.threat.getY());
			for (final CUnit unit : defenders) {
				if (!(unit.getCurrentBehavior() instanceof CBehaviorAttack)) {
					unit.order(ai.simulation, OrderIds.attack, target);
				}
			}
			if (attackIdle) {
				for (final CUnit unit : ai.attackCaptain.getMembers()) {
					if (!(unit.getCurrentBehavior() instanceof CBehaviorAttack)) {
						unit.order(ai.simulation, OrderIds.attack, target);
					}
				}
			}
			return;
		}
		for (final CUnit unit : defenders) {
			final float[] place = ai.guardPositions.get(unit);
			if ((place != null) && AIUnits.isIdle(unit)
					&& (distance(unit.getX(), unit.getY(), place[0], place[1]) > GUARD_RETURN_DISTANCE)) {
				unit.order(ai.simulation, OrderIds.attack, new AbilityPointTarget(place[0], place[1]));
			}
		}
	}
}
