package com.etheller.warsmash.parsers.jass.ai;

import static com.etheller.warsmash.parsers.jass.ai.AIUnits.distance;
import static com.etheller.warsmash.parsers.jass.ai.AIUnits.isIdle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.etheller.warsmash.parsers.jass.ai.AIController.AIZepWave;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;

/**
 * Attack groups carried by our zeppelins.
 */
final class AIZeppelins {
	private static final float ZEPPELIN_HOME_DISTANCE = 260f;

	private AIZeppelins() {
	}

	private static boolean isOwnZeppelin(final AIController ai, final CUnit unit) {
		return ai.isOurs(unit) && (unit.getCargoData() != null) && !unit.isHidden()
				&& (unit.getUnitType().getMovementType() == PathingGrid.MovementType.FLY);
	}

	private static boolean isZeppelinBusy(final AIController ai, final CUnit zeppelin) {
		for (final AIZepWave wave : ai.zepWaves) {
			if (wave.zeppelin == zeppelin) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Measured: the group boards at once where it stands, the zeppelin flies straight to the point, the units land and
	 * attack-move on and the zeppelin flies home by itself. Without a zeppelin the group walks.
	 */
	static void loadZepWave(final AIController ai, final int x, final int y) {
		final List<CUnit> waiting = new ArrayList<>();
		for (final CUnit unit : ai.attackCaptain.getMembers()) {
			if (!unit.isHidden() && !isAboard(ai, unit)
					&& (unit.getUnitType().getMovementType() != PathingGrid.MovementType.FLY)) {
				waiting.add(unit);
			}
		}
		for (final CUnit unit : ai.simulation.getUnits()) {
			if (waiting.isEmpty()) {
				return;
			}
			if (isOwnZeppelin(ai, unit) && !isZeppelinBusy(ai, unit)) {
				final AIZepWave wave = new AIZepWave(unit, x, y);
				final int capacity = unit.getCargoData().getCargoCapacity() - unit.getCargoData().getCargoCount();
				for (int i = 0; (i < capacity) && !waiting.isEmpty(); i++) {
					wave.cargo.add(waiting.remove(0));
				}
				if (!wave.cargo.isEmpty()) {
					ai.zepWaves.add(wave);
				}
			}
		}
	}

	static boolean isAboard(final AIController ai, final CUnit unit) {
		for (final AIZepWave wave : ai.zepWaves) {
			if (wave.cargo.contains(unit)) {
				return true;
			}
		}
		return false;
	}

	static void updateZepWaves(final AIController ai) {
		for (final Iterator<AIZepWave> iterator = ai.zepWaves.iterator(); iterator.hasNext();) {
			final AIZepWave wave = iterator.next();
			final CUnit zeppelin = wave.zeppelin;
			wave.cargo.removeIf((unit) -> unit.isDead()
					|| (wave.flying && !zeppelin.isDead() && !zeppelin.getCargoData().contains(unit)));
			if (zeppelin.isDead()) {
				iterator.remove();
				continue;
			}
			if (!wave.flying) {
				CUnit next = null;
				for (final CUnit unit : wave.cargo) {
					if (!zeppelin.getCargoData().contains(unit)) {
						next = unit;
						break;
					}
				}
				if (next == null) {
					wave.flying = true;
					zeppelin.order(ai.simulation, OrderIds.unloadall, new AbilityPointTarget(wave.x, wave.y));
				}
				else if (isIdle(zeppelin)) {
					zeppelin.order(ai.simulation, OrderIds.load, next);
				}
			}
			else if (wave.cargo.isEmpty()) {
				sendZeppelinHome(ai, zeppelin);
				iterator.remove();
			}
			else if (isIdle(zeppelin)) {
				zeppelin.order(ai.simulation, OrderIds.unloadall, new AbilityPointTarget(wave.x, wave.y));
			}
		}
	}

	private static void sendZeppelinHome(final AIController ai, final CUnit zeppelin) {
		final AITown town = AITowns.getTown(ai, 0);
		if ((town == null) || (town.getHall() == null)) {
			return;
		}
		final float hallX = town.getHall().getX();
		final float hallY = town.getHall().getY();
		final float d = Math.max(1, distance(hallX, hallY, zeppelin.getX(), zeppelin.getY()));
		zeppelin.order(ai.simulation, OrderIds.move,
				new AbilityPointTarget(hallX + (((zeppelin.getX() - hallX) * ZEPPELIN_HOME_DISTANCE) / d),
						hallY + (((zeppelin.getY() - hallY) * ZEPPELIN_HOME_DISTANCE) / d)));
	}
}
