package com.etheller.warsmash.viewer5.handlers.w3x;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CampaignMapsTest {
	@Test
	void missionsOfEveryDataLayoutAreCampaignMaps() {
		assertTrue(CampaignMaps.isCampaignMap("Maps\\Campaign\\Human01.w3m"));
		assertTrue(CampaignMaps.isCampaignMap("Campaign\\Classic\\ROC\\Human01.w3m"));
		assertTrue(CampaignMaps.isCampaignMap("Campaign\\Reforged\\ROC\\Human01.w3x"));
		assertTrue(CampaignMaps.isCampaignMap("campaign/reforged/tft/Human01x.w3x"));
	}

	@Test
	void otherMapsAreNot() {
		assertFalse(CampaignMaps.isCampaignMap(null));
		assertFalse(CampaignMaps.isCampaignMap("Maps\\FrozenThrone\\(2)EchoIsles.w3x"));
		assertFalse(CampaignMaps.isCampaignMap("Maps\\Download\\CampaignLike.w3x"));
	}
}
