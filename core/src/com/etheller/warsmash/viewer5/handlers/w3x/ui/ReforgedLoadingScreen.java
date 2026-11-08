package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Blending;
import com.badlogic.gdx.graphics.Pixmap.Filter;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.TimeUtils;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.parsers.fdf.FontGeneratorHolder;
import com.etheller.warsmash.util.Log;

/**
 * The loading screen of a map as Reforged shows it, laid out like its web interface's page in a 1080 pixel high
 * design space scaled to the screen height. A campaign map has its mission picture, the crest of the player's race
 * with the chapter, the mission name and its text; the picture keeps 16:9 and is centred, with black sides on a wider
 * screen. Any other map has the parchment over the whole screen with the teams and their players beside the map's
 * preview, its author under the bar, and loading tips in the bar. Both end with its loading bar.
 */
final class ReforgedLoadingScreen {
	static final String FOLDER = "webui\\loadingscreen\\";
	private static final String CAMPAIGN = FOLDER + "campaign\\";
	private static final String DEFAULT_BACKGROUND = FOLDER + "betaloading_screen_v2.jpg";

	/**
	 * The picture of each campaign loading screen of the map information, as the web interface's style sheet assigns
	 * them (CampaignBackground-<n>); the numbers it leaves out show its default picture.
	 */
	private static final String[] BACKGROUNDS = { "roc\\01_prologue\\prologue01_chasing visions.jpg",
		"roc\\01_prologue\\prologue02_departures.jpg", "roc\\02_human\\human01_defense of strahnbrad.jpg",
		"roc\\02_human\\human02_blackrock and roll.jpg", "roc\\02_human\\human_interlude01_jaina's meeting.jpg",
		"roc\\02_human\\human03_ravages of the plague.jpg", "roc\\02_human\\human04_cult of the damned.jpg",
		"roc\\02_human\\human05_march of the scourge.jpg",
		"roc\\02_human\\human_interlude02_the prince and the prophet.jpg", "roc\\02_human\\human06_the culling.jpg",
		"roc\\02_human\\human_interlude03_divergent courses.jpg", "roc\\02_human\\human07_the shores of northrend.jpg",
		"roc\\02_human\\human08_dissension.jpg", "roc\\02_human\\human09_frostmourne.jpg",
		"roc\\03_undead\\undead01_trudging through the ashes.jpg", "roc\\03_undead\\undead02_digging up the dead.jpg",
		"roc\\03_undead\\undead_interlude01_the dreadlords convene.jpg",
		"roc\\03_undead\\undead03_into the realm eternal.jpg", "roc\\03_undead\\undead04_key of the three moons.jpg",
		"roc\\03_undead\\undead05_the fall of silvermoon.jpg", "roc\\03_undead\\undead_interlude02_the revelation.jpg",
		"roc\\03_undead\\undead06_blackrock & roll, too.jpg", "roc\\03_undead\\undead07_the siege of dalaran.jpg",
		"roc\\03_undead\\undead08_under the burning sky.jpg", "roc\\04_orc\\orc01_landfall.jpg",
		"roc\\04_orc\\orc02_the long march.jpg", "roc\\04_orc\\orc_interlude01_wreckage of lordaeron.jpg",
		"roc\\04_orc\\orc03_cry of the warsong.jpg", "roc\\04_orc\\orc04_the spirits of ashenvale.jpg",
		"roc\\04_orc\\orc_interlude02_the blood of mannaroth.jpg", "roc\\04_orc\\orc05_the hunter of shadows.jpg",
		"roc\\04_orc\\orc06_where wyverns dare.jpg", "roc\\04_orc\\orc07_the oracle.jpg",
		"roc\\04_orc\\orc08_by demons be driven.jpg", "roc\\05_nightelf\\nightelf01_enemies at the gate.jpg",
		"roc\\05_nightelf\\nightelf02_daughters of the moon.jpg",
		"roc\\05_nightelf\\nightelf03_the awakening of stormrage.jpg",
		"roc\\05_nightelf\\nightelf04_the druids arise.jpg", "roc\\05_nightelf\\nightelf05_brothers in blood.jpg",
		"roc\\05_nightelf\\nightelf06_a destiny of flame and sorrow.jpg",
		"roc\\05_nightelf\\nightelf_interlude01_the last guardian.jpg",
		"roc\\05_nightelf\\nightelf07_twilight of the gods.jpg", null, null, null, "roc_credits.jpg",
		"tft\\01_sentinel\\sentinel01_rise of the naga.jpg", "tft\\01_sentinel\\sentinel02_the broken isles.jpg",
		"tft\\01_sentinel\\sentinel03_the tomb of sargeras.jpg",
		"tft\\01_sentinel\\sentinel04_wrath of the betrayer.jpg",
		"tft\\01_sentinel\\sentinel_inerlude_01_unfinished business.jpg",
		"tft\\01_sentinel\\sentinel05_balancing the scales.jpg",
		"tft\\01_sentinel\\sentinel06_shards of the alliance.jpg",
		"tft\\01_sentinel\\sentinel_inerlude_02_malfurions vision.jpg",
		"tft\\01_sentinel\\sentinel07_the ruins of dalaran.jpg",
		"tft\\01_sentinel\\sentinel08_the brothers stromrage.jpg",
		"tft\\01_sentinel\\sentinel_finale_a_parting_of_ways.jpg",
		"tft\\02_alliance\\alliance01_misconceptions.jpg", "tft\\02_alliance\\alliance02_a dark covenant.jpg",
		"tft\\02_alliance\\alliance03_the dungeons of dalaran.jpg",
		"tft\\02_alliance\\alliance_secret level - the crossing.jpg",
		"tft\\02_alliance\\alliance_interlude_01_the dusts of outland.jpg",
		"tft\\02_alliance\\alliance04_the search for illidan.jpg",
		"tft\\02_alliance\\alliance_interlude_02_illidans task.jpg",
		"tft\\02_alliance\\alliance05_gates of the abyss.jpg", "tft\\02_alliance\\alliance06_lord of outland.jpg",
		"tft\\02_alliance\\alliance_interlude_03_kiljaedens command.jpg", "tft\\03_scourge\\scourge01_king arthas.jpg",
		"tft\\03_scourge\\scourge_interlude_01_interlude_a kingdom divided.jpg",
		"tft\\03_scourge\\scourge02_the flight from lordaeron.jpg",
		"tft\\03_scourge\\scourge_interlude_02_sylvanas farewell.jpg", "tft\\03_scourge\\scourge03_the dark lady.jpg",
		"tft\\03_scourge\\scourge04_return to northrend.jpg", "tft\\03_scourge\\scourge05_dreadlord's fall.jpg",
		"tft\\03_scourge\\scourge06_a new power in lordaeron.jpg",
		"tft\\03_scourge\\scourge07-1_into the shadow web caverns.jpg",
		"tft\\03_scourge\\scourge07-2_the forgotten ones.jpg",
		"tft\\03_scourge\\scourge07-3_ascent to the upper kingdom.jpg",
		"tft\\03_scourge\\scourge_interlude_03_boiling point.jpg",
		"tft\\03_scourge\\scourge08_a symphony of frost and flame.jpg",
		"tft\\03_scourge\\scourge_interlude_04_a long time coming.jpg",
		"tft\\04_bonus\\orc01_act_one_to_tame_a_land.jpg", "tft\\04_bonus\\orc01-1_orgrimmar tunnels.jpg",
		"tft\\04_bonus\\orc01-2_beast den.jpg", "tft\\04_bonus\\orc01-3_the echo isles.jpg",
		"tft\\04_bonus\\orc01-4_thunder ridge.jpg", "tft\\04_bonus\\orc02_act_two_old_hatred.jpg",
		"tft\\04_bonus\\orc02-1_coastal base.jpg", "tft\\04_bonus\\orc02-2_theramore island.jpg",
		"tft\\04_bonus\\orc02-3_dustwallow marsh.jpg", "tft\\04_bonus\\orc02-4_mulgore.jpg",
		"tft\\04_bonus\\orc02-5_tidefury cove.jpg", "tft\\04_bonus\\orc02-6_tomb of the ancients.jpg",
		"tft\\04_bonus\\orc02-7_den of the lost.jpg", "tft\\04_bonus\\orc01_act_one_to_tame_a_land.jpg",
		"tft\\04_bonus\\orc02-8_outland arena.jpg", "tft\\04_bonus\\orc03-1_a blaze of glory.jpg",
		"tft\\04_bonus\\orc03-2_theramore city.jpg", "roc\\01_prologue\\prologue03_riders on the storm.jpg",
		"roc\\01_prologue\\prologue04_the fires down below.jpg",
		"roc\\01_prologue\\prologue05_countdown to extinction.jpg", "rebirth\\01_human\\human01_homecoming.jpg",
		"rebirth\\01_human\\human02_sewers.jpg", "rebirth\\01_human\\human03i_interlude.jpg",
		"rebirth\\01_human\\human03_andorhal.jpg", "rebirth\\01_human\\human04_stratholme.jpg",
		"rebirth\\02_undead\\undeadre01_undercity.png", "rebirth\\02_undead\\undeadre01-02_capitalruins.png",
		"rebirth\\02_undead\\undeadre01-03_tirisfalglades.png",
		"rebirth\\02_undead\\undeadre01-04_silverpinesprint.png",
		"rebirth\\02_undead\\undeadre01-05_arcanesanctuary.png", "rebirth\\02_undead\\undeadre01-06_cathedral.png",
		"rebirth\\02_undead\\undeadre02_undercity.png", "rebirth\\02_undead\\undeadre02-01_forgottenhollow.png",
		"rebirth\\02_undead\\undeadre02-02_tirisfalgladesvillages.png",
		"rebirth\\02_undead\\undeadre02-03_blackrockencampment.png",
		"rebirth\\02_undead\\undeadre02-04_dawnswatch.png", "rebirth\\02_undead\\undeadre02-06_scarletmonastery.png",
		"rebirth\\02_undead\\undeadre03i_interlude.png", "rebirth\\02_undead\\undeadre03a_capitalruins.png",
		"rebirth\\02_undead\\undeadre03b_naxxramas.png" };
	/** The first loading screen of the Forsaken chapters, whose crest is the Forsaken one instead of the race's. */
	private static final int FIRST_FORSAKEN_BACKGROUND = 106;
	private static final String[] RACE_CRESTS = { null, "human", "orc", "undead", "nightelf" };
	/** The race icons of the player list, in the order of {@link Player#race}. */
	private static final String[] RACE_ICONS = { "human", "orc", "undead", "nightelf", "random" };
	static final int RACE_RANDOM = 4;
	private static final String MAP_BACKGROUND = FOLDER + "loadscreen_dragonlineart.png";
	private static final String DEFAULT_PORTRAIT = "webui\\portraits\\p068.png";
	/** The web interface's player colours, which differ a little from the game's. */
	private static final int[] PLAYER_COLORS = { 0xff0303, 0x0042ff, 0x1ce6b9, 0x540081, 0xfffc00, 0xfe8a0e, 0x20c000,
		0xe55bb0, 0x959697, 0x7ebff1, 0x106246, 0x4e2a04, 0x9b0000, 0x0000c3, 0x00eaff, 0xbe00fe, 0xebcd87, 0xf8a48b,
		0xbfff80, 0xdcb9eb, 0x504f55, 0xebf0ff, 0x00781e, 0xa46f33 };
	private static final long TIP_MILLIS = 5000;

