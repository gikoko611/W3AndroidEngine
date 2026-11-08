package com.etheller.warsmash.viewer5.handlers.w3x;

import java.io.ByteArrayInputStream;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentWeather;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentUbersplat;
import com.etheller.warsmash.viewer5.handlers.w3x.replay.ReplayPlayback;
import com.etheller.warsmash.viewer5.handlers.w3x.replay.ReplayRecording;
import com.etheller.warsmash.viewer5.handlers.w3x.replay.ReplaySession;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.WeatherType;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderWeatherEffect;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrder;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.ExternStringMsgTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.jass.RecordingAbilityTargetCheckReceiver;
import com.etheller.interpreter.ast.value.visitor.ArithmeticJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ArithmeticLeftHandCodeJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ArithmeticLeftHandStructJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ArithmeticLeftHandHandleJassValueVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGlobalWidgetEvent;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.CWidgetEvent;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound.CSoundMixer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound.CStackedSounds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CWidgetAbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.BuildOnBuildingIntersector;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.ability.GetInstantTransformationBuffVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.commandbuttons.CommandCardPopulatingAbilityVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackMissileSplash;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackMissileBounce;
import com.etheller.warsmash.viewer5.AudioContext;
import java.util.IdentityHashMap;
import com.badlogic.gdx.audio.Sound;
import java.io.BufferedReader;
import java.io.File;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.etheller.warsmash.common.FetchDataTypeName;
import com.etheller.warsmash.common.LoadGenericCallback;
import com.etheller.warsmash.datasources.CompoundDataSource;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.datasources.MpqDataSource;
import com.etheller.warsmash.datasources.SubdirDataSource;
import com.etheller.warsmash.networking.GameTurnManager;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.parsers.w3x.War3Map;
import com.etheller.warsmash.parsers.w3x.doo.War3MapDoo;
import com.etheller.warsmash.parsers.w3x.objectdata.Warcraft3MapObjectData;
import com.etheller.warsmash.parsers.w3x.objectdata.Warcraft3MapRuntimeObjectData;
import com.etheller.warsmash.parsers.w3x.unitsdoo.War3MapUnitsDoo;
import com.etheller.warsmash.parsers.w3x.w3e.War3MapW3e;
import com.etheller.warsmash.parsers.w3x.w3i.War3MapW3i;
import com.etheller.warsmash.parsers.w3x.w3i.War3MapW3iFlags;
import com.etheller.warsmash.parsers.w3x.wpm.War3MapWpm;
import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.units.Element;
import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.units.ObjectData;
import com.etheller.warsmash.units.StandardObjectData;
import com.etheller.warsmash.units.custom.WTS;
import com.etheller.warsmash.units.manager.MutableObjectData.WorldEditorDataType;
import com.etheller.warsmash.util.ArgbImage;
import com.etheller.warsmash.util.ImageUtils;
import com.etheller.warsmash.util.MappedData;
import com.etheller.warsmash.util.Quadtree;
import com.etheller.warsmash.util.QuadtreeIntersector;
import com.etheller.warsmash.util.RenderMathUtils;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.util.PerfStats;
import com.etheller.warsmash.util.WorldEditStrings;
import com.etheller.warsmash.viewer5.Bounds;
import com.etheller.warsmash.viewer5.CanvasProvider;
import com.etheller.warsmash.viewer5.FogSettings;
import com.etheller.warsmash.viewer5.GenericResource;
import com.etheller.warsmash.viewer5.Grid;
import com.etheller.warsmash.viewer5.ModelInstance;
import com.etheller.warsmash.viewer5.ModelViewer;
import com.etheller.warsmash.viewer5.PathSolver;
import com.etheller.warsmash.viewer5.Scene;
import com.etheller.warsmash.viewer5.SceneLightManager;
import com.etheller.warsmash.viewer5.Texture;
import com.etheller.warsmash.viewer5.WorldScene;
import com.etheller.warsmash.viewer5.gl.WebGL;
import com.etheller.warsmash.viewer5.handlers.AbstractMdxModelViewer;
import com.etheller.warsmash.viewer5.handlers.mdx.Attachment;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxComplexInstance;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxHandler;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxHandler.ShaderEnvironmentType;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxModel;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxNode;
import com.etheller.warsmash.viewer5.handlers.mdx.SequenceLoopMode;
import com.etheller.warsmash.viewer5.handlers.tga.TgaFile;
import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens.SecondaryTag;
import com.etheller.warsmash.viewer5.Camera;
import com.etheller.warsmash.viewer5.handlers.w3x.SplatModel.SplatMover;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.BuildingShadow;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid.PathingType;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid.RemovablePathingMapInstance;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.RenderCorner;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.Terrain;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.Terrain.Splat;
import com.etheller.warsmash.viewer5.handlers.w3x.lightning.LightningEffectModel;
import com.etheller.warsmash.viewer5.handlers.w3x.lightning.LightningEffectModelHandler;
import com.etheller.warsmash.viewer5.handlers.w3x.lightning.LightningEffectNode;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderAttackInstant;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderFogGhost;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderDoodad;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderEffect;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderItem;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderItemType;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderItemTypeData;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderLightningEffect;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderShadowType;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderImage;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderSpellEffect;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderUnitType;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderUnitTypeData;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.ability.AbilityDataUI;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.ability.AbilityUI;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.ability.BuffUI;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.ability.EffectAttachmentUI;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.ability.EffectAttachmentUIMissile;
import com.etheller.warsmash.viewer5.handlers.w3x.save.SavedFormerly;
import com.etheller.warsmash.viewer5.handlers.w3x.save.SavedGame;
import com.etheller.warsmash.viewer5.handlers.w3x.save.SavedInterfaceState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CGameExit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CImage;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CTrackable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitClassification;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidgetFilterFunction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CTerrainData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CPathingTextures;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.NoRenderController;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.blight.CAbilityBlight;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackInstant;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.War3MapConfig;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CPlayerSlotState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision.CPlayerFogOfWar;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CFogState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CSoundVolumeGroup;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentLightning;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentLightningMovable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderController;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.command.SettableCommandErrorListener;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.sound.KeyedSounds;

import mpq.MPQArchive;
import mpq.MPQException;

public class War3MapViewer extends AbstractMdxModelViewer implements MdxAssetLoader {
	private static final int MAX_STEPS_PER_FRAME = 3;
	/** Game seconds per real second (desktop -speed), so automated playthroughs of long games finish sooner. */
	private static float defaultTimeScale = 1;
	private static final byte FOGGED_SHADE = (byte) 168;
	private static final byte MASKED_PARTIALLY_VISIBLE_SHADE = (byte) 214;
	// Supported graphics modes of the map information: 1 Classic, 2 Reforged, 3 either.
	private static final long SUPPORTED_MODES_REFORGED = 2;
	private final EnumSet<PauseSource> pauseSources = EnumSet.noneOf(PauseSource.class);
	private float timeScale = defaultTimeScale;
	private final CGameExit gameExit = new CGameExit();
	private final CSoundMixer soundMixer = new CSoundMixer();
	private static final List<String> ORIGIN_STRING_LIST = Arrays.asList("origin");

	public static int DEBUG_DEPTH = 9999;

	static final float DEFAULT_TEXT_TAG_HEIGHT = 0.024f;
	static final int BUILTIN_TEXT_TAG_REPORTED_HANDLE_ID = -1;
	static final War3ID ABILITY_HERO_RAWCODE = War3ID.fromString("AHer");
	static final War3ID ABILITY_REVIVE_RAWCODE = War3ID.fromString("Arev");
	/** Runs the game without drawing it (see {@link #updateAndRender()}). */
	static final boolean NO_RENDER = Boolean.getBoolean("warsmash.norender");
	private static final Color PLACEHOLDER_LUMBER_COLOR = new Color(0.0f, 200f / 255f, 80f / 255f, 1.0f);
	private static final Color PLACEHOLDER_GOLD_COLOR = new Color(1.0f, 220f / 255f, 0f, 1.0f);
	private static final String DESTRUCTABLE_PATHING = "pathTex"; // replaced from 'bptx'
	private static final String DESTRUCTABLE_PATHING_DEATH = "pathTexDeath"; // replaced from 'bptd'
	private static final War3ID sloc = War3ID.fromString("sloc");
	private static final LoadGenericCallback stringDataCallback = new StringDataCallbackImplementation();
	private static final float[] rayHeap = new float[6];
	public static final Ray gdxRayHeap = new Ray();
	public static final Plane planeHeap = new Plane();
	private static final Vector2 mousePosHeap = new Vector2();
	private static final Vector3 normalHeap = new Vector3();
	public static final Vector3 intersectionHeap = new Vector3();
	public static final Vector3 intersectionHeap2 = new Vector3();
	private final Vector3 constructionPickHeap = new Vector3();
	private static final Rectangle rectangleHeap = new Rectangle();
	public static final StreamDataCallbackImplementation streamDataCallback = new StreamDataCallbackImplementation();

	public WorldScene worldScene;
	private boolean worldHidden;
	private NoRenderController renderController;
	/** The step the game plays on to without drawing (see {@link #fastForwardTo}), or -1. */
	private int fastForwardTarget = -1;
	private FastForwardListener fastForwardReached;
	private int fastForwardSteps;
	private long fastForwardNanos;
	public boolean anyReady;
	public MappedData terrainData = new MappedData();
	public MappedData cliffTypesData = new MappedData();
	public MappedData waterData = new MappedData();
	public boolean terrainReady;
	public boolean cliffsReady;
	public boolean doodadsAndDestructiblesLoaded;
	public MappedData doodadsData = new MappedData();
	public MappedData doodadMetaData = new MappedData();
	public MappedData destructableMetaData = new MappedData();
	public List<RenderDoodad> doodads = new ArrayList<>();
	private static final float SKY_FAR_PLANE = 100000f;
	private static final float UNIT_INDICATOR_SECONDS = 3f;
	private static final float UNIT_INDICATOR_PULSE_SECONDS = 1f;
	private Scene skyScene;
	private MdxComplexInstance skyInstance;
	private final List<UnitIndicator> unitIndicators = new ArrayList<>();
	public List<RenderDoodad> decals = new ArrayList<>();
	private int fogDoodadTicks;
	public List<TerrainDoodad> terrainDoodads = new ArrayList<>();
	public boolean doodadsReady;
	public boolean unitsAndItemsLoaded;
	public MappedData unitsData = new MappedData();
	public MappedData unitMetaData = new MappedData();
	public List<RenderWidget> widgets = new ArrayList<>();
	public List<RenderUnit> units = new ArrayList<>();
	private final List<RenderFogGhost> fogGhosts = new ArrayList<>();
	public List<RenderEffect> projectiles = new ArrayList<>();
	private final List<RenderLightningEffect> lightnings = new ArrayList<>();
	private final List<RenderWeatherEffect> weatherEffects = new ArrayList<>();
	private final CStackedSounds stackedSounds = new CStackedSounds();
	private final Vector3 stackedSoundsForward = new Vector3();
	private final List<com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderUbersplat> scriptUbersplats = new ArrayList<>();
	private final Map<CImage, RenderImage> scriptImages = new IdentityHashMap<>();
	/** Not among the effects: a trackable's model is made again from the trackable after a load. */
	private final List<RenderSpellEffect> trackableModels = new ArrayList<>();
	private final List<CTrackable> trackableOwners = new ArrayList<>();
	private Map<War3ID, WeatherType> weatherTypes;
	public boolean unitsReady;
	public War3Map mapMpq;

	private final DataSource gameDataSource;

	public Terrain terrain;
	public int renderPathing = 0;
	public int renderLighting = 1;

	private final Set<String> selectedSplatModelKeys = new HashSet<>();
	public List<RenderWidget> selected = new ArrayList<>();
	private final Set<String> mouseHighlightSplatModelKeys = new HashSet<>();
	private final List<RenderWidget> mouseHighlightWidgets = new ArrayList<>();
	private DataTable unitAckSoundsTable;
	DataTable unitCombatSoundsTable;
	private DataTable animSoundsTable;
	private DataTable scriptSoundsTable;
	public DataTable miscData;
	private com.etheller.warsmash.viewer5.handlers.w3x.ui.AllyColorFilter allyColorFilter;
	private boolean restartMissionDisabled;
	private String altMinimapIcon;
	private Element misc;
	private final Map<String, TextTagConfig> keyToTextTagConfig = new HashMap<>();
	private DataTable unitGlobalStrings;
	public DataTable uiSoundsTable;
	private DataTable lightningDataTable;
	private Map<War3ID, LightningEffectModel> lightningTypeToModel;
	private MdxComplexInstance confirmationInstance;
	public MdxComplexInstance dncUnit;
	public MdxComplexInstance dncTerrain;
	public MdxComplexInstance dncTarget;
	public CSimulation simulation;
	private float updateTime = 0;

	// for World Editor, I think
	public Vector2[] startLocations = new Vector2[WarsmashConstants.MAX_PLAYERS];

	private final DynamicShadowManager dynamicShadowManager = new DynamicShadowManager();

	/** Seeds the simulation; -Dwarsmash.seed fixes it so test runs repeat. Kept in saved games. */
	private int gameSeed = chooseGameSeed();

	private final Random seededRandom = new Random(this.gameSeed);

	private ReplaySession replaySession;
	private boolean replayNotSaved;
	private boolean gameEnded;
	private Runnable gameEndScreen;

	private final Map<String, ArgbImage> filePathToPathingMap = new HashMap<>();
	private CPathingTextures pathingTextures;

	private final List<SelectionCircleSize> selectionCircleSizes = new ArrayList<>();

	final Map<CUnit, RenderUnit> unitToRenderPeer = new HashMap<>();
	final Map<CDestructable, RenderDestructable> destructableToRenderPeer = new HashMap<>();
	private final Map<CItem, RenderItem> itemToRenderPeer = new HashMap<>();
	GameUI gameUI;
	private Vector3 lightDirection;

	Quadtree<MdxComplexInstance> walkableObjectsTree;
	private final QuadtreeIntersectorFindsWalkableRenderHeight walkablesIntersector = new QuadtreeIntersectorFindsWalkableRenderHeight();
	private final QuadtreeIntersectorFindsHitPoint walkablesIntersectionFinder = new QuadtreeIntersectorFindsHitPoint();
	private final QuadtreeIntersectorFindsHighestWalkable intersectorFindsHighestWalkable = new QuadtreeIntersectorFindsHighestWalkable();

	KeyedSounds uiSounds;
	int localPlayerIndex;
	private final SettableCommandErrorListener commandErrorListener;

	public final List<TextTag> textTags = new ArrayList<>();

	private final War3MapConfig mapConfig;
	private CPlayerFogOfWar observerFogOfWar;
	/** Whose sight a replay's picture shows: a player, {@link #REPLAY_VISION_ALL_PLAYERS} or none (a game). */
	private int replayVisionPlayer = REPLAY_VISION_NONE;
	private boolean replayFogShown = true;
	private CPlayerFogOfWar replayFogOfWar;
	private final List<CPlayerFogOfWar> replayVisionSources = new ArrayList<>();
	/** A replay's speed: game and animation time both run this many times faster. */
	private float replaySpeed = 1;
	public static final int REPLAY_VISION_ALL_PLAYERS = -1;
	private static final int REPLAY_VISION_NONE = -2;

	private GameTurnManager gameTurnManager;

	private War3MapW3i lastLoadedMapInformation;
	private String mapFileName;
	private FileHandle savedGameToLoad;
	private boolean savedGameLoadedByScript;
	private InterfaceStateKeeper interfaceStateKeeper;
	private Object scriptEnvironment;
	private final List<SavedGame.AiEntry> loadedScripts = new ArrayList<>();
	private String dayNightTerrainModel;
	private String dayNightUnitModel;
	private String skyModel;
	private float[] waterColor;
	private AudioContext uiAudioContext;
	private final Map<Sound, String> fileSounds = new IdentityHashMap<>();
	final List<CreatedDestructable> createdDestructables = new ArrayList<>();
	private FileHandle pendingSave;
	private FileHandle pendingLoad;
	private boolean pendingLoadByScript;
	private Runnable afterPendingSave;
	private SaveGameListener saveGameListener;
	private LoadGameListener loadGameListener;

	private FogSettings defaultFogSettings;

	private int successfulFrames = 0;

	public War3MapViewer(final DataSource dataSource, final CanvasProvider canvas, final War3MapConfig mapConfig,
			final GameTurnManager gameTurnManager) {
		super(dataSource, canvas);
		this.gameTurnManager = gameTurnManager;
		MdxHandler.CURRENT_SHADER_TYPE = ShaderEnvironmentType.GAME;
		this.gameDataSource = dataSource;

		final WebGL webGL = this.webGL;

		addHandler(new MdxHandler());

		this.wc3PathSolver = PathSolver.DEFAULT;

		this.worldScene = addWorldScene();

		if (!this.dynamicShadowManager.setup(webGL)) {
			throw new IllegalStateException("FrameBuffer setup failed");
		}

		this.commandErrorListener = new SettableCommandErrorListener();
		this.mapConfig = mapConfig;
	}

