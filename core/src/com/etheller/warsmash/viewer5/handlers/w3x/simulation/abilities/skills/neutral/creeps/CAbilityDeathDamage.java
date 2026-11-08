package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.creeps;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CSpellDamageFlags;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CWeaponSoundTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

/**
 * Death Damage (land mines and other exploding units): the ability's duration after the unit dies it deals the full
 * damage (Data B) within the full damage radius (Data A) and the partial damage (Data D) out to the partial radius
 * (Data C).
 */
public class CAbilityDeathDamage extends CAbilityPassiveSpellBase {
	private static final CSpellDamageFlags DAMAGE_FLAGS = new CSpellDamageFlags();

	private float fullDamageRadius;
	private float fullDamageAmount;
	private float partialDamageRadius;
	private float partialDamageAmount;

	public CAbilityDeathDamage(final int handleId, final War3ID alias) {
		super(handleId, alias, alias);
	}

	@Override
	public void populateData(final GameObject worldEditorAbility, final int level) {
		this.fullDamageRadius = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.fullDamageAmount = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.partialDamageRadius = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		this.partialDamageAmount = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_D + level, 0);
	}

	@Override
	public void onDeath(final CSimulation game, final CUnit unit) {
		if (isDisabled()) {
			return;
		}
		game.createTemporarySpellEffectOnUnit(unit, getAlias(), CEffectType.CASTER);
		final float x = unit.getX();
		final float y = unit.getY();
		final CTimer blast = new CTimer() {
			@Override
			public void onFire(final CSimulation simulation) {
				explode(simulation, unit, x, y);
			}
		};
		blast.setTimeoutTime(getDuration());
		blast.start(game);
	}

	private void explode(final CSimulation game, final CUnit unit, final float x, final float y) {
		final float radius = StrictMath.max(this.partialDamageRadius, this.fullDamageRadius);
		game.getWorldCollision().enumUnitsInRange(x, y, radius, (enumUnit) -> {
			if ((enumUnit != unit) && enumUnit.canBeTargetedBy(game, unit, false, getTargetsAllowed(),
					BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) {
				final float damage = enumUnit.canReach(x, y, this.fullDamageRadius) ? this.fullDamageAmount
						: this.partialDamageAmount;
				// Measured on 1.29: armor reduces the damage and heroes take 70 % (Footman, Knight, Ghoul, Farm and
				// Guard Tower alike), as with the spells attack type.
				enumUnit.damage(game, unit, DAMAGE_FLAGS, CAttackType.SPELLS, CDamageType.NORMAL,
						CWeaponSoundTypeJass.WHOKNOWS.name(), damage);
			}
			return false;
		});
	}
}
