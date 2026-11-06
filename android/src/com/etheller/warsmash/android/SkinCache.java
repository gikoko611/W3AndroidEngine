package com.etheller.warsmash.android;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.fonts.Font;
import android.graphics.fonts.FontFamily;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.util.Log;

import com.etheller.warsmash.WarsmashGdxMapScreen;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.units.Element;
import com.etheller.warsmash.viewer5.handlers.blp.BlpDecoder;
import com.etheller.warsmash.viewer5.handlers.blp.DdsDecoder;

/**
 * The launcher's own copy of the game art it draws with. The APK carries no game pixels, so the preparation step
 * decodes the art from the player's game data into PNG files under the cache folder, which Android may clear; a
 * cleared or outdated cache is simply prepared again.
 */
final class SkinCache {
	private static final String TAG = "Warsmash";
	/** Raised whenever the list of cached files or the way they are made changes. */
	private static final int FORMAT = 10;
	private static final String STAMP = "stamp";
	private static final String PENDING = "pending";

	static final String BACKGROUND = "background.jpg";
	static final String PANEL_BORDER = "panel-border.9.png";
	static final String BUTTON_BORDER = "button-border.9.png";
	static final String BUTTON_BORDER_DOWN = "button-border-down.9.png";
	static final String BUTTON_BORDER_DISABLED = "button-border-disabled.9.png";
	static final String BUTTON_BACKGROUND = "button-background.png";
	static final String BUTTON_BACKGROUND_DOWN = "button-background-down.png";
	static final String BUTTON_BACKGROUND_DISABLED = "button-background-disabled.png";
	static final String HOLDER = "holder.png";
	static final String BAR_BACKGROUND = "bar-background.png";
	static final String BAR_FILL = "bar-fill.png";
	static final String BAR_GLASS = "bar-glass.png";
	static final String LOGO_TFT = "logo-tft.png";
	static final String LOGO_ROC = "logo-roc.png";
	static final String PORTRAIT_ACTIVE = "portrait-active.png";
	static final String QUESTION = "dialog-question.png";
	static final String MUSIC = "menu-music.mp3";
	static final String MUSIC_ROC = "menu-music-roc.mp3";
	static final String CHECKBOX = "checkbox.png";
	static final String CHECKBOX_CHECK = "checkbox-check.png";
	static final String DIALOG_BORDER = "dialog-border.9.png";
	static final String DIALOG_BACKGROUND = "dialog-background.png";
	static final String CAMPAIGN_POSTER = "campaign-prologue.jpg";
	static final String FONT = "frizqt.ttf";
	/** Friz Quadrata has no ą č ę ė į ų ū; the game's global font draws them. */
	static final String FONT_GLOBAL = "blizzardglobal.ttf";
	static final String CLICK = "click.ogg";
	static final String CLICK_BIG = "click-big.ogg";
	static final String ICON_FOLDER = "icon-folder.png";
	static final String STRINGS = "strings.txt";
	static final String REFORGED = "reforged";
	static final String RADIO = "radio.png";
	static final String RADIO_BUTTON = "radio-button.png";
	static final String SLIDER_BORDER = "slider-border.9.png";
	static final String SLIDER_BACKGROUND = "slider-background.png";
	static final String KNOB = "knob.png";
	static final String EDIT_BORDER = "edit-border.9.png";
	static final String EDIT_BACKGROUND = "edit-background.png";
	static final String RAIL_BACKGROUND = "rail-background.jpg";
	static final String CAMPAIGN_ARROW = "campaign-arrow.png";
	static final String PULLDOWN_ARROW = "pulldown-arrow.png";
	static final String ICON_FILE = "icon-file.png";
	static final String ICON_MELEE = "icon-file-melee.png";
	static final String ICON_UMS = "icon-file-ums.png";
	static final String MINIMAP_UNKNOWN = "minimap-unknown.png";
	static final String ROW_HIGHLIGHT = "row-highlight.png";
	static final String BUTTON_RED = "button-red.png";
	static final String BUTTON_RED_DOWN = "button-red-down.png";
	static final String BUTTON_GREEN = "button-green.png";
	static final String BUTTON_GREEN_DOWN = "button-green-down.png";
	static final String MISSION_BORDER = "mission-border.png";
	static final String MISSION_BORDER_HOVER = "mission-border-hover.png";
	static final String MISSION_CHECK = "mission-check.png";
	/** The Reforged mission pictures, made small: an index of "file TAB data path" lines and the files it names. */
	static final String MISSIONS = "missions/index.txt";
	private static final String MISSION_FOLDER = "webui\\loadingscreen\\campaign\\";

	/** The race symbols of the profile statistics: human, orc, undead, night elf (random has none). */
	static final String[] RACE_SYMBOLS = { "race-human.png", "race-orc.png", "race-undead.png", "race-nightelf.png" };

