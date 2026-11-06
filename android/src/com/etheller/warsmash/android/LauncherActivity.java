package com.etheller.warsmash.android;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.math.Rectangle;
import com.etheller.warsmash.GlueBackdropRenderer;
import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.viewer5.FogSettings;
import com.etheller.warsmash.viewer5.handlers.w3x.save.SavedGame;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CCampaignProgress;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.menu.CampaignCatalog;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.menu.CampaignMission;

/**
 * The launcher after the game data is chosen: prepares its art from the game data once (Prep), then shows the main
 * menu (Home), the Campaign, Missions, Custom Game and Load pages that start a game directly in the engine, and its
 * own Options and Help pages and the player profiles. The entries it has no page for yet open the engine's own menu.
 * Behind its pages it plays the original 3D glue scenes with the engine's viewer, or their recordings when the game
 * data has them.
 */
public class LauncherActivity extends AndroidApplication {
	private static final String TAG = "Warsmash";
	private static final String PREFERENCES = "launcher";
	private static final String KEY_EDITION = "edition";
	private static final String KEY_MUSIC = "music";
	private static final String KEY_SKIN = "skin";
	private static final String KEY_ANIMATED = "animatedBackground";
	private static final String KEY_CAMPAIGN = "campaign";
	private static final String KEY_MISSION = "mission";
	private static final String KEY_DIFFICULTY = "difficulty";
	private static final String KEY_MAP = "customMap";
	private static final String KEY_MAP_FOLDER = "customFolder";
	private static final String KEY_OPTIONS = "customOptions";
	private static final String KEY_SAVE = "save";
	private static final String KEY_RETURN_PAGE = "returnPage";
	/** What the Continue Campaign banner showed last, so that it shows at once: scene, mission, campaign line. */
	private static final String KEY_BANNER = "banner";
	/** The race the player chose for the custom game started last (lobby race index), for the statistics. */
	private static final String KEY_LAUNCH_RACE = "launchRace";
	/** Set while a game started from here runs; a launcher that finds it without a result knows the game died. */
	private static final String KEY_LAUNCHED = "launched";
	private static final long FADE_MILLIS = 250;
	private static final long PAGE_FADE_MILLIS = 150;
	private static final long DIALOG_MILLIS = 120;

	/** Home rail in its original order: the glue string and the text used when the game data lacks it. */
	private static final String[][] RAIL = { { "KEY_CAMPAIGN", "Campaign" },
		{ "KEY_LOAD_SAVED_GAME", "Load Saved Game" }, { "KEY_VIEW_REPLAY", "View Replay" },
		{ "KEY_CUSTOM_GAME", "Custom Games" }, { "KEY_LOCAL_AREA_NETWORK", "Local Area Network" },
		{ "KEY_OPTIONS", "Options" }, { "KEY_HELP", "Help" } };
	private static final int RAIL_CAMPAIGN = 0;
	private static final int RAIL_LOAD = 1;
	private static final int RAIL_REPLAY = 2;
	private static final int RAIL_CUSTOM = 3;
	private static final int RAIL_LAN = 4;
	private static final int RAIL_OPTIONS = 5;
	private static final int RAIL_HELP = 6;
	/** No rail entry is pressed (the profile page). */
	private static final int RAIL_NONE = RAIL.length;

	private static final int PAGE_PREP = -1;
	private static final int PAGE_HOME = 0;
	private static final int PAGE_OPTIONS = 1;
	private static final int PAGE_HELP = 2;
	private static final int PAGE_CAMPAIGN = 3;
	private static final int PAGE_MISSIONS = 4;
	private static final int PAGE_CUSTOM = 5;
	private static final int PAGE_LOAD = 6;
	private static final int PAGE_PROFILE = 7;

	/** The layers of the live scenes: the page's backdrop and the Continue Campaign banner. */
	private static final int LAYER_PAGE = 0;
	private static final int LAYER_BANNER = 1;
	/** The recorded scenes are 1920 x 810; their models are framed for that width. */
	private static final float SCENE_ASPECT = 1920f / 810f;

	/** The difficulties the engine's campaign screen offers (1.29 measured: Normal and Hard). */
	private static final String[][] DIFFICULTIES = { { "normal", "NORMAL", "Normal" }, { "hard", "HARD", "Hard" } };
	/** The campaign screen's choice of maps since 1.32; the first is the default, as there. */
	private static final String[][] CAMPAIGN_MAPS = {
		{ LaunchContract.CAMPAIGN_MAPS_REFORGED, "CAMPAIGN_VERSION_REFORGED", "REFORGED" },
		{ LaunchContract.CAMPAIGN_MAPS_CLASSIC, "CAMPAIGN_VERSION_CLASSIC", "CLASSIC" } };
	/** The slot choices of the skirmish lobby: the engine's lobby type, the glue string and its fallback. */
	private static final String[][] SLOT_TYPES = { { "OPEN", "OPEN", "Open" }, { "CLOSED", "CLOSED", "Closed" },
		{ "COMPUTER_NEWBIE", "COMPUTER_NEWBIE", "Computer (Easy)" },
		{ "COMPUTER_NORMAL", "COMPUTER_NORMAL", "Computer (Normal)" },
		{ "COMPUTER_INSANE", "COMPUTER_INSANE", "Computer (Insane)" } };
	private static final int SLOT_OPEN = 0;
	private static final int SLOT_COMPUTER_NORMAL = 3;
	/** Race item indexes as the lobby and the map information count them: 0 is random. */
	private static final String[][] RACES = { { "RANDOM", "Random" }, { "HUMAN", "Human" }, { "ORC", "Orc" },
		{ "UNDEAD", "Undead" }, { "NIGHT_ELF", "Night Elf" } };
	/** Advanced options in the order of the launch contract's names; Lock Teams is on in a new lobby. */
	private static final String[] OPTION_NAMES = { "lockTeams", "teamsTogether", "sharedControl", "randomRaces",
		"randomHero" };
	/** The options the page shows; teams together is not among them and stays on, as the original starts it. */
	private static final int[] OPTION_SHOWN = { 0, 2, 3, 4 };
	private static final int OPTION_TEAMS_TOGETHER = 1;
	/** The player colours by number: the glue string and the text used when the game data lacks it. */
	private static final String[][] COLOR_NAMES = { { "COLOR_RED", "Red" }, { "COLOR_BLUE", "Blue" },
		{ "COLOR_TEAL", "Teal" }, { "COLOR_PURPLE", "Purple" }, { "COLOR_YELLOW", "Yellow" },
		{ "COLOR_ORANGE", "Orange" }, { "COLOR_GREEN", "Green" }, { "COLOR_PINK", "Pink" }, { "COLOR_GRAY", "Gray" },
		{ "COLOR_LIGHT_BLUE", "Light Blue" }, { "COLOR_DARK_GREEN", "Dark Green" }, { "COLOR_BROWN", "Brown" } };
	private static final int[] PLAYER_COLORS = { 0xFFFF0303, 0xFF0042FF, 0xFF1CE6B9, 0xFF540081, 0xFFFFFC00,
		0xFFFE8A0E, 0xFF20C000, 0xFFE55BB0, 0xFF959697, 0xFF7EBFF1, 0xFF106246, 0xFF4E2A04 };

	private static final int TAB_GRAPHICS = 0;
	private static final int TAB_SOUND = 1;
	private static final int TAB_GAMEPLAY = 2;
	private static final int TAB_TOUCH = 3;
	private static final int TAB_GAME_DATA = 4;
	private static final int TAB_ABOUT = 5;

	/** Design pixels: the tablet board is 2960 x 1848, the phone board 1600 x 720. */
	private static final class Board {
		final boolean tablet;
		final float width, height;
		final float railW, logoX, logoY, logoW, logoH;
		final float holderX, holderY, holderW, holderH, holderPitch, quitY;
		final float chipX, chipY, chipH, name;
		final float bannerW, frame;
		final float panelW, panelH, panelFrame, title, body, barInset, barH, buttonH, buttonText;
		final float contentX, contentY, contentW, contentH, backX, backY, backW, backH;
		final float tabH, tabGap, tabText, rowH, pageFrame, pageInset, labelX, controlX, control, small;

		Board(final boolean tablet) {
			this.tablet = tablet;
			if (tablet) {
				this.width = 2960;
				this.height = 1848;
				this.railW = 640;
				this.logoX = 40;
				this.logoY = 24;
				this.logoW = 560;
				this.logoH = 250;
				this.holderX = 37;
				this.holderY = 296;
				this.holderW = 560;
				this.holderH = 146;
				this.holderPitch = 156;
				this.quitY = 1420;
				this.chipX = 720;
				this.chipY = 24;
				this.chipH = 168;
				this.name = 56;
				this.bannerW = 880;
				this.frame = 40;
				this.panelW = 1600;
				this.panelH = 620;
				this.panelFrame = 44;
				this.title = 56;
				this.body = 32;
				this.barInset = 72;
				this.barH = 72;
				this.buttonH = 112;
				this.buttonText = 40;
				this.contentX = 720;
				this.contentY = 208;
				this.contentW = 2200;
				this.contentH = 1432;
				this.backX = 720;
				this.backY = 1680;
				this.backW = 480;
				this.backH = 128;
				this.tabH = 104;
				this.tabGap = 16;
				this.tabText = 32;
				this.rowH = 170;
				this.pageFrame = 40;
				// The design sets panel contents 8 px inside the 40 px frame.
				this.pageInset = 48;
				this.labelX = 40;
				this.controlX = 1100;
				this.control = 64;
				this.small = 28;
			}
			else {
				this.width = 1600;
				this.height = 720;
				this.railW = 384;
				this.logoX = 1336;
				this.logoY = 8;
				this.logoW = 248;
				this.logoH = 108;
				this.holderX = 16;
				this.holderY = 24;
				this.holderW = 346;
				this.holderH = 96;
				this.holderPitch = 96;
				this.quitY = -1;
				this.chipX = 416;
				this.chipY = 6;
				this.chipH = 114;
				this.name = 40;
				this.bannerW = 440;
				this.frame = 24;
				this.panelW = 1200;
				this.panelH = 440;
				this.panelFrame = 30;
				this.title = 40;
				this.body = 28;
				this.barInset = 44;
				this.barH = 52;
				this.buttonH = 96;
				this.buttonText = 30;
				this.contentX = 416;
				this.contentY = 130;
				this.contentW = 1168;
				this.contentH = 474;
				this.backX = 416;
				this.backY = 616;
				this.backW = 280;
				this.backH = 96;
				// Every touch target on the phone is at least 48 dp: tabs and option rows are 96 px high.
				this.tabH = 96;
				this.tabGap = 8;
				this.tabText = 22;
				this.rowH = 96;
				this.pageFrame = 20;
				this.pageInset = 16;
				this.labelX = 16;
				this.controlX = 560;
				this.control = 44;
				this.small = 24;
			}
		}
	}

