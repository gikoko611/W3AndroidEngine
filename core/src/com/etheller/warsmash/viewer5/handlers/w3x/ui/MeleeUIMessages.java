package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.TimeUtils;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.parsers.fdf.datamodel.AnchorDefinition;
import com.etheller.warsmash.parsers.fdf.datamodel.FramePoint;
import com.etheller.warsmash.parsers.fdf.datamodel.TextJustify;
import com.etheller.warsmash.parsers.fdf.frames.AbstractRenderableFrame;
import com.etheller.warsmash.parsers.fdf.frames.GlueTextButtonFrame;
import com.etheller.warsmash.parsers.fdf.frames.SetPoint;
import com.etheller.warsmash.parsers.fdf.frames.SimpleFrame;
import com.etheller.warsmash.parsers.fdf.frames.StringFrame;
import com.etheller.warsmash.parsers.fdf.frames.UIFrame;
import com.etheller.warsmash.units.Element;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.UnitSound;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUpgradeType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUpgradeType.UpgradeLevel;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CLeaderboard;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog.CScriptDialog;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog.CScriptDialogButton;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog.CTimerDialog;

/**
 * What the game and its scripts tell the player: game and error messages, timer dialogs, the
 * leaderboard and script dialogs.
 */
final class MeleeUIMessages {
	private static final long WORLD_FRAME_MESSAGE_FADEOUT_MILLIS = TimeUnit.SECONDS.toMillis(9);
	private static final long WORLD_FRAME_MESSAGE_EXPIRE_MILLIS = TimeUnit.SECONDS.toMillis(10);
	private static final long WORLD_FRAME_MESSAGE_FADE_DURATION = WORLD_FRAME_MESSAGE_EXPIRE_MILLIS
			- WORLD_FRAME_MESSAGE_FADEOUT_MILLIS;
	private StringFrame errorMessageFrame;
	private final ArrayDeque<GameMessage> gameMessages = new ArrayDeque<>();
	private float gameMessageLift;
	private long lastErrorMessageExpireTime;
	private long lastErrorMessageFadeTime;
	private CLeaderboard shownLeaderboard;
	private UIFrame leaderboardFrame;
	private StringFrame leaderboardTitle;
	private StringFrame leaderboardLabels;
	private StringFrame leaderboardValues;
	private float timerDialogRight = Float.NaN;
	private float worldFrameUnitMessageFontHeight;
	private static final float TIMER_DIALOG_RIGHT = -0.0055f;
	private static final float TIMER_DIALOG_TOP = -0.0353f;
	private static final float TIMER_DIALOG_STEP = 0.03f;
	private static final float TIMER_DIALOG_GAP = 0.002f;
	// Measured on 1.29: shown 1 s after the death, fully for 10 s, then fading out.
	private static final long HERO_DEATH_MESSAGE_DELAY_MILLIS = 1000;
	private static final float HERO_DEATH_MESSAGE_SECONDS = 12;
	private final ArrayDeque<PendingHeroDeath> pendingHeroDeaths = new ArrayDeque<>();
	private static final float LEADERBOARD_WIDTH = 0.2f;
	private static final float LEADERBOARD_FONT_HEIGHT = 0.009f;
	private static final float LEADERBOARD_INSET_X = 0.016f;
	private static final float LEADERBOARD_TITLE_TOP = 0.013f;
	private static final float LEADERBOARD_ROWS_TOP = 0.028f;
	private static final float LEADERBOARD_ROW_HEIGHT = 0.014f;
	private static final float LEADERBOARD_BOTTOM_INSET = 0.012f;

	final MeleeUI ui;

	MeleeUIMessages(final MeleeUI ui) {
		this.ui = ui;
	}

