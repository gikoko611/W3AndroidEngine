package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.etheller.warsmash.parsers.fdf.datamodel.AnchorDefinition;
import com.etheller.warsmash.parsers.fdf.datamodel.FramePoint;
import com.etheller.warsmash.parsers.fdf.frames.GlueTextButtonFrame;
import com.etheller.warsmash.parsers.fdf.frames.SpriteFrame;
import com.etheller.warsmash.parsers.fdf.frames.StringFrame;
import com.etheller.warsmash.parsers.fdf.frames.UIFrame;
import com.etheller.warsmash.parsers.w3x.War3Map;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens.PrimaryTag;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.MenuUI.MenuState;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.menu.BattleNetUI;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.menu.BattleNetUIActionListener;

import net.warsmash.uberserver.GamingNetwork;
import net.warsmash.uberserver.GamingNetworkConnection;
import net.warsmash.uberserver.HostedGameVisibility;
import net.warsmash.uberserver.LobbyGameSpeed;
import net.warsmash.uberserver.LobbyPlayerType;

/**
 * The glue screens of the online service: the connect dialog of the main menu, the screens of {@link BattleNetUI} and
 * the requests their buttons send.
 */
final class MenuBattleNet implements BattleNetUIActionListener {
	private final MenuUI menu;
	private final GamingNetworkConnection gamingNetworkConnection;
	private BattleNetUI battleNetUI;
	private UIFrame battleNetConnectDialog;
	private StringFrame battleNetConnectInfoText;
	private GlueTextButtonFrame battleNetConnectCancelButton;
	private Task gameListQueryTask;

	MenuBattleNet(final MenuUI menu, final GamingNetworkConnection gamingNetworkConnection) {
		this.menu = menu;
		this.gamingNetworkConnection = gamingNetworkConnection;
	}

	BattleNetUI getBattleNetUI() {
		return this.battleNetUI;
	}

	SpriteFrame getDoors() {
		return this.battleNetUI.getDoors();
	}

	void createConnectDialog() {
		this.battleNetConnectDialog = this.menu.rootFrame.createFrame("BattleNetConnectDialog", this.menu.rootFrame, 0,
				0);
		this.battleNetConnectDialog.setVisible(false);
		this.battleNetConnectDialog.addAnchor(new AnchorDefinition(FramePoint.CENTER, 0, 0));
		this.battleNetConnectInfoText = (StringFrame) this.menu.rootFrame.getFrameByName("ConnectInfoText", 0);
		this.battleNetConnectCancelButton = (GlueTextButtonFrame) this.menu.rootFrame.getFrameByName("ConnectButton",
				0);
		this.battleNetConnectCancelButton.setOnClick(() -> {
			this.gamingNetworkConnection.userRequestDisconnect();
			this.battleNetConnectDialog.setVisible(false);
			this.menu.setMainMenuButtonsEnabled(true);
		});
	}

	/** The main menu's button of the online service. */
	void connect() {
		this.menu.setMainMenuButtonsEnabled(false);
		this.menu.rootFrame.setDecoratedText(this.battleNetConnectInfoText, "BNET_CONNECT_INIT");
		this.battleNetConnectDialog.positionBounds(this.menu.rootFrame, this.menu.uiViewport);
		this.battleNetConnectDialog.setVisible(true);
		if (this.gamingNetworkConnection.userRequestConnect()) {
			this.gamingNetworkConnection.handshake(WarsmashConstants.getGameId(), GamingNetwork.GAME_VERSION_DATA);
		}
		else {
			this.battleNetConnectDialog.setVisible(false);
			this.menu.setMainMenuButtonsEnabled(true);
			this.menu.dialog.showError("ERROR_ID_CANTCONNECT", () -> {
			});
		}
	}

	void showConnectDialog() {
		this.battleNetConnectDialog.setVisible(true);
	}

	void hideConnectDialog() {
		this.battleNetConnectDialog.setVisible(false);
	}

