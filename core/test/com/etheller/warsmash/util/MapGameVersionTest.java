package com.etheller.warsmash.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The game a map is played in is worked out from the map alone, for that map's game; nothing of the last map loaded
 * carries over to the next one.
 */
class MapGameVersionTest {
	@Test
	void reignOfChaosCampaignByFileName() {
		assertEquals(0, WarsmashConstants.mapGameVersion("Maps\\Campaign\\Human01.w3m", -1));
		assertEquals(0, WarsmashConstants.mapGameVersion("Maps/Campaign/Reforged/ROC/Human01.w3x", -1));
		assertEquals(WarsmashConstants.GAME_VERSION,
				WarsmashConstants.mapGameVersion("Maps\\FrozenThrone\\Campaign\\Human01x.w3x", -1));
	}

	@Test
	void gameDataVersionOfACampaignMapWins() {
		assertEquals(Math.min(1, WarsmashConstants.GAME_VERSION),
				WarsmashConstants.mapGameVersion("Maps/Campaign/Reforged/ROC/Human01.w3x", 1));
		assertEquals(0, WarsmashConstants.mapGameVersion("Maps\\Campaign\\Human01.w3x", 0));
	}

	@Test
	void otherMapsPlayTheConfiguredGame() {
		assertEquals(WarsmashConstants.GAME_VERSION, WarsmashConstants.mapGameVersion("Maps\\(2)BootyBay.w3m", 0));
		assertEquals(WarsmashConstants.GAME_VERSION, WarsmashConstants.mapGameVersion(null, -1));
	}
}