	private static final float DESIGN_HEIGHT = 1080;
	private static final Color GOLD = new Color(0xffd428ff);
	private static final Color CHAPTER_GOLD = new Color(0xffdd33ff);
	private static final Color LIGHT = new Color(0xe6e6e6ff);
	private static final Color TEAM_LIGHT = new Color(0xe6e6e6d1);
	/** The page draws the teams at 82 % opacity. */
	private static final float TEAM_ALPHA = 0.82f;

	/** A player as the list of a map's loading screen shows it. */
	static final class Player {
		private final String name;
		private final int color;
		private final String raceName;
		/** The index in {@link #RACE_ICONS}. */
		private final int race;
		private final boolean local;

		Player(final String name, final int color, final String raceName, final int race, final boolean local) {
			this.name = name;
			this.color = color;
			this.raceName = raceName;
			this.race = race;
			this.local = local;
		}
	}

	/** A team of the list, with its title. */
	static final class Team {
		private final String name;
		private final List<Player> players;

		Team(final String name, final List<Player> players) {
			this.name = name;
			this.players = players;
		}
	}

	private final Texture background;
	private final ReforgedLoadingBar bar;
	private final FontGeneratorHolder fonts;

	// A campaign map's screen.
	private Texture crest;
	private String chapter = "";
	private String name = "";
	private String text = "";