	/**
	 * The classic profile portraits, command buttons of the heroes and units by race: the glue name of the race, the
	 * text when the data lacks it, then the buttons. The first twelve were the only ones before; profiles keep their
	 * portrait by its place in {@link #CLASSIC_PORTRAITS}, so those come first there.
	 */
	private static final String[][] CLASSIC_PORTRAIT_RACES = {
		{ "HUMAN", "Human", "BTNHeroPaladin", "BTNHeroArchMage", "BTNHeroMountainKing", "BTNHeroBloodElfPrince",
			"BTNArthas", "BTNJaina", "BTNGarithos", "BTNFootman", "BTNKnight", "BTNRifleman", "BTNPriest",
			"BTNSorceress", "BTNSpellBreaker", "BTNGryphonRider", "BTNDragonHawk", "BTNMortarTeam", "BTNSteamTank",
			"BTNPeasant" },
		{ "ORC", "Orc", "BTNHeroBlademaster", "BTNHeroFarSeer", "BTNHeroTaurenChieftain", "BTNShadowHunter",
			"BTNThrall", "BTNRexxar", "BTNChaosGrom", "BTNGrunt", "BTNHeadhunter", "BTNRaider", "BTNShaman",
			"BTNWitchDoctor", "BTNSpiritWalker", "BTNTauren", "BTNWyvernRider", "BTNTrollBatRider", "BTNPeon" },
		{ "UNDEAD", "Undead", "BTNHeroDeathKnight", "BTNHeroLich", "BTNHeroDreadlord", "BTNHeroCryptLord",
			"BTNKelThuzad", "BTNTichondrius", "BTNBansheeRanger", "BTNGhoul", "BTNCryptFiend", "BTNAbomination",
			"BTNNecromancer", "BTNBanshee", "BTNGargoyle", "BTNFrostWyrm", "BTNDestroyer", "BTNAcolyte" },
		{ "NIGHT_ELF", "Night Elf", "BTNHeroDemonHunter", "BTNKeeperOfTheGrove", "BTNHeroMoonPriestess",
			"BTNHeroWarden", "BTNFurion", "BTNPriestessOfTheMoon", "BTNEvilIllidan", "BTNWarden2", "BTNArcher",
			"BTNHuntress", "BTNDryad", "BTNDruidOfTheClaw", "BTNDruidOfTheTalon", "BTNMountainGiant",
			"BTNHippogriffRider", "BTNChimaera", "BTNFaerieDragon", "BTNWisp" },
		{ "KEY_NEUTRAL", "Neutral", "BTNHeroTinker", "BTNHeroAlchemist", "BTNHeroAvatarOfFlame", "BTNPandarenBrewmaster",
			"BTNBeastmaster", "BTNPitLord", "BTNNagaSeaWitch", "BTNArchimonde", "BTNMannoroth", "BTNAkama",
			"BTNMedivh", "BTNNagaMyrmidon", "BTNMurloc", "BTNSatyr", "BTNFurbolg", "BTNOgreMagi", "BTNCentaurKhan",
			"BTNHarpyQueen", "BTNGnollKing", "BTNInfernal", "BTNDoomGuard", "BTNFelguard", "BTNBanditLord",
			"BTNRanger" } };
	private static final String[] CLASSIC_PORTRAITS = classicPortraits();
	/** Reforged keeps its portraits numbered from p001; as many as the data has are offered. */
	private static final int REFORGED_PORTRAITS = 256;

	private static String[] classicPortraits() {
		final List<String> names = new ArrayList<>(Arrays.asList("BTNHeroPaladin", "BTNArthas", "BTNThrall",
				"BTNJaina", "BTNFurion", "BTNHeroArchMage", "BTNHeroBlademaster", "BTNHeroDeathKnight",
				"BTNHeroMountainKing", "BTNKeeperOfTheGrove", "BTNKelThuzad", "BTNRexxar"));
		for (final String[] race : CLASSIC_PORTRAIT_RACES) {
			for (int i = 2; i < race.length; i++) {
				if (!names.contains(race[i])) {
					names.add(race[i]);
				}
			}
		}
		return names.toArray(new String[0]);
	}

	/** A group of portraits shown together: the race's glue name and text, and the portraits' numbers. */
	static final class PortraitGroup {
		final String key;
		final String fallback;
		final int[] portraits;

		PortraitGroup(final String key, final String fallback, final int[] portraits) {
			this.key = key;
			this.fallback = fallback;
			this.portraits = portraits;
		}
	}

	/** The portraits as the picker shows them: by race for the classic skin, in their order for Reforged. */
	List<PortraitGroup> portraitGroups() {
		final List<PortraitGroup> groups = new ArrayList<>();
		if (this.reforged) {
			final int[] all = new int[REFORGED_PORTRAITS];
			for (int i = 0; i < all.length; i++) {
				all[i] = i;
			}
			groups.add(new PortraitGroup(null, null, all));
			return groups;
		}
		final List<String> names = Arrays.asList(CLASSIC_PORTRAITS);
		for (final String[] race : CLASSIC_PORTRAIT_RACES) {
			final int[] portraits = new int[race.length - 2];
			for (int i = 2; i < race.length; i++) {
				portraits[i - 2] = names.indexOf(race[i]);
			}
			groups.add(new PortraitGroup(race[0], race[1], portraits));
		}
		return groups;
	}

	static final String CLASSIC_SKIN = "classic";
	static final String REFORGED_SKIN = "reforged";

	/**
	 * Original menu and campaign scenes, as Blizzard recorded them. The classic skin plays the "_sd" recordings of the
	 * classic 3D scenes, the Reforged skin the ones without; the two sets are never mixed.
	 */
	static final String[] SCENES = { "mainmenu", "mainmenu_tft", "roc_prologue", "roc_human", "roc_undead", "roc_orc",
		"roc_nightelf", "tft_nightelf", "tft_human", "tft_undead", "tft_orc" };

	/** The string lists the launcher speaks from: the game's own and the engine's (its saved game messages). */
	private static final String[] STRING_FILES = { "UI\\FrameDef\\GlobalStrings.fdf",
		"UI\\FrameDef\\SmashUI\\SmashStrings.fdf" };
	private static final Pattern STRING_LINE = Pattern.compile("^\\s*([A-Za-z0-9_]+)\\s+\"(.*)\"\\s*,?\\s*$");

	interface Progress {
		void step(int done, int total, String name);
	}

	private interface Maker {
		/** @return false if the game data does not have the file */
		boolean make(DataSource data, File target) throws IOException;
	}

	private static final class Item {
		final String name;
		final String shown;
		final Maker maker;

		Item(final String name, final String shown, final Maker maker) {
			this.name = name;
			this.shown = shown;
			this.maker = maker;
		}
	}

	private final File folder;
	private final String key;
	private final boolean reforged;

