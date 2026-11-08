package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl;

import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.CLevelingAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.units.CAbilitySpikedBarricades;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.CAbilityType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.CAbilityTypeLevelData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.CAbilityTypeDefinition;

public class CAbilityTypeDefinitionSpikedBarricades extends AbstractCAbilityTypeDefinition<CAbilityTypeLevelData>
		implements CAbilityTypeDefinition {

	@Override
	protected CAbilityTypeLevelData createLevelData(final GameObject abilityEditorData, final int level) {
		return new CAbilityTypeLevelData(getTargetsAllowed(abilityEditorData, level));
	}

	@Override
	protected CAbilityType<?> innerCreateAbilityType(final War3ID alias, final GameObject abilityEditorData,
			final List<CAbilityTypeLevelData> levelData) {
		return new CAbilityType<CAbilityTypeLevelData>(alias, abilityEditorData.getFieldAsWar3ID(CODE, -1),
				levelData) {
			@Override
			public CAbility createAbility(final int handleId) {
				return new CAbilitySpikedBarricades(handleId, getCode(), getAlias());
			}

			@Override
			public void setLevel(final CSimulation game, final CUnit unit, final CLevelingAbility existingAbility,
					final int level) {
				existingAbility.setLevel(game, unit, level);
			}
		};
	}

}
