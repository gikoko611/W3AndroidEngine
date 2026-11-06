package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import net.warsmash.map.Crc32c;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CSaveStorage;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CCampaignProgress;
import com.etheller.warsmash.viewer5.handlers.w3x.save.SavedGame;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.etheller.warsmash.input.touch.VirtualCursor;
import com.etheller.warsmash.SingleModelScreen;
import com.etheller.warsmash.WarsmashGdxMapScreen;
import com.etheller.warsmash.WarsmashGdxMenuScreen;
import com.etheller.warsmash.WarsmashGdxMultiScreenGame;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.networking.GameTurnManager;
import com.etheller.warsmash.networking.WarsmashClient;
import com.etheller.warsmash.networking.WarsmashClientSendingOrderListener;
import com.etheller.warsmash.networking.WarsmashClientWriter;
import com.etheller.warsmash.parsers.fdf.GameSkin;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.parsers.fdf.datamodel.AnchorDefinition;
import com.etheller.warsmash.parsers.fdf.datamodel.FramePoint;
import com.etheller.warsmash.parsers.fdf.datamodel.MenuItem;
import com.etheller.warsmash.parsers.fdf.datamodel.TextJustify;
import com.badlogic.gdx.graphics.Texture;
import com.etheller.warsmash.parsers.fdf.frames.AbstractRenderableFrame;
import com.etheller.warsmash.parsers.fdf.frames.AbstractUIFrame;
import com.etheller.warsmash.parsers.fdf.frames.TextureFrame;
import com.etheller.warsmash.util.ImageUtils;
import com.etheller.warsmash.parsers.fdf.frames.BackdropFrame;
import com.etheller.warsmash.parsers.fdf.frames.EditBoxFrame;
import com.etheller.warsmash.parsers.fdf.frames.GlueButtonFrame;
import com.etheller.warsmash.parsers.fdf.frames.GlueTextButtonFrame;
import com.etheller.warsmash.parsers.fdf.frames.ScrollBarFrame;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound.CSoundMixer;
import com.etheller.warsmash.parsers.fdf.frames.ListBoxFrame;
import com.etheller.warsmash.parsers.fdf.frames.ListBoxFrame.ListBoxSelelectionListener;
import com.etheller.warsmash.parsers.fdf.frames.MenuFrame;
import com.etheller.warsmash.parsers.fdf.frames.PopupMenuFrame;
import com.etheller.warsmash.parsers.fdf.frames.SetPoint;
import com.etheller.warsmash.parsers.fdf.frames.SimpleFrame;
import com.etheller.warsmash.parsers.fdf.frames.SpriteFrame;
import com.etheller.warsmash.parsers.fdf.frames.StringFrame;
import com.etheller.warsmash.parsers.fdf.frames.UIFrame;
import com.etheller.warsmash.parsers.jass.Jass2;
import com.etheller.warsmash.parsers.jass.Jass2.RootFrameListener;
import com.etheller.warsmash.parsers.w3x.War3Map;
import com.etheller.warsmash.parsers.w3x.objectdata.Warcraft3MapObjectData;
import com.etheller.warsmash.parsers.w3x.w3i.Player;
import com.etheller.warsmash.parsers.w3x.w3i.War3MapW3i;
import com.etheller.warsmash.parsers.w3x.w3i.War3MapW3iFlags;
import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.units.Element;
import com.etheller.warsmash.units.StandardObjectData;
import com.etheller.warsmash.units.custom.WTS;
import com.etheller.warsmash.util.DataSourceFileHandle;
import com.etheller.warsmash.util.StringBundle;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.util.WorldEditStrings;
import com.etheller.warsmash.viewer5.FogSettings;
import com.etheller.warsmash.viewer5.Scene;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens.PrimaryTag;
import com.etheller.warsmash.viewer5.handlers.w3x.SequenceUtils;
import com.etheller.warsmash.viewer5.handlers.w3x.UnitSound;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer.MapLoader;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.ai.AIDifficulty;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.CBasePlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.War3MapConfig;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.War3MapConfigStartLoc;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CMapControl;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CMapPlacement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderListenerDelaying;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CRace;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CRaceManagerEntry;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CRacePreference;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CPlayerSlotState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CMapDifficulty;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.command.ClickableFrame;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.command.FocusableFrame;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog.DialogWar3;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.mapsetup.AdvancedOptionsPane;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.mapsetup.CurrentNetGameMapLookup;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.mapsetup.CurrentNetGameMapLookupFile;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.mapsetup.CurrentNetGameMapLookupPath;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.mapsetup.MapInfoPane;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.mapsetup.MapListContainer;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.mapsetup.PlayerSlotPaneListener;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.mapsetup.TeamSetupPane;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.menu.BattleNetUI;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.menu.BattleNetUIActionListener;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.menu.CampaignButtonUI;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.menu.CampaignMenuData;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.menu.CampaignMenuUI;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.menu.CampaignCatalog;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.menu.CampaignMission;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.sound.KeyedSounds;
import com.etheller.warsmash.viewer5.gl.Extensions;

import net.warsmash.map.NetMapDownloader;
import net.warsmash.uberserver.AccountCreationFailureReason;
import net.warsmash.uberserver.ChannelServerMessageType;
import net.warsmash.uberserver.GameCreationFailureReason;
import net.warsmash.uberserver.GamingNetwork;
import net.warsmash.uberserver.GamingNetworkConnection;
import net.warsmash.uberserver.GamingNetworkServerToClientListener;
import net.warsmash.uberserver.HandshakeDeniedReason;
import net.warsmash.uberserver.HostedGameVisibility;
import net.warsmash.uberserver.JoinGameFailureReason;
import net.warsmash.uberserver.LobbyGameSpeed;
import net.warsmash.uberserver.LobbyPlayerType;
import net.warsmash.uberserver.LoginFailureReason;
import net.warsmash.uberserver.ServerErrorMessageType;

public class MenuUI {
	private static final Vector2 screenCoordsVector = new Vector2();
	private static boolean ENABLE_NOT_YET_IMPLEMENTED_BUTTONS = false;

	private final DataSource dataSource;
	private final Scene uiScene;
	private final Viewport uiViewport;
	private final MdxViewer viewer;
	private final RootFrameListener rootFrameListener;
	private final float widthRatioCorrection;
	private final float heightRatioCorrection;
	private GameUI rootFrame;
	private SpriteFrame cursorFrame;

	private ClickableFrame mouseDownUIFrame;
	private ClickableFrame mouseOverUIFrame;
	private FocusableFrame focusUIFrame;

	private UIFrame mainMenuFrame;

	private SpriteFrame glueSpriteLayerTopRight;

	private SpriteFrame glueSpriteLayerTopLeft;
	private SpriteFrame glueSpriteLayerCenter;

	private WorldEditStrings worldEditStrings;

	private DataTable uiSoundsTable;

	private KeyedSounds uiSounds;

	private GlueTextButtonFrame singlePlayerButton;
	private GlueTextButtonFrame battleNetButton;
	private GlueTextButtonFrame localAreaNetworkButton;
	private GlueTextButtonFrame optionsButton;
	private GlueTextButtonFrame creditsButton;
	private GlueButtonFrame realmButton;
	private GlueTextButtonFrame exitButton;

	private final boolean quitting = false;

	private MenuState menuState;

	private UIFrame singlePlayerMenu;
	private UIFrame singlePlayerMainPanel;

	// single player skirmish menu ("Custom Game")
	private UIFrame skirmish;
	private UIFrame mapInfoPanel;
	private UIFrame advancedOptionsPanel;
	private GlueTextButtonFrame mapInfoButton;
	private GlueTextButtonFrame advancedOptionsButton;
	private AdvancedOptionsPane skirmishAdvancedOptionsPane;
	private int randomStartLocationCount;

	private UIFrame profilePanel;
	private EditBoxFrame newProfileEditBox;

	private GlueButtonFrame profileButton;
	private GlueTextButtonFrame campaignButton;
	private GlueTextButtonFrame loadSavedButton;
	private SavedGameDialog savedGameDialog;
	/** The custom game screen's map list, its player slots and its Start Game, also used by a launcher. */
	private PlayerSlotPaneListenerImplementation skirmishSlotListener;
	private Runnable skirmishStart;
	private SimpleFrame savedGameHolder;
	private GameOptionsDialog optionsDialog;
	private GlueTextButtonFrame viewReplayButton;
	private GlueTextButtonFrame customCampaignButton;
	private GlueTextButtonFrame skirmishButton;
	private GlueTextButtonFrame singlePlayerCancelButton;
	private GlueButtonFrame editionButton;

	private GlueTextButtonFrame skirmishCancelButton;

	private final WarsmashGdxMultiScreenGame screenManager;

	private final DataTable warsmashIni;

	private UnitSound glueScreenLoop;

	private SpriteFrame warcraftIIILogo;
	// Campaign
	private UIFrame campaignMenu;
	private SpriteFrame campaignFade;
	private GlueTextButtonFrame campaignBackButton;
	private UIFrame missionSelectFrame;
	private UIFrame campaignSelectFrame;
	private DataTable campaignStrings;
	private CampaignCatalog campaignCatalog;
	private SpriteFrame campaignWarcraftIIILogo;
	private final SingleModelScreen menuScreen;

	private CampaignMenuData currentCampaign;
	private String[] campaignList;
	private CampaignMenuData[] campaignDatas;
	private final List<CampaignEntry> campaignEntries = new ArrayList<>();
	// Measured: the 1.29 campaign screen offers Normal and Hard; Easy is reached only by reducing the difficulty after
	// a defeat.
	private static final CMapDifficulty[] CAMPAIGN_DIFFICULTIES = { CMapDifficulty.NORMAL, CMapDifficulty.HARD };
	private static final String[] CAMPAIGN_DIFFICULTY_KEYS = { "NORMAL", "HARD" };
	private PopupMenuFrame difficultySelect;
	private CMapDifficulty campaignDifficulty = CMapDifficulty.NORMAL;

	// BattleNet
	private BattleNetUI battleNetUI;

	private UnitSound mainMenuGlueScreenLoop;
	private GlueTextButtonFrame addProfileButton;
	private GlueTextButtonFrame deleteProfileButton;
	private GlueTextButtonFrame selectProfileButton;
	private final PlayerProfileManager profileManager;
	private StringFrame profileNameText;
	private UIFrame confirmDialog;
	private CampaignMenuUI campaignRootMenuUI;
	private CampaignMenuUI currentMissionSelectMenuUI;
	private UIFrame loadingFrame;
	private UIFrame loadingCustomPanel;
	private UIFrame loadingMeleePanel;
	private TextureFrame loadingMinimapTexture;
	private AbstractRenderableFrame loadingMinimapImage;
	private Texture loadingMinimapUnmanagedTexture;
	private final List<UIFrame> loadingPlayerSlots = new ArrayList<>();
	private StringFrame loadingTitleText;
	private StringFrame loadingSubtitleText;
	private StringFrame loadingText;
	private StringFrame loadingBarText;
	private Color loadingBarTextColor;
	private static final String TAP_TO_CONTINUE = "TAP TO CONTINUE";
	private static final float TAP_PULSE_SECONDS = 1.2f;
	private boolean waitForTap;
	/** A launcher shows its own menu: the glue menu is never shown, only the loading screens of the games. */
	private boolean launcherMode;
	/** The loading screen of the map a launcher starts, shown while the menu is still being built. */
	private boolean previewingLoading;
	/** The melee backdrop model of the previewed screen, null for a campaign one. */
	private String previewedBackground;
	/** The part of the loading bar the menu build filled before the map itself loads. */
	private float loadingBarStart;
	private boolean loadingMapDone;
	private boolean continueTapped;
	private float tapPulse;
	private SpriteFrame loadingBar;
	public static final int MAIN_PHASES = 4;
	private static final float PREVIEW_BAR_SHARE = 0.25f;
	private BeginGameInformation beginGameInformation;
	private LoadingMap loadingMap;
	private SpriteFrame loadingBackground;
	private boolean unifiedCampaignInfo = false;
	private boolean classicCampaignList = false;
	private MapInfoPane skirmishMapInfoPane;
	private MapListContainer skirmishMapList;
	private static final String SKIRMISH_MAP_PREFERENCE = "SkirmishMap";
	private War3MapConfig currentMapConfig;
	private final DataTable musicSLK;
	private Music[] currentMusics;
	private int currentMusicIndex;
	private boolean currentMusicRandomizeIndex;
	private final GamingNetworkConnection gamingNetworkConnection;
	private UIFrame battleNetConnectDialog;
	private StringFrame battleNetConnectInfoText;
	private GlueTextButtonFrame battleNetConnectCancelButton;
	private DialogWar3 dialog;
	private Task gameListQueryTask;
	private boolean hideUI;
	private final DataTable miscData;
	private FogSettings menuFogSettings;

	public MenuUI(final DataSource dataSource, final Viewport uiViewport, final Scene uiScene, final MdxViewer viewer,
			final WarsmashGdxMultiScreenGame screenManager, final SingleModelScreen menuScreen,
			final DataTable warsmashIni, final RootFrameListener rootFrameListener,
			final GamingNetworkConnection gamingNetworkConnection, final String mapDownloadDir) {
		this.dataSource = dataSource;
		this.uiViewport = uiViewport;
		this.uiScene = uiScene;
		this.viewer = viewer;
		this.screenManager = screenManager;
		this.menuScreen = menuScreen;
		this.warsmashIni = warsmashIni;
		this.rootFrameListener = rootFrameListener;
		this.gamingNetworkConnection = gamingNetworkConnection;

		this.widthRatioCorrection = getMinWorldWidth() / 1600f;
		this.heightRatioCorrection = getMinWorldHeight() / 1200f;

		this.profileManager = PlayerProfileManager.loadFromGdx();

		this.musicSLK = new DataTable(StringBundle.EMPTY);
		final String musicSLKPath = "UI\\SoundInfo\\Music.SLK";
		if (viewer.dataSource.has(musicSLKPath)) {
			try (InputStream miscDataTxtStream = viewer.dataSource.getResourceAsStream(musicSLKPath)) {
				this.musicSLK.readSLK(miscDataTxtStream);
			}
			catch (final IOException e) {
				e.printStackTrace();
			}
		}
		this.miscData = new DataTable(this.worldEditStrings);
		try (InputStream miscDataTxtStream = this.dataSource.getResourceAsStream("UI\\MiscData.txt")) {
			this.miscData.readTXT(miscDataTxtStream, true);
		}
		catch (final IOException e) {
			e.printStackTrace();
		}
		final Element zFogElement = this.miscData.get("MenuZFog");
		if (zFogElement != null) {
			this.menuFogSettings = FogSettings.parse(zFogElement, WarsmashConstants.GAME_VERSION);
		}

		gamingNetworkConnection.addListener(new GamingNetworkServerToClientListener() {
			private boolean creatingGame = false;
			private CurrentNetGameMapLookup currentNetGameMapLookup;
			private long lastJoinGameChecksum;

			@Override
			public void disconnected() {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						System.err.println("Disconnected from server...");
						MenuUI.this.battleNetConnectDialog.setVisible(false);
						setMainMenuButtonsEnabled(true);
						MenuUI.this.dialog.showError("ERROR_ID_DISCONNECT", null);

						MenuUI.this.battleNetUI.hide();
						playCurrentBattleNetGlueSpriteDeath();
						MenuUI.this.glueSpriteLayerCenter.setSequence("Death");
						MenuUI.this.menuState = MenuState.LEAVING_BATTLE_NET_FROM_LOGGED_IN;
					}
				});
			}