	public void loadSLKs(final WorldEditStrings worldEditStrings) throws IOException {
		final GenericResource terrain = loadMapGeneric("TerrainArt\\Terrain.slk", FetchDataTypeName.SLK,
				stringDataCallback);
		final GenericResource cliffTypes = loadMapGeneric("TerrainArt\\CliffTypes.slk", FetchDataTypeName.SLK,
				stringDataCallback);
		final GenericResource water = loadMapGeneric("TerrainArt\\Water.slk", FetchDataTypeName.SLK,
				stringDataCallback);

		// == when loaded, which is always in our system ==
		this.terrainData.load(terrain.data.toString());
		this.cliffTypesData.load(cliffTypes.data.toString());
		this.waterData.load(water.data.toString());
		// emit terrain loaded??

		final GenericResource doodads = loadMapGeneric("Doodads\\Doodads.slk", FetchDataTypeName.SLK,
				stringDataCallback);
		final GenericResource doodadMetaData = loadMapGeneric("Doodads\\DoodadMetaData.slk", FetchDataTypeName.SLK,
				stringDataCallback);
		final GenericResource destructableData = loadMapGeneric("Units\\DestructableData.slk", FetchDataTypeName.SLK,
				stringDataCallback);
		final GenericResource destructableMetaData = loadMapGeneric("Units\\DestructableMetaData.slk",
				FetchDataTypeName.SLK, stringDataCallback);

		// == when loaded, which is always in our system ==
		this.doodadsAndDestructiblesLoaded = true;
		this.doodadsData.load(doodads.data.toString());
		this.doodadMetaData.load(doodadMetaData.data.toString());
		this.doodadsData.load(destructableData.data.toString());
		this.destructableMetaData.load(destructableData.data.toString());
		// emit doodads loaded

		final GenericResource unitData = loadMapGeneric("Units\\UnitData.slk", FetchDataTypeName.SLK,
				stringDataCallback);
		final GenericResource unitUi = loadMapGeneric("Units\\unitUI.slk", FetchDataTypeName.SLK, stringDataCallback);
		final GenericResource itemData = loadMapGeneric("Units\\ItemData.slk", FetchDataTypeName.SLK,
				stringDataCallback);
		final GenericResource unitMetaData = loadMapGeneric("Units\\UnitMetaData.slk", FetchDataTypeName.SLK,
				stringDataCallback);

		loadLightningData(worldEditStrings);
		loadWeatherData(worldEditStrings);

		// == when loaded, which is always in our system ==
		this.unitsAndItemsLoaded = true;
		this.unitsData.load(unitData.data.toString());
		this.unitsData.load(unitUi.data.toString());
		this.unitsData.load(itemData.data.toString());
		this.unitMetaData.load(unitMetaData.data.toString());
		// emit loaded

		this.unitAckSoundsTable = new DataTable(worldEditStrings);
		try (InputStream terrainSlkStream = this.dataSource.getResourceAsStream("UI\\SoundInfo\\UnitAckSounds.slk")) {
			this.unitAckSoundsTable.readSLK(terrainSlkStream);
		}
		DialogueLabels.resolve(this.dataSource, this.unitAckSoundsTable, worldEditStrings);
		this.unitCombatSoundsTable = new DataTable(worldEditStrings);
		try (InputStream terrainSlkStream = this.dataSource
				.getResourceAsStream("UI\\SoundInfo\\UnitCombatSounds.slk")) {
			this.unitCombatSoundsTable.readSLK(terrainSlkStream);
		}
		this.animSoundsTable = new DataTable(worldEditStrings);
		try (InputStream stream = this.dataSource.getResourceAsStream("UI\\SoundInfo\\AnimSounds.slk")) {
			if (stream != null) {
				this.animSoundsTable.readSLK(stream);
			}
		}
		this.miscData = new DataTable(worldEditStrings);
		try (InputStream miscDataTxtStream = this.dataSource.getResourceAsStream("UI\\MiscData.txt")) {
			this.miscData.readTXT(miscDataTxtStream, true);
		}
		try (InputStream miscDataTxtStream = this.dataSource.getResourceAsStream("Units\\MiscData.txt")) {
			this.miscData.readTXT(miscDataTxtStream, true);
		}
		try (InputStream miscDataTxtStream = this.dataSource.getResourceAsStream("Units\\MiscGame.txt")) {
			this.miscData.readTXT(miscDataTxtStream, true);
		}
		try (InputStream miscDataTxtStream = this.dataSource.getResourceAsStream("UI\\MiscUI.txt")) {
			this.miscData.readTXT(miscDataTxtStream, true);
		}
		try (InputStream miscDataTxtStream = this.dataSource.getResourceAsStream("UI\\SoundInfo\\MiscData.txt")) {
			this.miscData.readTXT(miscDataTxtStream, true);
		}
		if (this.dataSource.has("war3mapMisc.txt")) {
			try (InputStream miscDataTxtStream = this.dataSource.getResourceAsStream("war3mapMisc.txt")) {
				this.miscData.readTXT(miscDataTxtStream, true);
			}
		}
		this.misc = this.miscData.get("Misc");
		// TODO Find the upkeep constants inside the assets files ?????
		if (!this.misc.hasField("UpkeepUsage")) {
			this.misc.setField("UpkeepUsage", "50,80,10000,10000,10000,10000,10000,10000,10000,10000");
		}
		if (!this.misc.hasField("UpkeepGoldTax")) {
			this.misc.setField("UpkeepGoldTax", "0.00,0.30,0.60,0.60,0.60,0.60,0.60,0.60,0.60,0.60");
		}
		if (!this.misc.hasField("UpkeepLumberTax")) {
			this.misc.setField("UpkeepLumberTax", "0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00");
		}
		final Element zFogElement = this.miscData.get("DefaultZFog");
		if (zFogElement != null) {
			this.defaultFogSettings = FogSettings.parse(zFogElement, WarsmashConstants.GAME_VERSION);
		}
		final Element light = this.miscData.get("Light");
		final float lightX = light.getFieldFloatValue("Direction", 0);
		final float lightY = light.getFieldFloatValue("Direction", 1);
		final float lightZ = light.getFieldFloatValue("Direction", 2);
		this.lightDirection = new Vector3(lightX, lightY, lightZ).nor();
		this.unitGlobalStrings = new DataTable(worldEditStrings);
		try (InputStream miscDataTxtStream = this.dataSource.getResourceAsStream("Units\\UnitGlobalStrings.txt")) {
			this.unitGlobalStrings.readTXT(miscDataTxtStream, true);
		}
		final Element categories = this.unitGlobalStrings.get("Categories");
		for (final CUnitClassification unitClassification : CUnitClassification.values()) {
			if (unitClassification.getLocaleKey() != null) {
				final String displayName = categories.getField(unitClassification.getLocaleKey());
				unitClassification.setDisplayName(displayName);
			}
		}
		this.selectionCircleSizes.clear();
		final Element selectionCircleData = this.miscData.get("SelectionCircle");
		final int selectionCircleNumSizes = selectionCircleData.getFieldValue("NumSizes");
		for (int i = 0; i < selectionCircleNumSizes; i++) {
			final String indexString = i < 10 ? "0" + i : Integer.toString(i);
			final float size = selectionCircleData.getFieldFloatValue("Size" + indexString);
			final String texture = selectionCircleData.getField("Texture" + indexString);
			final String textureDotted = selectionCircleData.getField("TextureDotted" + indexString);
			this.selectionCircleSizes.add(new SelectionCircleSize(size, texture, textureDotted));
		}
		this.selectionCircleScaleFactor = selectionCircleData.getFieldFloatValue("ScaleFactor");
		this.imageWalkableZOffset = selectionCircleData.getFieldValue("ImageWalkableZOffset");
		this.selectionCircleColorFriend = parseColor(selectionCircleData, "ColorFriend");
		this.selectionCircleColorNeutral = parseColor(selectionCircleData, "ColorNeutral");
		this.selectionCircleColorEnemy = parseColor(selectionCircleData, "ColorEnemy");

		this.uiSoundsTable = new DataTable(worldEditStrings);
		try (InputStream miscDataTxtStream = this.dataSource.getResourceAsStream("UI\\SoundInfo\\UISounds.slk")) {
			this.uiSoundsTable.readSLK(miscDataTxtStream);
		}
		try (InputStream miscDataTxtStream = this.dataSource.getResourceAsStream("UI\\SoundInfo\\AmbienceSounds.slk")) {
			this.uiSoundsTable.readSLK(miscDataTxtStream);
		}
		try (InputStream miscDataTxtStream = this.dataSource.getResourceAsStream("UI\\SoundInfo\\AbilitySounds.slk")) {
			this.uiSoundsTable.readSLK(miscDataTxtStream);
		}
		// CreateMIDISound labels (day and night ambience) live here; 3.0 ships them as audio files.
		try (InputStream miscDataTxtStream = this.dataSource.getResourceAsStream("UI\\SoundInfo\\AmbientMusic.slk")) {
			if (miscDataTxtStream != null) {
				this.uiSoundsTable.readSLK(miscDataTxtStream);
			}
		}
	}

	TextTagConfig getTextTagConfig(final String key) {
		TextTagConfig textTagConfig = this.keyToTextTagConfig.get(key);
		if (textTagConfig == null) {
			textTagConfig = parseTextTagConfig(this.misc, key);
			this.keyToTextTagConfig.put(key, textTagConfig);
		}
		return textTagConfig;
	}

	private static TextTagConfig parseTextTagConfig(final Element misc, final String name) {
		final Color color = parseColor(misc, name + "TextColor");
		final String velocityKey = name + "TextVelocity";
		final float[] velocity = { misc.getFieldFloatValue(velocityKey, 0), misc.getFieldFloatValue(velocityKey, 1),
				misc.getFieldFloatValue(velocityKey, 2) };
		final float lifetime = misc.getFieldFloatValue(name + "TextLifetime");
		final float fadeStart = misc.getFieldFloatValue(name + "TextFadeStart");
		final float height = misc.getFieldFloatValue(name + "TextHeight");
		return new TextTagConfig(color, velocity, lifetime, fadeStart, height);
	}

	private void loadLightningData(final WorldEditStrings worldEditStrings) throws IOException {
		this.lightningDataTable = new DataTable(worldEditStrings);
		try (InputStream slkStream = this.dataSource.getResourceAsStream("Splats\\LightningData.slk")) {
			this.lightningDataTable.readSLK(slkStream);
		}
		this.lightningTypeToModel = new HashMap<>();
		final LightningEffectModelHandler lightningEffectModelHandler = new LightningEffectModelHandler();
		lightningEffectModelHandler.load(this);
		for (final String key : this.lightningDataTable.keySet()) {
			final War3ID typeId = War3ID.fromString(key);
			final Element element = this.lightningDataTable.get(key);
			final String textureFilePath = element.getField("Dir") + "\\" + element.getField("file");
			final float avgSegLen = element.getFieldFloatValue("AvgSegLen");
			final float width = element.getFieldFloatValue("Width");
			final float r = element.getFieldFloatValue("R");
			final float g = element.getFieldFloatValue("G");
			final float b = element.getFieldFloatValue("B");
			final float a = element.getFieldFloatValue("A");
			final float noiseScale = element.getFieldFloatValue("NoiseScale");
			final float texCoordScale = element.getFieldFloatValue("TexCoordScale");
			final float duration = element.getFieldFloatValue("Duration");
			final int version = element.getFieldValue("version");
			final LightningEffectModel lightningEffectModel = new LightningEffectModel(lightningEffectModelHandler,
					this, ".lightning", this.mapPathSolver, "<lightning:" + key + ">", typeId, textureFilePath,
					avgSegLen, width, new float[] { r / 255f, g / 255f, b / 255f, a / 255f }, noiseScale, texCoordScale,
					duration, version);
			lightningEffectModel.loadData(null, null);
			this.lightningTypeToModel.put(typeId, lightningEffectModel);
		}
	}

	private void loadWeatherData(final WorldEditStrings worldEditStrings) throws IOException {
		final DataTable weatherTable = new DataTable(worldEditStrings);
		try (InputStream slkStream = this.dataSource.getResourceAsStream("TerrainArt\\Weather.slk")) {
			if (slkStream == null) {
				this.weatherTypes = new HashMap<>();
				return;
			}
			weatherTable.readSLK(slkStream);
		}
		this.weatherTypes = new HashMap<>();
		for (final String key : weatherTable.keySet()) {
			this.weatherTypes.put(War3ID.fromString(key), new WeatherType(key, weatherTable.get(key)));
		}
	}

	public SimulationRenderComponentWeather createWeatherEffect(final War3ID weatherId, final float minX,
			final float minY, final float maxX, final float maxY) {
		final WeatherType type = this.weatherTypes == null ? null : this.weatherTypes.get(weatherId);
		if (type == null) {
			return SimulationRenderComponentWeather.DO_NOTHING;
		}
		final MdxModel model = new MdxModel((MdxHandler) findHandler(".mdx")[0], this, ".mdx", this.mapPathSolver,
				"<weather:" + type.getKey() + ">");
		model.loadData(new ByteArrayInputStream(type.buildModel().saveMdx().array()), null);
		if (!model.ok) {
			return SimulationRenderComponentWeather.DO_NOTHING;
		}
		final RenderWeatherEffect weather = new RenderWeatherEffect(this, weatherId, type, model, minX, minY, maxX,
				maxY);
		this.weatherEffects.removeIf(RenderWeatherEffect::isRemoved);
		this.weatherEffects.add(weather);
		return weather;
	}

	public CStackedSounds getStackedSounds() {
		return this.stackedSounds;
	}

	private void updateStackedSounds() {
		final Camera camera = this.worldScene.camera;
		float targetX = camera.location.x;
		float targetY = camera.location.y;
		final Vector3 forward = this.stackedSoundsForward.set(camera.directionZ).scl(-1);
		if (forward.z < -0.01f) {
			final float distance = (this.terrain.getGroundHeight(targetX, targetY) - camera.location.z) / forward.z;
			targetX += forward.x * distance;
			targetY += forward.y * distance;
		}
		this.stackedSounds.update(targetX, targetY, camera.directionX.x, camera.directionX.y);
	}

	public List<RenderWeatherEffect> getWeatherEffects() {
		return this.weatherEffects;
	}

	public com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderUbersplat createUbersplat(
			final SimulationRenderComponentUbersplat.State state) {
		final com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderUbersplat splat = new com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderUbersplat(
				this, state);
		this.scriptUbersplats.add(splat);
		return splat;
	}

	public List<com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderUbersplat> getScriptUbersplats() {
		return this.scriptUbersplats;
	}

	/** Draws a script image as it is now, or stops drawing it once destroyed. */
	public void updateImage(final CImage image) {
		RenderImage renderImage = this.scriptImages.get(image);
		if (renderImage == null) {
			if (image.isDestroyed() || !image.isValid()) {
				return;
			}
			renderImage = new RenderImage(this, image);
			this.scriptImages.put(image, renderImage);
		}
		if (!renderImage.update()) {
			this.scriptImages.remove(image);
		}
	}

	public void showTrackable(final CTrackable trackable) {
		final RenderSpellEffect model = addSpecialEffect(trackable.getModelPath(), trackable.getX(), trackable.getY(),
				(float) Math.toRadians(trackable.getFacing()));
		if (model != null) {
			this.projectiles.remove(model);
			this.trackableModels.add(model);
			this.trackableOwners.add(trackable);
		}
	}

	/**
	 * The trackable whose model is under the screen point, nearest to the camera, or null. Trackables are pointed at
	 * by their model's mesh, as the game does; a model without one is pointed at by its bounds.
	 */
	public CTrackable rayPickTrackable(final float x, final float y) {
		if (this.trackableModels.isEmpty()) {
			return null;
		}
		final float[] ray = rayHeap;
		mousePosHeap.set(x, y);
		this.worldScene.camera.screenToWorldRay(ray, mousePosHeap);
		gdxRayHeap.set(ray[0], ray[1], ray[2], ray[3] - ray[0], ray[4] - ray[1], ray[5] - ray[2]);
		gdxRayHeap.direction.nor();
		CTrackable picked = null;
		float pickedDistance = Float.MAX_VALUE;
		for (int i = 0, n = this.trackableModels.size(); i < n; i++) {
			final MdxComplexInstance instance = this.trackableModels.get(i).getModelInstance();
			if ((instance == null) || !instance.shown() || !instance.isVisible(this.worldScene.camera)) {
				continue;
			}
			final boolean hit = ((MdxModel) instance.model).geosets.isEmpty()
					? instance.intersectRayBounds(gdxRayHeap, intersectionHeap)
					: instance.intersectRayWithCollision(gdxRayHeap, intersectionHeap, true, true);
			if (hit) {
				final float distance = intersectionHeap.dst2(gdxRayHeap.origin);
				if (distance < pickedDistance) {
					pickedDistance = distance;
					picked = this.trackableOwners.get(i);
				}
			}
		}
		return picked;
	}

	private static Color parseColor(final Element selectionCircleData, final String field) {
		return new Color(selectionCircleData.getFieldFloatValue(field, 1) / 255f,
				selectionCircleData.getFieldFloatValue(field, 2) / 255f,
				selectionCircleData.getFieldFloatValue(field, 3) / 255f,
				selectionCircleData.getFieldFloatValue(field, 0) / 255f);
	}

	public GenericResource loadMapGeneric(final String path, final FetchDataTypeName dataType,
			final LoadGenericCallback callback) {
		if (this.mapMpq == null) {
			return this.loadGeneric(path, dataType, callback);
		} else {
			return this.loadGeneric(path, dataType, callback, this.dataSource);
		}
	}

	public static War3Map beginLoadingMap(final DataSource gameDataSource, final String mapFilePath)
			throws IOException {
		if (mapFilePath.startsWith("/") || !gameDataSource.has(mapFilePath)) {
			final File mapFile = new File(mapFilePath);
			if (mapFile.exists()) {
				return new War3Map(gameDataSource, mapFile);
			} else {
				throw new IllegalArgumentException("No such map file: " + mapFilePath);
			}
		}
		return new War3Map(gameDataSource, mapFilePath);
	}

	public DataTable loadWorldEditData(final War3Map map) {
		final StandardObjectData standardObjectData = new StandardObjectData(map);
		this.worldEditData = standardObjectData.getWorldEditData();
		return this.worldEditData;
	}

