package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.keeper;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffSpellArt;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponent;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentModel;

/**
 * Tranquility: while channeled, heals every friendly unit around the caster steadily, as the original game does,
 * rather than in pulses. The healing rain stands on the caster and every unit in it, the caster included, carries the
 * spell's buff while it stays inside.
 */
public class CAbilityTranquility extends CAbilityNoTargetSpellBase {
	private float healPerSecond;
	private float areaOfEffect;
	private War3ID buffId;
	private War3ID effectId;
	private int channelEndTick;
	private SimulationRenderComponentModel rain;
	private SimulationRenderComponent rainSound;
	private final Map<CUnit, CBuffSpellArt> buffed = new LinkedHashMap<>();

	public CAbilityTranquility(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.tranquility;
	}

	@Override
	public void populateData(final GameObject worldEditorAbility, final int level) {
		this.healPerSecond = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.areaOfEffect = worldEditorAbility.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		this.buffId = AbstractCAbilityTypeDefinition.getBuffId(worldEditorAbility, level);
		this.effectId = AbstractCAbilityTypeDefinition.getEffectId(worldEditorAbility, level);
	}

	@Override
	public boolean doEffect(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		final int tick = simulation.getGameTurnTick();
		this.channelEndTick = tick + ticks(getDuration());
		simulation.createTemporarySpellEffectOnUnit(caster, getAlias(), CEffectType.CASTER);
		removeRain();
		if (isSet(this.effectId)) {
			this.rain = simulation.spawnSpellEffectOnPoint(caster.getX(), caster.getY(), caster.getFacing(),
					this.effectId, CEffectType.EFFECT, 0);
			this.rainSound = simulation.unitLoopSoundEffectEvent(caster, this.effectId);
		}
		// Measured: the first healing comes one step after the buffs appear.
		healAround(simulation, caster, 0);
		return true;
	}

	@Override
	public boolean doChannelTick(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		final int tick = simulation.getGameTurnTick();
		healAround(simulation, caster, this.healPerSecond * WarsmashConstants.SIMULATION_STEP_TIME);
		return tick < this.channelEndTick;
	}

	@Override
	public void doChannelEnd(final CSimulation game, final CUnit unit, final AbilityTarget target,
			final boolean interrupted) {
		for (final Map.Entry<CUnit, CBuffSpellArt> entry : this.buffed.entrySet()) {
			if (entry.getKey().getAbilities().contains(entry.getValue())) {
				entry.getKey().remove(game, entry.getValue());
			}
		}
		this.buffed.clear();
		removeRain();
	}

	private void removeRain() {
		if (this.rain != null) {
			this.rain.remove();
			this.rain = null;
		}
		if (this.rainSound != null) {
			this.rainSound.remove();
			this.rainSound = null;
		}
	}

	private void healAround(final CSimulation simulation, final CUnit caster, final float heal) {
		final Set<CUnit> inArea = new LinkedHashSet<>();
		simulation.getWorldCollision().enumUnitsInRange(caster.getX(), caster.getY(), this.areaOfEffect,
				(enumUnit) -> {
					if (enumUnit.isUnitAlly(simulation.getPlayer(caster.getPlayerIndex()))
							&& enumUnit.canBeTargetedBy(simulation, caster, getTargetsAllowed())) {
						if (heal > 0) {
							enumUnit.heal(simulation, heal);
						}
						inArea.add(enumUnit);
					}
					return false;
				});
		if (!isSet(this.buffId)) {
			return;
		}
		for (final Iterator<Map.Entry<CUnit, CBuffSpellArt>> iterator = this.buffed.entrySet().iterator(); iterator
				.hasNext();) {
			final Map.Entry<CUnit, CBuffSpellArt> entry = iterator.next();
			final boolean attached = entry.getKey().getAbilities().contains(entry.getValue());
			if (!attached || !inArea.contains(entry.getKey())) {
				if (attached) {
					entry.getKey().remove(simulation, entry.getValue());
				}
				iterator.remove();
			}
		}
		for (final CUnit unit : inArea) {
			if (!this.buffed.containsKey(unit)) {
				final CBuffSpellArt buff = new CBuffSpellArt(simulation.getHandleIdAllocator().createId(),
						this.buffId, true, true);
				unit.add(simulation, buff);
				this.buffed.put(unit, buff);
			}
		}
	}

	private static boolean isSet(final War3ID id) {
		return (id != null) && !War3ID.NONE.equals(id);
	}

	private static int ticks(final float seconds) {
		return CTimer.ceilSteps(seconds);
	}
}