	void showConnectError(final String msg) {
		this.battleNetConnectDialog.setVisible(true);
		this.menu.rootFrame.setDecoratedText(this.battleNetConnectInfoText, msg);
	}

	void createBattleNetUI() {
		this.battleNetUI = new BattleNetUI(this.menu.rootFrame, this.menu.uiViewport, this.menu.uiScene,
				this.menu.dataSource, this);
	}

	@Override
	public void cancelLoginPrompt() {
		this.battleNetUI.hide();
		this.battleNetUI.getDoors().setSequence(PrimaryTag.DEATH);
		this.menu.menuScreen.unAlternateModelBackToNormal();
		this.menu.menuState = MenuState.LEAVING_BATTLE_NET;
		this.gamingNetworkConnection.userRequestDisconnect();
	}

	@Override
	public void recoverPassword(final String text) {

	}

	@Override
	public void logon(final String accountName, final String password) {
		if (accountName.isEmpty()) {
			this.menu.dialog.showError("ERROR_ID_NAMEBLANK", null);
		}
		else if (password.isEmpty()) {
			this.menu.dialog.showError("NETERROR_NOPASSWORD", null);
		}
		else {
			final char[] passwordData = getPasswordData(password);
			this.gamingNetworkConnection.login(accountName, passwordData);
		}
	}

	private static char[] getPasswordData(final String password) {
		final int nPasswordChars = password.length();
		final char[] passwordData = new char[nPasswordChars];
		password.getChars(0, nPasswordChars, passwordData, 0);
		return passwordData;
	}

	@Override
	public void quitBattleNet() {
		this.battleNetUI.hide();
		playCurrentBattleNetGlueSpriteDeath();
		this.menu.glueSpriteLayerCenter.setSequence("Death");
		this.menu.menuState = MenuState.LEAVING_BATTLE_NET_FROM_LOGGED_IN;
		this.gamingNetworkConnection.userRequestDisconnect();
	}

	@Override
	public void openCustomGameMenu() {
		this.battleNetUI.hideCurrentScreen();
		playCurrentBattleNetGlueSpriteDeath();
		final boolean insideTopBarMode = isInsideTopBarMode();
		if (insideTopBarMode) {
			this.menu.glueSpriteLayerCenter.setSequence("Death");
		}
		this.menu.menuState = MenuState.GOING_TO_BATTLE_NET_CUSTOM_GAME_MENU;
		if (this.gameListQueryTask != null) {
			this.gameListQueryTask.cancel();
		}
		this.gameListQueryTask = Timer.instance().scheduleTask(new Task() {
			@Override
			public void run() {
				MenuBattleNet.this.gamingNetworkConnection
						.queryGamesList(MenuBattleNet.this.battleNetUI.getGamingNetworkSessionToken());
			}
		}, 0.0f, 5.0f);
	}

	@Override
	public void enterDefaultChat() {
		requestEnterDefaultChat();
	}

	@Override
	public void returnToChat() {
		this.battleNetUI.hideCurrentScreen();
		playCurrentBattleNetGlueSpriteDeath();
		if (this.battleNetUI.getCurrentChannel() == null) {
			this.menu.menuState = MenuState.GOING_TO_BATTLE_NET_WELCOME;
		}
		else {
			final boolean insideTopBarMode = isInsideTopBarMode();
			if (insideTopBarMode) {
				this.menu.menuState = MenuState.GOING_TO_BATTLE_NET_CHAT_CHANNEL;
			}
			else {
				this.menu.menuState = MenuState.GOING_TO_BATTLE_NET_CHAT_CHANNEL_FROM_OUTSIDE;
			}
		}
	}

	@Override
	public void requestJoinChannel(final String text) {
		this.gamingNetworkConnection.joinChannel(this.battleNetUI.getGamingNetworkSessionToken(), text);
	}