	public WTS preloadWTS(final War3Map map) {
		try {
			this.preloadedWTS = Warcraft3MapObjectData.loadWTS(map);
			return this.preloadedWTS;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void dispose() {
		closeReplaySession();
		if (this.terrain != null) {
			this.terrain.dispose();
		}
		this.dynamicShadowManager.dispose();
		if (this.lightningTypeToModel != null) {
			for (final LightningEffectModel model : this.lightningTypeToModel.values()) {
				model.dispose();
			}
		}
		super.dispose();
		try {
			this.dataSource.close();
		}
		catch (final IOException e) {
			System.err.println("Could not close the data of the finished map: " + e.getMessage());
		}
		CUnit.releaseStaticReferences();
		CWidgetEvent.releaseStaticReferences();
		CGlobalWidgetEvent.releaseStaticReferences();
		ArithmeticJassValueVisitor.INSTANCE.reset(null, null);
		ArithmeticLeftHandHandleJassValueVisitor.INSTANCE.reset(null, null);
		RecordingAbilityTargetCheckReceiver.getInstance().reset();
		ExternStringMsgTargetCheckReceiver.getInstance().reset();
		COrder.targetCheckReceiver.reset();
		ArithmeticLeftHandStructJassValueVisitor.INSTANCE.reset(null, null);
		ArithmeticLeftHandCodeJassValueVisitor.INSTANCE.reset(null, null);
		CUnitAttackMissileBounce.releaseStaticReferences();
		CUnitAttackMissileSplash.releaseStaticReferences();
		CommandCardPopulatingAbilityVisitor.INSTANCE.release();
		GetInstantTransformationBuffVisitor.getInstance().release();
		BuildOnBuildingIntersector.INSTANCE.reset(0, 0);
		TreeBlightingCallback.INSTANCE.reset(null);
		CWidgetAbilityTargetCheckReceiver.INSTANCE.reset();
		this.soundMixer.release();
		this.stackedSounds.stopAll();
	}

	public MapLoader createMapLoader(final War3Map war3Map, final War3MapW3i w3iFile, final int localPlayerIndex)
			throws IOException {
		this.localPlayerIndex = localPlayerIndex;
		this.mapMpq = war3Map;
		this.pathingTextures = new CPathingTextures(war3Map);
		this.lastLoadedMapInformation = w3iFile;

		return new MapLoader(war3Map, w3iFile, localPlayerIndex);
	}

	public SimulationRenderComponentLightning createLightning(final War3ID lightningId,
			final RenderUnit renderPeerSource, final RenderWidget renderPeerTarget) {
		return this.createLightning(lightningId, renderPeerSource, renderPeerTarget, null);
	}

	public SimulationRenderComponentLightning createLightning(final War3ID lightningId,
			final RenderUnit renderPeerSource, final RenderWidget renderPeerTarget, final Float duration) {
		final LightningEffectModel lightningEffectModel = this.lightningTypeToModel.get(lightningId);
		if ((lightningEffectModel != null) && (renderPeerSource != null) && (renderPeerTarget != null)) {
			final LightningEffectNode source = (LightningEffectNode) lightningEffectModel.addInstance();
			final LightningEffectNode target = (LightningEffectNode) lightningEffectModel.addInstance();
			// ----- NOTE -----
			// Automatic ingame lightnings apply their builtin durations below. For user
			// code, we DONT do the same
			// on the other arbitrary (non-unit-bound) createLightning func.
			// Later on we might want a user API for creating unit-attached lightnings that
			// have a duration specified
			// by user code
			if (duration != null) {
				if (duration >= 0) {
					source.setLifeSpanRemaining(duration);
					target.setLifeSpanRemaining(duration);
				}
			} else {
				source.setLifeSpanRemaining(lightningEffectModel.getDuration());
				target.setLifeSpanRemaining(lightningEffectModel.getDuration());
			}
			source.setFriend(target);
			target.setFriend(source);
			source.setSource(true);
			source.setParent(renderPeerSource.getInstance());
			source.setLocation(0, 0, this.simulation.getUnitData()
					.getProjectileLaunchZ(renderPeerSource.getSimulationUnit().getTypeId()));
			target.setParent(renderPeerTarget.getInstance());
			target.setLocation(0, 0, renderPeerTarget.getSimulationWidget().getImpactZ());
			source.setScene(this.worldScene);
			target.setScene(this.worldScene);
			return addLightning(new RenderLightningEffect(source, target, this, lightningId,
					renderPeerSource.getSimulationUnit(), renderPeerTarget.getSimulationWidget(), 0, 0, 0, 0, 0, 0));
		}
		return SimulationRenderComponentLightning.DO_NOTHING;
	}

	public SimulationRenderComponentLightningMovable createLightning(final War3ID lightningId, final float x1,
			final float y1, final float z1, final float x2, final float y2, final float z2) {
		final LightningEffectModel lightningEffectModel = this.lightningTypeToModel.get(lightningId);
		if (lightningEffectModel != null) {
			final LightningEffectNode source = (LightningEffectNode) lightningEffectModel.addInstance();
			final LightningEffectNode target = (LightningEffectNode) lightningEffectModel.addInstance();
			source.setFriend(target);
			target.setFriend(source);
			source.setSource(true);
			source.setLocation(x1, y1, z1);
			target.setLocation(x2, y2, z2);
			source.setScene(this.worldScene);
			target.setScene(this.worldScene);
			return addLightning(
					new RenderLightningEffect(source, target, this, lightningId, null, null, x1, y1, z1, x2, y2, z2));
		}
		return SimulationRenderComponentLightningMovable.DO_NOTHING;
	}

	private RenderLightningEffect addLightning(final RenderLightningEffect lightning) {
		this.lightnings.removeIf(RenderLightningEffect::isRemoved);
		this.lightnings.add(lightning);
		return lightning;
	}

	public List<RenderLightningEffect> getLightnings() {
		return this.lightnings;
	}

	public void spawnFxOnOrigin(final RenderUnit renderUnit, final String heroLevelUpArt) {
		final MdxModel heroLevelUpModel = loadModelMdx(heroLevelUpArt);
		if (heroLevelUpModel != null) {
			final MdxComplexInstance modelInstance = (MdxComplexInstance) heroLevelUpModel.addInstance();
			modelInstance.setTeamColor(renderUnit.playerIndex);

			final MdxModel model = (MdxModel) renderUnit.instance.model;
			int index = -1;
			for (int i = 0; i < model.attachments.size(); i++) {
				final Attachment attachment = model.attachments.get(i);
				if (attachment.getName().startsWith("origin ref")) {
					index = i;
					break;
				}
			}
			if ((index != -1) && false) {
				final MdxNode attachment = renderUnit.instance.getAttachment(index);
				modelInstance.setParent(attachment);
			} else {
				modelInstance.setLocation(renderUnit.location);
			}

			modelInstance.setScene(War3MapViewer.this.worldScene);
			SequenceUtils.randomBirthSequence(modelInstance);
			War3MapViewer.this.projectiles.add(new RenderAttackInstant(modelInstance, War3MapViewer.this,
					(float) Math.toRadians(renderUnit.getSimulationUnit().getFacing())));
		}
	}

	protected ArgbImage getDestructablePathingPixelMap(final GameObject row) {
		return loadPathingTexture(row.getFieldAsString(DESTRUCTABLE_PATHING, 0));
	}

	protected ArgbImage getDestructablePathingDeathPixelMap(final GameObject row) {
		return loadPathingTexture(row.getFieldAsString(DESTRUCTABLE_PATHING_DEATH, 0));
	}

	private void loadSounds() {
		this.uiSounds = new KeyedSounds(this.uiSoundsTable, this.mapMpq);
	}

	/**
	 * Loads the map information that should be loaded after UI, such as units, who
	 * need to be able to setup their UI counterparts (icons, etc) for their
	 * abilities while loading. This allows the dynamic creation of units while the
	 * game is playing to better share code with the startup sequence's creation of
	 * units.
	 *
	 * @throws IOException
	 */
	public void loadAfterUI() throws IOException {
		if (this.unitsAndItemsLoaded) {
			loadUnitsAndItems(this.allObjectData, this.lastLoadedMapInformation);
		} else {
			throw new IllegalStateException("transcription of JS has not loaded a map and has no JS async promises");
		}

		// After we finish loading units, we need to update & create the stored shadow
		// information for all unit shadows
		this.terrain.initShadows();
		this.terrain.setFogOfWarData(this.simulation, this.simulation.getPlayer(this.localPlayerIndex).getFogOfWar());
		final CTimer fogUpdateTimer = new IdleFogTimer();
		fogUpdateTimer.setTimeoutTime(1.0f);
		fogUpdateTimer.setRepeats(true);
		fogUpdateTimer.start(this.simulation);
		final CTimer fogGpuUpdateTimer = new FogGpuUpdateTimer(this);
		fogGpuUpdateTimer.setTimeoutTime(0.03f);
		fogGpuUpdateTimer.setRepeats(true);
		fogGpuUpdateTimer.start(this.simulation);
	}

	private static final class IdleFogTimer extends CTimer {
		@Override
		public void onFire(final CSimulation simulation) {
		}
	}

	private static final class FogGpuUpdateTimer extends CTimer {
		@SavedFormerly("this$0")
		private final War3MapViewer viewer;

		private FogGpuUpdateTimer(final War3MapViewer viewer) {
			this.viewer = viewer;
		}

		@Override
		public void onFire(final CSimulation simulation) {
			final boolean fogChanged = this.viewer.terrain.reloadFogOfWarDataToGPU(simulation);
			// Doodads fade with the ground fog, so they only need the step while it changes; once a second they are
			// checked anyway.
			if (fogChanged || ((++this.viewer.fogDoodadTicks % 30) == 0)) {
				for (final RenderDoodad doodad : this.viewer.decals) {
					doodad.updateFog(this.viewer);
				}
			}
		}
	}

	private void loadDoodadsAndDestructibles(final Warcraft3MapRuntimeObjectData modifications,
			final War3MapW3i w3iFile) throws IOException {
		applyModificationFile(this.doodadsData, this.doodadMetaData, modifications.getDoodads(),
				WorldEditorDataType.DOODADS);
		applyModificationFile(this.doodadsData, this.destructableMetaData, modifications.getDestructibles(),
				WorldEditorDataType.DESTRUCTIBLES);

		final War3MapDoo doo = this.mapMpq.readDoodads(w3iFile);

		for (final com.etheller.warsmash.parsers.w3x.doo.Doodad doodad : doo.getDoodads()) {
			if ((doodad.getFlags() & 0x2) == 0) {
				continue;
			}
			final War3ID doodadId = doodad.getId();
			final int doodadVariation = doodad.getVariation();
			final float[] location = doodad.getLocation();
			final float facingRadians = doodad.getAngle();
			final short lifePercent = doodad.getLife();
			final float[] scale = doodad.getScale();
			createDestructableOrDoodad(doodadId, modifications, doodadVariation, location, facingRadians, lifePercent,
					scale);
		}

		// Cliff/Terrain doodads.
		for (final com.etheller.warsmash.parsers.w3x.doo.TerrainDoodad doodad : doo.getTerrainDoodads()) {
			final GameObject row = modifications.getDoodads().get(doodad.getId());
			String file = row.readSLKTag("file");//
			if ("".equals(file)) {
				final String blaBla = row.readSLKTag("file");
				System.out.println("bla");
			}
			if (file.toLowerCase().endsWith(".mdl")) {
				file = file.substring(0, file.length() - 4);
			}
			if (!file.toLowerCase().endsWith(".mdx")) {
				file += ".mdx";
			}
			final MdxModel model = (MdxModel) load(file, this.mapPathSolver, this.solverParams);

			final String pathingTexture = row.readSLKTag("pathTex");
			ArgbImage pathingTextureImage;
			if ((pathingTexture != null) && (pathingTexture.length() > 0) && !"_".equals(pathingTexture)) {

				pathingTextureImage = this.filePathToPathingMap.get(pathingTexture.toLowerCase());
				if (pathingTextureImage == null) {
					if (this.mapMpq.has(pathingTexture)) {
						try {
							pathingTextureImage = TgaFile.readTGAPixels(pathingTexture,
									this.mapMpq.getResourceAsStream(pathingTexture));
							this.filePathToPathingMap.put(pathingTexture.toLowerCase(), pathingTextureImage);
						} catch (final Exception exc) {
							exc.printStackTrace();
						}
					}
				}
			} else {
				pathingTextureImage = null;
			}
			if (pathingTextureImage != null) {
				// blit out terrain cells under this TerrainDoodad
				final int textureWidth = pathingTextureImage.getWidth();
				final int textureHeight = pathingTextureImage.getHeight();
				final int textureWidthTerrainCells = textureWidth / 4;
				final int textureHeightTerrainCells = textureHeight / 4;
				final int minCellX = (int) doodad.getLocation()[0];
				final int minCellY = (int) doodad.getLocation()[1];
				final int maxCellX = (minCellX + textureWidthTerrainCells) - 1;
				final int maxCellY = (minCellY + textureHeightTerrainCells) - 1;
				for (int j = minCellY; j <= maxCellY; j++) {
					for (int i = minCellX; i <= maxCellX; i++) {
						this.terrain.removeTerrainCellWithoutFlush(i, j);
						this.simulation.getTerrainData().hideCell(i, j);
					}
				}
				this.terrain.flushRemovedTerrainCells();
				this.simulation.getTerrainData().flushHiddenCells();
			}

			System.out.println("Loading terrain doodad: " + file);
			this.terrainDoodads.add(new TerrainDoodad(this, model, row, doodad, pathingTextureImage));
		}

		this.doodadsReady = true;
		this.anyReady = true;
	}

	private void createDoodad(final GameObject row, final int doodadVariation, final float[] location,
			final float facingRadians, final float[] scale) {
		final MdxModel model = getDoodadModel(doodadVariation, row);
		final float maxPitch = row.readSLKTagFloat("maxPitch");
		final float maxRoll = row.readSLKTagFloat("maxRoll");
		final float defScale = row.readSLKTagFloat("defScale");
		final RenderDoodad renderDoodad = new RenderDoodad(this, model, row, location, scale, facingRadians, maxPitch,
				maxRoll, defScale, doodadVariation);
		renderDoodad.instance.uniformScale(defScale);
		this.doodads.add(renderDoodad);
		this.decals.add(renderDoodad);
	}

	// Measured on 1.29: a destructable with a fixed rotation (gates, pathing blockers) ignores the facing it is
	// created with, and its pathing keeps the fixed orientation.
	private static float getDestructableFacing(final GameObject row, final float requestedFacingRadians) {
		final String fixedRotation = row.readSLKTag("fixedRot");
		if ((fixedRotation != null) && !fixedRotation.isEmpty()) {
			try {
				final float degrees = Float.parseFloat(fixedRotation.trim());
				if (degrees >= 0) {
					return (float) Math.toRadians(degrees);
				}
			}
			catch (final NumberFormatException e) {
				// not a fixed rotation
			}
		}
		return requestedFacingRadians;
	}

	// Measured on 1.29: a destructable's pathing map turns only in quarter turns, to the one nearest its facing
	// (45 degrees takes 90, 269.0 takes 270).
	private static int getPathingRotation(final float facingRadians) {
		final int quarterTurns = Math.round((float) Math.toDegrees(facingRadians) / 90f);
		return (((quarterTurns * 90) % 360) + 360) % 360;
	}

	RenderDestructable createNewDestructable(final War3ID doodadId, final GameObject row,
			final int doodadVariation, final float[] location, final float requestedFacingRadians,
			final short lifePercent, final float[] scale) {
		final float facingRadians = getDestructableFacing(row, requestedFacingRadians);
		BuildingShadow destructableShadow = null;
		RemovablePathingMapInstance destructablePathing = null;
		RemovablePathingMapInstance destructablePathingDeath = null;
		final MdxModel model = getDoodadModel(doodadVariation, row);

		final String portraitModelPath = row.readSLKTag("portraitmodel");
		final MdxModel portraitModel = loadModelMdx(portraitModelPath);

		final float maxPitch = row.readSLKTagFloat("maxPitch");
		final float maxRoll = row.readSLKTagFloat("maxRoll");
		final String shadowString = row.readSLKTag("shadow");
		if ((shadowString != null) && (shadowString.length() > 0) && !"_".equals(shadowString)
				&& !"none".equalsIgnoreCase(shadowString)) {
			destructableShadow = this.terrain.addShadow(shadowString, location[0], location[1]);
		}

		final ArgbImage destructablePathingPixelMap = getDestructablePathingPixelMap(row);
		if (destructablePathingPixelMap != null) {
			destructablePathing = this.terrain.pathingGrid.createRemovablePathingOverlayTexture(location[0],
					location[1], getPathingRotation(facingRadians), destructablePathingPixelMap);
			if (lifePercent > 0) {
				destructablePathing.add();
			}
		}
		final ArgbImage destructablePathingDeathPixelMap = getDestructablePathingDeathPixelMap(row);
		if (destructablePathingDeathPixelMap != null) {
			destructablePathingDeath = this.terrain.pathingGrid.createRemovablePathingOverlayTexture(location[0],
					location[1], getPathingRotation(facingRadians), destructablePathingDeathPixelMap);
			if (lifePercent <= 0) {
				destructablePathingDeath.add();
			}
		}
		final float x = location[0];
		final float y = location[1];
		final CDestructable simulationDestructable = this.simulation.internalCreateDestructable(
				War3ID.fromString(row.getId()), x, y, destructablePathing, destructablePathingDeath);
		// Used to be this, but why: (float) Math.sqrt((scale[0]) * (scale[1]) *
		// (scale[2]));
		final float selectionScale = 1.0f;
		simulationDestructable.setLife(this.simulation, simulationDestructable.getLife() * (lifePercent / 100f));
		final RenderDestructable renderDestructable = new RenderDestructable(this, model, portraitModel, row, location,
				scale, facingRadians, selectionScale, maxPitch, maxRoll, lifePercent, destructableShadow,
				simulationDestructable, doodadVariation);
		if (row.readSLKTagBoolean("walkable")) {
			final float angle = facingRadians;
			BoundingBox boundingBox = model.bounds.getBoundingBox();
			if (boundingBox == null) {
				// TODO this is a hack and should be fixed later
				boundingBox = new BoundingBox(new Vector3(-10, -10, 0), new Vector3(10, 10, 0));
			}
			final Rectangle renderDestructableBounds = getRotatedBoundingBox(x, y, angle, boundingBox);
			this.walkableObjectsTree.add((MdxComplexInstance) renderDestructable.instance, renderDestructableBounds);
			renderDestructable.walkableBounds = renderDestructableBounds;
		}
		this.widgets.add(renderDestructable);
		this.decals.add(renderDestructable);
		this.destructableToRenderPeer.put(simulationDestructable, renderDestructable);
		return renderDestructable;
	}

	private void createDestructableOrDoodad(final War3ID doodadId, final Warcraft3MapRuntimeObjectData modifications,
			final int doodadVariation, final float[] location, final float facingRadians, final short lifePercent,
			final float[] scale) {
		GameObject row = modifications.getDoodads().get(doodadId);
		if (row == null) {
			row = modifications.getDestructibles().get(doodadId);
			if (row != null) {
				createNewDestructable(doodadId, row, doodadVariation, location, facingRadians, lifePercent, scale);
			}
		} else {
			createDoodad(row, doodadVariation, location, facingRadians, scale);
		}
	}

	private MdxModel getDoodadModel(final int doodadVariation, final GameObject row) {
		String file = row.readSLKTag("file");
		final int numVar = row.readSLKTagInt("numVar");

		if (file.endsWith(".mdx") || file.endsWith(".mdl")) {
			file = file.substring(0, file.length() - 4);
		}

		String fileVar = file;

		file += ".mdx";

		if (numVar > 1) {
			fileVar += Math.min(doodadVariation, numVar - 1);
		}

		fileVar += ".mdx";
		// First see if the model is local.
		// Doodads referring to local models may have invalid variations, so if the
		// variation doesn't exist, try without a variation.

		String path;
		if (this.mapMpq.has(fileVar)) {
			path = fileVar;
		} else {
			path = file;
		}
		MdxModel model;
		if (this.mapMpq.has(path)) {
			model = (MdxModel) load(path, this.mapPathSolver, this.solverParams);
		} else {
			model = (MdxModel) load(fileVar, this.mapPathSolver, this.solverParams);
		}
		return model;
	}

	private Rectangle getRotatedBoundingBox(final float x, final float y, final float angle,
			final BoundingBox boundingBox) {
		final float x1 = boundingBox.min.x;
		final float y1 = boundingBox.min.y;
		final float x2 = boundingBox.min.x + boundingBox.getWidth();
		final float y2 = boundingBox.min.y;
		final float x3 = boundingBox.min.x + boundingBox.getWidth();
		final float y3 = boundingBox.min.y + boundingBox.getHeight();
		final float x4 = boundingBox.min.x;
		final float y4 = boundingBox.min.y + boundingBox.getHeight();
		final float angle1 = (float) StrictMath.atan2(y1, x1) + angle;
		final float len1 = (float) StrictMath.sqrt((x1 * x1) + (y1 * y1));
		final float angle2 = (float) StrictMath.atan2(y2, x2) + angle;
		final float len2 = (float) StrictMath.sqrt((x2 * x2) + (y2 * y2));
		final float angle3 = (float) StrictMath.atan2(y3, x3) + angle;
		final float len3 = (float) StrictMath.sqrt((x3 * x3) + (y3 * y3));
		final float angle4 = (float) StrictMath.atan2(y4, x4) + angle;
		final float len4 = (float) StrictMath.sqrt((x4 * x4) + (y4 * y4));
		final double x1prime = StrictMath.cos(angle1) * len1;
		final double x2prime = StrictMath.cos(angle2) * len2;
		final double x3prime = StrictMath.cos(angle3) * len3;
		final double x4prime = StrictMath.cos(angle4) * len4;
		final double y1prime = StrictMath.sin(angle1) * len1;
		final double y2prime = StrictMath.sin(angle2) * len2;
		final double y3prime = StrictMath.sin(angle3) * len3;
		final double y4prime = StrictMath.sin(angle4) * len4;
		final float minX = (float) StrictMath.min(StrictMath.min(x1prime, x2prime), StrictMath.min(x3prime, x4prime));
		final float minY = (float) StrictMath.min(StrictMath.min(y1prime, y2prime), StrictMath.min(y3prime, y4prime));
		final float maxX = (float) StrictMath.max(StrictMath.max(x1prime, x2prime), StrictMath.max(x3prime, x4prime));
		final float maxY = (float) StrictMath.max(StrictMath.max(y1prime, y2prime), StrictMath.max(y3prime, y4prime));
		return new Rectangle(x + minX, y + minY, maxX - minX, maxY - minY);
	}

	private void applyModificationFile(final MappedData doodadsData2, final MappedData doodadMetaData2,
			final ObjectData destructibles, final WorldEditorDataType dataType) {
		// TODO condense ported MappedData from Ghostwolf and MutableObjectData from
		// Retera

	}

	private void loadUnitsAndItems(final Warcraft3MapRuntimeObjectData modifications, final War3MapW3i mapInformation)
			throws IOException {
		final War3Map mpq = this.mapMpq;
		this.unitsReady = false;
		this.renderUnitTypeData = new RenderUnitTypeData(modifications.getUnits(), mpq, this, this.unitAckSoundsTable,
				this.terrain.uberSplatTable);
		this.renderItemTypeData = new RenderItemTypeData(modifications.getItems(), mpq, this, this.miscData);

		if (this.dataSource.has("war3mapUnits.doo") && WarsmashConstants.LOAD_UNITS_FROM_WORLDEDIT_DATA) {
			final War3MapUnitsDoo dooFile = mpq.readUnits(mapInformation);

			// Collect the units and items data.
			for (final com.etheller.warsmash.parsers.w3x.unitsdoo.Unit unit : dooFile.getUnits()) {
				final War3ID unitId = unit.getId();
				final float unitX = unit.getLocation()[0];
				final float unitY = unit.getLocation()[1];
				final float unitZ = unit.getLocation()[2];
				final int playerIndex = unit.getPlayer();
				final int customTeamColor = unit.getCustomTeamColor();
				final float unitAngle = unit.getAngle();
				final int editorConfigHitPointPercent = unit.getHitpoints();
				final int editorConfigManaAmount = unit.getMana();

				final CWidget widgetCreated = createNewUnit(modifications, unitId, unitX, unitY, playerIndex,
						customTeamColor, unitAngle, editorConfigHitPointPercent, editorConfigManaAmount);
				if (widgetCreated instanceof CUnit) {
					final CUnit unitCreated = (CUnit) widgetCreated;
					if (unit.getGoldAmount() != 0) {
						unitCreated.setGold(unit.getGoldAmount());
					}
				}
			}
		}
		this.simulation.unitsLoaded();

		this.terrain.loadSplats();

		this.unitsReady = true;
		this.anyReady = true;
	}

	CWidget createNewUnit(final Warcraft3MapRuntimeObjectData modifications, final War3ID unitId, float unitX,
			float unitY, final int playerIndex, int customTeamColor, final float unitAngle) {
		return createNewUnit(modifications, unitId, unitX, unitY, playerIndex, customTeamColor, unitAngle, -1, -1);
	}

	CWidget createNewUnit(final Warcraft3MapRuntimeObjectData modifications, final War3ID unitId, float unitX,
			float unitY, final int playerIndex, int customTeamColor, final float unitAngle,
			int editorConfigHitPointPercent, int editorConfigManaAmount) {
		Splat buildingUberSplat = null;
		SplatMover buildingUberSplatDynamicIngame = null;
		ArgbImage buildingPathingPixelMap = null;
		BuildingShadow buildingShadowInstance = null;

		// Hardcoded?
		if (sloc.equals(unitId)) {
			// path = "Objects\\StartLocation\\StartLocation.mdx";
			this.startLocations[playerIndex] = new Vector2(unitX, unitY);
		} else {
			final RenderUnitType renderUnitType = this.renderUnitTypeData.get(unitId);
			if (renderUnitType == null) {
				final RenderItemType renderItemType = this.renderItemTypeData.get(unitId);
				if (renderItemType != null) {

					final CItem simulationItem = this.simulation.internalCreateItem(unitId, unitX, unitY);
					final float unitZ = Math.max(getWalkableRenderHeight(unitX, unitY),
							War3MapViewer.this.terrain.getGroundHeight(unitX, unitY));
					final RenderItem renderItem = new RenderItem(this, renderItemType, unitX, unitY, unitZ, unitAngle,
							simulationItem);
					this.widgets.add(renderItem);
					this.itemToRenderPeer.put(simulationItem, renderItem);

					renderItem.shadow = createUnitShadow(renderItemType.getRenderShadowType(), unitX, unitY);
					return simulationItem;

				} else {
					System.err.println("Unknown unit ID: " + unitId);
				}
			} else {
				buildingPathingPixelMap = renderUnitType.getBuildingPathingPixelMap();

				final float angle = (float) Math.toDegrees(unitAngle);
				final CUnit simulationUnit = this.simulation.internalCreateUnit(unitId, playerIndex, unitX, unitY,
						angle, buildingPathingPixelMap, editorConfigHitPointPercent, editorConfigManaAmount);
				unitX = simulationUnit.getX();
				unitY = simulationUnit.getY();
				if (!renderUnitType.isAllowCustomTeamColor() || (customTeamColor == -1)) {
					if (renderUnitType.getTeamColor() != -1) {
						customTeamColor = renderUnitType.getTeamColor();
					} else {
						customTeamColor = this.simulation.getPlayer(playerIndex).getColor();
					}
				}
				final float unitZ = Math.max(getWalkableRenderHeight(unitX, unitY),
						War3MapViewer.this.terrain.getGroundHeight(unitX, unitY)) + simulationUnit.getFlyHeight();

				final String texturePath = renderUnitType.getUberSplat();
				final float s = renderUnitType.getUberSplatScaleValue();
				if (texturePath != null) {
					if (this.unitsReady) {
						buildingUberSplatDynamicIngame = addUberSplatIngame(unitX, unitY, texturePath, s);
					} else {
						if (!this.terrain.splats.containsKey(texturePath)) {
							this.terrain.splats.put(texturePath, new Splat());
						}
						final float x = unitX;
						final float y = unitY;
						buildingUberSplat = this.terrain.splats.get(texturePath);
						buildingUberSplat.locations.add(new float[] { x - s, y - s, x + s, y + s, 1 });
					}
				}

				final String buildingShadow = renderUnitType.getBuildingShadow();
				if (buildingShadow != null) {
					buildingShadowInstance = this.terrain.addShadow(buildingShadow, unitX, unitY);
				}

				final RenderUnit renderUnit = new RenderUnit(this, unitX, unitY, unitZ, customTeamColor, simulationUnit,
						renderUnitType, buildingShadowInstance, this.selectionCircleScaleFactor,
						renderUnitType.getAnimationWalkSpeed(), renderUnitType.getAnimationRunSpeed(),
						renderUnitType.getScalingValue());
				this.unitToRenderPeer.put(simulationUnit, renderUnit);
				this.widgets.add(renderUnit);
				this.units.add(renderUnit);
				renderUnit.shadow = createUnitShadow(renderUnitType.getRenderShadowType(), unitX, unitY);
				if (buildingUberSplat != null) {
					buildingUberSplat.unitMapping.add(new Consumer<SplatModel.SplatMover>() {

						@Override
						public void accept(final SplatMover t) {
							renderUnit.uberSplat = t;
						}
					});
				}
				if (buildingUberSplatDynamicIngame != null) {
					renderUnit.uberSplat = buildingUberSplatDynamicIngame;
				}
				return simulationUnit;

			}
		}
		return null;
	}

	SplatMover createUnitShadow(final RenderShadowType renderShadowType, final float unitX, final float unitY) {
		if (renderShadowType != null) {
			final float x = unitX - renderShadowType.getX();
			final float y = unitY - renderShadowType.getY();
			return this.terrain.addUnitShadowSplat(renderShadowType.getTexture(), x, y, x + renderShadowType.getWidth(),
					y + renderShadowType.getHeight(), 3, 0.5f, false);
		}
		return null;
	}

	public SplatMover addUberSplatIngame(final float unitX, final float unitY, final String texturePath,
			final float s) {
		SplatMover buildingUberSplatDynamicIngame;
		buildingUberSplatDynamicIngame = this.terrain.addUberSplat(texturePath, unitX, unitY, 1, s, false, false, false,
				false);
		return buildingUberSplatDynamicIngame;
	}

	@Override
	public ArgbImage loadPathingTexture(final String pathingTexture) {
		return this.pathingTextures.get(pathingTexture);
	}

	public RenderUnitType getUnitTypeData(final War3ID key) {
		return this.renderUnitTypeData.get(key);
	}

	/**
	 * The game stops while paused (a script, the Pause button, or the game menu in a single player game); the camera,
	 * the interface and rendering go on.
	 */
	public void setGamePaused(final PauseSource source, final boolean paused) {
		if (paused) {
			this.pauseSources.add(source);
		}
		else {
			this.pauseSources.remove(source);
		}
	}

	public boolean isGamePaused() {
		return !this.pauseSources.isEmpty();
	}

	public boolean isGamePaused(final PauseSource source) {
		return this.pauseSources.contains(source);
	}

	/**
	 * Scripts ask in the middle of a thread, and the menu in the middle of a frame; the game state is only whole
	 * between two simulation steps.
	 */
	public void requestSaveGame(final FileHandle file) {
		this.pendingSave = file;
	}

	public void requestLoadGame(final FileHandle file) {
		requestLoadGame(file, false);
	}

	/** byScript: a map's LoadGame (zone travel), which the loaded map tells apart from a player's load. */
	public void requestLoadGame(final FileHandle file, final boolean byScript) {
		this.pendingLoad = file;
		this.pendingLoadByScript = byScript;
	}

	/**
	 * SaveAndChangeLevelBJ is SaveGame then ChangeLevel in one thread; leaving the map at once would drop the save
	 * still waiting for the end of the step, so the level changes after it is written.
	 */
	public void runAfterPendingSave(final Runnable action) {
		if (this.pendingSave == null) {
			action.run();
		}
		else {
			this.afterPendingSave = action;
		}
	}

	public void setLoadGameListener(final LoadGameListener loadGameListener) {
		this.loadGameListener = loadGameListener;
	}

	private void runPendingSaveRequests() {
		if (this.pendingSave != null) {
			final FileHandle file = this.pendingSave;
			this.pendingSave = null;
			// The recorded game made this save; a playback goes on as if it had, without writing over the player's.
			final boolean saved = isReplayPlayback() || SavedGame.save(this, file);
			if (this.saveGameListener != null) {
				this.saveGameListener.saveFinished(file, saved);
			}
			if (this.afterPendingSave != null) {
				final Runnable action = this.afterPendingSave;
				this.afterPendingSave = null;
				action.run();
				return;
			}
		}
		if ((this.pendingLoad != null) && (this.loadGameListener != null)) {
			final FileHandle file = this.pendingLoad;
			this.pendingLoad = null;
			this.loadGameListener.loadRequested(file, this.pendingLoadByScript);
		}
	}

	public void setSaveGameListener(final SaveGameListener saveGameListener) {
		this.saveGameListener = saveGameListener;
	}

	public interface SaveGameListener {
		void saveFinished(FileHandle file, boolean saved);
	}

	public interface LoadGameListener {
		void loadRequested(FileHandle file, boolean byScript);
	}

	public SavedInterfaceState captureInterfaceState() {
		final SavedInterfaceState state = this.interfaceStateKeeper == null ? new SavedInterfaceState()
				: this.interfaceStateKeeper.captureInterfaceState();
		state.dayNightTerrainModel = this.dayNightTerrainModel;
		state.dayNightUnitModel = this.dayNightUnitModel;
		state.skyModel = this.skyModel;
		final FogSettings fog = this.worldScene.fogSettings;
		state.fogStyle = fog.style.ordinal();
		state.fogColor = new float[] { fog.color.r, fog.color.g, fog.color.b, fog.color.a };
		state.fogDensity = fog.density;
		state.fogStart = fog.start;
		state.fogEnd = fog.end;
		state.waterColor = this.waterColor;
		state.allyColorFilterState = getAllyColorFilter().getState();
		state.restartMissionDisabled = this.restartMissionDisabled;
		state.altMinimapIcon = this.altMinimapIcon;
		state.volumeGroupScales = new float[CSoundVolumeGroup.VALUES.length];
		for (final CSoundVolumeGroup group : CSoundVolumeGroup.VALUES) {
			state.volumeGroupScales[group.ordinal()] = this.soundMixer.getGroupScale(group);
		}
		return state;
	}

	public void restoreInterfaceState(final SavedInterfaceState state) {
		if (state.dayNightTerrainModel != null) {
			setDayNightModels(state.dayNightTerrainModel, state.dayNightUnitModel);
		}
		if (state.skyModel != null) {
			setSkyModel(state.skyModel);
		}
		if (state.fogColor != null) {
			final FogSettings fog = this.worldScene.fogSettings;
			fog.setStyleByIndex(state.fogStyle);
			fog.color = new Color(state.fogColor[0], state.fogColor[1], state.fogColor[2], state.fogColor[3]);
			fog.density = state.fogDensity;
			fog.start = state.fogStart;
			fog.end = state.fogEnd;
		}
		if (state.waterColor != null) {
			setWaterBaseColor(state.waterColor);
		}
		getAllyColorFilter().setState(state.allyColorFilterState);
		this.restartMissionDisabled = state.restartMissionDisabled;
		this.altMinimapIcon = state.altMinimapIcon;
		final float[] scales = state.volumeGroupScales;
		for (final CSoundVolumeGroup group : CSoundVolumeGroup.VALUES) {
			this.soundMixer.setGroupScale(group,
					(scales != null) && (group.ordinal() < scales.length) ? scales[group.ordinal()] : 1f);
		}
		if (this.interfaceStateKeeper != null) {
			this.interfaceStateKeeper.restoreInterfaceState(state);
		}
	}

	public void setInterfaceStateKeeper(final InterfaceStateKeeper keeper) {
		this.interfaceStateKeeper = keeper;
	}

	public void addLoadedScript(final SavedGame.AiEntry script) {
		this.loadedScripts.add(script);
	}

	public List<SavedGame.AiEntry> getLoadedScripts() {
		return this.loadedScripts;
	}

	public void setScriptEnvironment(final Object scriptEnvironment, final AudioContext uiAudioContext) {
		this.scriptEnvironment = scriptEnvironment;
		this.uiAudioContext = uiAudioContext;
	}

	public Object getScriptEnvironment() {
		return this.scriptEnvironment;
	}

	public AudioContext getUiAudioContext() {
		return this.uiAudioContext;
	}

	public void rememberFileSound(final Sound sound, final String fileName) {
		if (sound != null) {
			this.fileSounds.put(sound, fileName);
		}
	}

	public String getFileSoundName(final Sound sound) {
		return this.fileSounds.get(sound);
	}

	public interface InterfaceStateKeeper {
		SavedInterfaceState captureInterfaceState();

		void restoreInterfaceState(SavedInterfaceState state);
	}

	/** How a destructable the game made after the map loaded was made; saved games start again from the map. */
	public static final class CreatedDestructable {
		public final int savedHandleId;
		private final int typeId;
		private final float x;
		private final float y;
		private final float z;
		private final float facing;
		private final float scale;
		private final int variation;

		CreatedDestructable(final int savedHandleId, final int typeId, final float x, final float y, final float z,
				final float facing, final float scale, final int variation) {
			this.savedHandleId = savedHandleId;
			this.typeId = typeId;
			this.x = x;
			this.y = y;
			this.z = z;
			this.facing = facing;
			this.scale = scale;
			this.variation = variation;
		}

		public void write(final DataOutputStream out) throws IOException {
			out.writeInt(this.savedHandleId);
			out.writeInt(this.typeId);
			out.writeFloat(this.x);
			out.writeFloat(this.y);
			out.writeFloat(this.z);
			out.writeFloat(this.facing);
			out.writeFloat(this.scale);
			out.writeInt(this.variation);
		}

		public static CreatedDestructable read(final DataInputStream in) throws IOException {
			return new CreatedDestructable(in.readInt(), in.readInt(), in.readFloat(), in.readFloat(), in.readFloat(),
					in.readFloat(), in.readFloat(), in.readInt());
		}
	}

	public List<CreatedDestructable> getCreatedDestructables() {
		// A removed destructable stays in the game's list and a script variable can still hold it, so a save names
		// it too: loading has to make it again, unless its id was given to a destructable made later.
		final Set<Integer> removedIds = new HashSet<>();
		for (final CDestructable destructable : this.simulation.getDestructables()) {
			if (destructable.isRemovedFromGame()) {
				removedIds.add(destructable.getHandleId());
			}
		}
		final Set<Integer> liveIds = new HashSet<>();
		for (final CreatedDestructable created : this.createdDestructables) {
			if (this.simulation.getDestructable(created.savedHandleId) != null) {
				liveIds.add(created.savedHandleId);
			}
		}
		final List<CreatedDestructable> kept = new ArrayList<>();
		final Set<Integer> removedKept = new HashSet<>();
		for (final CreatedDestructable created : this.createdDestructables) {
			final int id = created.savedHandleId;
			if ((this.simulation.getDestructable(id) != null)
					|| (removedIds.contains(id) && !liveIds.contains(id) && removedKept.add(id))) {
				kept.add(created);
			}
		}
		return kept;
	}

	public CDestructable createDestructableForSavedGame(final CreatedDestructable created) {
		final CDestructable destructable = this.simulation.getSimulationRenderController().createDestructableZ(
				new War3ID(created.typeId), created.x, created.y, created.z, created.facing, created.scale,
				created.variation);
		// The simulation's handle id comes back with the saved state; this list keeps the saved game's numbers.
		this.createdDestructables.remove(this.createdDestructables.size() - 1);
		this.createdDestructables.add(created);
		return destructable;
	}

	public void clearRenderPeersForSavedGame() {
		final CSimulation game = this.simulation;
		final SimulationRenderController controller = game.getSimulationRenderController();
		for (final CUnit unit : new ArrayList<>(game.getUnits())) {
			controller.removeUnit(unit);
		}
		for (final RenderFogGhost ghost : this.fogGhosts) {
			ghost.remove(this);
		}
		this.fogGhosts.clear();
		for (final RenderItem item : new ArrayList<>(this.itemToRenderPeer.values())) {
			this.widgets.remove(item);
			this.worldScene.removeInstance(item.instance);
		}
		this.itemToRenderPeer.clear();
	}

	/**
	 * Destructables kept their peers (they were read into the objects the map or {@link #createDestructableForSavedGame}
	 * made); units and items get new ones.
	 */
	public void finishLoadingSavedGame(final Collection<CDestructable> preparedDestructables) {
		final Set<CDestructable> current = new HashSet<>(this.simulation.getDestructables());
		for (final CDestructable destructable : preparedDestructables) {
			if (!current.contains(destructable) || destructable.isRemovedFromGame()) {
				final RenderDestructable peer = this.destructableToRenderPeer.get(destructable);
				if (peer != null) {
					this.widgets.remove(peer);
					this.decals.remove(peer);
					this.simulation.getSimulationRenderController().removeDestructable(destructable);
				}
			}
		}
		for (final CUnit unit : this.simulation.getUnits()) {
			createRenderUnitForSavedGame(unit);
		}
		for (final CItem item : this.simulation.getItems()) {
			createRenderItemForSavedGame(item);
		}
		this.terrain.setFogOfWarData(this.simulation, this.simulation.getPlayer(this.localPlayerIndex).getFogOfWar());
	}

	private void createRenderItemForSavedGame(final CItem item) {
		final RenderItemType renderItemType = this.renderItemTypeData.get(item.getTypeId());
		if (renderItemType != null) {
			final float x = item.getX();
			final float y = item.getY();
			final float z = Math.max(getWalkableRenderHeight(x, y), this.terrain.getGroundHeight(x, y));
			final RenderItem renderItem = new RenderItem(this, renderItemType, x, y, z, 0, item);
			this.widgets.add(renderItem);
			this.itemToRenderPeer.put(item, renderItem);
			renderItem.shadow = createUnitShadow(renderItemType.getRenderShadowType(), x, y);
		}
	}

	private void createRenderUnitForSavedGame(final CUnit unit) {
		final RenderUnitType renderUnitType = this.renderUnitTypeData.get(unit.getTypeId());
		if (renderUnitType == null) {
			return;
		}
		final float x = unit.getX();
		final float y = unit.getY();
		final int teamColor = renderUnitType.getTeamColor() != -1 ? renderUnitType.getTeamColor()
				: this.simulation.getPlayer(unit.getPlayerIndex()).getColor();
		final float z = Math.max(getWalkableRenderHeight(x, y), this.terrain.getGroundHeight(x, y))
				+ unit.getFlyHeight();
		final String buildingShadow = renderUnitType.getBuildingShadow();
		final BuildingShadow shadow = buildingShadow == null ? null : this.terrain.addShadow(buildingShadow, x, y);
		final RenderUnit renderUnit = new RenderUnit(this, x, y, z, teamColor, unit, renderUnitType, shadow,
				this.selectionCircleScaleFactor, renderUnitType.getAnimationWalkSpeed(),
				renderUnitType.getAnimationRunSpeed(), renderUnitType.getScalingValue());
		this.unitToRenderPeer.put(unit, renderUnit);
		this.widgets.add(renderUnit);
		this.units.add(renderUnit);
		renderUnit.shadow = createUnitShadow(renderUnitType.getRenderShadowType(), x, y);
		if (renderUnitType.getUberSplat() != null) {
			renderUnit.uberSplat = addUberSplatIngame(x, y, renderUnitType.getUberSplat(),
					renderUnitType.getUberSplatScaleValue());
		}
	}

	public void setMapFileName(final String mapFileName) {
		this.mapFileName = mapFileName;
	}

	public String getMapFileName() {
		return this.mapFileName;
	}

	/**
	 * @param savedGame the save put in place of the map's start, or null to start the map
	 * @param byScript  whether the map's own LoadGame asked for it (zone travel), not the player
	 */
	public void setSavedGameToLoad(final FileHandle savedGame, final boolean byScript) {
		this.savedGameToLoad = savedGame;
		this.savedGameLoadedByScript = byScript;
	}

	public FileHandle getSavedGameToLoad() {
		return this.savedGameToLoad;
	}

	public boolean isSavedGameLoadedByScript() {
		return this.savedGameLoadedByScript;
	}

	private static int chooseGameSeed() {
		final Integer fixed = Integer.getInteger("warsmash.seed");
		final int seed = fixed != null ? fixed : ThreadLocalRandom.current().nextInt();
		System.err.println("Game seed: " + seed);
		return seed;
	}

	public int getGameSeed() {
		return this.gameSeed;
	}

	/** The simulation's random numbers come back with the saved game; only the seed it started from is set here. */
	public void setGameSeedForSavedGame(final int gameSeed) {
		this.gameSeed = gameSeed;
	}

	/** A replay's game starts from the recorded seed; set before the map is loaded. */
	public void setGameSeed(final int gameSeed) {
		this.gameSeed = gameSeed;
		this.seededRandom.setSeed(gameSeed);
	}

	public void setReplaySession(final ReplaySession replaySession) {
		closeReplaySession();
		this.replaySession = replaySession;
		if (this.replayNotSaved && (replaySession instanceof ReplayRecording)) {
			((ReplayRecording) replaySession).doNotSave();
		}
	}

	/** DoNotSaveReplay, also when the map's script calls it before the recording starts. */
	public void doNotSaveReplay() {
		this.replayNotSaved = true;
		if (this.replaySession instanceof ReplayRecording) {
			((ReplayRecording) this.replaySession).doNotSave();
		}
	}

	public ReplaySession getReplaySession() {
		return this.replaySession;
	}

	/** See {@link ReplaySession#localValue(float)}. */
	public float replayedLocalValue(final float value) {
		return this.replaySession == null ? value : this.replaySession.localValue(value);
	}

	/** A replay plays the game again; what the game wrote for the player (its saves) it does not write again. */
	public boolean isReplayPlayback() {
		return this.replaySession instanceof ReplayPlayback;
	}

	/**
	 * EndGame or a quit: the game makes no more steps, also none left in the frame it ended in, and its replay ends
	 * at this step.
	 */
	public void gameEnded() {
		if (this.gameEnded) {
			return;
		}
		this.gameEnded = true;
		this.updateTime = 0;
		if (this.replaySession != null) {
			this.replaySession.gameEnded();
		}
	}

	public boolean isGameEnded() {
		return this.gameEnded;
	}

	/** What the interface shows when a game ends with its score screen. */
	public void setGameEndScreen(final Runnable gameEndScreen) {
		this.gameEndScreen = gameEndScreen;
	}

	/** Ends the game on its score screen, as a replay that ends without the game's own EndGame does. */
	public void showGameEnd() {
		if (this.gameEndScreen != null) {
			this.gameEndScreen.run();
		}
		else {
			gameEnded();
		}
	}

	private void closeReplaySession() {
		if (this.replaySession != null) {
			final ReplaySession session = this.replaySession;
			this.replaySession = null;
			session.close();
		}
	}

	public static enum PauseSource {
		SCRIPT,
		PLAYER,
		MENU,
		/** The app is not on the screen (Android). */
		BACKGROUND,
		/** A replay's own Pause; the replay's game knows nothing of it. */
		REPLAY;
	}

	@Override
	public void update() {
		PerfStats.frameStart();
		if (this.anyReady) {
			this.successfulFrames++;
			// while paused everything is still placed and drawn, but no game or animation time passes
			final boolean paused = isGamePaused();
			final float deltaTime = paused ? 0 : Gdx.graphics.getDeltaTime() * this.replaySpeed;
			updateReplayVision();
			this.terrain.update(deltaTime);
			PerfStats.end(PerfStats.Section.TERRAIN_UPDATE);

			final float rawDeltaTime = paused ? 0 : Gdx.graphics.getRawDeltaTime() * this.replaySpeed;
			super.update(rawDeltaTime);
			PerfStats.end(PerfStats.Section.SCENE_UPDATE);

			this.updateTime += rawDeltaTime * this.timeScale;

			final Iterator<TextTag> textTagIterator = this.textTags.iterator();
			while (textTagIterator.hasNext()) {
				if (textTagIterator.next().update(deltaTime)) {
					textTagIterator.remove();
				}
			}
			for (final RenderWidget unit : this.widgets) {
				unit.updateAnimations(this);
			}
			for (int i = this.fogGhosts.size() - 1; i >= 0; i--) {
				if (this.fogGhosts.get(i).update(this)) {
					this.fogGhosts.remove(i);
				}
			}
			for (int i = 0, n = this.weatherEffects.size(); i < n; i++) {
				this.weatherEffects.get(i).update(this);
			}
			updateStackedSounds();
			this.scriptUbersplats.removeIf(splat -> !splat.update());
			for (int i = 0, n = this.trackableModels.size(); i < n; i++) {
				this.trackableModels.get(i).updateAnimations(this, deltaTime);
			}
			final Iterator<RenderEffect> projectileIterator = this.projectiles.iterator();
			while (projectileIterator.hasNext()) {
				final RenderEffect projectile = projectileIterator.next();
				if (projectile.updateAnimations(this, deltaTime)) {
					projectileIterator.remove();
				}
			}
			for (final RenderDoodad item : this.doodads) {
				final ModelInstance instance = item.instance;
				if (instance instanceof MdxComplexInstance) {
					final MdxComplexInstance mdxComplexInstance = (MdxComplexInstance) instance;
					if ((mdxComplexInstance.sequence == -1) || (mdxComplexInstance.sequenceEnded
							&& ((item.getAnimation() != AnimationTokens.PrimaryTag.DEATH)
									|| (((MdxModel) mdxComplexInstance.model).sequences.get(mdxComplexInstance.sequence)
											.getFlags() == 0)))) {
						SequenceUtils.randomSequence(mdxComplexInstance, item.getAnimation(), SequenceUtils.EMPTY,
								true);

					}
				}
			}

			PerfStats.end(PerfStats.Section.WIDGETS);
			if (this.worldHidden || this.gameEnded) {
				// The game is over on the score screen; its scores must not change behind it.
				this.updateTime = 0;
			}
			int stepsThisFrame = 0;
			while ((this.updateTime >= WarsmashConstants.SIMULATION_STEP_TIME) && !this.gameEnded) {
				if (stepsThisFrame >= (MAX_STEPS_PER_FRAME * this.timeScale * this.replaySpeed)) {
					// A slow device would otherwise spend the next frame catching up and fall further behind each
					// time; like the original, the game runs slower instead.
					this.updateTime = 0;
					break;
				}
				stepsThisFrame++;
				if (this.gameTurnManager.getLatestCompletedTurn() >= this.simulation.getGameTurnTick()) {
					this.updateTime -= WarsmashConstants.SIMULATION_STEP_TIME;
					this.simulation.update();
					PerfStats.simulationStep();
					this.gameTurnManager.turnCompleted(this.simulation.getGameTurnTick());
					runPendingSaveRequests();
				} else {
					if (this.updateTime > (WarsmashConstants.SIMULATION_STEP_TIME * 3)) {
						this.gameTurnManager.framesSkipped(this.updateTime / WarsmashConstants.SIMULATION_STEP_TIME);
						this.updateTime = 0;
					}
					break;
				}
			}
			PerfStats.end(PerfStats.Section.SIMULATION);
			if (paused) {
				runPendingSaveRequests();
			}
			if (this.dncTerrain != null) {
				this.dncTerrain.setFrameByRatio(
						this.simulation.getGameTimeOfDay() / this.simulation.getGameplayConstants().getGameDayHours());
				this.dncTerrain.update(rawDeltaTime, null);
			}
			if (this.dncUnit != null) {
				this.dncUnit.setFrameByRatio(
						this.simulation.getGameTimeOfDay() / this.simulation.getGameplayConstants().getGameDayHours());
				this.dncUnit.update(rawDeltaTime, null);
			}

			if (this.dncTarget != null) {
				this.dncTarget.setFrameByRatio(
						this.simulation.getGameTimeOfDay() / this.simulation.getGameplayConstants().getGameDayHours());
				this.dncTarget.update(rawDeltaTime, null);
			}
		}
	}

	/**
	 * With warsmash.norender only the game runs: no model, terrain or effect is updated or drawn, the way a replay
	 * skips ahead. The game must come out the same as when it is drawn.
	 */
	@Override
	public void updateAndRender() {
		if ((this.fastForwardTarget >= 0) && this.anyReady && !isGamePaused()) {
			fastForwardSteps();
			return;
		}
		if (NO_RENDER) {
			if (this.anyReady) {
				stepSimulationWithoutRender();
			}
			return;
		}
		super.updateAndRender();
	}

	/**
	 * Plays the game on to the step without drawing it, as a replay skips ahead: as many steps a frame as fit in
	 * {@link #FAST_FORWARD_FRAME_NANOS}, so the interface still answers. The skipped steps make no missiles, effects
	 * or sounds, only the last one does; the game then goes on drawn, at its own speed. whenReached is told at the
	 * step, or where the game cannot go further (the end of a replay).
	 */
	public void fastForwardTo(final int tick, final FastForwardListener whenReached) {
		this.fastForwardTarget = tick;
		this.fastForwardReached = whenReached;
		this.fastForwardSteps = 0;
		this.fastForwardNanos = 0;
	}

	public static interface FastForwardListener {
		/** @param nanos the time the steps took, frames between them left out */
		void reached(int steps, long nanos);
	}

	public boolean isFastForwarding() {
		return this.fastForwardTarget >= 0;
	}

	private static final long FAST_FORWARD_FRAME_NANOS = 40_000_000L;

	private void fastForwardSteps() {
		final long frameStart = System.nanoTime();
		final long frameEnd = frameStart + FAST_FORWARD_FRAME_NANOS;
		this.updateTime = 0;
		boolean blocked = this.worldHidden;
		while (!blocked && (this.simulation.getGameTurnTick() < this.fastForwardTarget)) {
			if (this.gameEnded
					|| (this.gameTurnManager.getLatestCompletedTurn() < this.simulation.getGameTurnTick())) {
				blocked = true;
				break;
			}
			this.renderController
					.setPresenting(!NO_RENDER && ((this.simulation.getGameTurnTick() + 1) >= this.fastForwardTarget));
			this.simulation.update();
			this.fastForwardSteps++;
			this.gameTurnManager.turnCompleted(this.simulation.getGameTurnTick());
			runPendingSaveRequests();
			if (isGamePaused() || (System.nanoTime() >= frameEnd)) {
				break;
			}
		}
		this.renderController.setPresenting(!NO_RENDER);
		this.fastForwardNanos += System.nanoTime() - frameStart;
		if (blocked || (this.simulation.getGameTurnTick() >= this.fastForwardTarget)) {
			final FastForwardListener reached = this.fastForwardReached;
			this.fastForwardTarget = -1;
			this.fastForwardReached = null;
			if (reached != null) {
				reached.reached(this.fastForwardSteps, this.fastForwardNanos);
			}
		}
	}

	private void stepSimulationWithoutRender() {
		if (isGamePaused()) {
			runPendingSaveRequests();
			return;
		}
		if (this.worldHidden || this.gameEnded) {
			return;
		}
		this.updateTime += Gdx.graphics.getRawDeltaTime() * timeScale;
		while ((this.updateTime >= WarsmashConstants.SIMULATION_STEP_TIME) && !this.gameEnded
				&& (this.gameTurnManager.getLatestCompletedTurn() >= this.simulation.getGameTurnTick())) {
			this.updateTime -= WarsmashConstants.SIMULATION_STEP_TIME;
			this.simulation.update();
			this.gameTurnManager.turnCompleted(this.simulation.getGameTurnTick());
			runPendingSaveRequests();
		}
		if (this.updateTime > (WarsmashConstants.SIMULATION_STEP_TIME * 3)) {
			this.gameTurnManager.framesSkipped(this.updateTime / WarsmashConstants.SIMULATION_STEP_TIME);
			this.updateTime = 0;
		}
	}

	public void setWorldHidden(final boolean worldHidden) {
		this.worldHidden = worldHidden;
	}

	public boolean isWorldHidden() {
		return this.worldHidden;
	}

	/**
	 * SetSkyModel. The sky draws behind everything and moves with the camera, so it has a scene of its own whose far
	 * plane the map's camera distance settings cannot cut through.
	 */
	public void setWaterBaseColor(final float[] color) {
		this.waterColor = color;
		this.terrain.setWaterBaseColor(color[0], color[1], color[2], color[3]);
	}

	public void setSkyModel(final String path) {
		this.skyModel = path;
		if (this.skyInstance != null) {
			this.skyInstance.detach();
			this.skyInstance = null;
		}
		if ((path == null) || path.isEmpty()) {
			return;
		}
		if (this.skyScene == null) {
			this.skyScene = addSimpleScene();
			this.skyScene.alpha = true;
		}
		final MdxModel model = loadModelMdx(this.dataSource, this, path, this.mapPathSolver, this.solverParams);
		this.skyInstance = (MdxComplexInstance) model.addInstance();
		this.skyInstance.setSequenceLoopMode(SequenceLoopMode.ALWAYS_LOOP);
		this.skyInstance.setSequence(0);
		this.skyInstance.setScene(this.skyScene);
	}

	private void renderSky() {
		final Camera camera = this.worldScene.camera;
		final Camera skyCamera = this.skyScene.camera;
		skyCamera.viewport(camera.rect);
		skyCamera.perspective(camera.getFov(), camera.getAspect(), 16f, SKY_FAR_PLANE);
		skyCamera.setLocation(camera.location);
		skyCamera.setRotation(camera.rotation);
		skyCamera.update();
		this.skyInstance.setLocation(camera.location);
		this.skyScene.startFrame();
		this.skyScene.renderOpaque();
		this.skyScene.renderTranslucent();
		this.gl.glDepthMask(true);
		this.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
	}

	/**
	 * UnitAddIndicator: the unit pulses in the color for a moment.
	 */
	public void addIndicator(final CWidget widget, final int red, final int green, final int blue, final int alpha) {
		if (widget instanceof CUnit) {
			final RenderUnit peer = getRenderPeer((CUnit) widget);
			if (peer != null) {
				final UnitIndicator indicator = new UnitIndicator();
				indicator.unit = peer;
				indicator.color = new float[] { red / 255f, green / 255f, blue / 255f };
				indicator.strength = alpha / 255f;
				this.unitIndicators.add(indicator);
			}
		}
	}

	private void updateUnitIndicators(final float deltaTime) {
		for (final Iterator<UnitIndicator> iterator = this.unitIndicators.iterator(); iterator.hasNext();) {
			final UnitIndicator indicator = iterator.next();
			indicator.elapsed += deltaTime;
			// Pulses around the color the game has for the unit, which can change meanwhile.
			final float[] original = this.simulation.getUnitVertexColor(indicator.unit.getSimulationUnit());
			if (indicator.elapsed >= UNIT_INDICATOR_SECONDS) {
				indicator.unit.setVertexColoring(this, original[0], original[1], original[2]);
				iterator.remove();
			}
			else {
				final float pulse = indicator.strength
						* (float) Math.abs(Math.sin((indicator.elapsed * Math.PI * 2) / UNIT_INDICATOR_PULSE_SECONDS));
				final float[] color = indicator.color;
				indicator.unit.setVertexColoring(this, original[0] + ((color[0] - original[0]) * pulse),
						original[1] + ((color[1] - original[1]) * pulse),
						original[2] + ((color[2] - original[2]) * pulse));
			}
		}
	}

	private static final class UnitIndicator {
		private RenderUnit unit;
		private float[] color;
		private float strength;
		private float elapsed;
	}

	/**
	 * SetDoodadAnimation. Null area means every doodad of the type.
	 */
	public void setDoodadAnimation(final String doodadId, final Rectangle area, final float centerX,
			final float centerY, final float radius, final boolean nearestOnly, final String animation) {
		RenderDoodad nearest = null;
		float nearestDistance = Float.MAX_VALUE;
		for (final RenderDoodad doodad : this.doodads) {
			if (!doodad.isOfType(doodadId)) {
				continue;
			}
			final float dx = doodad.getX() - centerX;
			final float dy = doodad.getY() - centerY;
			final float distance = (dx * dx) + (dy * dy);
			if (area != null) {
				if (area.contains(doodad.getX(), doodad.getY())) {
					doodad.playAnimation(animation);
				}
			}
			else if (distance <= (radius * radius)) {
				if (nearestOnly) {
					if (distance < nearestDistance) {
						nearestDistance = distance;
						nearest = doodad;
					}
				}
				else {
					doodad.playAnimation(animation);
				}
			}
		}
		if (nearest != null) {
			nearest.playAnimation(animation);
		}
	}

	@Override
	public void render() {
		if (this.anyReady) {
			final Scene worldScene = this.worldScene;

			PerfStats.mark();
			startFrame();
			worldScene.startFrame();
			if (!this.worldHidden && (this.skyInstance != null)) {
				renderSky();
			}
			// The score screen covers the game: only the interface scenes are drawn.
			if (!this.worldHidden && (DEBUG_DEPTH > 0)) {
				worldScene.renderOpaque(this.dynamicShadowManager, this.webGL);
			}
			PerfStats.end(PerfStats.Section.SHADOWS);
			if (!this.worldHidden && (DEBUG_DEPTH > 1)) {
				this.terrain.renderGround(this.dynamicShadowManager);
			}
			PerfStats.end(PerfStats.Section.GROUND);
			if (!this.worldHidden && (DEBUG_DEPTH > 2)) {
				this.terrain.renderCliffs();
			}
			PerfStats.end(PerfStats.Section.CLIFFS);
			if (!this.worldHidden && (DEBUG_DEPTH > 3)) {
				worldScene.renderOpaque();
			}
			PerfStats.end(PerfStats.Section.OPAQUE);
			if (!this.worldHidden && (DEBUG_DEPTH > 4)) {
				this.terrain.renderUberSplats(false);
			}
			PerfStats.end(PerfStats.Section.SPLATS);
			if (!this.worldHidden && (DEBUG_DEPTH > 5)) {
				this.terrain.renderWater();
			}
			PerfStats.end(PerfStats.Section.WATER);
			if (!this.worldHidden && (DEBUG_DEPTH > 6)) {
				worldScene.renderTranslucent();
			}
			PerfStats.end(PerfStats.Section.TRANSLUCENT);
			if (!this.worldHidden && (DEBUG_DEPTH > 7)) {
				this.terrain.renderUberSplats(true);
			}
			PerfStats.end(PerfStats.Section.SPLATS);

			final List<Scene> scenes = this.scenes;
			for (final Scene scene : scenes) {
				if ((scene != worldScene) && (scene != this.skyScene)) {
					scene.startFrame();
					if (DEBUG_DEPTH > 8) {
						scene.renderOpaque();
					}
					if (DEBUG_DEPTH > 9) {
						scene.renderTranslucent();
					}
				}
			}
			PerfStats.end(PerfStats.Section.INTERFACE);

			// glGetError waits for the GPU on phones, so it is only asked while debugging.
			if (WarsmashConstants.ENABLE_DEBUG) {
				final int glGetError = Gdx.gl.glGetError();
				if (glGetError != GL20.GL_NO_ERROR) {
					throw new IllegalStateException("GL ERROR: " + glGetError);
				}
			}
		}
	}

	public void deselect() {
		for (final String key : this.selectedSplatModelKeys) {
			this.terrain.removeSplatBatchModel(key);
		}
		for (final RenderWidget unit : this.selected) {
			unit.unassignSelectionCircle();
		}
		this.selectedSplatModelKeys.clear();
		this.selected.clear();
	}

	public void doUnselectUnit(final RenderWidget widget) {
		if (this.selected.remove(widget)) {
			widget.getSelectionCircle();
		}
	}

	public void doSelectUnit(final List<RenderWidget> units) {
		deselect();
		if (units.isEmpty()) {
			return;
		}

		final Map<String, Terrain.Splat> splats = new HashMap<String, Terrain.Splat>();
		for (final RenderWidget unit : units) {
			if (unit.getSelectionScale() > 0) {
				String allyKey = "n:";
				final float selectionSize = unit.getSelectionScale();
				String path = null;
				for (int i = 0; i < this.selectionCircleSizes.size(); i++) {
					final SelectionCircleSize selectionCircleSize = this.selectionCircleSizes.get(i);
					if ((selectionSize < selectionCircleSize.size) || (i == (this.selectionCircleSizes.size() - 1))) {
						path = selectionCircleSize.texture;
						break;
					}
				}
				if (!path.toLowerCase().endsWith(".blp")) {
					path += ".blp";
				}
				if (unit instanceof RenderUnit) {
					final int selectedUnitPlayerIndex = ((RenderUnit) unit).getSimulationUnit().getFakePlayerIndex();
					final CPlayer localPlayer = this.simulation.getPlayer(this.localPlayerIndex);
					if (!localPlayer.hasAlliance(selectedUnitPlayerIndex, CAllianceType.PASSIVE)) {
						allyKey = "e:";
					} else if (localPlayer.hasAlliance(((RenderUnit) unit).getSimulationUnit().getPlayerIndex(),
							CAllianceType.SHARED_CONTROL)
							|| localPlayer.hasAlliance(selectedUnitPlayerIndex, CAllianceType.SHARED_CONTROL)) {
						allyKey = "f:";
					}
				}
				path = allyKey + path;
				if (unit.isShowSelectionCircleAboveWater()) {
					path = path + ":abovewater";
				}
				final SplatModel splatModel = this.terrain.getSplatModel("selection:" + path);
				if (splatModel != null) {
					final float x = unit.getX();
					final float y = unit.getY();
					final SplatMover splatInstance = splatModel.add(x - (selectionSize / 2), y - (selectionSize / 2),
							x + (selectionSize / 2), y + (selectionSize / 2), 5, this.terrain.centerOffset);
					unit.assignSelectionCircle(splatInstance);
					if (unit.getInstance().hidden()) {
						splatInstance.hide();
					}
				} else {
					if (!splats.containsKey(path)) {
						splats.put(path, new Splat());
					}
					final float x = unit.getX();
					final float y = unit.getY();
					System.out.println("Selecting a unit at " + x + "," + y);
					if (unit.isShowSelectionCircleAboveWater()) {
						splats.get(path).aboveWater = true;
					}
					splats.get(path).locations.add(new float[] { x - (selectionSize / 2), y - (selectionSize / 2),
							x + (selectionSize / 2), y + (selectionSize / 2), 5 });
					splats.get(path).unitMapping.add(new Consumer<SplatModel.SplatMover>() {
						@Override
						public void accept(final SplatMover t) {
							unit.assignSelectionCircle(t);
							if (unit.getInstance().hidden()) {
								t.hide();
							}
						}
					});
				}
			}
			this.selected.add(unit);
		}
		for (final Map.Entry<String, Terrain.Splat> entry : splats.entrySet()) {
			final String path = entry.getKey();
			String filePath = path.substring(2);
			if (filePath.endsWith(":abovewater")) {
				filePath = filePath.substring(0, filePath.length() - 11);
			}
			final String allyKey = path.substring(0, 2);
			final Splat locations = entry.getValue();
			final SplatModel model = new SplatModel(Gdx.gl30, (Texture) load(filePath, PathSolver.DEFAULT, null),
					locations.locations, this.terrain.centerOffset, locations.unitMapping, true, false, true,
					locations.aboveWater);
			switch (allyKey) {
			case "e:":
				model.color[0] = this.selectionCircleColorEnemy.r;
				model.color[1] = this.selectionCircleColorEnemy.g;
				model.color[2] = this.selectionCircleColorEnemy.b;
				model.color[3] = this.selectionCircleColorEnemy.a;
				break;
			case "f:":
				model.color[0] = this.selectionCircleColorFriend.r;
				model.color[1] = this.selectionCircleColorFriend.g;
				model.color[2] = this.selectionCircleColorFriend.b;
				model.color[3] = this.selectionCircleColorFriend.a;
				break;
			default:
				model.color[0] = this.selectionCircleColorNeutral.r;
				model.color[1] = this.selectionCircleColorNeutral.g;
				model.color[2] = this.selectionCircleColorNeutral.b;
				model.color[3] = this.selectionCircleColorNeutral.a;
				break;
			}
			this.terrain.addSplatBatchModel("selection:" + path, model);
			this.selectedSplatModelKeys.add("selection:" + path);
		}
	}

	public void clearUnitMouseOverHighlight(final RenderWidget unit) {
		this.mouseHighlightWidgets.remove(unit);
		final SplatMover selectionPreviewHighlight = unit.getSelectionPreviewHighlight();
		if (selectionPreviewHighlight != null) {
			selectionPreviewHighlight.destroy(Gdx.gl30, this.terrain.centerOffset);
		}
		unit.unassignSelectionPreviewHighlight();
	}

	public void clearUnitMouseOverHighlight() {
		for (final String modelKey : this.mouseHighlightSplatModelKeys) {
			this.terrain.removeSplatBatchModel(modelKey);
		}
		for (final RenderWidget widget : this.mouseHighlightWidgets) {
			widget.unassignSelectionPreviewHighlight();
		}
		this.mouseHighlightSplatModelKeys.clear();
		this.mouseHighlightWidgets.clear();
	}

	public void showUnitMouseOverHighlight(final RenderWidget unit) {
		final Map<String, Terrain.Splat> splats = new HashMap<String, Terrain.Splat>();
		if (unit.getSelectionScale() > 0) {
			String allyKey = "n:";
			final float selectionSize = unit.getSelectionScale();
			String path = null;
			for (int i = 0; i < this.selectionCircleSizes.size(); i++) {
				final SelectionCircleSize selectionCircleSize = this.selectionCircleSizes.get(i);
				if ((selectionSize < selectionCircleSize.size) || (i == (this.selectionCircleSizes.size() - 1))) {
					path = selectionCircleSize.texture;
					break;
				}
			}
			if (!path.toLowerCase().endsWith(".blp")) {
				path += ".blp";
			}
			if (unit instanceof RenderUnit) {
				final CUnit simulationUnit = ((RenderUnit) unit).getSimulationUnit();
				final int selectedUnitPlayerIndex = simulationUnit.getFakePlayerIndex();
				final CPlayer localPlayer = this.simulation.getPlayer(this.localPlayerIndex);
				if (!localPlayer.hasAlliance(selectedUnitPlayerIndex, CAllianceType.PASSIVE)) {
					allyKey = "e:";
				} else if (localPlayer.hasAlliance(simulationUnit.getPlayerIndex(), CAllianceType.SHARED_CONTROL)
						|| localPlayer.hasAlliance(selectedUnitPlayerIndex, CAllianceType.SHARED_CONTROL)) {
					allyKey = "f:";
				}
			}
			path = allyKey + path;
			if (unit.isShowSelectionCircleAboveWater()) {
				path = path + ":abovewater";
			}
			final SplatModel splatModel = this.terrain.getSplatModel("mouseover:" + path);
			if (splatModel != null) {
				final float x = unit.getX();
				final float y = unit.getY();
				final SplatMover splatInstance = splatModel.add(x - (selectionSize / 2), y - (selectionSize / 2),
						x + (selectionSize / 2), y + (selectionSize / 2), 4, this.terrain.centerOffset);
				unit.assignSelectionPreviewHighlight(splatInstance);
				if (unit.getInstance().hidden()) {
					splatInstance.hide();
				}
			} else {
				if (!splats.containsKey(path)) {
					splats.put(path, new Splat());
				}
				final float x = unit.getX();
				final float y = unit.getY();
				if (unit.isShowSelectionCircleAboveWater()) {
					splats.get(path).aboveWater = true;
				}
				splats.get(path).locations.add(new float[] { x - (selectionSize / 2), y - (selectionSize / 2),
						x + (selectionSize / 2), y + (selectionSize / 2), 4 });
				splats.get(path).unitMapping.add(new Consumer<SplatModel.SplatMover>() {
					@Override
					public void accept(final SplatMover t) {
						unit.assignSelectionPreviewHighlight(t);
						if (unit.getInstance().hidden()) {
							t.hide();
						}
					}
				});
			}
		}
		this.mouseHighlightWidgets.add(unit);
		for (final Map.Entry<String, Terrain.Splat> entry : splats.entrySet()) {
			final String path = entry.getKey();
			String filePath = path.substring(2);
			if (filePath.endsWith(":abovewater")) {
				filePath = filePath.substring(0, filePath.length() - 11);
			}
			final String allyKey = path.substring(0, 2);
			final Splat locations = entry.getValue();
			final SplatModel model = new SplatModel(Gdx.gl30, (Texture) load(filePath, PathSolver.DEFAULT, null),
					locations.locations, this.terrain.centerOffset, locations.unitMapping, true, false, true,
					locations.aboveWater);
			switch (allyKey) {
			case "e:":
				model.color[0] = this.selectionCircleColorEnemy.r;
				model.color[1] = this.selectionCircleColorEnemy.g;
				model.color[2] = this.selectionCircleColorEnemy.b;
				model.color[3] = this.selectionCircleColorEnemy.a * 0.5f;
				break;
			case "f:":
				model.color[0] = this.selectionCircleColorFriend.r;
				model.color[1] = this.selectionCircleColorFriend.g;
				model.color[2] = this.selectionCircleColorFriend.b;
				model.color[3] = this.selectionCircleColorFriend.a * 0.5f;
				break;
			default:
				model.color[0] = this.selectionCircleColorNeutral.r;
				model.color[1] = this.selectionCircleColorNeutral.g;
				model.color[2] = this.selectionCircleColorNeutral.b;
				model.color[3] = this.selectionCircleColorNeutral.a * 0.5f;
				break;
			}
			this.mouseHighlightSplatModelKeys.add("mouseover:" + path);
			this.terrain.addSplatBatchModel("mouseover:" + path, model);
		}
	}

	public void getClickLocation(final Vector3 out, final int screenX, final int screenY,
			final boolean intersectWithWater, final boolean pushResultZAboveWater) {
		final float[] ray = rayHeap;
		mousePosHeap.set(screenX, screenY);
		this.worldScene.camera.screenToWorldRay(ray, mousePosHeap);
		gdxRayHeap.set(ray[0], ray[1], ray[2], ray[3] - ray[0], ray[4] - ray[1], ray[5] - ray[2]);
		gdxRayHeap.direction.nor();// needed for libgdx
		if (intersectWithWater) {
			RenderMathUtils.intersectRayTriangles(gdxRayHeap, this.terrain.softwareWaterAndGroundMesh.vertices,
					this.terrain.softwareWaterAndGroundMesh.indices, 3, out);
		} else {
			RenderMathUtils.intersectRayTriangles(gdxRayHeap, this.terrain.softwareGroundMesh.vertices,
					this.terrain.softwareGroundMesh.indices, 3, out);
		}
		rectangleHeap.set(Math.min(out.x, gdxRayHeap.origin.x), Math.min(out.y, gdxRayHeap.origin.y),
				Math.abs(out.x - gdxRayHeap.origin.x), Math.abs(out.y - gdxRayHeap.origin.y));
		this.walkableObjectsTree.intersect(rectangleHeap, this.walkablesIntersectionFinder.reset(gdxRayHeap));
		if (this.walkablesIntersectionFinder.found) {
			out.set(this.walkablesIntersectionFinder.intersection);
		} else {
			final float oldZ = out.z;
			out.z = Math.max(getWalkableRenderHeight(out.x, out.y), this.terrain.getGroundHeight(out.x, out.y));

			if (pushResultZAboveWater) {
				out.z = Math.max(out.z, oldZ);
				final short pathing = this.simulation.getPathingGrid().getPathing(out.x, out.y);
				if (PathingGrid.isPathingFlag(pathing, PathingType.SWIMMABLE)
						&& !PathingGrid.isPathingFlag(pathing, PathingType.WALKABLE)) {
					out.z = Math.max(out.z, this.terrain.getWaterHeight(out.x, out.y));
				}
			}
		}
	}

	public void getClickLocationOnZPlane(final Vector3 out, final int screenX, final int screenY, final float worldZ) {
		final float[] ray = rayHeap;
		mousePosHeap.set(screenX, screenY);
		this.worldScene.camera.screenToWorldRay(ray, mousePosHeap);
		gdxRayHeap.set(ray[0], ray[1], ray[2], ray[3] - ray[0], ray[4] - ray[1], ray[5] - ray[2]);
		gdxRayHeap.direction.nor();// needed for libgdx
		planeHeap.set(0, 0, -1, worldZ);
		Intersector.intersectRayPlane(gdxRayHeap, planeHeap, out);
	}

	public void showConfirmation(final Vector3 position, final float red, final float green, final float blue) {
		this.confirmationInstance.show();
		this.confirmationInstance.setSequence(0);
		this.confirmationInstance.setLocation(position);
		this.worldScene.instanceMoved(this.confirmationInstance, position.x, position.y);
		this.confirmationInstance.vertexColor[0] = red;
		this.confirmationInstance.vertexColor[1] = green;
		this.confirmationInstance.vertexColor[2] = blue;
	}

	public RenderWidget rayPickUnit(final float x, final float y) {
		return this.rayPickUnit(x, y, CWidgetFilterFunction.ACCEPT_ALL);
	}

	public RenderWidget rayPickUnit(final float x, final float y, final CWidgetFilterFunction filter) {
		final float[] ray = rayHeap;
		mousePosHeap.set(x, y);
		this.worldScene.camera.screenToWorldRay(ray, mousePosHeap);
		gdxRayHeap.set(ray[0], ray[1], ray[2], ray[3] - ray[0], ray[4] - ray[1], ray[5] - ray[2]);
		gdxRayHeap.direction.nor();// needed for libgdx

		RenderWidget entity = null;
		intersectionHeap2.set(ray[3], ray[4], ray[5]);
		for (final RenderWidget unit : this.widgets) {
			final MdxComplexInstance instance = unit.getInstance();
			if (instance.shown() && instance.isVisible(this.worldScene.camera)
					&& instance.intersectRayWithCollisionSimple(gdxRayHeap, intersectionHeap)) {
				if (filter.call(unit.getSimulationWidget())) {
					final float groundHeight = this.terrain.getGroundHeight(intersectionHeap.x, intersectionHeap.y);
					if (intersectionHeap.z > groundHeight) {
						if ((entity == null) && !unit.isIntersectedOnMeshAlways()) {
							entity = unit;
						} else {
							if (instance.intersectRayWithMeshSlow(gdxRayHeap, intersectionHeap)) {
								if (intersectionHeap.z > this.terrain.getGroundHeight(intersectionHeap.x,
										intersectionHeap.y)) {
									this.worldScene.camera.worldToCamera(intersectionHeap, intersectionHeap);
									if ((entity == null) || (intersectionHeap.z < intersectionHeap2.z)) {
										entity = unit;
										intersectionHeap2.set(intersectionHeap);
									}
								}
							}
						}
					}
				}
			}
		}
		if (entity == null) {
			entity = pickConstructionByFootprint(x, y, filter);
		}
		return entity;
	}

	/**
	 * A building still rising out of the ground (Night Elf birth animation) has its collision shapes below the
	 * terrain, so the model ray test misses it; the original still selects it by clicking where it stands.
	 */
	private RenderWidget pickConstructionByFootprint(final float x, final float y,
			final CWidgetFilterFunction filter) {
		getClickLocation(this.constructionPickHeap, (int) x, (int) y, false, false);
		final float pickX = this.constructionPickHeap.x;
		final float pickY = this.constructionPickHeap.y;
		RenderWidget best = null;
		float bestDistance = Float.MAX_VALUE;
		for (final RenderWidget widget : this.widgets) {
			if (!(widget instanceof RenderUnit)) {
				continue;
			}
			final CUnit unit = ((RenderUnit) widget).getSimulationUnit();
			if (!unit.isBuilding() || !unit.isConstructing() || !widget.getInstance().shown()
					|| !filter.call(unit)) {
				continue;
			}
			final float radius = Math.max(unit.getUnitType().getCollisionSize(), 48f);
			final float dx = unit.getX() - pickX;
			final float dy = unit.getY() - pickY;
			final float distance = (dx * dx) + (dy * dy);
			if ((distance <= (radius * radius)) && (distance < bestDistance)) {
				best = widget;
				bestDistance = distance;
			}
		}
		return best;
	}

	/**
	 * A fingertip covers more ground than a trunk, so a tap that misses every tree model takes the tree nearest the
	 * touched ground point, when that point lies within the tree's selection circle widened by the finger tolerance.
	 */
	public RenderWidget pickNearestTree(final float x, final float y, final float tolerancePx,
			final CWidgetFilterFunction filter) {
		getClickLocation(this.constructionPickHeap, (int) x, (int) y, false, false);
		final float pickX = this.constructionPickHeap.x;
		final float pickY = this.constructionPickHeap.y;
		getClickLocation(this.constructionPickHeap, (int) (x + tolerancePx), (int) y, false, false);
		final float tolerance = (float) Math.hypot(this.constructionPickHeap.x - pickX,
				this.constructionPickHeap.y - pickY);
		RenderWidget best = null;
		float bestDistance = Float.MAX_VALUE;
		for (final RenderWidget widget : this.widgets) {
			if (!(widget instanceof RenderDestructable)) {
				continue;
			}
			final CDestructable tree = ((RenderDestructable) widget).getSimulationDestructable();
			if (tree.isDead() || !tree.getDestType().getTargetedAs().contains(CTargetType.TREE)
					|| !widget.getInstance().shown() || !widget.getInstance().isVisible(this.worldScene.camera)) {
				continue;
			}
			final float radius = (widget.getSelectionScale() / 2) + tolerance;
			final float dx = tree.getX() - pickX;
			final float dy = tree.getY() - pickY;
			final float distance = (dx * dx) + (dy * dy);
			if ((distance <= (radius * radius)) && (distance < bestDistance) && filter.call(tree)) {
				best = widget;
				bestDistance = distance;
			}
		}
		return best;
	}

	private static final class MappedDataCallbackImplementation implements LoadGenericCallback {
		@Override
		public Object call(final InputStream data) {
			final StringBuilder stringBuilder = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(data, "utf-8"))) {
				String line;
				while ((line = reader.readLine()) != null) {
					stringBuilder.append(line);
					stringBuilder.append("\n");
				}
			} catch (final UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
			return new MappedData(stringBuilder.toString());
		}
	}

	private static final class StringDataCallbackImplementation implements LoadGenericCallback {
		@Override
		public Object call(final InputStream data) {
			if (data == null) {
				System.err.println("data null");
			}
			final StringBuilder stringBuilder = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(data, "utf-8"))) {
				String line;
				while ((line = reader.readLine()) != null) {
					stringBuilder.append(line);
					stringBuilder.append("\n");
				}
			} catch (final UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
			return stringBuilder.toString();
		}
	}

