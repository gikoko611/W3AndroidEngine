package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Locale;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.IntIntMap;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.parsers.w3x.War3Map;
import com.etheller.warsmash.util.Log;
import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens.PrimaryTag;
import com.etheller.warsmash.viewer5.handlers.w3x.SequenceUtils;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.MenuUI.MenuState;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.mapsetup.CurrentNetGameMapLookup;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.mapsetup.CurrentNetGameMapLookupFile;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.mapsetup.CurrentNetGameMapLookupPath;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.menu.BattleNetUI;

import net.warsmash.map.Crc32c;
import net.warsmash.map.NetMapDownloader;
import net.warsmash.uberserver.AccountCreationFailureReason;
import net.warsmash.uberserver.ChannelServerMessageType;
import net.warsmash.uberserver.GameCreationFailureReason;
import net.warsmash.uberserver.GamingNetworkConnection;
import net.warsmash.uberserver.GamingNetworkServerToClientListener;
import net.warsmash.uberserver.HandshakeDeniedReason;
import net.warsmash.uberserver.JoinGameFailureReason;
import net.warsmash.uberserver.LobbyPlayerType;
import net.warsmash.uberserver.LoginFailureReason;
import net.warsmash.uberserver.ServerErrorMessageType;

/**
 * The online service's messages to the menu; each is handled on the render thread.
 */
final class MenuBattleNetServerListener implements GamingNetworkServerToClientListener {
	private final MenuUI menu;
	private final MenuBattleNet battleNet;
	private final GamingNetworkConnection gamingNetworkConnection;
	private final DataSource dataSource;
	private final String mapDownloadDir;
	private boolean creatingGame = false;
	private CurrentNetGameMapLookup currentNetGameMapLookup;
	private long lastJoinGameChecksum;
	private NetMapDownloader netMapDownloader;

	MenuBattleNetServerListener(final MenuUI menu, final MenuBattleNet battleNet,
			final GamingNetworkConnection gamingNetworkConnection, final DataSource dataSource,
			final String mapDownloadDir) {
		this.menu = menu;
		this.battleNet = battleNet;
		this.gamingNetworkConnection = gamingNetworkConnection;
		this.dataSource = dataSource;
		this.mapDownloadDir = mapDownloadDir;
	}

	private BattleNetUI battleNetUI() {
		return this.battleNet.getBattleNetUI();
	}

	@Override
	public void disconnected() {
		Gdx.app.postRunnable(() -> {
			Log.warn("Disconnected from server...");
			this.battleNet.hideConnectDialog();
			this.menu.setMainMenuButtonsEnabled(true);
			this.menu.dialog.showError("ERROR_ID_DISCONNECT", null);

			battleNetUI().hide();
			this.battleNet.playCurrentBattleNetGlueSpriteDeath();
			this.menu.glueSpriteLayerCenter.setSequence("Death");
			this.menu.menuState = MenuState.LEAVING_BATTLE_NET_FROM_LOGGED_IN;
		});
	}

	@Override
	public void loginOk(final long sessionToken, final String welcomeMessage) {
		Gdx.app.postRunnable(() -> {
			battleNetUI().loginAccepted(sessionToken, welcomeMessage);
			battleNetUI().getDoors().setSequence(PrimaryTag.DEATH, SequenceUtils.ALTERNATE);
			this.menu.menuState = MenuState.GOING_TO_BATTLE_NET_WELCOME;
		});
	}

	@Override
	public void loginFailed(final LoginFailureReason loginFailureReason) {
		Gdx.app.postRunnable(() -> {
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
			this.menu.dialog.showError(msg, null);
		});
	}

