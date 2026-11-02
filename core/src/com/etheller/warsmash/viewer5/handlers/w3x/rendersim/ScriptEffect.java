package com.etheller.warsmash.viewer5.handlers.w3x.rendersim;

/**
 * What the effect natives need of an effect handle, which after a load is the saved stand-in of the effect.
 */
public interface ScriptEffect {
	void setKillWhenDone(boolean killWhenDone);

	void removeWithoutDeath();

	/** The effect being shown, or null while a saved stand-in has none. */
	RenderSpellEffect getLiveEffect();
}
