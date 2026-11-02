package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.forsakenpaladin;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CAbilityAuraBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffAuraBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;

/** Sacred Aura: magic resistance (Data A) and more healing received in percent (Data B). */
public class CAbilitySacredAura extends CAbilityAuraBase {
	private float magicResistance;
	private float healingFactor;

	public CAbilitySacredAura(final int handleId, final War3ID code, final War3ID alias) {
		super(handleId, code, alias);
	}

	@Override
	public void populateAuraData(final GameObject worldEditorAbility, final int level) {
		this.magicResistance = worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.healingFactor = 1f + (worldEditorAbility.getFieldAsFloat(AbilityFields.DATA_B + level, 0) / 100f);
	}

	@Override
	protected CBuffAuraBase createBuff(final int handleId, final CUnit source, final CUnit enumUnit) {
		return new CBuffSacredAura(handleId, getBuffId(), this.magicResistance, this.healingFactor);
	}
}
