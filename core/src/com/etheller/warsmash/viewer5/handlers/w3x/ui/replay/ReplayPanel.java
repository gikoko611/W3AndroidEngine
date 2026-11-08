package com.etheller.warsmash.viewer5.handlers.w3x.ui.replay;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.etheller.warsmash.input.touch.TouchInputRouter;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.parsers.fdf.datamodel.FramePoint;
import com.etheller.warsmash.parsers.fdf.datamodel.MenuItem;
import com.etheller.warsmash.parsers.fdf.frames.GlueTextButtonFrame;
import com.etheller.warsmash.parsers.fdf.frames.MenuFrame;
import com.etheller.warsmash.parsers.fdf.frames.PopupMenuFrame;
import com.etheller.warsmash.parsers.fdf.frames.SetPoint;
import com.etheller.warsmash.parsers.fdf.frames.SimpleCheckBoxFrame;
import com.etheller.warsmash.parsers.fdf.frames.SimpleFrame;
import com.etheller.warsmash.parsers.fdf.frames.SimpleStatusBarFrame;
import com.etheller.warsmash.parsers.fdf.frames.SpriteFrame;
import com.etheller.warsmash.parsers.fdf.frames.StringFrame;
import com.etheller.warsmash.parsers.fdf.frames.TextureFrame;
import com.etheller.warsmash.parsers.fdf.frames.UIFrame;
import com.etheller.warsmash.util.ImageUtils;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.camera.GameCameraManager;
import com.etheller.warsmash.viewer5.handlers.w3x.replay.ReplayPlayback;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CMapControl;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.CommandCardIcon;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.MeleeChat;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.command.CommandCardCommandListener;

/**
 * The replay panel of a replay's game, the game's own SimpleReplayPanelV1 in the command card's place, as measured on
 * 1.29: Replay Vision (a player, or all players), the elapsed time at the speed, the time bar, Fog of War and Auto
 * Camera, and Pause, Speed Up, Speed Down and Restart in the card's bottom row. Nothing here reaches the replay's
 * game: the pause is the viewer's own, the speed only plays its steps faster.
 */
public final class ReplayPanel implements CommandCardCommandListener {
	public interface Host {
		/** Plays the same replay again from its start. */
		void restart();

		/** The player whose resources the resource bar shows changed. */
		void viewingPlayerChanged();
	}

