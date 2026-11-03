package com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat;

public enum CRegenType {
	NONE, ALWAYS, BLIGHT, DAY, NIGHT;

	public static CRegenType parseRegenType(final String typeString) {
		if (typeString.trim().isEmpty()) {
			return NONE;
		}
		try {
			return valueOf(typeString.toUpperCase());
		}
		catch (final Exception exc) {
			exc.printStackTrace();
			return NONE;
		}
	}
}
