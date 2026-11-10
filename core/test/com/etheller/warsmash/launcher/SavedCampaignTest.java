package com.etheller.warsmash.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.etheller.warsmash.datasources.FolderDataSource;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.menu.CampaignCatalog;

/** A saved campaign mission is loaded from the launcher as a mission of its campaign. */
class SavedCampaignTest {
	@TempDir
	File temp;

	private static final String LIST = "[Index]\r\nCampaignList=\"Human\",\"Orc\"\r\n"
			+ "[Human]\r\nHeader=\"Human\"\r\nName=\"The Scourge of Lordaeron\"\r\nDefaultOpen=1\r\n"
			+ "Mission0=\"Chapter One\",\"The Defense of Strahnbrad\",\"Maps\\Campaign\\Human01.w3m\"\r\n"
			+ "Mission1=\"Chapter Two\",\"Blackrock and Roll\",\"Maps\\Campaign\\Human02.w3m\"\r\n"
			+ "Mission2=\"Interlude\",\"Shared\",\"Maps\\Campaign\\Shared.w3m\"\r\n"
			+ "[Orc]\r\nHeader=\"Orc\"\r\nName=\"The Invasion of Kalimdor\"\r\nDefaultOpen=1\r\n"
			+ "Mission0=\"Chapter One\",\"Landfall\",\"Maps\\Campaign\\Orc01.w3m\"\r\n"
			+ "Mission1=\"Interlude\",\"Shared\",\"Maps\\Campaign\\Shared.w3m\"\r\n";

	private CampaignCatalog catalog() throws IOException {
		final File root = new File(this.temp, "data");
		final File list = new File(root, "UI/CampaignInfoClassic.txt");
		list.getParentFile().mkdirs();
		Files.write(list.toPath(), LIST.getBytes(StandardCharsets.UTF_8));
		return CampaignCatalog.load(new FolderDataSource(root.toPath()), "UI\\CampaignInfo.txt");
	}

	private GameCatalog.SaveEntry save(final String map, final int campaign, final boolean kept) {
		final GameCatalog.SaveEntry entry = new GameCatalog.SaveEntry("Save", new File(this.temp, "Save.w3z"));
		entry.mapFile = map;
		entry.campaign = campaign;
		entry.campaignKept = kept;
		entry.difficulty = kept ? "hard" : null;
		return entry;
	}

	@Test
	void campaignIsKnownByItsMapOnlyWhenOneCampaignListsIt() throws IOException {
		final CampaignCatalog catalog = catalog();
		assertEquals(0, CampaignResult.campaignOfMap(catalog, "Maps/Campaign/human02.w3m").getNumber());
		assertEquals(1, CampaignResult.campaignOfMap(catalog, "Maps\\Campaign\\Orc01.w3m").getNumber());
		assertNull(CampaignResult.campaignOfMap(catalog, "Maps\\Campaign\\Shared.w3m"));
		assertNull(CampaignResult.campaignOfMap(catalog, "Maps\\FrozenThrone\\(2)EchoIsles.w3x"));
	}

	@Test
	void savedMissionLoadsAsItsCampaignsMission() throws IOException {
		final LaunchRequest request = LaunchRequest.load(save("Maps\\Campaign\\Human02.w3m", 0, true), catalog());
		assertEquals(LaunchRequest.SOURCE_CAMPAIGN, request.source());
		assertEquals(0, request.campaign);
		assertEquals(1, request.mission);
		assertEquals("hard", request.difficulty);
		assertEquals("Save", request.saveFile);
	}

	@Test
	void savedSharedMapKeepsTheCampaignItWasPlayedIn() throws IOException {
		final LaunchRequest request = LaunchRequest.load(save("Maps\\Campaign\\Shared.w3m", 1, true), catalog());
		assertEquals(1, request.campaign);
		assertEquals(1, request.mission);
	}

	@Test
	void olderSaveIsKnownByItsMap() throws IOException {
		final LaunchRequest request = LaunchRequest.load(save("Maps\\Campaign\\Orc01.w3m", -1, false), catalog());
		assertEquals(1, request.campaign);
		assertEquals(0, request.mission);
	}

	@Test
	void otherGamesLoadAsBefore() throws IOException {
		final CampaignCatalog catalog = catalog();
		assertEquals(LaunchRequest.SOURCE_LOAD,
				LaunchRequest.load(save("Maps\\FrozenThrone\\(2)EchoIsles.w3x", -1, true), catalog).source());
		// A save that kept its campaign is not looked up by its map.
		assertEquals(LaunchRequest.SOURCE_LOAD,
				LaunchRequest.load(save("Maps\\Campaign\\Orc01.w3m", -1, true), catalog).source());
		assertEquals(LaunchRequest.SOURCE_LOAD,
				LaunchRequest.load(save("Maps\\Campaign\\Orc01.w3m", 1, true), null).source());
	}
}