	// Any other map's screen.
	private boolean mapScreen;
	private Texture ornament;
	private Texture mapFrame;
	private Texture playerFrame;
	private Texture portraitFrame;
	private Texture portraitEndLeft;
	private Texture portraitEndRight;
	private Texture defaultPortrait;
	private Texture localPortrait;
	private Texture white;
	/** White, its alpha rising from left to right. */
	private Texture ramp;
	private final Texture[] raceIcons = new Texture[RACE_ICONS.length];
	/** Owned by the caller. */
	private Texture preview;
	private String mapName = "";
	private String author = "";
	private String createdBy = "";
	private List<Team> teams = Collections.emptyList();

	private String barText = "";
	private float barAlpha = 1;
	private float ratio;
	private List<String> tips;
	private long tipShown;
	private final Random tipRandom = new Random();
	private int laidOutWidth = -1;
	private int laidOutHeight = -1;
	private String laidOutBarText;
	private boolean layoutStale;
	private final List<BitmapFontCache> detailsCaches = new ArrayList<>();
	private final List<BitmapFontCache> teamCaches = new ArrayList<>();
	private final List<BitmapFontCache> barTextCaches = new ArrayList<>();
	/** Where the player list's rows go, in screen pixels from the bottom left: x, y, scale. */
	private final List<float[]> playerRows = new ArrayList<>();
	private final List<Player> rowPlayers = new ArrayList<>();
	/** The map preview's frame in screen pixels: x, y, size. */
	private final float[] previewBox = new float[3];

	private ReforgedLoadingScreen(final Texture background, final ReforgedLoadingBar bar,
			final FontGeneratorHolder fonts) {
		this.background = background;
		this.bar = bar;
		this.fonts = fonts;
	}

	/**
	 * @param race the local player's race as the map information numbers races (1 human, 2 orc, 3 undead, 4 night
	 *             elf)
	 * @return null when the game data has no Reforged web interface
	 */
	static ReforgedLoadingScreen create(final DataSource dataSource, final FontGeneratorHolder fonts,
			final int campaignBackground, final int race, final String chapter, final String name,
			final String text) {
		if (!dataSource.has(ReforgedLoadingBar.FRAME)) {
			return null;
		}
		String picture = (campaignBackground >= 0) && (campaignBackground < BACKGROUNDS.length)
				? BACKGROUNDS[campaignBackground]
				: null;
		picture = picture == null ? DEFAULT_BACKGROUND : CAMPAIGN + picture;
		final String crestName = campaignBackground >= FIRST_FORSAKEN_BACKGROUND ? "forsaken"
				: RACE_CRESTS[(race > 0) && (race < RACE_CRESTS.length) ? race : 1];
		final ReforgedLoadingScreen screen = createWithBar(dataSource, fonts, load(dataSource, picture, 0));
		if (screen != null) {
			screen.crest = load(dataSource, CAMPAIGN + "crests\\wc3_map_raceicon_" + crestName + ".png", 0);
			screen.chapter = plain(chapter);
			screen.name = plain(name);
			screen.text = plain(text);
		}
		return screen;
	}

	/**
	 * The screen of a map with its own loading screen model, which is drawn behind this one: a campaign screen
	 * without its picture.
	 *
	 * @return null when the game data has no Reforged web interface
	 */
	static ReforgedLoadingScreen createOverModel(final DataSource dataSource, final FontGeneratorHolder fonts,
			final int race, final String chapter, final String name, final String text) {
		if (!dataSource.has(ReforgedLoadingBar.FRAME)) {
			return null;
		}
		final ReforgedLoadingBar bar = ReforgedLoadingBar.load(dataSource);
		if (bar == null) {
			return null;
		}
		final ReforgedLoadingScreen screen = new ReforgedLoadingScreen(null, bar, fonts);
		final String crestName = RACE_CRESTS[(race > 0) && (race < RACE_CRESTS.length) ? race : 1];
		screen.crest = load(dataSource, CAMPAIGN + "crests\\wc3_map_raceicon_" + crestName + ".png", 0);
		screen.chapter = plain(chapter);
		screen.name = plain(name);
		screen.text = plain(text);
		return screen;
	}

