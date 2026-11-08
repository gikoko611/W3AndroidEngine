package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.ability.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.CBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.CLevelingAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.types.impl.CAbilityTypeAbilityBuilderLevelData;

public class CAbilityAbilityBuilderSimpleAuraTemplate extends AbstractCAbilityAbilityBuilderAuraTemplate {
	private CBuff buff;

	private Map<Integer,List<War3ID>> abilityIdsToAddPerLevel;
	private List<War3ID> levellingAbilityIdsToAdd;

	private Map<Integer,List<CAbility>> abilitiesToAddPerLevel;
	private List<CLevelingAbility> levellingAbilitiesToAdd;

	public CAbilityAbilityBuilderSimpleAuraTemplate(int handleId, War3ID code, War3ID alias,
			List<CAbilityTypeAbilityBuilderLevelData> levelData, Map<String, Object> localStore,
			Map<Integer,List<War3ID>> abilityIdsToAddPerLevel, List<War3ID> levellingAbilityIdsToAdd) {
		super(handleId, code, alias, levelData, localStore);
		this.abilityIdsToAddPerLevel = abilityIdsToAddPerLevel;
		this.levellingAbilityIdsToAdd = levellingAbilityIdsToAdd;
	}

	@Override
	public void setLevel(CSimulation game, CUnit unit, int level) {
		super.setLevel(game, unit, level);
		targetsAllowed = levelData.get(getLevel()).getTargetsAllowed();
		range = levelData.get(getLevel()).getCastRange();
	}

	@Override
	protected void beforeAuraStarts(CSimulation game, CUnit unit) {
		game.getAbilityData().createAbility(getAlias(), game.getHandleIdAllocator().createId());
	}

	@Override
	protected void onAuraStarted(CSimulation game, CUnit unit) {
		this.abilitiesToAddPerLevel = new HashMap<>();
		this.levellingAbilitiesToAdd = new ArrayList<>();
		if (abilityIdsToAddPerLevel != null) {
			for (Integer i : abilityIdsToAddPerLevel.keySet()) {
				List<CAbility> list = new ArrayList<>();
				abilitiesToAddPerLevel.put(i, list);
				for (War3ID abilityId : abilityIdsToAddPerLevel.get(i)) {
					list.add(game.getAbilityData().getAbilityType(abilityId)
							.createAbility(game.getHandleIdAllocator().createId()));
				}
			}
		}
		if (levellingAbilityIdsToAdd != null) {
			for (War3ID abilityId : levellingAbilityIdsToAdd) {
				CAbility abil = game.getAbilityData().getAbilityType(abilityId)
						.createAbility(game.getHandleIdAllocator().createId());
				if (abil instanceof CLevelingAbility) {
					levellingAbilitiesToAdd.add((CLevelingAbility) abil);
				}
			}
		}
	}

	@Override
	protected boolean tracksLevelChanges() {
		return true;
	}

	@Override
	public void addUnitToAura(CSimulation game, CUnit unit) {
		auraGroup.add(unit);
		if (abilitiesToAddPerLevel != null) {
			for (CAbility ability : abilitiesToAddPerLevel.get(getLevel())) {
				unit.add(game, ability);
			}
		}
		if (levellingAbilitiesToAdd != null) {
			for (CAbility ability : levellingAbilitiesToAdd) {
				unit.add(game, ability);
			}
		}
		unit.add(game, buff);
	}

	@Override
	public void updateLevelOfAura(CSimulation game, CUnit unit, int prevLevel, int curLevel) {
		if (abilitiesToAddPerLevel != null) {
			for (CAbility ability : abilitiesToAddPerLevel.get(prevLevel)) {
				unit.remove(game, ability);
			}
		}
		if (abilitiesToAddPerLevel != null) {
			for (CAbility ability : abilitiesToAddPerLevel.get(curLevel)) {
				unit.add(game, ability);
			}
		}
		if (levellingAbilitiesToAdd != null) {
			for (CLevelingAbility ability : levellingAbilitiesToAdd) {
				ability.setLevel(game, unit, curLevel);
			}
		}
	}

	@Override
	public void removeUnitFromAura(CSimulation game, CUnit unit) {
		unit.remove(game, buff);
		if (abilitiesToAddPerLevel != null) {
			for (CAbility ability : abilitiesToAddPerLevel.get(getLevel())) {
				unit.remove(game, ability);
			}
		}
		if (levellingAbilitiesToAdd != null) {
			for (CAbility ability : levellingAbilitiesToAdd) {
				unit.remove(game, ability);
			}
		}
		auraGroup.remove(unit);
	}
}
