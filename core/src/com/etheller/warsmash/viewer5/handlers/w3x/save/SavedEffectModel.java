package com.etheller.warsmash.viewer5.handlers.w3x.save;

import java.util.ArrayList;
import java.util.List;

import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderSpellEffect;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.SpellEffectModel;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentModel;

/**
 * Takes the place of the effects an ability holds in a saved game, so that the ability can still end them.
 */
final class SavedEffectModel implements SimulationRenderComponentModel {
	private final List<SavedEffect> effects = new ArrayList<>();
	private final float baseHeight;

	SavedEffectModel(final SpellEffectModel model) {
		this.baseHeight = model.getBaseHeight();
		for (final RenderSpellEffect effect : model.getEffects()) {
			this.effects.add(SavedEffect.of(effect));
		}
	}

	@Override
	public void remove() {
		for (final SavedEffect effect : this.effects) {
			effect.remove();
		}
	}

	@Override
	public void setHeight(final float height) {
		for (final SavedEffect effect : this.effects) {
			effect.setHeight(this.baseHeight + height);
		}
	}
}