	@Override
	public void joinedChannel(final String channelName) {
		Gdx.app.postRunnable(() -> {
			battleNetUI().joinedChannel(channelName);
			battleNetUI().hideCurrentScreen();
			this.battleNet.playCurrentBattleNetGlueSpriteDeath();
			final boolean insideTopBarMode = this.battleNet.isInsideTopBarMode();
			if (!insideTopBarMode) {
				this.menu.menuState = MenuState.GOING_TO_BATTLE_NET_CHAT_CHANNEL_FROM_OUTSIDE;
			}
			else {
				this.menu.menuState = MenuState.GOING_TO_BATTLE_NET_CHAT_CHANNEL;
			}
		});
	}

	@Override
	public void handshakeDenied(final HandshakeDeniedReason reason) {
		Gdx.app.postRunnable(() -> {
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
			this.battleNet.showConnectError(msg);
		});
	}

	@Override
	public void handshakeAccepted() {
		Gdx.app.postRunnable(() -> {
			this.battleNet.hideConnectDialog();
			this.menu.setGlueLayersSequence("MainMenu Death");
			this.menu.setMainMenuButtonsEnabled(true);
			this.menu.setMainMenuVisible(false);
			this.menu.menuState = MenuState.GOING_TO_BATTLE_NET_LOGIN;
		});
	}

	@Override
	public void channelMessage(final String userName, final String message) {
		Gdx.app.postRunnable(() -> battleNetUI().channelMessage(userName, message));
	}

	@Override
	public void channelEmote(final String userName, final String message) {
		Gdx.app.postRunnable(() -> battleNetUI().channelEmote(userName, message));
	}

	@Override
	public void channelServerMessage(final String userName, final ChannelServerMessageType messageType) {
		Gdx.app.postRunnable(() -> battleNetUI().channelServerMessage(userName, messageType));
	}

	@Override
	public void badSession() {
		Gdx.app.postRunnable(() -> this.menu.dialog.showError("ERROR_ID_NOTLOGGEDON", null));
	}

	@Override
	public void accountCreationOk() {
		Gdx.app.postRunnable(() -> battleNetUI().accountCreatedOk());
	}

	@Override
	public void accountCreationFailed(final AccountCreationFailureReason reason) {
		Gdx.app.postRunnable(() -> {
			switch (reason) {
			default:
			case USERNAME_ALREADY_EXISTS:
				this.menu.dialog.showError("ERROR_ID_NAMEUSED", null);
				break;
			}
		});
	}

	@Override
	public void joinedGame(final String gameName, final String mapName, final long mapChecksum) {
		this.lastJoinGameChecksum = mapChecksum;
		Gdx.app.postRunnable(() -> {
			boolean host = false;
			if (this.creatingGame) {
				host = true;
				this.creatingGame = false;
				battleNetUI().setJoinGamePreviewMapToHostedMap();
				this.currentNetGameMapLookup = new CurrentNetGameMapLookupPath(battleNetUI().getLastHostedGamePath());
			}
			else {
				// joining game as non-host
				final String mapNameLower = mapName.toLowerCase(Locale.US);
				if (!(mapNameLower.endsWith(".w3m") || mapNameLower.endsWith(".w3x")
						|| mapNameLower.endsWith(".wsm"))) {
					// error, not a map file
					Log.error("ERROR!!! NOT A MAP FILE: " + mapName);
					this.gamingNetworkConnection.leaveGame(battleNetUI().getGamingNetworkSessionToken());
					this.currentNetGameMapLookup = null;
				}
				else {
					final String mapNameOnly = mapName
							.substring(Math.max(mapName.lastIndexOf('/'), mapName.lastIndexOf('\\')) + 1);
					final String mapFileLookupName = this.mapDownloadDir + File.separator + mapNameOnly;
					File mapLookupFile = new File(mapFileLookupName);
					boolean hasMap = false;
					final Crc32c checksumCalculator = new Crc32c();
					War3Map map = null;
					int tildeIndex = 1;
					while (mapLookupFile.exists()) {
						try {
							map = War3MapViewer.beginLoadingMap(this.dataSource, mapLookupFile.getPath());
							final long localHDDMapChecksum = map.computeChecksum(checksumCalculator);
							if (localHDDMapChecksum == mapChecksum) {
								hasMap = true;
								break;
							}
						}
						catch (final Exception e) {
							Log.error(e);
						}
						final int dotIndex = mapFileLookupName.lastIndexOf('.');
						mapLookupFile = new File(mapFileLookupName.substring(0, dotIndex) + "~" + tildeIndex++
								+ mapFileLookupName.substring(dotIndex));
					}
					this.currentNetGameMapLookup = new CurrentNetGameMapLookupFile(mapLookupFile);
					if (hasMap) {
						battleNetUI().setJoinGamePreviewMap(mapLookupFile);
					}
					else {
						battleNetUI().clearJoinGamePreviewMap(mapNameOnly);
						this.gamingNetworkConnection.requestMap(battleNetUI().getGamingNetworkSessionToken());
					}

				}
			}

			battleNetUI().joinedGame(gameName, host);
			battleNetUI().hideCurrentScreen();
			this.battleNet.playCurrentBattleNetGlueSpriteDeath();
			this.menu.menuState = MenuState.GOING_TO_BATTLE_NET_CUSTOM_GAME_LOBBY;
		});
	}