	private static final class StreamDataCallbackImplementation implements LoadGenericCallback {
		@Override
		public Object call(final InputStream data) {
			return data;
		}
	}

	public static final class SolverParams {
		public char tileset;
		public boolean reforged;
		public boolean hd;
	}

	public static final class CliffInfo {
		public List<float[]> locations = new ArrayList<>();
		public List<Integer> textures = new ArrayList<>();
	}

	private static final int MAXIMUM_ACCEPTED = 1 << 30;
	float selectionCircleScaleFactor;
	private DataTable worldEditData;
	private WorldEditStrings worldEditStrings;
	Warcraft3MapRuntimeObjectData allObjectData;
	AbilityDataUI abilityDataUI;
	public int imageWalkableZOffset;
	private WTS preloadedWTS;

	private Color selectionCircleColorFriend;

	private Color selectionCircleColorNeutral;

	private Color selectionCircleColorEnemy;

	private int localPlayerServerSlot;

	private final Rectangle tempBlightRect = new Rectangle();
	private int[] blightChangedCorners = new int[512];
	private float[] blightSources = new float[48];
	private int blightSourceCount;

	RenderUnitTypeData renderUnitTypeData;
	RenderItemTypeData renderItemTypeData;

	/**
	 * Returns a power of two size for the given target capacity.
	 */
	private static final int pow2GreaterThan(final int capacity) {
		int numElements = capacity - 1;
		numElements |= numElements >>> 1;
		numElements |= numElements >>> 2;
		numElements |= numElements >>> 4;
		numElements |= numElements >>> 8;
		numElements |= numElements >>> 16;
		return numElements < 0 ? 1 : numElements >= MAXIMUM_ACCEPTED ? MAXIMUM_ACCEPTED : numElements + 1;
	}