	private SkinCache(final File folder, final String key, final boolean reforged) {
		this.folder = folder;
		this.key = key;
		this.reforged = reforged;
	}

	static File iniFile(final Context context) {
		return new File(context.getExternalFilesDir(null), AndroidLauncher.INI_FILE_NAME);
	}

	/** The cache of a skin for the data sources the engine configuration names, or null without a configuration. */
	static SkinCache forConfiguration(final Context context, final DataTable ini, final String skin) {
		final Element sources = ini.get("DataSources");
		if (sources == null) {
			return null;
		}
		final boolean reforged = REFORGED_SKIN.equals(skin);
		final String name = reforged ? REFORGED_SKIN : CLASSIC_SKIN;
		final StringBuilder key = new StringBuilder().append(FORMAT).append('|').append(name);
		for (int i = 0; i < sources.size(); i++) {
			final String id = String.format(Locale.ROOT, "%02d", i);
			key.append('|').append(sources.getField("Type" + id)).append(':').append(sources.getField("Path" + id));
		}
		return new SkinCache(new File(new File(context.getCacheDir(), "skin"), name), key.toString(), reforged);
	}

	/** Whether the game data has the Reforged interface, as the last preparation of either skin found it. */
	static boolean hasReforged(final Context context) {
		final File skins = new File(context.getCacheDir(), "skin");
		return new File(new File(skins, CLASSIC_SKIN), REFORGED).isFile()
				|| new File(new File(skins, REFORGED_SKIN), REFORGED).isFile();
	}

	boolean reforged() {
		return this.reforged;
	}

	static String portraitFile(final int index) {
		return String.format(Locale.ROOT, "portrait-%02d.png", index);
	}

	/** How many portraits this skin offers; the files the game data lacks are left out where they are listed. */
	int portraitCount() {
		return this.reforged ? REFORGED_PORTRAITS : CLASSIC_PORTRAITS.length;
	}

	/** The recording this skin plays for a scene: the "_sd" one for the classic skin. */
	String sceneName(final String scene) {
		return this.reforged ? scene : scene + "_sd";
	}

	/** Makes the next start prepare everything again from the game data, as Re-check asks. */
	void invalidate() {
		deleteTree(this.folder);
	}

	File file(final String name) {
		return new File(this.folder, name);
	}

	File scene(final String name, final boolean poster) {
		return new File(new File(this.folder, "webms"), name + (poster ? ".jpg" : ".webm"));
	}

	/** Whether every file of the last finished preparation is still there for the same game data. */
	boolean isCurrent() {
		if (!this.key.equals(read(file(STAMP)))) {
			return false;
		}
		final String list = read(file(STAMP + ".files"));
		if (list == null) {
			return false;
		}
		for (final String name : list.split("\n")) {
			if (!name.isEmpty() && !file(name).isFile()) {
				return false;
			}
		}
		return true;
	}

	int total() {
		return items().size();
	}

	/**
	 * Makes every file that is missing. An interrupted run is resumed from the files it already made; a run for other
	 * game data starts over.
	 */
	void prepare(final DataTable ini, final Progress progress, final boolean[] cancelled) throws IOException {
		final String pending = read(file(PENDING));
		if (!this.key.equals(pending) && !this.key.equals(read(file(STAMP)))) {
			deleteTree(this.folder);
		}
		this.folder.mkdirs();
		new File(this.folder, "webms").mkdirs();
		file(STAMP).delete();
		write(file(PENDING), this.key);
		final List<Item> items = items();
		progress.step(0, items.size(), null);
		final DataSource data = WarsmashGdxMapScreen.parseDataSources(ini);
		final StringBuilder made = new StringBuilder();
		try {
			for (int i = 0; i < items.size(); i++) {
				if (cancelled[0]) {
					return;
				}
				final Item item = items.get(i);
				final File target = item.name.startsWith("webms/") ? new File(this.folder, item.name)
						: file(item.name);
				boolean present = target.isFile();
				if (!present) {
					try {
						present = item.maker.make(data, target);
					}
					catch (final IOException | RuntimeException e) {
						// A file the decoders cannot read is drawn with its fallback, like a missing one.
						Log.w(TAG, "Launcher art " + item.shown + " not prepared", e);
						present = false;
					}
					if (!present) {
						Log.i(TAG, "Launcher art not in the game data: " + item.shown);
					}
				}
				if (present && !item.name.startsWith("webms/")) {
					made.append(item.name).append('\n');
				}
				progress.step(i + 1, items.size(), item.shown);
			}
		}
		finally {
			data.close();
		}
		write(file(STAMP + ".files"), made.toString());
		write(file(STAMP), this.key);
		file(PENDING).delete();
	}

