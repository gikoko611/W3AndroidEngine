package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.malganis;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

/**
 * Dark Conversion (Mal'Ganis): a unit of the race in Data A (villagers, "commoner") is stunned under the buff for
 * the duration and then becomes the Unit ID type in place, keeping its handle, owner and facing; the change fires
 * the summon event for the caster's player. The "Cast" field is a casting time before the cast event.
 */
public class CAbilityDarkConversion extends CAbilityTargetSpellBase {
	private String targetRace;
	private War3ID convertedUnitId;
	private War3ID buffId;

	public CAbilityDarkConversion(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public void populateData(final GameObject worldEditorAbility, final int level) {
		this.targetRace = worldEditorAbility.getFieldAsString(AbilityFields.DATA_A + level, 0);
		this.convertedUnitId = War3ID.fromString(worldEditorAbility.getFieldAsString(AbilityFields.UNIT_ID + level, 0));
		this.buffId = AbstractCAbilityTypeDefinition.getBuffId(worldEditorAbility, level);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.darkconversion;
	}

	@Override
	public boolean isCastingTimeBeforeCast() {
		return true;
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit unit, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		final CUnit targetUnit = target.visit(AbilityTargetVisitor.UNIT);
		if ((targetUnit == null) || (this.targetRace == null)
				|| !this.targetRace.equalsIgnoreCase(game.getUnitData().getRaceName(targetUnit.getTypeId()))) {
			receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_UNIT_WITH_THIS_ACTION);
		}
		else {
			receiver.targetOk(target);
		}
	}

	@Override
	public boolean doEffect(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		final CUnit targetUnit = target.visit(AbilityTargetVisitor.UNIT);
		if ((targetUnit == null) || targetUnit.isDead()) {
			return false;
		}
		targetUnit.add(simulation, new CBuffDarkConversion(simulation.getHandleIdAllocator().createId(), this.buffId,
				getDurationForTarget(targetUnit), caster, this.convertedUnitId));
		return false;
	}

	public String getTargetRace() {
		return this.targetRace;
	}

	public War3ID getConvertedUnitId() {
		return this.convertedUnitId;
	}
}