	public void standOnRepeat(final MdxComplexInstance instance) {
		instance.setSequenceLoopMode(SequenceLoopMode.ALWAYS_LOOP);
		SequenceUtils.randomStandSequence(instance);
	}

	private static final class SelectionCircleSize {
		private final float size;
		private final String texture;
		private final String textureDotted;

		public SelectionCircleSize(final float size, final String texture, final String textureDotted) {
			this.size = size;
			this.texture = texture;
			this.textureDotted = textureDotted;
		}
	}

	public void setDayNightModels(final String terrainDNCFile, final String unitDNCFile) {
		this.dayNightTerrainModel = terrainDNCFile;
		this.dayNightUnitModel = unitDNCFile;
		final MdxModel terrainDNCModel = loadModelMdx(terrainDNCFile);
		this.dncTerrain = (MdxComplexInstance) terrainDNCModel.addInstance();
		this.dncTerrain.setSequenceLoopMode(SequenceLoopMode.ALWAYS_LOOP);
		this.dncTerrain.setSequence(0);
		final MdxModel unitDNCModel = loadModelMdx(unitDNCFile);
		this.dncUnit = (MdxComplexInstance) unitDNCModel.addInstance();
		this.dncUnit.setSequenceLoopMode(SequenceLoopMode.ALWAYS_LOOP);
		this.dncUnit.setSequence(0);
		final MdxModel targetDNCModel = loadModelMdx(
				"Environment\\DNC\\DNCLordaeron\\DNCLordaeronTarget\\DNCLordaeronTarget.mdl");
		this.dncTarget = (MdxComplexInstance) targetDNCModel.addInstance();
		this.dncTarget.setSequenceLoopMode(SequenceLoopMode.ALWAYS_LOOP);
		this.dncTarget.setSequence(0);
	}

