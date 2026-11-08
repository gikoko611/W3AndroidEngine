package com.etheller.warsmash.launcher;

import com.badlogic.gdx.Gdx;
import com.etheller.warsmash.WarsmashGdxMenuScreen;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CMapDifficulty;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.MenuUI;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.menu.CampaignCatalog;

/**
 * A game the launcher starts in the engine without its menu: a campaign mission, a custom game, a saved game or a
 * replay, with the profile and the look of the loading screens. Android carries it in the extras of the engine's
 * activity, the desktop hands it to the engine in the same window.
 */
public final class LaunchRequest {
	public static final String CAMPAIGN_MAPS_REFORGED = "reforged";
	public static final String CAMPAIGN_MAPS_CLASSIC = "classic";
	public static final String SKIN_REFORGED = "reforged";

	public static final String SOURCE_CAMPAIGN = "campaign";
	public static final String SOURCE_CUSTOM = "custom";
	public static final String SOURCE_LOAD = "load";
	public static final String SOURCE_REPLAY = "replay";

	/** Campaign number as scripts count campaigns (the named entries of the campaign list); -1 for none. */
	public int campaign = -1;
	public int mission = -1;
	/** easy, normal or hard. */
	public String difficulty;
	/** Which version of the campaign maps is played: {@link #CAMPAIGN_MAPS_REFORGED} or classic (the default). */
	public String campaignMaps;
	/** A map of the custom game list, played with the slots and options. */
	public String customMap;
	public String slots;
	public String options;
	/** A saved game of the current profile, by its name without the extension. */
	public String saveFile;
	/** A replay of the Replay folder, by its full path. */
	public String replayFile;
	/** The map the game starts on, whose loading screen the engine shows from its first frame on. */
	public String mapFile;
	/** The launcher's skin: {@link #SKIN_REFORGED} shows the Reforged loading screens of maps. */
	public String skin;
	/** The profile's Reforged portrait, from 1, which the Reforged loading screens show for the local player. */
	public int portrait;
	/** The player profile the game is played with, by its name. */
	public String profile;

	/** A campaign mission, on the profile's version of the campaign maps. */
	public static LaunchRequest mission(final CampaignCatalog catalog, final CampaignCatalog.Entry entry,
			final int mission, final String campaignMaps, final String difficulty) {
		final LaunchRequest request = new LaunchRequest();
		request.campaign = entry.getNumber();
		request.mission = mission;
		request.campaignMaps = campaignMaps;
		request.mapFile = catalog.missionMapFilename(entry, mission, CAMPAIGN_MAPS_REFORGED.equals(campaignMaps));
		request.difficulty = difficulty;
		return request;
	}

	/** A saved game of the current profile. */
	public static LaunchRequest load(final GameCatalog.SaveEntry entry) {
		final LaunchRequest request = new LaunchRequest();
		request.saveFile = entry.name;
		request.mapFile = entry.mapFile;
		return request;
	}

	/** A replay this engine plays. */
	public static LaunchRequest replay(final GameCatalog.ReplayEntry entry) {
		final LaunchRequest request = new LaunchRequest();
		request.replayFile = entry.file.getAbsolutePath();
		request.mapFile = entry.mapFile;
		return request;
	}

	/** Where the game was started from, as the result tells the launcher. */
	public String source() {
		return this.campaign >= 0 ? SOURCE_CAMPAIGN
				: this.customMap != null ? SOURCE_CUSTOM : this.replayFile != null ? SOURCE_REPLAY : SOURCE_LOAD;
	}

	/** Sets up the engine's menu screen to show only this game's loading screen; before the menu is built. */
	public void prepare(final WarsmashGdxMenuScreen menuScreen) {
		menuScreen.setLauncherMode(true);
		menuScreen.setWaitForTap(true);
		menuScreen.setReforgedLoadingScreens(SKIN_REFORGED.equals(this.skin));
		menuScreen.setReforgedPortrait(this.portrait);
		menuScreen.setStartupMap(this.mapFile);
		menuScreen.setStartupSlots(this.slots);
		menuScreen.setStartupProfile(this.profile);
	}

	/** Starts the game on the built menu; false when the engine cannot start it. */
	public boolean start(final MenuUI menu) {
		try {
			menu.useProfile(this.profile);
			if (this.campaign >= 0) {
				return menu.startCampaignMission(this.campaign, this.mission, difficulty(this.difficulty),
						CAMPAIGN_MAPS_REFORGED.equals(this.campaignMaps));
			}
			if (this.customMap != null) {
				return menu.startSkirmish(this.customMap, this.slots, this.options);
			}
			if (this.saveFile != null) {
				return menu.startSavedGame(this.saveFile);
			}
			if (this.replayFile != null) {
				return menu.startReplay(Gdx.files.absolute(this.replayFile));
			}
		}
		catch (final RuntimeException e) {
			LauncherLog.e("Unable to start the game", e);
		}
		return false;
	}

	private static CMapDifficulty difficulty(final String name) {
		if ("easy".equalsIgnoreCase(name)) {
			return CMapDifficulty.EASY;
		}
		if ("hard".equalsIgnoreCase(name)) {
			return CMapDifficulty.HARD;
		}
		return CMapDifficulty.NORMAL;
	}

	@Override
	public String toString() {
		return "campaign=" + this.campaign + " mission=" + this.mission + " custom=" + this.customMap + " save="
				+ this.saveFile + " replay=" + this.replayFile + " map=" + this.mapFile;
	}
}