	/**
	 * The screen of a map without a campaign loading screen. Its players are listed once they are known.
	 *
	 * @param preview   the map's preview image; owned by the caller, and may be changed later
	 * @param createdBy the text before the author's name
	 * @param portrait  the number of the local player's portrait among the web interface's portraits (from 1), or 0
	 *                  for the default one that the others have
	 * @return null when the game data has no Reforged web interface
	 */
	static ReforgedLoadingScreen createForMap(final DataSource dataSource, final FontGeneratorHolder fonts,
			final String mapName, final String author, final String createdBy, final Texture preview,
			final int portrait) {
		if (!dataSource.has(ReforgedLoadingBar.FRAME)) {
			return null;
		}
		// The parchment is much larger than any screen; a texture that big would not fit every device.
		final int screenSize = Math.min(4096, Math.max(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
		final ReforgedLoadingScreen screen = createWithBar(dataSource, fonts,
				load(dataSource, MAP_BACKGROUND, screenSize));
		if (screen == null) {
			return null;
		}
		screen.mapScreen = true;
		screen.ornament = texture(contrast(loadPixmap(dataSource, FOLDER + "bottom_center_goth_piece.png", screenSize),
				0.8f));
		screen.mapFrame = load(dataSource, FOLDER + "loadscreen_mapframe.png", 0);
		screen.playerFrame = load(dataSource, "webui\\widgets\\textfields\\war3_text_normal_4k.png", 0);
		screen.portraitFrame = load(dataSource, "webui\\widgets\\playerwidget\\portrait_frame_large.png", 0);
		screen.portraitEndLeft = load(dataSource, "webui\\widgets\\buttons\\war3_button_frame_end_left_alt.png", 0);
		screen.portraitEndRight = load(dataSource, "webui\\widgets\\buttons\\war3_button_frame_end_right_alt.png", 0);
		screen.defaultPortrait = load(dataSource, DEFAULT_PORTRAIT, 0);
		if (portrait > 0) {
			screen.localPortrait = load(dataSource,
					String.format(Locale.ROOT, "webui\\portraits\\p%03d.png", portrait), 0);
		}
		for (int i = 0; i < RACE_ICONS.length; i++) {
			screen.raceIcons[i] = load(dataSource, "webui\\icons\\versus\\" + RACE_ICONS[i] + "-icon-resting.png", 0);
		}
		final Pixmap pixel = new Pixmap(1, 1, Format.RGBA8888);
		pixel.setColor(Color.WHITE);
		pixel.fill();
		screen.white = new Texture(pixel);
		pixel.dispose();
		final Pixmap ramp = new Pixmap(256, 1, Format.RGBA8888);
		ramp.setBlending(Blending.None);
		for (int i = 0; i < 256; i++) {
			ramp.drawPixel(i, 0, 0xffffff00 | i);
		}
		screen.ramp = new Texture(ramp);
		screen.ramp.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		ramp.dispose();
		screen.preview = preview;
		screen.mapName = plain(mapName);
		screen.author = plain(author);
		screen.createdBy = plain(createdBy);
		return screen;
	}

	private static ReforgedLoadingScreen createWithBar(final DataSource dataSource, final FontGeneratorHolder fonts,
			final Texture background) {
		final ReforgedLoadingBar bar = background == null ? null : ReforgedLoadingBar.load(dataSource);
		if (bar == null) {
			dispose(background);
			return null;
		}
		return new ReforgedLoadingScreen(background, bar, fonts);
	}

	/** @param maxSize the largest side the texture may have, scaling the image down to it; 0 for any */
	static Texture load(final DataSource dataSource, final String path, final int maxSize) {
		return texture(loadPixmap(dataSource, path, maxSize));
	}

	private static Pixmap loadPixmap(final DataSource dataSource, final String path, final int maxSize) {
		try {
			if (!dataSource.has(path)) {
				return null;
			}
			final ByteBuffer buffer = dataSource.read(path);
			if (buffer == null) {
				return null;
			}
			final ByteBuffer bytes = buffer.duplicate();
			bytes.position(0);
			final byte[] encoded = new byte[bytes.remaining()];
			bytes.get(encoded);
			final Pixmap pixmap = new Pixmap(encoded, 0, encoded.length);
			final int largest = Math.max(pixmap.getWidth(), pixmap.getHeight());
			if ((maxSize <= 0) || (largest <= maxSize)) {
				return pixmap;
			}
			final int width = Math.max(1, Math.round(((float) pixmap.getWidth() * maxSize) / largest));
			final int height = Math.max(1, Math.round(((float) pixmap.getHeight() * maxSize) / largest));
			final Pixmap scaled = new Pixmap(width, height, pixmap.getFormat());
			scaled.setBlending(Blending.None);
			scaled.setFilter(Filter.BiLinear);
			scaled.drawPixmap(pixmap, 0, 0, pixmap.getWidth(), pixmap.getHeight(), 0, 0, width, height);
			pixmap.dispose();
			return scaled;
		}
		catch (final IOException | RuntimeException e) {
			Log.error("Unable to load " + path + ": " + e);
			return null;
		}
	}

	private static Texture texture(final Pixmap pixmap) {
		if (pixmap == null) {
			return null;
		}
		final Texture texture = new Texture(pixmap);
		pixmap.dispose();
		texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		return texture;
	}

	/** The CSS contrast filter: each colour channel moves towards the middle grey. */
	private static Pixmap contrast(final Pixmap pixmap, final float amount) {
		if (pixmap == null) {
			return null;
		}
		final int channels = pixmap.getFormat() == Format.RGBA8888 ? 4 : pixmap.getFormat() == Format.RGB888 ? 3 : 0;
		if (channels == 0) {
			return pixmap;
		}
		final ByteBuffer pixels = pixmap.getPixels();
		final byte[] data = new byte[pixmap.getWidth() * pixmap.getHeight() * channels];
		pixels.position(0);
		pixels.get(data);
		for (int i = 0; i < data.length; i++) {
			if ((i % channels) != 3) {
				data[i] = (byte) Math.round((((data[i] & 0xff) - 127.5f) * amount) + 127.5f);
			}
		}
		pixels.position(0);
		pixels.put(data);
		pixels.position(0);
		return pixmap;
	}

	/** Map text without its color codes; "|n" breaks a line as in the game. */
	private static String plain(final String text) {
		if (text == null) {
			return "";
		}
		return text.replaceAll("(?i)\\|c[0-9a-f]{8}", "").replaceAll("(?i)\\|r", "").replaceAll("(?i)\\|n", "\n")
				.trim();
	}

	void setRatio(final float ratio) {
		this.ratio = Math.max(0, Math.min(1, ratio));
	}

	/** The text inside the bar; its alpha pulses while the screen waits for a tap. */
	void setBarText(final String barText, final float alpha) {
		this.barText = barText == null ? "" : barText;
		this.barAlpha = alpha;
		this.tips = null;
	}

	/** Shows the first tip in the bar, then a random one every five seconds, as the page does for other maps. */
	void setLoadingTips(final List<String> tips) {
		if (tips.isEmpty()) {
			return;
		}
		setBarText(tips.get(0), 1);
		this.tips = tips;
		this.tipShown = TimeUtils.millis();
	}

	/** Lists the teams beside the map preview. */
	void setTeams(final List<Team> teams) {
		this.teams = teams;
		this.layoutStale = true;
	}

	/** A new preview image of the map, after the caller replaced the one it gave. */
	void setPreview(final Texture preview) {
		this.preview = preview;
	}

	boolean isMapScreen() {
		return this.mapScreen;
	}

	/** Draws over the whole screen; the batch is drawing and projects screen pixels. */
	void render(final SpriteBatch batch, final int width, final int height) {
		if ((this.tips != null) && ((TimeUtils.millis() - this.tipShown) >= TIP_MILLIS)) {
			this.barText = this.tips.get(this.tipRandom.nextInt(this.tips.size()));
			this.tipShown = TimeUtils.millis();
		}
		if (this.layoutStale || (width != this.laidOutWidth) || (height != this.laidOutHeight)
				|| !this.barText.equals(this.laidOutBarText)) {
			layOut(width, height);
		}
		batch.setColor(Color.WHITE);
		if (this.mapScreen) {
			renderMap(batch, width, height);
		}
		else {
			renderCampaign(batch, width, height);
		}
		for (final BitmapFontCache cache : this.barTextCaches) {
			// Not draw(batch, alpha), which would give the shadow the text's colour.
			cache.setAlphas(this.barAlpha);
			cache.draw(batch);
		}
	}

	private void renderCampaign(final SpriteBatch batch, final int width, final int height) {
		final float s = height / DESIGN_HEIGHT;
		final float pictureW = Math.min((height * 16f) / 9f, width);
		final float pictureX = (width - pictureW) / 2;

		// The picture covers its 16:9 box, cut at the sides when the screen is narrower.
		if (this.background != null) {
			drawCover(batch, this.background, pictureX, 0, pictureW, height);
		}

		final float detailsW = 600 * s;
		final float detailsX = (pictureX + (pictureW * 0.9f)) - detailsW;
		final float detailsTop = 0.08f * height;
		if (this.crest != null) {
			final float crestH = 460 * s;
			final float crestW = (crestH * this.crest.getWidth()) / this.crest.getHeight();
			batch.draw(this.crest, detailsX + ((detailsW - crestW) / 2), height - detailsTop - crestH, crestW, crestH);
		}
		for (final BitmapFontCache cache : this.detailsCaches) {
			cache.draw(batch);
		}

		// LoadingBar: 1100 x 70, 80 above the bottom.
		this.bar.draw(batch, pictureX + ((pictureW - (1100 * s)) / 2), 80 * s, s, this.ratio);
	}

	private void renderMap(final SpriteBatch batch, final int width, final int height) {
		final float s = height / DESIGN_HEIGHT;
		// The parchment covers the whole screen, unlike a campaign picture.
		drawCover(batch, this.background, 0, 0, width, height);

		if (this.ornament != null) {
			// Bottom-Ornament: 1198 x 340 at the bottom centre, the image contained in it from its bottom.
			final float boxW = 1198 * s;
			final float boxH = 340 * s;
			final float scale = Math.min(boxW / this.ornament.getWidth(), boxH / this.ornament.getHeight());
			final float w = this.ornament.getWidth() * scale;
			batch.draw(this.ornament, (width - w) / 2, 0, w, this.ornament.getHeight() * scale);
		}

		// The bar is 1100 x 70 at the bottom centre, above the author line (18 high) and margins of 50 and 82.
		this.bar.draw(batch, (width - (1100 * s)) / 2, 150 * s, s, this.ratio);
		for (final BitmapFontCache cache : this.detailsCaches) {
			cache.draw(batch);
		}

		// MapImageHolder: 340 x 340, a frame image cut at 28 drawn 14 wide, around the 311 x 311 preview.
		final float frameX = this.previewBox[0];
		final float frameY = this.previewBox[1];
		final float frameSize = this.previewBox[2];
		if (this.preview != null) {
			final float box = (frameSize * 311) / 340;
			final float scale = Math.min(box / this.preview.getWidth(), box / this.preview.getHeight());
			final float w = this.preview.getWidth() * scale;
			final float h = this.preview.getHeight() * scale;
			batch.draw(this.preview, frameX + ((frameSize - w) / 2), frameY + ((frameSize - h) / 2), w, h);
		}
		if (this.mapFrame != null) {
			final float border = (frameSize * 14) / 340;
			drawFrame(batch, this.mapFrame, frameX, frameY, frameSize, frameSize, new float[] { 28, 28, 28, 28 },
					new float[] { border, border, border, border });
		}

		for (int i = 0; i < this.playerRows.size(); i++) {
			final float[] row = this.playerRows.get(i);
			drawPlayer(batch, this.rowPlayers.get(i), row[0], row[1], row[2]);
		}
		for (final BitmapFontCache cache : this.teamCaches) {
			cache.draw(batch);
		}
		batch.setColor(Color.WHITE);
	}

	/**
	 * One row of the player list (PlayerLoadingWidget, 390 x 90): a text field frame, the portrait in its frame, a
	 * band in the player's colour behind the name, and the race's icon at the right; the text is drawn later.
	 *
	 * @param x     left of the row
	 * @param top   top of the row, from the bottom of the screen
	 * @param scale screen pixels for one pixel of the page
	 */
	private void drawPlayer(final SpriteBatch batch, final Player player, final float x, final float top,
			final float scale) {
		batch.setColor(1, 1, 1, TEAM_ALPHA);
		if (this.playerFrame != null) {
			final float border = 30 * scale;
			drawFrame(batch, this.playerFrame, x, top - (90 * scale), 390 * scale, 90 * scale,
					new float[] { 48, 48, 48, 48 }, new float[] { border, border, border, border });
		}
		// The band of a player who is ready: black to grey at 35 %, then to the player's colour at 80 % alpha.
		final Color color = playerColor(player.color);
		final float bandLeft = x + (52 * scale);
		final float bandMiddle = bandLeft + (326 * scale * 0.35f);
		final float bandRight = x + (378 * scale);
		final float bandTop = top - (5 * scale);
		final float bandBottom = top - (72 * scale);
		final float grey = 30 / 255f;
		gradient(batch, bandLeft, bandBottom, bandMiddle, bandTop, new Color(0, 0, 0, TEAM_ALPHA),
				new Color(grey, grey, grey, 1));
		gradient(batch, bandMiddle, bandBottom, bandRight, bandTop, new Color(grey, grey, grey, TEAM_ALPHA),
				new Color(color.r, color.g, color.b, 0.8f));
		batch.setColor(1, 1, 1, TEAM_ALPHA);

		final Texture portrait = (player.local && (this.localPortrait != null)) ? this.localPortrait
				: this.defaultPortrait;
		if (portrait != null) {
			batch.draw(portrait, x + (11 * scale), top - (75 * scale), 68 * scale, 68 * scale);
		}
		if (this.portraitFrame != null) {
			drawFrame(batch, this.portraitFrame, x + (7 * scale), top - (78 * scale), 75 * scale, 75 * scale,
					new float[] { 55, 55, 55, 55 }, new float[] { 13 * scale, 14 * scale, 13 * scale, 14 * scale });
		}
		if (this.portraitEndLeft != null) {
			batch.draw(this.portraitEndLeft, x - (11 * scale), top - (52 * scale), 23 * scale, 23 * scale);
		}
		if (this.portraitEndRight != null) {
			batch.draw(this.portraitEndRight, x + (75 * scale), top - (52 * scale), 23 * scale, 23 * scale);
		}
		// The icon is 96 x 96 and stands 14 above the row; its picture covers it from its top left.
		final Texture icon = this.raceIcons[player.race];
		if (icon != null) {
			final float pictureH = ((float) icon.getHeight() * 96) / icon.getWidth();
			batch.draw(icon, x + (340 * scale), top - (82 * scale), 96 * scale, 96 * scale, 0,
					Math.min(1, 96 / pictureH), 1, 0);
		}
		batch.setColor(Color.WHITE);
	}

	private static Color playerColor(final int index) {
		final int rgb = PLAYER_COLORS[Math.floorMod(index, PLAYER_COLORS.length)];
		return new Color((((rgb >> 16) & 0xff) / 255f), ((rgb >> 8) & 0xff) / 255f, (rgb & 0xff) / 255f, 1);
	}

	/**
	 * A rectangle shaded from its left colour to its right colour: the right one is laid over the left one through a
	 * texture, since colours given to the corners came out in coarse steps on some devices.
	 */
	private void gradient(final SpriteBatch batch, final float x1, final float y1, final float x2, final float y2,
			final Color left, final Color right) {
		batch.setColor(left);
		batch.draw(this.white, x1, y1, x2 - x1, y2 - y1);
		batch.setColor(right);
		batch.draw(this.ramp, x1, y1, x2 - x1, y2 - y1, 0.5f / 256, 0, 255.5f / 256, 1);
	}

	/** An image covering a box, centred and cut at the sides or at the top and bottom. */
	private static void drawCover(final SpriteBatch batch, final Texture texture, final float x, final float y,
			final float w, final float h) {
		final float cover = Math.max(w / texture.getWidth(), h / (float) texture.getHeight());
		final float uSpan = w / (texture.getWidth() * cover);
		final float vSpan = h / (texture.getHeight() * cover);
		batch.draw(texture, x, y, w, h, (1 - uSpan) / 2, (1 + vSpan) / 2, (1 + uSpan) / 2, (1 - vSpan) / 2);
	}

	/** A whole frame image cut into nine parts: the corners keep their size, the sides and middle stretch. */
	static void drawFrame(final SpriteBatch batch, final Texture texture, final float x, final float y,
			final float w, final float h, final float[] slice, final float[] border) {
		final float tw = texture.getWidth();
		final float th = texture.getHeight();
		// top, right, bottom, left
		final float[] xs = { x, x + border[3], (x + w) - border[1], x + w };
		final float[] ys = { (y + h), (y + h) - border[0], y + border[2], y };
		final float[] us = { 0, slice[3] / tw, (tw - slice[1]) / tw, 1 };
		final float[] vs = { 0, slice[0] / th, (th - slice[2]) / th, 1 };
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 3; column++) {
				final float partW = xs[column + 1] - xs[column];
				final float partH = ys[row] - ys[row + 1];
				if ((partW > 0) && (partH > 0)) {
					batch.draw(texture, xs[column], ys[row + 1], partW, partH, us[column], vs[row + 1], us[column + 1],
							vs[row]);
				}
			}
		}
	}