	@Override
	public void joinGameFailed(final JoinGameFailureReason reason) {
		Gdx.app.postRunnable(() -> {
			switch (reason) {
			case GAME_ALREADY_STARTED:
				this.menu.dialog.showError("ERROR_ID_GAMECLOSED", null);
				break;
			case GAME_FULL:
				this.menu.dialog.showError("ERROR_ID_GAMEFULL", null);
				break;
			case SESSION_ERROR:
				this.menu.dialog.showError("NETERROR_JOINGAMEFAILED", null);
				break;
			default:
			case NO_SUCH_GAME:
				this.menu.dialog.showError("NETERROR_JOINGAMENOTFOUND", null);
				break;
			}
		});
	}

	@Override
	public void serverErrorMessage(final ServerErrorMessageType messageType) {
		Gdx.app.postRunnable(() -> {
			switch (messageType) {
			case UPLOAD_MAP_FAILED:
				this.menu.dialog.showError("NETERROR_MAPFILEWRITE", null);
				break;
			default:
			case ERROR_HANDLING_REQUEST:
				this.menu.dialog.showError("NETERROR_DEFAULTERROR", null);
				break;
			}
		});
	}

	@Override
	public void gameCreationFailed(final GameCreationFailureReason reason) {
		Gdx.app.postRunnable(() -> {
			switch (reason) {
			default:
			case GAME_NAME_ALREADY_USED:
				this.menu.dialog.showError("NETERROR_CREATEGAMEFAILED", null);
				break;
			}
		});
	}

	@Override
	public void gameCreationOk() {
		Log.info("NOTE: Server says we created a game OK");
		// NOTE: immediately following this the server should tell us we joined a game
		Gdx.app.postRunnable(() -> this.creatingGame = true);
	}

	@Override
	public void beginGamesList() {
		Gdx.app.postRunnable(() -> battleNetUI().beginGamesList());
	}

	@Override
	public void gamesListItem(final String gameName, final int openSlots, final int totalSlots) {
		Gdx.app.postRunnable(() -> battleNetUI().gamesListItem(gameName, openSlots, totalSlots));
	}

	@Override
	public void endGamesList() {
		Gdx.app.postRunnable(() -> battleNetUI().endGamesList());
	}

	@Override
	public void beginSendMap() {
		Gdx.app.postRunnable(() -> {
			if (this.currentNetGameMapLookup instanceof CurrentNetGameMapLookupFile) {
				final File mapFile = ((CurrentNetGameMapLookupFile) this.currentNetGameMapLookup).getFile();
				this.netMapDownloader = new NetMapDownloader(mapFile);
			}
			else {
				throw new IllegalStateException();
			}
		});
	}