	private List<Item> items() {
		final List<Item> items = new ArrayList<>();
		// The preparation screen draws with the first ones as soon as they exist.
		if (this.reforged) {
			items.add(web(BACKGROUND, "webui\\common\\homescreen.jpg", 0, false));
			items.add(web(PANEL_BORDER, "webui\\baseframes\\war3_metal_frame_4k.png", 90, false));
		}
		else {
			items.add(new Item(BACKGROUND, "generic-loading",
					(data, target) -> tiles(data, "UI\\Glues\\Loading\\Load-Generic\\Generic-Loading", target)));
			items.add(image(PANEL_BORDER, "UI\\Widgets\\EscMenu\\Human\\human-options-menu-border", Shape.EDGE));
		}
		items.add(image(BAR_BACKGROUND, "UI\\Glues\\Loading\\LoadBar\\Loading-BarBackground", Shape.PLAIN));
		items.add(image(BAR_FILL, "UI\\Glues\\Loading\\LoadBar\\Loading-BarFill", Shape.PLAIN));
		items.add(image(BAR_GLASS, "UI\\Glues\\Loading\\LoadBar\\Loading-BarGlass", Shape.PLAIN));
		items.add(new Item(FONT, "frizqt__.ttf", (data, target) -> copy(data, "Fonts\\FRIZQT__.TTF", target)));
		items.add(new Item(FONT_GLOBAL, "blizzardglobal-v5_4.ttf",
				(data, target) -> copy(data, "Fonts\\blizzardglobal-v5_4.ttf", target)));
		items.add(new Item(CLICK, "MouseClick1",
				(data, target) -> copy(data, "Sound\\Interface\\MouseClick1.ogg", target)));
		items.add(new Item(CLICK_BIG, "BigButtonClick",
				(data, target) -> copy(data, "Sound\\Interface\\BigButtonClick.ogg", target)));
		if (this.reforged) {
			reforgedItems(items);
		}
		else {
			classicItems(items);
		}
		items.add(new Item(CAMPAIGN_POSTER, "tutorial loading screen", (data, target) -> tiles(data,
				"UI\\Glues\\Loading\\Backgrounds\\Campaigns\\Tutorial", target)));
		items.add(new Item(STRINGS, "GlobalStrings.fdf", SkinCache::strings));
		final String glues = "UI\\Widgets\\Glues\\";
		items.add(image(CAMPAIGN_ARROW, glues + "GlueScreen-CampaignButton-Arrow", Shape.PLAIN));
		items.add(image(PULLDOWN_ARROW, glues + "GlueScreen-Pulldown-Arrow", Shape.PLAIN));
		items.add(image(ICON_FILE, glues + "Icon-File", Shape.PLAIN));
		items.add(image(ICON_MELEE, glues + "Icon-File-Melee", Shape.PLAIN));
		items.add(image(ICON_UMS, glues + "Icon-File-UMS", Shape.PLAIN));
		items.add(image(ICON_FOLDER, glues + "Icon-Folder", Shape.PLAIN));
		items.add(image(MINIMAP_UNKNOWN, glues + "Minimap-Unknown", Shape.PLAIN));
		final String[] races = { "Human", "Orc", "Undead", "NightElf" };
		for (int i = 0; i < races.length; i++) {
			items.add(image(RACE_SYMBOLS[i], "UI\\Glues\\Loading\\Backgrounds\\Campaigns\\" + races[i] + "Symbol",
					Shape.PLAIN));
		}
		items.add(new Item(MUSIC, "war3XMainScreen.mp3",
				(data, target) -> copy(data, "Sound\\Music\\mp3Music\\war3XMainScreen.mp3", target)));
		items.add(new Item(MUSIC_ROC, "MainScreen.mp3",
				(data, target) -> copy(data, "Sound\\Music\\mp3Music\\MainScreen.mp3", target)));
		items.add(new Item(REFORGED, "Reforged UI", (data, target) -> {
			if (!data.has("webui\\common\\homescreen.jpg")) {
				return false;
			}
			write(target, "");
			return true;
		}));
		for (final String base : SCENES) {
			final String scene = sceneName(base);
			items.add(new Item("webms/" + scene + ".webm", scene + ".webm", (data, target) -> scene(data, scene,
					target)));
		}
		return items;
	}

	private static void classicItems(final List<Item> items) {
		items.add(image(BUTTON_BORDER, "UI\\Widgets\\Glues\\GlueScreen-Button1-BackdropBorder", Shape.EDGE));
		items.add(image(BUTTON_BORDER_DOWN, "UI\\Widgets\\Glues\\GlueScreen-Button1-BackdropBorder-Down",
				Shape.EDGE));
		items.add(image(BUTTON_BORDER_DISABLED, "UI\\Widgets\\Glues\\GlueScreen-Button1-BackdropBorder-Disabled",
				Shape.EDGE));
		items.add(image(BUTTON_BACKGROUND, "UI\\Widgets\\Glues\\GlueScreen-Button1-BackdropBackground", Shape.PLAIN));
		items.add(image(BUTTON_BACKGROUND_DOWN, "UI\\Widgets\\Glues\\GlueScreen-Button1-BackdropBackground-Down",
				Shape.PLAIN));
		items.add(image(BUTTON_BACKGROUND_DISABLED,
				"UI\\Widgets\\Glues\\GlueScreen-Button1-BackdropBackground-Disabled", Shape.PLAIN));
		items.add(image(HOLDER, "UI\\Widgets\\Glues\\GlueScreen-Button1-Border", Shape.HOLDER));
		items.add(image(LOGO_TFT, "UI\\Glues\\MainMenu\\WarCraftIIILogo_exp\\war3xlogo", Shape.PLAIN));
		items.add(image(LOGO_ROC, "UI\\Glues\\SinglePlayer\\CampaignWarcraftIIILogo\\WarcraftIII-Logo-Alpha",
				Shape.PLAIN));
		for (int i = 0; i < CLASSIC_PORTRAITS.length; i++) {
			items.add(image(portraitFile(i), "ReplaceableTextures\\CommandButtons\\" + CLASSIC_PORTRAITS[i],
					Shape.PLAIN));
		}
		items.add(image(PORTRAIT_ACTIVE, "UI\\Widgets\\Console\\Human\\CommandButton\\human-activebutton",
				Shape.PLAIN));
		items.add(image(QUESTION, "UI\\Widgets\\Glues\\dialogbox-question", Shape.PLAIN));
		items.add(image(CHECKBOX, "UI\\Widgets\\Glues\\GlueScreen-Checkbox-Background", Shape.PLAIN));
		items.add(image(CHECKBOX_CHECK, "UI\\Widgets\\Glues\\GlueScreen-Checkbox-Check", Shape.PLAIN));
		items.add(image(DIALOG_BORDER, "UI\\Widgets\\BattleNet\\bnet-dialoguebox-border", Shape.EDGE));
		items.add(image(DIALOG_BACKGROUND, "UI\\Widgets\\BattleNet\\bnet-dialoguebox-background", Shape.PLAIN));
		items.add(image(RADIO, "UI\\Widgets\\Glues\\GlueScreen-RadioButton-Background", Shape.PLAIN));
		items.add(image(RADIO_BUTTON, "UI\\Widgets\\Glues\\GlueScreen-RadioButton-Button", Shape.PLAIN));
		items.add(image(SLIDER_BORDER, "UI\\Widgets\\Glues\\GlueScreen-Slider-BackdropBorder", Shape.EDGE));
		items.add(image(SLIDER_BACKGROUND, "UI\\Widgets\\Glues\\GlueScreen-Slider-BackdropBackground", Shape.PLAIN));
		items.add(image(KNOB, "UI\\Widgets\\Glues\\SinglePlayerSkirmish-ScrollBarKnob", Shape.PLAIN));
		items.add(image(EDIT_BORDER, "UI\\Widgets\\EscMenu\\Human\\editbox-border", Shape.EDGE));
		items.add(image(EDIT_BACKGROUND, "UI\\Widgets\\EscMenu\\Human\\editbox-background", Shape.PLAIN));
		items.add(image(ROW_HIGHLIGHT, "UI\\Widgets\\BattleNet\\bnet-button01-highlight-mouse", Shape.PLAIN));
	}

