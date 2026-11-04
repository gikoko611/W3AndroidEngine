package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.cryptlord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttack;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackPostDamageListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.replacement.CUnitPriorityLoopData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponent;

/**
 * Locust Swarm: while channeled, releases a locust every interval up to the swarm size. A locust swarms around the
 * caster, flies to an enemy within the area (at most a set number on one target), bites it once and swarms off again.
 * Once it carries enough damage it flies back to the caster and heals it for the return factor of what it carried.
 */
public class CAbilityLocustSwarm extends CAbilityNoTargetSpellBase {
	private static final float ARRIVAL_RANGE = 48f;
	private static final float RETURN_RANGE = 128f;
	private static final float REORDER_INTERVAL = 6f;
	/*
	 * Measured with one locust: it does not fly back at the bite that reaches the return threshold but bites on, mostly
	 * twice more (thresholds 10, 20 and 40 on Knights, Footmen and Gryphon Riders: 1 to 3 more bites, 2 in four of six).
	 */
	private static final int BITES_AFTER_THRESHOLD = 2;

	private int swarmSize;
	private float releaseInterval;
	private int maxPerTarget;
	private float returnFactor;
	private float returnThreshold;
	private War3ID locustType;
	private float areaOfEffect;
	private int channelEndTick;
	private SimulationRenderComponent loopSound;
	private int nextReleaseTick;
	private final Map<CUnit, Locust> locusts = new LinkedHashMap<>();

	public CAbilityLocustSwarm(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.locustswarm;
	}