	private void layOut(final int width, final int height) {
		this.laidOutWidth = width;
		this.laidOutHeight = height;
		this.laidOutBarText = this.barText;
		this.layoutStale = false;
		this.detailsCaches.clear();
		this.teamCaches.clear();
		this.barTextCaches.clear();
		this.playerRows.clear();
		this.rowPlayers.clear();
		final float s = height / DESIGN_HEIGHT;
		if (this.mapScreen) {
			layOutMap(width, height, s);
			layOutBarText(width / 2f, 150 * s, s, height);
		}
		else {
			final float pictureW = Math.min((height * 16f) / 9f, width);
			final float pictureX = (width - pictureW) / 2;
			layOutCampaign(pictureX, pictureW, s, height);
			layOutBarText(pictureX + (pictureW / 2), 80 * s, s, height);
		}
	}

	private void layOutCampaign(final float pictureX, final float pictureW, final float s, final int height) {
		final float detailsW = 600 * s;
		final float detailsX = (pictureX + (pictureW * 0.9f)) - detailsW;
		final float centre = detailsX + (detailsW / 2);
		float top = (0.08f * height) + (460 * s);

		final BitmapFont chapterFont = font(18 * s);
		// Map-CustomTitle: 18, capitals, letter spacing 0.1 em, line height 0.9.
		top = line(this.detailsCaches, chapterFont, null, this.chapter.toUpperCase(Locale.ROOT), 0.1f * 18 * s, centre,
				top, 18 * 0.9f * s, CHAPTER_GOLD, 2 * s, height);
		// Map-Name: 36, small capitals, letter spacing 0.04 em, 4 below.
		final BitmapFont nameFont = font(36 * s);
		final BitmapFont smallCapsFont = font(36 * 0.75f * s);
		top = line(this.detailsCaches, nameFont, smallCapsFont, this.name, 0.04f * 36 * s, centre, top + (2 * s),
				36 * 1.2f * s, GOLD, 2 * s, height) + (4 * s);
		// Map-Description: 18, white, centred, letter spacing 0.06 em, line height 1.2.
		final BitmapFont textFont = font(18 * s);
		for (final String textLine : wrap(textFont, this.text, 0.06f * 18 * s, detailsW)) {
			top = line(this.detailsCaches, textFont, null, textLine, 0.06f * 18 * s, centre, top, 18 * 1.2f * s,
					Color.WHITE, 2 * s, height);
		}
	}