	@Override
	public void createAccount(final String username, final String password, final String repeatPassword) {
		if (!password.equals(repeatPassword)) {
			this.menu.dialog.showError("NETERROR_PASSWORDMISMATCH", null);
		}
		else if (username.isEmpty()) {
			this.menu.dialog.showError("ERROR_ID_NAMEBLANK", null);
		}
		else if (password.isEmpty()) {
			this.menu.dialog.showError("NETERROR_NOPASSWORD", null);
		}
		else if (username.length() < 3) {
			// TODO checks like this should be server side!!!
			this.menu.dialog.showError("NETERROR_USERNAMETOOSHORT", null);
		}
		else if (password.length() < 3) {
			this.menu.dialog.showError("NETERROR_PASSWORDTOOSHORT", null);
		}
		else {
			final char[] passwordData = getPasswordData(password);
			this.gamingNetworkConnection.createAccount(username, passwordData);
		}
	}

	@Override
	public void submitChatText(final String text) {
		if (text.startsWith("/me ")) {
			this.gamingNetworkConnection.emoteMessage(this.battleNetUI.getGamingNetworkSessionToken(),
					text.substring(4));
		}
		else {
			this.gamingNetworkConnection.chatMessage(this.battleNetUI.getGamingNetworkSessionToken(), text);
		}
	}

	@Override
	public void showChannelChooserPanel() {
		this.battleNetUI.hideCurrentScreen();
		playCurrentBattleNetGlueSpriteDeath();
		this.menu.menuState = MenuState.GOING_TO_BATTLE_NET_CHANNEL_MENU;

	}

	@Override
	public void showCreateGameMenu() {
		this.battleNetUI.hideCurrentScreen();
		playCurrentBattleNetGlueSpriteDeath();
		this.menu.menuState = MenuState.GOING_TO_BATTLE_NET_CREATE_CUSTOM_GAME_MENU;
	}

	@Override
	public void requestJoinGame(final String text) {
		this.gamingNetworkConnection.joinGame(this.battleNetUI.getGamingNetworkSessionToken(), text);
	}

	@Override
	public void showError(final String errorKey) {
		this.menu.dialog.showError(errorKey, null);
	}