	@Override
	public void populateData(final GameObject worldEditorAbility, final int level) {
		this.swarmSize = worldEditorAbility.getFieldAsInteger(AbilityFields.DATA_A + level, 0);
		this.releaseInterval = Math.max(0.05f, worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_B + level, 0));
		this.maxPerTarget = worldEditorAbility.getFieldAsInteger(AbilityFields.DATA_C + level, 0);
		this.returnFactor = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_D + level, 0);
		this.returnThreshold = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_E + level, 0);
		this.locustType = War3ID.fromString(worldEditorAbility.getFieldAsString(AbilityFields.UNIT_ID + level, 0));
		this.areaOfEffect = worldEditorAbility.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
	}

	@Override
	public boolean doEffect(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		final int tick = simulation.getGameTurnTick();
		this.channelEndTick = tick + ticks(getDuration());
		this.nextReleaseTick = tick;
		this.locusts.clear();
		simulation.createTemporarySpellEffectOnUnit(caster, getAlias(), CEffectType.CASTER);
		stopLoopSound();
		this.loopSound = simulation.unitLoopSoundEffectEvent(caster, getAlias());
		return true;
	}

	@Override
	public boolean doChannelTick(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		final int tick = simulation.getGameTurnTick();
		if ((tick >= this.nextReleaseTick) && (this.locusts.size() < this.swarmSize)) {
			this.nextReleaseTick = tick + ticks(this.releaseInterval);
			final CUnit locustUnit = simulation.createUnit(this.locustType, caster.getPlayerIndex(), caster.getX(),
					caster.getY(), caster.getFacing());
			if (locustUnit != null) {
				final Locust locust = new Locust(locustUnit);
				locustUnit.addPostDamageListener(locust);
				this.locusts.put(locustUnit, locust);
				wander(simulation, caster, locust);
			}
		}
		updateLocusts(simulation, caster);
		return tick < this.channelEndTick;
	}

	private void updateLocusts(final CSimulation simulation, final CUnit caster) {
		final int tick = simulation.getGameTurnTick();
		final Map<CUnit, Integer> attackersPerTarget = new HashMap<>();
		for (final Iterator<Locust> iterator = this.locusts.values().iterator(); iterator.hasNext();) {
			final Locust locust = iterator.next();
			if (locust.unit.isDead()) {
				iterator.remove();
				continue;
			}
			if ((locust.state == LocustState.ATTACK) && !isValidTarget(simulation, caster, locust.target)) {
				locust.target = null;
				wander(simulation, caster, locust);
			}
			if (locust.state == LocustState.ATTACK) {
				attackersPerTarget.merge(locust.target, 1, Integer::sum);
			}
		}
		for (final Locust locust : this.locusts.values()) {
			switch (locust.state) {
			case WANDER:
				if ((locust.unit.distance(locust.wanderX, locust.wanderY) <= ARRIVAL_RANGE)
						|| (tick >= locust.reorderTick)) {
					final CUnit target = pickTarget(simulation, caster, attackersPerTarget);
					if (target != null) {
						attackersPerTarget.merge(target, 1, Integer::sum);
						locust.state = LocustState.ATTACK;
						locust.target = target;
						locust.reorderTick = tick + ticks(REORDER_INTERVAL);
						locust.unit.order(simulation, OrderIds.attack, target);
					}
					else {
						wander(simulation, caster, locust);
					}
				}
				break;
			case ATTACK:
				if (locust.bitten) {
					locust.bitten = false;
					locust.target = null;
					if (locust.carried >= this.returnThreshold) {
						locust.bitesAfterThreshold++;
					}
					if (locust.bitesAfterThreshold > BITES_AFTER_THRESHOLD) {
						locust.state = LocustState.RETURN;
						locust.reorderTick = tick + ticks(REORDER_INTERVAL);
						locust.unit.order(simulation, OrderIds.move, new AbilityPointTarget(caster.getX(), caster.getY()));
					}
					else {
						wander(simulation, caster, locust);
					}
				}
				else if (tick >= locust.reorderTick) {
					locust.reorderTick = tick + ticks(REORDER_INTERVAL);
					locust.unit.order(simulation, OrderIds.attack, locust.target);
				}
				break;
			case RETURN:
				if (locust.unit.distance(caster) <= RETURN_RANGE) {
					caster.heal(simulation, locust.carried * this.returnFactor);
					locust.carried = 0;
					locust.bitesAfterThreshold = 0;
					wander(simulation, caster, locust);
				}
				else if (tick >= locust.reorderTick) {
					locust.reorderTick = tick + ticks(REORDER_INTERVAL);
					locust.unit.order(simulation, OrderIds.move, new AbilityPointTarget(caster.getX(), caster.getY()));
				}
				break;
			}
		}
	}

	/**
	 * Measured: between bites a locust flies out to anywhere in the area around the caster before it turns to a
	 * target, so each one bites every two to four seconds rather than at its attack rate.
	 */
	private void wander(final CSimulation simulation, final CUnit caster, final Locust locust) {
		final Random random = simulation.getSeededRandom();
		final double angle = random.nextDouble() * StrictMath.PI * 2;
		final double distance = random.nextDouble() * this.areaOfEffect;
		locust.state = LocustState.WANDER;
		locust.wanderX = caster.getX() + (float) (StrictMath.cos(angle) * distance);
		locust.wanderY = caster.getY() + (float) (StrictMath.sin(angle) * distance);
		locust.reorderTick = simulation.getGameTurnTick() + ticks(REORDER_INTERVAL);
		locust.unit.order(simulation, OrderIds.move, new AbilityPointTarget(locust.wanderX, locust.wanderY));
	}

	private boolean isValidTarget(final CSimulation simulation, final CUnit caster, final CUnit target) {
		return (target != null) && !target.isDead() && (target.distance(caster) <= this.areaOfEffect)
				&& target.canBeTargetedBy(simulation, caster, getTargetsAllowed());
	}

	private CUnit pickTarget(final CSimulation simulation, final CUnit caster,
			final Map<CUnit, Integer> attackersPerTarget) {
		final List<CUnit> candidates = new ArrayList<>();
		simulation.getWorldCollision().enumUnitsInRange(caster.getX(), caster.getY(), this.areaOfEffect,
				(enumUnit) -> {
					if (enumUnit.canBeTargetedBy(simulation, caster, getTargetsAllowed())
							&& (attackersPerTarget.getOrDefault(enumUnit, 0) < this.maxPerTarget)) {
						candidates.add(enumUnit);
					}
					return false;
				});
		if (candidates.isEmpty()) {
			return null;
		}
		return candidates.get(simulation.getSeededRandom().nextInt(candidates.size()));
	}

	@Override
	public void doChannelEnd(final CSimulation game, final CUnit unit, final AbilityTarget target,
			final boolean interrupted) {
		for (final CUnit locust : this.locusts.keySet()) {
			if (!locust.isDead()) {
				locust.kill(game);
			}
		}
		this.locusts.clear();
		stopLoopSound();
	}

	private void stopLoopSound() {
		if (this.loopSound != null) {
			this.loopSound.remove();
			this.loopSound = null;
		}
	}

	private static int ticks(final float seconds) {
		return (int) StrictMath.ceil(seconds / WarsmashConstants.SIMULATION_STEP_TIME);
	}

	private enum LocustState {
		WANDER, ATTACK, RETURN;
	}

	private static final class Locust implements CUnitAttackPostDamageListener {
		private final CUnit unit;
		private LocustState state = LocustState.WANDER;
		private CUnit target;
		private float carried;
		private int bitesAfterThreshold;
		private boolean bitten;
		private float wanderX;
		private float wanderY;
		private int reorderTick;

		private Locust(final CUnit unit) {
			this.unit = unit;
		}

		@Override
		public void onHit(final CSimulation simulation, final CUnit attacker, final AbilityTarget hitTarget,
				final CUnitAttack cUnitAttack, final float damage, final CUnitPriorityLoopData postListenerLoop) {
			this.carried += damage;
			this.bitten = true;
		}

		@Override
		public int getPriority(final CSimulation simulation, final CUnit attacker, final AbilityTarget hitTarget,
				final CUnitAttack cUnitAttack) {
			return 0;
		}
	}
}
