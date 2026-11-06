package com.etheller.warsmash.android;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.badlogic.gdx.files.FileHandle;
import com.etheller.warsmash.WarsmashGdxMapScreen;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.parsers.w3x.War3Map;
import com.etheller.warsmash.parsers.w3x.objectdata.Warcraft3MapObjectData;
import com.etheller.warsmash.parsers.w3x.w3i.Force;
import com.etheller.warsmash.parsers.w3x.w3i.Player;
import com.etheller.warsmash.parsers.w3x.w3i.War3MapW3i;
import com.etheller.warsmash.parsers.w3x.w3i.War3MapW3iFlags;
import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.units.Element;
import com.etheller.warsmash.units.StandardObjectData;
import com.etheller.warsmash.units.custom.WTS;
import com.etheller.warsmash.util.ArgbImage;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.tga.TgaFile;
import com.etheller.warsmash.viewer5.handlers.w3x.save.SavedGame;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CCampaignProgress;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CSaveStorage;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.mapsetup.MapListContainer;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.menu.CampaignCatalog;

/**
 * What the launcher's game pages list, read from the game data and the profile's files the way the engine reads them:
 * the campaigns and the profile's progress, the custom game maps and the saved games. The game data is read on one
 * worker thread, the answers come back on the main thread.
 */
final class GameCatalog {
	private static final String TAG = "Warsmash";
	private static final String ENGINE_PREFERENCES = "WarsmashWC3Engine";
	private static final String MAP_LIST = "maps.txt";
	private static final int MAP_LIST_FORMAT = 1;

	/** The campaigns and how far the current profile got. */
	static final class Campaigns {
		final CampaignCatalog catalog;
		final CCampaignProgress.Snapshot progress;

		Campaigns(final CampaignCatalog catalog, final CCampaignProgress.Snapshot progress) {
			this.catalog = catalog;
			this.progress = progress;
		}
	}

	/** A map of the custom game list, as the engine's list shows it. */
	static final class MapEntry {
		final String path;
		final int players;
		final String name;
		final boolean melee;
		/** The name in the list; maps of the same name and players also say where they lie. */
		String label;

		MapEntry(final String path, final int players, final String name, final boolean melee) {
			this.path = path;
			this.players = players;
			this.name = name;
			this.melee = melee;
			this.label = name;
		}
	}

	/** A player slot of a map, as its information file sets it up. */
	static final class Slot {
		final int id;
		/** 1 user, 2 computer, 3 neutral, 4 rescuable */
		final int type;
		/** 0 selectable, 1 human, 2 orc, 3 undead, 4 night elf */
		final int race;
		final String name;
		int force = -1;

		Slot(final int id, final int type, final int race, final String name) {
			this.id = id;
			this.type = type;
			this.race = race;
			this.name = name;
		}
	}

	static final class MapDetails {
		final String path;
		String name = "";
		String description = "";
		String suggested = "";
		String size = "";
		String tileset = "";
		boolean melee;
		boolean fixedSettings;
		boolean customForces;
		final List<Slot> slots = new ArrayList<>();
		final List<String> forces = new ArrayList<>();
		Bitmap minimap;

		MapDetails(final String path) {
			this.path = path;
		}
	}

	static final class SaveEntry {
		final String name;
		final File file;
		final long saved;
		SavedGame.Loadability loadability = SavedGame.Loadability.DAMAGED;
		String mapFile;
		float gameTime;

		SaveEntry(final String name, final File file) {
			this.name = name;
			this.file = file;
			this.saved = file.lastModified();
		}
	}

