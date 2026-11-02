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
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.CBuff;
import java.util.ArrayList;
import java.util.List;

/**
 * Cleansing Fire: friends lose their debuffs, heal (Data A, plus Data B per debuff) and deal more damage for Data C
 * seconds (Data D percent, plus Data E per debuff). Enemies lose their buffs and are stunned for Data F seconds;
 * summoned enemies also take Data G damage.
 */
public class CAbilityCleansingFire extends CAbilityNoTargetSpellBase {
	private static final War3ID BUFF = War3ID.fromString("BHcl");
	private static final War3ID STUN_BUFF = War3ID.fromString("BPSE");
	private float heal;
	private float healPerDebuff;
	private float bonusDuration;
	private float damageBonus;
	private float damageBonusPerDebuff;
	private float stunDuration;
	private float summonDamage;
	private float area;

	public CAbilityCleansingFire(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.cleansingfire;
	}

	@Override
	public void populateData(final GameObject worldEditorAbility, final int level) {
		this.heal = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.healPerDebuff = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.bonusDuration = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		this.damageBonus = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_D + level, 0) / 100f;
		this.damageBonusPerDebuff = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_E + level, 0) / 100f;
		this.stunDuration = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_F + level, 0);
		this.summonDamage = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_G + level, 0);
		this.area = worldEditorAbility.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
	}

	@Override
	public boolean doEffect(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		simulation.createTemporarySpellEffectOnUnit(caster, getAlias(), CEffectType.AREA_EFFECT);
		simulation.getWorldCollision().enumUnitsInRange(caster.getX(), caster.getY(), this.area, (enumUnit) -> {
			if (enumUnit.isDead() || enumUnit.isBuilding() || enumUnit.isMagicImmune()) {
				return false;
			}
			final boolean ally = enumUnit.isUnitAlly(simulation.getPlayer(caster.getPlayerIndex()));
			final int removed = dispel(simulation, enumUnit, !ally);
			if (ally) {
				enumUnit.heal(simulation, this.heal + (removed * this.healPerDebuff));
				enumUnit.add(simulation,
						new CBuffCleansingFire(simulation.getHandleIdAllocator().createId(), BUFF, this.bonusDuration,
								this.damageBonus + (removed * this.damageBonusPerDebuff)));
			}
			else {
				enumUnit.add(simulation,
						new CBuffStun(simulation.getHandleIdAllocator().createId(), STUN_BUFF, this.stunDuration));
				if (enumUnit.isUnitType(CUnitTypeJass.SUMMONED)) {
					enumUnit.damage(simulation, caster, DAMAGE_FLAGS, CAttackType.SPELLS, CDamageType.UNIVERSAL,
							CWeaponSoundTypeJass.WHOKNOWS.name(), this.summonDamage);
				}
			}
			return false;
		});
		return false;
	}

	private static int dispel(final CSimulation simulation, final CUnit unit, final boolean positiveBuffs) {
		final List<CBuff> buffs = new ArrayList<>();
		for (final CAbility ability : unit.getAbilities()) {
			if (ability.getAbilityCategory() == CAbilityCategory.BUFF) {
				final CBuff buff = (CBuff) ability;
				if (buff.isDispellable() && (buff.isPositive() == positiveBuffs)) {
					buffs.add(buff);
				}
			}
		}
		for (final CBuff buff : buffs) {
			unit.remove(simulation, buff);
		}
		return buffs.size();
	}
}