	void createErrorMessageFrame() {
		final Element fontHeights = this.ui.war3MapViewer.miscData.get("FontHeights");
		final float worldFrameMessageFontHeight = fontHeights.getFieldFloatValue("WorldFrameMessage");
		this.errorMessageFrame = this.ui.rootFrame.createStringFrame("SmashErrorMessageFrame", this.ui.rootFrame,
				new Color(0xFFCC00FF), TextJustify.LEFT, TextJustify.MIDDLE, worldFrameMessageFontHeight);
		this.errorMessageFrame.addAnchor(new AnchorDefinition(FramePoint.BOTTOMLEFT,
				GameUI.convertX(this.ui.uiViewport, 0.212f), GameUI.convertY(this.ui.uiViewport, 0.182f)));
		this.errorMessageFrame.setWidth(GameUI.convertX(this.ui.uiViewport, 0.35f));
		this.errorMessageFrame.setHeight(GameUI.convertY(this.ui.uiViewport, worldFrameMessageFontHeight));

		this.errorMessageFrame.setFontShadowColor(new Color(0f, 0f, 0f, 0.9f));
		this.errorMessageFrame.setFontShadowOffsetX(GameUI.convertX(this.ui.uiViewport, 0.001f));
		this.errorMessageFrame.setFontShadowOffsetY(GameUI.convertY(this.ui.uiViewport, -0.001f));
		this.errorMessageFrame.setVisible(false);

		this.worldFrameUnitMessageFontHeight = fontHeights.getFieldFloatValue("WorldFrameUnitMessage");
	}

	public void showInterfaceError(final int playerIndex, final String externStringKey) {
		if (playerIndex == this.ui.war3MapViewer.getLocalPlayerIndex()) {
			if (externStringKey != null) {
				showLocalCommandErrorString(playerIndex, externStringKey);
			}
			getUiSoundForError(externStringKey).play(this.ui.uiScene.audioContext, 0, 0, 0);
		}
	}

	UnitSound getUiSoundForError(final String externStringKey) {
		String sound = "InterfaceError";
		final String soundKey = externStringKey + "Sound";
		if (this.ui.rootFrame.hasSkinField(soundKey)) {
			sound = this.ui.rootFrame.getSkinField(soundKey);
		}
		return this.ui.war3MapViewer.getUiSounds().getSound(sound);
	}

	public void showCommandErrorWithoutSound(final int playerIndex, final String message) {
		if (playerIndex == this.ui.war3MapViewer.getLocalPlayerIndex()) {
			showLocalCommandErrorString(playerIndex, message);
		}
	}

	private void showLocalCommandErrorString(final int playerIndex, final String message) {
		String errorString = this.ui.rootFrame.getErrorString(message);
		if (errorString.isEmpty() && !message.isEmpty()) {
			errorString = message; // this may show some NOTEXTERN engine garbage that we should fix later
		}
		innerShowLocalCommandErrorString(playerIndex, errorString);
	}

	private void innerShowLocalCommandErrorString(final int playerIndex, final String message) {
		this.ui.rootFrame.setText(this.errorMessageFrame, message);
		this.errorMessageFrame.setVisible(true);
		final long millis = TimeUtils.millis();
		this.lastErrorMessageExpireTime = millis + WORLD_FRAME_MESSAGE_EXPIRE_MILLIS;
		this.lastErrorMessageFadeTime = millis + WORLD_FRAME_MESSAGE_FADEOUT_MILLIS;
		this.errorMessageFrame.setAlpha(1.0f);
	}

	private static final class GameMessage {
		private final StringFrame stringFrame;
		private final long lastGameMessageExpireTime;
		private final long lastGameMessageFadeTime;

		public GameMessage(final StringFrame stringFrame, final long lastGameMessageExpireTime,
				final long lastGameMessageFadeTime) {
			this.stringFrame = stringFrame;
			this.lastGameMessageExpireTime = lastGameMessageExpireTime;
			this.lastGameMessageFadeTime = lastGameMessageFadeTime;
		}
	}

	/** Messages stack up from the middle of the screen; a chat line lifted by the on-screen keyboard can reach it. */
	void liftGameMessages() {
		float lift = 0;
		final float chatTop = this.ui.chat.entryTop();
		if (!this.gameMessages.isEmpty() && (chatTop != Float.NEGATIVE_INFINITY)) {
			final float newestHeight = this.gameMessages.getLast().stringFrame.getAssignedHeight();
			final float newestBottom = this.ui.rootFrame.getFramePointY(FramePoint.LEFT) - (newestHeight / 2);
			lift = Math.max(0, (chatTop + GameUI.convertY(this.ui.uiViewport, 0.005f)) - newestBottom);
		}
		if (lift == this.gameMessageLift) {
			return;
		}
		this.gameMessageLift = lift;
		if (this.gameMessages.isEmpty()) {
			return;
		}
		this.gameMessages.getLast().stringFrame.addAnchor(new AnchorDefinition(FramePoint.LEFT, 0, lift));
		final Iterator<GameMessage> descendingIterator = this.gameMessages.descendingIterator();
		while (descendingIterator.hasNext()) {
			descendingIterator.next().stringFrame.positionBounds(this.ui.rootFrame, this.ui.uiViewport);
		}
	}