	private Board board;
	private float scale;
	private FrameLayout root;
	private FrameLayout page;
	private DataTable ini;
	private SkinCache cache;
	private SkinCache.Art art;
	private SharedPreferences preferences;
	private final boolean[] cancelled = new boolean[1];
	private boolean prepDrawnWithArt;
	private TextView prepLabel;
	private TextView prepDetail;
	private LauncherKit.BarDrawable prepBar;
	private View prepBarView;
	private View prepButtons;
	private int prepDone;
	private int prepTotal;
	private View dialog;
	/** The box of the open dialog and its name field, which the on-screen keyboard must not cover. */
	private View openBox;
	private View dialogField;
	/** Where the keyboard leaves no room for the whole dialog, these buttons take the place of the folded view. */
	private View dialogFold;
	private View[] dialogButtons;
	private int shown = PAGE_PREP;
	private int afterPrep = PAGE_HOME;
	private int optionsTab = TAB_GRAPHICS;
	private int helpTab;
	private List<BackdropView> backdrops = new ArrayList<>();
	private List<BackdropView> building = new ArrayList<>();
	private boolean resumed;
	/** Grows with every page shown; an answer of the catalog for an earlier page is dropped. */
	private int generation;
	private GameCatalog catalog;
	private GameCatalog.Campaigns campaigns;
	private List<GameCatalog.MapEntry> maps;
	private GameCatalog.MapDetails mapDetails;
	/** The custom game's choices for the shown map, per slot of its details: lobby slot type and race. */
	private int[] slotTypes;
	private int[] slotRaces;
	/** The team and colour chosen in a slot's menus; -1 keeps what the map gives the slot. */
	private int[] slotTeams;
	private int[] slotColors;
	private boolean[] advanced;
	private LaunchContract.Result returned;
	private boolean returnedUnexpectedly;
	private Profiles profiles;
	private GlueBackdropRenderer renderer;
	private View sceneView;
	private long sceneRequests;
	private final GlueBackdropRenderer.Request[] buildingScenes = new GlueBackdropRenderer.Request[GlueBackdropRenderer.LAYERS];
	private final BackdropView[] buildingSceneViews = new BackdropView[GlueBackdropRenderer.LAYERS];
	private final long[] shownScenes = new long[GlueBackdropRenderer.LAYERS];
	private final BackdropView[] sceneViews = new BackdropView[GlueBackdropRenderer.LAYERS];
	private MediaPlayer music;
	private SoundPool clicks;
	private int click;
	private int bigClick;
	private File musicTrack;
	/** A dialog that only its own buttons close (the first profile). */
	private boolean dialogLocked;
	/** A game is being started; the launcher takes no more taps. */
	private boolean launching;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (AndroidLauncher.showRunningGame(this)) {
			finish();
			return;
		}
		this.preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
		this.profiles = new Profiles(this, this.preferences);
		final File iniFile = SkinCache.iniFile(this);
		try {
			this.ini = iniFile.isFile() ? AndroidLauncher.loadWarsmashIni(iniFile) : null;
		}
		catch (final IOException e) {
			Log.e(TAG, "Unable to read " + iniFile, e);
			this.ini = null;
		}
		this.cache = this.ini == null ? null : SkinCache.forConfiguration(this, this.ini, chosenSkin());
		if ((this.cache == null) || !DataFolderCheck.hasGameData(this.ini.get("DataSources"))) {
			openGameData();
			return;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
		}
		hideSystemBars();
		final DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
		final float screenW = Math.max(metrics.widthPixels, metrics.heightPixels);
		final float screenH = Math.min(metrics.widthPixels, metrics.heightPixels);
		// Phones are much wider than they are high; tablets come close to 16:10.
		this.board = new Board((screenW / screenH) < 1.9f);
		this.scale = Math.min(screenW / this.board.width, screenH / this.board.height);
		final FrameLayout outer = new FrameLayout(this);
		outer.setBackgroundColor(Color.BLACK);
		this.root = new FrameLayout(this);
		outer.addView(this.root, new FrameLayout.LayoutParams(px(this.board.width), px(this.board.height),
				Gravity.CENTER));
		addSceneView();
		setContentView(outer);
		trackOnscreenKeyboard();
		this.catalog = new GameCatalog(this, this.ini, GameCatalog.dataKey(this.ini));
		takeReturn();
		if (this.cache.isCurrent()) {
			showAfterPrep();
		}
		else {
			showPrep();
		}
	}

	/**
	 * The surface the live 3D scenes are drawn on, behind every page; the pages leave it visible where a scene is
	 * shown. The libGDX application of the launcher only draws these scenes; the game itself is its own activity.
	 */
	private void addSceneView() {
		AndroidLauncher.loadExtensions(new File(getCacheDir(), "sounds"));
		this.renderer = new GlueBackdropRenderer(this.ini, new GlueBackdropRenderer.Listener() {
			@Override
			public void shown(final int layer, final long request) {
				runOnUiThread(() -> {
					if ((LauncherActivity.this.shownScenes[layer] == request)
							&& (LauncherActivity.this.sceneViews[layer] != null)) {
						LauncherActivity.this.sceneViews[layer].reveal();
					}
				});
			}

			@Override
			public void failed(final int layer, final long request) {
				Log.w(TAG, "The 3D scene of layer " + layer + " cannot be shown");
			}
		});
		final AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.useGL30 = true;
		config.disableAudio = true;
		config.useImmersiveMode = true;
		config.useAccelerometer = false;
		config.useCompass = false;
		config.useGyroscope = false;
		config.useRotationVectorSensor = false;
		config.a = 0;
		config.depth = 16;
		this.sceneView = initializeForView(this.renderer, config);
		this.root.addView(this.sceneView, 0, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
	}

	/**
	 * After a game started from here: the page it was started from comes back, and a game that crashed closed
	 * unexpectedly. A game without a result was stopped from outside unless Android says it crashed.
	 */
	private void takeReturn() {
		final long launched = this.preferences.getLong(KEY_LAUNCHED, 0);
		if (launched == 0) {
			LaunchContract.takeResult(this);
			return;
		}
		this.returned = LaunchContract.takeResult(this);
		this.returnedUnexpectedly = this.returned == null ? LaunchContract.crashedSince(this, launched)
				: this.returned.crashed();
		if ((this.returned != null) && this.returned.crashed()) {
			this.returned = null;
		}
		this.afterPrep = this.preferences.getInt(KEY_RETURN_PAGE, PAGE_HOME);
		if ((this.returned != null) && "exit".equals(this.returned.result)) {
			this.afterPrep = PAGE_HOME;
		}
		final LaunchContract.Result result = this.returned;
		if ((result != null) && LaunchContract.SOURCE_CUSTOM.equals(result.source) && !result.failed()
				&& !"exit".equals(result.result) && !result.profile.isEmpty()) {
			// A random race is counted as the race it drew.
			final int played = (result.race > 0) && (result.race < RACES.length) ? result.race
					: this.preferences.getInt(KEY_LAUNCH_RACE, 0);
			this.profiles.record(result.profile, result.result, played);
		}
		final SharedPreferences.Editor editor = this.preferences.edit().remove(KEY_LAUNCHED);
		if ((this.returned != null) && "victory".equals(this.returned.result)
				&& LaunchContract.SOURCE_CAMPAIGN.equals(this.returned.source)) {
			// The mission the victory opened is the one shown chosen.
			editor.remove(KEY_MISSION);
		}
		editor.apply();
	}

	/** The dialog a return brings, once the page it returns to is shown. */
	private void showReturnDialog() {
		final LaunchContract.Result result = this.returned;
		final boolean unexpectedly = this.returnedUnexpectedly;
		this.returned = null;
		this.returnedUnexpectedly = false;
		if (unexpectedly) {
			showMessage(getString(R.string.game_closed_title), getString(R.string.game_closed_line));
		}
		else if ((result != null) && (result.loadFailure != null)) {
			showMessage(getString(R.string.game_cannot_load), loadFailureText(result.loadFailure));
		}
		else if ((result != null) && result.failed()) {
			showMessage(getString(R.string.game_not_started), result.map);
		}
	}

	private String loadFailureText(final String failure) {
		try {
			return loadabilityText(SavedGame.Loadability.valueOf(failure));
		}
		catch (final IllegalArgumentException e) {
			return failure;
		}
	}

	/**
	 * The skin the player chose; Reforged only while the game data has its interface. A classic cache that Android
	 * cleared cannot tell, so the choice is kept and the classic skin used until the preparation finds out again.
	 */
	private String chosenSkin() {
		final String skin = this.preferences.getString(KEY_SKIN, SkinCache.CLASSIC_SKIN);
		return SkinCache.REFORGED_SKIN.equals(skin) && SkinCache.hasReforged(this) ? SkinCache.REFORGED_SKIN
				: SkinCache.CLASSIC_SKIN;
	}

	@Override
	public void onWindowFocusChanged(final boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			hideSystemBars();
		}
	}

	@SuppressWarnings("deprecation")
	private void hideSystemBars() {
		getWindow().getDecorView()
				.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
						| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
	}

	@Override
	protected void onResume() {
		super.onResume();
		this.resumed = true;
		for (final BackdropView backdrop : this.backdrops) {
			backdrop.play();
		}
		updateMusic();
	}

	@Override
	protected void onPause() {
		this.resumed = false;
		for (final BackdropView backdrop : this.backdrops) {
			backdrop.stop();
		}
		stopMusic();
		super.onPause();
	}

	// ---------------------------------------------------------------- menu music

	/** The original menu music plays while the launcher shows a page and the Menu music box is on (Prep is silent). */
	private void updateMusic() {
		final File track = menuMusic();
		final boolean wanted = this.resumed && (this.shown != PAGE_PREP) && (track != null)
				&& this.preferences.getBoolean(KEY_MUSIC, true);
		if (!wanted) {
			stopMusic();
			return;
		}
		if (this.music != null) {
			if (track.equals(this.musicTrack)) {
				return;
			}
			stopMusic();
		}
		final MediaPlayer player = new MediaPlayer();
		this.music = player;
		this.musicTrack = track;
		try {
			player.setDataSource(track.getAbsolutePath());
			player.setLooping(true);
			// The engine's music volume (40 of 100 unless the player changed it).
			final float volume = Math.max(0, Math.min(100, new EngineSettings(this).number("musicVolume", 40))) / 100f;
			player.setVolume(volume, volume);
			player.setOnPreparedListener(prepared -> {
				if (this.music == prepared) {
					prepared.start();
				}
			});
			player.setOnErrorListener((failed, what, extra) -> {
				Log.w(TAG, "The menu music cannot play: " + what + "/" + extra);
				stopMusic();
				return true;
			});
			player.prepareAsync();
		}
		catch (final IOException | RuntimeException e) {
			Log.w(TAG, "The menu music cannot play", e);
			stopMusic();
		}
	}

	/** Reign of Chaos has its own main screen music; without it the expansion's plays. */
	private File menuMusic() {
		if (this.cache == null) {
			return null;
		}
		final File roc = this.cache.file(SkinCache.MUSIC_ROC);
		if ("roc".equals(this.preferences.getString(KEY_EDITION, "tft")) && roc.isFile()) {
			return roc;
		}
		final File tft = this.cache.file(SkinCache.MUSIC);
		return tft.isFile() ? tft : null;
	}

	private void stopMusic() {
		if (this.music != null) {
			try {
				this.music.release();
			}
			catch (final RuntimeException e) {
				Log.w(TAG, "Unable to release the menu music", e);
			}
			this.music = null;
		}
	}

	@Override
	protected void onDestroy() {
		stopMusic();
		if (this.clicks != null) {
			this.clicks.release();
			this.clicks = null;
			LauncherKit.clicks = null;
		}
		this.cancelled[0] = true;
		if (this.catalog != null) {
			this.catalog.close();
		}
		super.onDestroy();
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onBackPressed() {
		if (this.launching) {
			return;
		}
		if (this.dialog != null) {
			if (!this.dialogLocked) {
				closeDialog();
			}
		}
		else if (this.shown == PAGE_MISSIONS) {
			showCampaign();
		}
		else if ((this.shown == PAGE_OPTIONS) || (this.shown == PAGE_HELP) || (this.shown == PAGE_CAMPAIGN)
				|| (this.shown == PAGE_CUSTOM) || (this.shown == PAGE_LOAD) || (this.shown == PAGE_PROFILE)) {
			showHome();
		}
		else if (this.shown == PAGE_HOME) {
			showExit();
		}
		else {
			super.onBackPressed();
		}
	}

	@Override
	public boolean dispatchTouchEvent(final MotionEvent event) {
		if (this.launching) {
			return true;
		}
		return super.dispatchTouchEvent(event);
	}

	/** The glue click sounds of the skin's art, loaded once the art is prepared. */
	private void loadClicks() {
		if ((this.clicks != null) || (this.cache == null) || !new SkinCache.Art(this.cache).has(SkinCache.CLICK)) {
			return;
		}
		this.clicks = new SoundPool.Builder().setMaxStreams(2)
				.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
						.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
				.build();
		this.click = this.clicks.load(this.cache.file(SkinCache.CLICK).getAbsolutePath(), 1);
		this.bigClick = this.cache.file(SkinCache.CLICK_BIG).isFile()
				? this.clicks.load(this.cache.file(SkinCache.CLICK_BIG).getAbsolutePath(), 1)
				: this.click;
		LauncherKit.clicks = main -> {
			if (this.clicks != null) {
				this.clicks.play(main ? this.bigClick : this.click, 1, 1, 1, 0, 1);
			}
		};
	}

	/** A tappable view that clicks like the glue buttons: big for the main menu choices, the mouse click else. */
	private static void onTap(final View view, final boolean main, final View.OnClickListener listener) {
		view.setOnClickListener(v -> {
			if (LauncherKit.clicks != null) {
				LauncherKit.clicks.play(main);
			}
			listener.onClick(v);
		});
	}

	private int px(final float design) {
		return Math.round(design * this.scale);
	}

	private void place(final FrameLayout parent, final View view, final float x, final float y, final float w,
			final float h) {
		final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				w < 0 ? ViewGroup.LayoutParams.WRAP_CONTENT : px(w), h < 0 ? ViewGroup.LayoutParams.WRAP_CONTENT : px(h));
		params.leftMargin = px(x);
		params.topMargin = px(y);
		parent.addView(view, params);
	}

	private TextView text(final CharSequence value, final float size, final int color) {
		final TextView view = new TextView(this);
		view.setText(value);
		view.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * this.scale);
		view.setTextColor(color);
		view.setTypeface(this.art != null ? this.art.font() : null);
		view.setIncludeFontPadding(false);
		view.setSingleLine(true);
		view.setEllipsize(TextUtils.TruncateAt.END);
		view.setShadowLayer(1, size * this.scale * 0.07f, size * this.scale * 0.07f, Color.BLACK);
		return view;
	}

	/** A few lines of text that wrap within their width. */
	private TextView lines(final CharSequence value, final float size, final int color, final int maxLines) {
		final TextView view = text(value, size, color);
		view.setSingleLine(false);
		view.setMaxLines(maxLines);
		view.setLineSpacing(0, 1.15f);
		return view;
	}

	private LauncherKit.GlueButton button(final CharSequence label, final float height, final float textSize,
			final View.OnClickListener listener) {
		final LauncherKit.GlueButton button = new LauncherKit.GlueButton(this, this.art, px(height),
				textSize * this.scale);
		button.setText(label);
		button.setOnClickListener(listener);
		return button;
	}

	private String glue(final String key, final String fallback) {
		return this.art.string(key, fallback);
	}

	/** A vertical or horizontal gradient with stops, which GradientDrawable cannot place. */
	private static Drawable gradient(final boolean vertical, final int[] colors, final float[] stops) {
		final PaintDrawable drawable = new PaintDrawable();
		drawable.setShape(new RectShape());
		drawable.setShaderFactory(new ShapeDrawable.ShaderFactory() {
			@Override
			public Shader resize(final int width, final int height) {
				return new LinearGradient(0, 0, vertical ? 0 : width, vertical ? height : 0, colors, stops,
						Shader.TileMode.CLAMP);
			}
		});
		return drawable;
	}

	private void setPage(final FrameLayout view, final int kind) {
		for (final BackdropView backdrop : this.backdrops) {
			backdrop.stop();
		}
		this.backdrops = this.building;
		this.building = new ArrayList<>();
		final View previous = this.page;
		this.page = view;
		this.shown = kind;
		this.generation++;
		final int at = previous != null ? this.root.indexOfChild(previous) + 1 : this.sceneView != null ? 1 : 0;
		this.root.addView(view, at, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		if (previous != null) {
			if (ValueAnimator.areAnimatorsEnabled()) {
				view.setAlpha(0);
				view.animate().alpha(1).setDuration(PAGE_FADE_MILLIS).withEndAction(() -> this.root.removeView(previous));
			}
			else {
				this.root.removeView(previous);
			}
		}
		loadClicks();
		if (this.resumed) {
			for (final BackdropView backdrop : this.backdrops) {
				backdrop.play();
			}
		}
		for (int i = 0; i < GlueBackdropRenderer.LAYERS; i++) {
			showScene(i, this.buildingScenes[i], this.buildingSceneViews[i]);
			this.buildingScenes[i] = null;
			this.buildingSceneViews[i] = null;
		}
		updateMusic();
	}

	/** Asks the renderer for a layer's scene; the view's still gives way when the scene is drawn. */
	private void showScene(final int layer, final GlueBackdropRenderer.Request request, final BackdropView view) {
		this.shownScenes[layer] = request != null ? request.id() : 0;
		this.sceneViews[layer] = view;
		if (this.renderer != null) {
			this.renderer.show(layer, request);
		}
	}

	// ---------------------------------------------------------------- Prep

	private void showPrep() {
		buildPrep();
		startPrep();
	}

	private void buildPrep() {
		this.art = new SkinCache.Art(this.cache);
		this.prepDrawnWithArt = this.art.has(SkinCache.FONT);
		final Board b = this.board;
		final FrameLayout prep = new FrameLayout(this);
		final Bitmap background = this.art.bitmap(SkinCache.BACKGROUND);
		if (background != null) {
			prep.setBackground(new LauncherKit.CoverDrawable(background, 0.5f, Color.argb(0x59, 0, 0, 0)));
		}
		else {
			final GradientDrawable plain = new GradientDrawable();
			plain.setGradientType(GradientDrawable.RADIAL_GRADIENT);
			plain.setColors(new int[] { Color.rgb(0x12, 0x1A, 0x3A), Color.rgb(0x07, 0x0A, 0x18),
				Color.rgb(0x03, 0x04, 0x09) });
			plain.setGradientRadius(px(b.width) * 0.75f);
			plain.setGradientCenter(0.5f, 0.3f);
			prep.setBackground(plain);
		}
		final float x = (b.width - b.panelW) / 2;
		final float y = (b.height - b.panelH) / 2;
		final FrameLayout panel = new FrameLayout(this);
		panel.setBackground(panelFrame(b.panelFrame, Color.argb(0xCC, 0, 0, 0)));
		place(prep, panel, x, y, b.panelW, b.panelH);

		final TextView title = text(getString(R.string.launcher_preparing), b.title, this.art.gold());
		title.setGravity(Gravity.CENTER_HORIZONTAL);
		place(panel, title, 0, b.tablet ? 60 : 36, b.panelW, -1);
		final float inner = b.panelW - (2 * b.barInset);
		final float labelY = b.tablet ? 170 : 110;
		this.prepLabel = text("", b.body, this.art.text());
		place(panel, this.prepLabel, b.barInset, labelY, inner, -1);
		this.prepBar = new LauncherKit.BarDrawable(this.art.bitmap(SkinCache.BAR_BACKGROUND),
				this.art.bitmap(SkinCache.BAR_FILL), this.art.bitmap(SkinCache.BAR_GLASS), px(4));
		this.prepBarView = new View(this);
		this.prepBarView.setBackground(this.prepBar);
		place(panel, this.prepBarView, b.barInset, labelY + (b.tablet ? 56 : 44), inner, b.barH);
		this.prepDetail = text("", b.body * 0.85f, this.art.text2());
		place(panel, this.prepDetail, b.barInset, labelY + (b.tablet ? 170 : 128), inner, -1);

		final FrameLayout buttons = new FrameLayout(this);
		final float buttonW = (inner - (b.tablet ? 32 : 24)) / 2;
		place(buttons, button(getString(R.string.launcher_other_source), b.buttonH, b.buttonText,
				view -> openGameData()), 0, 0, buttonW, b.buttonH);
		place(buttons, button(getString(R.string.data_folder_retry), b.buttonH, b.buttonText, view -> startPrep()),
				inner - buttonW, 0, buttonW, b.buttonH);
		buttons.setVisibility(View.GONE);
		place(panel, buttons, b.barInset, b.panelH - b.panelFrame - b.buttonH - (b.tablet ? 8 : 4), inner,
				b.buttonH);
		this.prepButtons = buttons;

		setPage(prep, PAGE_PREP);
		showStep(this.prepDone, this.prepTotal, null);
	}

	private Drawable panelFrame(final float border, final int fill) {
		return new LauncherKit.FrameDrawable(this.art.bitmap(SkinCache.PANEL_BORDER), px(border)).filled(fill)
				.fillInset(px(border * 0.35f)).fallback(Color.argb(0xEB, 0x03, 0x04, 0x0A)).line(this.art.line());
	}

	private void startPrep() {
		this.prepButtons.setVisibility(View.GONE);
		this.prepDetail.setTextColor(this.art.text2());
		this.prepDetail.setText(R.string.launcher_reading);
		final DataTable ini = this.ini;
		final SkinCache cache = this.cache;
		final boolean[] cancelled = this.cancelled;
		new Thread(() -> {
			try {
				cache.prepare(ini, (done, total, name) -> runOnUiThread(() -> {
					if (!isFinishing() && (cache == this.cache)) {
						showStep(done, total, name);
					}
				}), cancelled);
				runOnUiThread(() -> {
					if (!cancelled[0] && !isFinishing() && (cache == this.cache)) {
						prepared();
					}
				});
			}
			catch (final IOException | RuntimeException e) {
				Log.e(TAG, "Unable to prepare the launcher art", e);
				runOnUiThread(() -> {
					if (!isFinishing() && (cache == this.cache)) {
						prepFailed(e);
					}
				});
			}
		}, "LauncherPrep").start();
	}

	private void showStep(final int done, final int total, final String name) {
		this.prepDone = done;
		this.prepTotal = total;
		this.prepLabel.setText(getString(R.string.launcher_art, done, Math.max(total, this.cache.total())));
		this.prepBar.setProgress(total == 0 ? 0 : done / (float) total);
		if (name != null) {
			this.prepDetail.setText(name);
		}
		// The backdrop, panel, bar and font are the first files made; from then on the screen draws with them.
		if (!this.prepDrawnWithArt && this.cache.file(SkinCache.FONT).isFile()) {
			buildPrep();
		}
	}

	private void prepFailed(final Throwable error) {
		this.prepDetail.setTextColor(Color.rgb(0xE0, 0x56, 0x4E));
		final String message = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
		this.prepDetail.setText(getString(R.string.launcher_failed, message));
		this.prepButtons.setVisibility(View.VISIBLE);
	}

	/** The one transition of the launcher: Prep darkens to black and the next page lights up from it. */
	private void prepared() {
		// Game data without the Reforged interface cannot give that skin; the classic one takes over.
		if (this.cache.reforged() && !this.cache.file(SkinCache.REFORGED).isFile()) {
			this.preferences.edit().putString(KEY_SKIN, SkinCache.CLASSIC_SKIN).apply();
			this.cache = SkinCache.forConfiguration(this, this.ini, SkinCache.CLASSIC_SKIN);
			if (!this.cache.isCurrent()) {
				this.prepDone = 0;
				this.prepTotal = 0;
				showPrep();
				return;
			}
		}
		if (!ValueAnimator.areAnimatorsEnabled()) {
			showAfterPrep();
			return;
		}
		final View curtain = new View(this);
		curtain.setBackgroundColor(Color.BLACK);
		curtain.setAlpha(0);
		curtain.setClickable(true);
		this.root.addView(curtain, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		curtain.animate().alpha(1).setDuration(FADE_MILLIS).withEndAction(() -> {
			showAfterPrep();
			curtain.bringToFront();
			curtain.animate().alpha(0).setDuration(FADE_MILLIS)
					.withEndAction(() -> this.root.removeView(curtain));
		});
	}

	private void showAfterPrep() {
		final int next = this.afterPrep;
		this.afterPrep = PAGE_HOME;
		this.prepDone = 0;
		this.prepTotal = 0;
		showPage(next);
		if (this.returnedUnexpectedly || (this.returned != null)) {
			showReturnDialog();
		}
	}

	private void showPage(final int kind) {
		switch (kind) {
		case PAGE_OPTIONS:
			showOptions(this.optionsTab);
			break;
		case PAGE_CAMPAIGN:
			showCampaign();
			break;
		case PAGE_MISSIONS:
			showMissions();
			break;
		case PAGE_CUSTOM:
			showCustom();
			break;
		case PAGE_LOAD:
			showLoad();
			break;
		case PAGE_PROFILE:
			showProfile(null);
			break;
		default:
			showHome();
			break;
		}
	}

	// ---------------------------------------------------------------- shared frame of the pages

	private boolean animated() {
		return this.preferences.getBoolean(KEY_ANIMATED, true) && ValueAnimator.areAnimatorsEnabled();
	}

	/** An original scene behind a page: its recording when the data has one and animations are on, else the still. */
	private BackdropView backdrop(final Drawable still, final String scene, final float focus) {
		final BackdropView view = new BackdropView(this, still, animated() ? this.art.recording(scene) : null, focus);
		this.building.add(view);
		return view;
	}

	/**
	 * A page's original scene: its recording when the game data has one, else the live 3D scene drawn behind the
	 * page once it is loaded, with the still until then. With a model x (a part of the recorded width, else negative)
	 * the scene is cropped around its model the way the recording is.
	 */
	private BackdropView sceneBackdrop(final Drawable still, final String recorded, final float focus, final int layer,
			final String skinField, final String fallbackField, final FogSettings fog, final int fogVersion,
			final float x, final float y, final float w, final float h, final float modelX) {
		final BackdropView view = backdrop(still, recorded, focus);
		if (!view.hasRecording() && animated() && (this.renderer != null)) {
			final Rectangle clip = glRect(x, y, w, h);
			Rectangle viewport = clip;
			final float sceneW = h * SCENE_ASPECT;
			if ((modelX >= 0) && (sceneW > w)) {
				final float crop = modelFocus(modelX, w / h, SCENE_ASPECT);
				viewport = glRect(x - (crop * (sceneW - w)), y, sceneW, h);
			}
			this.buildingScenes[layer] = new GlueBackdropRenderer.Request(++this.sceneRequests, skinField,
					fallbackField, fog, fogVersion, viewport, viewport == clip ? null : clip);
			this.buildingSceneViews[layer] = view;
		}
		return view;
	}

	/** A place of the board in the scene surface's pixels, counted from its bottom as GL counts. */
	private Rectangle glRect(final float x, final float y, final float w, final float h) {
		return new Rectangle(px(x), px(this.board.height) - px(y + h), px(w), px(h));
	}

	/** The main menu scene of the chosen edition, as the engine's menu shows it. */
	private BackdropView editionScene(final boolean roc) {
		final Board b = this.board;
		final String scene = roc ? "mainmenu" : "mainmenu_tft";
		final float focus = roc ? 0.05f : 1f;
		return sceneBackdrop(homeBackground(scene, focus), scene, focus, LAYER_PAGE,
				roc ? "GlueSpriteLayerBackground_V0" : "GlueSpriteLayerBackground_V1", "GlueSpriteLayerBackground",
				null, roc ? 0 : 1, 0, 0, b.width, b.height, -1);
	}

	/**
	 * Everything the launcher pages share: the backdrop, the rail with the shown page's entry pressed, the logo, the
	 * profile and on the tablet Quit and the footer. Home passes no selected entry and gets its scene and banner.
	 */
	private FrameLayout shell(final int selected) {
		return shell(selected, true);
	}

	/**
	 * @param editionScene false for a page that puts its own scene behind it (the campaign pages), which the shell
	 *                     must not cover, since a live scene is drawn behind the window; the others show the animated
	 *                     main menu scene of the chosen edition, Home as it is and the rest dimmed
	 */
	private FrameLayout shell(final int selected, final boolean editionScene) {
		this.art = new SkinCache.Art(this.cache);
		final Board b = this.board;
		final boolean home = selected < 0;
		final boolean roc = "roc".equals(this.preferences.getString(KEY_EDITION, "tft"));
		final FrameLayout view = new FrameLayout(this);
		if (editionScene) {
			view.addView(editionScene(roc), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT));
			if (!home) {
				final View scrim = new View(this);
				scrim.setBackgroundColor(Color.argb(0x73, 0, 0, 0));
				view.addView(scrim, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.MATCH_PARENT));
			}
		}

		final View rail = new View(this);
		final Bitmap stone = this.art.bitmap(SkinCache.RAIL_BACKGROUND);
		if (stone != null) {
			rail.setBackground(new LauncherKit.CoverDrawable(stone, 0.5f, Color.argb(0x33, 0, 0, 0)));
		}
		else {
			rail.setBackground(gradient(false, new int[] { Color.argb(0xDB, 0, 0, 0), Color.argb(0xB3, 0, 0, 0) },
					null));
		}
		place(view, rail, 0, 0, b.railW - 6, b.height);
		final View railEdge = new View(this);
		railEdge.setBackgroundColor(this.art.reforged() ? Color.rgb(0x6A, 0x65, 0x5C) : Color.rgb(0x2C, 0x24, 0x16));
		place(view, railEdge, b.railW - 6, 0, 6, b.height);

		if (home) {
			addBanner(view);
		}

		final ImageView logo = new ImageView(this);
		logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
		logo.setImageBitmap(this.art.bitmap(roc ? SkinCache.LOGO_ROC : SkinCache.LOGO_TFT));
		logo.setContentDescription(getString(R.string.app_name));
		// On Home the logo switches the edition; elsewhere it leads back to Home.
		logo.setContentDescription(getString(home ? R.string.launcher_logo_edition : R.string.launcher_logo_home));
		onTap(logo, false, v -> {
			if (home) {
				this.preferences.edit().putString(KEY_EDITION, roc ? "tft" : "roc").apply();
			}
			showHome();
		});
		if (b.tablet) {
			// Only Home darkens the top of the rail under the logo; the other pages show the rail's stone.
			final View shade = new View(this);
			shade.setVisibility(home ? View.VISIBLE : View.GONE);
			shade.setBackground(gradient(true, new int[] { Color.BLACK, Color.BLACK, Color.TRANSPARENT },
					new float[] { 0, 0.82f, 1 }));
			place(view, shade, 0, 0, b.railW - 6, b.logoY + b.logoH + 30);
			place(view, logo, b.logoX, b.logoY, b.logoW, b.logoH);
		}

		for (int i = 0; i < RAIL.length; i++) {
			final int entry = i;
			// Replays (v0.3) and the local network (v0.6) do not exist yet; their entries stay in place, disabled.
			final boolean open = (entry != RAIL_REPLAY) && (entry != RAIL_LAN);
			addHolder(view, glue(RAIL[i][0], RAIL[i][1]), b.holderY + (i * b.holderPitch), i == selected,
					open ? v -> openEntry(entry) : null);
		}
		if (b.quitY >= 0) {
			addHolder(view, glue("KEY_QUIT", "Quit"), b.quitY, false, v -> showExit());
			addFooter(view);
		}
		addProfile(view, home);
		if (!b.tablet) {
			// On the phone the logo sits in the top bar, over everything (on Home also over the banner).
			place(view, logo, b.logoX, b.logoY, b.logoW, b.logoH);
		}
		return view;
	}

	private void openEntry(final int entry) {
		if (entry == RAIL_OPTIONS) {
			showOptions(this.shown == PAGE_OPTIONS ? this.optionsTab : TAB_GRAPHICS);
		}
		else if (entry == RAIL_HELP) {
			showHelp(this.shown == PAGE_HELP ? this.helpTab : 0);
		}
		else if (entry == RAIL_CAMPAIGN) {
			showCampaign();
		}
		else if (entry == RAIL_LOAD) {
			showLoad();
		}
		else if (entry == RAIL_CUSTOM) {
			showCustom();
		}
	}

	/** The still behind Home: the scene's first frame, or the generic loading screen when the data has no recording. */
	private Drawable homeBackground(final String scene, final float focus) {
		final Bitmap still = this.art.poster(scene);
		if (still != null) {
			return new LauncherKit.CoverDrawable(still, focus, 0);
		}
		return sceneStill();
	}

	/** Without a recorded first frame: dark until the live scene shows, the generic screen when none will. */
	private Drawable sceneStill() {
		return animated() && (this.renderer != null) ? new ColorDrawable(Color.BLACK) : pageBackground();
	}

	/** Behind the other pages: the generic loading screen (Reforged: its home screen) under a scrim. */
	private Drawable pageBackground() {
		final Bitmap generic = this.art.bitmap(SkinCache.BACKGROUND);
		if (generic != null) {
			return new LauncherKit.CoverDrawable(generic, 0.5f, Color.argb(0x73, 0, 0, 0));
		}
		return new ColorDrawable(Color.rgb(0x07, 0x0A, 0x18));
	}

	/**
	 * A menu entry as the original main menu draws it: the holder frame with the blue button in its middle. The
	 * Reforged menu has no holders; its blue button fills most of the place.
	 */
	private void addHolder(final FrameLayout parent, final String label, final float y, final boolean selected,
			final View.OnClickListener listener) {
		final Board b = this.board;
		final FrameLayout holder = new FrameLayout(this);
		final Bitmap frame = this.art.bitmap(SkinCache.HOLDER);
		final boolean held = (frame != null) && !this.art.reforged();
		if (held) {
			final ImageView image = new ImageView(this);
			image.setScaleType(ImageView.ScaleType.FIT_XY);
			image.setImageBitmap(frame);
			holder.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT));
		}
		final float buttonH = b.holderH * (held ? 0.594f : 0.72f);
		final LauncherKit.GlueButton button = new LauncherKit.GlueButton(this, this.art, px(buttonH),
				b.holderH * 0.594f * 0.52f * this.scale);
		button.setText(LauncherKit.glueText(label));
		button.setDuplicateParentStateEnabled(true);
		if (held) {
			place(holder, button, b.holderW * 0.0455f, b.holderH * 0.188f, b.holderW * 0.909f, buttonH);
		}
		else {
			place(holder, button, b.holderW * 0.04f, b.holderH * 0.14f, b.holderW * 0.92f, buttonH);
		}
		holder.setSelected(selected);
		if (listener != null) {
			holder.setClickable(true);
			onTap(holder, true, listener);
		}
		else {
			holder.setEnabled(false);
			button.setEnabled(false);
		}
		holder.setContentDescription(LauncherKit.glueText(label).toString());
		place(parent, holder, b.holderX, y, b.holderW, b.holderH);
	}

	// ---------------------------------------------------------------- Home

	private void showHome() {
		final FrameLayout view = shell(-1);
		setPage(view, PAGE_HOME);
		if (!this.profiles.any()) {
			showFirstProfile();
		}
		if (this.campaigns == null) {
			final int generation = this.generation;
			this.catalog.campaigns(found -> {
				if (found == null) {
					return;
				}
				this.campaigns = found;
				if ((generation == this.generation) && (this.shown == PAGE_HOME)) {
					refillBanner();
				}
			});
		}
	}

	/** What the Continue Campaign banner shows: the next mission of the current profile's campaign progress. */
	private static final class BannerMission {
		String scene = "roc_prologue";
		String background = "TutorialBackdrop";
		FogSettings fog;
		String title = "";
		String line = "";
		CampaignCatalog.Entry entry;
		int mission;
	}

	/**
	 * The campaign the progress last chose; else the last one a mission opened or got into; else the first open one.
	 */
	private static CampaignCatalog.Entry playedCampaign(final CampaignCatalog catalog,
			final CCampaignProgress.Snapshot progress) {
		final CampaignCatalog.Entry chosen = catalog.getEntry(catalog.menuCampaignNumber(progress));
		if ((chosen != null) && CampaignCatalog.isOpen(chosen, progress)) {
			return chosen;
		}
		CampaignCatalog.Entry played = null;
		CampaignCatalog.Entry first = null;
		for (final CampaignCatalog.Entry entry : catalog.getEntries()) {
			if (!CampaignCatalog.isOpen(entry, progress)) {
				continue;
			}
			if (first == null) {
				first = entry;
			}
			if (started(entry, progress)) {
				played = entry;
			}
		}
		return played != null ? played : first;
	}

	/** A campaign a script opened, or one whose second mission is open. */
	private static boolean started(final CampaignCatalog.Entry entry, final CCampaignProgress.Snapshot progress) {
		return CampaignCatalog.isOpen(entry, progress)
				&& (!entry.getData().isDefaultOpen() || CampaignCatalog.isMissionOpen(entry, 1, progress));
	}

	/** The campaign the progress last chose (else the one played last) and its furthest open mission. */
	private BannerMission nextMission() {
		final CampaignCatalog catalog = this.campaigns.catalog;
		final CCampaignProgress.Snapshot progress = this.campaigns.progress;
		final CampaignCatalog.Entry entry = playedCampaign(catalog, progress);
		if ((entry == null) || entry.getData().getMissions().isEmpty()) {
			return null;
		}
		final List<CampaignMission> missions = entry.getData().getMissions();
		int mission = 0;
		for (int i = 0; i < missions.size(); i++) {
			if (CampaignCatalog.isMissionOpen(entry, i, progress)) {
				mission = i;
			}
		}
		final BannerMission next = new BannerMission();
		next.entry = entry;
		next.mission = mission;
		next.scene = campaignScene(entry.getSection());
		next.background = entry.getData().getBackground();
		next.fog = entry.getData().getBackgroundFogSettings();
		next.title = glueLabel(missions.get(mission).getMissionName(), missions.get(mission).getMissionName())
				.toString();
		next.line = glueLabel(entry.getData().getHeader(), entry.getData().getHeader()) + " · "
				+ glueLabel(missions.get(mission).getHeader(), missions.get(mission).getHeader());
		return next;
	}

	/** What the banner showed last, until the progress is read again; without it the Prologue's first chapter. */
	private BannerMission keptMission() {
		final BannerMission kept = new BannerMission();
		final String[] parts = this.preferences.getString(KEY_BANNER, "").split("\t", -1);
		if (parts.length == 4) {
			kept.scene = parts[0];
			kept.background = parts[1];
			kept.title = parts[2];
			kept.line = parts[3];
			return kept;
		}
		kept.title = glue("CAMP_MAP_PROLOGUE01", "");
		final String campaign = glue("CAMP_TUTORIAL_NAME", "");
		final String chapter = glue("CAMP_CHAPTER_1", getString(R.string.launcher_chapter_one));
		kept.line = campaign.isEmpty() ? chapter : campaign + " · " + chapter;
		return kept;
	}

	/** Where each classic scene's model stands, as a part of the recording's width (measured from its frames). */
	private static float modelX(final String scene) {
		switch (scene) {
		case "roc_prologue":
			return 0.32f;
		case "roc_human":
			return 0.33f;
		case "roc_undead":
			return 0.40f;
		case "roc_orc":
			return 0.31f;
		case "roc_nightelf":
		case "tft_nightelf":
			return 0.34f;
		case "tft_human":
			return 0.26f;
		case "tft_undead":
			return 0.23f;
		case "tft_orc":
			return 0.15f;
		default:
			return 0.5f;
		}
	}

	private FrameLayout banner;
	private BackdropView bannerScene;
	private BannerMission bannerMission;
	private boolean continuePending;

	/** Continue Campaign: the next mission's scene in the skin's frame along the right edge. */
	private void addBanner(final FrameLayout parent) {
		final Board b = this.board;
		this.banner = new FrameLayout(this);
		place(parent, this.banner, b.width - b.bannerW, 0, b.bannerW, b.height);
		this.continuePending = false;
		fillBanner(this.campaigns != null ? nextMission() : null);
	}

	/** The banner once the progress is read: the next mission, and its live scene. */
	private void refillBanner() {
		if (this.bannerScene != null) {
			this.bannerScene.stop();
			this.backdrops.remove(this.bannerScene);
		}
		fillBanner(nextMission());
		this.backdrops.addAll(this.building);
		if (this.resumed) {
			for (final BackdropView backdrop : this.building) {
				backdrop.play();
			}
		}
		this.building.clear();
		showScene(LAYER_BANNER, this.buildingScenes[LAYER_BANNER], this.buildingSceneViews[LAYER_BANNER]);
		this.buildingScenes[LAYER_BANNER] = null;
		this.buildingSceneViews[LAYER_BANNER] = null;
		if (this.continuePending) {
			continueCampaign();
		}
	}

	private void fillBanner(final BannerMission next) {
		final Board b = this.board;
		final boolean live = next != null;
		final BannerMission shown = live ? next : keptMission();
		this.bannerMission = next;
		if (live) {
			this.preferences.edit().putString(KEY_BANNER, shown.scene + "\t" + shown.background + "\t"
					+ shown.title.replace('\t', ' ') + "\t" + shown.line.replace('\t', ' ')).apply();
		}
		final FrameLayout box = this.banner;
		box.removeAllViews();
		final float x = b.width - b.bannerW;
		final float innerW = b.bannerW - b.frame;
		final float innerH = b.height - b.frame;
		final Bitmap recorded = this.art.poster(shown.scene);
		final float focus;
		final Drawable still;
		if (recorded != null) {
			// The Reforged scenes have their figures near the middle; the classic ones are framed on the model.
			focus = this.art.reforged() ? 0.3f
					: modelFocus(modelX(shown.scene), innerW / innerH,
							recorded.getWidth() / (float) recorded.getHeight());
			still = new LauncherKit.CoverDrawable(recorded, focus, 0);
		}
		else {
			focus = 0.5f;
			still = new LauncherKit.CoverDrawable(this.art.bitmap(SkinCache.CAMPAIGN_POSTER), 0.5f, 0);
		}
		if (live) {
			this.bannerScene = sceneBackdrop(still, shown.scene, focus, LAYER_BANNER, shown.background, null,
					shown.fog, 0, x + (b.frame / 2), b.frame / 2, innerW, innerH, modelX(shown.scene));
		}
		else {
			this.bannerScene = backdrop(still, shown.scene, focus);
		}
		place(box, this.bannerScene, b.frame / 2, b.frame / 2, innerW, innerH);
		final View shade = new View(this);
		shade.setBackground(gradient(true, new int[] { Color.TRANSPARENT, Color.TRANSPARENT,
			Color.argb(0xE6, 0, 0, 0), Color.argb(0xE6, 0, 0, 0) }, new float[] { 0, 0.46f, 0.84f, 1 }));
		place(box, shade, b.frame / 2, b.frame / 2, innerW, innerH);
		final View frame = new View(this);
		frame.setBackground(new LauncherKit.FrameDrawable(this.art.bitmap(SkinCache.PANEL_BORDER), px(b.frame))
				.line(this.art.line()));
		place(box, frame, 0, 0, b.bannerW, b.height);

		final float ix = b.tablet ? 64 : 30;
		final float iw = b.bannerW - (b.tablet ? 128 : 60);
		final float bh = b.tablet ? 112 : 96;
		final float by = b.height - (b.tablet ? 72 : 24) - bh;
		place(box, text(getString(R.string.launcher_continue_campaign), b.tablet ? 32 : 22, this.art.gold()), ix,
				by - (b.tablet ? 300 : 178), iw, -1);
		final TextView mission = text(LauncherKit.glueText(shown.title), b.tablet ? 60 : 34, this.art.text());
		mission.setSingleLine(false);
		mission.setMaxLines(2);
		mission.setLineSpacing(0, 1.1f);
		place(box, mission, ix, by - (b.tablet ? 250 : 150), iw, b.tablet ? 140 : 84);
		place(box, text(LauncherKit.glueText(shown.line), b.tablet ? 30 : 20, this.art.text2()), ix,
				by - (b.tablet ? 88 : 50), iw, -1);
		final View hit = new View(this);
		onTap(hit, true, v -> continueCampaign());
		hit.setContentDescription(getString(R.string.launcher_continue_campaign));
		box.addView(hit, frameParams(0, 0, b.bannerW, b.height));
		place(box, button(LauncherKit.glueText(glue("CONTINUE", "Continue")), bh, b.tablet ? 44 : 30,
				v -> continueCampaign()), ix, by, iw, bh);
	}

	/** Straight into the banner's mission with the difficulty last chosen; before the progress is read, once it is. */
	private void continueCampaign() {
		if (this.launching) {
			return;
		}
		if (this.campaigns == null) {
			this.continuePending = true;
			return;
		}
		this.continuePending = false;
		final BannerMission next = this.bannerMission != null ? this.bannerMission : nextMission();
		if (next == null) {
			showCampaign();
			return;
		}
		launchMission(next.entry, next.mission, PAGE_HOME);
	}

	private FrameLayout.LayoutParams frameParams(final float x, final float y, final float w, final float h) {
		final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(px(w), px(h));
		params.leftMargin = px(x);
		params.topMargin = px(y);
		return params;
	}

	/**
	 * Where to crop a recorded scene so that its model stands in the middle of a narrower frame: the model's x (as a
	 * part of the recording's width) moved as far as the recording allows.
	 */
	static float modelFocus(final float modelX, final float frameAspect, final float sceneAspect) {
		final float visible = frameAspect / sceneAspect;
		if (visible >= 1) {
			return 0.5f;
		}
		return Math.max(0, Math.min(1, (modelX - (visible / 2)) / (1 - visible)));
	}

	private void addProfile(final FrameLayout parent, final boolean home) {
		final Board b = this.board;
		final LinearLayout chip = new LinearLayout(this);
		chip.setOrientation(LinearLayout.HORIZONTAL);
		chip.setGravity(Gravity.CENTER_VERTICAL);
		final ImageView portrait = new ImageView(this);
		portrait.setScaleType(ImageView.ScaleType.FIT_XY);
		final String profile = this.profiles.current();
		portrait.setImageBitmap(portraitBitmap(this.profiles.portrait(profile)));
		chip.addView(portrait, new LinearLayout.LayoutParams(px(b.chipH), px(b.chipH)));
		final TextView name = text(this.profiles.any() ? profile : glue("PLAYER", "Player"), b.name,
				this.art.text());
		final LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		nameParams.leftMargin = px(b.tablet ? 28 : 18);
		chip.addView(name, nameParams);
		final float right = home ? b.width - b.bannerW - 40 : b.tablet ? b.width - 40 : b.logoX - 20;
		onTap(chip, false, v -> showProfile(null));
		chip.setContentDescription(profile);
		place(parent, chip, b.chipX, b.chipY, Math.min(b.tablet ? 1400 : 900, right - b.chipX), b.chipH);
	}

	/** A profile portrait of the skin; the first one stands in for one the game data lacks. */
	private Bitmap portraitBitmap(final int index) {
		final Bitmap chosen = this.art.bitmap(SkinCache.portraitFile(index));
		return chosen != null ? chosen : this.art.bitmap(SkinCache.portraitFile(0));
	}

	/** The portraits this skin has in the game data. */
	private List<Integer> portraits() {
		final List<Integer> found = new ArrayList<>();
		for (int i = 0; i < this.cache.portraitCount(); i++) {
			if (this.art.has(SkinCache.portraitFile(i))) {
				found.add(i);
			}
		}
		return found;
	}

	/** The tablet footer: which game art the data holds, the music box and the versions. */
	private void addFooter(final FrameLayout parent) {
		final LinearLayout row = new LinearLayout(this);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setBackgroundColor(Color.argb(0x8C, 0, 0, 0));
		row.setPadding(px(20), 0, px(12), 0);
		row.addView(indicator(getString(R.string.launcher_classic), true));
		final View gap = new View(this);
		row.addView(gap, new LinearLayout.LayoutParams(px(18), 1));
		row.addView(indicator(getString(R.string.launcher_reforged), SkinCache.hasReforged(this)));
		onTap(row, false, v -> showOptions(TAB_GAME_DATA));
		row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
		final LinearLayout music = new LinearLayout(this);
		music.setGravity(Gravity.CENTER_VERTICAL);
		music.addView(text(getString(R.string.launcher_music), 28, Color.rgb(0xE6, 0xE0, 0xD2)));
		final LauncherKit.CheckDrawable check = new LauncherKit.CheckDrawable(this.art);
		check.setChecked(this.preferences.getBoolean(KEY_MUSIC, true));
		final View box = new View(this);
		box.setBackground(check);
		final LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(px(52), px(52));
		boxParams.leftMargin = px(12);
		music.addView(box, boxParams);
		music.setMinimumHeight(px(88));
		onTap(music, false, v -> {
			final boolean on = !this.preferences.getBoolean(KEY_MUSIC, true);
			this.preferences.edit().putBoolean(KEY_MUSIC, on).apply();
			check.setChecked(on);
			updateMusic();
		});
		row.addView(music);
		place(parent, row, 24, 1600, 592, 88);

		final TextView version = text("v" + versionName() + dataVersion(), 24, LauncherKit.CAPTION);
		version.setPadding(px(20), 0, 0, 0);
		version.setShadowLayer(0, 0, 0, 0);
		place(parent, version, 24, 1730, 592, 48);
	}

	private View indicator(final String label, final boolean present) {
		final LinearLayout item = new LinearLayout(this);
		item.setGravity(Gravity.CENTER_VERTICAL);
		final GradientDrawable dot = new GradientDrawable();
		dot.setShape(GradientDrawable.OVAL);
		dot.setColor(present ? LauncherKit.OK : LauncherKit.OFF);
		final View mark = new View(this);
		mark.setBackground(dot);
		final LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(px(20), px(20));
		markParams.rightMargin = px(10);
		item.addView(mark, markParams);
		item.addView(text(label, 28, Color.rgb(0xE6, 0xE0, 0xD2)));
		// The dot's colour alone says nothing to a screen reader; the word does.
		item.setContentDescription(
				label + ": " + getString(present ? R.string.options_found : R.string.options_missing));
		return item;
	}

	private String versionName() {
		try {
			return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		}
		catch (final PackageManager.NameNotFoundException e) {
			return "";
		}
	}

	private boolean installedGame() {
		return "CASC".equalsIgnoreCase(this.ini.get("DataSources").getField("Type00"));
	}

	private String dataFolder() {
		return this.ini.get("DataSources").getField("Path00").replace("\"", "");
	}

	/**
	 * The version and build of the game the data comes from: an installed game's build info; for a folder, the build
	 * info or the data packer's note it was made with, else that the data does not say.
	 */
	private String dataVersion() {
		final File folder = new File(dataFolder());
		String version = "";
		if (installedGame()) {
			version = DataFolderCheck.describe(DataFolderCheck.Kind.CASC, folder).replace("version ", "");
		}
		else {
			version = DataFolderCheck.folderGameVersion(folder);
		}
		return " (" + (version.isEmpty() ? getString(R.string.launcher_data_version_unknown) : version) + ")";
	}

	// ---------------------------------------------------------------- Options and Help

	/** A row of tab buttons over the page content, as wide as six would be; the chosen one stays pressed. */
	private float addTabs(final FrameLayout content, final int[] labels, final int selected,
			final IntConsumer open) {
		final Board b = this.board;
		final float tabW = (b.contentW - (5 * b.tabGap)) / 6;
		for (int i = 0; i < labels.length; i++) {
			final int tab = i;
			final LauncherKit.GlueButton button = button(getString(labels[i]), b.tabH, b.tabText,
					v -> open.accept(tab));
			button.setSelected(i == selected);
			place(content, button, i * (tabW + b.tabGap), 0, tabW, b.tabH);
		}
		return b.tabH + (b.tablet ? 40 : 14);
	}

	/** The page's panel in the skin's frame, from the top given down to the bottom given (content coordinates). */
	private FrameLayout addPanel(final FrameLayout content, final float top, final float bottom) {
		final FrameLayout panel = new FrameLayout(this);
		panel.setBackground(panelFrame(this.board.pageFrame,
				this.art.reforged() ? Color.argb(0xDB, 0x0E, 0x0C, 0x0A) : Color.argb(0xC7, 0, 0, 0)));
		place(content, panel, 0, top, this.board.contentW, bottom - top);
		return panel;
	}

	private FrameLayout content(final FrameLayout view, final float extraHeight) {
		final Board b = this.board;
		final FrameLayout content = new FrameLayout(this);
		place(view, content, b.contentX, b.contentY, b.contentW, b.contentH + extraHeight);
		return content;
	}

	private void showOptions(final int tab) {
		this.optionsTab = tab;
		final Board b = this.board;
		final FrameLayout view = shell(RAIL_OPTIONS);
		final FrameLayout content = content(view, 0);
		final float top = addTabs(content, new int[] { R.string.options_graphics, R.string.options_sound,
			R.string.options_gameplay, R.string.options_touch, R.string.options_game_data, R.string.options_about },
				tab, this::showOptions);
		final FrameLayout panel = addPanel(content, top, b.contentH);
		final ScrollView scroll = new ScrollView(this);
		scroll.setVerticalScrollBarEnabled(true);
		scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
		final LinearLayout rows = new LinearLayout(this);
		rows.setOrientation(LinearLayout.VERTICAL);
		scroll.addView(rows, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));
		place(panel, scroll, b.pageFrame, b.pageFrame, b.contentW - (2 * b.pageFrame),
				b.contentH - top - (2 * b.pageFrame));
		final EngineSettings engine = new EngineSettings(this);
		switch (tab) {
		case TAB_SOUND:
			soundRows(rows, engine);
			break;
		case TAB_GAMEPLAY:
			gameplayRows(rows, engine);
			break;
		case TAB_TOUCH:
			touchRows(rows, engine);
			break;
		case TAB_GAME_DATA:
			gameDataRows(rows);
			break;
		case TAB_ABOUT:
			aboutRows(rows);
			break;
		default:
			graphicsRows(rows, engine);
			break;
		}
		if (b.tablet) {
			// The tablet's panel is as high as its rows, as the design draws it; a longer tab scrolls.
			int rowsH = 0;
			for (int i = 0; i < rows.getChildCount(); i++) {
				final int h = rows.getChildAt(i).getLayoutParams().height;
				rowsH = (h < 0) || (rowsH < 0) ? -1 : rowsH + h;
			}
			if (rowsH >= 0) {
				final int panelH = Math.min(px(b.contentH - top), rowsH + px(2 * b.pageFrame));
				panel.getLayoutParams().height = panelH;
				scroll.getLayoutParams().height = panelH - px(2 * b.pageFrame);
			}
		}
		place(view, button(getString(R.string.launcher_back), b.backH, b.tablet ? 36 : 30, v -> showHome()), b.backX,
				b.backY, b.backW, b.backH);
		setPage(view, PAGE_OPTIONS);
	}

	private void graphicsRows(final LinearLayout rows, final EngineSettings engine) {
		final boolean reforgedHere = SkinCache.hasReforged(this);
		radioRow(rows, R.string.options_graphics_set,
				new String[] { getString(R.string.launcher_classic),
					getString(reforgedHere ? R.string.launcher_reforged : R.string.options_reforged_locked) },
				new boolean[] { true, reforgedHere }, this.cache.reforged() ? 1 : 0, choice -> chooseSkin(choice == 1));
		checkRow(rows, R.string.options_animated, this.preferences.getBoolean(KEY_ANIMATED, true),
				on -> this.preferences.edit().putBoolean(KEY_ANIMATED, on).apply());
		radioRow(rows, R.string.options_particles,
				new String[] { getString(R.string.options_low), getString(R.string.options_medium),
					getString(R.string.options_high) },
				null, Math.max(0, Math.min(2, engine.number("particles", 2))),
				choice -> engine.setNumber("particles", choice));
		checkRow(rows, R.string.options_unit_shadows, engine.flag("unitShadows", true),
				on -> engine.setFlag("unitShadows", on));
		sliderRow(rows, R.string.options_gamma, engine.number("gamma", 50), getString(R.string.options_low),
				getString(R.string.options_high), (value, done) -> {
					if (done) {
						engine.setNumber("gamma", value);
					}
				});
	}

	/** The engine's sound settings; their defaults are the engine's (music at 40, as Warcraft III has it). */
	private void soundRows(final LinearLayout rows, final EngineSettings engine) {
		checkRow(rows, R.string.options_sound_effects, engine.flag("sound", true), on -> engine.setFlag("sound", on));
		volumeRow(rows, R.string.options_sound_volume, engine, "soundVolume", 100);
		checkRow(rows, R.string.launcher_music, engine.flag("music", true), on -> engine.setFlag("music", on));
		volumeRow(rows, R.string.options_music_volume, engine, "musicVolume", 40);
		checkRow(rows, R.string.options_ambient, engine.flag("ambientSounds", true),
				on -> engine.setFlag("ambientSounds", on));
		checkRow(rows, R.string.options_movement, engine.flag("movementSounds", true),
				on -> engine.setFlag("movementSounds", on));
		checkRow(rows, R.string.options_unit_sounds, engine.flag("unitSounds", true),
				on -> engine.setFlag("unitSounds", on));
		checkRow(rows, R.string.options_subtitles, engine.flag("subtitles", true),
				on -> engine.setFlag("subtitles", on));
		checkRow(rows, R.string.options_menu_music, this.preferences.getBoolean(KEY_MUSIC, true), on -> {
			this.preferences.edit().putBoolean(KEY_MUSIC, on).apply();
			updateMusic();
		});
	}

	private void volumeRow(final LinearLayout rows, final int label, final EngineSettings engine, final String key,
			final int fallback) {
		sliderRow(rows, label, engine.number(key, fallback), getString(R.string.options_low),
				getString(R.string.options_high), (value, done) -> {
					if (done) {
						engine.setNumber(key, value);
					}
				});
	}

	private void gameplayRows(final LinearLayout rows, final EngineSettings engine) {
		// With touch the mouse scroll speed is the speed of a camera drag.
		sliderRow(rows, R.string.options_scroll, engine.number("mouseScroll", 50), getString(R.string.options_slow),
				getString(R.string.options_fast), (value, done) -> {
					if (done) {
						engine.setNumber("mouseScroll", value);
					}
				});
		checkRow(rows, R.string.options_tooltips, engine.flag("enhancedTooltips", true),
				on -> engine.setFlag("enhancedTooltips", on));
		checkRow(rows, R.string.options_health_bars, engine.flag("statusBars", false),
				on -> engine.setFlag("statusBars", on));
		checkRow(rows, R.string.options_formation, engine.flag("formationMovement", true),
				on -> engine.setFlag("formationMovement", on));
		infoRow(rows, R.string.options_profile, this.profiles.current(), false);
		final View profileRow = rows.getChildAt(rows.getChildCount() - 1);
		onTap(profileRow, false, v -> showProfile(null));
		profileRow.setContentDescription(getString(R.string.options_profile));
	}

	/** The [Touch] keys of warsmash.ini with the engine's defaults and a sensible range around them. */
	private void touchRows(final LinearLayout rows, final EngineSettings engine) {
		touchRow(rows, R.string.options_tap_slop, engine, "TapSlopDp", 10, 4, 24, R.string.options_dp);
		touchRow(rows, R.string.options_long_press, engine, "LongPressMs", 300, 200, 800, R.string.options_ms);
		touchRow(rows, R.string.options_palm, engine, "PalmTouchMajorDp", 40, 20, 80, R.string.options_dp);
		touchRow(rows, R.string.options_tap_reach, engine, "GhostTapRadiusDp", 48, 16, 96, R.string.options_dp);
		checkRow(rows, R.string.options_pen_fingers, "Allow".equalsIgnoreCase(engine.touch("PenFingerPolicy", "")),
				on -> engine.setTouch("PenFingerPolicy", on ? "Allow" : "Ignore"));
	}

	private void touchRow(final LinearLayout rows, final int label, final EngineSettings engine, final String key,
			final int fallback, final int min, final int max, final int unit) {
		int current;
		try {
			current = Math.round(Float.parseFloat(engine.touch(key, Integer.toString(fallback))));
		}
		catch (final NumberFormatException e) {
			current = fallback;
		}
		current = Math.max(min, Math.min(max, current));
		final TextView shown = text(getString(unit, current), this.board.small, this.art.text());
		sliderRow(rows, label, Math.round(((current - min) * 100f) / (max - min)), null, null, (value, done) -> {
			final int next = min + Math.round(((max - min) * value) / 100f);
			shown.setText(getString(unit, next));
			if (done) {
				engine.setTouch(key, Integer.toString(next));
			}
		}, shown);
	}

	private void gameDataRows(final LinearLayout rows) {
		infoRow(rows, R.string.options_source,
				getString(installedGame() ? R.string.options_source_casc : R.string.options_source_folder), false);
		infoRow(rows, R.string.options_folder, dataFolder(), true);
		final String version = dataVersion();
		infoRow(rows, R.string.options_version, version.isEmpty() ? getString(R.string.options_version_unknown)
				: version.trim().replace("(", "").replace(")", ""), false);
		final DataFolderCheck.Kind kind = installedGame() ? DataFolderCheck.Kind.CASC : DataFolderCheck.Kind.FOLDER;
		final File folder = new File(dataFolder());
		final String locale = DataFolderCheck.locale(kind, folder);
		infoRow(rows, R.string.options_locale, locale.isEmpty() ? getString(R.string.options_not_stated) : locale,
				false);
		if (!installedGame()) {
			infoRow(rows, R.string.options_files, DataFolderCheck.describe(kind, folder), false);
			final String packer = DataFolderCheck.packerTool(folder);
			infoRow(rows, R.string.options_packer, packer.isEmpty() ? getString(R.string.options_not_stated) : packer,
					false);
		}
		infoRow(rows, R.string.options_reforged_ui,
				getString(SkinCache.hasReforged(this) ? R.string.options_found : R.string.options_missing), false);
		final int recordings = this.art.recordings();
		infoRow(rows, R.string.options_backgrounds, recordings == 0 ? getString(R.string.options_backgrounds_missing)
				: getString(R.string.options_backgrounds_count, recordings, SkinCache.SCENES.length), false);
		final Board b = this.board;
		final FrameLayout row = row(rows);
		final float buttonW = b.tablet ? 480 : 300;
		final float buttonH = b.tablet ? 112 : 80;
		place(row, button(getString(R.string.options_change), buttonH, b.tablet ? 36 : 26, v -> openGameData()),
				b.labelX, (b.rowH - buttonH) / 2, buttonW, buttonH);
		place(row, button(getString(R.string.options_recheck), buttonH, b.tablet ? 36 : 26, v -> recheck()),
				b.labelX + buttonW + (b.tablet ? 32 : 24), (b.rowH - buttonH) / 2, buttonW, buttonH);
	}

	private void aboutRows(final LinearLayout rows) {
		infoRow(rows, R.string.options_launcher, "v" + versionName(), false);
		infoRow(rows, R.string.options_engine, getString(R.string.options_engine_value), false);
		infoRow(rows, R.string.options_license, getString(R.string.options_license_value), false);
		infoRow(rows, R.string.options_art, getString(R.string.options_art_value), false);
	}

	/** An option row: the gold label on the left, a line above every row but the first. */
	private FrameLayout row(final LinearLayout rows) {
		final Board b = this.board;
		final FrameLayout row = new FrameLayout(this);
		if (rows.getChildCount() > 0) {
			final View line = new View(this);
			line.setBackgroundColor(Color.argb(0x14, 0xFF, 0xFF, 0xFF));
			row.addView(line, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, px(2))));
		}
		rows.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(b.rowH)));
		return row;
	}

	private FrameLayout labelledRow(final LinearLayout rows, final int label) {
		final Board b = this.board;
		final FrameLayout row = row(rows);
		final TextView name = text(getString(label), b.body, this.art.gold());
		name.setGravity(Gravity.CENTER_VERTICAL);
		place(row, name, b.labelX, 0, b.controlX - b.pageFrame - b.labelX - 16, b.rowH);
		return row;
	}

	/** Control positions are given in content coordinates; the rows start inside the panel frame. */
	private float controlX(final float offset) {
		return (this.board.controlX - this.board.pageFrame) + offset;
	}

	private void checkRow(final LinearLayout rows, final int label, final boolean value,
			final Consumer<Boolean> changed) {
		final Board b = this.board;
		final FrameLayout row = labelledRow(rows, label);
		final LauncherKit.CheckDrawable check = new LauncherKit.CheckDrawable(this.art);
		check.setChecked(value);
		final View box = new View(this);
		box.setBackground(check);
		place(row, box, controlX(0), (b.rowH - b.control) / 2, b.control, b.control);
		final boolean[] on = { value };
		// The whole row is the touch target.
		onTap(row, false, v -> {
			on[0] = !on[0];
			check.setChecked(on[0]);
			changed.accept(on[0]);
		});
		row.setContentDescription(getString(label));
	}

	private void radioRow(final LinearLayout rows, final int label, final String[] choices, final boolean[] enabled,
			final int selected, final IntConsumer changed) {
		final Board b = this.board;
		final FrameLayout row = labelledRow(rows, label);
		final float spacing = choices.length > 2 ? (b.tablet ? 340 : 190) : (b.tablet ? 440 : 240);
		final LauncherKit.RadioDrawable[] marks = new LauncherKit.RadioDrawable[choices.length];
		final int[] current = { selected };
		for (int i = 0; i < choices.length; i++) {
			final int choice = i;
			final boolean active = (enabled == null) || enabled[i];
			final LinearLayout item = new LinearLayout(this);
			item.setGravity(Gravity.CENTER_VERTICAL);
			marks[i] = new LauncherKit.RadioDrawable(this.art);
			marks[i].set(i == selected, active);
			final View mark = new View(this);
			mark.setBackground(marks[i]);
			item.addView(mark, new LinearLayout.LayoutParams(px(b.control), px(b.control)));
			final TextView name = text(choices[i], b.body, active ? this.art.text() : LauncherKit.DISABLED);
			final LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			nameParams.leftMargin = px(14);
			item.addView(name, nameParams);
			item.setEnabled(active);
			if (active) {
				onTap(item, false, v -> {
					if (current[0] == choice) {
						return;
					}
					marks[current[0]].set(false, true);
					current[0] = choice;
					marks[choice].set(true, true);
					changed.accept(choice);
				});
			}
			place(row, item, controlX(i * spacing), 0, spacing - 16, b.rowH);
		}
	}

	private void sliderRow(final LinearLayout rows, final int label, final int value, final String low,
			final String high, final LauncherKit.Slider.Listener changed) {
		sliderRow(rows, label, value, low, high, changed, null);
	}

	/** Low and High on either side of the slider, or the slider and its value shown after it. */
	private void sliderRow(final LinearLayout rows, final int label, final int value, final String low,
			final String high, final LauncherKit.Slider.Listener changed, final TextView shown) {
		final Board b = this.board;
		final FrameLayout row = labelledRow(rows, label);
		final float lowW = low != null ? (b.tablet ? 90 : 72) : 0;
		final float sliderW = b.tablet ? 720 : 380;
		if (low != null) {
			final TextView lowText = text(low, b.small, this.art.text());
			lowText.setGravity(Gravity.CENTER_VERTICAL);
			place(row, lowText, controlX(0), 0, lowW, b.rowH);
		}
		final LauncherKit.Slider slider = new LauncherKit.Slider(this, this.art, px(b.control * 0.5f), px(b.control));
		slider.setValue(value);
		slider.setListener(changed);
		slider.setContentDescription(getString(label));
		place(row, slider, controlX(lowW), 0, sliderW, b.rowH);
		final TextView after = shown != null ? shown : text(high, b.small, this.art.text());
		after.setGravity(Gravity.CENTER_VERTICAL);
		place(row, after, controlX(lowW + sliderW + (b.tablet ? 20 : 12)), 0, b.tablet ? 240 : 140, b.rowH);
	}

	private void infoRow(final LinearLayout rows, final int label, final String value, final boolean path) {
		final Board b = this.board;
		final FrameLayout row = labelledRow(rows, label);
		final TextView shown = text(value, b.body, this.art.text());
		shown.setGravity(Gravity.CENTER_VERTICAL);
		// A long folder keeps its last parts visible.
		shown.setEllipsize(path ? TextUtils.TruncateAt.START : TextUtils.TruncateAt.END);
		place(row, shown, controlX(0), 0, b.contentW - b.controlX - b.pageFrame - 24, b.rowH);
	}

	private void chooseSkin(final boolean reforged) {
		final String skin = reforged ? SkinCache.REFORGED_SKIN : SkinCache.CLASSIC_SKIN;
		this.preferences.edit().putString(KEY_SKIN, skin).apply();
		this.cache = SkinCache.forConfiguration(this, this.ini, skin);
		if (this.cache.isCurrent()) {
			showOptions(TAB_GRAPHICS);
		}
		else {
			this.afterPrep = PAGE_OPTIONS;
			showPrep();
		}
	}

	/** Re-check: the art is prepared again from the game data, as after a change of data. */
	private void recheck() {
		this.cache.invalidate();
		this.afterPrep = PAGE_OPTIONS;
		this.optionsTab = TAB_GAME_DATA;
		showPrep();
	}

	private void showHelp(final int topic) {
		this.helpTab = topic;
		final Board b = this.board;
		final FrameLayout view = shell(RAIL_HELP);
		// Help has no action bar; on the phone its panel takes that space too.
		final float extra = b.tablet ? 0 : b.height - 16 - b.contentY - b.contentH;
		final FrameLayout content = content(view, extra);
		final float top = addTabs(content, new int[] { R.string.help_touch, R.string.help_game_data,
			R.string.help_problems }, topic, this::showHelp);
		final float bottom = b.contentH + extra;
		final FrameLayout panel = addPanel(content, top, bottom);
		final float frame = b.tablet ? 44 : 18;
		final float x = frame + (b.tablet ? 20 : 6);
		final float w = b.contentW - (2 * x);
		final float h = bottom - top;
		if (topic == 0) {
			gestureCards(panel, x, frame, w, h - (2 * frame));
		}
		else if (topic == 1) {
			numberedRows(panel, x, frame, w, h - (2 * frame), R.string.help_data_title,
					new int[][] { { R.string.help_step_pc, R.string.help_step_pc_line },
						{ R.string.help_step_zip, R.string.help_step_zip_line },
						{ R.string.help_step_folder, R.string.help_step_folder_line } },
					new LauncherKit.Pictogram.Kind[] { LauncherKit.Pictogram.Kind.COMPUTER,
						LauncherKit.Pictogram.Kind.ZIP, LauncherKit.Pictogram.Kind.FOLDER });
		}
		else {
			numberedRows(panel, x, frame, w, h - (2 * frame), R.string.help_problems_title,
					new int[][] { { R.string.help_problem_data, R.string.help_problem_data_line },
						{ R.string.help_problem_zip, R.string.help_problem_zip_line },
						{ R.string.help_problem_save, R.string.help_problem_save_line },
						{ R.string.help_problem_closed, R.string.help_problem_closed_line },
						{ R.string.help_problem_backgrounds, R.string.help_problem_backgrounds_line } },
					null);
		}
		setPage(view, PAGE_HELP);
	}

	/** One card per gesture in a 4 x 2 grid: the pictogram, the gesture's name and what it does. */
	private void gestureCards(final FrameLayout panel, final float x, final float y, final float w, final float h) {
		final Board b = this.board;
		final Object[][] gestures = { { LauncherKit.Pictogram.Kind.TAP, R.string.help_tap, R.string.help_tap_line },
			{ LauncherKit.Pictogram.Kind.DRAG, R.string.help_drag, R.string.help_drag_line },
			{ LauncherKit.Pictogram.Kind.HOLD, R.string.help_hold, R.string.help_hold_line },
			{ LauncherKit.Pictogram.Kind.SHIFT, R.string.help_shift, R.string.help_shift_line },
			{ LauncherKit.Pictogram.Kind.TWO, R.string.help_two, R.string.help_two_line },
			{ LauncherKit.Pictogram.Kind.PINCH, R.string.help_pinch, R.string.help_pinch_line },
			{ LauncherKit.Pictogram.Kind.MINIMAP, R.string.help_minimap, R.string.help_minimap_line } };
		float top = y + (b.tablet ? 4 : 4);
		if (b.tablet) {
			place(panel, text(getString(R.string.help_touch_title), b.title, this.art.text()), x, y, w, -1);
			top = y + 96;
		}
		final int columns = 4;
		final float gap = b.tablet ? 24 : 10;
		final float cardW = (w - ((columns - 1) * gap)) / columns;
		final float cardH = ((y + h) - top - gap) / 2;
		final float nameSize = b.tablet ? 36 : 24;
		final float lineSize = b.tablet ? 28 : 20;
		for (int i = 0; i < gestures.length; i++) {
			final float cx = x + ((i % columns) * (cardW + gap));
			final float cy = top + ((i / columns) * (cardH + gap));
			final FrameLayout card = new FrameLayout(this);
			final LauncherKit.FrameDrawable border = new LauncherKit.FrameDrawable(this.art.bitmap(SkinCache.EDIT_BORDER),
					px(b.tablet ? 14 : 8)).tiled(this.art.bitmap(SkinCache.EDIT_BACKGROUND), px(b.tablet ? 110 : 60))
							.fallback(Color.argb(0xCC, 0x05, 0x06, 0x0C)).line(this.art.line());
			card.setBackground(this.art.reforged() ? border.centre() : border);
			place(panel, card, cx, cy, cardW, cardH);
			final float icon = Math.min(cardH * 0.5f, cardW * 0.6f);
			final View picture = new View(this);
			picture.setBackground(new LauncherKit.Pictogram((LauncherKit.Pictogram.Kind) gestures[i][0],
					this.art.font()));
			place(card, picture, (cardW - icon) / 2, b.tablet ? 24 : 6, icon, icon);
			final TextView name = text(getString((Integer) gestures[i][1]), nameSize, this.art.gold());
			name.setGravity(Gravity.CENTER_HORIZONTAL);
			place(card, name, 8, icon + (b.tablet ? 34 : 10), cardW - 16, -1);
			final TextView line = lines(getString((Integer) gestures[i][2]), lineSize, this.art.text(), 2);
			line.setGravity(Gravity.CENTER_HORIZONTAL);
			final float lineY = icon + (b.tablet ? 86 : 42);
			place(card, line, 12, lineY, cardW - 24, cardH - lineY - 4);
			card.setContentDescription(getString((Integer) gestures[i][1]) + ": "
					+ getString((Integer) gestures[i][2]));
		}
	}

	/** Numbered rows: a number in a disc, an optional pictogram, the gold title and one white line. */
	private void numberedRows(final FrameLayout panel, final float x, final float y, final float w, final float h,
			final int title, final int[][] items, final LauncherKit.Pictogram.Kind[] pictures) {
		final Board b = this.board;
		float top = y + (b.tablet ? 4 : 0);
		if (b.tablet) {
			place(panel, text(getString(title), b.title, this.art.text()), x, y, w, -1);
			top = y + 110;
		}
		final float step = ((y + h) - top) / items.length;
		final float number = b.tablet ? 56 : 36;
		final float picture = pictures == null ? 0 : Math.min(step * 0.8f, b.tablet ? 160 : 96);
		final float textX = x + number + (b.tablet ? 28 : 16) + (picture > 0 ? picture + (b.tablet ? 32 : 16) : 0);
		final float titleSize = b.tablet ? 36 : 24;
		final float lineSize = b.tablet ? 30 : 21;
		for (int i = 0; i < items.length; i++) {
			final float rowY = top + (i * step);
			final TextView disc = text(Integer.toString(i + 1), number * 0.55f, this.art.gold());
			disc.setGravity(Gravity.CENTER);
			disc.setBackground(LauncherKit.disc());
			place(panel, disc, x, rowY, number, number);
			if (picture > 0) {
				final View image = new View(this);
				image.setBackground(new LauncherKit.Pictogram(pictures[i], this.art.font()));
				place(panel, image, x + number + (b.tablet ? 28 : 16), rowY, picture, picture);
			}
			place(panel, text(getString(items[i][0]), titleSize, this.art.gold()), textX, rowY, w - (textX - x), -1);
			place(panel, lines(getString(items[i][1]), lineSize, this.art.text(), 2), textX,
					rowY + titleSize + (b.tablet ? 14 : 6), w - (textX - x), -1);
		}
	}

	// ---------------------------------------------------------------- game pages: shared parts

	private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
	private static final int ERROR = Color.rgb(0xE0, 0x56, 0x4E);
	private TextView pageNote;

	/** The page's frame while the catalog reads what the page lists. */
	private int loadingPage(final int rail, final int kind) {
		final FrameLayout view = shell(rail);
		final FrameLayout content = content(view, 0);
		this.pageNote = text(getString(R.string.game_loading_list), this.board.body, this.art.text2());
		place(content, this.pageNote, 0, 0, this.board.contentW, -1);
		setPage(view, kind);
		return this.generation;
	}

	private void unreadable() {
		if (this.pageNote != null) {
			this.pageNote.setTextColor(ERROR);
			this.pageNote.setText(R.string.game_unreadable);
		}
	}

	/** A panel in the skin's frame at a place of the content. */
	private FrameLayout framed(final FrameLayout parent, final float x, final float y, final float w, final float h) {
		final FrameLayout panel = new FrameLayout(this);
		panel.setBackground(panelFrame(this.board.pageFrame,
				this.art.reforged() ? Color.argb(0xDB, 0x0E, 0x0C, 0x0A) : Color.argb(0xC7, 0, 0, 0)));
		place(parent, panel, x, y, w, h);
		return panel;
	}

	/** The edit box frame of the difficulty box and the drop-down menus. */
	private Drawable editFrame(final float border) {
		final LauncherKit.FrameDrawable frame = new LauncherKit.FrameDrawable(this.art.bitmap(SkinCache.EDIT_BORDER),
				px(border)).tiled(this.art.bitmap(SkinCache.EDIT_BACKGROUND), px(this.board.tablet ? 110 : 60))
						.fillInset(px(border * 0.4f)).fallback(Color.argb(0xCC, 0x05, 0x06, 0x0C)).line(this.art.line());
		return this.art.reforged() ? frame.centre() : frame;
	}

	private LinearLayout scrollList(final FrameLayout parent, final float x, final float y, final float w,
			final float h) {
		final ScrollView scroll = new ScrollView(this);
		scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
		scroll.setVerticalScrollBarEnabled(true);
		final LinearLayout rows = new LinearLayout(this);
		rows.setOrientation(LinearLayout.VERTICAL);
		scroll.addView(rows, new FrameLayout.LayoutParams(MATCH, ViewGroup.LayoutParams.WRAP_CONTENT));
		place(parent, scroll, x, y, w, h);
		return rows;
	}

	/** Scrolls a list so that its chosen row is in view. */
	private static void reveal(final View row) {
		row.post(() -> {
			final ScrollView scroll = (ScrollView) row.getParent().getParent();
			final int visible = scroll.getHeight();
			if ((row.getTop() < scroll.getScrollY()) || ((row.getBottom()) > (scroll.getScrollY() + visible))) {
				scroll.scrollTo(0, Math.max(0, row.getTop() - ((visible - row.getHeight()) / 2)));
			}
		});
	}

	/** The chosen row of a list carries the skin's row highlight. */
	private void markRow(final View row, final boolean chosen) {
		if (!chosen) {
			row.setBackground(null);
			return;
		}
		final Bitmap glow = this.art.bitmap(SkinCache.ROW_HIGHLIGHT);
		row.setBackground(glow != null ? new BitmapDrawable(getResources(), glow)
				: new ColorDrawable(Color.argb(0x40, 0xFC, 0xD3, 0x12)));
	}

	private ImageView image(final String name, final ImageView.ScaleType scaleType) {
		final ImageView view = new ImageView(this);
		view.setScaleType(scaleType);
		view.setImageBitmap(this.art.bitmap(name));
		return view;
	}

	/** The action bar: 0 is Back, 1 the secondary place, 2 the main action. */
	private LauncherKit.GlueButton action(final FrameLayout view, final int slot, final CharSequence label,
			final View.OnClickListener listener) {
		final Board b = this.board;
		final float x;
		final float w;
		if (slot == 0) {
			x = b.backX;
			w = b.backW;
		}
		else if (slot == 1) {
			x = b.tablet ? 1880 : 880;
			w = b.tablet ? 480 : 320;
		}
		else {
			x = b.tablet ? 2400 : 1224;
			w = b.tablet ? 520 : 360;
		}
		final LauncherKit.GlueButton button = button(label, b.backH, b.tablet ? 36 : 30, listener);
		button.setMain(slot == 2);
		if (slot == 2) {
			button.setTone(LauncherKit.GlueButton.TONE_CONFIRM);
		}
		place(view, button, x, b.backY, w, b.backH);
		return button;
	}

	private CharSequence glueLabel(final String key, final String fallback) {
		return LauncherKit.glueText(glue(key, fallback));
	}

	private String[] glueList(final String[][] table, final int keyColumn) {
		final String[] labels = new String[table.length];
		for (int i = 0; i < table.length; i++) {
			labels[i] = glue(table[i][keyColumn], table[i][keyColumn + 1]);
		}
		return labels;
	}

	/** A drop-down menu as the glue screens have it: the glue button, the chosen item and the pull-down arrow. */
	private View dropdown(final String[] items, final int selected, final boolean enabled, final float height,
			final float textSize, final IntConsumer chosen) {
		return dropdown(items, selected, enabled, height, textSize, chosen, null);
	}

	/** @param colors with them the menu shows a colour (a player colour menu), its items named in their colour */
	private View dropdown(final String[] items, final int selected, final boolean enabled, final float height,
			final float textSize, final IntConsumer chosen, final int[] colors) {
		final FrameLayout box = new FrameLayout(this);
		box.setBackground(LauncherKit.GlueButton.backdrop(this.art, px(height), LauncherKit.GlueButton.TONE_PLAIN));
		// Inside the button's rim, as the button's own text sits.
		final float rim = this.art.reforged() ? Math.max(8, height * 0.3f) : Math.max(4, height * 0.22f);
		final float arrowBox = Math.min(height, 72);
		final float arrowSize = arrowBox * 0.42f;
		final TextView label = text(LauncherKit.glueText(items[selected]), textSize,
				enabled ? LauncherKit.GlueButton.restText(this.art) : LauncherKit.DISABLED);
		label.setGravity(Gravity.CENTER_VERTICAL);
		final FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(MATCH, MATCH);
		labelParams.leftMargin = px(rim + 4);
		labelParams.rightMargin = px(Math.max(rim + 4, arrowBox * 0.8f));
		box.addView(label, labelParams);
		final View swatch = colors != null ? new View(this) : null;
		if (swatch != null) {
			// The colour menu shows only the colour, in the middle of the button, without the arrow.
			label.setVisibility(View.GONE);
			swatch.setBackgroundColor(colors[selected]);
			swatch.setAlpha(enabled ? 1f : 0.5f);
			box.addView(swatch, new FrameLayout.LayoutParams(px(height * 0.9f),
					px(Math.max(8, (height - (2 * rim)) * 0.55f)), Gravity.CENTER));
		}
		else {
			final ImageView arrow = image(SkinCache.PULLDOWN_ARROW, ImageView.ScaleType.FIT_CENTER);
			arrow.setAlpha(enabled ? 1f : 0.4f);
			final FrameLayout.LayoutParams arrowParams = new FrameLayout.LayoutParams(px(arrowSize), px(arrowSize),
					Gravity.END | Gravity.CENTER_VERTICAL);
			arrowParams.rightMargin = px((arrowBox * 0.75f) - arrowSize);
			box.addView(arrow, arrowParams);
		}
		box.setEnabled(enabled);
		box.setContentDescription(LauncherKit.glueText(items[selected]).toString());
		if (enabled && (chosen != null)) {
			final int[] current = { selected };
			onTap(box, false, v -> showChoices(box, items, current[0], textSize, colors, choice -> {
				current[0] = choice;
				label.setText(LauncherKit.glueText(items[choice]));
				if (swatch != null) {
					swatch.setBackgroundColor(colors[choice]);
				}
				box.setContentDescription(LauncherKit.glueText(items[choice]).toString());
				chosen.accept(choice);
			}));
		}
		return box;
	}

	/** The open menu of a drop-down, under it or, near the bottom, over it. */
	private void showChoices(final View anchor, final String[] items, final int selected, final float textSize,
			final int[] colors, final IntConsumer chosen) {
		if (this.dialog != null) {
			return;
		}
		final Board b = this.board;
		final int[] at = new int[2];
		anchor.getLocationInWindow(at);
		final int[] origin = new int[2];
		this.root.getLocationInWindow(origin);
		final float x = (at[0] - origin[0]) / this.scale;
		final float top = (at[1] - origin[1]) / this.scale;
		final float w = anchor.getWidth() / this.scale;
		final float anchorH = anchor.getHeight() / this.scale;
		final float itemH = b.tablet ? Math.max(88, anchorH * 0.8f) : 96;
		final float edge = b.tablet ? 14 : 8;
		// A long menu (the player colours on the phone) scrolls within the screen.
		final float h = Math.min((items.length * itemH) + (2 * edge), b.height - 16);
		float y = top + anchorH;
		if ((y + h) > b.height) {
			y = Math.max(8, top - h);
		}
		if ((y + h) > b.height) {
			y = b.height - 8 - h;
		}
		final FrameLayout scrim = new FrameLayout(this);
		scrim.setClickable(true);
		scrim.setOnClickListener(v -> closeDialog());
		final LinearLayout list = new LinearLayout(this);
		list.setOrientation(LinearLayout.VERTICAL);
		list.setBackground(new LauncherKit.FrameDrawable(this.art.bitmap(SkinCache.EDIT_BORDER), px(edge))
				.filled(Color.argb(0xF2, 0, 0, 0)).fillInset(px(edge * 0.4f)).fallback(Color.argb(0xF2, 0x03, 0x04, 0x0A))
				.line(this.art.line()));
		list.setPadding(px(edge), px(edge), px(edge), px(edge));
		list.setClickable(true);
		for (int i = 0; i < items.length; i++) {
			final int choice = i;
			final TextView item = text(LauncherKit.glueText(items[i]), textSize,
					colors != null ? colors[i] : i == selected ? this.art.gold() : this.art.text());
			item.setGravity(Gravity.CENTER_VERTICAL);
			item.setPadding(px(b.tablet ? 16 : 10), 0, px(8), 0);
			markRow(item, i == selected);
			onTap(item, false, v -> {
				closeDialog();
				chosen.accept(choice);
			});
			list.addView(item, new LinearLayout.LayoutParams(MATCH, px(itemH)));
		}
		final ScrollView scroll = new ScrollView(this);
		scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
		scroll.addView(list, new FrameLayout.LayoutParams(MATCH, ViewGroup.LayoutParams.WRAP_CONTENT));
		place(scrim, scroll, x, y, w, h);
		this.root.addView(scrim, new FrameLayout.LayoutParams(MATCH, MATCH));
		this.dialog = scrim;
		if (selected > 0) {
			scroll.post(() -> scroll.scrollTo(0, px(selected * itemH) - ((scroll.getHeight() - px(itemH)) / 2)));
		}
	}

	/** A check box row: the whole row is the touch target. */
	private View checkLine(final CharSequence label, final boolean value, final float height, final float size,
			final Consumer<Boolean> changed) {
		final LinearLayout row = new LinearLayout(this);
		row.setGravity(Gravity.CENTER_VERTICAL);
		final LauncherKit.CheckDrawable check = new LauncherKit.CheckDrawable(this.art);
		check.setChecked(value);
		final View box = new View(this);
		box.setBackground(check);
		final float boxSize = Math.min(height * 0.6f, this.board.control);
		row.addView(box, new LinearLayout.LayoutParams(px(boxSize), px(boxSize)));
		final TextView name = text(label, size, this.art.text());
		final LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0,
				ViewGroup.LayoutParams.WRAP_CONTENT, 1);
		nameParams.leftMargin = px(this.board.tablet ? 18 : 12);
		row.addView(name, nameParams);
		final boolean[] on = { value };
		onTap(row, false, v -> {
			on[0] = !on[0];
			check.setChecked(on[0]);
			changed.accept(on[0]);
		});
		row.setContentDescription(label.toString());
		return row;
	}

	/** Starts a game in the engine; the launcher comes back when it ends, on the page given. */
	private void launchGame(final Intent intent, final int returnPage) {
		// A second tap while the first one starts the game would start another game in the same process.
		if (this.launching || isFinishing()) {
			return;
		}
		this.launching = true;
		intent.setClass(this, AndroidLauncher.class);
		stopMusic();
		intent.putExtra(LaunchContract.EXTRA_PROFILE, this.profiles.current());
		LaunchContract.takeResult(this);
		intent.putExtra(LaunchContract.EXTRA_TOKEN, LaunchContract.newToken(this));
		this.preferences.edit().putInt(KEY_RETURN_PAGE, returnPage)
				.putLong(KEY_LAUNCHED, System.currentTimeMillis()).commit();
		startActivity(intent);
		finish();
		cutTo();
	}

	/**
	 * The engine's loading screen takes the whole screen at once: the window animation would slide it in over the
	 * launcher's menu, which then shows beside the loading art.
	 */
	private void cutTo() {
		overridePendingTransition(0, 0);
	}

	// ---------------------------------------------------------------- Campaign and Missions

	private boolean campaignsReady(final int kind) {
		if (this.campaigns != null) {
			return true;
		}
		final int generation = loadingPage(RAIL_CAMPAIGN, kind);
		this.catalog.campaigns(found -> {
			if (generation != this.generation) {
				return;
			}
			if (found == null) {
				unreadable();
				return;
			}
			this.campaigns = found;
			showPage(kind);
		});
		return false;
	}

	/** The campaign last chosen, else the one the profile's last mission chose, else the first open one. */
	private CampaignCatalog.Entry chosenCampaign() {
		final CampaignCatalog catalog = this.campaigns.catalog;
		final CCampaignProgress.Snapshot progress = this.campaigns.progress;
		final int menu = catalog.menuCampaignNumber(progress);
		for (final int number : new int[] { this.preferences.getInt(KEY_CAMPAIGN, menu), menu }) {
			final CampaignCatalog.Entry entry = catalog.getEntry(number);
			if ((entry != null) && CampaignCatalog.isOpen(entry, progress)) {
				return entry;
			}
		}
		for (final CampaignCatalog.Entry entry : catalog.getEntries()) {
			if (CampaignCatalog.isOpen(entry, progress)) {
				return entry;
			}
		}
		return null;
	}

	/** The scene behind a campaign's pages: the recorded model its Background names. */
	private static String campaignScene(final String section) {
		switch (section.toLowerCase(Locale.ROOT)) {
		case "human":
			return "roc_human";
		case "undead":
			return "roc_undead";
		case "orc":
			return "roc_orc";
		case "nightelf":
			return "roc_nightelf";
		case "nightelfex":
			return "tft_nightelf";
		case "humanex":
			return "tft_human";
		case "undeadex":
			return "tft_undead";
		case "orcex":
			return "tft_orc";
		default:
			return "roc_prologue";
		}
	}

	/** The campaign's scene, its model at 22 % from the left, darkening to the right where the list is. */
	private void sceneBehind(final FrameLayout view, final CampaignCatalog.Entry entry) {
		final String scene = entry != null ? campaignScene(entry.getSection()) : "roc_prologue";
		final float focus = 0.22f;
		final Bitmap still = this.art.poster(scene);
		final Drawable drawable = still != null ? new LauncherKit.CoverDrawable(still, focus, 0) : sceneStill();
		final Board b = this.board;
		view.addView(sceneBackdrop(drawable, scene, focus, LAYER_PAGE,
				entry != null ? entry.getData().getBackground() : "TutorialBackdrop", null,
				entry != null ? entry.getData().getBackgroundFogSettings() : null, 0, 0, 0, b.width, b.height, -1), 0,
				new FrameLayout.LayoutParams(MATCH, MATCH));
		final View shade = new View(this);
		shade.setBackground(gradient(false, new int[] { Color.TRANSPARENT, Color.TRANSPARENT,
			Color.argb(0xC7, 0, 0, 0) }, new float[] { 0, 0.4f, 1 }));
		view.addView(shade, 1, new FrameLayout.LayoutParams(MATCH, MATCH));
	}

	private float campaignListX() {
		return this.board.tablet ? 1256 : 532;
	}

	private float campaignListW() {
		return this.board.tablet ? 924 : 600;
	}

	/** On the phone the campaign pages reach the bottom of the screen. */
	private float campaignExtra() {
		return this.board.tablet ? 0 : 108;
	}

	private float campaignRowH() {
		return this.board.tablet ? 135 : 96;
	}

	/** A row of the campaign screens: the arrow button, the small gold header and the name. */
	private View campaignRow(final String header, final String name, final boolean chosen,
			final View.OnClickListener listener) {
		final boolean t = this.board.tablet;
		final float rowH = campaignRowH();
		final float w = campaignListW();
		final FrameLayout row = new FrameLayout(this);
		final float arrow = Math.min(rowH * 0.62f, 72);
		place(row, image(SkinCache.CAMPAIGN_ARROW, ImageView.ScaleType.FIT_CENTER), 0, (rowH - arrow) / 2, arrow,
				arrow);
		final float textX = arrow + (t ? 20 : 12);
		final float headerSize = t ? 30 : 20;
		final float nameSize = t ? 50 : 32;
		final float top = (rowH - headerSize - nameSize - (t ? 10 : 4)) / 2;
		final CharSequence headerText = glueLabel(header, header);
		final CharSequence nameText = glueLabel(name, name);
		place(row, text(headerText, headerSize, this.art.gold()), textX, top, w - textX - 8, -1);
		final TextView title = text(nameText, nameSize, chosen ? this.art.gold() : this.art.text());
		if (chosen) {
			title.setShadowLayer(nameSize * this.scale * 0.35f, 0, 0, this.art.gold());
		}
		place(row, title, textX, top + headerSize + (t ? 10 : 4), w - textX - 8, -1);
		onTap(row, false, listener);
		row.setContentDescription(headerText + " " + nameText);
		return row;
	}

	private void showCampaign() {
		if (!campaignsReady(PAGE_CAMPAIGN)) {
			return;
		}
		final Board b = this.board;
		final CampaignCatalog catalog = this.campaigns.catalog;
		final CCampaignProgress.Snapshot progress = this.campaigns.progress;
		final CampaignCatalog.Entry chosen = chosenCampaign();
		final FrameLayout view = shell(RAIL_CAMPAIGN, false);
		sceneBehind(view, chosen);
		final FrameLayout content = content(view, campaignExtra());
		final LinearLayout rows = scrollList(content, campaignListX(), 0, campaignListW(), b.contentH + campaignExtra());
		View chosenRow = null;
		boolean gap = true;
		int number = 0;
		for (final Object data : catalog.getDatas()) {
			if (data == null) {
				// A blank entry of the list is a gap between groups of campaigns.
				if (!gap) {
					rows.addView(new View(this), new LinearLayout.LayoutParams(1, px(campaignRowH() / 2)));
					gap = true;
				}
				continue;
			}
			final CampaignCatalog.Entry entry = catalog.getEntry(number++);
			if (!CampaignCatalog.isOpen(entry, progress)) {
				continue;
			}
			final View row = campaignRow(entry.getData().getHeader(), entry.getData().getName(), entry == chosen,
					v -> {
						if (this.preferences.getInt(KEY_CAMPAIGN, -1) != entry.getNumber()) {
							this.preferences.edit().putInt(KEY_CAMPAIGN, entry.getNumber()).remove(KEY_MISSION)
									.apply();
						}
						showMissions();
					});
			rows.addView(row, new LinearLayout.LayoutParams(MATCH, px(campaignRowH())));
			gap = false;
			if (entry == chosen) {
				chosenRow = row;
			}
		}
		if (chosenRow == null) {
			place(content, text(getString(R.string.game_no_campaigns), b.body, this.art.text2()), campaignListX(), 0,
					campaignListW(), -1);
		}
		else {
			reveal(chosenRow);
		}
		addDifficulty(content);
		setPage(view, PAGE_CAMPAIGN);
	}

	/**
	 * The difficulty box at the bottom left: the label and its menu in one line, the description under them; with
	 * the Reforged maps in the data, the campaign version's menu in a box of its own above it.
	 */
	private void addDifficulty(final FrameLayout content) {
		final boolean t = this.board.tablet;
		final boolean versions = (this.campaigns != null) && this.campaigns.catalog.hasReforgedMaps();
		final float edge = t ? 38 : 14;
		final float pad = t ? 24 : 12;
		final float inner = edge + pad;
		final float menuH = t ? 98 : 96;
		final float menuW = t ? 400 : 230;
		final float labelSize = t ? 36 : 24;
		final float w = t ? 820 : 508;
		final float h = t ? 354 : 222;
		final float y = t ? 1078 : 360;
		final FrameLayout box = new FrameLayout(this);
		box.setBackground(editFrame(edge));
		place(content, box, 0, y, w, h);
		final TextView label = text(glueLabel("SELECT_DIFFICULTY_LEVEL", "Difficulty Level:"), labelSize,
				this.art.gold());
		label.setGravity(Gravity.CENTER_VERTICAL);
		place(box, label, inner, inner, w - (2 * inner) - menuW - 12, menuH);
		final String current = this.preferences.getString(KEY_DIFFICULTY, DIFFICULTIES[0][0]);
		int selected = 0;
		for (int i = 0; i < DIFFICULTIES.length; i++) {
			if (DIFFICULTIES[i][0].equals(current)) {
				selected = i;
			}
		}
		place(box, dropdown(glueList(DIFFICULTIES, 1), selected, true, menuH, t ? 34 : 26,
				choice -> this.preferences.edit().putString(KEY_DIFFICULTY, DIFFICULTIES[choice][0]).apply()),
				w - inner - menuW, inner, menuW, menuH);
		place(box, lines(glueLabel("DIFFICULTY_TEXT", ""), t ? 26 : 19, this.art.text(), 3), inner,
				inner + menuH + (t ? 16 : 10), w - (2 * inner), -1);
		if (!versions) {
			return;
		}
		// The design has no campaign version; it keeps the difficulty box as drawn and stands above it.
		final float versionH = (2 * inner) + menuH;
		final FrameLayout versionBox = new FrameLayout(this);
		versionBox.setBackground(editFrame(edge));
		place(content, versionBox, 0, y - versionH - (t ? 24 : 8), w, versionH);
		final TextView versionLabel = text(glueLabel("CAMPAIGN_VERSION", getString(R.string.launcher_campaign_maps)),
				labelSize, this.art.gold());
		versionLabel.setGravity(Gravity.CENTER_VERTICAL);
		// "Campaign Version:" is longer than the difficulty's label; its menu is narrower.
		final float versionMenuW = t ? 320 : menuW;
		place(versionBox, versionLabel, inner, inner, w - (2 * inner) - versionMenuW - 12, menuH);
		final String profile = this.profiles.current();
		final int version = LaunchContract.CAMPAIGN_MAPS_CLASSIC.equals(this.profiles.campaignMaps(profile)) ? 1 : 0;
		place(versionBox, dropdown(glueList(CAMPAIGN_MAPS, 1), version, true, menuH, t ? 34 : 26,
				choice -> this.profiles.setCampaignMaps(profile, CAMPAIGN_MAPS[choice][0])), w - inner - versionMenuW,
				inner, versionMenuW, menuH);
	}

	private void showMissions() {
		if (!campaignsReady(PAGE_MISSIONS)) {
			return;
		}
		final CampaignCatalog.Entry entry = chosenCampaign();
		if (entry == null) {
			showCampaign();
			return;
		}
		final Board b = this.board;
		final boolean t = b.tablet;
		final CCampaignProgress.Snapshot progress = this.campaigns.progress;
		final List<CampaignMission> missions = entry.getData().getMissions();
		// The mission last played, else the furthest one open: after a victory the one it opened.
		int chosen = this.preferences.getInt(KEY_MISSION, -1);
		if ((chosen < 0) || (chosen >= missions.size()) || !CampaignCatalog.isMissionOpen(entry, chosen, progress)) {
			chosen = 0;
			for (int i = 0; i < missions.size(); i++) {
				if (CampaignCatalog.isMissionOpen(entry, i, progress)) {
					chosen = i;
				}
			}
		}
		final FrameLayout view = shell(RAIL_CAMPAIGN, false);
		sceneBehind(view, entry);
		final FrameLayout content = content(view, campaignExtra());
		final LinearLayout rows = scrollList(content, campaignListX(), 0, campaignListW(), b.contentH + campaignExtra());
		// Reforged rows: the mission's own loading picture in the campaign button frame.
		final String[] pictures = this.art.reforged() && this.art.has(SkinCache.MISSION_BORDER)
				? missionPictures(missions)
				: null;
		if (pictures != null) {
			rows.setPadding(0, px(t ? 16 : 8), 0, 0);
		}
		View chosenRow = null;
		for (int i = 0; i < missions.size(); i++) {
			if (!CampaignCatalog.isMissionOpen(entry, i, progress)) {
				continue;
			}
			final int mission = i;
			final View.OnClickListener launch = v -> launchMission(entry, mission, PAGE_MISSIONS);
			final View row;
			final float rowH;
			if (pictures != null) {
				final boolean won = i < (missions.size() - 1) ? CampaignCatalog.isMissionOpen(entry, i + 1, progress)
						: progress.isEndingCinematicAvailable(entry.getNumber());
				row = missionRow(missions.get(i).getHeader(), missions.get(i).getMissionName(), i == chosen, won,
						pictures[i], launch);
				rowH = missionRowH();
			}
			else {
				row = campaignRow(missions.get(i).getHeader(), missions.get(i).getMissionName(), i == chosen, launch);
				rowH = campaignRowH();
			}
			rows.addView(row, new LinearLayout.LayoutParams(MATCH, px(rowH)));
			if (i == chosen) {
				chosenRow = row;
			}
		}
		if (chosenRow != null) {
			reveal(chosenRow);
		}
		// The campaign's header and name centred under the scene, left of the list.
		final float nameSize = t ? 72 : 40;
		final float headerSize = t ? 40 : 24;
		final float nameY = t ? b.contentH - 20 - (nameSize * 1.2f) : 380;
		final float nameW = campaignListX() - (t ? 60 : 24);
		final TextView header = text(glueLabel(entry.getData().getHeader(), ""), headerSize, this.art.gold());
		header.setGravity(Gravity.CENTER_HORIZONTAL);
		place(content, header, 0, t ? b.contentH - 110 - (headerSize * 1.2f) : nameY - headerSize - 8, nameW, -1);
		final TextView name = text(glueLabel(entry.getData().getName(), ""), nameSize, this.art.text());
		name.setGravity(Gravity.CENTER_HORIZONTAL);
		place(content, name, 0, nameY, nameW, -1);
		action(view, 0, glueLabel("BACK", getString(R.string.launcher_back)), v -> showCampaign());
		setPage(view, PAGE_MISSIONS);
	}

	private float missionRowH() {
		return this.board.tablet ? 146 : 102;
	}

	/**
	 * A Reforged mission row: the mission's picture in the campaign button frame (the hover frame when chosen), the
	 * check mark once won, then the header and the name.
	 */
	private View missionRow(final String header, final String name, final boolean chosen, final boolean won,
			final String picture, final View.OnClickListener listener) {
		final boolean t = this.board.tablet;
		final float rowH = missionRowH();
		final float gap = t ? 10 : 6;
		final float frame = rowH - gap - 8;
		final float x = t ? 16 : 8;
		final float y = 4;
		final FrameLayout row = new FrameLayout(this);
		final ImageView art = new ImageView(this);
		art.setScaleType(ImageView.ScaleType.FIT_XY);
		final Bitmap full = picture != null ? this.art.bitmap(picture) : null;
		if (full != null) {
			// The pictures are wide; the frame shows a square of them a little left of the middle.
			final int side = Math.min(full.getWidth(), full.getHeight());
			final int left = Math.round((full.getWidth() - side) * 0.18f);
			art.setImageBitmap(Bitmap.createBitmap(full, left, 0, side, side));
		}
		else {
			art.setBackgroundColor(Color.rgb(0x11, 0x11, 0x11));
		}
		place(row, art, x + (frame * 0.07f), y + (frame * 0.07f), frame * 0.86f, frame * 0.86f);
		if (full == null) {
			place(row, image(SkinCache.CAMPAIGN_ARROW, ImageView.ScaleType.FIT_CENTER), x + (frame * 0.28f),
					y + (frame * 0.28f), frame * 0.44f, frame * 0.44f);
		}
		place(row, image(chosen ? SkinCache.MISSION_BORDER_HOVER : SkinCache.MISSION_BORDER,
				ImageView.ScaleType.FIT_XY), x, y, frame, frame);
		if (won) {
			place(row, image(SkinCache.MISSION_CHECK, ImageView.ScaleType.FIT_CENTER), x + (frame * 0.62f),
					frame * 0.62f, frame * 0.42f, frame * 0.42f);
		}
		final float textX = x + frame + (t ? 24 : 14);
		final float w = campaignListW() - textX - 8;
		final float rh = rowH - gap;
		final CharSequence headerText = glueLabel(header, header);
		final CharSequence nameText = glueLabel(name, name);
		place(row, text(headerText, rh * 0.2f, chosen ? this.art.text() : this.art.gold()), textX, y + (rh * 0.16f),
				w, -1);
		final TextView title = text(nameText, rh * 0.3f, chosen ? this.art.gold() : this.art.text());
		if (chosen) {
			title.setShadowLayer(rh * 0.3f * this.scale * 0.4f, 0, 0, this.art.gold());
		}
		place(row, title, textX, y + (rh * 0.42f), w, -1);
		onTap(row, false, listener);
		row.setContentDescription(headerText + " " + nameText);
		return row;
	}

	/**
	 * The cached picture of each mission, found by the words of its name in the picture's file name, within the
	 * campaign folder most of the missions match; null for a mission without one (the cinematics).
	 */
	private String[] missionPictures(final List<CampaignMission> missions) {
		final String[] found = new String[missions.size()];
		final Map<String, String> pictures = this.art.missionPictures();
		if (pictures.isEmpty()) {
			return found;
		}
		final List<List<String>> names = new ArrayList<>();
		for (final CampaignMission mission : missions) {
			names.add(words(glueLabel(mission.getMissionName(), mission.getMissionName()).toString()));
		}
		final Map<String, Integer> votes = new HashMap<>();
		for (final List<String> name : names) {
			final String best = bestPicture(name, pictures.keySet(), null);
			if (best != null) {
				votes.merge(best.substring(0, best.lastIndexOf('\\') + 1), 1, Integer::sum);
			}
		}
		String folder = null;
		for (final Map.Entry<String, Integer> vote : votes.entrySet()) {
			if ((folder == null) || (vote.getValue() > votes.get(folder))) {
				folder = vote.getKey();
			}
		}
		if (folder == null) {
			return found;
		}
		for (int i = 0; i < found.length; i++) {
			final String best = bestPicture(names.get(i), pictures.keySet(), folder);
			found[i] = best == null ? null : pictures.get(best);
		}
		return found;
	}

	private static String bestPicture(final List<String> name, final Collection<String> paths, final String folder) {
		String best = null;
		int bestScore = 0;
		for (final String path : paths) {
			if ((folder != null) && !(path.startsWith(folder) && (path.indexOf('\\', folder.length()) < 0))) {
				continue;
			}
			final List<String> file = words(path.substring(path.lastIndexOf('\\') + 1));
			int score = 0;
			for (final String word : name) {
				if (file.contains(word)) {
					score++;
				}
			}
			if (score > bestScore) {
				best = path;
				bestScore = score;
			}
		}
		return best;
	}

	private static List<String> words(final String text) {
		final List<String> words = new ArrayList<>();
		for (final String word : text.toLowerCase(Locale.ROOT).replace(".jpg", "").split("[^a-z]+")) {
			if ((word.length() >= 3) && !"the".equals(word) && !"and".equals(word)) {
				words.add(word);
			}
		}
		return words;
	}

	private void launchMission(final CampaignCatalog.Entry entry, final int mission, final int returnPage) {
		this.preferences.edit().putInt(KEY_CAMPAIGN, entry.getNumber()).putInt(KEY_MISSION, mission).apply();
		final Intent intent = new Intent();
		intent.putExtra(LaunchContract.EXTRA_CAMPAIGN, entry.getNumber());
		intent.putExtra(LaunchContract.EXTRA_MISSION, mission);
		final String maps = this.profiles.campaignMaps(this.profiles.current());
		intent.putExtra(LaunchContract.EXTRA_CAMPAIGN_MAPS, maps);
		final String mapFile = this.campaigns.catalog.missionMapFilename(entry, mission,
				LaunchContract.CAMPAIGN_MAPS_REFORGED.equals(maps));
		if (mapFile != null) {
			intent.putExtra(LaunchContract.EXTRA_MAP_FILE, mapFile);
		}
		intent.putExtra(LaunchContract.EXTRA_DIFFICULTY,
				this.preferences.getString(KEY_DIFFICULTY, DIFFICULTIES[0][0]));
		launchGame(intent, returnPage);
	}

	// ---------------------------------------------------------------- Custom Game

	private static final int SLOT_YOU = -1;
	private static final int SLOT_HIDDEN = -2;
	private FrameLayout customRight;
	private LauncherKit.GlueButton customStart;
	private View customAdvanced;

	private String chosenMap() {
		final String path = this.preferences.getString(KEY_MAP, null);
		for (final GameCatalog.MapEntry entry : this.maps) {
			if (entry.path.equals(path)) {
				return path;
			}
		}
		return this.maps.isEmpty() ? null : this.maps.get(0).path;
	}

	private boolean[] advancedOptions() {
		final List<String> on = Arrays
				.asList(this.preferences.getString(KEY_OPTIONS, OPTION_NAMES[0]).split(","));
		final boolean[] options = new boolean[OPTION_NAMES.length];
		for (int i = 0; i < options.length; i++) {
			options[i] = on.contains(OPTION_NAMES[i]);
		}
		return options;
	}

	private void showCustom() {
		if (this.maps == null) {
			final int generation = loadingPage(RAIL_CUSTOM, PAGE_CUSTOM);
			this.catalog.maps(found -> {
				if (generation != this.generation) {
					return;
				}
				if (found == null) {
					unreadable();
					return;
				}
				this.maps = found;
				showCustom();
			});
			return;
		}
		final Board b = this.board;
		final boolean t = b.tablet;
		this.advanced = advancedOptions();
		final FrameLayout view = shell(RAIL_CUSTOM);
		final FrameLayout content = content(view, 0);
		final float listW = t ? 620 : 420;
		final float rowH = t ? 112 : 96;
		final float frame = b.pageInset;
		final FrameLayout listPanel = framed(content, 0, 0, listW, b.contentH);
		final float rowW = listW - (2 * frame);
		final LinearLayout rows = scrollList(listPanel, frame, frame, rowW, b.contentH - (2 * frame));
		final String chosen = chosenMap();
		final String folder = shownMapFolder(chosen);
		final String prefix = folder.isEmpty() ? "" : folder + "/";
		// As in the original list: the way up, the folders, then the maps of the folder shown.
		if (!folder.isEmpty()) {
			final int up = folder.lastIndexOf('/');
			rows.addView(folderRow("..", rowW, rowH, up < 0 ? "" : folder.substring(0, up)),
					new LinearLayout.LayoutParams(MATCH, px(rowH)));
		}
		final TreeMap<String, String> folders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (final GameCatalog.MapEntry entry : this.maps) {
			final String mapFolder = mapFolder(entry.path);
			if ((mapFolder.length() > prefix.length()) && mapFolder.regionMatches(true, 0, prefix, 0, prefix.length())) {
				final int end = mapFolder.indexOf('/', prefix.length());
				final String child = end < 0 ? mapFolder : mapFolder.substring(0, end);
				folders.put(child, child);
			}
		}
		for (final String child : folders.values()) {
			rows.addView(folderRow(child.substring(prefix.length()), rowW, rowH, child),
					new LinearLayout.LayoutParams(MATCH, px(rowH)));
		}
		final List<View> mapRows = new ArrayList<>();
		View chosenRow = null;
		for (final GameCatalog.MapEntry entry : this.maps) {
			if (!mapFolder(entry.path).equalsIgnoreCase(folder)) {
				continue;
			}
			final View row = mapRow(entry, rowW, rowH);
			onTap(row, false, v -> {
				for (final View other : mapRows) {
					markRow(other, other == v);
				}
				this.preferences.edit().putString(KEY_MAP, entry.path).apply();
				loadMapDetails(entry.path);
			});
			markRow(row, entry.path.equals(chosen));
			if (entry.path.equals(chosen)) {
				chosenRow = row;
			}
			mapRows.add(row);
			rows.addView(row, new LinearLayout.LayoutParams(MATCH, px(rowH)));
		}
		if (chosenRow != null) {
			reveal(chosenRow);
		}
		else if (this.maps.isEmpty()) {
			place(listPanel, text(getString(R.string.game_no_maps), b.body, this.art.text2()), frame, frame, rowW, -1);
		}
		final float rightX = t ? 656 : 430;
		this.customRight = new FrameLayout(this);
		place(content, this.customRight, rightX, 0, b.contentW - rightX, b.contentH);
		action(view, 0, glueLabel("CANCEL", "Cancel"), v -> showHome());
		this.customAdvanced = t ? null
				: action(view, 1, getString(R.string.game_advanced), v -> showAdvancedSheet());
		if (this.customAdvanced != null) {
			this.customAdvanced.setEnabled(false);
		}
		this.customStart = action(view, 2, glueLabel("KEY_START_GAME", "Start Game"), v -> startCustom());
		this.customStart.setEnabled(false);
		setPage(view, PAGE_CUSTOM);
		if (chosen != null) {
			loadMapDetails(chosen);
		}
	}

	/** The folder of a map below the maps folder, with forward slashes; empty for a map right in it. */
	private static String mapFolder(final String path) {
		String folder = path.replace('\\', '/');
		final int slash = folder.lastIndexOf('/');
		folder = slash < 0 ? "" : folder.substring(0, slash);
		if (folder.regionMatches(true, 0, "maps", 0, 4) && ((folder.length() == 4) || (folder.charAt(4) == '/'))) {
			folder = folder.substring(Math.min(folder.length(), 5));
		}
		return folder;
	}

	/** The folder the list shows: the one opened last while it still has maps, else the chosen map's. */
	private String shownMapFolder(final String chosen) {
		final String kept = this.preferences.getString(KEY_MAP_FOLDER, null);
		final String wanted = kept != null ? kept : chosen != null ? mapFolder(chosen) : "";
		if (wanted.isEmpty()) {
			return "";
		}
		for (final GameCatalog.MapEntry entry : this.maps) {
			final String folder = mapFolder(entry.path);
			if (folder.equalsIgnoreCase(wanted) || folder.regionMatches(true, 0, wanted + "/", 0, wanted.length() + 1)) {
				return wanted;
			}
		}
		return "";
	}

	/** The game's own folders as the original list names them; a lower-cased one of the data gets a capital. */
	private static String folderName(final String name) {
		switch (name) {
		case "frozenthrone":
			return "FrozenThrone";
		case "scenario":
			return "Scenario";
		case "download":
			return "Download";
		default:
			return "..".equals(name) || !name.equals(name.toLowerCase(Locale.ROOT)) ? name
					: Character.toUpperCase(name.charAt(0)) + name.substring(1);
		}
	}

	/** A folder of the list (icon-folder and its name); tapping it shows its maps. */
	private View folderRow(final String name, final float w, final float rowH, final String opens) {
		final boolean t = this.board.tablet;
		final LinearLayout row = new LinearLayout(this);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setPadding(px(6), 0, px(8), 0);
		final float line = Math.min(rowH * 0.86f, 84);
		final float icon = line * 0.82f;
		row.addView(image(SkinCache.ICON_FOLDER, ImageView.ScaleType.FIT_CENTER),
				new LinearLayout.LayoutParams(px(icon), px(icon)));
		final String shown = folderName(name);
		final TextView title = text(shown, line * 0.56f, this.art.text());
		final LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0,
				ViewGroup.LayoutParams.WRAP_CONTENT, 1);
		titleParams.leftMargin = px(18);
		row.addView(title, titleParams);
		row.setContentDescription(shown);
		onTap(row, false, v -> {
			this.preferences.edit().putString(KEY_MAP_FOLDER, opens).apply();
			showCustom();
		});
		return row;
	}

	/** A map of the list: its kind of map, the number of players and the name, cut to the list's width. */
	private View mapRow(final GameCatalog.MapEntry entry, final float w, final float rowH) {
		final boolean t = this.board.tablet;
		final LinearLayout row = new LinearLayout(this);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setPadding(px(6), 0, px(8), 0);
		final float line = Math.min(rowH * 0.86f, 84);
		final float icon = line * 0.82f;
		// The number of players stands in the map icon, as the original list draws it.
		final FrameLayout mark = new FrameLayout(this);
		mark.addView(image(entry.melee ? SkinCache.ICON_MELEE : SkinCache.ICON_UMS, ImageView.ScaleType.FIT_CENTER),
				new FrameLayout.LayoutParams(MATCH, MATCH));
		final TextView players = text(Integer.toString(entry.players), icon * 0.5f, this.art.gold());
		players.setGravity(Gravity.CENTER);
		mark.addView(players, new FrameLayout.LayoutParams(MATCH, MATCH));
		row.addView(mark, new LinearLayout.LayoutParams(px(icon), px(icon)));
		final CharSequence name = LauncherKit.glueText(entry.label);
		final TextView title = text(name, line * 0.56f, this.art.text());
		final LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0,
				ViewGroup.LayoutParams.WRAP_CONTENT, 1);
		titleParams.leftMargin = px(18);
		row.addView(title, titleParams);
		row.setContentDescription(name);
		return row;
	}

	private void loadMapDetails(final String path) {
		this.mapDetails = null;
		this.customStart.setEnabled(false);
		if (this.customAdvanced != null) {
			this.customAdvanced.setEnabled(false);
		}
		this.customRight.removeAllViews();
		place(this.customRight, text(getString(R.string.game_loading_list), this.board.body, this.art.text2()), 0, 0,
				-1, -1);
		final int generation = this.generation;
		this.catalog.mapDetails(path, details -> {
			if ((generation != this.generation) || !path.equals(chosenMap())) {
				return;
			}
			this.customRight.removeAllViews();
			if (details == null) {
				final TextView failed = text(getString(R.string.game_unreadable), this.board.body, ERROR);
				place(this.customRight, failed, 0, 0, -1, -1);
				return;
			}
			this.mapDetails = details;
			chooseSlots(details);
			fillMapDetails(details);
			this.customStart.setEnabled(true);
			if (this.customAdvanced != null) {
				this.customAdvanced.setEnabled(true);
			}
		});
	}

	/** Fixed player settings hold only with custom forces; melee maps carry the flag without them. */
	private static boolean fixedSlots(final GameCatalog.MapDetails details) {
		return details.fixedSettings && details.customForces;
	}

	/**
	 * The lobby's first setup: the first user slot is the player, the computer slots the map plays (all of them with
	 * fixed settings), and when the map gives no computer, the next user slot is Computer (Normal) as in the original
	 * single player lobby. The others are open.
	 */
	private void chooseSlots(final GameCatalog.MapDetails details) {
		final int count = details.slots.size();
		this.slotTypes = new int[count];
		this.slotRaces = new int[count];
		this.slotTeams = new int[count];
		this.slotColors = new int[count];
		Arrays.fill(this.slotTeams, -1);
		Arrays.fill(this.slotColors, -1);
		boolean you = false;
		boolean computer = false;
		for (int i = 0; i < count; i++) {
			final GameCatalog.Slot slot = details.slots.get(i);
			this.slotRaces[i] = (slot.race >= 0) && (slot.race < RACES.length) ? slot.race : 0;
			if (slot.type == 1) {
				this.slotTypes[i] = you ? SLOT_OPEN : SLOT_YOU;
				you = true;
			}
			else if (slot.type == 2) {
				this.slotTypes[i] = !computer || details.fixedSettings ? SLOT_COMPUTER_NORMAL : SLOT_OPEN;
				computer = true;
			}
			else {
				this.slotTypes[i] = SLOT_HIDDEN;
			}
		}
		if (!computer && !fixedSlots(details)) {
			for (int i = 0; i < count; i++) {
				if ((details.slots.get(i).type == 1) && (this.slotTypes[i] == SLOT_OPEN)) {
					this.slotTypes[i] = SLOT_COMPUTER_NORMAL;
					break;
				}
			}
		}
	}

	private void fillMapDetails(final GameCatalog.MapDetails details) {
		final Board b = this.board;
		final boolean t = b.tablet;
		final FrameLayout right = this.customRight;
		final float frame = b.pageInset;
		final float mapW = t ? 1008 : 738;
		final float mapH = t ? 620 : 120;
		final FrameLayout map = framed(right, 0, 0, mapW, mapH);
		final float picture = t ? 400 : mapH - (2 * frame);
		final ImageView minimap = new ImageView(this);
		minimap.setScaleType(ImageView.ScaleType.FIT_XY);
		minimap.setBackground(minimapFrame(t ? 8 : 4));
		final int rim = px(t ? 8 : 4);
		minimap.setPadding(rim, rim, rim, rim);
		minimap.setImageBitmap(details.minimap != null ? details.minimap : this.art.bitmap(SkinCache.MINIMAP_UNKNOWN));
		minimap.setContentDescription(LauncherKit.glueText(details.name).toString());
		place(map, minimap, frame, frame, picture, picture);
		final float factsX = frame + picture + (t ? 36 : 14);
		final float factsW = mapW - factsX - frame;
		final CharSequence suggested = glueLabel("COLON_SUGGESTED_PLAYERS", "Suggested Players:");
		final CharSequence size = glueLabel("COLON_MAP_SIZE", "Map Size:");
		final CharSequence tileset = glueLabel("COLON_TILESET", "Tileset:");
		if (t) {
			place(map, text(mapTitle(details), 42, this.art.text()), factsX, frame, factsW, -1);
			final CharSequence[][] facts = { { suggested, LauncherKit.glueText(details.suggested) },
				{ size, details.size }, { tileset, details.tileset } };
			final float labelW = 270;
			for (int i = 0; i < facts.length; i++) {
				final float y = frame + 76 + (i * 46);
				place(map, text(facts[i][0], 30, this.art.gold()), factsX, y, labelW, -1);
				place(map, text(facts[i][1], 30, this.art.text()), factsX + labelW + 10, y, factsW - labelW - 10, -1);
			}
			final float descriptionY = frame + 76 + (3 * 46) + 20;
			final TextView description = lines(LauncherKit.glueText(details.description), 27, this.art.text2(), 6);
			place(map, description, factsX, descriptionY, factsW, mapH - frame - descriptionY);
			addAdvanced(right, 1044, 0, 500, 620);
		}
		else {
			place(map, text(mapTitle(details), 28, this.art.text()), factsX, frame - 2, factsW, -1);
			final String line = LauncherKit.glueText(details.suggested) + " · " + details.size + " · " + details.tileset;
			place(map, text(line, 20, this.art.text()), factsX, frame + 44, factsW, -1);
		}
		final float slotsY = t ? 656 : 130;
		final float slotsW = t ? 1544 : 738;
		final float slotsH = b.contentH - slotsY;
		final FrameLayout slots = framed(right, 0, slotsY, slotsW, slotsH);
		final float rowW = slotsW - (2 * frame);
		final float headerH = t ? 56 : 0;
		if (t) {
			final float[] columns = slotColumns(rowW);
			final String[] headers = { glue("PLAYER", "Player"), glue("COLON_RACE", "Race"), glue("TEAM", "Team"),
				glue("COLOR", "Color") };
			for (int i = 0; i < headers.length; i++) {
				place(slots, text(LauncherKit.glueText(headers[i]).toString().replace(":", ""), 28, this.art.gold()),
						frame + columns[2 * i] + 8, frame - 4, columns[(2 * i) + 1], -1);
			}
		}
		final LinearLayout rows = scrollList(slots, frame, frame + headerH, rowW, slotsH - (2 * frame) - headerH);
		for (int i = 0; i < details.slots.size(); i++) {
			if (this.slotTypes[i] != SLOT_HIDDEN) {
				rows.addView(slotRow(details, i, rowW), new LinearLayout.LayoutParams(MATCH, px(slotRowH())));
			}
		}
	}

	/** The map's name after its number of players, as the original list shows it. */
	private CharSequence mapTitle(final GameCatalog.MapDetails details) {
		final android.text.SpannableStringBuilder title = new android.text.SpannableStringBuilder();
		int players = 0;
		for (final GameCatalog.Slot slot : details.slots) {
			if ((slot.type == 1) || (slot.type == 2)) {
				players++;
			}
		}
		if (players > 0) {
			title.append(Integer.toString(players), new android.text.style.ForegroundColorSpan(this.art.gold()),
					android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE).append(' ');
		}
		return title.append(LauncherKit.glueText(details.name));
	}

	/** The minimap's frame: a black border with a grey line round it. */
	private Drawable minimapFrame(final float border) {
		final GradientDrawable frame = new GradientDrawable();
		frame.setColor(Color.BLACK);
		frame.setStroke(Math.max(1, px(3)), Color.rgb(0x55, 0x55, 0x55));
		return frame;
	}

	private float slotRowH() {
		return this.board.tablet ? 154 : 96;
	}

	/** Where the slot columns start and how wide they are: player, race, team and colour, as 40 : 24 : 15 : 18. */
	private float[] slotColumns(final float w) {
		final boolean t = this.board.tablet;
		final float gap = t ? 16 : 6;
		final float player = w * (t ? 0.40f : 0.38f);
		final float race = w * (t ? 0.24f : 0.28f);
		final float team = w * 0.15f;
		final float color = w - player - race - team - (3 * gap);
		return new float[] { 0, player, player + gap, race, player + race + (2 * gap), team,
			player + race + team + (3 * gap), color };
	}

	/** A player slot: who plays it, the race, the team and the colour; fixed settings cannot change. */
	private View slotRow(final GameCatalog.MapDetails details, final int index, final float w) {
		final boolean t = this.board.tablet;
		final GameCatalog.Slot slot = details.slots.get(index);
		final float rowH = slotRowH();
		final float menuH = t ? 130 : 88;
		final float textSize = t ? 38 : 22;
		final float[] columns = slotColumns(w);
		final FrameLayout row = new FrameLayout(this);
		final float menuY = t ? 0 : (rowH - menuH) / 2;
		final boolean editable = !fixedSlots(details);
		if (this.slotTypes[index] == SLOT_YOU) {
			// The player's own slot shows the profile's name; it cannot be changed.
			place(row, dropdown(new String[] { GameCatalog.currentProfile(this) }, 0, true, menuH, textSize, null),
					columns[0], menuY, columns[1], menuH);
		}
		else {
			place(row, dropdown(glueList(SLOT_TYPES, 1), this.slotTypes[index], editable, menuH, textSize,
					choice -> this.slotTypes[index] = choice), columns[0], menuY, columns[1], menuH);
		}
		place(row, dropdown(glueList(RACES, 0), this.slotRaces[index], editable, menuH, textSize,
				choice -> this.slotRaces[index] = choice), columns[2], menuY, columns[3], menuH);
		final String[] teams = teamNames(details);
		final int team = this.slotTeams[index] >= 0 ? this.slotTeams[index] : defaultTeam(details, index);
		place(row, dropdown(teams, Math.min(team, teams.length - 1), editable && !details.fixedSettings, menuH,
				textSize, choice -> this.slotTeams[index] = choice), columns[4], menuY, columns[5], menuH);
		final String[] colors = glueList(COLOR_NAMES, 0);
		final int color = this.slotColors[index] >= 0 ? this.slotColors[index]
				: Math.abs(slot.id) % PLAYER_COLORS.length;
		place(row, dropdown(colors, color, editable, menuH, textSize, choice -> this.slotColors[index] = choice,
				PLAYER_COLORS), columns[6], menuY, columns[7], menuH);
		return row;
	}

	/** The teams a slot can join: the map's forces when it sets them, else one team per slot of the lobby. */
	private String[] teamNames(final GameCatalog.MapDetails details) {
		if (details.customForces && !details.forces.isEmpty()) {
			final String[] names = new String[details.forces.size()];
			for (int i = 0; i < names.length; i++) {
				names[i] = LauncherKit.glueText(details.forces.get(i)).toString();
			}
			return names;
		}
		final String[] names = new String[details.slots.size()];
		for (int i = 0; i < names.length; i++) {
			names[i] = Integer.toString(i + 1);
		}
		return names;
	}

	private static int defaultTeam(final GameCatalog.MapDetails details, final int index) {
		final GameCatalog.Slot slot = details.slots.get(index);
		if (details.customForces && (slot.force >= 0) && (slot.force < details.forces.size())) {
			return slot.force;
		}
		return index;
	}

	private String teamName(final int number) {
		final String format = glue("TEAM_FORMAT", "Team %d");
		try {
			return String.format(Locale.ROOT, format.replace("%s", "%d"), number);
		}
		catch (final RuntimeException e) {
			return "Team " + number;
		}
	}
	private CharSequence[] optionLabels() {
		return new CharSequence[] { glueLabel("LOCK_TEAMS", "Lock Teams"),
			glueLabel("FULL_SHARED_UNIT_CONTROL", getString(R.string.game_shared_control)),
			glueLabel("RANDOM_RACES", "Random Races"), glueLabel("RANDOM_HERO", "Random Hero") };
	}

	/**
	 * The tablet's Advanced Options beside the map: the title, a check box per option and the Observers menu, which
	 * a single player game keeps at none as the original does.
	 */
	private void addAdvanced(final FrameLayout parent, final float x, final float y, final float w, final float h) {
		final float frame = this.board.pageInset;
		final FrameLayout panel = framed(parent, x, y, w, h);
		place(panel, text(glueLabel("ADVANCED_OPTIONS", "Advanced Options"), 34, this.art.gold()), frame, frame - 4,
				w - (2 * frame), -1);
		final CharSequence[] labels = optionLabels();
		final float step = 82;
		// Check boxes of 56 px every 82 px, as the design has them; the row is the larger touch target.
		final float top = frame + 37;
		for (int i = 0; i < labels.length; i++) {
			final int option = OPTION_SHOWN[i];
			place(panel, checkLine(labels[i], this.advanced[option], 94, 27, on -> setOption(option, on)), frame,
					top + (i * step), w - (2 * frame), 94);
		}
		final float observersY = top + 19 + (labels.length * step) + 6;
		place(panel, text(glueLabel("COLON_OBSERVERS", "Observers:"), 27, this.art.gold()), frame, observersY,
				w - (2 * frame), -1);
		place(panel, dropdown(new String[] { glue("NO_OBSERVERS", "None") }, 0, true, 80, 33, null), frame,
				observersY + 40, w - (2 * frame), 80);
	}

	private void setOption(final int option, final boolean on) {
		this.advanced[option] = on;
		final StringBuilder names = new StringBuilder();
		for (int i = 0; i < OPTION_NAMES.length; i++) {
			if (this.advanced[i]) {
				names.append(names.length() > 0 ? "," : "").append(OPTION_NAMES[i]);
			}
		}
		this.preferences.edit().putString(KEY_OPTIONS, names.toString()).apply();
	}

	/** The phone's Advanced Options: a sheet over the page, two columns of full height rows. */
	private void showAdvancedSheet() {
		final FrameLayout box = dialogBox(1120, 600);
		if (box == null) {
			return;
		}
		final float edge = 32;
		place(box, text(glueLabel("ADVANCED_OPTIONS", "Advanced Options"), 30, this.art.gold()), edge + 16, edge + 8,
				1120 - (2 * edge) - 32, -1);
		final CharSequence[] labels = optionLabels();
		final float columnW = (1120 - (2 * edge) - 48) / 2;
		for (int i = 0; i < labels.length; i++) {
			final int option = i;
			place(box, checkLine(labels[i], this.advanced[OPTION_SHOWN[i]], 96, 24,
					on -> setOption(OPTION_SHOWN[option], on)),
					edge + 16 + ((i % 2) * (columnW + 16)), edge + 70 + ((i / 2) * 96), columnW, 96);
		}
		place(box, button(glueLabel("OK", getString(R.string.game_ok)), 96, 30, v -> closeDialog()), 1120 - edge - 320,
				600 - edge - 96, 320, 96);
	}

	private void startCustom() {
		final GameCatalog.MapDetails details = this.mapDetails;
		if (details == null) {
			return;
		}
		final StringBuilder slots = new StringBuilder();
		if (!fixedSlots(details)) {
			for (int i = 0; i < details.slots.size(); i++) {
				if (this.slotTypes[i] == SLOT_HIDDEN) {
					continue;
				}
				final String type = this.slotTypes[i] == SLOT_YOU ? "USER" : SLOT_TYPES[this.slotTypes[i]][0];
				slots.append(details.slots.get(i).id).append(':').append(type).append(':').append(this.slotRaces[i])
						.append(':').append(this.slotTeams[i]).append(':').append(this.slotColors[i]).append(';');
			}
		}
		final StringBuilder options = new StringBuilder();
		for (int i = 0; i < OPTION_NAMES.length; i++) {
			if (this.advanced[i] || (i == OPTION_TEAMS_TOGETHER)) {
				options.append(options.length() > 0 ? "," : "").append(OPTION_NAMES[i]);
			}
		}
		final Intent intent = new Intent();
		intent.putExtra(LaunchContract.EXTRA_CUSTOM_MAP, details.path);
		intent.putExtra(LaunchContract.EXTRA_MAP_FILE, details.path);
		intent.putExtra(LaunchContract.EXTRA_SLOTS, slots.toString());
		intent.putExtra(LaunchContract.EXTRA_OPTIONS, options.toString());
		int race = 0;
		for (int i = 0; i < details.slots.size(); i++) {
			if (this.slotTypes[i] == SLOT_YOU) {
				race = this.slotRaces[i];
			}
		}
		this.preferences.edit().putInt(KEY_LAUNCH_RACE, race).apply();
		launchGame(intent, PAGE_CUSTOM);
	}

	// ---------------------------------------------------------------- Load Game

	private List<GameCatalog.SaveEntry> saves;
	private FrameLayout saveInfo;
	private LauncherKit.GlueButton loadButton;
	private LauncherKit.GlueButton deleteButton;

	private void showLoad() {
		final Board b = this.board;
		final boolean t = b.tablet;
		final FrameLayout view = shell(RAIL_LOAD);
		final FrameLayout content = content(view, 0);
		final float listW = t ? 1300 : 700;
		final FrameLayout listPanel = framed(content, 0, 0, listW, b.contentH);
		final float infoX = t ? 1340 : 716;
		this.saveInfo = framed(content, infoX, 0, b.contentW - infoX, b.contentH);
		final float frame = b.pageInset;
		final TextView note = text(getString(R.string.game_loading_list), b.body, this.art.text2());
		place(listPanel, note, frame, frame, listW - (2 * frame), -1);
		action(view, 0, glueLabel("BACK", getString(R.string.launcher_back)), v -> showHome());
		this.deleteButton = action(view, 1, glueLabel("DELETE", "Delete"), v -> askDelete());
		this.deleteButton.setTone(LauncherKit.GlueButton.TONE_DANGER);
		this.loadButton = action(view, 2, glueLabel("KEY_LOAD_GAME", "Load Game"), v -> startLoad());
		this.deleteButton.setEnabled(false);
		this.loadButton.setEnabled(false);
		setPage(view, PAGE_LOAD);
		final int generation = this.generation;
		this.catalog.saves(found -> {
			if (generation != this.generation) {
				return;
			}
			if (found == null) {
				note.setTextColor(ERROR);
				note.setText(R.string.game_unreadable);
				return;
			}
			this.saves = found;
			if (found.isEmpty()) {
				note.setText(R.string.game_no_saves);
				return;
			}
			listPanel.removeView(note);
			fillSaves(listPanel, listW - (2 * frame));
		});
	}

	private GameCatalog.SaveEntry chosenSave() {
		final String name = this.preferences.getString(KEY_SAVE, null);
		for (final GameCatalog.SaveEntry entry : this.saves) {
			if (entry.name.equals(name)) {
				return entry;
			}
		}
		return this.saves.isEmpty() ? null : this.saves.get(0);
	}

	private void fillSaves(final FrameLayout listPanel, final float rowW) {
		final Board b = this.board;
		final boolean t = b.tablet;
		final float frame = b.pageInset;
		final float rowH = t ? 123 : 96;
		final LinearLayout rows = scrollList(listPanel, frame, frame + (t ? 8 : 4), rowW,
				b.contentH - (2 * frame) - (t ? 8 : 4));
		final GameCatalog.SaveEntry chosen = chosenSave();
		final List<View> saveRows = new ArrayList<>();
		final DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
		View chosenRow = null;
		for (final GameCatalog.SaveEntry entry : this.saves) {
			final boolean loadable = entry.loadability == SavedGame.Loadability.LOADABLE;
			final LinearLayout row = new LinearLayout(this);
			row.setGravity(Gravity.CENTER_VERTICAL);
			row.setPadding(px(t ? 16 : 10), 0, px(t ? 24 : 8), 0);
			final float icon = rowH * 0.7f;
			final ImageView file = image(SkinCache.ICON_FILE, ImageView.ScaleType.FIT_CENTER);
			file.setAlpha(loadable ? 1f : 0.4f);
			row.addView(file, new LinearLayout.LayoutParams(px(icon), px(icon)));
			final TextView name = text(entry.name, rowH * (t ? 0.42f : 0.3f),
					loadable ? this.art.text() : LauncherKit.DISABLED);
			final LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0,
					ViewGroup.LayoutParams.WRAP_CONTENT, 1);
			nameParams.leftMargin = px(16);
			row.addView(name, nameParams);
			final TextView date = text(format.format(new Date(entry.saved)), rowH * (t ? 0.34f : 0.2f),
					loadable ? this.art.text2() : LauncherKit.DISABLED);
			final LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			dateParams.leftMargin = px(12);
			row.addView(date, dateParams);
			row.setContentDescription(entry.name);
			onTap(row, false, v -> {
				for (final View other : saveRows) {
					markRow(other, other == v);
				}
				this.preferences.edit().putString(KEY_SAVE, entry.name).apply();
				showSaveInfo(entry);
			});
			markRow(row, entry == chosen);
			if (entry == chosen) {
				chosenRow = row;
			}
			saveRows.add(row);
			rows.addView(row, new LinearLayout.LayoutParams(MATCH, px(rowH)));
		}
		if (chosenRow != null) {
			reveal(chosenRow);
			showSaveInfo(chosen);
		}
	}

	private String loadabilityText(final SavedGame.Loadability loadability) {
		switch (loadability) {
		case DAMAGED:
			return getString(R.string.game_save_damaged);
		case OTHER_VERSION:
			return getString(R.string.game_save_other_version);
		case OTHER_SCRIPTS:
			return getString(R.string.game_save_other_scripts);
		default:
			return "";
		}
	}

	private static String elapsed(final float seconds) {
		final int total = Math.max(0, (int) seconds);
		return String.format(Locale.ROOT, "%02d:%02d:%02d", total / 3600, (total / 60) % 60, total % 60);
	}

	/**
	 * Map Info: the save's name, its picture (a campaign's mission picture, else the map's minimap) and the facts as
	 * gold labels with their values on the right, or why this engine cannot load it.
	 */
	private void showSaveInfo(final GameCatalog.SaveEntry entry) {
		final Board b = this.board;
		final boolean t = b.tablet;
		final FrameLayout info = this.saveInfo;
		info.removeAllViews();
		final boolean loadable = entry.loadability == SavedGame.Loadability.LOADABLE;
		this.loadButton.setEnabled(loadable);
		this.deleteButton.setEnabled(true);
		final float frame = b.pageInset;
		final float w = b.contentW - (t ? 1340 : 716);
		final float inner = w - (2 * frame);
		final TextView title = text(entry.name, t ? 44 : 28, this.art.text());
		title.setGravity(t ? Gravity.CENTER_HORIZONTAL : Gravity.START);
		place(info, title, frame, frame, inner, -1);
		final float picture = t ? w * 0.6f : 150;
		// A campaign save shows its mission's picture, a wider one.
		final boolean campaign = (entry.mapFile != null) && this.art.reforged()
				&& entry.mapFile.toLowerCase(Locale.ROOT).contains("campaign");
		final float pictureH = campaign ? picture * 0.75f : picture;
		final ImageView minimap = new ImageView(this);
		minimap.setScaleType(campaign ? ImageView.ScaleType.FIT_CENTER : ImageView.ScaleType.FIT_XY);
		minimap.setBackground(minimapFrame(t ? 10 : 6));
		final int rim = px(t ? 10 : 6);
		minimap.setPadding(rim, rim, rim, rim);
		minimap.setImageBitmap(this.art.bitmap(SkinCache.MINIMAP_UNKNOWN));
		if (!loadable) {
			minimap.setAlpha(0.5f);
		}
		final float pictureY = frame + (t ? 76 : 48);
		place(info, minimap, t ? (w - picture) / 2 : frame, pictureY, picture, pictureH);
		final float factsX = t ? frame + 12 : frame + picture + 14;
		final float factsY = t ? pictureY + pictureH + 40 : pictureY;
		final float factsW = t ? inner - 24 : inner - picture - 14;
		final float size = t ? 30 : 19;
		final float step = size * 1.6f;
		if (!loadable) {
			final TextView reason = lines(loadabilityText(entry.loadability), size, ERROR, 3);
			reason.setGravity(t ? Gravity.CENTER_HORIZONTAL : Gravity.START);
			place(info, reason, factsX, factsY, factsW, -1);
			return;
		}
		final TextView players = fact(info, glue("COLON_PLAYERS", "Players:"), "", factsX, factsY, factsW, size);
		fact(info, glue("COLON_ELAPSED_TIME", "Elapsed Time:"), elapsed(entry.gameTime), factsX, factsY + step, factsW,
				size);
		if (t) {
			fact(info, glue("COLON_SAVED", "Saved:"),
					DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(entry.saved)),
					factsX, factsY + (2 * step), factsW, size);
		}
		if (entry.mapFile == null) {
			return;
		}
		final int generation = this.generation;
		this.catalog.mapDetails(entry.mapFile, details -> {
			if ((generation != this.generation) || (details == null) || (chosenSave() != entry)) {
				return;
			}
			final String mission = campaign ? missionPicture(details.name) : null;
			final Bitmap missionArt = mission != null ? this.art.bitmap(mission) : null;
			if (missionArt != null) {
				minimap.setScaleType(ImageView.ScaleType.CENTER_CROP);
				minimap.setImageBitmap(missionArt);
			}
			else if (details.minimap != null) {
				minimap.setImageBitmap(details.minimap);
			}
			int count = 0;
			for (final GameCatalog.Slot slot : details.slots) {
				if ((slot.type == 1) || (slot.type == 2)) {
					count++;
				}
			}
			players.setText(Integer.toString(count));
		});
	}

	/** A fact of Map Info: the gold label on the left, its value on the right; returns the value. */
	private TextView fact(final FrameLayout parent, final String label, final String value, final float x,
			final float y, final float w, final float size) {
		place(parent, text(LauncherKit.glueText(label), size, this.art.gold()), x, y, w * 0.5f, -1);
		final TextView shown = text(value, size, this.art.text());
		shown.setGravity(Gravity.END);
		place(parent, shown, x + (w * 0.4f), y, w * 0.6f, -1);
		return shown;
	}

	/** The Reforged picture of the campaign mission with the name given, or null. */
	private String missionPicture(final String mapName) {
		final Map<String, String> pictures = this.art.missionPictures();
		final String best = bestPicture(words(LauncherKit.glueText(mapName).toString()), pictures.keySet(), null);
		return best == null ? null : pictures.get(best);
	}

	private void askDelete() {
		final GameCatalog.SaveEntry entry = this.saves == null ? null : chosenSave();
		if (entry == null) {
			return;
		}
		// Destructive: No comes first and is the plain way out.
		showQuestion(getString(R.string.game_delete_save, entry.name), null,
				new CharSequence[] { glueLabel("NO", getString(R.string.game_no)),
					glueLabel("YES", getString(R.string.game_yes)) },
				new Runnable[] { null, () -> {
					if (!entry.file.delete()) {
						Log.w(TAG, "Unable to delete " + entry.file);
					}
					showLoad();
				} });
	}

	private void startLoad() {
		final GameCatalog.SaveEntry entry = this.saves == null ? null : chosenSave();
		if ((entry == null) || (entry.loadability != SavedGame.Loadability.LOADABLE)) {
			return;
		}
		final Intent intent = new Intent();
		intent.putExtra(LaunchContract.EXTRA_SAVE_FILE, entry.name);
		if (entry.mapFile != null) {
			intent.putExtra(LaunchContract.EXTRA_MAP_FILE, entry.mapFile);
		}
		launchGame(intent, PAGE_LOAD);
	}

	// ---------------------------------------------------------------- Player Profile

	/** The campaign progress of every profile, by name, while the profile page shows; read when it opens. */
	private java.util.Map<String, CCampaignProgress.Snapshot> profileProgress;
	private String profileShown;

	/** Player Profile: the profiles on the left, the one tapped on the right; Select makes it the current one. */
	private void showProfile(final String chosen) {
		final List<String> names = this.profiles.names();
		final String current = this.profiles.current();
		final String selected = chosen != null && names.contains(chosen) ? chosen
				: (this.profileShown != null) && names.contains(this.profileShown) && (this.shown == PAGE_PROFILE)
						? this.profileShown
						: current;
		this.profileShown = selected;
		final Board b = this.board;
		final boolean t = b.tablet;
		final FrameLayout view = shell(RAIL_NONE);
		final FrameLayout content = content(view, 0);
		final float listW = t ? 1060 : 560;
		final float frame = b.pageInset;
		final FrameLayout listPanel = framed(content, 0, 0, listW, b.contentH);
		final float rowW = listW - (2 * frame);
		final float fieldH = t ? 112 : 96;
		final float addW = t ? 240 : 180;
		final float gap = t ? 16 : 8;
		final float listH = b.contentH - (2 * frame) - fieldH - gap;
		final LinearLayout rows = scrollList(listPanel, frame, frame, rowW, listH);
		final float rowH = t ? 150 : 96;
		View selectedRow = null;
		for (final String name : names) {
			final View row = profileRow(name, rowW, rowH);
			onTap(row, false, v -> showProfile(name));
			markRow(row, name.equals(selected));
			if (name.equals(selected)) {
				selectedRow = row;
			}
			rows.addView(row, new LinearLayout.LayoutParams(MATCH, px(rowH)));
		}
		if (selectedRow != null) {
			reveal(selectedRow);
		}
		final float fieldY = b.contentH - frame - fieldH;
		final float fieldW = rowW - addW - gap;
		final EditText field = nameField(t ? 34 : 24);
		place(listPanel, field, frame, fieldY, fieldW, fieldH);
		final LauncherKit.GlueButton add = button(glueLabel("KEY_ADD", "Add"), fieldH, t ? 34 : 26, v -> {
			final String typed = field.getText().toString();
			if (this.profiles.check(typed, null) == Profiles.NameProblem.NONE) {
				this.profiles.create(typed, firstPortrait(), false);
				hideKeyboard(field);
				showProfile(typed.trim());
			}
		});
		add.setEnabled(false);
		place(listPanel, add, frame + fieldW + gap, fieldY, addW, fieldH);
		watchName(field, null, add, null);
		field.setOnEditorActionListener((v, action, event) -> {
			if ((action == EditorInfo.IME_ACTION_DONE) && add.isEnabled()) {
				add.performClick();
			}
			return false;
		});

		final float rightX = t ? 1096 : 570;
		final float rightW = b.contentW - rightX;
		final FrameLayout panel = framed(content, rightX, 0, rightW, b.contentH);
		final ScrollView scroll = new ScrollView(this);
		scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
		final FrameLayout details = new FrameLayout(this);
		scroll.addView(details, new FrameLayout.LayoutParams(MATCH, ViewGroup.LayoutParams.WRAP_CONTENT));
		place(panel, scroll, frame, frame, rightW - (2 * frame), b.contentH - (2 * frame));
		fillProfileDetails(details, selected, rightW - (2 * frame));

		action(view, 0, glueLabel("BACK", getString(R.string.launcher_back)), v -> showHome());
		final LauncherKit.GlueButton delete = action(view, 1, glueLabel("KEY_DELETE", "Delete"),
				v -> askDeleteProfile(selected));
		delete.setTone(LauncherKit.GlueButton.TONE_DANGER);
		delete.setEnabled(names.size() > 1);
		final LauncherKit.GlueButton select = action(view, 2, glueLabel("KEY_SELECT", "Select"), v -> {
			this.profiles.select(selected);
			profileChanged();
			showHome();
		});
		setPage(view, PAGE_PROFILE);
		if (this.profileProgress == null) {
			readProfileProgress(names);
		}
	}

	/** A profile of the list: portrait, name and how far its campaigns got. */
	private View profileRow(final String name, final float w, final float rowH) {
		final boolean t = this.board.tablet;
		final LinearLayout row = new LinearLayout(this);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setPadding(px(t ? 14 : 8), 0, px(8), 0);
		final float icon = rowH - (t ? 28 : 16);
		final ImageView portrait = new ImageView(this);
		portrait.setScaleType(ImageView.ScaleType.FIT_XY);
		portrait.setImageBitmap(portraitBitmap(this.profiles.portrait(name)));
		row.addView(portrait, new LinearLayout.LayoutParams(px(icon), px(icon)));
		final LinearLayout texts = new LinearLayout(this);
		texts.setOrientation(LinearLayout.VERTICAL);
		texts.addView(text(name, t ? 40 : 26, this.art.text()));
		final String progress = campaignLine(name);
		if (!progress.isEmpty()) {
			final TextView line = text(progress, t ? 28 : 19, this.art.text2());
			final LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			lineParams.topMargin = px(t ? 10 : 4);
			texts.addView(line, lineParams);
		}
		final LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0,
				ViewGroup.LayoutParams.WRAP_CONTENT, 1);
		textParams.leftMargin = px(t ? 22 : 12);
		row.addView(texts, textParams);
		row.setContentDescription(name);
		return row;
	}

	/** "Undead Campaign · Chapter Four": the campaign the profile plays and its furthest mission. */
	private String campaignLine(final String name) {
		final CCampaignProgress.Snapshot progress = this.profileProgress == null ? null : this.profileProgress.get(name);
		if ((progress == null) || (this.campaigns == null)) {
			return "";
		}
		final CampaignCatalog catalog = this.campaigns.catalog;
		final CampaignCatalog.Entry entry = playedCampaign(catalog, progress);
		if (entry == null) {
			return "";
		}
		final String chapter = chapterOf(entry, progress);
		return glueLabel(entry.getData().getHeader(), entry.getData().getHeader())
				+ (chapter.isEmpty() ? "" : " · " + chapter);
	}

	/** The header of the furthest open mission of a campaign. */
	private String chapterOf(final CampaignCatalog.Entry entry, final CCampaignProgress.Snapshot progress) {
		final List<CampaignMission> missions = entry.getData().getMissions();
		int furthest = -1;
		for (int i = 0; i < missions.size(); i++) {
			if (CampaignCatalog.isMissionOpen(entry, i, progress)) {
				furthest = i;
			}
		}
		return furthest < 0 ? ""
				: glueLabel(missions.get(furthest).getHeader(), missions.get(furthest).getHeader()).toString();
	}

	private void readProfileProgress(final List<String> names) {
		final int generation = this.generation;
		final Runnable read = () -> {
			final List<File> folders = new ArrayList<>();
			for (final String name : names) {
				folders.add(this.profiles.folder(name));
			}
			this.catalog.progress(folders, snapshots -> {
				if ((snapshots == null) || (generation != this.generation) || (this.shown != PAGE_PROFILE)) {
					return;
				}
				final java.util.Map<String, CCampaignProgress.Snapshot> found = new java.util.HashMap<>();
				for (int i = 0; i < names.size(); i++) {
					found.put(names.get(i), snapshots.get(i));
				}
				this.profileProgress = found;
				showProfile(this.profileShown);
			});
		};
		if (this.campaigns != null) {
			read.run();
			return;
		}
		this.catalog.campaigns(found -> {
			if (found != null) {
				this.campaigns = found;
				read.run();
			}
		});
	}

	/** The right side: portrait, name, the two buttons, then the campaigns and the statistics side by side. */
	private void fillProfileDetails(final FrameLayout details, final String name, final float w) {
		final boolean t = this.board.tablet;
		final float picture = t ? 240 : 120;
		// The design sets the portrait a little further in than the panel's contents.
		final float left = t ? 22 : 0;
		final float top0 = t ? 16 : 0;
		final ImageView portrait = new ImageView(this);
		portrait.setScaleType(ImageView.ScaleType.FIT_XY);
		portrait.setImageBitmap(portraitBitmap(this.profiles.portrait(name)));
		portrait.setContentDescription(name);
		place(details, portrait, left, top0, picture, picture);
		final float textX = left + picture + (t ? 44 : 16);
		place(details, text(name, t ? 60 : 32, this.art.text()), textX, top0, w - textX, -1);
		final long created = this.profiles.created(name);
		final int saved = this.profiles.savedGames(name);
		if (t) {
			final String facts = created > 0
					? getString(R.string.profile_created,
							DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(created)), saved)
					: getString(R.string.profile_saved_games, saved);
			place(details, text(facts, 28, this.art.text2()), textX, top0 + 84, w - textX, -1);
		}
		final float buttonH = t ? 112 : 96;
		final float buttonsX = t ? textX : 0;
		final float buttonsY = t ? top0 + 128 : picture + 12;
		final float changeW = t ? 340 : ((w - buttonsX) - 12) / 2;
		final float renameW = t ? 260 : changeW;
		place(details, button(LauncherKit.glueText(glue("CHANGE_PORTRAIT", "Change Portrait") + "…"), buttonH,
				t ? 32 : 24, v -> showChoosePortrait(name)), buttonsX, buttonsY, changeW, buttonH);
		place(details, button(getString(R.string.profile_rename), buttonH, t ? 32 : 24, v -> showRename(name)),
				buttonsX + changeW + (t ? 20 : 12), buttonsY, renameW, buttonH);
		final float top = t ? top0 + picture + 50 : buttonsY + buttonH + 20;
		// Campaigns take a little more than half of the width, as the design's columns do.
		final float inner = w - (2 * left) - (t ? 16 : 0);
		final float campaignsW = t ? inner * 0.52f : (w - 16) / 2;
		final float statisticsX = left + campaignsW + (t ? 40 : 16);
		final float campaignsEnd = campaignColumn(details, name, left, top, campaignsW);
		final float statisticsEnd = statisticsColumn(details, name, statisticsX, top, w - statisticsX - left);
		final View end = new View(this);
		place(details, end, 0, Math.max(campaignsEnd, statisticsEnd), 1, 1);
	}

	/** The campaigns under the game they belong to; each with "Completed", its chapter or a dash. */
	private float campaignColumn(final FrameLayout parent, final String name, final float x, final float top,
			final float w) {
		final boolean t = this.board.tablet;
		final float step = t ? 48 : 36;
		final float size = t ? 28 : 18;
		float y = top;
		if ((this.campaigns == null) || (this.profileProgress == null)) {
			place(parent, text(getString(R.string.game_loading_list), size, this.art.text2()), x, y, w, -1);
			return y + step;
		}
		final CampaignCatalog catalog = this.campaigns.catalog;
		final CCampaignProgress.Snapshot progress = this.profileProgress.get(name);
		// The combined list runs on from Reign of Chaos into The Frozen Throne at its first expansion campaign.
		Boolean frozenShown = null;
		boolean frozen = !catalog.isUnified() && !catalog.isClassicList();
		for (final CampaignCatalog.Entry entry : catalog.getEntries()) {
			frozen |= catalog.isUnified() && entry.getSection().toLowerCase(Locale.ROOT).endsWith("ex");
			if ((frozenShown == null) || (frozenShown != frozen)) {
				if (y > top) {
					y += t ? 18 : 4;
				}
				y = sectionHead(parent, getString(frozen ? R.string.profile_frozen : R.string.profile_reign), x, y, w);
				frozenShown = frozen;
			}
			final String status;
			final int color;
			if (progress == null) {
				status = getString(R.string.profile_not_started);
				color = this.art.text2();
			}
			else if (progress.isEndingCinematicAvailable(entry.getNumber())) {
				status = getString(R.string.profile_completed);
				color = LauncherKit.OK;
			}
			else if (started(entry, progress) || (playedCampaign(catalog, progress) == entry)) {
				status = chapterOf(entry, progress);
				color = this.art.text();
			}
			else {
				status = getString(R.string.profile_not_started);
				color = this.art.text2();
			}
			final float labelW = w * (t ? 0.62f : 0.55f);
			final float indent = t ? 16 : 0;
			place(parent, text(shortCampaign(glueLabel(entry.getData().getHeader(), entry.getData().getHeader())),
					size, this.art.text()), x + indent, y, labelW - indent - 8, -1);
			place(parent, text(status, size, color), x + labelW, y, w - labelW, -1);
			y += step;
		}
		return y;
	}

	/** Games played, wins, the favourite race, the race with most wins and a row per race. */
	private float statisticsColumn(final FrameLayout parent, final String name, final float x, final float top,
			final float w) {
		final boolean t = this.board.tablet;
		final float step = t ? 48 : 36;
		final float size = t ? 28 : 18;
		final float indent = t ? 16 : 0;
		float y = sectionHead(parent, getString(R.string.profile_statistics), x, top, w);
		final int[][] counts = this.profiles.statistics(name, RACES.length);
		int games = 0;
		int wins = 0;
		int favourite = -1;
		int mostWins = -1;
		for (int i = 0; i < counts.length; i++) {
			games += counts[i][0];
			wins += counts[i][1];
			if ((counts[i][0] > 0) && ((favourite < 0) || (counts[i][0] > counts[favourite][0]))) {
				favourite = i;
			}
			if ((counts[i][1] > 0) && ((mostWins < 0) || (counts[i][1] > counts[mostWins][1]))) {
				mostWins = i;
			}
		}
		final String[] races = glueList(RACES, 0);
		final String none = getString(R.string.profile_not_started);
		final String[][] facts = { { getString(R.string.profile_games_played), Integer.toString(games) },
			{ getString(R.string.profile_wins),
				wins + (games > 0 ? String.format(Locale.ROOT, " (%d %%)", Math.round((wins * 100f) / games)) : "") },
			{ getString(R.string.profile_favourite), favourite < 0 ? none : LauncherKit.glueText(races[favourite]).toString() },
			{ getString(R.string.profile_most_wins), mostWins < 0 ? none : LauncherKit.glueText(races[mostWins]).toString() } };
		final float labelW = w * 0.55f;
		for (final String[] fact : facts) {
			place(parent, text(fact[0], size, this.art.text2()), x + indent, y, labelW - indent - 8, -1);
			place(parent, text(fact[1], size, this.art.text()), x + labelW, y, w - labelW, -1);
			y += step;
		}
		y += t ? 24 : step * 0.3f;
		final float symbol = t ? 40 : 28;
		final float gamesX = x + (w * 0.55f);
		final float winsX = x + (w * 0.8f);
		place(parent, text(getString(R.string.profile_games), t ? 24 : size, this.art.text2()), gamesX, y, w * 0.25f,
				-1);
		place(parent, text(getString(R.string.profile_wins), t ? 24 : size, this.art.text2()), winsX, y, w * 0.2f, -1);
		y += t ? 40 : step;
		// Random first in the lobby; the table lists it last, as the design's race order has it.
		final int[] order = { 1, 2, 3, 4, 0 };
		for (final int race : order) {
			final View mark;
			if (race > 0) {
				mark = image(SkinCache.RACE_SYMBOLS[race - 1], ImageView.ScaleType.FIT_CENTER);
			}
			else {
				final TextView question = text("?", symbol * 0.8f, this.art.gold());
				question.setGravity(Gravity.CENTER);
				mark = question;
			}
			place(parent, mark, x + indent, y - ((symbol - size) / 2), symbol, symbol);
			place(parent, text(LauncherKit.glueText(races[race]), size, this.art.text()), x + indent + symbol + 14, y,
					gamesX - x - indent - symbol - 20, -1);
			place(parent, text(Integer.toString(counts[race][0]), size, this.art.text()), gamesX, y, w * 0.25f, -1);
			place(parent, text(Integer.toString(counts[race][1]), size, this.art.gold()), winsX, y, w * 0.2f, -1);
			y += step;
		}
		return y;
	}

	/** A heading of the profile's tables: gold, with a thin line under it on the tablet; returns the next line's y. */
	private float sectionHead(final FrameLayout parent, final String title, final float x, final float y,
			final float w) {
		final boolean t = this.board.tablet;
		place(parent, text(title, t ? 30 : 22, this.art.gold()), x, y, w, -1);
		if (!t) {
			return y + 38;
		}
		final View line = new View(this);
		line.setBackgroundColor(Color.argb(0x1F, 0xFF, 0xFF, 0xFF));
		place(parent, line, x, y + 42, w, 2);
		return y + 60;
	}

	/** "Human Campaign" is "Human" in the profile's table, which is about campaigns already. */
	private static String shortCampaign(final CharSequence header) {
		final String text = header.toString();
		return text.endsWith(" Campaign") ? text.substring(0, text.length() - " Campaign".length()) : text;
	}

	/** The current profile changed: its progress and saves are read again. */
	private void profileChanged() {
		this.campaigns = null;
		this.profileProgress = null;
		this.saves = null;
	}

	private int firstPortrait() {
		final List<Integer> found = portraits();
		return found.isEmpty() ? 0 : found.get(0);
	}

	/** The original edit box with a profile name: one line, at most the name's length. */
	private EditText nameField(final float size) {
		final EditText field = new EditText(this);
		field.setBackground(editFrame(this.board.tablet ? 14 : 8));
		field.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * this.scale);
		field.setTextColor(this.art.text());
		field.setHintTextColor(LauncherKit.DISABLED);
		field.setHint(glue("NAME", getString(R.string.profile_name)));
		field.setTypeface(this.art.font());
		field.setSingleLine(true);
		field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
		field.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
		field.setFilters(new InputFilter[] { new InputFilter.LengthFilter(Profiles.MAX_NAME) });
		final int pad = px(this.board.tablet ? 24 : 14);
		field.setPadding(pad, 0, pad, 0);
		field.setGravity(Gravity.CENTER_VERTICAL);
		return field;
	}

	/** Keeps the confirming button enabled only for a usable name, and says why another one is not. */
	private void watchName(final EditText field, final String renaming, final View confirm, final TextView problem) {
		final Runnable check = () -> {
			final String typed = field.getText().toString();
			final Profiles.NameProblem found = this.profiles.check(typed, renaming);
			confirm.setEnabled(found == Profiles.NameProblem.NONE);
			if (problem != null) {
				problem.setText(found == Profiles.NameProblem.EMPTY
						? glue("PROFILE_NEEDS_A_NAME", getString(R.string.profile_needs_name))
						: found == Profiles.NameProblem.TAKEN ? getString(R.string.profile_name_taken)
								: found == Profiles.NameProblem.CHARACTERS
										? getString(R.string.profile_name_characters)
										: "");
			}
		};
		field.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(final CharSequence text, final int start, final int count, final int after) {
			}

			@Override
			public void onTextChanged(final CharSequence text, final int start, final int before, final int count) {
			}

			@Override
			public void afterTextChanged(final Editable text) {
				check.run();
			}
		});
		check.run();
	}

	private void showKeyboard(final EditText field) {
		field.requestFocus();
		field.post(() -> {
			final InputMethodManager input = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
			if (input != null) {
				input.showSoftInput(field, InputMethodManager.SHOW_IMPLICIT);
			}
		});
	}

	private void hideKeyboard(final View view) {
		final InputMethodManager input = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		if (input != null) {
			input.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}

	/**
	 * The skin's portraits as a grid, or on the phone's first profile one row that scrolls sideways; the chosen one
	 * carries the command card's active glow.
	 */
	/**
	 * The portraits to pick from, a race under its name (one scrolling row on the phone's first profile); the chosen
	 * one glows and is scrolled into sight.
	 */
	private View portraitGrid(final int[] chosen, final int columns, final float cell, final float gap,
			final boolean oneRow) {
		final List<ImageView> glows = new ArrayList<>();
		final LinearLayout lines = new LinearLayout(this);
		lines.setOrientation(oneRow ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
		final View[] chosenItem = new View[1];
		int number = 0;
		for (final SkinCache.PortraitGroup group : this.cache.portraitGroups()) {
			final List<Integer> found = new ArrayList<>();
			for (final int portrait : group.portraits) {
				if ((portrait >= 0) && this.art.has(SkinCache.portraitFile(portrait))) {
					found.add(portrait);
				}
			}
			if (found.isEmpty()) {
				continue;
			}
			if (oneRow && (number > 0)) {
				lines.addView(new View(this), new LinearLayout.LayoutParams(px(gap * 2), 1));
			}
			if (!oneRow && (group.key != null)) {
				final TextView header = text(glue(group.key, group.fallback), Math.max(24, cell * 0.16f),
						this.art.gold());
				final LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(MATCH,
						ViewGroup.LayoutParams.WRAP_CONTENT);
				headerParams.topMargin = number > 0 ? px(gap) : 0;
				headerParams.bottomMargin = px(gap / 2);
				lines.addView(header, headerParams);
			}
			LinearLayout line = null;
			for (int i = 0; i < found.size(); i++) {
				final int portrait = found.get(i);
				if (oneRow) {
					line = lines;
				}
				else if ((i % columns) == 0) {
					line = new LinearLayout(this);
					final LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(MATCH, px(cell));
					lineParams.bottomMargin = px(gap);
					lines.addView(line, lineParams);
				}
				final FrameLayout item = new FrameLayout(this);
				final ImageView picture = new ImageView(this);
				picture.setScaleType(ImageView.ScaleType.FIT_XY);
				picture.setImageBitmap(portraitBitmap(portrait));
				item.addView(picture, new FrameLayout.LayoutParams(MATCH, MATCH));
				final ImageView glow = new ImageView(this);
				glow.setScaleType(ImageView.ScaleType.FIT_XY);
				glow.setImageBitmap(this.art.bitmap(SkinCache.PORTRAIT_ACTIVE));
				if (!this.art.reforged()) {
					// The command card draws its active glow additively.
					final android.graphics.Paint add = new android.graphics.Paint();
					add.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.ADD));
					glow.setLayerType(View.LAYER_TYPE_HARDWARE, add);
				}
				glow.setVisibility(portrait == chosen[0] ? View.VISIBLE : View.INVISIBLE);
				if (portrait == chosen[0]) {
					chosenItem[0] = oneRow ? item : line;
				}
				item.addView(glow, new FrameLayout.LayoutParams(MATCH, MATCH));
				glows.add(glow);
				number++;
				item.setContentDescription(getString(R.string.profile_portrait) + " " + number);
				onTap(item, false, v -> {
					chosen[0] = portrait;
					for (final ImageView other : glows) {
						other.setVisibility(other == glow ? View.VISIBLE : View.INVISIBLE);
					}
				});
				final LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(px(cell), px(cell));
				itemParams.rightMargin = px(gap);
				line.addView(item, itemParams);
			}
		}
		if (oneRow) {
			final HorizontalScrollView scroll = new HorizontalScrollView(this);
			scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
			scroll.addView(lines);
			if (chosenItem[0] != null) {
				scroll.post(() -> scroll.scrollTo(Math.max(0, chosenItem[0].getLeft() - px(cell)), 0));
			}
			return scroll;
		}
		final ScrollView scroll = new ScrollView(this);
		scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
		scroll.addView(lines);
		if (chosenItem[0] != null) {
			scroll.post(() -> scroll.scrollTo(0, Math.max(0, chosenItem[0].getTop() - px(cell / 2))));
		}
		return scroll;
	}

	/** No profile yet: New Profile over Home, which only Create or Skip close. */
	private void showFirstProfile() {
		final Board b = this.board;
		final boolean t = b.tablet;
		final float w = t ? 2200 : 1568;
		final float h = t ? 1600 : 688;
		final FrameLayout box = dialogBox(w, h);
		if (box == null) {
			return;
		}
		this.dialogLocked = true;
		final float inset = t ? 80 : 40;
		final float inner = w - (2 * inset);
		final EditText field = nameField(t ? 36 : 26);
		final TextView problem = text("", t ? 26 : 18, ERROR);
		final int[] portrait = { firstPortrait() };
		final View grid;
		final float buttonY = h - (t ? 72 : 32) - b.buttonH;
		if (t) {
			place(box, text(glue("NEW_PROFILE", "New Profile"), b.title, this.art.gold()), inset, 64, inner, -1);
			place(box, lines(getString(R.string.profile_message), b.body, this.art.text(), 2), inset, 140, inner, -1);
			place(box, field, inset, 210, inner, 120);
			place(box, problem, inset, 340, inner, -1);
			place(box, text(getString(R.string.profile_portrait), b.body, this.art.gold()), inset, 392, inner, -1);
			final int columns = 8;
			final float cell = 220;
			final float gridW = (columns * (cell + 20)) - 20;
			grid = portraitGrid(portrait, columns, cell, 20, false);
			place(box, grid, inset + ((inner - gridW) / 2), 446, gridW, buttonY - 446 - 32);
		}
		else {
			// On the phone the field is at the top, where the keyboard leaves it in sight.
			place(box, field, inset, 36, inner, 96);
			place(box, problem, inset, 140, inner * 0.5f, -1);
			place(box, text(getString(R.string.profile_message), 20, this.art.text2()), inset + (inner * 0.5f), 140,
					inner * 0.5f, -1);
			grid = portraitGrid(portrait, 0, 176, 12, true);
			place(box, grid, inset, 190, inner, 176 + 12);
		}
		final float buttonW = t ? 440 : 360;
		final float buttonGap = t ? 32 : 24;
		final float rowX = (w - ((2 * buttonW) + buttonGap)) / 2;
		final LauncherKit.GlueButton skip = button(getString(R.string.profile_skip), b.buttonH, b.buttonText, v -> {
			final String fallback = glue("PLAYER", "Player");
			if (this.profiles.check(fallback, null) == Profiles.NameProblem.NONE) {
				this.profiles.create(fallback, firstPortrait(), true);
			}
			else {
				this.profiles.select(fallback);
			}
			closeDialog();
			profileChanged();
			showHome();
		});
		place(box, skip, rowX, buttonY, buttonW, b.buttonH);
		final LauncherKit.GlueButton create = button(getString(R.string.profile_create), b.buttonH, b.buttonText,
				v -> {
					this.profiles.create(field.getText().toString(), portrait[0], true);
					closeDialog();
					profileChanged();
					showHome();
				});
		create.setTone(LauncherKit.GlueButton.TONE_CONFIRM);
		place(box, create, rowX + buttonW + buttonGap, buttonY, buttonW, b.buttonH);
		watchName(field, null, create, problem);
		typeInDialog(field);
		this.dialogFold = grid;
		this.dialogButtons = new View[] { skip, create };
		problem.setText("");
		field.setOnEditorActionListener((v, action, event) -> {
			if ((action == EditorInfo.IME_ACTION_DONE) && create.isEnabled()) {
				create.performClick();
			}
			return false;
		});
	}

	/** Choose Portrait: every portrait of the skin, the profile's own marked; OK keeps the one tapped. */
	private void showChoosePortrait(final String name) {
		final Board b = this.board;
		final boolean t = b.tablet;
		final int columns = 8;
		final float cell = t ? 240 : 168;
		final float gap = t ? 24 : 12;
		final float w = t ? 2400 : 1568;
		final float gridY = t ? 140 : 84;
		final float h = t ? 1760 : 704;
		final FrameLayout box = dialogBox(w, h);
		if (box == null) {
			return;
		}
		final float inset = t ? 72 : 36;
		place(box, text(getString(R.string.profile_choose_portrait), b.title, this.art.gold()), inset, t ? 56 : 28,
				w - (2 * inset), -1);
		final int[] portrait = { this.profiles.portrait(name) };
		final float gridW = (columns * (cell + gap)) - gap;
		final float buttonY = h - (t ? 64 : 28) - b.buttonH;
		place(box, portraitGrid(portrait, columns, cell, gap, false), (w - gridW) / 2, gridY, gridW,
				buttonY - gridY - (t ? 24 : 12));
		final float buttonW = t ? 400 : 320;
		place(box, button(glueLabel("CANCEL", "Cancel"), b.buttonH, b.buttonText, v -> closeDialog()),
				w - inset - (2 * buttonW) - 24, buttonY, buttonW, b.buttonH);
		place(box, button(glueLabel("OK", getString(R.string.game_ok)), b.buttonH, b.buttonText, v -> {
			this.profiles.setPortrait(name, portrait[0]);
			closeDialog();
			showProfile(name);
		}), w - inset - buttonW, buttonY, buttonW, b.buttonH);
	}

	/** Rename: the same name rules as a new profile; the folder with the saves moves along. */
	private void showRename(final String name) {
		final Board b = this.board;
		final boolean t = b.tablet;
		final float w = t ? 1400 : 1080;
		final float h = t ? 780 : 404;
		final FrameLayout box = dialogBox(w, h);
		if (box == null) {
			return;
		}
		final float inset = t ? 80 : 40;
		final float inner = w - (2 * inset);
		place(box, text(getString(R.string.profile_rename_title), b.title, this.art.gold()), inset, t ? 70 : 30, inner,
				-1);
		final EditText field = nameField(t ? 36 : 26);
		field.setText(name);
		field.setSelection(name.length());
		place(box, field, inset, t ? 180 : 90, inner, t ? 120 : 96);
		final TextView problem = text("", t ? 26 : 18, ERROR);
		place(box, problem, inset, t ? 316 : 194, inner, -1);
		final float buttonY = h - (t ? 72 : 28) - b.buttonH;
		final float buttonW = t ? 400 : 320;
		place(box, button(glueLabel("CANCEL", "Cancel"), b.buttonH, b.buttonText, v -> closeDialog()),
				w - inset - (2 * buttonW) - 24, buttonY, buttonW, b.buttonH);
		final LauncherKit.GlueButton ok = button(glueLabel("OK", getString(R.string.game_ok)), b.buttonH,
				b.buttonText, v -> {
					final String typed = field.getText().toString().trim();
					closeDialog();
					if (!typed.equals(name) && this.profiles.rename(name, typed)) {
						this.profileProgress = null;
						showProfile(typed);
					}
				});
		place(box, ok, w - inset - buttonW, buttonY, buttonW, b.buttonH);
		watchName(field, name, ok, problem);
		field.setOnEditorActionListener((v, action, event) -> {
			if ((action == EditorInfo.IME_ACTION_DONE) && ok.isEnabled()) {
				ok.performClick();
			}
			return false;
		});
		typeInDialog(field);
		showKeyboard(field);
	}

	/** The original question, No first: the profile goes with its saves and campaign progress. */
	private void askDeleteProfile(final String name) {
		if (this.profiles.names().size() <= 1) {
			return;
		}
		final String format = glue("DELETE_PROFILE_MESSAGE", getString(R.string.profile_delete_fallback));
		String question;
		try {
			question = String.format(Locale.ROOT, format, name);
		}
		catch (final RuntimeException e) {
			question = format;
		}
		showQuestion(question, getString(R.string.profile_delete_line),
				new CharSequence[] { glueLabel("NO", getString(R.string.game_no)),
					glueLabel("KEY_DELETE", "Delete") },
				new Runnable[] { null, () -> {
					final boolean current = name.equals(this.profiles.current());
					this.profiles.delete(name);
					if (current) {
						profileChanged();
					}
					this.profileProgress = null;
					this.profileShown = null;
					showProfile(this.profiles.current());
				} }, this.art.bitmap(SkinCache.QUESTION));
	}

	// ---------------------------------------------------------------- dialogs

	/** A modal box in the skin's dialog frame over a dark scrim; null while another dialog is open. */
	private FrameLayout dialogBox(final float w, final float h) {
		if (this.dialog != null) {
			return null;
		}
		final Board b = this.board;
		final FrameLayout scrim = new FrameLayout(this);
		scrim.setBackgroundColor(Color.argb(0xAB, 0, 0, 0));
		scrim.setClickable(true);
		scrim.setOnClickListener(v -> {
			if (!this.dialogLocked) {
				closeDialog();
			}
		});
		final FrameLayout box = new FrameLayout(this);
		box.setClickable(true);
		final float edge = b.tablet ? 48 : 32;
		box.setBackground(new LauncherKit.FrameDrawable(this.art.bitmap(SkinCache.DIALOG_BORDER), px(edge))
				.tiled(this.art.bitmap(SkinCache.DIALOG_BACKGROUND), px(b.tablet ? 256 : 128)).fillInset(px(edge * 0.4f))
				.fallback(Color.argb(0xEB, 0x03, 0x04, 0x0A)).line(this.art.line()));
		place(scrim, box, (b.width - w) / 2, (b.height - h) / 2, w, h);
		this.root.addView(scrim, new FrameLayout.LayoutParams(MATCH, MATCH));
		if (ValueAnimator.areAnimatorsEnabled()) {
			scrim.setAlpha(0);
			box.setScaleX(0.94f);
			box.setScaleY(0.94f);
			scrim.animate().alpha(1).setDuration(DIALOG_MILLIS);
			box.animate().scaleX(1).scaleY(1).setDuration(DIALOG_MILLIS);
		}
		this.dialog = scrim;
		this.openBox = box;
		return box;
	}

	private void showMessage(final String title, final String line) {
		showQuestion(title, line, new CharSequence[] { glueLabel("OK", getString(R.string.game_ok)) },
				new Runnable[] { null });
	}

	/** A question or a message with its buttons side by side; every button closes the dialog. */
	private void showQuestion(final String title, final String line, final CharSequence[] labels,
			final Runnable[] actions) {
		showQuestion(title, line, labels, actions, null);
	}

	/** With an icon, the question stands beside it, as the glue dialogs that ask about a profile have it. */
	private void showQuestion(final String title, final String line, final CharSequence[] labels,
			final Runnable[] actions, final Bitmap icon) {
		final Board b = this.board;
		final float w = b.tablet ? 1200 : 900;
		final float h = b.tablet ? 620 : 440;
		final FrameLayout box = dialogBox(w, h);
		if (box == null) {
			return;
		}
		final float inset = b.tablet ? 80 : 56;
		float textX = inset;
		if (icon != null) {
			final float side = b.tablet ? 160 : 112;
			final ImageView mark = new ImageView(this);
			mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
			mark.setImageBitmap(icon);
			place(box, mark, inset, b.tablet ? 90 : 56, side, side);
			textX = inset + side + (b.tablet ? 40 : 24);
		}
		final int gravity = icon != null ? Gravity.START : Gravity.CENTER;
		final LinearLayout texts = new LinearLayout(this);
		texts.setOrientation(LinearLayout.VERTICAL);
		final TextView question = lines(title, icon != null ? b.body * 1.25f : b.title, this.art.text(),
				icon != null ? 3 : 2);
		question.setGravity(gravity);
		texts.addView(question, new LinearLayout.LayoutParams(MATCH, ViewGroup.LayoutParams.WRAP_CONTENT));
		if ((line != null) && !line.isEmpty()) {
			final TextView detail = lines(line, b.body, this.art.text2(), 3);
			detail.setGravity(gravity);
			final LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(MATCH,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			detailParams.topMargin = px(b.tablet ? 32 : 20);
			texts.addView(detail, detailParams);
		}
		place(box, texts, textX, b.tablet ? 100 : 64, w - inset - textX, -1);
		final float gap = b.tablet ? 32 : 24;
		final float buttonW = Math.min(b.tablet ? 480 : 360,
				(w - (2 * inset) - ((labels.length - 1) * gap)) / labels.length);
		final float rowW = (labels.length * buttonW) + ((labels.length - 1) * gap);
		final float buttonY = h - inset - b.buttonH;
		for (int i = 0; i < labels.length; i++) {
			final Runnable action = actions[i];
			place(box, button(labels[i], b.buttonH, b.buttonText, v -> {
				closeDialog();
				if (action != null) {
					action.run();
				}
			}), ((w - rowW) / 2) + (i * (buttonW + gap)), buttonY, buttonW, b.buttonH);
		}
	}

	private void showExit() {
		showQuestion(getString(R.string.launcher_exit), null,
				new CharSequence[] { glueLabel("NO", getString(R.string.game_no)),
					glueLabel("YES", getString(R.string.game_yes)) },
				new Runnable[] { null, this::finish }, this.art.bitmap(SkinCache.QUESTION));
	}

	private void closeDialog() {
		if (this.dialog != null) {
			hideKeyboard(this.dialog);
			this.root.removeView(this.dialog);
			this.dialog = null;
		}
		if (this.dialogField != null) {
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		}
		this.openBox = null;
		this.dialogField = null;
		this.dialogFold = null;
		this.dialogButtons = null;
		this.dialogLocked = false;
	}

	/**
	 * Panning only keeps the focused field in sight, so the keyboard would cover the dialog's buttons below it. From
	 * Android 11 the dialog moves up by itself as far as the keyboard needs; before that the keyboard's height cannot be
	 * read without panning, so the dialog stands at the top of the screen.
	 */
	private void typeInDialog(final View field) {
		this.dialogField = field;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
		}
		else if (this.openBox != null) {
			final View box = this.openBox;
			box.post(() -> box.setTranslationY(px(this.board.tablet ? 32 : 12) - box.getTop()));
		}
	}

	private void trackOnscreenKeyboard() {
		final View decor = getWindow().getDecorView();
		decor.setOnApplyWindowInsetsListener((view, insets) -> {
			liftDialog(insets);
			return view.onApplyWindowInsets(insets);
		});
	}

	private void liftDialog(final WindowInsets insets) {
		if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.R) || (this.openBox == null) || (this.dialogField == null)) {
			return;
		}
		final View box = this.openBox;
		final int covered = insets.isVisible(WindowInsets.Type.ime()) ? insets.getInsets(WindowInsets.Type.ime()).bottom
				: 0;
		final View decor = getWindow().getDecorView();
		final int[] at = new int[2];
		decor.getLocationOnScreen(at);
		final int keyboardTop = (at[1] + decor.getHeight()) - covered;
		box.getLocationOnScreen(at);
		final int top = at[1] - Math.round(box.getTranslationY());
		final int margin = px(this.board.tablet ? 32 : 12);
		// The field itself stays on the screen even where the buttons cannot fit above the keyboard too.
		final int maxLift = Math.max(0, (top + this.dialogField.getTop()) - margin);
		int bottom = top + box.getHeight();
		final boolean fold = (covered > 0) && (this.dialogFold != null)
				&& (((bottom + margin) - keyboardTop) > maxLift);
		if (this.dialogFold != null) {
			this.dialogFold.setVisibility(fold ? View.INVISIBLE : View.VISIBLE);
			for (final View button : this.dialogButtons) {
				button.setTranslationY(fold ? this.dialogFold.getTop() - button.getTop() : 0);
				if (fold) {
					bottom = top + this.dialogFold.getTop() + button.getHeight();
				}
			}
		}
		final int lift = covered > 0 ? Math.max(0, Math.min((bottom + margin) - keyboardTop, maxLift)) : 0;
		box.animate().translationY(-lift).setDuration(150).start();
	}

	// ---------------------------------------------------------------- leaving

	private void openGameData() {
		final Intent intent = new Intent(this, DataFolderActivity.class);
		intent.putExtra(DataFolderActivity.EXTRA_OPEN, true);
		startActivity(intent);
		finish();
	}
}