	/**
	 * The Reforged interface keeps its art as PNG and JPEG under webui. Frames are whole images, cut into nine parts at
	 * the slice the game's own style sheets use.
	 */
	private static void reforgedItems(final List<Item> items) {
		final String buttons = "webui\\widgets\\buttons\\";
		items.add(web(BUTTON_BORDER, buttons + "wc3_btn_blue_normal_4k.png", 30, true));
		items.add(web(BUTTON_BORDER_DOWN, buttons + "wc3_btn_blue_down_4k.png", 30, true));
		items.add(web(BUTTON_BORDER_DISABLED, buttons + "wc3_btn_disabled_normal_4k.png", 30, true));
		items.add(web(LOGO_TFT, "webui\\common\\wc3tft-logo.png", 0, false));
		items.add(web(LOGO_ROC, "webui\\common\\wc3roc-logo.png", 0, false));
		for (int i = 0; i < REFORGED_PORTRAITS; i++) {
			items.add(web(portraitFile(i), String.format(Locale.ROOT, "webui\\portraits\\p%03d.png", i + 1), 0,
					false));
		}
		items.add(web(PORTRAIT_ACTIVE, "webui\\widgets\\playerwidget\\portrait_frame_hover_4k.png", 0, false));
		items.add(web(QUESTION, "webui\\modal\\errormodal\\error_icon.png", 0, false));
		items.add(web(CHECKBOX, buttons + "checkbox-unchecked.png", 0, false));
		items.add(web(CHECKBOX_CHECK, buttons + "checkbox-checked.png", 0, false));
		items.add(web(DIALOG_BORDER, "webui\\baseframes\\war3_metal_frame_4k.png", 90, false));
		items.add(web(DIALOG_BACKGROUND, "webui\\baseframes\\war3_stone_bg_landscape_4k.png", 0, false));
		items.add(web(RAIL_BACKGROUND, "webui\\baseframes\\war3_stone_bg_landscape_4k.png", 0, false));
		items.add(web(KNOB, "webui\\widgets\\slider\\war3_slider_normal_4k.png", 0, false));
		items.add(web(EDIT_BORDER, "webui\\widgets\\textfields\\war3_text_normal_4k.png", 14, true));
		items.add(web(ROW_HIGHLIGHT, "webui\\social\\clan\\clan_settings_members_playerrow_bg.png", 0, false));
		items.add(web(BUTTON_RED, buttons + "wc3_btn_red_normal_4k.png", 30, true));
		items.add(web(BUTTON_RED_DOWN, buttons + "wc3_btn_red_down_4k.png", 30, true));
		items.add(web(BUTTON_GREEN, buttons + "wc3_btn_green_normal_4k.png", 30, true));
		items.add(web(BUTTON_GREEN_DOWN, buttons + "wc3_btn_green_down_4k.png", 30, true));
		items.add(web(MISSION_BORDER, "webui\\campaign\\campaign-select-border.png", 0, false));
		items.add(web(MISSION_BORDER_HOVER, "webui\\campaign\\campaign-select-border_hover.png", 0, false));
		items.add(web(MISSION_CHECK, "webui\\campaign\\campaign-mission-checkmark.png", 0, false));
		items.add(new Item(MISSIONS, "mission pictures", SkinCache::missionPictures));
	}

	/** Every mission loading picture of the Reforged campaigns, scaled down to the size of a list row. */
	private static boolean missionPictures(final DataSource data, final File index) throws IOException {
		final Collection<String> listed = data.getListfile();
		if (listed == null) {
			return false;
		}
		final File folder = index.getParentFile();
		folder.mkdirs();
		final StringBuilder lines = new StringBuilder();
		int number = 0;
		for (final String listedPath : listed) {
			final String path = listedPath.replace('/', '\\');
			final String lower = path.toLowerCase(Locale.ROOT);
			// Only the pictures in a campaign's own folder; the credits and crests lie right in the campaign folder.
			if (!lower.startsWith(MISSION_FOLDER) || !lower.endsWith(".jpg")
					|| (lower.indexOf('\\', MISSION_FOLDER.length()) < 0)) {
				continue;
			}
			final ByteBuffer file = data.read(path);
			if (file == null) {
				continue;
			}
			final ByteBuffer bytes = file.duplicate();
			bytes.position(0);
			final byte[] encoded = new byte[bytes.remaining()];
			bytes.get(encoded);
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(encoded, 0, encoded.length, options);
			options.inJustDecodeBounds = false;
			options.inSampleSize = 1;
			while ((Math.min(options.outWidth, options.outHeight) / (options.inSampleSize * 2)) >= 256) {
				options.inSampleSize *= 2;
			}
			final Bitmap bitmap = BitmapFactory.decodeByteArray(encoded, 0, encoded.length, options);
			if (bitmap == null) {
				continue;
			}
			final String name = String.format(Locale.ROOT, "m%03d.jpg", number++);
			save(bitmap, new File(folder, name), Bitmap.CompressFormat.JPEG);
			bitmap.recycle();
			lines.append(name).append('\t').append(lower.substring(MISSION_FOLDER.length())).append('\n');
		}
		if (number == 0) {
			return false;
		}
		write(index, lines.toString());
		return true;
	}