	public void showGameMessage(final String message, final float expireTime) {
		this.ui.messageLog.add(message);
		// The score screen replaces the game screen; messages that come after it are only kept in the log.
		if ((this.ui.scoreScreen != null) && this.ui.scoreScreen.isVisible()) {
			return;
		}
		final StringFrame gameMessagesFrame = this.ui.rootFrame.createStringFrame("SmashGameMessageFrame",
				this.ui.rootFrame, Color.WHITE, TextJustify.LEFT, TextJustify.MIDDLE,
				this.worldFrameUnitMessageFontHeight);
		gameMessagesFrame.setWidth(GameUI.convertX(this.ui.uiViewport, 0.35f));
//		gameMessagesFrame.setHeight(GameUI.convertY(this.uiViewport, this.worldFrameUnitMessageFontHeight));

		gameMessagesFrame.setFontShadowColor(new Color(0f, 0f, 0f, 0.9f));
		gameMessagesFrame.setFontShadowOffsetX(GameUI.convertX(this.ui.uiViewport, 0.001f));
		gameMessagesFrame.setFontShadowOffsetY(GameUI.convertY(this.ui.uiViewport, -0.001f));

		this.ui.rootFrame.setText(gameMessagesFrame, message);
		gameMessagesFrame.setVisible(true);

		final long millis = TimeUtils.millis();

		final long lastGameMessageExpireTime = millis + (long) (expireTime * 1000);
		final long lastGameMessageFadeTime = millis + (long) (expireTime * 900);
		gameMessagesFrame.addAnchor(new AnchorDefinition(FramePoint.LEFT, 0, this.gameMessageLift));
		if (!this.gameMessages.isEmpty()) {
			final GameMessage lastFrame = this.gameMessages.getLast();
			lastFrame.stringFrame
					.addSetPoint(new SetPoint(FramePoint.BOTTOMLEFT, gameMessagesFrame, FramePoint.TOPLEFT, 0, 0));
		}
		this.gameMessages
				.addLast(new GameMessage(gameMessagesFrame, lastGameMessageExpireTime, lastGameMessageFadeTime));
		final Iterator<GameMessage> descendingIterator = this.gameMessages.descendingIterator();
		while (descendingIterator.hasNext()) {
			final GameMessage gameMessage = descendingIterator.next();
			gameMessage.stringFrame.positionBounds(this.ui.rootFrame, this.ui.uiViewport);
			gameMessage.stringFrame.setHeight(gameMessage.stringFrame.getPredictedViewportHeight());
		}
		gameMessagesFrame.setAlpha(1.0f);
	}

	/**
	 * The local player's hero died: as on 1.29, a second later "<name> the <class> (level N) has fallen." in the
	 * wording of the player's race, the name in the player's color, with the race's hero death sound.
	 */
	void heroDied(final CUnit hero) {
		if ((hero.getPlayerIndex() != this.ui.war3MapViewer.getLocalPlayerIndex()) || (hero.getHeroData() == null)) {
			return;
		}
		final String format = this.ui.rootFrame.getErrorString("Herodeath");
		final String name = "|cff"
				+ MeleeChat.playerTextHex(this.ui.war3MapViewer.simulation.getPlayer(hero.getPlayerIndex()).getColor())
				+ hero.getHeroData().getProperName() + "|r";
		final String message = (format == null) || format.isEmpty() ? null
				: format.replaceFirst("%s", Matcher.quoteReplacement(name))
						.replaceFirst("%s", Matcher.quoteReplacement(hero.getUnitType().getName()))
						.replaceFirst("%d", Integer.toString(hero.getHeroData().getHeroLevel()));
		this.pendingHeroDeaths.addLast(new PendingHeroDeath(TimeUtils.millis() + HERO_DEATH_MESSAGE_DELAY_MILLIS,
				message));
	}