	@Override
	public void sendMapData(final int sequenceNumber, final ByteBuffer data) {
		final ByteBuffer gdxThreadBuffer = ByteBuffer.allocate(data.remaining());
		gdxThreadBuffer.clear();
		gdxThreadBuffer.put(data);
		gdxThreadBuffer.flip();
		Gdx.app.postRunnable(() -> {
			Log.info("receiving map: " + sequenceNumber + ", " + gdxThreadBuffer.remaining());
			this.netMapDownloader.receive(sequenceNumber, gdxThreadBuffer);
		});
	}

	@Override
	public void endSendMap(final int sequenceNumber) {
		Gdx.app.postRunnable(() -> {
			Log.info("receiving map finishing: " + sequenceNumber);
			final long checksum = this.netMapDownloader.finish(sequenceNumber);
			if (this.currentNetGameMapLookup instanceof CurrentNetGameMapLookupFile) {
				if ((checksum == this.lastJoinGameChecksum) && this.netMapDownloader.isSequenceNumberingOK()) {
					final File mapFile = ((CurrentNetGameMapLookupFile) this.currentNetGameMapLookup).getFile();
					battleNetUI().setJoinGamePreviewMap(mapFile);
				}
				else {
					this.menu.dialog.showError("NETERROR_MAPFILEINCOMPLETE", null);
					this.gamingNetworkConnection.leaveGame(battleNetUI().getGamingNetworkSessionToken());
				}
			}
			else {
				throw new IllegalStateException();
			}
		});
	}

	@Override
	public void gameLobbySlotSetPlayerType(final int slot, final LobbyPlayerType playerType) {
		Gdx.app.postRunnable(() -> {
			Log.warn("gameLobbySlotSetPlayerType " + slot + " " + playerType);
			battleNetUI().gameLobbySlotSetPlayerType(slot, playerType);
		});
	}

	@Override
	public void gameLobbySlotSetPlayer(final int slot, final String userName) {
		Gdx.app.postRunnable(() -> {
			Log.warn("gameLobbySlotSetPlayer " + slot + " " + userName);
			battleNetUI().gameLobbySlotSetPlayer(slot, userName);
		});
	}

	@Override
	public void gameLobbySlotSetPlayerRace(final int slot, final int raceItemIndex) {
		Gdx.app.postRunnable(() -> {
			Log.warn("gameLobbySlotSetPlayerRace " + slot + " " + raceItemIndex);
			battleNetUI().gameLobbySlotSetPlayerRace(slot, raceItemIndex);
		});
	}

	@Override
	public void gameLobbyStartGame(final byte[] hostIpAddressBytes, final short hostUdpPort,
			final int yourServerPlayerSlot) {
		Gdx.app.postRunnable(() -> {
			this.menu.campaigns.hideScreens();
			this.menu.campaigns.getCurrentMissionSelectMenuUI().setVisible(false);
			this.menu.skirmish.hide();
			battleNetUI().hideCurrentScreen();
			this.battleNet.playCurrentBattleNetGlueSpriteDeath();
			final BeginGameInformation beginGameInformation = new BeginGameInformation();
			this.menu.beginGameInformation = beginGameInformation;
			beginGameInformation.gameMapLookup = this.currentNetGameMapLookup;
			beginGameInformation.sessionToken = battleNetUI().getGamingNetworkSessionToken();
			beginGameInformation.hostInetAddress = hostIpAddressBytes;
			beginGameInformation.hostUdpPort = hostUdpPort & 0xFFFF;
			final IntIntMap gameChatroomServerSlotToMapSlot = battleNetUI().getGameChatroomServerSlotToMapSlot();
			beginGameInformation.serverSlotToMapSlot = gameChatroomServerSlotToMapSlot;
			beginGameInformation.mapSlotToServerSlot = battleNetUI().getGameChatroomMapSlotToServerSlot();
			beginGameInformation.localPlayerIndex = gameChatroomServerSlotToMapSlot.get(yourServerPlayerSlot, -1);
			this.menu.currentMapConfig = battleNetUI().getGameChatroomMapConfig();
			this.menu.menuState = MenuState.GOING_TO_MAP;
		});
	}
}
