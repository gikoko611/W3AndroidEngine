package com.etheller.warsmash.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.etheller.warsmash.units.Element;

final class DataFolderCheck {
	enum Kind {
		CASC,
		FOLDER,
		/** The "_retail_" folder of a local files install: game files with the text in a locale layer. */
		LOCAL_FILES,
		NONE
	}

	static final String CLASSIC_PREFIXES = "war3.w3mod,war3.w3mod\\_deprecated.w3mod,war3.w3mod\\_locales\\enus.w3mod";

	private static final Map<String, String> DESCRIPTIONS = new HashMap<>();
	private static final int MAX_SEARCH_DEPTH = 2;
	/** Picking the root of a full storage must not freeze the window. */
	private static final int MAX_SEARCHED_FOLDERS = 400;

	private DataFolderCheck() {
	}

	/**
	 * Without "All files access" Android 11+ still lists the folders of another app's files but hides the files, so a
	 * folder counts only when the files the engine opens first can be read.
	 */
	static Kind detect(final File folder) {
		if ((folder == null) || !folder.isDirectory() || !folder.canRead()) {
			return Kind.NONE;
		}
		// A local files install keeps the build info, indices and a small archive of an install, but its game files
		// lie unpacked in "_retail_", which the archives do not hold.
		final File archives = child(child(folder, "Data"), "data");
		if (isWarcraftBuildInfo(child(folder, ".build.info")) && hasCascIndex(archives)
				&& isReadable(child(archives, "data.000")) && (detect(child(folder, "_retail_")) == Kind.NONE)) {
			return Kind.CASC;
		}
		// An extracted game data folder, as the desktop Folder data source reads it.
		if (isReadable(child(child(folder, "ui"), "WorldEditGameStrings.txt"))
				&& isReadable(child(child(folder, "units"), "UnitData.slk"))) {
			return Kind.FOLDER;
		}
		if ((localeLayer(folder) != null) && isReadable(child(child(folder, "units"), "UnitData.slk"))) {
			return Kind.LOCAL_FILES;
		}
		return Kind.NONE;
	}

	private static File localeLayer(final File folder) {
		final File locales = child(folder, "_locales");
		File layer = child(locales, "enus.w3mod");
		if (layer == null) {
			final File[] children = locales == null ? null : locales.listFiles();
			if (children != null) {
				Arrays.sort(children);
				for (final File child : children) {
					if (child.getName().toLowerCase(Locale.ROOT).endsWith(".w3mod")) {
						layer = child;
						break;
					}
				}
			}
		}
		return isReadable(child(child(layer, "ui"), "WorldEditGameStrings.txt")) ? layer : null;
	}