	private void showPendingHeroDeaths(final long currentMillis) {
		while (!this.pendingHeroDeaths.isEmpty() && (currentMillis >= this.pendingHeroDeaths.getFirst().dueMillis)) {
			final PendingHeroDeath death = this.pendingHeroDeaths.removeFirst();
			if (death.message != null) {
				showGameMessage(death.message, HERO_DEATH_MESSAGE_SECONDS);
			}
			if (this.ui.rootFrame.hasSkinField("HeroDiesSound")) {
				final UnitSound sound = this.ui.war3MapViewer.getUiSounds()
						.getSound(this.ui.rootFrame.getSkinField("HeroDiesSound"));
				if (sound != null) {
					sound.play(this.ui.uiScene.audioContext, 0, 0, 0);
				}
			}
		}
	}

	private static final class PendingHeroDeath {
		private final long dueMillis;
		private final String message;

		private PendingHeroDeath(final long dueMillis, final String message) {
			this.dueMillis = dueMillis;
			this.message = message;
		}
	}

	public void showUpgradeCompleteAlert(final int playerIndex, final War3ID queuedRawcode, final int level) {
		if (playerIndex == this.ui.war3MapViewer.getLocalPlayerIndex()) {
			String upgradeName;
			final CUpgradeType upgradeType = this.ui.war3MapViewer.simulation.getUpgradeData().getType(queuedRawcode);
			if (upgradeType != null) {
				final UpgradeLevel upgradeLevel = upgradeType.getLevel(level - 1);
				if (upgradeLevel != null) {
					upgradeName = upgradeLevel.getName();
				}
				else {
					upgradeName = "NOTEXTERN Unknown Level " + level + " for '" + queuedRawcode + "'";
				}
			}
			else {
				upgradeName = "NOTEXTERN Unknown ('" + queuedRawcode + "')";
			}
			innerShowLocalCommandErrorString(playerIndex,
					this.ui.rootFrame.getTemplates().getDecoratedString("COLON_COMPLETED") + upgradeName);
			this.ui.war3MapViewer.getUiSounds().getSound(this.ui.rootFrame.getSkinField("ResearchComplete"))
					.play(this.ui.uiScene.audioContext, 0, 0, 0);
		}
	}

	void updateMessages(final long currentMillis) {
		showPendingHeroDeaths(currentMillis);
		if (currentMillis > this.lastErrorMessageExpireTime) {
			this.errorMessageFrame.setVisible(false);
		}
		else if (currentMillis > this.lastErrorMessageFadeTime) {
			final float fadeAlpha = (this.lastErrorMessageExpireTime - currentMillis)
					/ (float) WORLD_FRAME_MESSAGE_FADE_DURATION;
			this.errorMessageFrame.setAlpha(fadeAlpha);
		}
		final Iterator<GameMessage> iterator = this.gameMessages.iterator();
		while (iterator.hasNext()) {
			final GameMessage gameMessage = iterator.next();
			if (currentMillis > gameMessage.lastGameMessageExpireTime) {
				this.ui.rootFrame.remove(gameMessage.stringFrame);
				iterator.remove();
			}
			else if (currentMillis > gameMessage.lastGameMessageFadeTime) {
				final float fadeAlpha = (gameMessage.lastGameMessageExpireTime - currentMillis)
						/ (float) (gameMessage.lastGameMessageExpireTime - gameMessage.lastGameMessageFadeTime);
				gameMessage.stringFrame.setAlpha(fadeAlpha);
			}
		}
	}