	/** LoadingBarText: 30, capitals, letter spacing 0.22 em, centred in the bar and 4 higher. */
	private void layOutBarText(final float centre, final float barBottom, final float s, final int height) {
		final BitmapFont barFont = font(30 * s);
		final float barTop = height - barBottom - (70 * s);
		final float lineH = 30 * 1.125f * s;
		line(this.barTextCaches, barFont, null, this.barText.toUpperCase(Locale.ROOT), 0.22f * 30 * s, centre,
				(barTop + (((70 * s) - lineH) / 2)) - (4 * s), lineH, GOLD, 2 * s, height);
	}

	/**
	 * GameInfo: the first half of the teams left of the map, the others right of it, in columns of at most 780 that
	 * grow outwards; all of it centred, 75 to 855 from the top. Under the bar, MapAuthorHolder.
	 */
	private void layOutMap(final int width, final int height, final float s) {
		// MapAuthorHolder: "Map Created By:" 15 light, the author 18 gold, small capitals, 50 above the bottom margin.
		// Reforged leaves the line out for the maps that come with the game.
		if (!this.author.isEmpty() && !isBlizzardAuthor(this.author)) {
			final BitmapFont byFont = font(15 * s);
			final BitmapFont bySmall = font(15 * 0.75f * s);
			final BitmapFont authorFont = font(18 * s);
			final BitmapFont authorSmall = font(18 * 0.75f * s);
			final String by = this.createdBy.isEmpty() ? "" : this.createdBy + " ";
			final float byW = width(byFont, bySmall, by, 0.08f * 15 * s);
			final float authorW = width(authorFont, authorSmall, this.author, 0.1f * 18 * s);
			final float authorTop = height - ((82 + 50 + 18) * s);
			final float authorLeft = (width - byW - authorW) / 2;
			place(this.detailsCaches, byFont, bySmall, by, 0.08f * 15 * s, authorLeft, authorTop, 18 * s, LIGHT,
					2 * s, height);
			place(this.detailsCaches, authorFont, authorSmall, this.author, 0.1f * 18 * s, authorLeft + byW,
					authorTop, 18 * s, GOLD, 2 * s, height);
		}

		// getPlayerCountScaling of the page: rows shrink with more players.
		int players = 0;
		int largestTeam = 0;
		for (final Team team : this.teams) {
			players += team.players.size();
			largestTeam = Math.max(largestTeam, team.players.size());
		}
		float rowScale = largestTeam <= 4 ? 1
				: players <= 6 ? 0.9f
						: players <= 8 ? 0.85f
								: players <= 12 ? 0.8f : players <= 14 ? 0.75f : players <= 16 ? 0.7f : 0.6f;
		rowScale = players <= 4 ? 1
				: Math.min(rowScale, players <= 8 ? 0.9f : players <= 16 ? 0.8f : players <= 24 ? 0.7f : 0.6f);

		final int leftCount = this.teams.size() / 2;
		final List<List<Team>> leftColumns = columns(this.teams.subList(0, leftCount), rowScale);
		final List<List<Team>> rightColumns = columns(this.teams.subList(leftCount, this.teams.size()), rowScale);
		// A team is a row wide with 37 on each side.
		final float columnW = (390 * rowScale) + 74;
		final float leftW = leftColumns.size() * columnW;
		final float rightW = rightColumns.size() * columnW;
		final float designW = width / s;
		final float left = (designW - (leftW + 70 + 340 + 48 + rightW + 22)) / 2;
		final float centreY = 75 + (780 / 2f);

		final float mapX = left + leftW + 70;
		final float mapTop = centreY - 170;
		this.previewBox[0] = mapX * s;
		this.previewBox[1] = height - ((mapTop + 340) * s);
		this.previewBox[2] = 340 * s;
		// MapTitle: 30, capitals, gold, standing on the frame and as wide as it.
		final BitmapFont titleFont = font(30 * s);
		final List<String> titleLines = wrap(titleFont, this.mapName.toUpperCase(Locale.ROOT), 0, 340 * s);
		float titleTop = (mapTop * s) - (titleLines.size() * 36 * s);
		for (final String titleLine : titleLines) {
			titleTop = line(this.detailsCaches, titleFont, null, titleLine, 0, (mapX + 170) * s, titleTop, 36 * s, GOLD,
					2 * s, height);
		}

		// The left side wraps its columns outwards, to the left of the first.
		for (int i = 0; i < leftColumns.size(); i++) {
			layOutColumn(leftColumns.get(i), (left + leftW) - ((i + 1) * columnW), centreY,
					columnHeight(leftColumns, rowScale), rowScale, s, height);
		}
		for (int i = 0; i < rightColumns.size(); i++) {
			layOutColumn(rightColumns.get(i), mapX + 340 + 48 + (i * columnW), centreY,
					columnHeight(rightColumns, rowScale), rowScale, s, height);
		}
	}