	@Override
	public void createGame(final String gameName, final String mapPath, final int mapPlayerSlots,
			final LobbyGameSpeed gameSpeed, final HostedGameVisibility hostedGameVisibility, final long mapChecksum,
			final War3Map map) {
		this.gamingNetworkConnection.createGame(this.battleNetUI.getGamingNetworkSessionToken(), gameName, mapPath,
				mapPlayerSlots, gameSpeed, hostedGameVisibility, mapChecksum);
		final SeekableByteChannel inputChannel = map.getInternalMpqContentsDataSource().getInputChannel();
		try {
			inputChannel.position(0);
			final ByteBuffer mapDataBuffer = ByteBuffer.allocate(1300).clear();
			int mapSeqNo = 0;
			while (inputChannel.read(mapDataBuffer) != -1) {
				mapDataBuffer.flip();
				this.gamingNetworkConnection.uploadMapData(this.battleNetUI.getGamingNetworkSessionToken(),
						mapSeqNo++, mapDataBuffer);
				mapDataBuffer.clear();
			}
			this.gamingNetworkConnection.mapDone(this.battleNetUI.getGamingNetworkSessionToken(), mapSeqNo);
		}
		catch (final IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void leaveCustomGame() {
		this.gamingNetworkConnection.leaveGame(this.battleNetUI.getGamingNetworkSessionToken());
	}

	@Override
	public void gameLobbySetPlayerRace(final int serverSlot, final int raceItemIndex) {
		this.gamingNetworkConnection.gameLobbySetPlayerRace(this.battleNetUI.getGamingNetworkSessionToken(),
				serverSlot, raceItemIndex);
	}

	@Override
	public void gameLobbySetPlayerSlot(final int serverSlot, final LobbyPlayerType lobbyPlayerType) {
		this.gamingNetworkConnection.gameLobbySetPlayerSlot(this.battleNetUI.getGamingNetworkSessionToken(),
				serverSlot, lobbyPlayerType);
	}

	@Override
	public void startGame() {
		this.gamingNetworkConnection.gameLobbyStartGame(this.battleNetUI.getGamingNetworkSessionToken());
	}

	/** @return whether the state is one of the online service's screens, which it then advances */
	boolean updateMenuState(final MenuState menuState) {
		switch (menuState) {
		case GOING_TO_BATTLE_NET_LOGIN:
			this.menu.setGlueLayersSequence("Death");
			this.battleNetUI.setVisible(true);
			final SpriteFrame doors = this.battleNetUI.getDoors();
			doors.setVisible(true);
			doors.setSequence(PrimaryTag.BIRTH);
			this.menu.menuState = MenuState.GOING_TO_BATTLE_NET_LOGIN_PART2;
			return true;
		case GOING_TO_BATTLE_NET_LOGIN_PART2:
			this.menu.menuScreen.alternateModelToBattlenet();
			this.battleNetUI.showLoginPrompt(this.gamingNetworkConnection.getGatewayString());
			this.menu.menuState = MenuState.BATTLE_NET_LOGIN;
			return true;
		case LEAVING_BATTLE_NET_FROM_LOGGED_IN:
			this.menu.menuScreen.unAlternateModelBackToNormal();
			this.menu.glueSpriteLayerCenter.setVisible(false);
			this.menu.playMusic(this.menu.rootFrame.trySkinField("GlueMusic"), true, 0);
			// no break
		case LEAVING_BATTLE_NET:
			this.menu.setGlueLayersSequence("Birth");
			this.menu.menuState = MenuState.GOING_TO_MAIN_MENU;
			return true;
		case GOING_TO_BATTLE_NET_WELCOME:
			this.menu.setGlueLayersSequence("BattleNetWelcome Birth");
			this.menu.glueSpriteLayerCenter.setVisible(true);
			this.menu.glueSpriteLayerCenter.setSequence("Birth");
			this.menu.menuState = MenuState.BATTLE_NET_WELCOME;
			this.menu.playMusic(this.menu.rootFrame.trySkinField("ChatMusic"), true, 0);
			return true;
		case BATTLE_NET_WELCOME:
			this.menu.setGlueLayersSequence("BattleNetWelcome Stand");
			this.battleNetUI.showWelcomeScreen();
			this.menu.glueSpriteLayerCenter.setSequence("Stand");
			this.menu.menuState = MenuState.BATTLE_NET_WELCOME;
			return true;
		case GOING_TO_BATTLE_NET_CUSTOM_GAME_MENU:
			this.menu.setGlueLayersSequence("BattleNetCustom Birth");
			this.menu.menuState = MenuState.BATTLE_NET_CUSTOM_GAME_MENU;
			return true;
		case BATTLE_NET_CUSTOM_GAME_MENU:
			this.battleNetUI.showCustomGameMenu();
			this.menu.setGlueLayersSequence("BattleNetCustom Stand");
			return true;
		case GOING_TO_BATTLE_NET_CREATE_CUSTOM_GAME_MENU:
			this.menu.setGlueLayersSequence("BattleNetCustomCreate Birth");
			this.menu.menuState = MenuState.BATTLE_NET_CREATE_CUSTOM_GAME_MENU;
			return true;
		case BATTLE_NET_CREATE_CUSTOM_GAME_MENU:
			this.battleNetUI.showCustomGameCreateMenu();
			this.menu.setGlueLayersSequence("BattleNetCustomCreate Stand");
			return true;
		case GOING_TO_BATTLE_NET_CHANNEL_MENU:
			this.menu.setGlueLayersSequence("BattleNetChannel Birth");
			this.menu.menuState = MenuState.BATTLE_NET_CHANNEL_MENU;
			return true;
		case BATTLE_NET_CHANNEL_MENU:
			this.battleNetUI.showChannelMenu();
			this.menu.setGlueLayersSequence("BattleNetChannel Stand");
			return true;
		case GOING_TO_BATTLE_NET_CHAT_CHANNEL_FROM_OUTSIDE:
			this.menu.glueSpriteLayerCenter.setVisible(true);
			this.menu.glueSpriteLayerCenter.setSequence("Birth");
			// no break
		case GOING_TO_BATTLE_NET_CHAT_CHANNEL:
			this.menu.setGlueLayersSequence("BattleNetChatRoom Birth");
			this.menu.menuState = MenuState.BATTLE_NET_CHAT_CHANNEL;
			return true;
		case BATTLE_NET_CHAT_CHANNEL:
			this.battleNetUI.showChatChannel();
			this.menu.setGlueLayersSequence("BattleNetChatRoom Stand");
			return true;
		case GOING_TO_BATTLE_NET_CUSTOM_GAME_LOBBY:
			this.menu.setGlueLayersSequence("MultiplayerPreGameChat Birth");
			this.menu.menuState = MenuState.BATTLE_NET_CUSTOM_GAME_LOBBY;
			return true;
		case BATTLE_NET_CUSTOM_GAME_LOBBY:
			this.battleNetUI.showCustomGameLobby();
			this.menu.setGlueLayersSequence("MultiplayerPreGameChat Stand");
			return true;
		default:
			return false;
		}
	}

	void playCurrentBattleNetGlueSpriteDeath() {
		switch (this.menu.menuState) {
		case BATTLE_NET_LOGIN:
		case GOING_TO_BATTLE_NET_LOGIN:
		case GOING_TO_BATTLE_NET_LOGIN_PART2:
			this.battleNetUI.getDoors().setSequence(PrimaryTag.DEATH);
			break;
		case BATTLE_NET_CHAT_CHANNEL:
		case GOING_TO_BATTLE_NET_CHAT_CHANNEL:
			this.menu.setGlueLayersSequence("BattleNetChatRoom Death");
			break;
		case BATTLE_NET_CUSTOM_GAME_MENU:
		case GOING_TO_BATTLE_NET_CUSTOM_GAME_MENU:
			this.menu.setGlueLayersSequence("BattleNetCustom Death");
			break;
		case BATTLE_NET_CREATE_CUSTOM_GAME_MENU:
		case GOING_TO_BATTLE_NET_CREATE_CUSTOM_GAME_MENU:
			this.menu.setGlueLayersSequence("BattleNetCustomCreate Death");
			break;
		case BATTLE_NET_CHANNEL_MENU:
		case GOING_TO_BATTLE_NET_CHANNEL_MENU:
			this.menu.setGlueLayersSequence("BattleNetChannel Death");
			break;
		case BATTLE_NET_CUSTOM_GAME_LOBBY:
		case GOING_TO_BATTLE_NET_CUSTOM_GAME_LOBBY:
			this.menu.setGlueLayersSequence("MultiplayerPreGameChat Death");
			break;
		default:
		case BATTLE_NET_WELCOME:
		case GOING_TO_BATTLE_NET_WELCOME:
			this.menu.setGlueLayersSequence("BattleNetWelcome Death");
			break;
		}
		if (this.gameListQueryTask != null) {
			this.gameListQueryTask.cancel();
			this.gameListQueryTask = null;
		}
	}

	boolean isInsideTopBarMode() {
		boolean insideTopBarMode = false;
		switch (this.menu.menuState) {
		case GOING_TO_BATTLE_NET_CHANNEL_MENU:
		case BATTLE_NET_CHANNEL_MENU:
		case GOING_TO_BATTLE_NET_CHAT_CHANNEL:
		case GOING_TO_BATTLE_NET_CHAT_CHANNEL_FROM_OUTSIDE:
		case BATTLE_NET_CHAT_CHANNEL:
		case GOING_TO_BATTLE_NET_WELCOME:
		case BATTLE_NET_WELCOME:
			insideTopBarMode = true;
			break;
		default:
			break;
		}
		return insideTopBarMode;
	}

	void requestEnterDefaultChat() {
		this.gamingNetworkConnection.joinChannel(this.battleNetUI.getGamingNetworkSessionToken(),
				"Frozen Throne USA-1"); // TODO maybe not hardcode this
	}
}
