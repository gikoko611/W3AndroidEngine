package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.archimonde;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CWeaponSoundTypeJass;

/**
 * Finger of Death (also Finger of Pain and the item version): a bolt from the caster strikes the target for the
 * damage (Data C) at once; the bolt stays for the graphic duration (Data B).
 */
public class CAbilityFingerOfDeath extends CAbilityTargetSpellBase {
	private float graphicDuration;
	private float damage;
	private War3ID lightningId;

	public CAbilityFingerOfDeath(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.fingerofdeath;
	}

	@Override
	public void populateData(final GameObject worldEditorAbility, final int level) {
		this.graphicDuration = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.damage = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		this.lightningId = AbstractCAbilityTypeDefinition.getLightningId(worldEditorAbility, level);
	}

	@Override
	public boolean doEffect(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		final CUnit targetUnit = target.visit(AbilityTargetVisitor.UNIT);
		if (targetUnit == null) {
			return false;
		}
		if ((this.lightningId != null) && !War3ID.NONE.equals(this.lightningId)) {
			simulation.createLightning(caster, this.lightningId, targetUnit, this.graphicDuration);
		}
		simulation.createTemporarySpellEffectOnUnit(targetUnit, getAlias(), CEffectType.TARGET);
		targetUnit.damage(simulation, caster, DAMAGE_FLAGS, CAttackType.SPELLS, CDamageType.UNIVERSAL,
				CWeaponSoundTypeJass.WHOKNOWS.name(), this.damage);
		return false;
	}
}