	/**
	 * A script dialog hotkey is a Windows virtual key code ('C' is the C key; 'c' is no key). As in 1.29, only the
	 * dialog shown last takes it, not even an earlier one still shown once that one is hidden, and of its buttons
	 * sharing the key the one added last.
	 */
	boolean pressScriptDialogHotkey(final int keycode) {
		if (keycode == Input.Keys.UNKNOWN) {
			return false;
		}
		CScriptDialog shown = null;
		for (final CScriptDialog dialog : this.ui.scriptDialogs) {
			if ((shown == null) || (dialog.getShownOrder() > shown.getShownOrder())) {
				shown = dialog;
			}
		}
		if ((shown == null) || !shown.isVisible()) {
			return false;
		}
		final List<CScriptDialogButton> buttons = shown.getButtons();
		for (int i = buttons.size() - 1; i >= 0; i--) {
			final CScriptDialogButton button = buttons.get(i);
			if ((MeleeUIInput.virtualKeyToKeycode(button.getHotkey()) == keycode)
					&& (button.getButtonFrame() != null)) {
				button.getButtonFrame().onClick(Input.Buttons.LEFT);
				return true;
			}
		}
		return false;
	}

	public void setLeaderboard(final CLeaderboard leaderboard) {
		if (this.shownLeaderboard != null) {
			this.shownLeaderboard.setChangeListener(null);
		}
		this.shownLeaderboard = leaderboard;
		if (this.leaderboardFrame == null) {
			if (leaderboard == null) {
				return;
			}
			this.leaderboardFrame = this.ui.rootFrame.createFrame("Leaderboard", this.ui.rootFrame, 0, 0);
			this.leaderboardTitle = (StringFrame) this.ui.rootFrame.getFrameByName("LeaderboardTitle", 0);
			final UIFrame backdrop = this.ui.rootFrame.getFrameByName("LeaderboardBackdrop", 0);
			this.leaderboardLabels = this.ui.rootFrame.createStringFrame("SmashLeaderboardLabels", backdrop,
					Color.WHITE, TextJustify.LEFT, TextJustify.TOP, LEADERBOARD_FONT_HEIGHT);
			this.leaderboardLabels.addSetPoint(new SetPoint(FramePoint.TOPLEFT, backdrop, FramePoint.TOPLEFT,
					GameUI.convertX(this.ui.uiViewport, LEADERBOARD_INSET_X),
					GameUI.convertY(this.ui.uiViewport, -LEADERBOARD_ROWS_TOP)));
			this.leaderboardLabels.addSetPoint(new SetPoint(FramePoint.BOTTOMRIGHT, backdrop, FramePoint.BOTTOMRIGHT,
					GameUI.convertX(this.ui.uiViewport, -LEADERBOARD_INSET_X), 0));
			this.leaderboardValues = this.ui.rootFrame.createStringFrame("SmashLeaderboardValues", backdrop,
					Color.WHITE, TextJustify.RIGHT, TextJustify.TOP, LEADERBOARD_FONT_HEIGHT);
			this.leaderboardValues.addSetPoint(new SetPoint(FramePoint.TOPLEFT, backdrop, FramePoint.TOPLEFT,
					GameUI.convertX(this.ui.uiViewport, LEADERBOARD_INSET_X),
					GameUI.convertY(this.ui.uiViewport, -LEADERBOARD_ROWS_TOP)));
			this.leaderboardValues.addSetPoint(new SetPoint(FramePoint.BOTTOMRIGHT, backdrop, FramePoint.BOTTOMRIGHT,
					GameUI.convertX(this.ui.uiViewport, -LEADERBOARD_INSET_X), 0));
			this.leaderboardFrame.setWidth(GameUI.convertX(this.ui.uiViewport, LEADERBOARD_WIDTH));
			this.ui.leaderboardSlot = -1;
		}
		final boolean shown = leaderboard != null;
		this.leaderboardFrame.setVisible(shown);
		this.leaderboardLabels.setVisible(shown);
		this.leaderboardValues.setVisible(shown);
		if (shown) {
			leaderboard.setChangeListener(this::refreshLeaderboard);
			refreshLeaderboard();
		}
	}

