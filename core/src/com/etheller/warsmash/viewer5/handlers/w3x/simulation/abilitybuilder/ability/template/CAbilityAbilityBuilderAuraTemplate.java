package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.ability.template;

import java.util.List;
import java.util.Map;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.CBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.ABAction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.types.impl.CAbilityTypeAbilityBuilderLevelData;

public class CAbilityAbilityBuilderAuraTemplate extends AbstractCAbilityAbilityBuilderAuraTemplate {
	private CBuff buff;

	private List<ABAction> addToAuraActions;
	private List<ABAction> updateAuraLevelActions;
	private List<ABAction> removeFromAuraActions;

	public CAbilityAbilityBuilderAuraTemplate(int handleId, War3ID code, War3ID alias,
			List<CAbilityTypeAbilityBuilderLevelData> levelData, Map<String, Object> localStore,
			List<ABAction> addToAuraActions, List<ABAction> updateAuraLevelActions,
			List<ABAction> removeFromAuraActions) {
		super(handleId, code, alias, levelData, localStore);
		this.addToAuraActions = addToAuraActions;
		this.updateAuraLevelActions = updateAuraLevelActions;
		this.removeFromAuraActions = removeFromAuraActions;
	}

	@Override
	public void setLevel(CSimulation game, CUnit unit, int level) {
		super.setLevel(game, unit, level);
		targetsAllowed = levelData.get(getLevel()).getTargetsAllowed();
		range = levelData.get(getLevel()).getCastRange();
	}

	@Override
	protected boolean tracksLevelChanges() {
		return true;
	}

	@Override
	public void addUnitToAura(CSimulation game, CUnit unit) {
		auraGroup.add(unit);
		if (addToAuraActions != null) {
			for (ABAction action : addToAuraActions) {
				action.runAction(game, unit, localStore, 0);
			}
		}
		unit.add(game, buff);
	}

	@Override
	public void updateLevelOfAura(CSimulation game, CUnit unit, int prevLevel, int curLevel) {
		if (updateAuraLevelActions != null) {
			for (ABAction action : updateAuraLevelActions) {
				action.runAction(game, unit, localStore, 0);
			}
		}
	}

	@Override
	public void removeUnitFromAura(CSimulation game, CUnit unit) {
		unit.remove(game, buff);
		if (removeFromAuraActions != null) {
			for (ABAction action : removeFromAuraActions) {
				action.runAction(game, unit, localStore, 0);
			}
		}
		auraGroup.remove(unit);
	}
}
