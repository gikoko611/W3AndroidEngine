package com.etheller.warsmash.viewer5.handlers.w3x;

import java.util.Locale;

public final class CampaignMaps {
	private CampaignMaps() {
	}

	/** The missions are in Maps/Campaign in the data before 1.32, in Campaign/Classic and Campaign/Reforged since. */
	public static boolean isCampaignMap(final String mapFileName) {
		if (mapFileName == null) {
			return false;
		}
		final String name = mapFileName.toLowerCase(Locale.ROOT).replace('\\', '/');
		return name.startsWith("campaign/") || name.startsWith("maps/campaign/") || name.contains("/campaign/");
	}
}