			@Override
			public void loginOk(final long sessionToken, final String welcomeMessage) {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						MenuUI.this.battleNetUI.loginAccepted(sessionToken, welcomeMessage);
						MenuUI.this.battleNetUI.getDoors().setSequence(PrimaryTag.DEATH, SequenceUtils.ALTERNATE);
						MenuUI.this.menuState = MenuState.GOING_TO_BATTLE_NET_WELCOME;
					}
				});
			}

			@Override
			public void loginFailed(final LoginFailureReason loginFailureReason) {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						String msg;
						switch (loginFailureReason) {
						case INVALID_CREDENTIALS:
							msg = "ERROR_ID_BADPASSWORD";
							break;
						case UNKNOWN_USER:
							msg = "ERROR_ID_UNKNOWNACCOUNT";
							break;
						default:
							msg = "ERROR_ID_INVALIDPARAMS";
							break;
						}
						MenuUI.this.dialog.showError(msg, null);
					}
				});
			}

			@Override
			public void joinedChannel(final String channelName) {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						MenuUI.this.battleNetUI.joinedChannel(channelName);
						MenuUI.this.battleNetUI.hideCurrentScreen();
						playCurrentBattleNetGlueSpriteDeath();
						final boolean insideTopBarMode = isInsideTopBarMode();
						if (!insideTopBarMode) {
							MenuUI.this.menuState = MenuState.GOING_TO_BATTLE_NET_CHAT_CHANNEL_FROM_OUTSIDE;
						}
						else {
							MenuUI.this.menuState = MenuState.GOING_TO_BATTLE_NET_CHAT_CHANNEL;
						}
					}
				});
			}

			@Override
			public void handshakeDenied(final HandshakeDeniedReason reason) {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						MenuUI.this.battleNetConnectDialog.setVisible(true);
						String msg;
						switch (reason) {
						case BAD_GAME_VERSION: {
							msg = "ERROR_ID_VERSION_BAD";
							break;
						}
						default: {
							msg = "NETERROR_DEFAULTERROR";
							break;
						}
						}
						MenuUI.this.rootFrame.setDecoratedText(MenuUI.this.battleNetConnectInfoText, msg);
					}
				});
			}

			@Override
			public void handshakeAccepted() {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						MenuUI.this.battleNetConnectDialog.setVisible(false);
						MenuUI.this.glueSpriteLayerTopLeft.setSequence("MainMenu Death");
						MenuUI.this.glueSpriteLayerTopRight.setSequence("MainMenu Death");
						setMainMenuButtonsEnabled(true);
						setMainMenuVisible(false);
						MenuUI.this.menuState = MenuState.GOING_TO_BATTLE_NET_LOGIN;
					}
				});
			}

			@Override
			public void channelMessage(final String userName, final String message) {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						MenuUI.this.battleNetUI.channelMessage(userName, message);
					}
				});
			}

			@Override
			public void channelEmote(final String userName, final String message) {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						MenuUI.this.battleNetUI.channelEmote(userName, message);
					}
				});
			}

			@Override
			public void channelServerMessage(final String userName, final ChannelServerMessageType messageType) {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						MenuUI.this.battleNetUI.channelServerMessage(userName, messageType);
					}
				});
			}

			@Override
			public void badSession() {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						MenuUI.this.dialog.showError("ERROR_ID_NOTLOGGEDON", null);
					}
				});
			}

			@Override
			public void accountCreationOk() {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						MenuUI.this.battleNetUI.accountCreatedOk();
					}
				});
			}

			@Override
			public void accountCreationFailed(final AccountCreationFailureReason reason) {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						switch (reason) {
						default:
						case USERNAME_ALREADY_EXISTS:
							MenuUI.this.dialog.showError("ERROR_ID_NAMEUSED", null);
							break;
						}
					}
				});
			}

			@Override
			public void joinedGame(final String gameName, final String mapName, final long mapChecksum) {
				this.lastJoinGameChecksum = mapChecksum;
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						boolean host = false;
						if (creatingGame) {
							host = true;
							creatingGame = false;
							MenuUI.this.battleNetUI.setJoinGamePreviewMapToHostedMap();
							currentNetGameMapLookup = new CurrentNetGameMapLookupPath(
									MenuUI.this.battleNetUI.getLastHostedGamePath());
						}
						else {
							// joining game as non-host
							final String mapNameLower = mapName.toLowerCase(Locale.US);
							if (!(mapNameLower.endsWith(".w3m") || mapNameLower.endsWith(".w3x")
									|| mapNameLower.endsWith(".wsm"))) {
								// error, not a map file
								System.err.println("ERROR!!! NOT A MAP FILE: " + mapName);
								gamingNetworkConnection
										.leaveGame(MenuUI.this.battleNetUI.getGamingNetworkSessionToken());
								currentNetGameMapLookup = null;
							}
							else {
								final String mapNameOnly = mapName
										.substring(Math.max(mapName.lastIndexOf('/'), mapName.lastIndexOf('\\')) + 1);
								final String mapFileLookupName = mapDownloadDir + File.separator + mapNameOnly;
								File mapLookupFile = new File(mapFileLookupName);
								boolean hasMap = false;
								final Crc32c checksumCalculator = new Crc32c();
								War3Map map = null;
								int tildeIndex = 1;
								while (mapLookupFile.exists()) {
									try {
										map = War3MapViewer.beginLoadingMap(dataSource, mapLookupFile.getPath());
										final long localHDDMapChecksum = map.computeChecksum(checksumCalculator);
										if (localHDDMapChecksum == mapChecksum) {
											hasMap = true;
											break;
										}
									}
									catch (final Exception e) {
										e.printStackTrace();
									}
									final int dotIndex = mapFileLookupName.lastIndexOf('.');
									mapLookupFile = new File(mapFileLookupName.substring(0, dotIndex) + "~"
											+ tildeIndex++ + mapFileLookupName.substring(dotIndex));
								}
								currentNetGameMapLookup = new CurrentNetGameMapLookupFile(mapLookupFile);
								if (hasMap) {
									MenuUI.this.battleNetUI.setJoinGamePreviewMap(mapLookupFile);
								}
								else {
									MenuUI.this.battleNetUI.clearJoinGamePreviewMap(mapNameOnly);
									gamingNetworkConnection
											.requestMap(MenuUI.this.battleNetUI.getGamingNetworkSessionToken());
								}

							}
						}

						MenuUI.this.battleNetUI.joinedGame(gameName, host);
						MenuUI.this.battleNetUI.hideCurrentScreen();
						playCurrentBattleNetGlueSpriteDeath();
						MenuUI.this.menuState = MenuState.GOING_TO_BATTLE_NET_CUSTOM_GAME_LOBBY;
					}
				});
			}

			@Override
			public void joinGameFailed(final JoinGameFailureReason reason) {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						switch (reason) {
						case GAME_ALREADY_STARTED:
							MenuUI.this.dialog.showError("ERROR_ID_GAMECLOSED", null);
							break;
						case GAME_FULL:
							MenuUI.this.dialog.showError("ERROR_ID_GAMEFULL", null);
							break;
						case SESSION_ERROR:
							MenuUI.this.dialog.showError("NETERROR_JOINGAMEFAILED", null);
							break;
						default:
						case NO_SUCH_GAME:
							MenuUI.this.dialog.showError("NETERROR_JOINGAMENOTFOUND", null);
							break;
						}
					}
				});
			}

			@Override
			public void serverErrorMessage(final ServerErrorMessageType messageType) {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						switch (messageType) {
						case UPLOAD_MAP_FAILED:
							MenuUI.this.dialog.showError("NETERROR_MAPFILEWRITE", null);
							break;
						default:
						case ERROR_HANDLING_REQUEST:
							MenuUI.this.dialog.showError("NETERROR_DEFAULTERROR", null);
							break;
						}
					}
				});
			}

			@Override
			public void gameCreationFailed(final GameCreationFailureReason reason) {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						switch (reason) {
						default:
						case GAME_NAME_ALREADY_USED:
							MenuUI.this.dialog.showError("NETERROR_CREATEGAMEFAILED", null);
							break;
						}
					}
				});
			}

			@Override
			public void gameCreationOk() {
				System.out.println("NOTE: Server says we created a game OK");
				// NOTE: immediately following this the server should tell us we joined a game
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						creatingGame = true;
					}
				});
			}

			@Override
			public void beginGamesList() {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						MenuUI.this.battleNetUI.beginGamesList();
					}
				});
			}

			@Override
			public void gamesListItem(final String gameName, final int openSlots, final int totalSlots) {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						MenuUI.this.battleNetUI.gamesListItem(gameName, openSlots, totalSlots);
					}
				});
			}

			@Override
			public void endGamesList() {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						MenuUI.this.battleNetUI.endGamesList();
					}
				});
			}

			private NetMapDownloader netMapDownloader;

			@Override
			public void beginSendMap() {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						if (currentNetGameMapLookup instanceof CurrentNetGameMapLookupFile) {
							final File mapFile = ((CurrentNetGameMapLookupFile) currentNetGameMapLookup).getFile();
							netMapDownloader = new NetMapDownloader(mapFile);
						}
						else {
							throw new IllegalStateException();
						}
					}
				});
			}

			@Override
			public void sendMapData(final int sequenceNumber, final ByteBuffer data) {
				final ByteBuffer gdxThreadBuffer = ByteBuffer.allocate(data.remaining());
				gdxThreadBuffer.clear();
				gdxThreadBuffer.put(data);
				gdxThreadBuffer.flip();
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						System.out.println("receiving map: " + sequenceNumber + ", " + gdxThreadBuffer.remaining());
						netMapDownloader.receive(sequenceNumber, gdxThreadBuffer);
					}
				});
			}

			@Override
			public void endSendMap(final int sequenceNumber) {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						System.out.println("receiving map finishing: " + sequenceNumber);
						final long checksum = netMapDownloader.finish(sequenceNumber);
						if (currentNetGameMapLookup instanceof CurrentNetGameMapLookupFile) {
							if ((checksum == lastJoinGameChecksum) && netMapDownloader.isSequenceNumberingOK()) {
								final File mapFile = ((CurrentNetGameMapLookupFile) currentNetGameMapLookup).getFile();
								MenuUI.this.battleNetUI.setJoinGamePreviewMap(mapFile);
							}
							else {
								MenuUI.this.dialog.showError("NETERROR_MAPFILEINCOMPLETE", null);
								gamingNetworkConnection
										.leaveGame(MenuUI.this.battleNetUI.getGamingNetworkSessionToken());
							}
						}
						else {
							throw new IllegalStateException();
						}
					}
				});
			}

			@Override
			public void gameLobbySlotSetPlayerType(final int slot, final LobbyPlayerType playerType) {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						System.err.println("gameLobbySlotSetPlayerType " + slot + " " + playerType);
						MenuUI.this.battleNetUI.gameLobbySlotSetPlayerType(slot, playerType);
					}
				});
			}

			@Override
			public void gameLobbySlotSetPlayer(final int slot, final String userName) {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						System.err.println("gameLobbySlotSetPlayer " + slot + " " + userName);
						MenuUI.this.battleNetUI.gameLobbySlotSetPlayer(slot, userName);
					}
				});
			}

			@Override
			public void gameLobbySlotSetPlayerRace(final int slot, final int raceItemIndex) {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						System.err.println("gameLobbySlotSetPlayerRace " + slot + " " + raceItemIndex);
						MenuUI.this.battleNetUI.gameLobbySlotSetPlayerRace(slot, raceItemIndex);
					}
				});
			}

			@Override
			public void gameLobbyStartGame(final byte[] hostIpAddressBytes, final short hostUdpPort,
					final int yourServerPlayerSlot) {
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						MenuUI.this.campaignMenu.setVisible(false);
						MenuUI.this.campaignBackButton.setVisible(false);
						MenuUI.this.missionSelectFrame.setVisible(false);
						MenuUI.this.campaignSelectFrame.setVisible(false);
						MenuUI.this.campaignWarcraftIIILogo.setVisible(false);
						MenuUI.this.campaignRootMenuUI.setVisible(false);
						MenuUI.this.currentMissionSelectMenuUI.setVisible(false);
						MenuUI.this.skirmish.setVisible(false);
						MenuUI.this.battleNetUI.hideCurrentScreen();
						playCurrentBattleNetGlueSpriteDeath();
						MenuUI.this.beginGameInformation = new BeginGameInformation();
						MenuUI.this.beginGameInformation.gameMapLookup = currentNetGameMapLookup;
						MenuUI.this.beginGameInformation.sessionToken = MenuUI.this.battleNetUI
								.getGamingNetworkSessionToken();
						MenuUI.this.beginGameInformation.hostInetAddress = hostIpAddressBytes;
						MenuUI.this.beginGameInformation.hostUdpPort = hostUdpPort & 0xFFFF;
						final IntIntMap gameChatroomServerSlotToMapSlot = MenuUI.this.battleNetUI
								.getGameChatroomServerSlotToMapSlot();
						MenuUI.this.beginGameInformation.serverSlotToMapSlot = gameChatroomServerSlotToMapSlot;
						MenuUI.this.beginGameInformation.mapSlotToServerSlot = MenuUI.this.battleNetUI
								.getGameChatroomMapSlotToServerSlot();
						MenuUI.this.beginGameInformation.localPlayerIndex = gameChatroomServerSlotToMapSlot
								.get(yourServerPlayerSlot, -1);
						MenuUI.this.currentMapConfig = MenuUI.this.battleNetUI.getGameChatroomMapConfig();
						MenuUI.this.menuState = MenuState.GOING_TO_MAP;
					}
				});
			}
		});
	}

	public float getHeightRatioCorrection() {
		return this.heightRatioCorrection;
	}

	/**
	 * Called "main" because this was originally written in JASS so that maps could
	 * override it, and I may convert it back to the JASS at some point.
	 */
	public void main() {
		for (int phase = 0; phase < MAIN_PHASES; phase++) {
			mainPhase(phase);
		}
	}

	/**
	 * One part of {@link #main()}, so a caller can draw progress between the parts.
	 */
	public void mainPhase(final int phase) {
		switch (phase) {
		case 0:
			mainSkins();
			break;
		case 1:
			mainMenus();
			break;
		case 2:
			mainSkirmish();
			break;
		default:
			mainDialogs();
			break;
		}
	}

	private void mainSkins() {
		// =================================
		// Load skins and templates
		// =================================
		this.rootFrame = new GameUI(this.dataSource, GameUI.loadSkin(this.dataSource, WarsmashConstants.GAME_VERSION),
				this.uiViewport, this.uiScene, this.viewer, 0, WTS.DO_NOTHING);
		this.rootFrame.setWideRoot(true);

		if (!this.launcherMode) {
			this.menuScreen.setModel(this.rootFrame.getSkinField("GlueSpriteLayerBackground"), this.menuFogSettings);
		}
		this.rootFrameListener.onCreate(this.rootFrame);
		try {
			this.rootFrame.loadTOCFile("UI\\FrameDef\\FrameDef.toc");
		}
		catch (final IOException exc) {
			throw new IllegalStateException("Unable to load FrameDef.toc", exc);
		}
		try {
			this.rootFrame.loadTOCFile("UI\\FrameDef\\SmashFrameDef.toc");
		}
		catch (final IOException exc) {
			throw new IllegalStateException("Unable to load SmashFrameDef.toc", exc);
		}

		final CampaignCatalog campaignCatalog;
		try {
			campaignCatalog = CampaignCatalog.load(this.dataSource, this.rootFrame.trySkinField("CampaignFile"));
		}
		catch (final IOException e) {
			throw new RuntimeException(e);
		}
		this.campaignCatalog = campaignCatalog;
		this.campaignStrings = campaignCatalog.getStrings();
		this.classicCampaignList = campaignCatalog.isClassicList();
		this.unifiedCampaignInfo = campaignCatalog.isUnified();
		CCampaignProgress.setCampaignList(campaignCatalog.getList());

	}

	private void mainMenus() {
		// Create main menu
		this.mainMenuFrame = this.rootFrame.createFrame("MainMenuFrame", this.rootFrame, 0, 0);

		this.warcraftIIILogo = (SpriteFrame) this.rootFrame.getFrameByName("WarCraftIIILogo", 0);
		this.rootFrame.setSpriteFrameModel(this.warcraftIIILogo, this.rootFrame.getSkinField("MainMenuLogo"));
		this.warcraftIIILogo.addSetPoint(new SetPoint(FramePoint.TOPLEFT, this.mainMenuFrame, FramePoint.TOPLEFT,
				GameUI.convertX(this.uiViewport, 0.13f), GameUI.convertY(this.uiViewport, -0.08f)));
		setMainMenuVisible(false);
		this.rootFrame.getFrameByName("RealmSelect", 0).setVisible(false);
		// 2.0 and later menus carry an age rating badge that the game shows only in the Korean region.
		final UIFrame ratingsLogo = this.rootFrame.getFrameByName("RatingsLogo", 0);
		if (ratingsLogo != null) {
			ratingsLogo.setVisible(false);
		}

		this.glueSpriteLayerTopRight = (SpriteFrame) this.rootFrame.createFrameByType("SPRITE",
				"SmashGlueSpriteLayerTopRight", this.rootFrame, "", 0);
		anchorGlueLayer(this.glueSpriteLayerTopRight, FramePoint.TOPRIGHT);
		final String topRightModel = this.rootFrame.getSkinField("GlueSpriteLayerTopRight");
		this.rootFrame.setSpriteFrameModel(this.glueSpriteLayerTopRight, topRightModel);
		this.glueSpriteLayerTopRight.setSequence("MainMenu Birth");

		this.glueSpriteLayerTopLeft = (SpriteFrame) this.rootFrame.createFrameByType("SPRITE",
				"SmashGlueSpriteLayerTopLeft", this.rootFrame, "", 0);
		anchorGlueLayer(this.glueSpriteLayerTopLeft, FramePoint.TOPLEFT);
		final String topLeftModel = this.rootFrame.getSkinField("GlueSpriteLayerTopLeft");
		this.rootFrame.setSpriteFrameModel(this.glueSpriteLayerTopLeft, topLeftModel);
		this.glueSpriteLayerTopLeft.setSequence("MainMenu Birth");

		this.glueSpriteLayerCenter = (SpriteFrame) this.rootFrame.createFrameByType("SPRITE",
				"SmashGlueSpriteLayerCenter", this.rootFrame, "", 0);
		anchorGlueLayer(this.glueSpriteLayerCenter, FramePoint.CENTER);
		final String centerModel = this.rootFrame.getSkinField("GlueSpriteLayerCenter");
		this.rootFrame.setSpriteFrameModel(this.glueSpriteLayerCenter, centerModel);
		this.glueSpriteLayerCenter.setVisible(false);
		// A previewed loading screen goes straight on to its map; the menu behind it is never shown.
		if (this.launcherMode || this.previewingLoading) {
			this.glueSpriteLayerTopRight.setVisible(false);
			this.glueSpriteLayerTopLeft.setVisible(false);
		}

		this.cursorFrame = (SpriteFrame) this.rootFrame.createFrameByType("SPRITE", "SmashCursorFrame", this.rootFrame,
				"", 0);
		this.rootFrame.setSpriteFrameModel(this.cursorFrame, this.rootFrame.getSkinField("Cursor"));
		this.cursorFrame.setSequence("Normal");
		this.cursorFrame.setZDepth(1024.0f);
		if (WarsmashConstants.CATCH_CURSOR) {
			Gdx.input.setCursorCatched(true);
		}

		this.battleNetConnectDialog = this.rootFrame.createFrame("BattleNetConnectDialog", this.rootFrame, 0, 0);
		this.battleNetConnectDialog.setVisible(false);
		this.battleNetConnectDialog.addAnchor(new AnchorDefinition(FramePoint.CENTER, 0, 0));
		this.battleNetConnectInfoText = (StringFrame) this.rootFrame.getFrameByName("ConnectInfoText", 0);
		this.battleNetConnectCancelButton = (GlueTextButtonFrame) this.rootFrame.getFrameByName("ConnectButton", 0);
		this.battleNetConnectCancelButton.setOnClick(new Runnable() {
			@Override
			public void run() {
				MenuUI.this.gamingNetworkConnection.userRequestDisconnect();
				MenuUI.this.battleNetConnectDialog.setVisible(false);
				setMainMenuButtonsEnabled(true);
			}
		});

		// Main Menu interactivity
		this.singlePlayerButton = (GlueTextButtonFrame) this.rootFrame.getFrameByName("SinglePlayerButton", 0);
		this.battleNetButton = (GlueTextButtonFrame) this.rootFrame.getFrameByName("BattleNetButton", 0);
		this.realmButton = (GlueButtonFrame) this.rootFrame.getFrameByName("RealmButton", 0);
		this.localAreaNetworkButton = (GlueTextButtonFrame) this.rootFrame.getFrameByName("LocalAreaNetworkButton", 0);
		this.optionsButton = (GlueTextButtonFrame) this.rootFrame.getFrameByName("OptionsButton", 0);
		this.creditsButton = (GlueTextButtonFrame) this.rootFrame.getFrameByName("CreditsButton", 0);
		this.exitButton = (GlueTextButtonFrame) this.rootFrame.getFrameByName("ExitButton", 0);
		this.editionButton = (GlueButtonFrame) this.rootFrame.getFrameByName("EditionButton", 0);

		if (this.editionButton != null) {
			if (WarsmashConstants.GAME_VERSION == 0) {
				// The shipped FDF only describes the expansion menu, whose button offers the switch back to RoC.
				for (final String[] state : new String[][] { { "", "up" }, { "Pushed", "down" },
						{ "Disabled", "disabled" } }) {
					final UIFrame backdrop = this.rootFrame.getFrameByName("EditionButton" + state[0] + "BackdropTemplate",
							0);
					if (backdrop instanceof BackdropFrame) {
						((BackdropFrame) backdrop).setBackground(this.rootFrame
								.loadTexture("UI\\Widgets\\Glues\\GlueScreen-TFT-EditionButton-" + state[1] + ".blp"));
					}
				}
			}
			this.editionButton.setOnClick(new Runnable() {
				@Override
				public void run() {
					WarsmashConstants.GAME_VERSION = WarsmashConstants.GAME_VERSION == 1 ? 0 : 1;
					MenuUI.this.glueSpriteLayerTopLeft.setSequence("MainMenu Death");
					MenuUI.this.glueSpriteLayerTopRight.setSequence("MainMenu Death");
					setMainMenuVisible(false);
					MenuUI.this.menuState = MenuState.RESTARTING;
				}
			});
		}

		this.localAreaNetworkButton.setEnabled(true);
		GameOptions.get().apply();
		this.optionsButton.setEnabled(true);
		this.optionsButton.setOnClick(this::showOptions);
		this.creditsButton.setEnabled(false);

		this.exitButton.setOnClick(new Runnable() {
			@Override
			public void run() {
				MenuUI.this.glueSpriteLayerTopLeft.setSequence("MainMenu Death");
				MenuUI.this.glueSpriteLayerTopRight.setSequence("MainMenu Death");
				setMainMenuVisible(false);
				MenuUI.this.menuState = MenuState.QUITTING;
			}
		});

		this.singlePlayerButton.setOnClick(new Runnable() {
			@Override
			public void run() {
				MenuUI.this.glueSpriteLayerTopLeft.setSequence("MainMenu Death");
				MenuUI.this.glueSpriteLayerTopRight.setSequence("MainMenu Death");
				setMainMenuVisible(false);
				MenuUI.this.menuState = MenuState.GOING_TO_SINGLE_PLAYER;
			}
		});

		this.battleNetButton.setOnClick(new Runnable() {
			@Override
			public void run() {
				setMainMenuButtonsEnabled(false);
				MenuUI.this.rootFrame.setDecoratedText(MenuUI.this.battleNetConnectInfoText, "BNET_CONNECT_INIT");
				MenuUI.this.battleNetConnectDialog.positionBounds(MenuUI.this.rootFrame, MenuUI.this.uiViewport);
				MenuUI.this.battleNetConnectDialog.setVisible(true);
				if (MenuUI.this.gamingNetworkConnection.userRequestConnect()) {
					MenuUI.this.gamingNetworkConnection.handshake(WarsmashConstants.getGameId(),
							GamingNetwork.GAME_VERSION_DATA);
				}
				else {
					MenuUI.this.battleNetConnectDialog.setVisible(false);
					setMainMenuButtonsEnabled(true);
					MenuUI.this.dialog.showError("ERROR_ID_CANTCONNECT", new Runnable() {
						@Override
						public void run() {

						}
					});
				}
			}
		});
		this.realmButton.setOnClick(new Runnable() {
			@Override
			public void run() {
				MenuUI.this.battleNetConnectDialog.setVisible(true);
			}
		});

		// Create single player
		this.singlePlayerMenu = this.rootFrame.createFrame("SinglePlayerMenu", this.rootFrame, 0, 0);
		this.singlePlayerMenu.setVisible(false);

		this.profilePanel = this.rootFrame.getFrameByName("ProfilePanel", 0);
		this.profilePanel.setVisible(false);

		this.newProfileEditBox = (EditBoxFrame) this.rootFrame.getFrameByName("NewProfileEditBox", 0);
		this.newProfileEditBox.setOnChange(new Runnable() {
			@Override
			public void run() {
				MenuUI.this.addProfileButton
						.setEnabled(!MenuUI.this.profileManager.hasProfile(MenuUI.this.newProfileEditBox.getText()));
			}
		});
		final StringFrame profileListText = (StringFrame) this.rootFrame.getFrameByName("ProfileListText", 0);
		final SimpleFrame profileListContainer = (SimpleFrame) this.rootFrame.getFrameByName("ProfileListContainer", 0);
		final ListBoxFrame profileListBox = (ListBoxFrame) this.rootFrame.createFrameByType("LISTBOX", "ListBoxWar3",
				profileListContainer, "WITHCHILDREN", 0);
		profileListBox.setSetAllPoints(true);
		profileListBox.setFrameFont(profileListText.getFrameFont());
		for (final PlayerProfile profile : this.profileManager.getProfiles()) {
			profileListBox.addItem(profile.getName(), this.rootFrame, this.uiViewport);
		}
		profileListContainer.add(profileListBox);

		this.addProfileButton = (GlueTextButtonFrame) this.rootFrame.getFrameByName("AddProfileButton", 0);
		this.deleteProfileButton = (GlueTextButtonFrame) this.rootFrame.getFrameByName("DeleteProfileButton", 0);
		this.selectProfileButton = (GlueTextButtonFrame) this.rootFrame.getFrameByName("SelectProfileButton", 0);
		this.selectProfileButton.setEnabled(false);
		this.deleteProfileButton.setEnabled(false);
		this.addProfileButton.setOnClick(new Runnable() {
			@Override
			public void run() {
				final String newProfileName = MenuUI.this.newProfileEditBox.getText();
				if (!newProfileName.isEmpty() && !MenuUI.this.profileManager.hasProfile(newProfileName)) {
					MenuUI.this.profileManager.addProfile(newProfileName);
					profileListBox.addItem(newProfileName, MenuUI.this.rootFrame, MenuUI.this.uiViewport);
					MenuUI.this.addProfileButton.setEnabled(false);
				}
			}
		});
		this.deleteProfileButton.setOnClick(new Runnable() {
			@Override
			public void run() {
				final int selectedIndex = profileListBox.getSelectedIndex();
				final boolean validSelect = (selectedIndex >= 0)
						&& (selectedIndex < MenuUI.this.profileManager.getProfiles().size());
				if (validSelect) {
					if (MenuUI.this.profileManager.getProfiles().size() > 1) {
						final PlayerProfile profileToRemove = MenuUI.this.profileManager.getProfiles()
								.get(selectedIndex);
						final String removeProfileName = profileToRemove.getName();
						final boolean deletingCurrentProfile = removeProfileName
								.equals(MenuUI.this.profileManager.getCurrentProfile());
						MenuUI.this.profileManager.removeProfile(profileToRemove);
						profileListBox.removeItem(selectedIndex, MenuUI.this.rootFrame, MenuUI.this.uiViewport);
						if (deletingCurrentProfile) {
							setCurrentProfile(MenuUI.this.profileManager.getProfiles().get(0).getName());
						}
					}
				}
			}
		});
		this.selectProfileButton.setOnClick(new Runnable() {
			@Override
			public void run() {
				final int selectedIndex = profileListBox.getSelectedIndex();
				final boolean validSelect = (selectedIndex >= 0)
						&& (selectedIndex < MenuUI.this.profileManager.getProfiles().size());
				if (validSelect) {
					final PlayerProfile profileToSelect = MenuUI.this.profileManager.getProfiles().get(selectedIndex);
					final String selectedProfileName = profileToSelect.getName();
					setCurrentProfile(selectedProfileName);

					MenuUI.this.glueSpriteLayerTopLeft.setSequence("RealmSelection Death");
					MenuUI.this.profilePanel.setVisible(false);
					MenuUI.this.menuState = MenuState.SINGLE_PLAYER;
					setSinglePlayerButtonsEnabled(false);
				}

			}

		});
		profileListBox.setSelectionListener(new ListBoxSelelectionListener() {
			@Override
			public void onSelectionChanged(final int newSelectedIndex, final String newSelectedItem) {
				final boolean validSelect = newSelectedItem != null;
				MenuUI.this.selectProfileButton.setEnabled(validSelect);
				MenuUI.this.deleteProfileButton.setEnabled(validSelect);
			}
		});

		this.singlePlayerMainPanel = this.rootFrame.getFrameByName("MainPanel", 0);

		// Single Player Interactivity
		this.profileButton = (GlueButtonFrame) this.rootFrame.getFrameByName("ProfileButton", 0);
		this.campaignButton = (GlueTextButtonFrame) this.rootFrame.getFrameByName("CampaignButton", 0);
		this.loadSavedButton = (GlueTextButtonFrame) this.rootFrame.getFrameByName("LoadSavedButton", 0);
		this.loadSavedButton.setOnClick(this::showSavedGames);
		this.viewReplayButton = (GlueTextButtonFrame) this.rootFrame.getFrameByName("ViewReplayButton", 0);
		this.customCampaignButton = (GlueTextButtonFrame) this.rootFrame.getFrameByName("CustomCampaignButton", 0);
		this.skirmishButton = (GlueTextButtonFrame) this.rootFrame.getFrameByName("SkirmishButton", 0);

		this.singlePlayerCancelButton = (GlueTextButtonFrame) this.rootFrame.getFrameByName("CancelButton", 0);

		this.profileNameText = (StringFrame) this.rootFrame.getFrameByName("ProfileNameText", 0);
		this.rootFrame.setText(this.profileNameText, this.profileManager.getCurrentProfile());

		setSinglePlayerButtonsEnabled(true);

		this.profileButton.setOnClick(new Runnable() {
			@Override
			public void run() {
				MenuUI.this.glueSpriteLayerTopLeft.setSequence("RealmSelection Birth");
				setSinglePlayerButtonsEnabled(false);
				MenuUI.this.menuState = MenuState.SINGLE_PLAYER_PROFILE;
			}
		});

		this.campaignButton.setOnClick(new Runnable() {
			@Override
			public void run() {
				MenuUI.this.glueSpriteLayerTopLeft.setSequence("SinglePlayer Death");
				MenuUI.this.glueSpriteLayerTopRight.setSequence("SinglePlayer Death");
				MenuUI.this.singlePlayerMenu.setVisible(false);
				MenuUI.this.profilePanel.setVisible(false);
				refreshCampaignAvailability();
				MenuUI.this.menuState = MenuState.GOING_TO_CAMPAIGN;
			}
		});

		this.skirmishButton.setOnClick(new Runnable() {
			@Override
			public void run() {
				MenuUI.this.glueSpriteLayerTopLeft.setSequence("SinglePlayer Death");
				MenuUI.this.glueSpriteLayerTopRight.setSequence("SinglePlayer Death");
				MenuUI.this.singlePlayerMenu.setVisible(false);
				MenuUI.this.profilePanel.setVisible(false);
				MenuUI.this.menuState = MenuState.GOING_TO_SINGLE_PLAYER_SKIRMISH;
			}
		});

		this.singlePlayerCancelButton.setOnClick(new Runnable() {
			@Override
			public void run() {
				if (MenuUI.this.menuState == MenuState.SINGLE_PLAYER_PROFILE) {
					MenuUI.this.glueSpriteLayerTopLeft.setSequence("RealmSelection Death");
					MenuUI.this.profilePanel.setVisible(false);
				}
				else {
					MenuUI.this.glueSpriteLayerTopLeft.setSequence("SinglePlayer Death");
				}
				MenuUI.this.glueSpriteLayerTopRight.setSequence("SinglePlayer Death");
				MenuUI.this.singlePlayerMenu.setVisible(false);
				MenuUI.this.menuState = MenuState.GOING_TO_MAIN_MENU;
			}
		});

	}

	private void showSkirmishAdvancedOptions(final boolean advanced) {
		this.mapInfoPanel.setVisible(!advanced);
		this.advancedOptionsPanel.setVisible(advanced);
	}

	private void mainSkirmish() {
		// Create skirmish UI
		this.skirmish = this.rootFrame.createFrame("Skirmish", this.rootFrame, 0, 0);
		this.skirmish.setVisible(false);

		this.mapInfoButton = (GlueTextButtonFrame) this.rootFrame.getFrameByName("MapInfoButton", 0);
		this.advancedOptionsButton = (GlueTextButtonFrame) this.rootFrame.getFrameByName("AdvancedOptionsButton", 0);
		this.mapInfoPanel = this.rootFrame.getFrameByName("MapInfoPanel", 0);
		this.advancedOptionsPanel = this.rootFrame.getFrameByName("AdvancedOptionsPanel", 0);
		final SimpleFrame mapInfoPaneContainer = (SimpleFrame) this.rootFrame.getFrameByName("MapInfoPaneContainer", 0);
		final SimpleFrame advancedOptionsPaneContainer = (SimpleFrame) this.rootFrame
				.getFrameByName("AdvancedOptionsPaneContainer", 0);
		this.skirmishAdvancedOptionsPane = new AdvancedOptionsPane(this.rootFrame, advancedOptionsPaneContainer);
		// Each panel's button opens the other panel: the map info one reads "Advanced Options" and the other "Map Info".
		this.mapInfoButton.setOnClick(() -> showSkirmishAdvancedOptions(true));
		this.advancedOptionsButton.setOnClick(() -> showSkirmishAdvancedOptions(false));
		this.skirmishMapInfoPane = new MapInfoPane(this.rootFrame, this.uiViewport, mapInfoPaneContainer);

		final SimpleFrame teamSetupContainer = (SimpleFrame) this.rootFrame.getFrameByName("TeamSetupContainer", 0);
		final TeamSetupPane teamSetupPane = new TeamSetupPane(this.rootFrame, this.uiViewport, teamSetupContainer);

		final GlueTextButtonFrame playGameButton = (GlueTextButtonFrame) this.rootFrame.getFrameByName("PlayGameButton",
				0);
		final MapListContainer mapListContainer = new MapListContainer(this.rootFrame, this.uiViewport,
				"MapListContainer", this.dataSource,
				((StringFrame) this.rootFrame.getFrameByName("ProfileListText", 0)).getFrameFont());
		this.skirmishMapList = mapListContainer;
		mapListContainer.addSelectionListener(new ListBoxSelelectionListener() {
			War3Map lastMapListMap;
			String prevSelectedItem = "";

			@Override
			public void onSelectionChanged(final int newSelectedIndex, final String newSelectedItem) {
				if (newSelectedItem != null) {
					if (newSelectedItem.compareTo(this.prevSelectedItem) == 0) {
						return;
					}
					this.prevSelectedItem = newSelectedItem;

					try {
						final War3Map map = War3MapViewer.beginLoadingMap(MenuUI.this.dataSource, newSelectedItem);
						if (this.lastMapListMap != null) {
							try {
								this.lastMapListMap.close();
							}
							catch (final IOException e) {
								e.printStackTrace();
							}
						}
						this.lastMapListMap = map;
						final War3MapW3i mapInfo = map.readMapInformation();
						final WTS wtsFile = Warcraft3MapObjectData.loadWTS(map);
						MenuUI.this.rootFrame.setMapStrings(wtsFile);
						final War3MapConfig war3MapConfig = new War3MapConfig(WarsmashConstants.MAX_PLAYERS);
						for (int i = 0; (i < WarsmashConstants.MAX_PLAYERS) && (i < mapInfo.getPlayers().size()); i++) {
							final CBasePlayer player = war3MapConfig.getPlayer(i);
							player.setName(MenuUI.this.rootFrame.getTrigStr(mapInfo.getPlayers().get(i).getName()));
						}
						war3MapConfig.setMapName("NOTEXTERN: default name string");
						war3MapConfig.setMapDescription("NOTEXTERN: default description string");
						war3MapConfig.applyMapInformation(mapInfo);
						MenuUI.this.skirmishAdvancedOptionsPane.setMapInformation(mapInfo);
						Jass2.loadConfig(map, MenuUI.this.uiViewport, MenuUI.this.uiScene, MenuUI.this.rootFrame,
								war3MapConfig, WarsmashConstants.JASS_FILE_LIST).config();
						boolean foundFirstHuman = false;
						boolean foundFirstComp = false;
						for (int i = 0; i < WarsmashConstants.MAX_PLAYERS; i++) {
							final CBasePlayer player = war3MapConfig.getPlayer(i);
							if (player.getController() == CMapControl.USER) {
								if (!foundFirstHuman) {
									player.setSlotState(CPlayerSlotState.PLAYING);
									player.setName(MenuUI.this.profileManager.getCurrentProfile());
									foundFirstHuman = true;
								}
							}
							else if (player.getController() == CMapControl.COMPUTER) {
								if (!foundFirstComp
										|| mapInfo.hasFlag(War3MapW3iFlags.FIXED_PLAYER_SETTINGS_FOR_CUSTOM_FORCES)) {
									player.setSlotState(CPlayerSlotState.PLAYING);
									foundFirstComp = true;
								}
							}
						}
						MenuUI.this.randomStartLocationCount = prepareMeleeLobby(war3MapConfig, mapInfo);
						MenuUI.this.skirmishMapInfoPane.setMap(MenuUI.this.rootFrame, MenuUI.this.uiViewport, map,
								mapInfo, war3MapConfig);
						MenuUI.this.skirmishSlotListener = new PlayerSlotPaneListenerImplementation(teamSetupPane,
								war3MapConfig, mapInfo, MenuUI.this.rootFrame, MenuUI.this.uiViewport);
						teamSetupPane.setMap(MenuUI.this.rootFrame, MenuUI.this.uiViewport, war3MapConfig,
								mapInfo.getPlayers().size(), mapInfo, MenuUI.this.skirmishSlotListener);
						MenuUI.this.currentMapConfig = war3MapConfig;
					}
					catch (final IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
		this.skirmishStart = new Runnable() {
			@Override
			public void run() {
				final String selectedItem = mapListContainer.getSelectedItem();
				if (selectedItem != null) {
					final Preferences preferences = Gdx.app.getPreferences("WarsmashWC3Engine");
					preferences.putString(SKIRMISH_MAP_PREFERENCE, selectedItem);
					preferences.flush();
					MenuUI.this.campaignMenu.setVisible(false);
					MenuUI.this.campaignBackButton.setVisible(false);
					MenuUI.this.missionSelectFrame.setVisible(false);
					MenuUI.this.campaignSelectFrame.setVisible(false);
					MenuUI.this.campaignWarcraftIIILogo.setVisible(false);
					MenuUI.this.campaignRootMenuUI.setVisible(false);
					MenuUI.this.currentMissionSelectMenuUI.setVisible(false);
					MenuUI.this.skirmish.setVisible(false);
					MenuUI.this.glueSpriteLayerTopLeft.setSequence("Death");
					MenuUI.this.glueSpriteLayerTopRight.setSequence("Death");
					MenuUI.this.skirmishAdvancedOptionsPane.applyTo(MenuUI.this.currentMapConfig);
					// The lobby marks a closed slot as playing without a controller; in the game it is empty, as in
					// the original, so the melee scripts give it no starting base.
					for (int i = 0; i < WarsmashConstants.MAX_PLAYERS; i++) {
						final CBasePlayer configPlayer = MenuUI.this.currentMapConfig.getPlayer(i);
						if ((configPlayer.getSlotState() == CPlayerSlotState.PLAYING)
								&& (configPlayer.getController() == CMapControl.NONE)) {
							configPlayer.setSlotState(CPlayerSlotState.EMPTY);
						}
						else if ((configPlayer.getSlotState() == CPlayerSlotState.PLAYING)
								&& (configPlayer.getController() == CMapControl.COMPUTER)) {
							// Measured: a lobby computer plays under its slot label, e.g. "Computer (Easy)", which
							// GetPlayerName, the score screen and the unit tooltips show.
							configPlayer.setName(MenuUI.this.rootFrame.getTemplates()
									.getDecoratedString(getComputerSlotKey(configPlayer.getAIDifficulty())));
						}
					}
					shuffleStartLocations(MenuUI.this.currentMapConfig, MenuUI.this.randomStartLocationCount);
					MenuUI.this.beginGameInformation = new BeginGameInformation();
					MenuUI.this.beginGameInformation.gameMapLookup = new CurrentNetGameMapLookupPath(selectedItem);
					int localPlayerIndex = -1;
					for (int i = 0; i < WarsmashConstants.MAX_PLAYERS; i++) {
						final CBasePlayer configPlayer = MenuUI.this.currentMapConfig.getPlayer(i);
						if ((configPlayer.getSlotState() == CPlayerSlotState.PLAYING)
								&& (configPlayer.getController() == CMapControl.USER)) {
							localPlayerIndex = i;
							break;
						}
					}
					MenuUI.this.beginGameInformation.localPlayerIndex = localPlayerIndex;
					MenuUI.this.menuState = MenuState.GOING_TO_MAP;
				}

			}
		};
		playGameButton.setOnClick(this.skirmishStart);

		this.skirmishCancelButton = (GlueTextButtonFrame) this.rootFrame.getFrameByName("CancelButton", 0);
		this.skirmishCancelButton.setOnClick(new Runnable() {
			@Override
			public void run() {
				MenuUI.this.glueSpriteLayerTopLeft.setSequence("SinglePlayerSkirmish Death");
				MenuUI.this.glueSpriteLayerTopRight.setSequence("SinglePlayerSkirmish Death");
				MenuUI.this.skirmish.setVisible(false);
				MenuUI.this.menuState = MenuState.GOING_TO_SINGLE_PLAYER;

			}
		});

		// Create Campaign UI

		this.campaignMenu = this.rootFrame.createFrame("CampaignMenu", this.rootFrame, 0, 0);
		this.campaignMenu.setVisible(false);
		this.campaignFade = (SpriteFrame) this.rootFrame.getFrameByName("SlidingDoors", 0);
		// The doors darken the screen behind the campaign menus; left at 4:3 they leave the sides of a wide screen bright.
		fillScreenWidth(this.campaignFade);
		this.campaignFade.setVisible(false);
		this.campaignBackButton = (GlueTextButtonFrame) this.rootFrame.getFrameByName("BackButton", 0);
		this.campaignBackButton.setVisible(false);
		this.missionSelectFrame = this.rootFrame.getFrameByName("MissionSelectFrame", 0);
		this.missionSelectFrame.setVisible(false);
		final StringFrame missionName = (StringFrame) this.rootFrame.getFrameByName("MissionName", 0);
		final StringFrame missionNameHeader = (StringFrame) this.rootFrame.getFrameByName("MissionNameHeader", 0);

		this.campaignSelectFrame = this.rootFrame.getFrameByName("CampaignSelectFrame", 0);
		this.campaignSelectFrame.setVisible(false);
		this.difficultySelect = (PopupMenuFrame) this.rootFrame.getFrameByName("DifficultySelect", 0);
		final List<MenuItem> difficultyItems = new ArrayList<>();
		for (final String difficultyKey : CAMPAIGN_DIFFICULTY_KEYS) {
			difficultyItems.add(new MenuItem(this.rootFrame.getTemplates().getDecoratedString(difficultyKey), -2));
		}
		((MenuFrame) this.difficultySelect.getPopupMenuFrame()).setItems(this.uiViewport, difficultyItems);
		final StringFrame difficultyTitle = (StringFrame) ((GlueTextButtonFrame) this.difficultySelect
				.getPopupTitleFrame()).getButtonText();
		this.rootFrame.setDecoratedText(difficultyTitle, CAMPAIGN_DIFFICULTY_KEYS[0]);
		this.difficultySelect.setMenuClickListener((button, menuItemIndex) -> {
			if ((menuItemIndex >= 0) && (menuItemIndex < CAMPAIGN_DIFFICULTY_KEYS.length)) {
				this.campaignDifficulty = CAMPAIGN_DIFFICULTIES[menuItemIndex];
				this.rootFrame.setDecoratedText(difficultyTitle, CAMPAIGN_DIFFICULTY_KEYS[menuItemIndex]);
			}
		});

		this.campaignWarcraftIIILogo = (SpriteFrame) this.rootFrame.getFrameByName("WarCraftIIILogo", 0);
		this.rootFrame.setSpriteFrameModel(this.campaignWarcraftIIILogo, this.rootFrame.getSkinField("MainMenuLogo"));
		this.campaignWarcraftIIILogo.setVisible(false);
		this.campaignWarcraftIIILogo
				.addSetPoint(new SetPoint(FramePoint.TOPRIGHT, this.campaignMenu, FramePoint.TOPRIGHT,
						GameUI.convertX(this.uiViewport, -0.13f), GameUI.convertY(this.uiViewport, -0.08f)));
		this.campaignRootMenuUI = new CampaignMenuUI(null, this.campaignMenu, this.rootFrame, this.uiViewport);
		this.campaignRootMenuUI.setVisible(false);
		this.campaignRootMenuUI.addSetPoint(new SetPoint(FramePoint.TOPRIGHT, this.campaignMenu, FramePoint.TOPRIGHT,
				GameUI.convertX(this.uiViewport, -0.0f), GameUI.convertY(this.uiViewport, -0.12f)));
		this.campaignRootMenuUI.setWidth(GameUI.convertX(this.uiViewport, 0.30f));
		this.campaignRootMenuUI.setHeight(GameUI.convertY(this.uiViewport, 0.42f));
		this.rootFrame.add(this.campaignRootMenuUI);

		this.campaignBackButton.setOnClick(new Runnable() {
			@Override
			public void run() {
				if (MenuUI.this.menuState == MenuState.MISSION_SELECT) {
					MenuUI.this.currentMissionSelectMenuUI.setVisible(false);
					MenuUI.this.missionSelectFrame.setVisible(false);
					MenuUI.this.menuState = MenuState.CAMPAIGN;
				}
				else {
					MenuUI.this.campaignMenu.setVisible(false);
					MenuUI.this.campaignBackButton.setVisible(false);
					MenuUI.this.missionSelectFrame.setVisible(false);
					MenuUI.this.campaignSelectFrame.setVisible(false);
					MenuUI.this.campaignWarcraftIIILogo.setVisible(false);
					MenuUI.this.campaignRootMenuUI.setVisible(false);
					MenuUI.this.difficultySelect.getPopupMenuFrame().setVisible(false);
					MenuUI.this.campaignFade.setSequence("Birth");
					MenuUI.this.menuState = MenuState.LEAVING_CAMPAIGN;
				}
			}
		});
		final Element campaignIndex = this.campaignStrings.get("Index");
		if (campaignIndex != null) {
			// Read entry by entry: the joined field string drops the blank entries at the start of the list.
			this.campaignList = campaignIndex.getFieldAsList("CampaignList").toArray(new String[0]);
			this.campaignDatas = new CampaignMenuData[this.campaignList.length];
			for (int i = 0; i < this.campaignList.length; i++) {
				final String campaign = this.campaignList[i];
				final Element campaignElement = this.campaignStrings.get(campaign);
				if (campaignElement != null) {
					final CampaignMenuData newCampaign = new CampaignMenuData(campaignElement);
					this.campaignDatas[i] = newCampaign;
					if (this.currentCampaign == null) {
						this.currentCampaign = newCampaign;
					}

				}
			}
			// Scripts number the campaigns by their place among the list's named entries (SetMissionAvailable,
			// SetCampaignAvailable); a blank entry is a gap between groups of campaigns on the screen.
			int campaignNumber = 0;
			for (final CampaignMenuData campaign : this.campaignDatas) {
				if (campaign == null) {
					this.campaignRootMenuUI.addGap();
				}
				else {
					final List<CampaignButtonUI> missionButtons = new ArrayList<>();
					final CampaignMenuUI missionSelectMenuUI = new CampaignMenuUI(null, this.campaignMenu,
							this.rootFrame, this.uiViewport);
					missionSelectMenuUI.setVisible(false);
					missionSelectMenuUI
							.addSetPoint(new SetPoint(FramePoint.TOPRIGHT, this.campaignMenu, FramePoint.TOPRIGHT,
									GameUI.convertX(this.uiViewport, -0.0f), GameUI.convertY(this.uiViewport, -0.12f)));
					missionSelectMenuUI.setWidth(GameUI.convertX(this.uiViewport, 0.30f));
					missionSelectMenuUI.setHeight(GameUI.convertY(this.uiViewport, 0.42f));
					this.rootFrame.add(missionSelectMenuUI);

					for (final CampaignMission mission : campaign.getMissions()) {
						missionButtons.add(missionSelectMenuUI.addButton(mission.getHeader(), mission.getMissionName(),
								() -> startMission(mission)));
					}

					final CampaignButtonUI campaignButtonUI = this.campaignRootMenuUI.addButton(campaign.getHeader(),
							campaign.getName(), new Runnable() {
						@Override
						public void run() {
							MenuUI.this.difficultySelect.getPopupMenuFrame().setVisible(false);
							if (campaign != MenuUI.this.currentCampaign) {
								MenuUI.this.campaignMenu.setVisible(false);
								MenuUI.this.campaignBackButton.setVisible(false);
								MenuUI.this.missionSelectFrame.setVisible(false);
								MenuUI.this.campaignSelectFrame.setVisible(false);
								MenuUI.this.campaignWarcraftIIILogo.setVisible(false);
								MenuUI.this.campaignRootMenuUI.setVisible(false);
								MenuUI.this.campaignFade.setSequence("Birth");
								MenuUI.this.currentCampaign = campaign;
								MenuUI.this.currentMissionSelectMenuUI = missionSelectMenuUI;
								MenuUI.this.menuState = MenuState.GOING_TO_MISSION_SELECT;
							}
							else {
								MenuUI.this.campaignSelectFrame.setVisible(false);
								MenuUI.this.campaignRootMenuUI.setVisible(false);
								MenuUI.this.currentMissionSelectMenuUI.setVisible(true);
								MenuUI.this.missionSelectFrame.setVisible(true);
								MenuUI.this.menuState = MenuState.MISSION_SELECT;
							}
							MenuUI.this.rootFrame.setDecoratedText(missionName, campaign.getName());
							MenuUI.this.rootFrame.setDecoratedText(missionNameHeader, campaign.getHeader());
						}
					});
					this.campaignEntries.add(new CampaignEntry(campaign, campaignNumber++, campaignButtonUI,
							missionSelectMenuUI, missionButtons));
					if (campaign == MenuUI.this.currentCampaign) {
						MenuUI.this.currentMissionSelectMenuUI = missionSelectMenuUI;
					}
				}
			}
		}
		else {
			this.campaignButton.setEnabled(false);
		}

	}

	private void startMission(final CampaignMission mission) {
		MenuUI.this.campaignMenu.setVisible(false);
		MenuUI.this.campaignBackButton.setVisible(false);
		MenuUI.this.missionSelectFrame.setVisible(false);
		MenuUI.this.campaignSelectFrame.setVisible(false);
		MenuUI.this.campaignWarcraftIIILogo.setVisible(false);
		MenuUI.this.campaignRootMenuUI.setVisible(false);
		if (MenuUI.this.currentMissionSelectMenuUI != null) {
			MenuUI.this.currentMissionSelectMenuUI.setVisible(false);
		}
		MenuUI.this.campaignFade.setSequence("Birth");
		MenuUI.this.difficultySelect.getPopupMenuFrame().setVisible(false);
		try {
			loadAndCacheMapConfigs(mission.getMapFilename());
		}
		catch (final IOException e) {
			e.printStackTrace();
		}
		MenuUI.this.currentMapConfig.setGameDifficulty(MenuUI.this.campaignDifficulty);
		int localPlayerIndex = -1;
		for (int i = 0; i < WarsmashConstants.MAX_PLAYERS; i++) {
			final CBasePlayer player = MenuUI.this.currentMapConfig.getPlayer(i);
			if (player.getController() == CMapControl.USER) {
				player.setSlotState(CPlayerSlotState.PLAYING);
				player.setName(MenuUI.this.profileManager.getCurrentProfile());
				if (localPlayerIndex == -1) {
					localPlayerIndex = i;
				}
			}
		}
		MenuUI.this.beginGameInformation = new BeginGameInformation();
		MenuUI.this.beginGameInformation.gameMapLookup = new CurrentNetGameMapLookupPath(mission.getMapFilename());
		MenuUI.this.beginGameInformation.localPlayerIndex = localPlayerIndex;
	}

	/**
	 * Starts a mission as its button on the mission screen does, for a launcher that shows the campaigns itself.
	 *
	 * @param campaignNumber the campaign's number among the named entries of the campaign list, as scripts count
	 * @return false when the game data has no such mission
	 */
	public boolean startCampaignMission(final int campaignNumber, final int missionIndex,
			final CMapDifficulty difficulty) {
		return startCampaignMission(campaignNumber, missionIndex, difficulty, false);
	}

	/**
	 * @param reforgedMaps the campaign screen's version choice: the Reforged maps of the missions where the data has
	 *                     them, else the classic ones
	 */
	public boolean startCampaignMission(final int campaignNumber, final int missionIndex,
			final CMapDifficulty difficulty, final boolean reforgedMaps) {
		for (final CampaignEntry entry : this.campaignEntries) {
			if (entry.number == campaignNumber) {
				final List<CampaignMission> missions = entry.data.getMissions();
				if ((missionIndex < 0) || (missionIndex >= missions.size())) {
					return false;
				}
				CampaignMission mission = missions.get(missionIndex);
				final CampaignCatalog.Entry catalogEntry = this.campaignCatalog.getEntry(campaignNumber);
				if (catalogEntry != null) {
					final String mapFilename = this.campaignCatalog.missionMapFilename(catalogEntry, missionIndex,
							reforgedMaps);
					if ((mapFilename != null) && !mapFilename.equals(mission.getMapFilename())) {
						mission = new CampaignMission(mission.getHeader(), mission.getMissionName(), mapFilename);
					}
				}
				this.mainMenuFrame.setVisible(false);
				this.currentCampaign = entry.data;
				this.currentMissionSelectMenuUI = entry.missionMenu;
				if (difficulty != null) {
					this.campaignDifficulty = difficulty;
				}
				startMission(mission);
				return true;
			}
		}
		return false;
	}

	/**
	 * Starts a custom game as the custom game screen does after the given choices, for a launcher that shows its own
	 * screen: the map is selected in the list, the slots and options are set through the screen's own controls and
	 * Start Game is pressed.
	 *
	 * @param slots   "slot:type:race:team:color" entries separated by ";", where type is a {@link LobbyPlayerType}
	 *                name, race the race menu's item (0 random), team the team's index and color the player colour's
	 *                number, -1 leaving any of them as the map has it
	 * @param options the Advanced Options that are on, separated by ",": lockTeams, teamsTogether, sharedControl,
	 *                randomRaces, randomHero; null keeps the screen's defaults
	 * @return false when the map is not in the list or cannot be read
	 */
	public boolean startSkirmish(final String mapPath, final String slots, final String options) {
		final PlayerSlotPaneListenerImplementation before = this.skirmishSlotListener;
		if (!this.skirmishMapList.select(mapPath) || (this.skirmishSlotListener == before)
				|| (this.skirmishSlotListener == null)) {
			return false;
		}
		if (slots != null) {
			for (final String slot : slots.split(";")) {
				final String[] parts = slot.trim().split(":");
				if (parts.length < 2) {
					continue;
				}
				final int index = Integer.parseInt(parts[0].trim());
				this.skirmishSlotListener.setPlayerSlot(index, LobbyPlayerType.valueOf(parts[1].trim()));
				if ((parts.length > 2) && (Integer.parseInt(parts[2].trim()) >= 0)) {
					this.skirmishSlotListener.setPlayerRace(index, Integer.parseInt(parts[2].trim()));
				}
				if ((parts.length > 3) && (Integer.parseInt(parts[3].trim()) >= 0)) {
					this.skirmishSlotListener.setPlayerTeam(index, Integer.parseInt(parts[3].trim()));
				}
				if ((parts.length > 4) && (Integer.parseInt(parts[4].trim()) >= 0)) {
					this.skirmishSlotListener.setPlayerColor(index, Integer.parseInt(parts[4].trim()));
				}
			}
		}
		if (options != null) {
			final List<String> on = Arrays.asList(options.split(","));
			this.skirmishAdvancedOptionsPane.setOptions(on.contains("lockTeams"), on.contains("teamsTogether"),
					on.contains("sharedControl"), on.contains("randomRaces"), on.contains("randomHero"));
		}
		this.mainMenuFrame.setVisible(false);
		this.skirmishStart.run();
		return true;
	}

	/**
	 * The campaign screens list only what the profile has opened: a campaign that is open by default or made available
	 * by a script, and in it the first mission and those a script made available.
	 */
	private void refreshCampaignAvailability() {
		final int menuCampaignIndex = this.classicCampaignList ? CCampaignProgress.getClassicMenuCampaignIndex()
				: CCampaignProgress.getMenuCampaignIndex();
		CampaignEntry firstOpen = null;
		CampaignEntry menuCampaign = null;
		boolean currentOpen = false;
		for (final CampaignEntry entry : this.campaignEntries) {
			final boolean open = entry.data.isDefaultOpen() || CCampaignProgress.isCampaignAvailable(entry.number);
			entry.button.setVisible(open);
			for (int i = 0; i < entry.missionButtons.size(); i++) {
				entry.missionButtons.get(i)
						.setVisible(open && ((i == 0) || CCampaignProgress.isMissionAvailable(entry.number, i)));
			}
			entry.missionMenu.positionBounds(this.rootFrame, this.uiViewport);
			if (open) {
				if (firstOpen == null) {
					firstOpen = entry;
				}
				if (entry.number == menuCampaignIndex) {
					menuCampaign = entry;
				}
				currentOpen |= entry.data == this.currentCampaign;
			}
		}
		this.campaignRootMenuUI.positionBounds(this.rootFrame, this.uiViewport);
		final CampaignEntry shown = menuCampaign != null ? menuCampaign : (currentOpen ? null : firstOpen);
		if (shown != null) {
			this.currentCampaign = shown.data;
			this.currentMissionSelectMenuUI = shown.missionMenu;
		}
	}

	private static final class CampaignEntry {
		private final CampaignMenuData data;
		private final int number;
		private final CampaignButtonUI button;
		private final CampaignMenuUI missionMenu;
		private final List<CampaignButtonUI> missionButtons;

		private CampaignEntry(final CampaignMenuData data, final int number, final CampaignButtonUI button,
				final CampaignMenuUI missionMenu, final List<CampaignButtonUI> missionButtons) {
			this.data = data;
			this.number = number;
			this.button = button;
			this.missionMenu = missionMenu;
			this.missionButtons = missionButtons;
		}
	}

	/** The loading screen frame; built once, early when a launcher previews it. */
	private void createLoadingFrame() {
		if (this.loadingFrame != null) {
			return;
		}
		this.loadingFrame = this.rootFrame.createFrame("Loading", this.rootFrame, 0, 0);
		((AbstractRenderableFrame) this.loadingFrame).clearFramePointAssignments();
		anchorGlueLayer(this.loadingFrame, FramePoint.CENTER);
		this.loadingFrame.setVisible(false);
		this.loadingCustomPanel = this.rootFrame.getFrameByName("LoadingCustomPanel", 0);
		this.loadingCustomPanel.setVisible(false);
		this.loadingTitleText = (StringFrame) this.rootFrame.getFrameByName("LoadingTitleText", 0);
		this.loadingSubtitleText = (StringFrame) this.rootFrame.getFrameByName("LoadingSubtitleText", 0);
		this.loadingText = (StringFrame) this.rootFrame.getFrameByName("LoadingText", 0);
		this.loadingBar = (SpriteFrame) this.rootFrame.getFrameByName("LoadingBar", 0);
		this.loadingBarText = (StringFrame) this.rootFrame.getFrameByName("LoadingBarText", 0);
		this.loadingBackground = (SpriteFrame) this.rootFrame.getFrameByName("LoadingBackground", 0);

		this.loadingMeleePanel = this.rootFrame.getFrameByName("LoadingMeleePanel", 0);
		this.loadingMeleePanel.setVisible(false);
		// The skirmish map info pane later registers a frame with the same name, so this one is kept here.
		this.loadingMinimapImage = (AbstractRenderableFrame) this.rootFrame.getFrameByName("MinimapImage", 0);
		this.loadingMinimapTexture = this.rootFrame.createTextureFrame("LoadingMinimapTexture", this.loadingMinimapImage,
				false, TextureFrame.DEFAULT_TEX_COORDS);
		this.rootFrame.remove(this.loadingMinimapTexture);
		((SimpleFrame) this.loadingMeleePanel).add(this.loadingMinimapTexture);
		this.loadingMinimapTexture.setSetAllPoints(true);
	}

	private void mainDialogs() {
		this.confirmDialog = this.rootFrame.createFrame("DialogWar3", this.rootFrame, 0, 0);
		this.confirmDialog.setVisible(false);

		createLoadingFrame();

		this.battleNetUI = new BattleNetUI(this.rootFrame, this.uiViewport, this.uiScene, this.dataSource,
				new BattleNetUIActionListener() {

					@Override
					public void cancelLoginPrompt() {
						MenuUI.this.battleNetUI.hide();
						MenuUI.this.battleNetUI.getDoors().setSequence(PrimaryTag.DEATH);
						MenuUI.this.menuScreen.unAlternateModelBackToNormal();
						MenuUI.this.menuState = MenuState.LEAVING_BATTLE_NET;
						MenuUI.this.gamingNetworkConnection.userRequestDisconnect();
					}

					@Override
					public void recoverPassword(final String text) {

					}

					@Override
					public void logon(final String accountName, final String password) {
						if (accountName.isEmpty()) {
							MenuUI.this.dialog.showError("ERROR_ID_NAMEBLANK", null);
						}
						else if (password.isEmpty()) {
							MenuUI.this.dialog.showError("NETERROR_NOPASSWORD", null);
						}
						else {
							final char[] passwordData = getPasswordData(password);
							MenuUI.this.gamingNetworkConnection.login(accountName, passwordData);
						}
					}

					private char[] getPasswordData(final String password) {
						final int nPasswordChars = password.length();
						final char[] passwordData = new char[nPasswordChars];
						password.getChars(0, nPasswordChars, passwordData, 0);
						return passwordData;
					}

					@Override
					public void quitBattleNet() {
						MenuUI.this.battleNetUI.hide();
						playCurrentBattleNetGlueSpriteDeath();
						MenuUI.this.glueSpriteLayerCenter.setSequence("Death");
						MenuUI.this.menuState = MenuState.LEAVING_BATTLE_NET_FROM_LOGGED_IN;
						MenuUI.this.gamingNetworkConnection.userRequestDisconnect();
					}

					@Override
					public void openCustomGameMenu() {
						MenuUI.this.battleNetUI.hideCurrentScreen();
						playCurrentBattleNetGlueSpriteDeath();
						final boolean insideTopBarMode = isInsideTopBarMode();
						if (insideTopBarMode) {
							MenuUI.this.glueSpriteLayerCenter.setSequence("Death");
						}
						MenuUI.this.menuState = MenuState.GOING_TO_BATTLE_NET_CUSTOM_GAME_MENU;
						if (MenuUI.this.gameListQueryTask != null) {
							MenuUI.this.gameListQueryTask.cancel();
						}
						MenuUI.this.gameListQueryTask = Timer.instance().scheduleTask(new Task() {
							@Override
							public void run() {
								MenuUI.this.gamingNetworkConnection
										.queryGamesList(MenuUI.this.battleNetUI.getGamingNetworkSessionToken());
							}
						}, 0.0f, 5.0f);
					}

					@Override
					public void enterDefaultChat() {
						requestEnterDefaultChat();
					}

					@Override
					public void returnToChat() {
						MenuUI.this.battleNetUI.hideCurrentScreen();
						playCurrentBattleNetGlueSpriteDeath();
						if (MenuUI.this.battleNetUI.getCurrentChannel() == null) {
							MenuUI.this.menuState = MenuState.GOING_TO_BATTLE_NET_WELCOME;
						}
						else {
							final boolean insideTopBarMode = isInsideTopBarMode();
							if (insideTopBarMode) {
								MenuUI.this.menuState = MenuState.GOING_TO_BATTLE_NET_CHAT_CHANNEL;
							}
							else {
								MenuUI.this.menuState = MenuState.GOING_TO_BATTLE_NET_CHAT_CHANNEL_FROM_OUTSIDE;
							}
						}
					}

					@Override
					public void requestJoinChannel(final String text) {
						MenuUI.this.gamingNetworkConnection
								.joinChannel(MenuUI.this.battleNetUI.getGamingNetworkSessionToken(), text);
					}

					@Override
					public void createAccount(final String username, final String password,
							final String repeatPassword) {
						if (!password.equals(repeatPassword)) {
							MenuUI.this.dialog.showError("NETERROR_PASSWORDMISMATCH", null);
						}
						else if (username.isEmpty()) {
							MenuUI.this.dialog.showError("ERROR_ID_NAMEBLANK", null);
						}
						else if (password.isEmpty()) {
							MenuUI.this.dialog.showError("NETERROR_NOPASSWORD", null);
						}
						else if (username.length() < 3) {
							// TODO checks like this should be server side!!!
							MenuUI.this.dialog.showError("NETERROR_USERNAMETOOSHORT", null);
						}
						else if (password.length() < 3) {
							MenuUI.this.dialog.showError("NETERROR_PASSWORDTOOSHORT", null);
						}
						else {
							final char[] passwordData = getPasswordData(password);
							MenuUI.this.gamingNetworkConnection.createAccount(username, passwordData);
						}
					}

					@Override
					public void submitChatText(final String text) {
						if (text.startsWith("/me ")) {
							MenuUI.this.gamingNetworkConnection.emoteMessage(
									MenuUI.this.battleNetUI.getGamingNetworkSessionToken(), text.substring(4));
						}
						else {
							MenuUI.this.gamingNetworkConnection
									.chatMessage(MenuUI.this.battleNetUI.getGamingNetworkSessionToken(), text);
						}
					}

					@Override
					public void showChannelChooserPanel() {
						MenuUI.this.battleNetUI.hideCurrentScreen();
						playCurrentBattleNetGlueSpriteDeath();
						MenuUI.this.menuState = MenuState.GOING_TO_BATTLE_NET_CHANNEL_MENU;

					}

					@Override
					public void showCreateGameMenu() {
						MenuUI.this.battleNetUI.hideCurrentScreen();
						playCurrentBattleNetGlueSpriteDeath();
						MenuUI.this.menuState = MenuState.GOING_TO_BATTLE_NET_CREATE_CUSTOM_GAME_MENU;
					}

					@Override
					public void requestJoinGame(final String text) {
						MenuUI.this.gamingNetworkConnection
								.joinGame(MenuUI.this.battleNetUI.getGamingNetworkSessionToken(), text);
					}

					@Override
					public void showError(final String errorKey) {
						MenuUI.this.dialog.showError(errorKey, null);
					}

					@Override
					public void createGame(final String gameName, final String mapPath, final int mapPlayerSlots,
							final LobbyGameSpeed gameSpeed, final HostedGameVisibility hostedGameVisibility,
							final long mapChecksum, final War3Map map) {
						MenuUI.this.gamingNetworkConnection.createGame(
								MenuUI.this.battleNetUI.getGamingNetworkSessionToken(), gameName, mapPath,
								mapPlayerSlots, gameSpeed, hostedGameVisibility, mapChecksum);
						final SeekableByteChannel inputChannel = map.getInternalMpqContentsDataSource()
								.getInputChannel();
						try {
							inputChannel.position(0);
							final ByteBuffer mapDataBuffer = ByteBuffer.allocate(1300).clear();
							int mapSeqNo = 0;
							while (inputChannel.read(mapDataBuffer) != -1) {
								mapDataBuffer.flip();
								MenuUI.this.gamingNetworkConnection.uploadMapData(
										MenuUI.this.battleNetUI.getGamingNetworkSessionToken(), mapSeqNo++,
										mapDataBuffer);
								mapDataBuffer.clear();
							}
							MenuUI.this.gamingNetworkConnection
									.mapDone(MenuUI.this.battleNetUI.getGamingNetworkSessionToken(), mapSeqNo);
						}
						catch (final IOException e) {
							throw new IllegalStateException(e);
						}
					}

					@Override
					public void leaveCustomGame() {
						MenuUI.this.gamingNetworkConnection
								.leaveGame(MenuUI.this.battleNetUI.getGamingNetworkSessionToken());
					}

					@Override
					public void gameLobbySetPlayerRace(final int serverSlot, final int raceItemIndex) {
						MenuUI.this.gamingNetworkConnection.gameLobbySetPlayerRace(
								MenuUI.this.battleNetUI.getGamingNetworkSessionToken(), serverSlot, raceItemIndex);
					}

					@Override
					public void gameLobbySetPlayerSlot(final int serverSlot, final LobbyPlayerType lobbyPlayerType) {
						MenuUI.this.gamingNetworkConnection.gameLobbySetPlayerSlot(
								MenuUI.this.battleNetUI.getGamingNetworkSessionToken(), serverSlot, lobbyPlayerType);
					}

					@Override
					public void startGame() {
						MenuUI.this.gamingNetworkConnection
								.gameLobbyStartGame(MenuUI.this.battleNetUI.getGamingNetworkSessionToken());
					}
				});

		this.dialog = new DialogWar3(this.rootFrame, this.uiViewport);

		// position all
		this.rootFrame.positionBounds(this.rootFrame, this.uiViewport);

		this.menuState = MenuState.GOING_TO_MAIN_MENU;

		loadSounds();

		try {
			final String glueLoopField = this.rootFrame.getSkinField("GlueScreenLoop");
			this.mainMenuGlueScreenLoop = this.uiSounds.getSound(glueLoopField);
		}
		catch (final Exception exc) {
			exc.printStackTrace();
			this.mainMenuGlueScreenLoop = new UnitSound(null, 0, 0, 0, 0, 0, 0, false);
		}
		this.glueScreenLoop = this.mainMenuGlueScreenLoop;
		if (!this.launcherMode) {
			this.glueScreenLoop.play(this.uiScene.audioContext, 0f, 0f, 0f);
		}
	}

	public void show() {
		if (this.launcherMode) {
			return;
		}
		playMusic(this.rootFrame.trySkinField("GlueMusic"), true, 0);
		this.glueScreenLoop.play(this.uiScene.audioContext, 0f, 0f, 0f);
	}

	/** Must be set before the menu is built. */
	public void setLauncherMode(final boolean launcherMode) {
		this.launcherMode = launcherMode;
	}

	/**
	 * Shows the loading screen of the map a launcher starts while the rest of the menu is still being built, so that
	 * the start shows the map's loading screen from its first frames; the game then goes on loading on the same
	 * screen. The players of a melee screen are only listed once the game is set up.
	 *
	 * @param startupSlots the launcher's slots as {@link #startSkirmish} takes them, for the local player's race; or
	 *                     null
	 * @return false when the map cannot be read
	 */
	public boolean previewLoadingScreen(final String mapFilename, final String startupSlots) {
		try {
			final War3Map map = War3MapViewer.beginLoadingMap(this.dataSource, mapFilename);
			final War3MapW3i mapInfo = map.readMapInformation();
			final int campaignBackground = mapInfo.getCampaignBackground();
			final WTS wts = Warcraft3MapObjectData.loadWTS(map);
			createLoadingFrame();
			if (campaignBackground < 0) {
				this.previewedBackground = meleeLoadingBackground(map, previewSkinKey(mapInfo, startupSlots));
				fillLoadingScreen(this.previewedBackground, 0, mapInfo, wts);
				fillMapLoadingPanels(map, mapInfo, wts);
			}
			else {
				final DataTable worldEditData = new StandardObjectData(map).getWorldEditData();
				fillCampaignLoadingScreen(worldEditData, campaignBackground, mapInfo, wts);
			}
			this.loadingFrame.setVisible(true);
			this.loadingBar.setVisible(true);
			this.previewingLoading = true;
			return true;
		}
		catch (final IOException | RuntimeException e) {
			e.printStackTrace();
			return false;
		}
	}

	/** While the loading screen is previewed: how much of the menu is built. */
	public void setPreviewProgress(final float ratio) {
		if (this.previewingLoading) {
			this.loadingBarStart = PREVIEW_BAR_SHARE * ratio;
			this.loadingBar.setFrameByRatio(this.loadingBarStart);
		}
	}

	/**
	 * A loaded map waits on its loading screen until the player taps (or presses a key), the touch counterpart of the
	 * original's "Press any key to continue"; the game clock does not start before.
	 */
	public void setWaitForTap(final boolean waitForTap) {
		this.waitForTap = waitForTap;
	}

	private void showTapToContinue(final float deltaTime) {
		if (this.tapPulse == 0) {
			this.loadingBar.setFrameByRatio(1.0f);
			this.rootFrame.setText(this.loadingBarText, TAP_TO_CONTINUE);
			this.loadingBarText.setColor(new Color(0.99f, 0.827f, 0.0705f, 1.0f));
		}
		this.tapPulse = (this.tapPulse + deltaTime) % TAP_PULSE_SECONDS;
		this.loadingBarText.setAlpha(
				0.6f + (0.4f * (float) Math.cos((this.tapPulse / TAP_PULSE_SECONDS) * 2 * Math.PI)));
	}

	private boolean isWaitingForTap() {
		return this.loadingMapDone && this.waitForTap && !this.continueTapped;
	}

	/** @return whether the input was the tap a loaded map waited for */
	private boolean continueAfterLoading() {
		if (!isWaitingForTap()) {
			return false;
		}
		this.continueTapped = true;
		this.tapPulse = 0;
		this.loadingBarText.setAlpha(1.0f);
		if (this.loadingBarTextColor != null) {
			this.loadingBarText.setColor(this.loadingBarTextColor);
		}
		this.rootFrame.setDecoratedText(this.loadingBarText, "LOADING_LOADING");
		return true;
	}

	private void internalStartMap(final String mapFilename, final int localPlayerIndex) {
		this.loadingMapDone = false;
		this.continueTapped = false;
		this.tapPulse = 0;
		if (this.loadingBarTextColor == null) {
			this.loadingBarTextColor = new Color(this.loadingBarText.getColor());
		}
		this.loadingFrame.setVisible(true);
		this.loadingBar.setVisible(true);
		final DataSource codebase = WarsmashGdxMapScreen.parseDataSources(this.warsmashIni);
		final GameTurnManager turnManager;
		turnManager = GameTurnManager.PAUSED;
		final War3MapViewer viewer = new War3MapViewer(codebase, this.screenManager, this.currentMapConfig,
				turnManager);

		if (WarsmashGdxMapScreen.ENABLE_AUDIO) {
			viewer.worldScene.enableAudio();
			viewer.enableAudio();
		}
		try {
			viewer.setMapFileName(mapFilename);
			final War3Map map = War3MapViewer.beginLoadingMap(codebase, mapFilename);
			final War3MapW3i mapInfo = map.readMapInformation();
			WarsmashConstants.selectMapGameVersion(mapFilename, mapInfo.getGameDataVersion());
			final DataTable worldEditData = viewer.loadWorldEditData(map);
			final WTS wts = viewer.preloadWTS(map);

			final int campaignBackground = mapInfo.getCampaignBackground();
			if (campaignBackground == -1) {
				final String background = meleeLoadingBackground(map,
						loadingSkinKey(this.currentMapConfig.getPlayer(localPlayerIndex)));
				// A previewed screen goes on with its bar; its backdrop only changes for a race the preview could not
				// know.
				if (!this.previewingLoading || !background.equals(this.previewedBackground)) {
					fillLoadingScreen(background, 0, mapInfo, wts);
				}
				if (!this.previewingLoading) {
					this.loadingBarStart = 0;
				}
				this.loadingBar.setFrameByRatio(this.loadingBarStart);
			}
			else if (!this.previewingLoading) {
				fillCampaignLoadingScreen(worldEditData, campaignBackground, mapInfo, wts);
				this.loadingBarStart = 0;
			}
			// A previewed screen has been shown since the menu started building; only its bar goes on.
			this.previewingLoading = false;
			this.previewedBackground = null;
			if (fillMapLoadingPanels(map, mapInfo, wts)) {
				setLoadingPlayers();
			}
			this.loadingMap = new LoadingMap(viewer, map, mapInfo);

		}
		catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Shows the melee panel (map name, game type, minimap) or the custom panel of a loading screen without a campaign
	 * background.
	 *
	 * @return whether it is the melee panel
	 */
	private boolean fillMapLoadingPanels(final War3Map map, final War3MapW3i mapInfo, final WTS wts) {
		final boolean meleeLoading = (mapInfo.getCampaignBackground() == -1)
				&& mapInfo.hasFlag(War3MapW3iFlags.MELEE_MAP);
		this.loadingCustomPanel.setVisible(!meleeLoading);
		this.loadingMeleePanel.setVisible(meleeLoading);
		if (meleeLoading) {
			this.rootFrame.setText((StringFrame) this.rootFrame.getFrameByName("LoadingMeleeMapName", 0),
					getStringWithWTS(wts, mapInfo.getName()));
			this.rootFrame.setDecoratedText(
					(StringFrame) this.rootFrame.getFrameByName("LoadingMeleeGameTypeValue", 0), "MELEE");
			setLoadingMinimap(map, mapInfo);
			alignMinimapWithBackdrop();
		}
		return meleeLoading;
	}

	/**
	 * NOTE: this is a heavy reload to get the user skin, because MeleeUI loads it again so much later in the map load
	 * pipeline. It's probably possible to optimize that, so that the stuff we load here is passed downstream to
	 * MeleeUI and only loaded once.
	 */
	private static String meleeLoadingBackground(final War3Map map, final String skinKey) {
		final GameSkin userSkin = GameUI.loadSkin(map, skinKey);
		return userSkin.getSkin().getField("LoadingMeleeBackground");
	}

	/** The skin whose melee loading backdrop a player sees: that of the chosen race, Default for random. */
	private static String loadingSkinKey(final CBasePlayer player) {
		for (int j = 0; j < WarsmashConstants.RACE_MANAGER.getEntryCount(); j++) {
			final CRaceManagerEntry entry = WarsmashConstants.RACE_MANAGER.get(j);
			final CRacePreference racePreference = WarsmashConstants.RACE_MANAGER
					.getRacePreferenceById(entry.getRacePrefId());
			if (player.isRacePrefSet(racePreference)) {
				return entry.getKey();
			}
		}
		return "Default";
	}

	/**
	 * The skin of the local player's race before the map's script has set the players up: the race of the first user
	 * slot of a launcher's slots ("slot:USER:race..." as {@link #startSkirmish}), else of the map's first human
	 * player.
	 */
	private static String previewSkinKey(final War3MapW3i mapInfo, final String startupSlots) {
		int raceId = -1;
		if (startupSlots != null) {
			for (final String slot : startupSlots.split(";")) {
				final String[] parts = slot.trim().split(":");
				if ((parts.length > 2) && "USER".equals(parts[1].trim())) {
					try {
						raceId = Integer.parseInt(parts[2].trim());
					}
					catch (final NumberFormatException e) {
						raceId = -1;
					}
					break;
				}
			}
		}
		if (raceId < 0) {
			for (final Player player : mapInfo.getPlayers()) {
				if (player.getType() == 1) {
					raceId = player.getRace();
					break;
				}
			}
		}
		final CRace race = raceId > 0 ? WarsmashConstants.RACE_MANAGER.getRace(raceId) : null;
		final CRaceManagerEntry entry = race == null ? null : WarsmashConstants.RACE_MANAGER.get(race);
		return entry == null ? "Default" : entry.getKey();
	}

	private void fillCampaignLoadingScreen(final DataTable worldEditData, final int campaignBackground,
			final War3MapW3i mapInfo, final WTS wts) {
		final Element loadingScreens = worldEditData.get("LoadingScreens");
		final String key = String.format("%2s", Integer.toString(campaignBackground)).replace(' ', '0');
		fillLoadingScreen(loadingScreens.getField(key, 3), loadingScreens.getFieldValue(key, 2), mapInfo, wts);
	}

	private void fillLoadingScreen(final String screenModel, final int sequence, final War3MapW3i mapInfo,
			final WTS wts) {
		this.menuScreen.setModel(null, null);
		this.rootFrame.setSpriteFrameModel(this.loadingBackground, screenModel);
		this.loadingBackground.setSequence(sequence);
		this.loadingBackground.setVisible(true);
		fillScreenWidth(this.loadingBackground);
		this.rootFrame.setSpriteFrameModel(this.loadingBar, this.rootFrame.getSkinField("LoadingProgressBar"));
		this.loadingBar.setSequence(0);
		this.loadingBar.setFrameByRatio(0.0f);
		this.loadingBar.setZDepth(0.25f);
		this.rootFrame.setText(this.loadingTitleText, getStringWithWTS(wts, mapInfo.getLoadingScreenTitle()));
		this.rootFrame.setText(this.loadingSubtitleText, getStringWithWTS(wts, mapInfo.getLoadingScreenSubtitle()));
		this.loadingText.setJustifyV(TextJustify.TOP);
		this.rootFrame.setText(this.loadingText, getStringWithWTS(wts, mapInfo.getLoadingScreenText()));
		this.loadingCustomPanel.setVisible(true);
		this.loadingMeleePanel.setVisible(false);
	}

	private static String getStringWithWTS(final WTS wts, String string) {
		if (string.startsWith("TRIGSTR_")) {
			string = wts.get(Integer.parseInt(string.substring(8)));
		}
		return string;
	}

	public void startMap(final String mapFilename) {
		startMap(mapFilename, null);
	}

	/**
	 * @param computerSlots comma separated "slot:race" pairs that are played by a normal computer, or null
	 */
	public void startMap(final String mapFilename, final String computerSlots) {
		this.mainMenuFrame.setVisible(false);
		// A level a script changes to (or restarts) is played at the difficulty of the game before it.
		final CMapDifficulty difficulty = this.currentMapConfig == null ? CMapDifficulty.NORMAL
				: this.currentMapConfig.getGameDifficulty();

		try {
			loadAndCacheMapConfigs(mapFilename);
		}
		catch (final IOException e) {
			e.printStackTrace();
		}
		this.currentMapConfig.setGameDifficulty(difficulty);

		MenuUI.this.campaignMenu.setVisible(false);
		MenuUI.this.campaignBackButton.setVisible(false);
		MenuUI.this.missionSelectFrame.setVisible(false);
		MenuUI.this.campaignSelectFrame.setVisible(false);
		MenuUI.this.campaignWarcraftIIILogo.setVisible(false);
		MenuUI.this.campaignRootMenuUI.setVisible(false);
		if (this.currentMissionSelectMenuUI != null) {
			MenuUI.this.currentMissionSelectMenuUI.setVisible(false);
		}
		MenuUI.this.skirmish.setVisible(false);
		MenuUI.this.glueSpriteLayerTopLeft.setSequence("Death");
		MenuUI.this.glueSpriteLayerTopRight.setSequence("Death");
		MenuUI.this.beginGameInformation = new BeginGameInformation();
		MenuUI.this.beginGameInformation.gameMapLookup = new CurrentNetGameMapLookupPath(mapFilename);
		int localPlayerIndex = -1;
		for (int i = 0; i < WarsmashConstants.MAX_PLAYERS; i++) {
			final CBasePlayer player = this.currentMapConfig.getPlayer(i);
			if (player.getController() == CMapControl.USER) {
				player.setSlotState(CPlayerSlotState.PLAYING);
				if (localPlayerIndex == -1) {
					localPlayerIndex = i;
				}
			}
			else if (player.getController() == CMapControl.COMPUTER) {
				// Measured: the original's -loadfile plays the computer slots of the map script's config.
				player.setSlotState(CPlayerSlotState.PLAYING);
				player.setAIDifficulty(AIDifficulty.NORMAL);
			}
		}
		if (computerSlots != null) {
			for (final String slot : computerSlots.split(",")) {
				final String[] slotAndRace = slot.split(":");
				final CBasePlayer player = this.currentMapConfig.getPlayer(Integer.parseInt(slotAndRace[0].trim()));
				player.setController(CMapControl.COMPUTER);
				player.setSlotState(CPlayerSlotState.PLAYING);
				player.setAIDifficulty(AIDifficulty.NORMAL);
				if (slotAndRace.length > 1) {
					for (int i = 0; i < WarsmashConstants.RACE_MANAGER.getEntryCount(); i++) {
						final CRaceManagerEntry entry = WarsmashConstants.RACE_MANAGER.get(i);
						if (entry.getKey().equalsIgnoreCase(slotAndRace[1].trim())) {
							player.setRacePref(
									WarsmashConstants.RACE_MANAGER.getRacePreferenceById(entry.getRacePrefId()));
						}
					}
				}
				if (localPlayerIndex == player.getId()) {
					localPlayerIndex = -1;
				}
			}
			if (localPlayerIndex == -1) {
				for (int i = 0; i < WarsmashConstants.MAX_PLAYERS; i++) {
					if (this.currentMapConfig.getPlayer(i).getController() == CMapControl.USER) {
						localPlayerIndex = i;
						break;
					}
				}
			}
		}
		if (localPlayerIndex != -1) {
			this.currentMapConfig.getPlayer(localPlayerIndex).setName(this.profileManager.getCurrentProfile());
		}
		MenuUI.this.beginGameInformation.localPlayerIndex = localPlayerIndex;
//		this.beginGameInformation.loadingStarted = true;
		MenuUI.this.menuState = MenuState.GOING_TO_MAP;
	}

	private void loadAndCacheMapConfigs(final String mapFilename) throws IOException {
		final War3Map map = War3MapViewer.beginLoadingMap(MenuUI.this.dataSource, mapFilename);
		final War3MapW3i mapInfo = map.readMapInformation();
		final WTS wtsFile = Warcraft3MapObjectData.loadWTS(map);
		MenuUI.this.rootFrame.setMapStrings(wtsFile);
		final War3MapConfig war3MapConfig = new War3MapConfig(WarsmashConstants.MAX_PLAYERS);
		for (int i = 0; (i < WarsmashConstants.MAX_PLAYERS) && (i < mapInfo.getPlayers().size()); i++) {
			final CBasePlayer player = war3MapConfig.getPlayer(i);
			player.setName(MenuUI.this.rootFrame.getTrigStr(mapInfo.getPlayers().get(i).getName()));
		}
		war3MapConfig.applyMapInformation(mapInfo);
		Jass2.loadConfig(map, MenuUI.this.uiViewport, MenuUI.this.uiScene, MenuUI.this.rootFrame, war3MapConfig,
				WarsmashConstants.JASS_FILE_LIST).config();
		shuffleStartLocations(war3MapConfig, prepareMeleeLobby(war3MapConfig, mapInfo));
		MenuUI.this.currentMapConfig = war3MapConfig;
	}

	/**
	 * Melee games start as free for all: each player on a team of their own until the lobby joins them.
	 *
	 * @return how many players take random start locations, 0 when the map fixes them
	 */
	private static int prepareMeleeLobby(final War3MapConfig config, final War3MapW3i mapInfo) {
		if (!mapInfo.hasFlag(War3MapW3iFlags.MELEE_MAP)
				|| mapInfo.hasFlag(War3MapW3iFlags.FIXED_PLAYER_SETTINGS_FOR_CUSTOM_FORCES)) {
			return 0;
		}
		final int playerCount = Math.min(WarsmashConstants.MAX_PLAYERS, mapInfo.getPlayers().size());
		for (int i = 0; i < playerCount; i++) {
			config.getPlayer(i).setTeam(i);
		}
		config.setLobbyTeams(true);
		return playerCount;
	}

	/**
	 * A melee map without fixed player settings gives its players random start locations, as the original game's
	 * game setup does; the map script itself gives each player its own. With teams together, the players of a team
	 * take the free locations nearest to their first member's.
	 */
	private static void shuffleStartLocations(final War3MapConfig config, final int playerCount) {
		// Measured: a script's SetGamePlacement(MAP_PLACEMENT_FIXED) keeps each player on the start location its
		// SetPlayerStartLocation gave it.
		if (config.getPlacement() == CMapPlacement.FIXED) {
			return;
		}
		final List<Integer> locations = new ArrayList<>();
		for (int i = 0; i < playerCount; i++) {
			locations.add(config.getPlayer(i).getStartLocationIndex());
		}
		Collections.shuffle(locations);
		if (config.getPlacement() != CMapPlacement.TEAMS_TOGETHER) {
			for (int i = 0; i < playerCount; i++) {
				config.getPlayer(i).setStartLocationIndex(locations.get(i));
			}
			return;
		}
		final Map<Integer, List<Integer>> teams = new LinkedHashMap<>();
		final List<Integer> order = new ArrayList<>();
		for (int i = 0; i < playerCount; i++) {
			order.add(i);
		}
		Collections.shuffle(order);
		for (final int i : order) {
			teams.computeIfAbsent(config.getPlayer(i).getTeam(), (team) -> new ArrayList<>()).add(i);
		}
		for (final List<Integer> members : teams.values()) {
			final War3MapConfigStartLoc first = config.getStartLoc(locations.get(0));
			for (final int member : members) {
				int nearest = 0;
				float nearestDistance = Float.MAX_VALUE;
				for (int k = 0; k < locations.size(); k++) {
					final War3MapConfigStartLoc candidate = config.getStartLoc(locations.get(k));
					final float dx = candidate.getX() - first.getX();
					final float dy = candidate.getY() - first.getY();
					final float distance = (dx * dx) + (dy * dy);
					if (distance < nearestDistance) {
						nearestDistance = distance;
						nearest = k;
					}
				}
				config.getPlayer(member).setStartLocationIndex(locations.remove(nearest));
			}
		}
	}

	/** The profile a launcher chose for the game it starts: saves and campaign progress go to its folder. */
	public void useProfile(final String profileName) {
		if ((profileName == null) || profileName.isEmpty()) {
			return;
		}
		if (!this.profileManager.hasProfile(profileName)) {
			this.profileManager.addProfile(profileName);
		}
		setCurrentProfile(profileName);
	}

	private void setCurrentProfile(final String selectedProfileName) {
		this.profileManager.setCurrentProfile(selectedProfileName);
		this.rootFrame.setText(MenuUI.this.profileNameText, selectedProfileName);
	}

	protected void setSinglePlayerButtonsEnabled(final boolean b) {
		this.profileButton.setEnabled(b);
		this.campaignButton.setEnabled(b);
		this.loadSavedButton.setEnabled(b);
		this.viewReplayButton.setEnabled(b && ENABLE_NOT_YET_IMPLEMENTED_BUTTONS);
		if (this.customCampaignButton != null) {
			this.customCampaignButton.setEnabled(b && ENABLE_NOT_YET_IMPLEMENTED_BUTTONS);
		}
		this.skirmishButton.setEnabled(b);
		this.singlePlayerCancelButton.setEnabled(b);
	}

	private void setMainMenuButtonsEnabled(final boolean b) {
		this.singlePlayerButton.setEnabled(b);
		this.battleNetButton.setEnabled(b);
		this.realmButton.setEnabled(b);
		this.localAreaNetworkButton.setEnabled(b && ENABLE_NOT_YET_IMPLEMENTED_BUTTONS);
		this.optionsButton.setEnabled(b);
		this.creditsButton.setEnabled(b && ENABLE_NOT_YET_IMPLEMENTED_BUTTONS);
		this.exitButton.setEnabled(b);
		if (this.editionButton != null) {
			this.editionButton.setEnabled(b);
		}
	}

	private void setMainMenuVisible(final boolean visible) {
		this.mainMenuFrame.setVisible(visible);
		this.warcraftIIILogo.setVisible(visible);
	}

	/**
	 * Glue layer models are authored for a 4:3 screen, so each keeps a 4:3 frame pinned to the screen side it
	 * decorates; on a wide screen the side panels then meet the edges where their buttons are.
	 */
	private void anchorGlueLayer(final UIFrame layer, final FramePoint point) {
		layer.setWidth(GameUI.convertX(this.uiViewport, 0.8f));
		layer.setHeight(GameUI.convertY(this.uiViewport, 0.6f));
		layer.addSetPoint(new SetPoint(point, this.rootFrame, point, 0, 0));
	}

	/**
	 * Scales a 4:3 backdrop model up until it covers the full screen width, keeping its proportions; the top and
	 * bottom are cut off instead of leaving empty bars at the sides.
	 */
	private float getScreenFillScale() {
		return Math.max(1, this.uiViewport.getWorldWidth() / getMinWorldWidth());
	}

	private void fillScreenWidth(final SpriteFrame backdrop) {
		final float scale = getScreenFillScale();
		backdrop.clearFramePointAssignments();
		backdrop.setWidth(GameUI.convertX(this.uiViewport, 0.8f) * scale);
		backdrop.setHeight(GameUI.convertY(this.uiViewport, 0.6f) * scale);
		backdrop.addSetPoint(new SetPoint(FramePoint.CENTER, this.rootFrame, FramePoint.CENTER, 0, 0));
		backdrop.setModelScale(scale);
		backdrop.positionBounds(this.rootFrame, this.uiViewport);
	}

	/**
	 * The minimap border is painted into the melee loading backdrop, which is scaled up to fill the screen width, so
	 * the minimap image is scaled the same way about the screen center to stay inside that border.
	 */
	private void alignMinimapWithBackdrop() {
		final float scale = getScreenFillScale();
		final AbstractRenderableFrame frame = this.loadingMinimapImage;
		frame.clearFramePointAssignments();
		frame.setWidth(GameUI.convertX(this.uiViewport, 0.16f * scale));
		frame.setHeight(GameUI.convertY(this.uiViewport, 0.16f * scale));
		// LoadingMeleePanel puts the image's left edge 0.056875 right of the 4:3 left edge, 0.0425 above center.
		frame.addSetPoint(new SetPoint(FramePoint.LEFT, this.rootFrame, FramePoint.CENTER,
				GameUI.convertX(this.uiViewport, (0.056875f - 0.4f) * scale),
				GameUI.convertY(this.uiViewport, 0.0425f * scale)));
		this.loadingMeleePanel.positionBounds(this.rootFrame, this.uiViewport);
	}

	private void setLoadingPlayers() {
		for (final UIFrame slot : this.loadingPlayerSlots) {
			slot.setVisible(false);
		}
		this.loadingPlayerSlots.clear();
		final UIFrame container = this.rootFrame.getFrameByName("LoadingMeleePlayerContainer", 0);
		UIFrame previous = null;
		for (int i = 0; i < WarsmashConstants.MAX_PLAYERS; i++) {
			final CBasePlayer player = this.currentMapConfig.getPlayer(i);
			if ((player.getSlotState() != CPlayerSlotState.PLAYING) || ((player.getController() != CMapControl.USER)
					&& (player.getController() != CMapControl.COMPUTER))) {
				continue;
			}
			final UIFrame slot = this.rootFrame.createFrame("LoadingPlayerSlot", container, 0, i);
			((AbstractUIFrame) container).add(slot);
			if (previous == null) {
				slot.addSetPoint(new SetPoint(FramePoint.TOPLEFT, container, FramePoint.TOPLEFT, 0, 0));
			}
			else {
				slot.addSetPoint(new SetPoint(FramePoint.TOPLEFT, previous, FramePoint.BOTTOMLEFT, 0, 0));
			}
			// Frames created from a template register their children by name, so the latest slot's are found here.
			final StringFrame name = (StringFrame) this.rootFrame.getFrameByName("LoadingPlayerSlotName", i);
			final StringFrame race = (StringFrame) this.rootFrame.getFrameByName("LoadingPlayerSlotRace", i);
			this.rootFrame.setText((StringFrame) this.rootFrame.getFrameByName("LoadingPlayerSlotLevel", i), "");
			if (player.getController() == CMapControl.COMPUTER) {
				this.rootFrame.setDecoratedText(name, getComputerSlotKey(player.getAIDifficulty()));
			}
			else {
				this.rootFrame.setText(name, player.getName());
			}
			this.rootFrame.setDecoratedText(race, getRaceStringKey(player));
			this.loadingPlayerSlots.add(slot);
			previous = slot;
		}
		container.setHeight(GameUI.convertY(this.uiViewport, 0.0217f) * this.loadingPlayerSlots.size());
		this.loadingMeleePanel.positionBounds(this.rootFrame, this.uiViewport);
	}

	private static String getComputerSlotKey(final AIDifficulty difficulty) {
		if (difficulty == AIDifficulty.NEWBIE) {
			return "COMPUTER_NEWBIE";
		}
		if (difficulty == AIDifficulty.INSANE) {
			return "COMPUTER_INSANE";
		}
		return "COMPUTER_NORMAL";
	}

	private static String getRaceStringKey(final CBasePlayer player) {
		for (int i = 0; i < WarsmashConstants.RACE_MANAGER.getEntryCount(); i++) {
			final CRaceManagerEntry entry = WarsmashConstants.RACE_MANAGER.get(i);
			if (player.isRacePrefSet(WarsmashConstants.RACE_MANAGER.getRacePreferenceById(entry.getRacePrefId()))) {
				switch (entry.getKey()) {
				case "Human":
					return "HUMAN";
				case "Orc":
					return "ORC";
				case "Undead":
					return "UNDEAD";
				case "NightElf":
					return "NIGHT_ELF";
				default:
					return entry.getKey();
				}
			}
		}
		return "RANDOM";
	}

	private void setLoadingMinimap(final War3Map map, final War3MapW3i mapInfo) {
		if (this.loadingMinimapUnmanagedTexture != null) {
			this.loadingMinimapUnmanagedTexture.dispose();
			this.loadingMinimapUnmanagedTexture = null;
		}
		Texture minimapTexture;
		if (mapInfo.hasFlag(War3MapW3iFlags.HIDE_MINIMAP_IN_PREVIEW_SCREENS)) {
			minimapTexture = this.rootFrame.loadTexture("ui\\widgets\\glues\\minimap-unknown.blp");
		}
		else {
			try {
				minimapTexture = ImageUtils.getAnyExtensionTexture(map, "war3mapPreview.blp");
			}
			catch (final Exception exc) {
				minimapTexture = ImageUtils.getAnyExtensionTexture(map, "war3mapMap.blp");
			}
			this.loadingMinimapUnmanagedTexture = minimapTexture;
		}
		this.loadingMinimapTexture.setTexture(minimapTexture);
	}

	public void resize() {
		this.rootFrame.updateRootBounds();
		this.rootFrame.positionBounds(this.rootFrame, this.uiViewport);
	}

	public void render(final SpriteBatch batch, final GlyphLayout glyphLayout) {
		if (!this.hideUI) {
			final BitmapFont font20 = this.rootFrame.getFont20();
			if (WarsmashConstants.SHOW_FPS) {
				final BitmapFont font14 = this.rootFrame.getFont14();
				font14.setColor(Color.YELLOW);
				final Rectangle screen = this.rootFrame.getRenderBounds();
				font14.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), screen.x + 8,
						(screen.y + screen.height) - 8);
			}
			if (this.previewingLoading) {
				// The rest of the menu is still being built and shows nothing but the loading screen yet.
				this.loadingFrame.render(batch, font20, glyphLayout);
			}
			else {
				this.rootFrame.render(batch, font20, glyphLayout);
			}
		}
	}

	private float getMinWorldWidth() {
		if (this.uiViewport instanceof ExtendViewport) {
			return ((ExtendViewport) this.uiViewport).getMinWorldWidth();
		}
		return this.uiViewport.getWorldWidth();
	}

	private float getMinWorldHeight() {
		if (this.uiViewport instanceof ExtendViewport) {
			return ((ExtendViewport) this.uiViewport).getMinWorldHeight();
		}
		return this.uiViewport.getWorldHeight();
	}

	public void update(final float deltaTime) {
		if ((this.beginGameInformation != null) && (this.menuState != MenuState.GOING_TO_MAP)) {
			this.campaignFade.setVisible(false);
			if (!this.beginGameInformation.loadingStarted) {
				if (this.beginGameInformation.gameMapLookup instanceof CurrentNetGameMapLookupFile) {
					internalStartMap(((CurrentNetGameMapLookupFile) this.beginGameInformation.gameMapLookup).getFile()
							.getAbsolutePath(), this.beginGameInformation.localPlayerIndex);
				}
				else if (this.beginGameInformation.gameMapLookup instanceof CurrentNetGameMapLookupPath) {
					internalStartMap(((CurrentNetGameMapLookupPath) this.beginGameInformation.gameMapLookup).getPath(),
							this.beginGameInformation.localPlayerIndex);
				}
				else {
					throw new RuntimeException("Begin game information failed");
				}
				this.beginGameInformation.loadingStarted = true;
				return;
			}
			else {
				if (this.loadingMap != null) {
					final int localPlayerIndex = this.beginGameInformation.localPlayerIndex;
					try {
						if (this.loadingMap.activeMapLoader != null) {
							if (this.loadingMapDone || this.loadingMap.activeMapLoader.process()) {
								this.loadingMapDone = true;
								if (this.waitForTap && !this.continueTapped
										&& (this.beginGameInformation.hostInetAddress == null)) {
									showTapToContinue(deltaTime);
									return;
								}
								this.loadingMapDone = false;
								this.continueTapped = false;
								CPlayerUnitOrderListener uiOrderListener;
								final WarsmashClient warsmashClient;
								if (this.beginGameInformation.hostInetAddress != null) {

									try {
										final InetAddress byAddress = InetAddress
												.getByAddress(this.beginGameInformation.hostInetAddress);
										System.err.println("Connecting to address: " + byAddress);
										warsmashClient = new WarsmashClient(byAddress,
												this.beginGameInformation.hostUdpPort, this.loadingMap.viewer,
												this.beginGameInformation.sessionToken,
												this.beginGameInformation.serverSlotToMapSlot);
									}
									catch (final UnknownHostException e) {
										throw new RuntimeException(e);
									}
									catch (final IOException e) {
										throw new RuntimeException(e);
									}
									final WarsmashClientWriter warsmashClientWriter = warsmashClient.getWriter();
									warsmashClientWriter.joinGame();
									warsmashClientWriter.send();
									uiOrderListener = new WarsmashClientSendingOrderListener(warsmashClientWriter);
								}
								else {
									final War3MapViewer mapViewer = this.loadingMap.viewer;
									final CPlayerUnitOrderExecutor executor = new CPlayerUnitOrderExecutor(
											this.loadingMap.viewer.simulation, localPlayerIndex);
									final CPlayerUnitOrderListenerDelaying delayingListener = new CPlayerUnitOrderListenerDelaying(
											executor);
									uiOrderListener = delayingListener;
									warsmashClient = null;
									mapViewer.setGameTurnManager(new GameTurnManager() {
										@Override
										public void turnCompleted(final int gameTurnTick) {
											delayingListener.publishDelayedActions();
										}

										@Override
										public int getLatestCompletedTurn() {
											return Integer.MAX_VALUE;
										}

										@Override
										public void framesSkipped(final float skippedCount) {

										}
									});
								}

								// TODO not cast menu screen
								MenuUI.this.screenManager.setScreen(new WarsmashGdxMapScreen(this.loadingMap.viewer,
										this.screenManager, (WarsmashGdxMenuScreen) this.menuScreen, uiOrderListener));
								this.loadingMap = null;
								this.beginGameInformation = null;
								// The loading screen stays drawn until the game has drawn its first frame; hiding it now
								// would end this frame black while the game's first frames are still being prepared.
								if (warsmashClient != null) {
									warsmashClient.startThread();
								}
							}
							else {
								this.loadingBar.setFrameByRatio(this.loadingBarStart + ((1 - this.loadingBarStart)
										* this.loadingMap.activeMapLoader.getCompletionRatio()));
							}
						}
						else {
							this.loadingMap.activeMapLoader = this.loadingMap.viewer
									.createMapLoader(this.loadingMap.map, this.loadingMap.mapInfo, localPlayerIndex);
						}
					}
					catch (final IOException e) {
						throw new RuntimeException(e);
					}
					return;
				}
			}
		}
		if ((this.currentMusics != null) && (this.currentMusics.length > 0)) {
			if ((this.currentMusics[this.currentMusicIndex] != null)
					&& !this.currentMusics[this.currentMusicIndex].isPlaying()) {
				if (this.currentMusicRandomizeIndex) {
					this.currentMusicIndex = (int) (Math.random() * this.currentMusics.length);
				}
				else {
					this.currentMusicIndex = (this.currentMusicIndex + 1) % this.currentMusics.length;
				}
				this.currentMusics[this.currentMusicIndex].play();
			}
		}
		if ((this.focusUIFrame != null) && !this.focusUIFrame.isVisibleOnScreen()) {
			setFocusFrame(getNextFocusFrame());
		}

		final int baseMouseX = VirtualCursor.getX();
		int mouseX = baseMouseX;
		final int baseMouseY = VirtualCursor.getY();
		int mouseY = baseMouseY;
		final int minX = this.uiViewport.getScreenX();
		final int maxX = minX + this.uiViewport.getScreenWidth();
		final int minY = this.uiViewport.getScreenY();
		final int maxY = minY + this.uiViewport.getScreenHeight();

		mouseX = Math.max(minX, Math.min(maxX, mouseX));
		mouseY = Math.max(minY, Math.min(maxY, mouseY));
		if (Gdx.input.isCursorCatched()) {
			if (WarsmashConstants.CATCH_CURSOR) {
				Gdx.input.setCursorPosition(mouseX, mouseY);
			}
		}

		screenCoordsVector.set(mouseX, mouseY);
		this.uiViewport.unproject(screenCoordsVector);
		this.cursorFrame.setFramePointX(FramePoint.LEFT, screenCoordsVector.x);
		this.cursorFrame.setFramePointY(FramePoint.BOTTOM, screenCoordsVector.y);
		this.cursorFrame.setSequence("Normal");
		if (this.cursorFrame.isVisible() == VirtualCursor.isCursorHidden()) {
			this.cursorFrame.setVisible(!VirtualCursor.isCursorHidden());
		}

		// A hidden sprite's animation does not advance (the launcher hides the glue layers), so it never ends.
		if ((!this.glueSpriteLayerTopRight.isVisible() || this.glueSpriteLayerTopRight.isSequenceEnded())
				&& (!this.glueSpriteLayerTopLeft.isVisible() || this.glueSpriteLayerTopLeft.isSequenceEnded())
				&& (!this.campaignFade.isVisible() || this.campaignFade.isSequenceEnded())
				&& (!this.battleNetUI.getDoors().isVisible() || this.battleNetUI.getDoors().isSequenceEnded())) {
			switch (this.menuState) {
			case GOING_TO_MAIN_MENU:
				this.glueSpriteLayerTopLeft.setSequence("MainMenu Birth");
				this.glueSpriteLayerTopRight.setSequence("MainMenu Birth");
				if (this.battleNetUI.getDoors().isVisible()) {
					this.battleNetUI.getDoors().setVisible(false);
					this.battleNetUI.setVisible(false);
				}
				this.menuState = MenuState.MAIN_MENU;
				break;
			case MAIN_MENU:
				setMainMenuVisible(true);
				this.glueSpriteLayerTopLeft.setSequence("MainMenu Stand");
				this.glueSpriteLayerTopRight.setSequence("MainMenu Stand");
				break;
			case GOING_TO_BATTLE_NET_LOGIN:
				this.glueSpriteLayerTopLeft.setSequence("Death");
				this.glueSpriteLayerTopRight.setSequence("Death");
				MenuUI.this.battleNetUI.setVisible(true);
				final SpriteFrame doors = MenuUI.this.battleNetUI.getDoors();
				doors.setVisible(true);
				doors.setSequence(PrimaryTag.BIRTH);
				this.menuState = MenuState.GOING_TO_BATTLE_NET_LOGIN_PART2;
				break;
			case GOING_TO_BATTLE_NET_LOGIN_PART2:
				MenuUI.this.menuScreen.alternateModelToBattlenet();
				this.battleNetUI.showLoginPrompt(this.gamingNetworkConnection.getGatewayString());
				this.menuState = MenuState.BATTLE_NET_LOGIN;
				break;
			case LEAVING_BATTLE_NET_FROM_LOGGED_IN:
				MenuUI.this.menuScreen.unAlternateModelBackToNormal();
				this.glueSpriteLayerCenter.setVisible(false);
				playMusic(this.rootFrame.trySkinField("GlueMusic"), true, 0);
				// no break
			case LEAVING_BATTLE_NET:
				MenuUI.this.glueSpriteLayerTopLeft.setSequence("Birth");
				MenuUI.this.glueSpriteLayerTopRight.setSequence("Birth");
				this.menuState = MenuState.GOING_TO_MAIN_MENU;
				break;
			case GOING_TO_BATTLE_NET_WELCOME:
				MenuUI.this.glueSpriteLayerTopLeft.setSequence("BattleNetWelcome Birth");
				MenuUI.this.glueSpriteLayerTopRight.setSequence("BattleNetWelcome Birth");
				this.glueSpriteLayerCenter.setVisible(true);
				this.glueSpriteLayerCenter.setSequence("Birth");
				this.menuState = MenuState.BATTLE_NET_WELCOME;
				playMusic(this.rootFrame.trySkinField("ChatMusic"), true, 0);
				break;
			case BATTLE_NET_WELCOME:
				MenuUI.this.glueSpriteLayerTopLeft.setSequence("BattleNetWelcome Stand");
				MenuUI.this.glueSpriteLayerTopRight.setSequence("BattleNetWelcome Stand");
				this.battleNetUI.showWelcomeScreen();
				this.glueSpriteLayerCenter.setSequence("Stand");
				this.menuState = MenuState.BATTLE_NET_WELCOME;
				break;
			case GOING_TO_BATTLE_NET_CUSTOM_GAME_MENU:
				MenuUI.this.glueSpriteLayerTopLeft.setSequence("BattleNetCustom Birth");
				MenuUI.this.glueSpriteLayerTopRight.setSequence("BattleNetCustom Birth");
				this.menuState = MenuState.BATTLE_NET_CUSTOM_GAME_MENU;
				break;
			case BATTLE_NET_CUSTOM_GAME_MENU:
				this.battleNetUI.showCustomGameMenu();
				MenuUI.this.glueSpriteLayerTopLeft.setSequence("BattleNetCustom Stand");
				MenuUI.this.glueSpriteLayerTopRight.setSequence("BattleNetCustom Stand");
				break;
			case GOING_TO_BATTLE_NET_CREATE_CUSTOM_GAME_MENU:
				MenuUI.this.glueSpriteLayerTopLeft.setSequence("BattleNetCustomCreate Birth");
				MenuUI.this.glueSpriteLayerTopRight.setSequence("BattleNetCustomCreate Birth");
				this.menuState = MenuState.BATTLE_NET_CREATE_CUSTOM_GAME_MENU;
				break;
			case BATTLE_NET_CREATE_CUSTOM_GAME_MENU:
				this.battleNetUI.showCustomGameCreateMenu();
				MenuUI.this.glueSpriteLayerTopLeft.setSequence("BattleNetCustomCreate Stand");
				MenuUI.this.glueSpriteLayerTopRight.setSequence("BattleNetCustomCreate Stand");
				break;
			case GOING_TO_BATTLE_NET_CHANNEL_MENU:
				MenuUI.this.glueSpriteLayerTopLeft.setSequence("BattleNetChannel Birth");
				MenuUI.this.glueSpriteLayerTopRight.setSequence("BattleNetChannel Birth");
				this.menuState = MenuState.BATTLE_NET_CHANNEL_MENU;
				break;
			case BATTLE_NET_CHANNEL_MENU:
				this.battleNetUI.showChannelMenu();
				MenuUI.this.glueSpriteLayerTopLeft.setSequence("BattleNetChannel Stand");
				MenuUI.this.glueSpriteLayerTopRight.setSequence("BattleNetChannel Stand");
				break;
			case GOING_TO_BATTLE_NET_CHAT_CHANNEL_FROM_OUTSIDE:
				this.glueSpriteLayerCenter.setVisible(true);
				this.glueSpriteLayerCenter.setSequence("Birth");
			case GOING_TO_BATTLE_NET_CHAT_CHANNEL:
				MenuUI.this.glueSpriteLayerTopLeft.setSequence("BattleNetChatRoom Birth");
				MenuUI.this.glueSpriteLayerTopRight.setSequence("BattleNetChatRoom Birth");
				this.menuState = MenuState.BATTLE_NET_CHAT_CHANNEL;
				break;
			case BATTLE_NET_CHAT_CHANNEL:
				this.battleNetUI.showChatChannel();
				MenuUI.this.glueSpriteLayerTopLeft.setSequence("BattleNetChatRoom Stand");
				MenuUI.this.glueSpriteLayerTopRight.setSequence("BattleNetChatRoom Stand");
				break;
			case GOING_TO_BATTLE_NET_CUSTOM_GAME_LOBBY:
				MenuUI.this.glueSpriteLayerTopLeft.setSequence("MultiplayerPreGameChat Birth");
				MenuUI.this.glueSpriteLayerTopRight.setSequence("MultiplayerPreGameChat Birth");
				this.menuState = MenuState.BATTLE_NET_CUSTOM_GAME_LOBBY;
				break;
			case BATTLE_NET_CUSTOM_GAME_LOBBY:
				this.battleNetUI.showCustomGameLobby();
				MenuUI.this.glueSpriteLayerTopLeft.setSequence("MultiplayerPreGameChat Stand");
				MenuUI.this.glueSpriteLayerTopRight.setSequence("MultiplayerPreGameChat Stand");
				break;
			case GOING_TO_SINGLE_PLAYER:
				this.glueSpriteLayerTopLeft.setSequence("SinglePlayer Birth");
				this.glueSpriteLayerTopRight.setSequence("SinglePlayer Birth");
				this.menuState = MenuState.SINGLE_PLAYER;
				break;
			case GOING_TO_MAP:
				if ((this.beginGameInformation != null) && (this.beginGameInformation.sessionToken != 0)) {
					this.menuState = MenuState.BATTLE_NET_CUSTOM_GAME_LOBBY;
				}
				else {
					this.menuState = MenuState.SINGLE_PLAYER;
				}
				break;
			case LEAVING_CAMPAIGN:
				this.glueSpriteLayerTopLeft.setSequence("Birth");
				this.glueSpriteLayerTopRight.setSequence("Birth");
				if (this.campaignFade.isVisible()) {
					this.campaignFade.setSequence("Death");
				}
				this.glueScreenLoop.stop();
				this.glueScreenLoop = this.mainMenuGlueScreenLoop;
				this.glueScreenLoop.play(this.uiScene.audioContext, 0f, 0f, 0f);
				this.menuScreen.setModel(this.rootFrame.getSkinField("GlueSpriteLayerBackground"),
						this.menuFogSettings);
				this.rootFrame.setSpriteFrameModel(this.cursorFrame, this.rootFrame.getSkinField("Cursor"));
				this.menuState = MenuState.GOING_TO_SINGLE_PLAYER;
				break;
			case SINGLE_PLAYER:
				// This state is applied every frame, and the menu has to stay hidden behind the saved games.
				this.singlePlayerMenu.setVisible((this.savedGameHolder == null) || !this.savedGameHolder.isVisible());
				this.campaignFade.setVisible(false);
				setSinglePlayerButtonsEnabled(true);
				this.glueSpriteLayerTopLeft.setSequence("SinglePlayer Stand");
				this.glueSpriteLayerTopRight.setSequence("SinglePlayer Stand");
				break;
			case GOING_TO_SINGLE_PLAYER_SKIRMISH:
				this.glueSpriteLayerTopLeft.setSequence("SinglePlayerSkirmish Birth");
				this.glueSpriteLayerTopRight.setSequence("SinglePlayerSkirmish Birth");
				showSkirmishAdvancedOptions(false);
				this.skirmishMapList.selectInitial(
						Gdx.app.getPreferences("WarsmashWC3Engine").getString(SKIRMISH_MAP_PREFERENCE, null));
				this.menuState = MenuState.SINGLE_PLAYER_SKIRMISH;
				break;
			case SINGLE_PLAYER_SKIRMISH:
				this.skirmish.setVisible(true);
				this.glueSpriteLayerTopLeft.setSequence("SinglePlayerSkirmish Stand");
				this.glueSpriteLayerTopRight.setSequence("SinglePlayerSkirmish Stand");
				break;
			case GOING_TO_CAMPAIGN:
				this.glueSpriteLayerTopLeft.setSequence("Death");
				this.glueSpriteLayerTopRight.setSequence("Death");
				this.campaignMenu.setVisible(true);
				this.campaignFade.setVisible(true);
				this.campaignFade.setSequence("Birth");
				this.menuState = MenuState.GOING_TO_CAMPAIGN_PART2;
				break;
			case GOING_TO_CAMPAIGN_PART2: {
				final String currentCampaignBackgroundModel = getCurrentBackgroundModel();
				final String currentCampaignAmbientSound = this.rootFrame
						.trySkinField(this.currentCampaign.getAmbientSound());
				this.menuScreen.setModel(currentCampaignBackgroundModel,
						this.currentCampaign.getBackgroundFogSettings());
				this.glueScreenLoop.stop();
				this.glueScreenLoop = this.uiSounds.getSound(currentCampaignAmbientSound);
				this.glueScreenLoop.play(this.uiScene.audioContext, 0f, 0f, 0f);
				final DataTable skinData = this.rootFrame.getSkinData();
				final String cursorSkin = getRaceNameByCursorID(this.currentCampaign.getCursor());
				this.rootFrame.setSpriteFrameModel(this.cursorFrame, skinData.get(cursorSkin).getField("Cursor"));

				this.campaignFade.setSequence("Death");
				this.menuState = MenuState.CAMPAIGN;
				break;
			}
			case CAMPAIGN:
				this.campaignMenu.setVisible(true);
				this.campaignBackButton.setVisible(true);
				this.campaignWarcraftIIILogo.setVisible(true);
				this.campaignSelectFrame.setVisible(true);
				this.campaignRootMenuUI.setVisible(true);
				break;
			case GOING_TO_MISSION_SELECT: {
				final String currentCampaignBackgroundModel = getCurrentBackgroundModel();
				final String currentCampaignAmbientSound = this.rootFrame
						.trySkinField(this.currentCampaign.getAmbientSound());
				this.menuScreen.setModel(currentCampaignBackgroundModel,
						this.currentCampaign.getBackgroundFogSettings());
				this.glueScreenLoop.stop();
				this.glueScreenLoop = this.uiSounds.getSound(currentCampaignAmbientSound);
				this.glueScreenLoop.play(this.uiScene.audioContext, 0f, 0f, 0f);
				final DataTable skinData = this.rootFrame.getSkinData();
				final String cursorSkin = getRaceNameByCursorID(this.currentCampaign.getCursor());
				this.rootFrame.setSpriteFrameModel(this.cursorFrame, skinData.get(cursorSkin).getField("Cursor"));

				this.campaignFade.setSequence("Death");
				this.menuState = MenuState.MISSION_SELECT;
				break;
			}
			case MISSION_SELECT:
				this.campaignMenu.setVisible(true);
				this.campaignBackButton.setVisible(true);
				this.campaignWarcraftIIILogo.setVisible(true);
				this.currentMissionSelectMenuUI.setVisible(true);
				this.missionSelectFrame.setVisible(true);
				break;
			case GOING_TO_SINGLE_PLAYER_PROFILE:
				this.glueSpriteLayerTopLeft.setSequence("RealmSelection Birth");
				this.menuState = MenuState.SINGLE_PLAYER_PROFILE;
				break;
			case SINGLE_PLAYER_PROFILE:
				this.profilePanel.setVisible(true);
				setSinglePlayerButtonsEnabled(true);
				this.glueSpriteLayerTopLeft.setSequence("RealmSelection Stand");
				// TODO the below should probably be some generic focusing thing when we enter a
				// new view?
				if ((this.newProfileEditBox != null) && this.newProfileEditBox.isFocusable()) {
					setFocusFrame(this.newProfileEditBox);
				}
				break;
			case QUITTING:
				Gdx.app.exit();
				break;
			case RESTARTING:
				MenuUI.this.screenManager
						.setScreen(new WarsmashGdxMenuScreen(MenuUI.this.warsmashIni, this.screenManager));
				break;
			default:
				break;
			}
		}

	}

	public boolean isOverSlider(final int screenX, final int screenY) {
		screenCoordsVector.set(screenX, screenY);
		this.uiViewport.unproject(screenCoordsVector);
		return ScrollBarFrame.isSliderPart(this.rootFrame.getFrameChildUnderMouse(screenCoordsVector.x,
				screenCoordsVector.y));
	}

	public boolean scrolled(final int screenX, final int screenY, final float amountY) {
		screenCoordsVector.set(screenX, screenY);
		this.uiViewport.unproject(screenCoordsVector);
		final ListBoxFrame listBox = ListBoxFrame.findUnder(this.rootFrame, screenCoordsVector.x, screenCoordsVector.y);
		if (listBox == null) {
			return false;
		}
		listBox.scrollItems(this.rootFrame, this.uiViewport, Math.round(amountY));
		return true;
	}

	private FocusableFrame getNextFocusFrame() {
		return this.rootFrame.getNextFocusFrame();
	}

	public boolean touchDown(final int screenX, final int screenY, final float worldScreenY, final int button) {
		if (isWaitingForTap()) {
			return true;
		}
		screenCoordsVector.set(screenX, screenY);
		this.uiViewport.unproject(screenCoordsVector);
		final UIFrame clickedUIFrame = this.rootFrame.touchDown(screenCoordsVector.x, screenCoordsVector.y, button);
		closePopupMenusOutside(clickedUIFrame);
		if (this.optionsDialog != null) {
			this.optionsDialog.pressed(clickedUIFrame);
		}
		if (clickedUIFrame != null) {
			if (clickedUIFrame instanceof ClickableFrame) {
				this.mouseDownUIFrame = (ClickableFrame) clickedUIFrame;
				this.mouseDownUIFrame.mouseDown(this.rootFrame, this.uiViewport);
			}
			if (clickedUIFrame instanceof FocusableFrame) {
				final FocusableFrame clickedFocusableFrame = (FocusableFrame) clickedUIFrame;
				if (clickedFocusableFrame.isFocusable()) {
					if (clickedFocusableFrame == this.focusUIFrame) {
						// A tap on the box that has focus brings back an on-screen keyboard closed with Enter.
						clickedFocusableFrame.onFocusGained();
					}
					else {
						setFocusFrame(clickedFocusableFrame);
					}
				}
			}
		}
		return false;
	}

	private void closePopupMenusOutside(final UIFrame clickedUIFrame) {
		for (final UIFrame childFrame : this.rootFrame.getChildFrames()) {
			if ((childFrame instanceof MenuFrame) && childFrame.isVisible()
					&& (childFrame.getParent() instanceof ClickableFrame)
					&& !isSelfOrDescendant(clickedUIFrame, childFrame.getParent())) {
				childFrame.setVisible(false);
			}
		}
	}

	private static boolean isSelfOrDescendant(UIFrame frame, final UIFrame ancestor) {
		while (frame != null) {
			if (frame == ancestor) {
				return true;
			}
			frame = frame.getParent();
		}
		return false;
	}

	private void showOptions() {
		if (this.optionsDialog == null) {
			final SimpleFrame holder = (SimpleFrame) this.rootFrame.createSimpleFrame("SmashEscMenu", this.rootFrame, 0);
			holder.addAnchor(new AnchorDefinition(FramePoint.TOP, 0, GameUI.convertY(this.uiViewport, -0.05f)));
			final UIFrame backdrop = this.rootFrame.createFrame("EscMenuBackdrop", holder, 0, 0);
			holder.add(backdrop);
			this.rootFrame.add(holder);
			this.optionsDialog = new GameOptionsDialog(this.rootFrame, this.uiViewport, holder, backdrop,
					new GameOptionsDialog.Host() {
						@Override
						public void closed() {
							holder.setVisible(false);
							setMainMenuVisible(true);
						}

						@Override
						public void applied() {
							if (MenuUI.this.currentMusics != null) {
								for (final Music music : MenuUI.this.currentMusics) {
									if (music != null) {
										music.setVolume(CSoundMixer.getMusicOptionScale());
									}
								}
							}
						}
					});
		}
		setMainMenuVisible(false);
		this.optionsDialog.getHolder().setVisible(true);
		this.optionsDialog.show();
	}

	private void showSavedGames() {
		if (this.savedGameDialog == null) {
			final SimpleFrame holder = (SimpleFrame) this.rootFrame.createSimpleFrame("SmashEscMenu", this.rootFrame, 0);
			holder.addAnchor(new AnchorDefinition(FramePoint.TOP, 0, GameUI.convertY(this.uiViewport, -0.05f)));
			final UIFrame backdrop = this.rootFrame.createFrame("EscMenuBackdrop", holder, 0, 0);
			holder.add(backdrop);
			this.rootFrame.add(holder);
			this.savedGameDialog = new SavedGameDialog(this.rootFrame, this.uiViewport, holder, backdrop,
					new SavedGameDialog.Host() {
						@Override
						public void focusOn(final FocusableFrame frame) {
							setFocusFrame(frame);
						}

						@Override
						public void saveGame(final String name) {
						}

						@Override
						public SavedGame.Loadability loadability(final FileHandle file) {
							return SavedGame.loadability(file);
						}

						@Override
						public void loadGame(final String name) {
							holder.setVisible(false);
							startSavedGame(name);
						}

						@Override
						public void closed() {
							holder.setVisible(false);
							MenuUI.this.singlePlayerMenu.setVisible(true);
						}
					});
			this.savedGameHolder = holder;
		}
		this.singlePlayerMenu.setVisible(false);
		this.savedGameHolder.setVisible(true);
		this.savedGameDialog.showLoad();
	}

	/**
	 * Loads a saved game of the current profile, as Load Game in the saved games list does.
	 *
	 * @return false when the file cannot be read
	 */
	public boolean startSavedGame(final String name) {
		final FileHandle file = CSaveStorage.savedGame(name);
		try {
			final SavedGame.Header header = SavedGame.readHeader(file);
			SavedGame.setPendingLoad(file);
			this.singlePlayerMenu.setVisible(false);
			startMap(header.mapFile);
			return true;
		}
		catch (final IOException exc) {
			exc.printStackTrace();
			this.singlePlayerMenu.setVisible(true);
			return false;
		}
	}

	private void setFocusFrame(final FocusableFrame clickedFocusableFrame) {
		// The profile view asks for its edit box again while it stays open; refocusing would reopen the
		// on-screen keyboard every time.
		if (clickedFocusableFrame == this.focusUIFrame) {
			return;
		}
		if (this.focusUIFrame != null) {
			this.focusUIFrame.onFocusLost();
		}
		this.focusUIFrame = clickedFocusableFrame;
		if (this.focusUIFrame != null) {
			this.focusUIFrame.onFocusGained();
		}
	}

	public boolean touchUp(final int screenX, final int screenY, final float worldScreenY, final int button) {
		// The game starts on the release, so that no half of the tap reaches it.
		if (continueAfterLoading()) {
			return true;
		}
		screenCoordsVector.set(screenX, screenY);
		this.uiViewport.unproject(screenCoordsVector);
		final UIFrame clickedUIFrame = this.rootFrame.touchUp(screenCoordsVector.x, screenCoordsVector.y, button);
		if (this.mouseDownUIFrame != null) {
			if (clickedUIFrame == this.mouseDownUIFrame) {
				this.mouseDownUIFrame.onClick(button);
				final String soundKey = this.mouseDownUIFrame.getSoundKey();
				if (soundKey != null) {
					this.uiSounds.getSound(soundKey).play(this.uiScene.audioContext, 0, 0, 0);
				}
			}
			this.mouseDownUIFrame.mouseUp(this.rootFrame, this.uiViewport);
		}
		this.mouseDownUIFrame = null;
		return false;
	}

	public boolean touchDragged(final int screenX, final int screenY, final float worldScreenY, final int pointer) {
		screenCoordsVector.set(screenX, screenY);
		this.uiViewport.unproject(screenCoordsVector);
		if (this.mouseDownUIFrame != null) {
			this.mouseDownUIFrame.mouseDragged(this.rootFrame, this.uiViewport, screenCoordsVector.x,
					screenCoordsVector.y);
		}
		return false;
	}

	public boolean mouseMoved(final int screenX, final int screenY, final float worldScreenY) {
		screenCoordsVector.set(screenX, screenY);
		this.uiViewport.unproject(screenCoordsVector);
		final UIFrame mousedUIFrame = this.rootFrame.getFrameChildUnderMouse(screenCoordsVector.x,
				screenCoordsVector.y);
		if (mousedUIFrame != this.mouseOverUIFrame) {
			if (this.mouseOverUIFrame != null) {
				this.mouseOverUIFrame.mouseExit(this.rootFrame, this.uiViewport);
			}
			if (mousedUIFrame instanceof ClickableFrame) {
				this.mouseOverUIFrame = (ClickableFrame) mousedUIFrame;
				this.mouseOverUIFrame.mouseEnter(this.rootFrame, this.uiViewport);
			}
			else {
				this.mouseOverUIFrame = null;
			}
		}
		return false;
	}

	private void loadSounds() {
		this.worldEditStrings = new WorldEditStrings(this.dataSource);
		this.uiSoundsTable = new DataTable(this.worldEditStrings);
		try {
			try (InputStream miscDataTxtStream = this.dataSource.getResourceAsStream("UI\\SoundInfo\\UISounds.slk")) {
				this.uiSoundsTable.readSLK(miscDataTxtStream);
			}
			try (InputStream miscDataTxtStream = this.dataSource
					.getResourceAsStream("UI\\SoundInfo\\AmbienceSounds.slk")) {
				this.uiSoundsTable.readSLK(miscDataTxtStream);
			}
		}
		catch (final IOException e) {
			e.printStackTrace();
		}
		this.uiSounds = new KeyedSounds(this.uiSoundsTable, this.dataSource);
	}

	public KeyedSounds getUiSounds() {
		return this.uiSounds;
	}

	private static final class PlayerSlotPaneListenerImplementation implements PlayerSlotPaneListener {
		private final TeamSetupPane teamSetupPane;
		private final War3MapConfig war3MapConfig;
		private final War3MapW3i mapInfo;
		private final GameUI rootFrame;
		private final Viewport uiViewport;

		private PlayerSlotPaneListenerImplementation(final TeamSetupPane teamSetupPane,
				final War3MapConfig war3MapConfig, final War3MapW3i mapInfo, final GameUI rootFrame,
				final Viewport uiViewport) {
			this.teamSetupPane = teamSetupPane;
			this.war3MapConfig = war3MapConfig;
			this.mapInfo = mapInfo;
			this.rootFrame = rootFrame;
			this.uiViewport = uiViewport;
		}

		@Override
		public void setPlayerSlot(final int index, final LobbyPlayerType lobbyPlayerType) {
			final CBasePlayer player = this.war3MapConfig.getPlayer(index);
			switch (lobbyPlayerType) {
			case OPEN:
				player.setController(CMapControl.NONE);
				player.setSlotState(CPlayerSlotState.EMPTY);
				player.setAIDifficulty(null);
				break;
			case CLOSED:
				player.setController(CMapControl.NONE);
				player.setSlotState(CPlayerSlotState.PLAYING);
				player.setAIDifficulty(null);
				break;
			case COMPUTER_NEWBIE:
				player.setController(CMapControl.COMPUTER);
				player.setSlotState(CPlayerSlotState.PLAYING);
				player.setAIDifficulty(AIDifficulty.NEWBIE);
				break;
			case COMPUTER_NORMAL:
				player.setController(CMapControl.COMPUTER);
				player.setSlotState(CPlayerSlotState.PLAYING);
				player.setAIDifficulty(AIDifficulty.NORMAL);
				break;
			case COMPUTER_INSANE:
				player.setController(CMapControl.COMPUTER);
				player.setSlotState(CPlayerSlotState.PLAYING);
				player.setAIDifficulty(AIDifficulty.INSANE);
				break;
			case USER:
				player.setController(CMapControl.USER);
				player.setSlotState(CPlayerSlotState.PLAYING);
				player.setAIDifficulty(null);
				break;
			}
			this.teamSetupPane.notifyPlayerDataUpdated(index, this.rootFrame, this.uiViewport, this.war3MapConfig,
					this.mapInfo);
		}

		@Override
		public void setPlayerRace(final int index, final int raceItemIndex) {
			final CBasePlayer player = this.war3MapConfig.getPlayer(index);
			if (raceItemIndex == 0) {
				player.setRacePref(WarsmashConstants.RACE_MANAGER.getRandomRacePreference());
			}
			else {
				final CRace race = WarsmashConstants.RACE_MANAGER.getRace(raceItemIndex);
				final CRacePreference racePreference = WarsmashConstants.RACE_MANAGER.getRacePreferenceForRace(race);
				player.setRacePref(racePreference);
			}
			this.teamSetupPane.notifyPlayerDataUpdated(index, this.rootFrame, this.uiViewport, this.war3MapConfig,
					this.mapInfo);
		}

		@Override
		public void setPlayerTeam(final int index, final int team) {
			this.war3MapConfig.getPlayer(index).setTeam(team);
			this.teamSetupPane.notifyPlayerDataUpdated(index, this.rootFrame, this.uiViewport, this.war3MapConfig,
					this.mapInfo);
		}

		@Override
		public void setPlayerHandicap(final int index, final float handicap) {
			this.war3MapConfig.getPlayer(index).setHandicap(handicap);
			this.teamSetupPane.notifyPlayerDataUpdated(index, this.rootFrame, this.uiViewport, this.war3MapConfig,
					this.mapInfo);
		}

		@Override
		public void setPlayerColor(final int index, final int color) {
			this.war3MapConfig.getPlayer(index).setColor(color);
			this.teamSetupPane.notifyPlayerDataUpdated(index, this.rootFrame, this.uiViewport, this.war3MapConfig,
					this.mapInfo);
		}
	}

	private static enum MenuState {
		GOING_TO_MAIN_MENU,
		MAIN_MENU,
		GOING_TO_BATTLE_NET_LOGIN,
		GOING_TO_BATTLE_NET_LOGIN_PART2,
		BATTLE_NET_LOGIN,
		LEAVING_BATTLE_NET,
		LEAVING_BATTLE_NET_FROM_LOGGED_IN,
		GOING_TO_BATTLE_NET_CUSTOM_GAME_MENU,
		BATTLE_NET_CUSTOM_GAME_MENU,
		GOING_TO_BATTLE_NET_CREATE_CUSTOM_GAME_MENU,
		BATTLE_NET_CREATE_CUSTOM_GAME_MENU,
		GOING_TO_BATTLE_NET_CHANNEL_MENU,
		BATTLE_NET_CHANNEL_MENU,
		GOING_TO_BATTLE_NET_WELCOME,
		BATTLE_NET_WELCOME,
		GOING_TO_SINGLE_PLAYER,
		LEAVING_CAMPAIGN,
		SINGLE_PLAYER,
		GOING_TO_SINGLE_PLAYER_SKIRMISH,
		SINGLE_PLAYER_SKIRMISH,
		GOING_TO_MAP,
		GOING_TO_CAMPAIGN,
		GOING_TO_CAMPAIGN_PART2,
		GOING_TO_MISSION_SELECT,
		MISSION_SELECT,
		CAMPAIGN,
		GOING_TO_SINGLE_PLAYER_PROFILE,
		SINGLE_PLAYER_PROFILE,
		GOING_TO_LOADING_SCREEN,
		QUITTING,
		RESTARTING,
		GOING_TO_BATTLE_NET_CHAT_CHANNEL,
		GOING_TO_BATTLE_NET_CHAT_CHANNEL_FROM_OUTSIDE,
		BATTLE_NET_CHAT_CHANNEL,
		GOING_TO_BATTLE_NET_CUSTOM_GAME_LOBBY,
		BATTLE_NET_CUSTOM_GAME_LOBBY;
	}

	public void hide() {
		this.glueScreenLoop.stop();
		stopMusic();
	}

	public void dispose() {
		if (this.rootFrame != null) {
			this.rootFrame.dispose();
		}
	}

	public boolean keyDown(final int keycode) {
		if (isWaitingForTap()) {
			return true;
		}
		if ((this.optionsDialog != null) && this.optionsDialog.isShowing()) {
			this.optionsDialog.keyDown(keycode);
			return true;
		}
		if (this.focusUIFrame != null) {
			this.focusUIFrame.keyDown(keycode);
		}
		return false;
	}

	public boolean keyUp(final int keycode) {
		if (continueAfterLoading()) {
			return true;
		}
		if (keycode == Input.Keys.TAB) {
			// accessibility tab focus ui
			final List<FocusableFrame> focusableFrames = this.rootFrame.getFocusableFrames();
			final int indexOf = focusableFrames.indexOf(this.focusUIFrame) + 1;
			for (int i = indexOf; i < focusableFrames.size(); i++) {
				final FocusableFrame focusableFrame = focusableFrames.get(i);
				if (focusableFrame.isVisibleOnScreen() && focusableFrame.isFocusable()) {
					setFocusFrame(focusableFrame);
					break;
				}
			}
		}
		else {
			if (this.focusUIFrame != null) {
				this.focusUIFrame.keyUp(keycode);
			}
		}
		return false;
	}

	public boolean keyTyped(final char character) {
		if (this.focusUIFrame != null) {
			this.focusUIFrame.keyTyped(character);
		}
		if ((character == 'z') && Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT)) {
			this.hideUI = !this.hideUI;
			this.uiScene.show = !this.uiScene.show;
		}
		return false;
	}

	public void onReturnFromGame() {
//		MenuUI.this.campaignMenu.setVisible(true);
//		MenuUI.this.campaignBackButton.setVisible(true);
//		MenuUI.this.missionSelectFrame.setVisible(true);
//		MenuUI.this.campaignSelectFrame.setVisible(false);
//		MenuUI.this.campaignWarcraftIIILogo.setVisible(true);
//		MenuUI.this.campaignRootMenuUI.setVisible(false);
//		MenuUI.this.currentMissionSelectMenuUI.setVisible(true);
		this.loadingBar.setVisible(false);
		this.loadingFrame.setVisible(false);
		this.loadingBackground.setVisible(false);
		final SavedGame.Loadability loadFailure = SavedGame.consumeLoadFailure();
		if (loadFailure != null) {
			this.dialog.showError(SavedGameDialog.reasonKey(loadFailure), null);
		}
		final boolean forceCampaignSelect = CCampaignProgress.consumeCampaignSelectScreenForced();
		switch (this.menuState) {
		default:
		case GOING_TO_MAIN_MENU:
		case MAIN_MENU:
			this.glueScreenLoop.stop();
			this.glueScreenLoop = this.mainMenuGlueScreenLoop;
			this.glueScreenLoop.play(this.uiScene.audioContext, 0f, 0f, 0f);
			this.menuScreen.setModel(this.rootFrame.getSkinField("GlueSpriteLayerBackground"), this.menuFogSettings);
			this.rootFrame.setSpriteFrameModel(this.cursorFrame, this.rootFrame.getSkinField("Cursor"));
			break;
		case CAMPAIGN:
		case MISSION_SELECT:
			// The mission may have opened missions or campaigns and picked the campaign the menu shows.
			this.currentMissionSelectMenuUI.setVisible(false);
			refreshCampaignAvailability();
			if (forceCampaignSelect) {
				this.missionSelectFrame.setVisible(false);
				this.menuState = MenuState.CAMPAIGN;
			}
			this.rootFrame.setDecoratedText((StringFrame) this.rootFrame.getFrameByName("MissionName", 0),
					this.currentCampaign.getName());
			this.rootFrame.setDecoratedText((StringFrame) this.rootFrame.getFrameByName("MissionNameHeader", 0),
					this.currentCampaign.getHeader());
			final String currentCampaignBackgroundModel = getCurrentBackgroundModel();
			final String currentCampaignAmbientSound = this.rootFrame
					.trySkinField(this.currentCampaign.getAmbientSound());
			this.menuScreen.setModel(currentCampaignBackgroundModel, this.currentCampaign.getBackgroundFogSettings());
			this.glueScreenLoop.stop();
			this.glueScreenLoop = this.uiSounds.getSound(currentCampaignAmbientSound);
			this.glueScreenLoop.play(this.uiScene.audioContext, 0f, 0f, 0f);
			final DataTable skinData = this.rootFrame.getSkinData();
			final String cursorSkin = getRaceNameByCursorID(this.currentCampaign.getCursor());
			this.rootFrame.setSpriteFrameModel(this.cursorFrame, skinData.get(cursorSkin).getField("Cursor"));
			break;
		case BATTLE_NET_CUSTOM_GAME_LOBBY: {
			this.menuScreen.setModel(this.rootFrame.getSkinField("GlueSpriteLayerBackground"), this.menuFogSettings);
			MenuUI.this.menuScreen.alternateModelToBattlenet();
			this.rootFrame.setSpriteFrameModel(this.cursorFrame, this.rootFrame.getSkinField("Cursor"));
			requestEnterDefaultChat();
			break;
		}
		}
//		MenuUI.this.campaignFade.setSequence("Death");
//		this.campaignFade.setVisible(true);
//		this.menuState = MenuState.MISSION_SELECT;
	}

	private String getRaceNameByCursorID(final int cursorId) {
		final CRaceManagerEntry raceEntry = WarsmashConstants.RACE_MANAGER.get(getRaceByCursorID(cursorId));
		return raceEntry.getKey();
	}

	private CRace getRaceByCursorID(final int cursorId) {
		final int raceId = cursorId + 1;
		final CRace race = WarsmashConstants.RACE_MANAGER.getRace(raceId);
		if (race == null) {
			return WarsmashConstants.RACE_MANAGER.getRace(1); // when in doubt, default to human
		}
		return race;
	}

	private String getCurrentBackgroundModel() {
		final String background = this.currentCampaign.getBackground();
		final String versionedBackground = background;
		if (this.rootFrame.hasSkinField(versionedBackground)) {
			return this.rootFrame.getSkinField(versionedBackground);
		}
		return this.rootFrame.getSkinField(background);
	}

	private static final class LoadingMap {

		private final War3MapViewer viewer;
		private final War3Map map;
		private final War3MapW3i mapInfo;

		private MapLoader activeMapLoader = null;

		public LoadingMap(final War3MapViewer viewer, final War3Map map, final War3MapW3i mapInfo) {
			this.viewer = viewer;
			this.map = map;
			this.mapInfo = mapInfo;
		}

	}

	private void stopMusic() {
		if (this.currentMusics != null) {
			for (final Music music : this.currentMusics) {
				if (music != null) {
					music.stop();
				}
			}
			this.currentMusics = null;
		}
	}

	public Music playMusic(final String musicField, final boolean random, int index) {
		if (WarsmashConstants.ENABLE_MUSIC) {
			stopMusic();

			final String[] semicolonMusics = musicField.split(";");
			final List<String> musicPaths = new ArrayList<>();
			for (String musicPath : semicolonMusics) {
				// dumb support for comma as well as semicolon, I wonder if we can
				// clean this up, simplify?
				if (this.musicSLK.get(musicPath) != null) {
					musicPath = this.musicSLK.get(musicPath).getField("FileNames");
				}
				final String[] moreSplitMusics = musicPath.split(",");
				for (final String finalSplitPath : moreSplitMusics) {
					musicPaths.add(finalSplitPath);
				}
			}
			final String[] musics = musicPaths.toArray(new String[musicPaths.size()]);

			this.currentMusics = new Music[musics.length];
			int validMusicCount = 0;
			for (int i = 0; i < musics.length; i++) {
				final String path = MusicPlayerLibGDX.findMusicFile(this.viewer.dataSource, musics[i]);
				if (path != null) {
					final Music newMusic = Extensions.audio.newMusic(new DataSourceFileHandle(this.viewer.dataSource, path));
					newMusic.setVolume(CSoundMixer.getMusicOptionScale());
					this.currentMusics[i] = newMusic;
					validMusicCount++;
				}
			}
			if (this.currentMusics.length != validMusicCount) {
				final Music[] fixedList = new Music[validMusicCount];
				int fixedListIndex = 0;
				for (int i = 0; i < this.currentMusics.length; i++) {
					if (this.currentMusics[i] != null) {
						fixedList[fixedListIndex++] = this.currentMusics[i];
					}
				}
				this.currentMusics = fixedList;
			}
			if (this.currentMusics.length == 0) {
				return null;
			}
			if (random) {
				index = (int) (Math.random() * this.currentMusics.length);
			}
			index = Math.min(index, this.currentMusics.length - 1);
			this.currentMusicIndex = index;
			this.currentMusicRandomizeIndex = random;
			if (this.currentMusics[index] != null) {
				this.currentMusics[index].play();
			}
		}
		return null;
	}

	public void playCurrentBattleNetGlueSpriteDeath() {
		switch (MenuUI.this.menuState) {
		case BATTLE_NET_LOGIN:
		case GOING_TO_BATTLE_NET_LOGIN:
		case GOING_TO_BATTLE_NET_LOGIN_PART2:
			this.battleNetUI.getDoors().setSequence(PrimaryTag.DEATH);
			break;
		case BATTLE_NET_CHAT_CHANNEL:
		case GOING_TO_BATTLE_NET_CHAT_CHANNEL:
			MenuUI.this.glueSpriteLayerTopLeft.setSequence("BattleNetChatRoom Death");
			MenuUI.this.glueSpriteLayerTopRight.setSequence("BattleNetChatRoom Death");
			break;
		case BATTLE_NET_CUSTOM_GAME_MENU:
		case GOING_TO_BATTLE_NET_CUSTOM_GAME_MENU:
			MenuUI.this.glueSpriteLayerTopLeft.setSequence("BattleNetCustom Death");
			MenuUI.this.glueSpriteLayerTopRight.setSequence("BattleNetCustom Death");
			break;
		case BATTLE_NET_CREATE_CUSTOM_GAME_MENU:
		case GOING_TO_BATTLE_NET_CREATE_CUSTOM_GAME_MENU:
			MenuUI.this.glueSpriteLayerTopLeft.setSequence("BattleNetCustomCreate Death");
			MenuUI.this.glueSpriteLayerTopRight.setSequence("BattleNetCustomCreate Death");
			break;
		case BATTLE_NET_CHANNEL_MENU:
		case GOING_TO_BATTLE_NET_CHANNEL_MENU:
			MenuUI.this.glueSpriteLayerTopLeft.setSequence("BattleNetChannel Death");
			MenuUI.this.glueSpriteLayerTopRight.setSequence("BattleNetChannel Death");
			break;
		case BATTLE_NET_CUSTOM_GAME_LOBBY:
		case GOING_TO_BATTLE_NET_CUSTOM_GAME_LOBBY:
			MenuUI.this.glueSpriteLayerTopLeft.setSequence("MultiplayerPreGameChat Death");
			MenuUI.this.glueSpriteLayerTopRight.setSequence("MultiplayerPreGameChat Death");
			break;
		default:
		case BATTLE_NET_WELCOME:
		case GOING_TO_BATTLE_NET_WELCOME:
			MenuUI.this.glueSpriteLayerTopLeft.setSequence("BattleNetWelcome Death");
			MenuUI.this.glueSpriteLayerTopRight.setSequence("BattleNetWelcome Death");
			break;
		}
		if (this.gameListQueryTask != null) {
			this.gameListQueryTask.cancel();
			this.gameListQueryTask = null;
		}
	}

	private boolean isInsideTopBarMode() {
		boolean insideTopBarMode = false;
		switch (MenuUI.this.menuState) {
		case GOING_TO_BATTLE_NET_CHANNEL_MENU:
		case BATTLE_NET_CHANNEL_MENU:
		case GOING_TO_BATTLE_NET_CHAT_CHANNEL:
		case GOING_TO_BATTLE_NET_CHAT_CHANNEL_FROM_OUTSIDE:
		case BATTLE_NET_CHAT_CHANNEL:
		case GOING_TO_BATTLE_NET_WELCOME:
		case BATTLE_NET_WELCOME:
			insideTopBarMode = true;
			break;
		}
		return insideTopBarMode;
	}

	private void requestEnterDefaultChat() {
		MenuUI.this.gamingNetworkConnection.joinChannel(MenuUI.this.battleNetUI.getGamingNetworkSessionToken(),
				"Frozen Throne USA-1"); // TODO
										// maybe
										// not
										// hardcode
										// this
	}
}