	/**
	 * The leaderboard is an edit box styled panel under the shown timer dialogs: the label centered on top, then one
	 * row per item with its label on the left and its value on the right, both in the item's player color unless the
	 * map sets other colors (1.29).
	 */
	private void refreshLeaderboard() {
		final CLeaderboard board = this.shownLeaderboard;
		if ((board == null) || (this.leaderboardFrame == null)) {
			return;
		}
		// Measured on 1.29: an empty title takes no line, the rows start where it would be (the Human08 board).
		final boolean titled = board.isShowLabel() && (board.getLabel() != null) && !board.getLabel().isEmpty();
		this.leaderboardTitle.setVisible(titled);
		this.ui.rootFrame.setText(this.leaderboardTitle, titled ? board.getLabel() : "");
		final StringBuilder labels = new StringBuilder();
		final StringBuilder values = new StringBuilder();
		for (final CLeaderboard.Item item : board.getItems()) {
			if (labels.length() > 0) {
				labels.append('\n');
				values.append('\n');
			}
			// A row with nothing in it still takes its line.
			if (board.isShowNames() && item.showLabel && !item.label.isEmpty()) {
				appendLeaderboardText(labels, item.label,
						leaderboardColor(item.labelColor, board.getLabelColor(), item.playerIndex));
			}
			else {
				labels.append(' ');
			}
			if (board.isShowValues() && item.showValue) {
				appendLeaderboardText(values, Integer.toString(item.value),
						leaderboardColor(item.valueColor, board.getValueColor(), item.playerIndex));
			}
			else {
				values.append(' ');
			}
		}
		final int rows = Math.max(board.getItemCount(), board.getSizeByItemCount());
		final float rowsTop = titled ? LEADERBOARD_ROWS_TOP : LEADERBOARD_TITLE_TOP;
		this.leaderboardLabels.addSetPoint(new SetPoint(FramePoint.TOPLEFT, this.leaderboardLabels.getParent(),
				FramePoint.TOPLEFT, GameUI.convertX(this.ui.uiViewport, LEADERBOARD_INSET_X),
				GameUI.convertY(this.ui.uiViewport, -rowsTop)));
		this.leaderboardValues.addSetPoint(new SetPoint(FramePoint.TOPLEFT, this.leaderboardValues.getParent(),
				FramePoint.TOPLEFT, GameUI.convertX(this.ui.uiViewport, LEADERBOARD_INSET_X),
				GameUI.convertY(this.ui.uiViewport, -rowsTop)));
		this.leaderboardFrame.setHeight(GameUI.convertY(this.ui.uiViewport,
				rowsTop + (rows * LEADERBOARD_ROW_HEIGHT) + LEADERBOARD_BOTTOM_INSET));
		this.leaderboardFrame.positionBounds(this.ui.rootFrame, this.ui.uiViewport);
		this.ui.rootFrame.setText(this.leaderboardLabels, labels.toString());
		this.ui.rootFrame.setText(this.leaderboardValues, values.toString());
	}

	private static void appendLeaderboardText(final StringBuilder text, final String value, final int rgba) {
		text.append("|c").append(String.format("%02x%06x", rgba & 0xFF, (rgba >>> 8) & 0xFFFFFF)).append(value)
				.append("|r");
	}

	private int leaderboardColor(final int itemColor, final int boardColor, final int playerIndex) {
		if (itemColor != -1) {
			return itemColor;
		}
		if (boardColor != -1) {
			return boardColor;
		}
		if (playerIndex >= 0) {
			final CPlayer player = this.ui.war3MapViewer.simulation.getPlayer(playerIndex);
			if (player != null) {
				return MeleeChat.playerTextColorRgba(player.getColor());
			}
		}
		return 0xFFFFFFFF;
	}

	public void removeTimerDialog(final CTimerDialog dialog) {
		this.ui.timerDialogs.remove(dialog);
		this.ui.timerDialogSlots.remove(dialog.getFrame());
	}