	/**
	 * The folder with game data at or near the chosen one: players often pick the folder above it ("Games", "Warcraft
	 * III" around an extracted "data") or a folder inside it ("Data" of an install, "ui" of extracted files).
	 */
	static File locate(final File folder) {
		if (folder == null) {
			return null;
		}
		File found = gameData(folder);
		if (found == null) {
			found = gameData(folder.getParentFile());
		}
		if (found != null) {
			return found;
		}
		final int[] budget = { MAX_SEARCHED_FOLDERS };
		for (int depth = 1; depth <= MAX_SEARCH_DEPTH; depth++) {
			found = locateBelow(folder, depth, budget);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	/** The folder itself, or the "_retail_" folder when it is the root of a local files install. */
	private static File gameData(final File folder) {
		if (detect(folder) != Kind.NONE) {
			return folder;
		}
		final File retail = child(folder, "_retail_");
		return detect(retail) != Kind.NONE ? retail : null;
	}

	private static File locateBelow(final File folder, final int depth, final int[] budget) {
		final File[] children = folder.listFiles();
		if (children == null) {
			return null;
		}
		Arrays.sort(children);
		for (final File child : children) {
			if (!child.isDirectory() || child.getName().startsWith(".") || (budget[0]-- <= 0)) {
				continue;
			}
			final File found = depth == 1 ? gameData(child) : locateBelow(child, depth - 1, budget);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	/** The engine cannot read zipped data, and the prep tool hands its output out as a zip. */
	static boolean hasZip(final File folder) {
		final File[] children = folder == null ? null : folder.listFiles();
		if (children == null) {
			return false;
		}
		for (final File child : children) {
			if (child.isFile() && child.getName().toLowerCase(Locale.ROOT).endsWith(".zip")) {
				return true;
			}
		}
		return false;
	}

	/** Whether the main data source of an engine configuration still holds game data. */
	static boolean hasGameData(final Element dataSources) {
		if (dataSources == null) {
			return true;
		}
		final String type = dataSources.getField("Type00");
		if (!"Folder".equalsIgnoreCase(type) && !"CASC".equalsIgnoreCase(type)) {
			return true;
		}
		String path = dataSources.getField("Path00");
		if ((path.length() > 1) && path.startsWith("\"") && path.endsWith("\"")) {
			path = path.substring(1, path.length() - 1);
		}
		return detect(new File(path)) != Kind.NONE;
	}

	static String dataSourcesSection(final Kind kind, final File dataFolder, final File... extraFolders) {
		final List<String> lines = new ArrayList<>();
		int index = 0;
		lines.add("Type00=" + (kind == Kind.CASC ? "CASC" : "Folder"));
		lines.add("Path00=\"" + dataFolder.getAbsolutePath() + "\"");
		if (kind == Kind.CASC) {
			lines.add("Prefixes00=" + CLASSIC_PREFIXES);
			// 1.31 stores its file table in the older TVFS layout.
			if (cascVersion(dataFolder).startsWith("version 1.31")) {
				lines.add("OldFormat00=1");
			}
		}
		final List<File> folders = new ArrayList<>();
		if (kind == Kind.LOCAL_FILES) {
			folders.add(localeLayer(dataFolder));
		}
		folders.addAll(Arrays.asList(extraFolders));
		for (final File extraFolder : folders) {
			if ((extraFolder != null) && extraFolder.isDirectory()) {
				index++;
				final String id = String.format(Locale.ROOT, "%02d", index);
				lines.add("Type" + id + "=Folder");
				lines.add("Path" + id + "=\"" + extraFolder.getAbsolutePath() + "\"");
			}
		}
		final StringBuilder section = new StringBuilder("[DataSources]\n");
		section.append("Count=").append(index + 1).append('\n');
		for (final String line : lines) {
			section.append(line).append('\n');
		}
		return section.toString();
	}

	/** A short line for the launcher: the CASC build version, or how many files an extracted folder holds. */
	static synchronized String describe(final Kind kind, final File folder) {
		final String key = kind + ":" + folder.getAbsolutePath();
		String description = DESCRIPTIONS.get(key);
		if (description == null) {
			description = kind == Kind.CASC ? cascVersion(folder)
					: String.format(Locale.US, "%,d files", countFiles(folder));
			DESCRIPTIONS.put(key, description);
		}
		return description;
	}

	private static String cascVersion(final File folder) {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(new File(folder, ".build.info")), StandardCharsets.UTF_8))) {
			final String header = reader.readLine();
			final String values = reader.readLine();
			if ((header != null) && (values != null)) {
				final String[] names = header.split("\\|");
				final String[] fields = values.split("\\|", -1);
				for (int i = 0; (i < names.length) && (i < fields.length); i++) {
					if (names[i].startsWith("Version!")) {
						return "version " + fields[i];
					}
				}
			}
		}
		catch (final IOException e) {
			return "";
		}
		return "";
	}

	private static int countFiles(final File folder) {
		final File[] children = folder.listFiles();
		if (children == null) {
			return 0;
		}
		int count = 0;
		for (final File child : children) {
			count += child.isDirectory() ? countFiles(child) : 1;
		}
		return count;
	}

	/** The child with this name in any letter case, as the data sources look files up. */
	private static File child(final File folder, final String name) {
		if (folder == null) {
			return null;
		}
		final File[] children = folder.listFiles();
		if (children == null) {
			return null;
		}
		for (final File child : children) {
			if (child.getName().equalsIgnoreCase(name)) {
				return child;
			}
		}
		return null;
	}

	private static boolean isReadable(final File file) {
		if ((file == null) || !file.isFile()) {
			return false;
		}
		try (InputStream stream = new FileInputStream(file)) {
			return stream.read() >= 0;
		}
		catch (final IOException e) {
			return false;
		}
	}

	/** Other Blizzard games are CASC installs too; their build info names another CDN path. */
	private static boolean isWarcraftBuildInfo(final File file) {
		if (!isReadable(file)) {
			return false;
		}
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			final String header = reader.readLine();
			final String values = reader.readLine();
			if ((header == null) || (values == null)) {
				return false;
			}
			final String[] names = header.split("\\|");
			final String[] fields = values.split("\\|", -1);
			for (int i = 0; (i < names.length) && (i < fields.length); i++) {
				if (names[i].startsWith("CDN Path!")) {
					return fields[i].toLowerCase(Locale.ROOT).contains("war3");
				}
			}
			return true;
		}
		catch (final IOException e) {
			return false;
		}
	}

	private static boolean hasCascIndex(final File folder) {
		final File[] children = folder == null ? null : folder.listFiles();
		if (children == null) {
			return false;
		}
		for (final File child : children) {
			if (child.getName().toLowerCase(Locale.ROOT).endsWith(".idx") && isReadable(child)) {
				return true;
			}
		}
		return false;
	}
}