	private static boolean isBlizzardAuthor(final String author) {
		final String name = author.trim().toLowerCase(Locale.ROOT);
		return name.equals("blizzard") || name.equals("blizzard entertainment");
	}

	/** The teams of one side in columns of at most 780 high; a team never breaks. */
	private static List<List<Team>> columns(final List<Team> teams, final float rowScale) {
		final List<List<Team>> columns = new ArrayList<>();
		List<Team> column = null;
		float columnH = 0;
		for (final Team team : teams) {
			final float teamH = teamHeight(team, rowScale);
			if ((column == null) || (((columnH + teamH) > 780) && !column.isEmpty())) {
				column = new ArrayList<>();
				columns.add(column);
				columnH = 0;
			}
			column.add(team);
			columnH += teamH;
		}
		return columns;
	}

	/** TeamName is 24 with a line height of 0.9 and 3 below it; then 90 for each row. */
	private static float teamHeight(final Team team, final float rowScale) {
		return (24 * 0.9f) + 3 + (team.players.size() * 90 * rowScale);
	}

	/** A side is as high as its highest column and centred on the middle of GameInfo. */
	private static float columnHeight(final List<List<Team>> columns, final float rowScale) {
		float highest = 0;
		for (final List<Team> column : columns) {
			float columnH = 0;
			for (final Team team : column) {
				columnH += teamHeight(team, rowScale);
			}
			highest = Math.max(highest, columnH);
		}
		return highest;
	}

	private void layOutColumn(final List<Team> column, final float columnX, final float centreY, final float sideH,
			final float rowScale, final float s, final int height) {
		final BitmapFont teamFont = font(24 * s);
		final BitmapFont teamSmall = font(24 * 0.75f * s);
		final float z = rowScale * s;
		final BitmapFont nameFont = font(24 * z);
		final BitmapFont nameSmall = font(24 * 0.75f * z);
		final BitmapFont raceFont = font(16 * z);
		final BitmapFont raceSmall = font(16 * 0.75f * z);
		final float x = columnX + 37;
		float top = centreY - (sideH / 2);
		for (final Team team : column) {
			// TeamName: 24, small capitals, light, 7 in.
			place(this.teamCaches, teamFont, teamSmall, team.name, 0.08f * 24 * s, (x + 7) * s, top * s,
					24 * 0.9f * s, TEAM_LIGHT, 1 * s, height);
			top += (24 * 0.9f) + 3;
			for (final Player player : team.players) {
				final float rowX = x * s;
				final float rowTop = top * s;
				this.playerRows.add(new float[] { rowX, height - rowTop, z });
				this.rowPlayers.add(player);
				// PlayerName: 24 in the player's colour, 14 from the top and 106 in, cut at 240 wide.
				final float tracking = 0.06f * 24 * z;
				final String name = fit(nameFont, nameSmall, player.name, tracking, 240 * z);
				final Color nameColor = playerColor(player.color);
				nameColor.a = TEAM_ALPHA;
				place(this.teamCaches, nameFont, nameSmall, name, tracking, rowX + (106 * z), rowTop + (14 * z),
						24 * 1.1f * z, nameColor, 1 * z, height);
				// Race-Name: 16, light, ending at the icon, its bottom 27 above the row's bottom.
				final float raceTracking = 0.08f * 16 * z;
				final float raceW = width(raceFont, raceSmall, player.raceName, raceTracking);
				place(this.teamCaches, raceFont, raceSmall, player.raceName, raceTracking,
						(rowX + (340 * z)) - raceW, rowTop + ((63 - (16 * 1.1f)) * z), 16 * 1.1f * z, TEAM_LIGHT, 1 * z,
						height);
				top += 90 * rowScale;
			}
		}
	}