	/**
	 * Shown timer dialogs form a row under the upper bar from the top right corner of the screen (not of the 4:3
	 * area) to the left, the oldest on the right (1.29).
	 */
	void layoutTimerDialogs() {
		float screenRight = GameUI.convertX(this.ui.uiViewport, TIMER_DIALOG_RIGHT)
				- Math.min(0, (this.ui.uiViewport.getMinWorldWidth() - this.ui.uiViewport.getWorldWidth()) / 2);
		// The touch control group buttons take the right screen edge, so the timers and the leaderboard go beside
		// them.
		final ControlGroupBar groupBar = this.ui.groups.getControlGroupBar();
		if (groupBar != null) {
			screenRight = Math.min(screenRight,
					groupBar.columnLeft() - this.ui.rootFrame.getFramePointX(FramePoint.RIGHT));
		}
		if (screenRight != this.timerDialogRight) {
			this.timerDialogRight = screenRight;
			this.ui.timerDialogSlots.clear();
			this.ui.leaderboardSlot = -1;
		}
		final float top = GameUI.convertY(this.ui.uiViewport, TIMER_DIALOG_TOP);
		float right = screenRight;
		int slot = 0;
		for (final CTimerDialog dialog : this.ui.timerDialogs) {
			final UIFrame frame = dialog.getFrame();
			if (!dialog.isVisible()) {
				continue;
			}
			final Integer placed = this.ui.timerDialogSlots.get(frame);
			if ((placed == null) || (placed != slot)) {
				this.ui.timerDialogSlots.put(frame, slot);
				frame.addAnchor(new AnchorDefinition(FramePoint.TOPRIGHT, right, top));
				frame.positionBounds(this.ui.rootFrame, this.ui.uiViewport);
			}
			right -= ((AbstractRenderableFrame) frame).getAssignedWidth()
					+ GameUI.convertX(this.ui.uiViewport, TIMER_DIALOG_GAP);
			slot++;
		}
		// The leaderboard takes the place under the row of shown timer dialogs, right aligned with it. A shown
		// multiboard takes that place instead: the game shows one board at a time.
		final int leaderboardRow = Math.min(slot, 1);
		this.ui.multiboard.anchor(screenRight,
				GameUI.convertY(this.ui.uiViewport, TIMER_DIALOG_TOP - (leaderboardRow * TIMER_DIALOG_STEP)));
		if (this.leaderboardFrame != null) {
			final boolean leaderboardShown = (this.shownLeaderboard != null) && !this.ui.multiboard.isShowing();
			if (this.leaderboardFrame.isVisible() != leaderboardShown) {
				this.leaderboardFrame.setVisible(leaderboardShown);
				this.leaderboardLabels.setVisible(leaderboardShown);
				this.leaderboardValues.setVisible(leaderboardShown);
			}
		}
		if ((this.leaderboardFrame != null) && (this.ui.leaderboardSlot != leaderboardRow)) {
			this.ui.leaderboardSlot = leaderboardRow;
			this.leaderboardFrame.addAnchor(new AnchorDefinition(FramePoint.TOPRIGHT, screenRight,
					GameUI.convertY(this.ui.uiViewport, TIMER_DIALOG_TOP - (leaderboardRow * TIMER_DIALOG_STEP))));
			this.leaderboardFrame.positionBounds(this.ui.rootFrame, this.ui.uiViewport);
			// The row texts belong to the root frame, so moving the leaderboard does not move them.
			this.leaderboardLabels.positionBounds(this.ui.rootFrame, this.ui.uiViewport);
			this.leaderboardValues.positionBounds(this.ui.rootFrame, this.ui.uiViewport);
		}
	}

	public CTimerDialog createTimerDialog(final CTimer timer) {
		final UIFrame timerDialog = this.ui.rootFrame.createFrame("TimerDialog", this.ui.rootFrame, 0, 0);
		final StringFrame valueFrame = (StringFrame) this.ui.rootFrame.getFrameByName("TimerDialogValue", 0);
		final StringFrame titleFrame = (StringFrame) this.ui.rootFrame.getFrameByName("TimerDialogTitle", 0);
		final CTimerDialog dialog = new CTimerDialog(this.ui.rootFrame, timer, timerDialog, valueFrame, titleFrame);
		// Like in the game, a new timer dialog stays hidden until TimerDialogDisplay.
		dialog.setVisible(false);
		this.ui.timerDialogs.add(dialog);
		return dialog;
	}

	public void displayTimedText(final float x, final float y, final float duration, final String message) {
		showGameMessage(message, duration); // TODO x y
	}

	public void clearTextMessages() {
		for (final GameMessage gameMessage : this.gameMessages) {
			this.ui.rootFrame.remove(gameMessage.stringFrame);
		}
		this.gameMessages.clear();
	}

