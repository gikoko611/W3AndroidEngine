package com.etheller.warsmash.viewer5.handlers.w3x.rendersim;

import java.util.List;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentModel;

/**
 * The lasting spell effects an ability holds and ends later, with the height its setHeight counts from.
 */
public final class SpellEffectModel implements SimulationRenderComponentModel {
	private final List<RenderSpellEffect> effects;
	private final float baseHeight;

	public SpellEffectModel(final List<RenderSpellEffect> effects, final float baseHeight) {
		this.effects = effects;
		this.baseHeight = baseHeight;
	}

	public List<RenderSpellEffect> getEffects() {
		return this.effects;
	}

	public float getBaseHeight() {
		return this.baseHeight;
	}

	@Override
	public void remove() {
		for (final RenderSpellEffect effect : this.effects) {
			effect.setAnimations(RenderSpellEffect.DEATH_ONLY, true);
		}
	}

	@Override
	public void setHeight(final float height) {
		for (final RenderSpellEffect effect : this.effects) {
			effect.setHeight(this.baseHeight + height);
		}
	}
}