	/** A text cut with an ellipsis to fit a width. */
	private static String fit(final BitmapFont font, final BitmapFont smallCaps, final String text,
			final float tracking, final float maxWidth) {
		if (width(font, smallCaps, text, tracking) <= maxWidth) {
			return text;
		}
		for (int end = text.length() - 1; end > 0; end--) {
			final String cut = text.substring(0, end).trim() + "...";
			if (width(font, smallCaps, cut, tracking) <= maxWidth) {
				return cut;
			}
		}
		return text;
	}

	private BitmapFont font(final float size) {
		final FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = Math.max(6, Math.round(size));
		return this.fonts.generateFont(parameter);
	}

	/**
	 * Adds one line of text, centred on a point, with a shadow below it; small capitals use a second, smaller font for
	 * the lower case letters.
	 *
	 * @param top    top of the line box, from the top of the screen
	 * @return the top of the next line
	 */
	private static float line(final List<BitmapFontCache> caches, final BitmapFont font, final BitmapFont smallCaps,
			final String text, final float tracking, final float centre, final float top, final float lineHeight,
			final Color color, final float shadow, final int screenHeight) {
		return place(caches, font, smallCaps, text, tracking, centre - (width(font, smallCaps, text, tracking) / 2),
				top, lineHeight, color, shadow, screenHeight);
	}

	/**
	 * Adds one line of text from a left edge, as {@link #line} does.
	 *
	 * @return the top of the next line
	 */
	private static float place(final List<BitmapFontCache> caches, final BitmapFont font, final BitmapFont smallCaps,
			final String text, final float tracking, final float left, final float top, final float lineHeight,
			final Color color, final float shadow, final int screenHeight) {
		if (text.isEmpty()) {
			return top + lineHeight;
		}
		final float capHeight = font.getCapHeight();
		final float capTop = (top + (lineHeight / 2)) - (capHeight / 2);
		final Color shadowColor = new Color(0, 0, 0, color.a);
		for (int pass = 0; pass < 2; pass++) {
			float x = left;
			for (int i = 0; i < text.length(); i++) {
				final char c = text.charAt(i);
				final boolean small = (smallCaps != null) && Character.isLowerCase(c);
				final BitmapFont glyphFont = small ? smallCaps : font;
				final String glyph = small ? String.valueOf(Character.toUpperCase(c)) : String.valueOf(c);
				// A smaller letter stands on the same baseline.
				final float drop = capHeight - glyphFont.getCapHeight();
				final float y = screenHeight - capTop - drop - (pass == 0 ? shadow : 0);
				cacheFor(caches, glyphFont, pass == 0 ? shadowColor : color).addText(glyph, x, y);
				x += advance(glyphFont, glyph.charAt(0)) + tracking;
			}
		}
		return top + lineHeight;
	}

	/** The width of a line as {@link #place} lays it out. */
	private static float width(final BitmapFont font, final BitmapFont smallCaps, final String text,
			final float tracking) {
		float width = 0;
		for (int i = 0; i < text.length(); i++) {
			final char c = text.charAt(i);
			final boolean small = (smallCaps != null) && Character.isLowerCase(c);
			width += advance(small ? smallCaps : font, small ? Character.toUpperCase(c) : c) + tracking;
		}
		return width;
	}

	/**
	 * The cache of a list that draws with a font in a colour; caches hold the glyphs of one font and one colour each,
	 * as a cache draws all of its text in the colour last set on it.
	 */
	private static BitmapFontCache cacheFor(final List<BitmapFontCache> caches, final BitmapFont font,
			final Color color) {
		for (final BitmapFontCache cache : caches) {
			if ((cache.getFont() == font) && cache.getColor().equals(color)) {
				return cache;
			}
		}
		final BitmapFontCache cache = new BitmapFontCache(font, true);
		cache.setColor(color);
		caches.add(cache);
		return cache;
	}

	private static float advance(final BitmapFont font, final char c) {
		final Glyph glyph = font.getData().getGlyph(c);
		if (glyph != null) {
			return glyph.xadvance * font.getScaleX();
		}
		final Glyph space = font.getData().getGlyph(' ');
		return space == null ? font.getSpaceXadvance() : space.xadvance * font.getScaleX();
	}

	/** The lines a text breaks into at spaces and line breaks within a width. */
	private static List<String> wrap(final BitmapFont font, final String text, final float tracking,
			final float maxWidth) {
		final List<String> lines = new ArrayList<>();
		for (final String paragraph : text.split("\n", -1)) {
			final StringBuilder current = new StringBuilder();
			float currentWidth = 0;
			for (final String word : paragraph.split(" ")) {
				float wordWidth = 0;
				for (int i = 0; i < word.length(); i++) {
					wordWidth += advance(font, word.charAt(i)) + tracking;
				}
				final float spaceWidth = advance(font, ' ') + tracking;
				if ((current.length() > 0) && ((currentWidth + spaceWidth + wordWidth) > maxWidth)) {
					lines.add(current.toString());
					current.setLength(0);
					currentWidth = 0;
				}
				if (current.length() > 0) {
					current.append(' ');
					currentWidth += spaceWidth;
				}
				current.append(word);
				currentWidth += wordWidth;
			}
			lines.add(current.toString());
		}
		return lines;
	}

	void dispose() {
		this.detailsCaches.clear();
		this.teamCaches.clear();
		this.barTextCaches.clear();
		this.bar.dispose();
		dispose(this.background, this.crest, this.ornament, this.mapFrame,
				this.playerFrame, this.portraitFrame, this.portraitEndLeft, this.portraitEndRight, this.defaultPortrait,
				this.localPortrait, this.white, this.ramp);
		dispose(this.raceIcons);
	}

	private static void dispose(final Texture... textures) {
		for (final Texture texture : textures) {
			if (texture != null) {
				texture.dispose();
			}
		}
	}
}