	public CScriptDialog createScriptDialog(final GlobalScope globalScope) {
		final SimpleFrame scriptDialog = (SimpleFrame) this.ui.rootFrame.createFrame("ScriptDialog", this.ui.rootFrame,
				0, 0);
		scriptDialog.addAnchor(new AnchorDefinition(FramePoint.TOP, 0, GameUI.convertY(this.ui.uiViewport, -0.05f)));
		scriptDialog.setVisible(false);
		final StringFrame scriptDialogTextFrame = (StringFrame) this.ui.rootFrame.getFrameByName("ScriptDialogText", 0);
		scriptDialog.positionBounds(this.ui.rootFrame, this.ui.uiViewport);
		final CScriptDialog dialog = new CScriptDialog(globalScope, scriptDialog, scriptDialogTextFrame);
		this.ui.scriptDialogs.add(dialog);
		return dialog;
	}

	public CScriptDialogButton createScriptDialogButton(final CScriptDialog scriptDialog, final String text,
			final char hotkey) {
		if (scriptDialog.getScriptDialogFrame() == null) {
			// A dialog destroyed before a save has no frames in the loaded game.
			clearDialog(scriptDialog);
		}
		final CScriptDialogButton newButton = new CScriptDialogButton(null, null, text, hotkey);
		attachScriptDialogButton(scriptDialog, newButton);
		return newButton;
	}

	void attachScriptDialogButton(final CScriptDialog scriptDialog, final CScriptDialogButton button) {
		final GlueTextButtonFrame scriptDialogButton = (GlueTextButtonFrame) this.ui.rootFrame
				.createFrame("ScriptDialogButton", scriptDialog.getScriptDialogFrame(), 0, 0);
		scriptDialogButton.setHeight(GameUI.convertY(this.ui.uiViewport, 0.03f));
		final StringFrame scriptDialogTextFrame = (StringFrame) this.ui.rootFrame
				.getFrameByName("ScriptDialogButtonText", 0);
		this.ui.rootFrame.setText(scriptDialogTextFrame, button.getText());
		scriptDialogButton.addSetPoint(new SetPoint(FramePoint.TOP, scriptDialog.getLastAddedComponent(),
				FramePoint.BOTTOM, 0, GameUI.convertY(this.ui.uiViewport, -0.005f)));
		button.attachFrames(scriptDialogButton, scriptDialogTextFrame);
		scriptDialog.addButton(this.ui.rootFrame, this.ui.uiViewport, button,
				() -> pressScriptDialogButton(scriptDialog, button));
	}

	private void pressScriptDialogButton(final CScriptDialog dialog, final CScriptDialogButton button) {
		if (this.ui.scriptDialogs.contains(dialog) && dialog.getButtons().contains(button)) {
			this.ui.unitOrderListener.clickDialogButton(dialog.getHandleId(), button.getHandleId());
		}
	}

	public void destroyDialog(final CScriptDialog dialog) {
		this.ui.scriptDialogs.remove(dialog);
		if (dialog.getScriptDialogFrame() != null) {
			this.ui.rootFrame.remove(dialog.getScriptDialogFrame());
		}
	}

	public void displayScriptDialog(final CScriptDialog dialog, final boolean flag) {
		if (flag && !dialog.isVisible()) {
			dialog.setShownOrder(++this.ui.lastScriptDialogShown);
		}
		dialog.setVisible(flag);
	}

	public void clearDialog(final CScriptDialog dialog) {
		destroyDialog(dialog);
		this.ui.scriptDialogs.add(dialog);
		final SimpleFrame scriptDialog = (SimpleFrame) this.ui.rootFrame.createFrame("ScriptDialog", this.ui.rootFrame,
				0, 0);
		scriptDialog.addAnchor(new AnchorDefinition(FramePoint.TOP, 0, GameUI.convertY(this.ui.uiViewport, -0.05f)));
		scriptDialog.setVisible(false);
		final StringFrame scriptDialogTextFrame = (StringFrame) this.ui.rootFrame.getFrameByName("ScriptDialogText", 0);
		scriptDialog.positionBounds(this.ui.rootFrame, this.ui.uiViewport);
		dialog.reset(scriptDialog, scriptDialogTextFrame);
	}
}