	private final Context context;
	private final DataTable ini;
	private final String dataKey;
	private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
		final Thread thread = new Thread(runnable, "LauncherCatalog");
		thread.setDaemon(true);
		return thread;
	});
	private final Handler main = new Handler(Looper.getMainLooper());
	private DataSource data;
	private CampaignCatalog campaignCatalog;
	private List<MapEntry> maps;
	private Element tilesets;
	private DataTable worldEditData;
	private boolean closed;

	GameCatalog(final Context context, final DataTable ini, final String dataKey) {
		this.context = context.getApplicationContext();
		this.ini = ini;
		this.dataKey = dataKey;
	}

	/** What the kept map list was read from: the data sources of the engine configuration. */
	static String dataKey(final DataTable ini) {
		final Element sources = ini.get("DataSources");
		final StringBuilder key = new StringBuilder();
		for (int i = 0; (sources != null) && (i < sources.size()); i++) {
			final String id = String.format(Locale.ROOT, "%02d", i);
			key.append('|').append(sources.getField("Type" + id)).append(':').append(sources.getField("Path" + id));
		}
		return key.toString();
	}

	/** The engine's current player profile, as its menu chose it (the engine's preferences). */
	static String currentProfile(final Context context) {
		final String name = context.getSharedPreferences(ENGINE_PREFERENCES, Context.MODE_PRIVATE)
				.getString("CurrentProfile", "WorldEdit");
		final String safe = CSaveStorage.safeName(name);
		return safe.isEmpty() ? "Player" : safe;
	}

	/** Save/&lt;profile&gt; in the private files, where the engine keeps the profile's saves and progress. */
	static File profileFolder(final Context context) {
		return new File(new File(context.getFilesDir(), "Save"), currentProfile(context));
	}

	void close() {
		this.closed = true;
		this.worker.execute(() -> {
			if (this.data != null) {
				try {
					this.data.close();
				}
				catch (final IOException e) {
					Log.w(TAG, "Unable to close the game data", e);
				}
				this.data = null;
			}
		});
		this.worker.shutdown();
	}

	private <T> void run(final Work<T> work, final Consumer<T> done) {
		if (this.closed) {
			return;
		}
		this.worker.execute(() -> {
			T value;
			try {
				value = work.run();
			}
			catch (final Exception | LinkageError e) {
				Log.e(TAG, "Launcher catalog failed", e);
				value = null;
			}
			final T answer = value;
			this.main.post(() -> {
				if (!this.closed) {
					done.accept(answer);
				}
			});
		});
	}

	private interface Work<T> {
		T run() throws Exception;
	}

	private DataSource data() {
		if (this.data == null) {
			WarsmashConstants.loadConstants(this.ini.get("Emulator"), this.ini);
			this.data = WarsmashGdxMapScreen.parseDataSources(this.ini);
		}
		return this.data;
	}

	/** null when the game data cannot be read */
	void campaigns(final Consumer<Campaigns> done) {
		run(() -> {
			if (this.campaignCatalog == null) {
				final DataSource data = data();
				final String file = GameUI.trySkinField(
						GameUI.loadSkin(data, WarsmashConstants.GAME_VERSION).getSkin(), "CampaignFile");
				this.campaignCatalog = CampaignCatalog.load(data, file);
			}
			return new Campaigns(this.campaignCatalog, readProgress(this.campaignCatalog.getList()));
		}, done);
	}

	/** The campaigns and the progress of each profile folder given, in their order; null when unreadable. */
	void progress(final List<File> folders, final Consumer<List<CCampaignProgress.Snapshot>> done) {
		run(() -> {
			if (this.campaignCatalog == null) {
				return null;
			}
			final List<CCampaignProgress.Snapshot> snapshots = new ArrayList<>();
			for (final File folder : folders) {
				snapshots.add(readProgress(this.campaignCatalog.getList(), folder));
			}
			return snapshots;
		}, done);
	}

	private CCampaignProgress.Snapshot readProgress(final CCampaignProgress.CampaignList list) {
		return readProgress(list, profileFolder(this.context));
	}

	private CCampaignProgress.Snapshot readProgress(final CCampaignProgress.CampaignList list, final File folder) {
		final File file = new File(folder, "Campaigns.w3v");
		if (!file.isFile()) {
			return CCampaignProgress.emptySnapshot();
		}
		try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
			return CCampaignProgress.readSnapshot(in, list);
		}
		catch (final IOException e) {
			Log.w(TAG, "Unable to read the campaign progress", e);
			return CCampaignProgress.emptySnapshot();
		}
	}

	/** The maps of the custom game list, ordered as the engine orders them; kept on disk for the same game data. */
	void maps(final Consumer<List<MapEntry>> done) {
		run(() -> {
			if (this.maps == null) {
				this.maps = readMapList();
				if (this.maps == null) {
					this.maps = scanMaps();
					writeMapList(this.maps);
				}
				labelNamesakes(this.maps);
			}
			return this.maps;
		}, done);
	}

	private File mapListFile() {
		return new File(new File(this.context.getCacheDir(), "catalog"), MAP_LIST);
	}

	private List<MapEntry> readMapList() {
		final File file = mapListFile();
		if (!file.isFile()) {
			return null;
		}
		try {
			final List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
			if (lines.isEmpty() || !lines.get(0).equals(MAP_LIST_FORMAT + "|" + this.dataKey)) {
				return null;
			}
			final List<MapEntry> entries = new ArrayList<>();
			for (int i = 1; i < lines.size(); i++) {
				final String[] parts = lines.get(i).split("\t", -1);
				if (parts.length == 4) {
					entries.add(new MapEntry(parts[0], Integer.parseInt(parts[1]), parts[2], "1".equals(parts[3])));
				}
			}
			return entries;
		}
		catch (final IOException | RuntimeException e) {
			return null;
		}
	}

	private void writeMapList(final List<MapEntry> entries) {
		final StringBuilder text = new StringBuilder().append(MAP_LIST_FORMAT).append('|').append(this.dataKey)
				.append('\n');
		for (final MapEntry entry : entries) {
			text.append(entry.path).append('\t').append(entry.players).append('\t')
					.append(entry.name.replace('\t', ' ').replace('\n', ' ')).append('\t')
					.append(entry.melee ? '1' : '0').append('\n');
		}
		try {
			final File file = mapListFile();
			file.getParentFile().mkdirs();
			Files.write(file.toPath(), text.toString().getBytes(StandardCharsets.UTF_8));
		}
		catch (final IOException e) {
			Log.w(TAG, "Unable to keep the map list", e);
		}
	}

	private List<MapEntry> scanMaps() {
		final DataSource data = data();
		final List<MapEntry> entries = new ArrayList<>();
		final Set<String> seen = new HashSet<>();
		for (final String path : data.getListfile()) {
			// Several data sources can list the same file, also spelled with other case or separators.
			if (!MapListContainer.isListedMap(path)
					|| !seen.add(path.replace('\\', '/').toLowerCase(Locale.ROOT))) {
				continue;
			}
			try {
				final War3Map map = new War3Map(data, path);
				final War3MapW3i info = map.readMapInformation();
				final WTS wts = Warcraft3MapObjectData.loadWTS(map);
				entries.add(new MapEntry(path, info.getPlayers().size(), trigStr(wts, info.getName()),
						info.hasFlag(War3MapW3iFlags.MELEE_MAP)));
			}
			catch (final IOException | RuntimeException e) {
				// The engine's list leaves out a map it cannot read, too.
				Log.w(TAG, "Skipping map with unreadable info: " + path, e);
			}
		}
		// As the engine's list sorts them: by players, then by name.
		Collections.sort(entries, (a, b) -> a.players == b.players ? a.name.compareTo(b.name)
				: Integer.compare(a.players, b.players));
		return entries;
	}

	/**
	 * Ladder seasons and the Reign of Chaos copies keep the names of the maps they change, so maps of the same name
	 * and players are told apart by their folder, or by their file when the folders are the same, too.
	 */
	private static void labelNamesakes(final List<MapEntry> entries) {
		final Map<String, List<MapEntry>> byName = new HashMap<>();
		for (final MapEntry entry : entries) {
			entry.label = entry.name;
			byName.computeIfAbsent(entry.players + "|" + entry.name, key -> new ArrayList<>()).add(entry);
		}
		for (final List<MapEntry> namesakes : byName.values()) {
			if (namesakes.size() < 2) {
				continue;
			}
			final Set<String> folders = new HashSet<>();
			for (final MapEntry entry : namesakes) {
				folders.add(folderLabel(entry.path));
			}
			final boolean byFolder = folders.size() == namesakes.size();
			for (final MapEntry entry : namesakes) {
				entry.label = entry.name + " (" + (byFolder ? folderLabel(entry.path) : fileName(entry.path)) + ")";
			}
		}
	}

	private static String fileName(final String path) {
		final String normal = path.replace('\\', '/');
		return normal.substring(normal.lastIndexOf('/') + 1);
	}

	/** The map's folder as words: frozenthrone is Frozen Throne, 2023season2 is 2023 Season 2. */
	private static String folderLabel(final String path) {
		if (path.toLowerCase(Locale.ROOT).endsWith(".w3m")) {
			return "Reign of Chaos";
		}
		final String normal = path.replace('\\', '/');
		final int end = normal.lastIndexOf('/');
		final String folder = normal.substring(normal.lastIndexOf('/', end - 1) + 1, Math.max(end, 0));
		if ("frozenthrone".equalsIgnoreCase(folder)) {
			return "Frozen Throne";
		}
		final StringBuilder words = new StringBuilder();
		for (int i = 0; i < folder.length(); i++) {
			final char c = folder.charAt(i);
			if ((i > 0) && (Character.isDigit(c) != Character.isDigit(folder.charAt(i - 1)))) {
				words.append(' ');
			}
			words.append((i == 0) || (words.charAt(words.length() - 1) == ' ') ? Character.toUpperCase(c) : c);
		}
		return words.toString();
	}

	static String trigStr(final WTS wts, final String value) {
		if ((value != null) && value.startsWith("TRIGSTR_")) {
			try {
				final String found = wts.get(Integer.parseInt(value.substring(8).trim()));
				return found != null ? found : value;
			}
			catch (final NumberFormatException e) {
				return value;
			}
		}
		return value != null ? value : "";
	}

	/** A map's information pane and player slots; null when the map cannot be read. */
	void mapDetails(final String path, final Consumer<MapDetails> done) {
		run(() -> {
			final DataSource data = data();
			final War3Map map = new War3Map(data, path);
			final War3MapW3i info = map.readMapInformation();
			final WTS wts = Warcraft3MapObjectData.loadWTS(map);
			final MapDetails details = new MapDetails(path);
			details.name = trigStr(wts, info.getName());
			details.description = trigStr(wts, info.getDescription());
			details.suggested = trigStr(wts, info.getRecommendedPlayers());
			final int[] size = info.getPlayableSize();
			details.size = size[0] + " x " + size[1];
			details.tileset = tilesetName(info.getTileset());
			details.melee = info.hasFlag(War3MapW3iFlags.MELEE_MAP);
			details.fixedSettings = info.hasFlag(War3MapW3iFlags.FIXED_PLAYER_SETTINGS_FOR_CUSTOM_FORCES);
			details.customForces = info.hasFlag(War3MapW3iFlags.USE_CUSTOM_FORCES);
			for (final Force force : info.getForces()) {
				details.forces.add(trigStr(wts, force.getName()));
			}
			for (final Player player : info.getPlayers()) {
				final Slot slot = new Slot(player.getId(), player.getType(), player.getRace(),
						trigStr(wts, player.getName()));
				for (int f = 0; f < info.getForces().size(); f++) {
					if ((info.getForces().get(f).getPlayerMasks() & (1L << player.getId())) != 0) {
						slot.force = f;
						break;
					}
				}
				details.slots.add(slot);
			}
			Collections.sort(details.slots, (a, b) -> Integer.compare(a.id, b.id));
			if (!info.hasFlag(War3MapW3iFlags.HIDE_MINIMAP_IN_PREVIEW_SCREENS)) {
				details.minimap = minimap(map);
			}
			return details;
		}, done);
	}

	private static Bitmap minimap(final War3Map map) {
		// The map's own archive only: the game data has a default preview that would hide the map's.
		final DataSource archive = map.getInternalMpqContentsDataSource();
		for (final String name : new String[] { "war3mapPreview", "war3mapMap" }) {
			try {
				// The editor saves the map picture as TGA; BLP2 pictures have no decoder here.
				if (archive.has(name + ".tga")) {
					try (InputStream stream = archive.getResourceAsStream(name + ".tga")) {
						final ArgbImage tga = TgaFile.readTGAPixels(name + ".tga", stream);
						final int width = tga.getWidth();
						final int height = tga.getHeight();
						final int[] pixels = new int[width * height];
						for (int y = 0; y < height; y++) {
							for (int x = 0; x < width; x++) {
								pixels[(y * width) + x] = tga.getRGB(x, y);
							}
						}
						return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
					}
				}
				final Bitmap image = SkinCache.decode(archive, name);
				if (image != null) {
					return image;
				}
			}
			catch (final IOException | RuntimeException e) {
				Log.w(TAG, "Unable to read " + name, e);
			}
		}
		return null;
	}

	private String tilesetName(final char tileset) {
		try {
			if (this.tilesets == null) {
				this.worldEditData = new StandardObjectData(data()).getWorldEditData();
				this.tilesets = this.worldEditData.get("TileSets");
			}
			final String key = this.tilesets.getField(Character.toString(tileset), 0);
			final String name = this.worldEditData.getLocalizedString(key);
			return name != null ? name : key;
		}
		catch (final RuntimeException e) {
			return "?";
		}
	}

	/** The profile's saved games, newest first, each with whether this engine can load it. */
	void saves(final Consumer<List<SaveEntry>> done) {
		run(() -> {
			final List<SaveEntry> saves = new ArrayList<>();
			final File[] files = profileFolder(this.context).listFiles();
			if (files != null) {
				for (final File file : files) {
					final String name = file.getName();
					if (!file.isFile() || !name.toLowerCase(Locale.ROOT).endsWith(CSaveStorage.SAVE_EXTENSION)) {
						continue;
					}
					final SaveEntry entry = new SaveEntry(
							name.substring(0, name.length() - CSaveStorage.SAVE_EXTENSION.length()), file);
					final FileHandle handle = new FileHandle(file);
					try {
						entry.loadability = SavedGame.loadability(handle);
						final SavedGame.Header header = SavedGame.readHeader(handle);
						entry.mapFile = header.mapFile;
						entry.gameTime = header.gameTime;
					}
					catch (final IOException | RuntimeException | LinkageError e) {
						entry.loadability = entry.loadability == SavedGame.Loadability.LOADABLE
								? SavedGame.Loadability.DAMAGED
								: entry.loadability;
					}
					saves.add(entry);
				}
			}
			Collections.sort(saves, (a, b) -> Long.compare(b.saved, a.saved));
			return saves;
		}, done);
	}
}