	public static String mdx(String mdxPath) {
		if (mdxPath.toLowerCase().endsWith(".mdl")) {
			mdxPath = mdxPath.substring(0, mdxPath.length() - 4);
		}
		if (!mdxPath.toLowerCase().endsWith(".mdx")) {
			mdxPath += ".mdx";
		}
		return mdxPath;
	}

	public static String mdl(String mdxPath) {
		if (mdxPath.toLowerCase().endsWith(".mdx")) {
			mdxPath = mdxPath.substring(0, mdxPath.length() - 4);
		}
		if (!mdxPath.toLowerCase().endsWith(".mdl")) {
			mdxPath += ".mdl";
		}
		return mdxPath;
	}

	public String blp(String iconPath) {
		final int lastDotIndex = iconPath.lastIndexOf('.');
		if (lastDotIndex != -1) {
			iconPath = iconPath.substring(0, lastDotIndex);
		}
		if (!iconPath.toLowerCase().endsWith(".blp")) {
			iconPath += ".blp";
		}
		return iconPath;
	}

	@Override
	public SceneLightManager createLightManager(final boolean simple) {
		if (simple) {
			return new W3xScenePortraitLightManager(this, this.lightDirection);
		} else {
			return new W3xSceneWorldLightManager(this);
		}
	}

	@Override
	public WorldEditStrings getWorldEditStrings() {
		return this.worldEditStrings;
	}

	public void setGameUI(final GameUI gameUI) {
		this.gameUI = gameUI;
		this.abilityDataUI = new AbilityDataUI(this.allObjectData, gameUI, this);
	}

	public GameUI getGameUI() {
		return this.gameUI;
	}

	public AbilityDataUI getAbilityDataUI() {
		return this.abilityDataUI;
	}

	public KeyedSounds getUiSounds() {
		return this.uiSounds;
	}

