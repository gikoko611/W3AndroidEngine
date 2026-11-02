package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.forsakenpaladin;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPointTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffSlow;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffStun;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CWeaponSoundTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.CUnitTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentModel;

/**
 * Consecration: the ground around the caster heals friendly living units (Data A) and burns enemy Undead (Data B) every
 * second, and those Undead receive less healing (Data C, percent).
 */
public class CAbilityConsecration extends CAbilityNoTargetSpellBase {
	private static final War3ID ALLY_BUFF = War3ID.fromString("BHcf");
	private static final War3ID ENEMY_BUFF = War3ID.fromString("BHce");
	private static final float INTERVAL = 1f;

	private float heal;
	private float damage;
	private float healingReduction;
	private float area;

	public CAbilityConsecration(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.consecration;
	}

	@Override
	public void populateData(final GameObject worldEditorAbility, final int level) {
		this.heal = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.damage = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.healingReduction = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_C + level, 0) / 100f;
		this.area = worldEditorAbility.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
	}

	@Override
	public boolean doEffect(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		final float x = caster.getX();
		final float y = caster.getY();
		final float heal = this.heal;
		final float damage = this.damage;
		final float healingReduction = this.healingReduction;
		final float area = this.area;
		final SimulationRenderComponentModel ground = simulation.spawnSpellEffectOnPoint(x, y, 0, getAlias(),
				CEffectType.AREA_EFFECT, 0);
		final int pulses = Math.max(1, Math.round(getDuration() / INTERVAL));
		final CTimer timer = new CTimer() {
			private int pulse;

			@Override
			public void onFire(final CSimulation game) {
				game.getWorldCollision().enumUnitsInRange(x, y, area, (enumUnit) -> {
					if (enumUnit.isDead() || enumUnit.isUnitType(CUnitTypeJass.MECHANICAL) || enumUnit.isBuilding()) {
						return false;
					}
					final boolean undead = enumUnit.isUnitType(CUnitTypeJass.UNDEAD);
					final boolean ally = enumUnit.isUnitAlly(game.getPlayer(caster.getPlayerIndex()));
					if (ally && !undead) {
						enumUnit.heal(game, heal);
						refresh(game, enumUnit, ALLY_BUFF, 1f, true);
					}
					else if (!ally && undead) {
						enumUnit.damage(game, caster, DAMAGE_FLAGS, CAttackType.SPELLS, CDamageType.UNIVERSAL,
								CWeaponSoundTypeJass.WHOKNOWS.name(), damage);
						refresh(game, enumUnit, ENEMY_BUFF, 1f - healingReduction, false);
					}
					return false;
				});
				this.pulse++;
				if (this.pulse >= pulses) {
					pause(game);
					if (ground != null) {
						ground.remove();
					}
				}
			}
		};
		timer.setRepeats(true);
		timer.setTimeoutTime(INTERVAL);
		timer.start(simulation);
		simulation.createTemporarySpellEffectOnUnit(caster, getAlias(), CEffectType.CASTER);
		return false;
	}

	private static void refresh(final CSimulation simulation, final CUnit unit, final War3ID buffId, final float factor,
			final boolean positive) {
		for (final CAbility ability : unit.getAbilities()) {
			if ((ability instanceof CBuffHealingModifier) && ability.getAlias().equals(buffId)) {
				unit.remove(simulation, (CBuffHealingModifier) ability);
				break;
			}
		}
		unit.add(simulation, new CBuffHealingModifier(simulation.getHandleIdAllocator().createId(), buffId,
				INTERVAL * 1.5f, factor, positive));
	}
}
