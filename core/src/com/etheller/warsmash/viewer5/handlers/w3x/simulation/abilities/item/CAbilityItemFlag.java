package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentModel;

/**
 * Flags (the capture-the-flag items and the Orc Battle Standard): no game effect, the holder only carries the
 * flag's model at the ability's target attachment point while it has the item.
 */
public class CAbilityItemFlag extends CAbilityPassiveSpellBase {
	private SimulationRenderComponentModel flagArt;

	public CAbilityItemFlag(final int handleId, final War3ID alias) {
		super(handleId, alias, alias);
	}

	@Override
	public void populateData(final GameObject worldEditorAbility, final int level) {
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		this.flagArt = game.createPersistentSpellEffectOnUnit(unit, getAlias(), CEffectType.TARGET);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		if (this.flagArt != null) {
			this.flagArt.remove();
			this.flagArt = null;
		}
	}
}