	private enum Shape {
		PLAIN, EDGE, HOLDER
	}

	private static Item image(final String name, final String path, final Shape shape) {
		final String shown = path.substring(path.lastIndexOf('\\') + 1);
		return new Item(name, shown, (data, target) -> {
			final Bitmap bitmap = decode(data, path);
			if (bitmap == null) {
				return false;
			}
			final Bitmap shaped;
			switch (shape) {
			case EDGE:
				shaped = nineSlice(bitmap);
				break;
			case HOLDER:
				shaped = holder(bitmap);
				break;
			default:
				shaped = bitmap;
				break;
			}
			save(shaped, target, Bitmap.CompressFormat.PNG);
			return true;
		});
	}

	/**
	 * A PNG or JPEG of the Reforged interface. With a slice the image is a whole frame and becomes the 3 x 3 image the
	 * frame drawing takes; pictures larger than a screen are scaled down.
	 */
	private static Item web(final String name, final String path, final int slice, final boolean centre) {
		final String shown = path.substring(path.lastIndexOf('\\') + 1);
		return new Item(name, shown, (data, target) -> {
			if (!data.has(path)) {
				return false;
			}
			final ByteBuffer file = data.read(path);
			if (file == null) {
				return false;
			}
			final ByteBuffer bytes = file.duplicate();
			bytes.position(0);
			final byte[] encoded = new byte[bytes.remaining()];
			bytes.get(encoded);
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(encoded, 0, encoded.length, options);
			options.inJustDecodeBounds = false;
			options.inSampleSize = 1;
			while ((slice == 0) && ((Math.max(options.outWidth, options.outHeight) / options.inSampleSize) > 2048)) {
				options.inSampleSize *= 2;
			}
			final Bitmap bitmap = BitmapFactory.decodeByteArray(encoded, 0, encoded.length, options);
			if (bitmap == null) {
				return false;
			}
			save(slice > 0 ? sliced(bitmap, slice, centre) : bitmap, target,
					name.endsWith(".jpg") ? Bitmap.CompressFormat.JPEG : Bitmap.CompressFormat.PNG);
			return true;
		});
	}

