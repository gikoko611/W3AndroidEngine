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
import java.util.HashSet;
import java.util.Set;

/**
 * Righteous Fury: a dash at Data A speed to a point up to the cast range away. Enemies in the area along the way take
 * Data B damage once and are slowed (Data I movement, Data J attack rate).
 */
public class CAbilityRighteousFury extends CAbilityPointTargetSpellBase {
	private float speed;
	private float damage;
	private float moveSlow;
	private float attackSlow;
	private float area;
	private float dashRange;
	private War3ID buffId;
	private float targetX;
	private float targetY;
	private final Set<CUnit> hit = new HashSet<>();

	public CAbilityRighteousFury(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.righteousfury;
	}

	@Override
	public void populateData(final GameObject worldEditorAbility, final int level) {
		this.speed = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.damage = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.moveSlow = worldEditorAbility.getFieldAsFloat("DataI" + level, 0);
		this.attackSlow = worldEditorAbility.getFieldAsFloat("DataJ" + level, 0);
		this.area = worldEditorAbility.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		this.dashRange = worldEditorAbility.getFieldAsFloat(AbilityFields.CAST_RANGE + level, 0);
		this.buffId = AbstractCAbilityTypeDefinition.getBuffId(worldEditorAbility, level);
	}

	@Override
	public float getCastRange() {
		// A farther point shortens the dash instead of making the hero walk first.
		return Float.MAX_VALUE / 2;
	}

	@Override
	public boolean doEffect(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		final float dx = target.getX() - caster.getX();
		final float dy = target.getY() - caster.getY();
		final float distance = (float) StrictMath.sqrt((dx * dx) + (dy * dy));
		final float length = Math.min(distance, this.dashRange);
		final float scale = distance > 0 ? length / distance : 0;
		this.targetX = caster.getX() + (dx * scale);
		this.targetY = caster.getY() + (dy * scale);
		this.hit.clear();
		caster.setFacing((float) StrictMath.toDegrees(StrictMath.atan2(dy, dx)));
		simulation.createTemporarySpellEffectOnUnit(caster, getAlias(), CEffectType.CASTER);
		return true;
	}

	@Override
	public boolean doChannelTick(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		final float dx = this.targetX - caster.getX();
		final float dy = this.targetY - caster.getY();
		final float remaining = (float) StrictMath.sqrt((dx * dx) + (dy * dy));
		final float step = this.speed * WarsmashConstants.SIMULATION_STEP_TIME;
		final boolean arriving = remaining <= step;
		final float nextX = arriving ? this.targetX : caster.getX() + ((dx / remaining) * step);
		final float nextY = arriving ? this.targetY : caster.getY() + ((dy / remaining) * step);
		if (!simulation.getPathingGrid().isPathable(nextX, nextY, caster.getMovementType())) {
			return false;
		}
		caster.setPointAndCheckUnstuck(nextX, nextY, simulation);
		simulation.getWorldCollision().enumUnitsInRange(caster.getX(), caster.getY(), this.area, (enumUnit) -> {
			if ((enumUnit != caster) && !this.hit.contains(enumUnit)
					&& !enumUnit.isUnitAlly(simulation.getPlayer(caster.getPlayerIndex()))
					&& enumUnit.canBeTargetedBy(simulation, caster, getTargetsAllowed())) {
				this.hit.add(enumUnit);
				enumUnit.add(simulation, new CBuffSlow(simulation.getHandleIdAllocator().createId(), this.buffId,
						getDurationForTarget(enumUnit), this.attackSlow, this.moveSlow));
				enumUnit.damage(simulation, caster, DAMAGE_FLAGS, CAttackType.SPELLS, CDamageType.UNIVERSAL,
						CWeaponSoundTypeJass.WHOKNOWS.name(), this.damage);
			}
			return false;
		});
		return !arriving;
	}
}