	/** Scripts name labels of unit response, combat and animation sounds as well as UI and ambience ones. */
	public Element getSoundLabel(final String label) {
		if ((label == null) || label.isEmpty()) {
			return null;
		}
		for (final DataTable table : new DataTable[] { this.uiSoundsTable, this.unitAckSoundsTable,
				this.unitCombatSoundsTable, this.animSoundsTable }) {
			final Element row = table == null ? null : table.get(label);
			if (row != null) {
				return row;
			}
		}
		if (this.scriptSoundsTable == null) {
			// Campaign dialogue alone is thousands of rows that only scripts look up.
			this.scriptSoundsTable = new DataTable(this.worldEditStrings);
			for (final String name : new String[] { "DialogSounds", "MIDISounds" }) {
				try (InputStream stream = this.dataSource.getResourceAsStream("UI\\SoundInfo\\" + name + ".slk")) {
					if (stream != null) {
						this.scriptSoundsTable.readSLK(stream);
					}
				}
				catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
		return this.scriptSoundsTable.get(label);
	}

	public Warcraft3MapRuntimeObjectData getAllObjectData() {
		return this.allObjectData;
	}

	public float getWalkableRenderHeight(final float x, final float y) {
		this.walkableObjectsTree.intersect(x, y, this.walkablesIntersector.reset(x, y));
		return this.walkablesIntersector.z;
	}

	public MdxComplexInstance getHighestWalkableUnder(final float x, final float y) {
		this.walkableObjectsTree.intersect(x, y, this.intersectorFindsHighestWalkable.reset(x, y));
		return this.intersectorFindsHighestWalkable.highestInstance;
	}

	public int getLocalPlayerIndex() {
		return this.localPlayerIndex;
	}

	public RenderFogGhost addFogGhost(final RenderUnit unit) {
		final RenderFogGhost ghost = new RenderFogGhost(this, unit);
		this.fogGhosts.add(ghost);
		return ghost;
	}

	public List<RenderFogGhost> getFogGhosts() {
		return this.fogGhosts;
	}

	public com.etheller.warsmash.viewer5.handlers.w3x.ui.AllyColorFilter getAllyColorFilter() {
		if (this.allyColorFilter == null) {
			this.allyColorFilter = new com.etheller.warsmash.viewer5.handlers.w3x.ui.AllyColorFilter(this.miscData);
			this.allyColorFilter.setState(com.etheller.warsmash.viewer5.handlers.w3x.ui.GameOptions.get().getAllyFilter());
		}
		return this.allyColorFilter;
	}

	/** DisableRestartMission: the game menu's Restart button stays greyed out. */
	public float getTimeScale() {
		return this.timeScale;
	}

	/** Steps per frame follow it; the steps themselves stay the same length. */
	public void setTimeScale(final float timeScale) {
		this.timeScale = timeScale;
	}

	public static void setDefaultTimeScale(final float timeScale) {
		defaultTimeScale = timeScale;
	}

	/** The game's VolumeGroupSetVolume scales. */
	public CSoundMixer getSoundMixer() {
		return this.soundMixer;
	}

	public CGameExit getGameExit() {
		return this.gameExit;
	}

	public boolean isRestartMissionDisabled() {
		return this.restartMissionDisabled;
	}

	public void setRestartMissionDisabled(final boolean restartMissionDisabled) {
		this.restartMissionDisabled = restartMissionDisabled;
	}

	public String getAltMinimapIcon() {
		return this.altMinimapIcon;
	}

	public void setAltMinimapIcon(final String altMinimapIcon) {
		this.altMinimapIcon = altMinimapIcon;
	}

	public RenderUnit getRenderPeer(final CUnit unit) {
		return this.unitToRenderPeer.get(unit);
	}

	public int getMissileTeamColor(final CUnit source) {
		final RenderUnit renderPeer = getRenderPeer(source);
		if (renderPeer != null) {
			return renderPeer.playerIndex;
		}
		// Missiles outlive their source: a bounce or a delayed launch can come after the unit was removed.
		return this.simulation.getPlayer(source.getPlayerIndex()).getColor();
	}

	public RenderDestructable getRenderPeer(final CDestructable dest) {
		return this.destructableToRenderPeer.get(dest);
	}

	public RenderItem getRenderPeer(final CItem item) {
		return this.itemToRenderPeer.get(item);
	}

	public RenderWidget getRenderPeer(final CWidget damagedDestructable) {
		RenderWidget damagedWidget = War3MapViewer.this.unitToRenderPeer.get(damagedDestructable);
		if (damagedWidget == null) {
			damagedWidget = War3MapViewer.this.destructableToRenderPeer.get(damagedDestructable);
		}
		if (damagedWidget == null) {
			damagedWidget = War3MapViewer.this.itemToRenderPeer.get(damagedDestructable);
		}
		return damagedWidget;
	}

	private static final class QuadtreeIntersectorFindsWalkableRenderHeight
			implements QuadtreeIntersector<MdxComplexInstance> {
		private float z;
		private final Ray ray = new Ray();
		private final Vector3 intersection = new Vector3();

		private QuadtreeIntersectorFindsWalkableRenderHeight reset(final float x, final float y) {
			this.z = -Float.MAX_VALUE;
			this.ray.set(x, y, 4096, 0, 0, -8192);
			return this;
		}

		@Override
		public boolean onIntersect(final MdxComplexInstance intersectingObject) {
			if (intersectingObject.intersectRayWithCollision(this.ray, this.intersection, true, true)) {
				this.z = Math.max(this.z, this.intersection.z);
			}
			return false;
		}
	}

	private static final class QuadtreeIntersectorFindsHighestWalkable
			implements QuadtreeIntersector<MdxComplexInstance> {
		private float z;
		private final Ray ray = new Ray();
		private final Vector3 intersection = new Vector3();
		private MdxComplexInstance highestInstance;

		private QuadtreeIntersectorFindsHighestWalkable reset(final float x, final float y) {
			this.z = -Float.MAX_VALUE;
			this.ray.set(x, y, 4096, 0, 0, -8192);
			this.highestInstance = null;
			return this;
		}

		@Override
		public boolean onIntersect(final MdxComplexInstance intersectingObject) {
			if (intersectingObject.intersectRayWithCollision(this.ray, this.intersection, true, true)) {
				if (this.intersection.z > this.z) {
					this.z = this.intersection.z;
					this.highestInstance = intersectingObject;
				}
			}
			return false;
		}
	}

	private static final class QuadtreeIntersectorFindsHitPoint implements QuadtreeIntersector<MdxComplexInstance> {
		private Ray ray;
		private final Vector3 intersection = new Vector3();
		private boolean found;

		private QuadtreeIntersectorFindsHitPoint reset(final Ray ray) {
			this.ray = ray;
			this.found = false;
			return this;
		}

		@Override
		public boolean onIntersect(final MdxComplexInstance intersectingObject) {
			if (intersectingObject.intersectRayWithCollision(this.ray, this.intersection, true, true)) {
				this.found = true;
				return true;
			}
			return false;
		}
	}

	public void add(final TextTag textTag) {
		this.textTags.add(textTag);
	}

	public SettableCommandErrorListener getCommandErrorListener() {
		return this.commandErrorListener;
	}

	public War3MapConfig getMapConfig() {
		return this.mapConfig;
	}

	public void setLocalPlayerIndex(final int playerIndex) {
		// TODO this is HACKY to not do this on INIT, but it is a cheese way to try to
		// do the networking for now!!!
		this.localPlayerIndex = playerIndex;
	}

	public void setLocalPlayerServerSlot(final int localPlayerServerSlot) {
		this.localPlayerServerSlot = localPlayerServerSlot;
	}

	public boolean isNetworkGame() {
		return this.gameTurnManager instanceof com.etheller.warsmash.networking.WarsmashClient;
	}

	public void setGameTurnManager(final GameTurnManager gameTurnManager) {
		this.gameTurnManager = gameTurnManager;
	}

	public RenderSpellEffect spawnSpellEffectOnUnitEx(final CWidget unit, final War3ID alias,
			final CEffectType effectType, final int index) {
		final EffectAttachmentUI effectAttachmentUI = getEffectAttachmentUI(alias, effectType, index);
		if (effectAttachmentUI == null) {
			return null;
		}
		final String modelPath = effectAttachmentUI.getModelPath();
		final List<String> attachmentPoint = effectAttachmentUI.getAttachmentPoint();
		final RenderSpellEffect specialEffect = addSpecialEffectTarget(modelPath, unit, attachmentPoint);
		return specialEffect;
	}

	public RenderSpellEffect spawnSpellEffectOnUnitEx(final CWidget unit, final War3ID alias,
			final CEffectType effectType, final int index, final String attachPointName) {
		final EffectAttachmentUI effectAttachmentUI = getEffectAttachmentUI(alias, effectType, index);
		if (effectAttachmentUI == null) {
			return null;
		}
		final String modelPath = effectAttachmentUI.getModelPath();
		final RenderSpellEffect specialEffect = addSpecialEffectTarget(modelPath, unit,
				Arrays.asList(attachPointName.split(",")));
		return specialEffect;
	}

	public List<RenderSpellEffect> spawnSpellEffectOnUnitEx(final CUnit unit, final War3ID alias,
			final CEffectType effectType) {
		final List<EffectAttachmentUI> effectAttachmentUI = getEffectAttachmentUIList(alias, effectType);
		if (effectAttachmentUI == null) {
			return null;
		}
		final List<RenderSpellEffect> renderEffects = new ArrayList<>();
		for (final EffectAttachmentUI effect : effectAttachmentUI) {
			final String modelPath = effect.getModelPath();
			final List<String> attachmentPoint = effect.getAttachmentPoint();
			final RenderSpellEffect specialEffect = addSpecialEffectTarget(modelPath, unit, attachmentPoint);
			if (specialEffect != null) {
				renderEffects.add(specialEffect);
			}
		}
		return renderEffects;
	}

	public RenderSpellEffect spawnSpellEffectEx(final float x, final float y, final float facing, final War3ID alias,
			final CEffectType effectType, final int index) {
		final EffectAttachmentUI effectAttachmentUI = getEffectAttachmentUI(alias, effectType, index);
		if (effectAttachmentUI == null) {
			return null;
		}
		final String modelPath = effectAttachmentUI.getModelPath();
		final List<String> attachmentPoint = effectAttachmentUI.getAttachmentPoint();
		final RenderSpellEffect specialEffect = addSpecialEffect(modelPath, x, y, (float) StrictMath.toRadians(facing));
		return specialEffect;
	}

	public List<EffectAttachmentUI> getEffectAttachmentUIList(final War3ID alias, final CEffectType effectType) {
		final AbilityUI abilityUI = War3MapViewer.this.abilityDataUI.getUI(alias);
		List<EffectAttachmentUI> effectAttachmentUI = null;
		if (abilityUI != null) {
			switch (effectType) {
			case EFFECT:
				effectAttachmentUI = abilityUI.getEffectArt();
				break;
			case TARGET:
				effectAttachmentUI = abilityUI.getTargetArt();
				break;
			case CASTER:
				effectAttachmentUI = abilityUI.getCasterArt();
				break;
			case SPECIAL:
				effectAttachmentUI = abilityUI.getSpecialArt();
				break;
			case AREA_EFFECT:
				effectAttachmentUI = abilityUI.getAreaEffectArt();
				break;
			case MISSILE:
				effectAttachmentUI = new ArrayList<>(abilityUI.getMissileArt());
				break;
			default:
				throw new IllegalArgumentException("Unsupported effect type: " + effectType);
			}
		} else {
			final BuffUI buffUI = War3MapViewer.this.abilityDataUI.getBuffUI(alias);
			if (buffUI != null) {
				switch (effectType) {
				case EFFECT:
					effectAttachmentUI = buffUI.getEffectArt();
					break;
				case TARGET:
					effectAttachmentUI = buffUI.getTargetArt();
					break;
				case SPECIAL:
					effectAttachmentUI = buffUI.getSpecialArt();
					break;
				case MISSILE:
					effectAttachmentUI = new ArrayList<>(buffUI.getMissileArt());
					break;
				default:
					throw new IllegalArgumentException("Unsupported effect type: " + effectType);
				}
			} else {
				return null;
			}
		}
		return effectAttachmentUI;
	}

	public EffectAttachmentUI getEffectAttachmentUI(final War3ID alias, final CEffectType effectType, final int index) {
		final AbilityUI abilityUI = War3MapViewer.this.abilityDataUI.getUI(alias);
		EffectAttachmentUI effectAttachmentUI = null;
		if (abilityUI != null) {
			switch (effectType) {
			case EFFECT:
				effectAttachmentUI = abilityUI.getEffectArt(index);
				break;
			case TARGET:
				effectAttachmentUI = abilityUI.getTargetArt(index);
				break;
			case CASTER:
				effectAttachmentUI = abilityUI.getCasterArt(index);
				break;
			case SPECIAL:
				effectAttachmentUI = abilityUI.getSpecialArt(index);
				break;
			case AREA_EFFECT:
				effectAttachmentUI = abilityUI.getAreaEffectArt(index);
				break;
			case MISSILE:
				effectAttachmentUI = abilityUI.getMissileArt(index);
				break;
			default:
				throw new IllegalArgumentException("Unsupported effect type: " + effectType);
			}
		} else {
			final BuffUI buffUI = War3MapViewer.this.abilityDataUI.getBuffUI(alias);
			if (buffUI != null) {
				switch (effectType) {
				case EFFECT:
					effectAttachmentUI = buffUI.getEffectArt(index);
					break;
				case TARGET:
					effectAttachmentUI = buffUI.getTargetArt(index);
					break;
				case SPECIAL:
					effectAttachmentUI = buffUI.getSpecialArt(index);
					break;
				case MISSILE:
					effectAttachmentUI = buffUI.getMissileArt(index);
					break;
				default:
					throw new IllegalArgumentException("Unsupported effect type: " + effectType);
				}
			} else {
				return null;
			}
		}
		return effectAttachmentUI;
	}

	public List<War3ID> getLightningEffectList(final War3ID alias) {
		final AbilityUI abilityUI = War3MapViewer.this.abilityDataUI.getUI(alias);
		if (abilityUI == null) {
			// Mana Flare names its lightning on the buff.
			final BuffUI buffUI = War3MapViewer.this.abilityDataUI.getBuffUI(alias);
			return buffUI == null ? Collections.emptyList() : buffUI.getLightningEffects();
		}
		return abilityUI.getLightningEffects();
	}

	public RenderSpellEffect addSpecialEffectTarget(final String modelName, final CWidget targetWidget,
			final String attachPointName) {
		return addSpecialEffectTarget(modelName, targetWidget, Arrays.asList(attachPointName.split("\\s+")));
	}

	public RenderSpellEffect addSpecialEffectTarget(final String modelName, final CWidget targetWidget,
			List<String> attachPointNames) {
		if (targetWidget instanceof CUnit) {
			if (attachPointNames.isEmpty() || ((attachPointNames.size() == 1) && attachPointNames.get(0).isEmpty())) {
				attachPointNames = ORIGIN_STRING_LIST;
			}
			final RenderUnit renderUnit = War3MapViewer.this.unitToRenderPeer.get(targetWidget);
			if (renderUnit == null) {
				final NullPointerException nullPointerException = new NullPointerException(
						"renderUnit is null! targetWidget is \"" + ((CUnit) targetWidget).getUnitType().getName()
								+ "\", attachPointName=\"" + attachPointNames + "\"");
				if (WarsmashConstants.ENABLE_DEBUG) {
					throw nullPointerException;
				} else {
					nullPointerException.printStackTrace();
				}
			}
			final MdxModel spawnedEffectModel = loadModelMdx(modelName);
			if (spawnedEffectModel != null) {
				final MdxComplexInstance modelInstance = (MdxComplexInstance) spawnedEffectModel.addInstance();
				float yaw = 0;
				if (renderUnit != null) {
					modelInstance.setTeamColor(renderUnit.playerIndex);
					{
						final MdxModel model = (MdxModel) renderUnit.instance.model;
						int index = -1;
						int bestFitAttachmentNameLength = Integer.MAX_VALUE;
						for (int i = 0; i < model.attachments.size(); i++) {
							final Attachment attachment = model.attachments.get(i);
							boolean match = true;
							for (final String attachmentPointNameToken : attachPointNames) {
								if (!attachment.getName().contains(attachmentPointNameToken)) {
									match = false;
								}
							}
							final int attachmentNameLength = attachment.getName().length();
							if (match && (attachmentNameLength < bestFitAttachmentNameLength)) {
								index = i;
								bestFitAttachmentNameLength = attachmentNameLength;
							}
						}
						if (index != -1) {
							modelInstance.detach();
							final MdxNode attachment = renderUnit.instance.getAttachment(index);
							modelInstance.setParent(attachment);
							modelInstance.setLocation(0, 0, 0);
						} else {
							// TODO This is not consistent with War3, is it? Should look nice though.
							modelInstance.setLocation(renderUnit.location);
							yaw = (float) Math.toRadians(renderUnit.getSimulationUnit().getFacing());
						}
					}
				} else {
					modelInstance.setLocation(0, 0, 0);
				}
				modelInstance.setScene(War3MapViewer.this.worldScene);
				final EnumSet<SecondaryTag> requiredAnimationNamesForAttachments = renderUnit == null
						? SequenceUtils.EMPTY
						: renderUnit.getTypeData().getRequiredAnimationNamesForAttachments();
				final RenderSpellEffect renderAttackInstant = new RenderSpellEffect(modelInstance, War3MapViewer.this,
						yaw, RenderSpellEffect.DEFAULT_ANIMATION_QUEUE, requiredAnimationNamesForAttachments);
				renderAttackInstant.setOrigin(modelName, targetWidget, attachPointNames, 0, 0, 0);
				War3MapViewer.this.projectiles.add(renderAttackInstant);
				return renderAttackInstant;
			}
		} else if (targetWidget instanceof CItem) {
			// TODO this is stupid api, who would do this?
			throw new UnsupportedOperationException("API for addSpecialEffectTarget() on item is NYI");
		} else if (targetWidget instanceof CDestructable) {
			if (attachPointNames.isEmpty() || ((attachPointNames.size() == 1) && attachPointNames.get(0).isEmpty())) {
				attachPointNames = ORIGIN_STRING_LIST;
			}
			final RenderDestructable renderDest = War3MapViewer.this.destructableToRenderPeer.get(targetWidget);

			if (renderDest == null) {
				final NullPointerException nullPointerException = new NullPointerException(
						"renderDest is null! targetWidget is \""
								+ ((CDestructable) targetWidget).getDestType().getName() + "\", attachPointName=\""
								+ attachPointNames + "\"");
				if (WarsmashConstants.ENABLE_DEBUG) {
					throw nullPointerException;
				} else {
					nullPointerException.printStackTrace();
				}
			}
			final MdxModel spawnedEffectModel = loadModelMdx(modelName);
			if (spawnedEffectModel != null) {
				final MdxComplexInstance modelInstance = (MdxComplexInstance) spawnedEffectModel.addInstance();
				if (renderDest != null) {
					{
						final MdxModel model = (MdxModel) renderDest.instance.model;
						int index = -1;
						int bestFitAttachmentNameLength = Integer.MAX_VALUE;
						for (int i = 0; i < model.attachments.size(); i++) {
							final Attachment attachment = model.attachments.get(i);
							boolean match = true;
							for (final String attachmentPointNameToken : attachPointNames) {
								if (!attachment.getName().contains(attachmentPointNameToken)) {
									match = false;
								}
							}
							final int attachmentNameLength = attachment.getName().length();
							if (match && (attachmentNameLength < bestFitAttachmentNameLength)) {
								index = i;
								bestFitAttachmentNameLength = attachmentNameLength;
							}
						}
						if (index != -1) {
							modelInstance.detach();
							final MdxNode attachment = renderDest.getInstance().getAttachment(index);
							modelInstance.setParent(attachment);
							modelInstance.setLocation(0, 0, 0);
						} else {
							// TODO This is not consistent with War3, is it? Should look nice though.
							modelInstance.setLocation(renderDest.getX(), renderDest.getY(), 0);
						}
					}
				} else {
					modelInstance.setLocation(0, 0, 0);
				}
				modelInstance.setScene(War3MapViewer.this.worldScene);
				final RenderSpellEffect renderAttackInstant = new RenderSpellEffect(modelInstance, War3MapViewer.this,
						0, RenderSpellEffect.DEFAULT_ANIMATION_QUEUE, SequenceUtils.EMPTY);
				renderAttackInstant.setOrigin(modelName, targetWidget, attachPointNames, 0, 0, 0);
				War3MapViewer.this.projectiles.add(renderAttackInstant);
				return renderAttackInstant;
			}
		}
		return null;
	}

	public RenderSpellEffect addSpecialEffect(final String modelName, final float x, final float y, final float yaw) {
		final MdxModel spawnedEffectModel = loadModelMdx(modelName);
		if (spawnedEffectModel != null) {
			final MdxComplexInstance modelInstance = (MdxComplexInstance) spawnedEffectModel.addInstance();
			{
				modelInstance.setLocation(x, y,
						Math.max(getWalkableRenderHeight(x, y), this.terrain.getGroundHeight(x, y)));
			}
			modelInstance.setScene(War3MapViewer.this.worldScene);
			final RenderSpellEffect renderAttackInstant = new RenderSpellEffect(modelInstance, War3MapViewer.this, yaw,
					RenderSpellEffect.DEFAULT_ANIMATION_QUEUE, SequenceUtils.EMPTY);
			renderAttackInstant.setOrigin(modelName, null, null, x, y, yaw);
			War3MapViewer.this.projectiles.add(renderAttackInstant);
			return renderAttackInstant;
		}
		return null;
	}

	@Override
	public MdxModel loadModelMdx(final String path) {
		return loadModelMdx(this.dataSource, this, path, this.mapPathSolver, this.solverParams);
	}

	public static MdxModel loadModelMdx(final DataSource dataSource, final ModelViewer modelViewer, final String path,
			final PathSolver pathSolver, final Object solverParams) {
		// "_" is the data's "no model" entry (e.g. empty art fields in the 1.29 SLKs).
		if ("".equals(path) || "_".equals(path) || "_.mdl".equalsIgnoreCase(path)
				|| "_.mdx".equalsIgnoreCase(path)) {
			return null;
		}
		final String mdxPath = mdx(path);
		if (dataSource.has(mdxPath)) {
			return (MdxModel) modelViewer.load(mdxPath, pathSolver, solverParams);
		} else {
			final String mdlPath = mdl(mdxPath);
			if (dataSource.has(mdlPath)) {
				return (MdxModel) modelViewer.load(mdlPath, pathSolver, solverParams);
			}
		}
		return (MdxModel) modelViewer.load(mdxPath, pathSolver, solverParams);
	}

	public void setBlight(final float whichLocationX, final float whichLocationY, final float radius,
			final boolean blighted) {
		setBlight(whichLocationX, whichLocationY, radius, false, blighted);
	}

	public void setBlight(final float whichLocationX, final float whichLocationY, final float radius,
			final boolean square, final boolean blighted) {
		// Measured on the original game: blight is set on the terrain corners (every 128) around the corner nearest
		// to the point (halves rounded away from zero), with the radius rounded to whole corners and a corner counted
		// when it is within that many corners plus a little; a 128 tile is blighted, for building and blight checks
		// too, when any of its four corners is.
		final int cornerRadius = getBlightCornerRadius(radius);
		final float cornerReach = cornerRadius + 0.35f;
		final float centerX = roundHalfAwayFromZero(whichLocationX / 128f) * 128f;
		final float centerY = roundHalfAwayFromZero(whichLocationY / 128f) * 128f;
		int changedCount = 0;
		int minI = Integer.MAX_VALUE;
		int maxI = Integer.MIN_VALUE;
		int minJ = Integer.MAX_VALUE;
		int maxJ = Integer.MIN_VALUE;
		boolean sourcesFound = false;
		for (int i = -cornerRadius; i <= cornerRadius; i++) {
			for (int j = -cornerRadius; j <= cornerRadius; j++) {
				if (!square && (((i * i) + (j * j)) > (cornerReach * cornerReach))) {
					continue;
				}
				final float x = centerX + (i * 128f);
				final float y = centerY + (j * 128f);
				final RenderCorner corner = this.terrain.getCorner(x, y);
				if ((corner == null) || ((corner.getBlight() != 0) == blighted)) {
					continue;
				}
				if (!blighted) {
					if (!sourcesFound) {
						findStandingBlightSources(centerX, centerY, cornerRadius);
						sourcesFound = true;
					}
					if (isInStandingBlightSource(x, y)) {
						continue;
					}
				}
				corner.setBlight(blighted);
				if (this.blightChangedCorners.length < ((changedCount + 1) * 2)) {
					this.blightChangedCorners = Arrays.copyOf(this.blightChangedCorners,
							this.blightChangedCorners.length * 2);
				}
				this.blightChangedCorners[changedCount * 2] = i;
				this.blightChangedCorners[(changedCount * 2) + 1] = j;
				changedCount++;
				minI = Math.min(minI, i);
				maxI = Math.max(maxI, i);
				minJ = Math.min(minJ, j);
				maxJ = Math.max(maxJ, j);
			}
		}
		if (changedCount == 0) {
			return;
		}
		final PathingGrid pathingGrid = this.simulation.getPathingGrid();
		for (int k = 0; k < changedCount; k++) {
			final float cornerX = centerX + (this.blightChangedCorners[k * 2] * 128f);
			final float cornerY = centerY + (this.blightChangedCorners[(k * 2) + 1] * 128f);
			for (int a = -1; a <= 0; a++) {
				for (int b = -1; b <= 0; b++) {
					final float tileX = cornerX + (a * 128f);
					final float tileY = cornerY + (b * 128f);
					final boolean tileBlighted = blighted || isCornerBlighted(tileX, tileY)
							|| isCornerBlighted(tileX + 128f, tileY) || isCornerBlighted(tileX, tileY + 128f)
							|| isCornerBlighted(tileX + 128f, tileY + 128f);
					for (float x = tileX + 16f; x < (tileX + 128f); x += 32f) {
						for (float y = tileY + 16f; y < (tileY + 128f); y += 32f) {
							if (pathingGrid.contains(x, y)) {
								pathingGrid.setBlighted(x, y, tileBlighted);
							}
						}
					}
				}
			}
		}
		final float minX = centerX + ((minI - 1) * 128f);
		final float minY = centerY + ((minJ - 1) * 128f);
		final Rectangle blightRectangle = new Rectangle(minX, minY, ((maxI - minI) + 2) * 128f,
				((maxJ - minJ) + 2) * 128f);
		final int cellMinX = this.terrain.get128CellX(centerX + (minI * 128f));
		final int cellMinY = this.terrain.get128CellY(centerY + (minJ * 128f));
		this.terrain.updateGroundTextures(this.tempBlightRect.set(cellMinX, cellMinY, maxI - minI, maxJ - minJ));
		if (blighted) {
			this.simulation.getWorldCollision().enumDestructablesInRect(blightRectangle,
					TreeBlightingCallback.INSTANCE.reset(this.simulation));
		}
	}

	private static int getBlightCornerRadius(final float radius) {
		return (int) ((radius / 128f) + 0.5f);
	}

	/**
	 * Measured: the blight of a standing blight source (Necropolis) cannot be removed, it stays as it was.
	 */
	private void findStandingBlightSources(final float centerX, final float centerY, final int cornerRadius) {
		this.blightSourceCount = 0;
		final float reach = (cornerRadius + 2) * 128f;
		for (final CUnit unit : this.simulation.getUnitsIncludingNew()) {
			final CAbilityBlight blight = unit.getFirstAbilityOfType(CAbilityBlight.class);
			if ((blight == null) || !blight.isCreatesBlight() || blight.isDisabled() || unit.isDead()) {
				continue;
			}
			final int sourceRadius = getBlightCornerRadius(blight.getCurrentRadius());
			final float sourceX = roundHalfAwayFromZero(unit.getX() / 128f) * 128f;
			final float sourceY = roundHalfAwayFromZero(unit.getY() / 128f) * 128f;
			final float sourceReach = reach + (sourceRadius * 128f);
			final float dx = sourceX - centerX;
			final float dy = sourceY - centerY;
			if (((dx * dx) + (dy * dy)) > (sourceReach * sourceReach)) {
				continue;
			}
			if (this.blightSources.length < ((this.blightSourceCount + 1) * 3)) {
				this.blightSources = Arrays.copyOf(this.blightSources, this.blightSources.length * 2);
			}
			final float sourceCornerReach = sourceRadius + 0.35f;
			this.blightSources[this.blightSourceCount * 3] = sourceX;
			this.blightSources[(this.blightSourceCount * 3) + 1] = sourceY;
			this.blightSources[(this.blightSourceCount * 3) + 2] = sourceCornerReach * sourceCornerReach;
			this.blightSourceCount++;
		}
	}

	private boolean isInStandingBlightSource(final float x, final float y) {
		for (int k = 0; k < this.blightSourceCount; k++) {
			final int i = Math.round((x - this.blightSources[k * 3]) / 128f);
			final int j = Math.round((y - this.blightSources[(k * 3) + 1]) / 128f);
			if (((i * i) + (j * j)) <= this.blightSources[(k * 3) + 2]) {
				return true;
			}
		}
		return false;
	}

	private boolean isCornerBlighted(final float x, final float y) {
		final RenderCorner corner = this.terrain.getCorner(x, y);
		return (corner != null) && (corner.getBlight() != 0);
	}

	private static float roundHalfAwayFromZero(final float value) {
		return (int) (value + (value < 0 ? -0.5f : 0.5f));
	}

	/**
	 * The shade a fog state is drawn with, measured on 1.29 against the same view without fog: fog leaves 34 % of the
	 * brightness. Melee maps set "masked areas are partially visible": the black mask then leaves 16 %, half of fog;
	 * campaign maps do not, and their mask stays black.
	 */
	public byte getFogShade(final byte fogState) {
		if (fogState == CFogState.FOGGED.getMask()) {
			return FOGGED_SHADE;
		}
		if ((fogState == CFogState.MASKED.getMask()) && isMaskedAreasPartiallyVisible()) {
			return MASKED_PARTIALLY_VISIBLE_SHADE;
		}
		return fogState;
	}

	public boolean isMaskedAreasPartiallyVisible() {
		return (this.lastLoadedMapInformation != null)
				&& this.lastLoadedMapInformation.hasFlag(War3MapW3iFlags.MASKED_AREAS_ARE_PARTIALLY_VISIBLE);
	}

	/**
	 * A map made for Reforged graphics alone (the Reforged versions of the campaign missions): the game plays it in
	 * Reforged mode, with that mode's interface for its cinematics.
	 */
	public boolean isReforgedGraphicsMap() {
		return (this.lastLoadedMapInformation != null)
				&& (this.lastLoadedMapInformation.getSupportedModes() == SUPPORTED_MODES_REFORGED);
	}

	public CPlayerFogOfWar getFogOfWar() {
		if (this.observerFogOfWar != null) {
			return this.observerFogOfWar;
		}
		if (this.replayVisionPlayer != REPLAY_VISION_NONE) {
			if (!this.replayFogShown || (this.replayVisionPlayer == REPLAY_VISION_ALL_PLAYERS)) {
				return this.replayFogOfWar;
			}
			return this.simulation.getPlayer(this.replayVisionPlayer).getFogOfWar();
		}
		return this.simulation.getPlayer(this.localPlayerIndex).getFogOfWar();
	}

	/**
	 * A replay's Replay Vision and Fog of War: the picture, the minimap and the units shown are what that player (or
	 * every player together) saw, or the whole map without the fog. The game itself, and GetLocalPlayer, stay the
	 * recording player's.
	 */
	public void setReplayVision(final int playerIndex, final boolean fogShown) {
		this.replayVisionPlayer = playerIndex;
		this.replayFogShown = fogShown;
		if (this.replayFogOfWar == null) {
			this.replayFogOfWar = new CPlayerFogOfWar(this.simulation.getPathingGrid());
		}
		this.replayVisionSources.clear();
		for (int i = 0; i < WarsmashConstants.MAX_PLAYERS; i++) {
			final CPlayer player = this.simulation.getPlayer(i);
			if (isReplayVisionPlayer(player)) {
				this.replayVisionSources.add(player.getFogOfWar());
			}
		}
		if (!fogShown) {
			this.replayFogOfWar.revealAll();
		}
		else {
			updateReplayVision();
		}
		if (this.observerFogOfWar == null) {
			this.terrain.setFogOfWarData(this.simulation, getFogOfWar());
		}
	}

	/** The players Replay Vision offers: those who played, not the neutral ones. */
	public boolean isReplayVisionPlayer(final CPlayer player) {
		return (player != null) && (player.getId() < (WarsmashConstants.MAX_PLAYERS - 4))
				&& (player.getSlotState() == CPlayerSlotState.PLAYING);
	}

	private void updateReplayVision() {
		if ((this.replayVisionPlayer == REPLAY_VISION_ALL_PLAYERS) && this.replayFogShown) {
			this.replayFogOfWar.showMostVisibleOf(this.replayVisionSources);
		}
	}

	public int getReplayVisionPlayer() {
		return this.replayVisionPlayer;
	}

	public boolean isReplayFogShown() {
		return this.replayFogShown;
	}

	/** The player whose resources and sight the interface shows: a replay's Replay Vision, else the local player. */
	public int getViewingPlayerIndex() {
		return this.replayVisionPlayer >= 0 ? this.replayVisionPlayer : this.localPlayerIndex;
	}

	/** Whether the picture shows the unit: the local player sees it, or a replay's vision does. */
	public boolean isShownToViewer(final CUnit unit) {
		if ((this.replayVisionPlayer != REPLAY_VISION_NONE) && !this.replayFogShown) {
			return true;
		}
		if (this.replayVisionPlayer == REPLAY_VISION_ALL_PLAYERS) {
			for (int i = 0; i < WarsmashConstants.MAX_PLAYERS; i++) {
				if (isReplayVisionPlayer(this.simulation.getPlayer(i)) && unit.isVisible(this.simulation, i)) {
					return true;
				}
			}
			return false;
		}
		return unit.isVisible(this.simulation, getViewingPlayerIndex());
	}

	public float getReplaySpeed() {
		return this.replaySpeed;
	}

	public void setReplaySpeed(final float replaySpeed) {
		this.replaySpeed = replaySpeed;
	}

	/**
	 * What a player defeated with observers on death sees: the whole map. Only the picture changes, the original's
	 * IsVisibleToPlayer and its like keep answering from the player's own fog.
	 */
	public void startObserverView() {
		if (this.observerFogOfWar == null) {
			this.observerFogOfWar = new CPlayerFogOfWar(this.simulation.getPathingGrid());
			this.observerFogOfWar.revealAll();
			this.terrain.setFogOfWarData(this.simulation, this.observerFogOfWar);
		}
	}

	public boolean isObserverView() {
		return this.observerFogOfWar != null;
	}

	public static byte fadeLineOfSightColor(final byte lastFogStateColor, final byte state) {
		final short prevValue = (short) (lastFogStateColor & 0xFF);
		final short newValue = (short) (state & 0xFF);
		final short delta = (short) (newValue - prevValue);
		final short appliedMagnitude = (short) Math.min(9, Math.abs(delta));

		return (byte) ((prevValue + (appliedMagnitude * (delta < 0 ? -1 : 1))) & 0xFF);
	}

	public final class MapLoader {
		private final LinkedList<LoadMapTask> loadMapTasks = new LinkedList<>();
		private int startingTaskCount;

		private char tileset;
		private DataSource tilesetSource;

		private War3MapW3e terrainData;

		private War3MapWpm terrainPathing;

		private MapLoader(final War3Map war3Map, final War3MapW3i w3iFile, final int localPlayerIndex) {
			final PathSolver wc3PathSolver = War3MapViewer.this.wc3PathSolver;

			this.loadMapTasks.add(() -> {
				this.tileset = 'A';
				this.tileset = w3iFile.getTileset();
				int gameDataSet = w3iFile.getGameDataSet();

				try {
					// Slightly complex. Here's the theory:
					// 1.) Copy map into RAM
					// 2.) Setup a Data Source that will read assets
					// from either the map or the game, giving the map priority.
					SeekableByteChannel sbc;
					final DataSource compoundDataSource = war3Map.getCompoundDataSource();
					final List<DataSource> dataSources = new ArrayList<>();
					dataSources.add(compoundDataSource);
					if (WarsmashConstants.FIX_FLAT_FILES_TILESET_LOADING) {
						dataSources.add(new SubdirDataSource(compoundDataSource, this.tileset + ".mpq/"));
					} else {
						try (InputStream mapStream = compoundDataSource.getResourceAsStream(this.tileset + ".mpq")) {
							if (mapStream == null) {
								dataSources.add(new SubdirDataSource(compoundDataSource, this.tileset + ".mpq/"));
								dataSources.add(new SubdirDataSource(compoundDataSource,
										"_tilesets/" + this.tileset + ".w3mod/"));
							} else {
								final byte[] mapData = IOUtils.toByteArray(mapStream);
								sbc = new SeekableInMemoryByteChannel(mapData);
								final DataSource internalMpqContentsDataSource = new MpqDataSource(new MPQArchive(sbc),
										sbc);
								dataSources.add(internalMpqContentsDataSource);
							}
						} catch (final IOException exc) {
							dataSources.add(new SubdirDataSource(compoundDataSource, this.tileset + ".mpq/"));
							dataSources.add(
									new SubdirDataSource(compoundDataSource, "_tilesets/" + this.tileset + ".w3mod/"));
						}
					}
					// Some tileset textures exist only in the graphics packages (the Stratholme bridge); first in the
					// list, so any Classic file wins over them.
					dataSources.add(0, new SubdirDataSource(compoundDataSource,
							"_hd.w3mod/_tilesets/" + this.tileset + ".w3mod/"));
					dataSources.add(1, new SubdirDataSource(compoundDataSource,
							"_de.w3mod/_tilesets/" + this.tileset + ".w3mod/"));
					if (gameDataSet <= 0) {
						gameDataSet = w3iFile.hasFlag(War3MapW3iFlags.MELEE_MAP) ? 2 : 1;
					}
					if (gameDataSet == 1) {
						// Custom data
						dataSources.add(new SubdirDataSource(compoundDataSource,
								"Custom_V" + WarsmashConstants.GAME_VERSION + "\\"));
					} else {
						// Melee data
						dataSources.add(new SubdirDataSource(compoundDataSource,
								"Melee_V" + WarsmashConstants.GAME_VERSION + "\\"));
					}
					this.tilesetSource = new CompoundDataSource(dataSources);
				} catch (final MPQException e) {
					throw new RuntimeException(e);
				}
			});

			this.loadMapTasks.add(() -> {
				setDataSource(this.tilesetSource);
				War3MapViewer.this.mapMpq.setDataSource(this.tilesetSource);
			});
			this.loadMapTasks.add(() -> {
				War3MapViewer.this.worldEditStrings = new WorldEditStrings(War3MapViewer.this.dataSource);
			});
			this.loadMapTasks.add(() -> {
				loadSLKs(War3MapViewer.this.worldEditStrings);
			});

			this.loadMapTasks.add(() -> {
				War3MapViewer.this.solverParams.tileset = Character.toLowerCase(this.tileset);
			});

			this.loadMapTasks.add(() -> {
				this.terrainData = War3MapViewer.this.mapMpq.readEnvironment();
			});

			this.loadMapTasks.add(() -> {
				this.terrainPathing = War3MapViewer.this.mapMpq.readPathing();
			});

			this.loadMapTasks.add(() -> {
				War3MapViewer.this.terrain = new Terrain(this.terrainData, this.terrainPathing, w3iFile,
						War3MapViewer.this.webGL, War3MapViewer.this.dataSource, War3MapViewer.this.worldEditStrings,
						War3MapViewer.this, War3MapViewer.this.worldEditData);
			});

			this.loadMapTasks.add(() -> {
				final float[] centerOffset = this.terrainData.getCenterOffset();
				final int[] mapSize = this.terrainData.getMapSize();

				War3MapViewer.this.terrainReady = true;
				War3MapViewer.this.anyReady = true;
				War3MapViewer.this.cliffsReady = true;

				// Override the grid based on the map.
				War3MapViewer.this.worldScene.grid = new Grid(centerOffset[0], centerOffset[1],
						(mapSize[0] * 128) - 128, (mapSize[1] * 128) - 128, 16 * 128, 16 * 128);
			});

			this.loadMapTasks.add(() -> {
				final MdxModel confirmation = (MdxModel) load("UI\\Feedback\\Confirmation\\Confirmation.mdx",
						PathSolver.DEFAULT, null);
				War3MapViewer.this.confirmationInstance = (MdxComplexInstance) confirmation.addInstance();
				War3MapViewer.this.confirmationInstance
						.setSequenceLoopMode(SequenceLoopMode.NEVER_LOOP_AND_HIDE_WHEN_DONE);
				War3MapViewer.this.confirmationInstance.setSequence(0);
				War3MapViewer.this.confirmationInstance.setScene(War3MapViewer.this.worldScene);
			});

			this.loadMapTasks.add(() -> {
				if (War3MapViewer.this.preloadedWTS != null) {
					War3MapViewer.this.allObjectData = War3MapViewer.this.mapMpq
							.readModifications(War3MapViewer.this.preloadedWTS);
				} else {
					War3MapViewer.this.allObjectData = War3MapViewer.this.mapMpq.readModifications();
				}
			});
			this.loadMapTasks.add(() -> {
				War3MapViewer.this.renderController = new NoRenderController(
						new War3MapRenderController(War3MapViewer.this));
				War3MapViewer.this.renderController.setPresenting(!NO_RENDER);
				War3MapViewer.this.simulation = new CSimulation(War3MapViewer.this.mapConfig, w3iFile.getVersion(),
						WarsmashConstants.mapGameVersion(War3MapViewer.this.mapFileName, w3iFile.getGameDataVersion()),
						War3MapViewer.this.miscData, War3MapViewer.this.allObjectData.getUnits(),
						War3MapViewer.this.allObjectData.getItems(),
						War3MapViewer.this.allObjectData.getDestructibles(),
						War3MapViewer.this.allObjectData.getAbilities(), War3MapViewer.this.allObjectData.getUpgrades(),
						War3MapViewer.this.allObjectData.getStandardUpgradeEffectMeta(),
						War3MapViewer.this.renderController, War3MapViewer.this.pathingTextures,
						War3MapViewer.this.terrain.pathingGrid,
						CTerrainData.load(this.terrainData, this.tileset, War3MapViewer.this.dataSource,
								War3MapViewer.this.worldEditStrings),
						War3MapViewer.this.terrain.getEntireMap(),
						War3MapViewer.this.seededRandom, War3MapViewer.this.commandErrorListener);
			});

			this.loadMapTasks.add(() -> {
				War3MapViewer.this.walkableObjectsTree = new Quadtree<>(War3MapViewer.this.terrain.getEntireMap());
				if (War3MapViewer.this.doodadsAndDestructiblesLoaded) {
					loadDoodadsAndDestructibles(War3MapViewer.this.allObjectData, w3iFile);
				} else {
					throw new IllegalStateException(
							"transcription of JS has not loaded a map and has no JS async promises");
				}
			});

			this.loadMapTasks.add(() -> {
				loadSounds();
			});

			this.loadMapTasks.add(() -> {
				War3MapViewer.this.terrain.createWaves();
			});

			this.startingTaskCount = this.loadMapTasks.size();
		}

		public boolean process() throws IOException {
			final LoadMapTask nextTask = this.loadMapTasks.pollFirst();
			nextTask.run();
			return this.loadMapTasks.isEmpty();
		}

		public void addLoadTask(final LoadMapTask task) {
			this.loadMapTasks.add(task);
		}

		public float getCompletionRatio() {
			return 1.0f - (this.loadMapTasks.size() / (float) this.startingTaskCount);
		}
	}

	public static interface LoadMapTask {
		void run() throws IOException;
	}

	public float getElapsedSecondsForRender(final CTimer timer) {
		return Math.min(timer.getElapsed(this.simulation) + this.updateTime, timer.getTimeoutTime());
	}

	public float getRemainingSecondsForRender(final CTimer timer) {
		return timer.getTimeoutTime() - getElapsedSecondsForRender(timer);
	}

	public void resetTerrainFog() {
		if (this.defaultFogSettings != null) {
			this.worldScene.fogSettings.color = this.defaultFogSettings.color;
			this.worldScene.fogSettings.style = this.defaultFogSettings.style;
			this.worldScene.fogSettings.density = this.defaultFogSettings.density;
			this.worldScene.fogSettings.start = this.defaultFogSettings.start;
			this.worldScene.fogSettings.end = this.defaultFogSettings.end;
		}
	}
}