	/** Corners kept, edges and middle scaled to squares of the slice size; without centre the middle stays empty. */
	private static Bitmap sliced(final Bitmap frame, final int slice, final boolean centre) {
		final int s = Math.min(slice, Math.min(frame.getWidth(), frame.getHeight()) / 3);
		final int[] xs = { 0, s, frame.getWidth() - s, frame.getWidth() };
		final int[] ys = { 0, s, frame.getHeight() - s, frame.getHeight() };
		final Bitmap nine = Bitmap.createBitmap(s * 3, s * 3, Bitmap.Config.ARGB_8888);
		final Canvas canvas = new Canvas(nine);
		final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 3; column++) {
				if ((row == 1) && (column == 1) && !centre) {
					continue;
				}
				canvas.drawBitmap(frame, new Rect(xs[column], ys[row], xs[column + 1], ys[row + 1]),
						new Rect(column * s, row * s, (column + 1) * s, (row + 1) * s), paint);
			}
		}
		return nine;
	}

	/** The image at a path given without extension: DDS in data from 1.30 on, BLP before. */
	static Bitmap decode(final DataSource data, final String path) throws IOException {
		for (final String extension : new String[] { ".dds", ".blp" }) {
			if (!data.has(path + extension)) {
				continue;
			}
			// A map archive's read leaves the buffer at its end; the stream reads it whole from the start.
			final ByteBuffer file;
			try (InputStream stream = data.getResourceAsStream(path + extension)) {
				if (stream == null) {
					continue;
				}
				file = ByteBuffer.wrap(bytes(stream));
			}
			final int width;
			final int height;
			final ByteBuffer rgba;
			if (DdsDecoder.isDds(file)) {
				final DdsDecoder image = DdsDecoder.decode(file);
				width = image.getWidth();
				height = image.getHeight();
				rgba = image.getRgba();
			}
			else if (BlpDecoder.isJpegBlp1(file)) {
				final BlpDecoder image = BlpDecoder.decodeJpeg(file);
				width = image.getWidth();
				height = image.getHeight();
				rgba = image.getRgba();
			}
			else {
				final BlpDecoder image = BlpDecoder.decode(file);
				width = image.getWidth();
				height = image.getHeight();
				rgba = image.getRgba();
			}
			return bitmap(width, height, rgba);
		}
		return null;
	}

	private static Bitmap bitmap(final int width, final int height, final ByteBuffer rgba) {
		final int[] pixels = new int[width * height];
		final ByteBuffer data = rgba.duplicate();
		data.position(0);
		for (int i = 0; i < pixels.length; i++) {
			final int r = data.get() & 0xFF;
			final int g = data.get() & 0xFF;
			final int b = data.get() & 0xFF;
			final int a = data.get() & 0xFF;
			pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
		}
		final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		// setPixels takes straight colours and multiplies them by alpha as bitmaps keep them.
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		return bitmap;
	}

	/**
	 * An edge file holds eight squares in a row: left, right, top, bottom and the four corners; top and bottom lie
	 * turned on their side. They are laid out as a 3 x 3 image, as BackdropFrame draws them.
	 */
	private static Bitmap nineSlice(final Bitmap edge) {
		final int s = Math.min(edge.getWidth() / 8, edge.getHeight());
		final Bitmap nine = Bitmap.createBitmap(s * 3, s * 3, Bitmap.Config.ARGB_8888);
		final Canvas canvas = new Canvas(nine);
		final int[][] places = { { 0, 1 }, { 2, 1 }, { 1, 0 }, { 1, 2 }, { 0, 0 }, { 2, 0 }, { 0, 2 }, { 2, 2 } };
		for (int i = 0; i < 8; i++) {
			final Rect source = new Rect(i * s, 0, (i + 1) * s, s);
			final int x = places[i][0] * s;
			final int y = places[i][1] * s;
			canvas.save();
			if ((i == 2) || (i == 3)) {
				canvas.translate(x + s, y);
				canvas.rotate(90);
				canvas.drawBitmap(edge, source, new Rect(0, 0, s, s), null);
			}
			else {
				canvas.drawBitmap(edge, source, new Rect(x, y, x + s, y + s), null);
			}
			canvas.restore();
		}
		return nine;
	}

	/**
	 * The menu button holder without the spike on its left: the frame part of the 512 x 128 image, its left end made
	 * from the mirrored right end so the blue button sits in the middle.
	 */
	private static Bitmap holder(final Bitmap border) {
		final float unit = border.getWidth() / 512f;
		final int left = Math.round(114 * unit);
		final int top = Math.round(14 * unit);
		final int width = Math.round(396 * unit);
		final int height = Math.round(101 * unit);
		final int end = Math.round(40 * unit);
		final Bitmap holder = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		final Canvas canvas = new Canvas(holder);
		canvas.drawBitmap(border, new Rect(left + end, top, left + width, top + height),
				new Rect(end, 0, width, height), null);
		final Matrix mirror = new Matrix();
		mirror.setScale(-1, 1);
		mirror.postTranslate(end, 0);
		canvas.save();
		canvas.concat(mirror);
		canvas.drawBitmap(border, new Rect((left + width) - end, top, left + width, top + height),
				new Rect(0, 0, end, height), null);
		canvas.restore();
		return holder;
	}

	/** Loading screens are four tiles: two square ones above and two half-height ones below. */
	private static boolean tiles(final DataSource data, final String prefix, final File target) throws IOException {
		final Bitmap topLeft = decode(data, prefix + "-TopLeft");
		final Bitmap topRight = decode(data, prefix + "-TopRight");
		final Bitmap bottomLeft = decode(data, prefix + "-BotLeft");
		final Bitmap bottomRight = decode(data, prefix + "-BotRight");
		if ((topLeft == null) || (topRight == null) || (bottomLeft == null) || (bottomRight == null)) {
			return false;
		}
		final Bitmap joined = Bitmap.createBitmap(topLeft.getWidth() + topRight.getWidth(),
				topLeft.getHeight() + bottomLeft.getHeight(), Bitmap.Config.ARGB_8888);
		final Canvas canvas = new Canvas(joined);
		canvas.drawColor(0xFF000000);
		final Paint paint = new Paint();
		canvas.drawBitmap(topLeft, 0, 0, paint);
		canvas.drawBitmap(topRight, topLeft.getWidth(), 0, paint);
		canvas.drawBitmap(bottomLeft, 0, topLeft.getHeight(), paint);
		canvas.drawBitmap(bottomRight, topLeft.getWidth(), topLeft.getHeight(), paint);
		save(joined, target, Bitmap.CompressFormat.JPEG);
		return true;
	}

	private static boolean copy(final DataSource data, final String path, final File target) throws IOException {
		if (!data.has(path)) {
			return false;
		}
		try (InputStream in = data.getResourceAsStream(path)) {
			if (in == null) {
				return false;
			}
			store(in, target);
		}
		return true;
	}

	/** The scene recordings and a first frame of each, shown until the recording plays. */
	private static boolean scene(final DataSource data, final String scene, final File target) throws IOException {
		if (!copy(data, "webui\\webms\\" + scene + ".webm", target)) {
			return false;
		}
		final MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		try {
			retriever.setDataSource(target.getAbsolutePath());
			final Bitmap frame = retriever.getFrameAtTime(0);
			if (frame != null) {
				save(frame, new File(target.getParentFile(), scene + ".jpg"), Bitmap.CompressFormat.JPEG);
			}
		}
		catch (final RuntimeException e) {
			Log.w(TAG, "No poster frame for " + scene, e);
		}
		finally {
			try {
				retriever.release();
			}
			catch (final IOException | RuntimeException e) {
				Log.w(TAG, "Unable to release the metadata reader", e);
			}
		}
		return true;
	}

	/** The glue strings the launcher shows, so that it speaks as the installed game does. */
	private static boolean strings(final DataSource data, final File target) throws IOException {
		if (!data.has(STRING_FILES[0])) {
			return false;
		}
		final Map<String, String> found = new LinkedHashMap<>();
		for (final String path : STRING_FILES) {
			if (!data.has(path)) {
				continue;
			}
			try (InputStream in = data.getResourceAsStream(path);
					BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					final Matcher matcher = STRING_LINE.matcher(line);
					if (matcher.matches()) {
						found.putIfAbsent(matcher.group(1).toUpperCase(Locale.ROOT), matcher.group(2));
					}
				}
			}
		}
		final StringBuilder text = new StringBuilder();
		for (final Map.Entry<String, String> entry : found.entrySet()) {
			text.append(entry.getKey()).append('\t').append(entry.getValue()).append('\n');
		}
		write(target, text.toString());
		return true;
	}

	private static void save(final Bitmap bitmap, final File target, final Bitmap.CompressFormat format)
			throws IOException {
		final File partial = new File(target.getParentFile(), target.getName() + ".part");
		try (OutputStream out = new FileOutputStream(partial)) {
			if (!bitmap.compress(format, 90, out)) {
				throw new IOException("Unable to encode " + target.getName());
			}
		}
		if (!partial.renameTo(target)) {
			throw new IOException("Unable to store " + target);
		}
	}

	private static byte[] bytes(final InputStream in) throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final byte[] buffer = new byte[65536];
		int read;
		while ((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
		}
		return out.toByteArray();
	}

	private static void store(final InputStream in, final File target) throws IOException {
		final File partial = new File(target.getParentFile(), target.getName() + ".part");
		try (OutputStream out = new FileOutputStream(partial)) {
			final byte[] buffer = new byte[65536];
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
		}
		if (!partial.renameTo(target)) {
			throw new IOException("Unable to store " + target);
		}
	}

	private static void write(final File file, final String text) throws IOException {
		file.getParentFile().mkdirs();
		final File partial = new File(file.getParentFile(), file.getName() + ".part");
		Files.write(partial.toPath(), text.getBytes(StandardCharsets.UTF_8));
		if (!partial.renameTo(file)) {
			throw new IOException("Unable to store " + file);
		}
	}

	private static String read(final File file) {
		if (!file.isFile()) {
			return null;
		}
		try {
			return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
		}
		catch (final IOException e) {
			return null;
		}
	}

	private static void deleteTree(final File file) {
		final File[] children = file.listFiles();
		if (children != null) {
			for (final File child : children) {
				deleteTree(child);
			}
		}
		file.delete();
	}

	/** The cached art loaded for drawing. Missing files give null, and the drawings fall back. */
	static final class Art {
		private final SkinCache cache;
		private final Map<String, Bitmap> bitmaps = new HashMap<>();
		private final Map<String, String> strings = new HashMap<>();
		private Typeface font;

		Art(final SkinCache cache) {
			this.cache = cache;
			final String text = read(cache.file(STRINGS));
			if (text != null) {
				for (final String line : text.split("\n")) {
					final int tab = line.indexOf('\t');
					if (tab > 0) {
						this.strings.put(line.substring(0, tab), line.substring(tab + 1));
					}
				}
			}
		}

		Bitmap bitmap(final String name) {
			if (!this.bitmaps.containsKey(name)) {
				final File file = this.cache.file(name);
				this.bitmaps.put(name, file.isFile() ? BitmapFactory.decodeFile(file.getAbsolutePath()) : null);
			}
			return this.bitmaps.get(name);
		}

		/** A scene's first frame in this skin, or null when the game data has no recording of it. */
		Bitmap poster(final String scene) {
			final String recorded = this.cache.sceneName(scene);
			final String name = "webms/" + recorded + ".jpg";
			if (!this.bitmaps.containsKey(name)) {
				final File file = this.cache.scene(recorded, true);
				this.bitmaps.put(name, file.isFile() ? BitmapFactory.decodeFile(file.getAbsolutePath()) : null);
			}
			return this.bitmaps.get(name);
		}

		/** The scene's recording in this skin, or null when the game data has none. */
		File recording(final String scene) {
			final File file = this.cache.scene(this.cache.sceneName(scene), false);
			return file.isFile() && (file.length() > 0) ? file : null;
		}

		/** How many of the menu and campaign recordings the game data had. */
		int recordings() {
			int count = 0;
			for (final String scene : SCENES) {
				if (recording(scene) != null) {
					count++;
				}
			}
			return count;
		}

		boolean reforged() {
			return this.cache.reforged;
		}

		int gold() {
			return this.cache.reforged ? Color.rgb(0xE6, 0xB9, 0x4A) : LauncherKit.GOLD;
		}

		int text() {
			return this.cache.reforged ? Color.rgb(0xF2, 0xEB, 0xDD) : LauncherKit.TEXT;
		}

		int text2() {
			return this.cache.reforged ? Color.rgb(0xA7, 0x9F, 0x90) : LauncherKit.TEXT2;
		}

		/** Row lines and the plain frames drawn when an edge file is missing. */
		int line() {
			return this.cache.reforged ? Color.rgb(0x4A, 0x46, 0x40) : LauncherKit.BRONZE;
		}

		boolean has(final String name) {
			return this.cache.file(name).isFile();
		}

		/** The small mission pictures: their cached name by the lower case path below the campaign pictures. */
		Map<String, String> missionPictures() {
			final Map<String, String> pictures = new LinkedHashMap<>();
			final String index = read(this.cache.file(MISSIONS));
			if (index != null) {
				final String folder = MISSIONS.substring(0, MISSIONS.indexOf('/') + 1);
				for (final String line : index.split("\n")) {
					final int tab = line.indexOf('\t');
					if (tab > 0) {
						pictures.put(line.substring(tab + 1), folder + line.substring(0, tab));
					}
				}
			}
			return pictures;
		}

		Typeface font() {
			if (this.font == null) {
				final File file = this.cache.file(FONT);
				Typeface loaded = null;
				if (file.isFile()) {
					try {
						loaded = Typeface.createFromFile(file);
					}
					catch (final RuntimeException e) {
						Log.w(TAG, "Unable to load " + file, e);
					}
				}
				if ((loaded != null) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)) {
					loaded = withGlobalFallback(file, loaded);
				}
				this.font = loaded != null ? loaded : Typeface.SERIF;
			}
			return this.font;
		}

		private Typeface withGlobalFallback(final File main, final Typeface plain) {
			final File global = this.cache.file(FONT_GLOBAL);
			if (!global.isFile()) {
				return plain;
			}
			try {
				return new Typeface.CustomFallbackBuilder(new FontFamily.Builder(new Font.Builder(main).build()).build())
						.addCustomFallback(new FontFamily.Builder(new Font.Builder(global).build()).build()).build();
			}
			catch (final IOException | RuntimeException e) {
				Log.w(TAG, "Unable to add " + global, e);
				return plain;
			}
		}

		String string(final String key, final String fallback) {
			final String value = this.strings.get(key);
			return value != null ? value : fallback;
		}
	}
}