	private static final String PANEL = "SimpleReplayPanelV1";
	private static final int ACTION_PAUSE = 1;
	private static final int ACTION_SPEED_UP = 2;
	private static final int ACTION_SPEED_DOWN = 3;
	private static final int ACTION_RESTART = 4;
	/** 1/2X is the slowest (measured); faster goes by whole steps. */
	private static final float[] SPEEDS = { 0.5f, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
	private static final int NORMAL_SPEED = 1;
	/** A finger's size on the panel's small controls, as the touch design asks (96 px of a 1848 px tall screen). */
	private static final float TOUCH_HEIGHT = 0.031f;
	private static final float AUTO_CAMERA_INTERVAL = 3f;
	private static final float AUTO_CAMERA_PAN_TIME = 0.75f;
	private static final float AUTO_CAMERA_MIN_DISTANCE = 512f;

	private final GameUI rootFrame;
	private final Viewport uiViewport;
	private final War3MapViewer viewer;
	private final ReplayPlayback playback;
	private final GameCameraManager camera;
	private final Host host;
	private final SimpleFrame holder;
	private final StringFrame elapsedTime;
	private final StringFrame speedMultiplier;
	private final UIFrame speedLabel;
	private final SimpleStatusBarFrame statusBar;
	private final PopupMenuFrame visionMenu;
	private final StringFrame visionTitle;
	private final SimpleCheckBoxFrame fogCheckBox;
	private final SimpleCheckBoxFrame cameraCheckBox;
	private final CommandCardIcon[] buttons = new CommandCardIcon[4];
	/** The menu's entries: a player index or {@link War3MapViewer#REPLAY_VISION_ALL_PLAYERS}. */
	private final List<Integer> visionChoices = new ArrayList<>();
	private final List<String> visionNames = new ArrayList<>();
	private int speedIndex = NORMAL_SPEED;
	private boolean paused;
	private int shownSecond = -1;
	private float autoCameraTime;
	private final Map<CUnit, Float> lastLife = new IdentityHashMap<>();

	/**
	 * @param commandCard the command card's buttons: the panel covers the card, its buttons take the bottom row's
	 *                    places
	 */
	public static ReplayPanel create(final GameUI rootFrame, final Viewport uiViewport, final War3MapViewer viewer,
			final ReplayPlayback playback, final GameCameraManager camera, final CommandCardIcon[][] commandCard,
			final Host host) {
		if (rootFrame.getTemplates().getFrame(PANEL) == null) {
			System.err.println("Replay: the game data has no " + PANEL + ", the replay is shown without its panel");
			return null;
		}
		return new ReplayPanel(rootFrame, uiViewport, viewer, playback, camera, commandCard, host);
	}

	private ReplayPanel(final GameUI rootFrame, final Viewport uiViewport, final War3MapViewer viewer,
			final ReplayPlayback playback, final GameCameraManager camera, final CommandCardIcon[][] commandCard,
			final Host host) {
		this.rootFrame = rootFrame;
		this.uiViewport = uiViewport;
		this.viewer = viewer;
		this.playback = playback;
		this.camera = camera;
		this.host = host;
		final CommandCardIcon topLeft = commandCard[0][0];
		final CommandCardIcon bottomRight = commandCard[commandCard.length - 1][commandCard[0].length - 1];
		this.holder = (SimpleFrame) rootFrame.createFrameByType("SIMPLEFRAME", "SmashReplayPanel", rootFrame, "", 0);
		this.holder.addSetPoint(new SetPoint(FramePoint.TOPLEFT, topLeft, FramePoint.TOPLEFT, 0, 0));
		this.holder.addSetPoint(new SetPoint(FramePoint.BOTTOMRIGHT, bottomRight, FramePoint.BOTTOMRIGHT, 0, 0));
		rootFrame.createSimpleFrame(PANEL, this.holder, 0);

		this.elapsedTime = (StringFrame) rootFrame.getFrameByName("ReplayElapsedTime", 0);
		this.speedMultiplier = (StringFrame) rootFrame.getFrameByName("ReplaySpeedMultiplier", 0);
		this.speedLabel = rootFrame.getFrameByName("ReplaySpeedLabel", 0);
		this.statusBar = (SimpleStatusBarFrame) rootFrame.getFrameByName("ReplayStatusBar", 0);
		this.visionMenu = (PopupMenuFrame) rootFrame.getFrameByName("ReplayVisionMenu", 0);
		this.visionTitle = (StringFrame) ((GlueTextButtonFrame) this.visionMenu.getPopupTitleFrame()).getButtonText();
		this.fogCheckBox = (SimpleCheckBoxFrame) rootFrame.getFrameByName("ReplayFogCheckBox", 0);
		this.cameraCheckBox = (SimpleCheckBoxFrame) rootFrame.getFrameByName("ReplayCameraCheckBox", 0);
		final boolean touch = TouchInputRouter.isActive();
		final float touchHeight = touch ? GameUI.convertY(uiViewport, TOUCH_HEIGHT) : 0;
		this.fogCheckBox.setTouchArea(rootFrame.getFrameByName("ReplayFogString", 0), touchHeight);
		this.cameraCheckBox.setTouchArea(rootFrame.getFrameByName("ReplayCameraString", 0), touchHeight);
		this.fogCheckBox.setChecked(true);
		this.fogCheckBox.setOnClick(this::visionChanged);
		this.cameraCheckBox.setOnClick(() -> this.autoCameraTime = AUTO_CAMERA_INTERVAL);

		fillVisionMenu(touch);
		this.visionMenu.setMenuClickListener((button, index) -> chooseVision(index));

		final String[][] buttonArt = { { "ReplayPauseIcon", "REPLAY_TOOLTIP_PAUSE" },
			{ "ReplaySpeedUpIcon", "REPLAY_TOOLTIP_SPEED_UP" }, { "ReplaySpeedDownIcon", "REPLAY_TOOLTIP_SPEED_DOWN" },
			{ "ReplayRestartIcon", "REPLAY_TOOLTIP_RESTART" } };
		final int bottomRow = commandCard.length - 1;
		for (int i = 0; i < this.buttons.length; i++) {
			this.buttons[i] = button(i, commandCard[bottomRow][i], buttonArt[i][0], tip(buttonArt[i][1], touch));
		}
		this.viewer.setReplayVision(this.visionChoices.get(defaultVisionIndex()), true);
		this.rootFrame.setText(this.visionTitle, this.visionNames.get(defaultVisionIndex()));
		showSpeed();
		update(0);
	}

	private CommandCardIcon button(final int index, final CommandCardIcon place, final String iconKey,
			final String tip) {
		final String name = "SmashReplayButton_" + index;
		final CommandCardIcon button = new CommandCardIcon(name, this.holder, this);
		this.holder.add(button);
		button.addSetPoint(new SetPoint(FramePoint.CENTER, place, FramePoint.CENTER, 0, 0));
		button.setWidth(place.getAssignedWidth());
		button.setHeight(place.getAssignedHeight());
		final TextureFrame icon = new TextureFrame(name + "_Icon", this.rootFrame, false, null);
		icon.addSetPoint(new SetPoint(FramePoint.CENTER, button, FramePoint.CENTER, 0, 0));
		icon.setWidth(place.getAssignedWidth());
		icon.setHeight(place.getAssignedHeight());
		final SpriteFrame cooldown = (SpriteFrame) this.rootFrame.createFrameByType("SPRITE", name + "_Cooldown",
				this.rootFrame, "", 0);
		button.set(icon, null, cooldown, null, null, null);
		button.clear();
		Texture texture = null;
		if (this.rootFrame.hasSkinField(iconKey)) {
			texture = ImageUtils.getAnyExtensionTexture(this.viewer.dataSource, this.rootFrame.getSkinField(iconKey));
		}
		if (texture == null) {
			texture = ImageUtils.getAnyExtensionTexture(this.viewer.dataSource, ImageUtils.DEFAULT_ICON_PATH);
		}
		button.setCommandButtonData(texture, 0, index + 1, 0, false, false, false, tip, "", '\0', 0, 0, 0, 0,
				false, 0, 0, -1);
		return button;
	}

	/** A touch screen has no keys, so the key in the tip is left out there. */
	private String tip(final String key, final boolean touch) {
		final String tip = this.rootFrame.getTemplates().getDecoratedString(key);
		return touch ? tip.replaceAll("\\s*\\((?i:\\|c[0-9a-f]{8})[^|]*\\|r\\)", "") : tip;
	}

	private void fillVisionMenu(final boolean touch) {
		final CSimulation game = this.viewer.simulation;
		int people = 0;
		for (int i = 0; i < WarsmashConstants.MAX_PLAYERS; i++) {
			final CPlayer player = game.getPlayer(i);
			if (this.viewer.isReplayVisionPlayer(player) && (player.getController() == CMapControl.USER)) {
				people++;
			}
		}
		// A replay of one person against the computer has no All Players (measured).
		if (people > 1) {
			this.visionChoices.add(War3MapViewer.REPLAY_VISION_ALL_PLAYERS);
			this.visionNames.add(this.rootFrame.getTemplates().getDecoratedString("ALL_PLAYERS"));
		}
		for (int i = 0; i < WarsmashConstants.MAX_PLAYERS; i++) {
			final CPlayer player = game.getPlayer(i);
			if (!this.viewer.isReplayVisionPlayer(player)) {
				continue;
			}
			this.visionChoices.add(i);
			this.visionNames.add("|cff" + MeleeChat.playerTextHex(player.getColor()) + playerName(player) + "|r");
		}
		final MenuFrame menu = (MenuFrame) this.visionMenu.getPopupMenuFrame();
		if (touch) {
			menu.setItemHeight(GameUI.convertY(this.uiViewport, TOUCH_HEIGHT));
		}
		final List<MenuItem> items = new ArrayList<>();
		for (final String name : this.visionNames) {
			items.add(new MenuItem(name, -2));
		}
		menu.setItems(this.uiViewport, items);
		// The panel is at the bottom of the screen: the menu opens upwards (measured).
		menu.clearFramePointAssignments();
		menu.addSetPoint(new SetPoint(FramePoint.BOTTOMLEFT, this.visionMenu, FramePoint.TOPLEFT, 0, 0));
		menu.addSetPoint(new SetPoint(FramePoint.BOTTOMRIGHT, this.visionMenu, FramePoint.TOPRIGHT, 0, 0));
	}

	/** The computer's slot has no name in a replay; the original calls it by its number (measured). */
	private String playerName(final CPlayer player) {
		if (player.getController() == CMapControl.COMPUTER) {
			return this.rootFrame.getTemplates().getDecoratedString("PLAYER") + " " + (player.getId() + 1);
		}
		return player.getName();
	}

	private int defaultVisionIndex() {
		final int recording = this.viewer.getLocalPlayerIndex();
		final int index = this.visionChoices.indexOf(recording);
		return index >= 0 ? index : 0;
	}

	private void chooseVision(final int index) {
		if ((index < 0) || (index >= this.visionChoices.size())) {
			return;
		}
		this.rootFrame.setText(this.visionTitle, this.visionNames.get(index));
		this.viewer.setReplayVision(this.visionChoices.get(index), this.fogCheckBox.isChecked());
		this.host.viewingPlayerChanged();
		this.lastLife.clear();
	}

	private void visionChanged() {
		this.viewer.setReplayVision(this.viewer.getReplayVisionPlayer(), this.fogCheckBox.isChecked());
	}

	@Override
	public void onClick(final int abilityHandleId, final int orderId, final boolean rightClick) {
		if (rightClick) {
			return;
		}
		switch (orderId) {
		case ACTION_PAUSE:
			togglePause();
			break;
		case ACTION_SPEED_UP:
			changeSpeed(1);
			break;
		case ACTION_SPEED_DOWN:
			changeSpeed(-1);
			break;
		case ACTION_RESTART:
			this.host.restart();
			break;
		default:
			break;
		}
	}

	@Override
	public void openMenu(final int orderId) {
	}

	public void togglePause() {
		this.paused = !this.paused;
		this.viewer.setGamePaused(War3MapViewer.PauseSource.REPLAY, this.paused);
	}

	public boolean isPaused() {
		return this.paused;
	}

	public void changeSpeed(final int steps) {
		this.speedIndex = Math.max(0, Math.min(SPEEDS.length - 1, this.speedIndex + steps));
		this.viewer.setReplaySpeed(SPEEDS[this.speedIndex]);
		showSpeed();
	}

	private void showSpeed() {
		final float speed = SPEEDS[this.speedIndex];
		this.rootFrame.setText(this.speedMultiplier,
				speed < 1 ? "1/" + Math.round(1 / speed) + "X" : Math.round(speed) + "X");
		// "Speed" follows the number, which changes its width.
		this.speedMultiplier.positionBounds(this.rootFrame, this.uiViewport);
		this.speedLabel.positionBounds(this.rootFrame, this.uiViewport);
	}

	/** The replay's keys, as the tooltips name them: P pauses, + and - change the speed. */
	public boolean keyDown(final int keycode) {
		switch (keycode) {
		case Input.Keys.P:
		case Input.Keys.PAUSE:
			togglePause();
			return true;
		case Input.Keys.PLUS:
		case Input.Keys.EQUALS:
		case Input.Keys.NUMPAD_ADD:
			changeSpeed(1);
			return true;
		case Input.Keys.MINUS:
		case Input.Keys.NUMPAD_SUBTRACT:
			changeSpeed(-1);
			return true;
		default:
			return false;
		}
	}

	public UIFrame getFrame() {
		return this.holder;
	}

	public void setVisible(final boolean visible) {
		this.holder.setVisible(visible);
		if (!visible) {
			this.visionMenu.getPopupMenuFrame().setVisible(false);
		}
	}

	public void update(final float deltaTime) {
		final int tick = this.viewer.simulation.getGameTurnTick();
		final int second = (int) (tick * WarsmashConstants.SIMULATION_STEP_TIME);
		if (second != this.shownSecond) {
			this.shownSecond = second;
			this.rootFrame.setText(this.elapsedTime, String.format(Locale.ROOT, "%02d:%02d:%02d", second / 3600,
					(second / 60) % 60, second % 60));
		}
		final int lastTick = Math.max(1, this.playback.getLastTick());
		this.statusBar.setValue(Math.min(1f, tick / (float) lastTick));
		if (this.cameraCheckBox.isChecked()) {
			followAction(deltaTime);
		}
	}

	/**
	 * Auto Camera: every few seconds the camera pans to where the shown player's units (or anyone's, with all players
	 * shown) lost the most life since the last look, if that is away from where it looks.
	 */
	private void followAction(final float deltaTime) {
		this.autoCameraTime += deltaTime;
		if ((this.autoCameraTime < AUTO_CAMERA_INTERVAL) || this.paused) {
			return;
		}
		this.autoCameraTime = 0;
		final int vision = this.viewer.getReplayVisionPlayer();
		float sumX = 0;
		float sumY = 0;
		float lost = 0;
		for (final CUnit unit : this.viewer.simulation.getUnits()) {
			final float life = unit.isDead() ? 0 : unit.getLife();
			final Float before = this.lastLife.put(unit, life);
			if ((before == null) || (life >= before) || unit.isHidden()) {
				continue;
			}
			final boolean watched = (vision == War3MapViewer.REPLAY_VISION_ALL_PLAYERS)
					? this.viewer.isReplayVisionPlayer(this.viewer.simulation.getPlayer(unit.getPlayerIndex()))
					: (unit.getPlayerIndex() == vision);
			if (!watched || !this.viewer.isShownToViewer(unit)) {
				continue;
			}
			final float damage = before - life;
			sumX += unit.getX() * damage;
			sumY += unit.getY() * damage;
			lost += damage;
		}
		this.lastLife.keySet().removeIf(CUnit::isDead);
		if (lost <= 0) {
			return;
		}
		final float x = sumX / lost;
		final float y = sumY / lost;
		final float dx = x - this.camera.target.x;
		final float dy = y - this.camera.target.y;
		if (((dx * dx) + (dy * dy)) >= (AUTO_CAMERA_MIN_DISTANCE * AUTO_CAMERA_MIN_DISTANCE)) {
			this.camera.panToTimed(x, y, AUTO_CAMERA_PAN_TIME);
		}
	}
}
