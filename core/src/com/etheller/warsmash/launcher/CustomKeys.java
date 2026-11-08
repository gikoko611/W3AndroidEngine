package com.etheller.warsmash.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.badlogic.gdx.Gdx;

/**
 * The player's own hotkeys, in the game's CustomKeys.txt format: sections by rawcode or command name with the fields
 * Hotkey, Researchhotkey (Learn) and Unhotkey (turn off). Only the keys the player changed are written; they are laid
 * over the game data. The grid layout gives each command the key of its button's place (Q W E R / A S D F / Z X C V)
 * unless the player set one.
 */
public final class CustomKeys {
	public static final String FILE = "CustomKeys.txt";
	/** In the engine's preferences: "grid" for the grid layout, else the keys of the game data. */
	public static final String KEY_LAYOUT = "hotkeyLayout";
	public static final String LAYOUT_GRID = "grid";
	public static final String HOTKEY = "Hotkey";
	public static final String RESEARCH = "Researchhotkey";
	public static final String OFF = "Unhotkey";
	private static final String[] GRID = { "QWER", "ASDF", "ZXCV" };
	private static final String KEY_COLOUR = "|cffffcc00";

	private final Map<String, Map<String, String>> sections = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private boolean grid;

	public CustomKeys(final boolean grid) {
		this.grid = grid;
	}

	/**
	 * Where the player's file is: beside the engine, or in the preferences folder a run was given instead, so that
	 * automated runs never take the player's own keys.
	 */
	public static File playerFile() {
		final String preferences = System.getProperty("warsmash.prefs");
		if ((preferences != null) && !preferences.isEmpty()) {
			return new File(preferences, FILE);
		}
		return Gdx.files.local(FILE).file();
	}

	/** The player's file, or no keys of their own when there is none. */
	public static CustomKeys read(final File file, final boolean grid) {
		final CustomKeys keys = new CustomKeys(grid);
		if ((file != null) && file.isFile()) {
			try (InputStream in = new FileInputStream(file)) {
				keys.read(in);
			}
			catch (final IOException e) {
				LauncherLog.w("Unable to read " + file, e);
			}
		}
		return keys;
	}

	public void read(final InputStream in) throws IOException {
		final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
		Map<String, String> section = null;
		String line;
		while ((line = reader.readLine()) != null) {
			line = line.replace("\uFEFF", "").trim();
			if (line.isEmpty() || line.startsWith("//")) {
				continue;
			}
			if (line.startsWith("[") && line.endsWith("]")) {
				section = this.sections.computeIfAbsent(line.substring(1, line.length() - 1).trim(),
						name -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
				continue;
			}
			final int equals = line.indexOf('=');
			if ((section == null) || (equals <= 0)) {
				continue;
			}
			final String field = line.substring(0, equals).trim();
			if (HOTKEY.equalsIgnoreCase(field) || RESEARCH.equalsIgnoreCase(field) || OFF.equalsIgnoreCase(field)) {
				final String value = line.substring(equals + 1).trim();
				final int comma = value.indexOf(',');
				section.put(canonical(field), (comma >= 0 ? value.substring(0, comma) : value).trim());
			}
		}
		this.sections.values().removeIf(Map::isEmpty);
	}

	/** Writes the keys the player changed; with none the file is removed, which restores the game's keys. */
	public void write(final File file) throws IOException {
		if (this.sections.isEmpty()) {
			if (file.exists() && !file.delete()) {
				throw new IOException("Unable to delete " + file);
			}
			return;
		}
		try (Writer out = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
			for (final Map.Entry<String, Map<String, String>> section : this.sections.entrySet()) {
				out.write("[" + section.getKey() + "]\r\n");
				for (final Map.Entry<String, String> field : section.getValue().entrySet()) {
					out.write(field.getKey() + "=" + field.getValue() + "\r\n");
				}
				out.write("\r\n");
			}
		}
	}

	private static String canonical(final String field) {
		return RESEARCH.equalsIgnoreCase(field) ? RESEARCH : OFF.equalsIgnoreCase(field) ? OFF : HOTKEY;
	}

	public boolean grid() {
		return this.grid;
	}

	public void setGrid(final boolean grid) {
		this.grid = grid;
	}

	public boolean isEmpty() {
		return this.sections.isEmpty();
	}

	/** The player's own key of a command, or null. */
	public String own(final String section, final String field) {
		final Map<String, String> fields = this.sections.get(section);
		return fields == null ? null : fields.get(canonical(field));
	}

	public void set(final String section, final String field, final String key) {
		this.sections.computeIfAbsent(section, name -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER))
				.put(canonical(field), key);
	}

	public void remove(final String section, final String field) {
		final Map<String, String> fields = this.sections.get(section);
		if (fields != null) {
			fields.remove(canonical(field));
			if (fields.isEmpty()) {
				this.sections.remove(section);
			}
		}
	}

	public void clear() {
		this.sections.clear();
	}

	/**
	 * The key a command uses: the player's own, else in the grid layout the key of its button's place, else the game
	 * data's. Escape and keys of buttons outside the card stay as they are.
	 */
	public String key(final String section, final String field, final String dataKey, final int x, final int y) {
		final String own = section == null ? null : own(section, field);
		if (own != null) {
			return own;
		}
		if (this.grid && (dataKey != null) && !dataKey.isEmpty() && !keyCode(dataKey)) {
			final String place = gridKey(x, y);
			if (place != null) {
				return place;
			}
		}
		return dataKey;
	}

	/** The key of a command card place in the grid layout, or null outside the card. */
	public static String gridKey(final int x, final int y) {
		if ((y < 0) || (y >= GRID.length) || (x < 0) || (x >= GRID[y].length())) {
			return null;
		}
		return String.valueOf(GRID[y].charAt(x));
	}

	/** A key given by its number, as the game data gives Escape (512, or 27 in later data). */
	private static boolean keyCode(final String key) {
		if (key.length() < 2) {
			return false;
		}
		for (int i = 0; i < key.length(); i++) {
			if (!Character.isDigit(key.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/** How a key is shown: Escape by name, letters as capitals. */
	public static String shown(final String key) {
		if ((key == null) || key.isEmpty()) {
			return "";
		}
		if (keyCode(key)) {
			final int code = Integer.parseInt(key);
			return (code == 512) || (code == 27) ? "ESC" : String.valueOf((char) code).toUpperCase(Locale.ROOT);
		}
		return key.substring(0, 1).toUpperCase(Locale.ROOT);
	}

	/**
	 * A tooltip title with the command's key in it changed: the coloured key in brackets, "(|cffffcc00T|r)", gets the
	 * new key; a coloured letter inside the name, the older way, loses its colour and the new key follows the name in
	 * brackets. Other coloured parts, such as the level, stay.
	 */
	public static String retip(final String tip, final String dataKey, final String key) {
		if ((tip == null) || tip.isEmpty() || (key == null) || key.isEmpty() || shown(key).equals(shown(dataKey))) {
			return tip;
		}
		final String old = shown(dataKey);
		final String lower = tip.toLowerCase(Locale.ROOT);
		final String marker = KEY_COLOUR;
		int at = lower.indexOf(marker);
		while (at >= 0) {
			final int start = at + marker.length();
			final int end = lower.indexOf("|r", start);
			if (end < 0) {
				break;
			}
			final String coloured = tip.substring(start, end);
			if (!old.isEmpty() && coloured.equalsIgnoreCase(old)) {
				final boolean bracketed = (at > 0) && (tip.charAt(at - 1) == '(') && ((end + 2) < tip.length())
						&& (tip.charAt(end + 2) == ')');
				if (bracketed) {
					return tip.substring(0, start) + shown(key) + tip.substring(end);
				}
				final String plain = tip.substring(0, at) + coloured + tip.substring(end + 2);
				final int level = plain.indexOf(" - [");
				final int cut = level >= 0 ? level : plain.length();
				return plain.substring(0, cut) + " (" + marker + shown(key) + "|r)" + plain.substring(cut);
			}
			at = lower.indexOf(marker, end);
		}
		return tip;
	}

	/**
	 * A tooltip title with its key: titles of later game data name no key, and the game shows it after the name, as
	 * "Holy Light (|cffffcc00T|r) - [Level 1]"; titles that mark their key keep their own.
	 */
	public static String titled(final String tip, final String dataKey, final String key) {
		if ((tip == null) || tip.isEmpty() || (key == null) || key.isEmpty()) {
			return tip;
		}
		final String lower = tip.toLowerCase(Locale.ROOT);
		final String old = shown(dataKey);
		int at = lower.indexOf(KEY_COLOUR);
		while (at >= 0) {
			final int start = at + KEY_COLOUR.length();
			final int end = lower.indexOf("|r", start);
			if (end < 0) {
				break;
			}
			if (tip.substring(start, end).equalsIgnoreCase(old)) {
				return retip(tip, dataKey, key);
			}
			at = lower.indexOf(KEY_COLOUR, end);
		}
		final int level = tip.indexOf(" - [");
		final int cut = level >= 0 ? level : tip.length();
		return tip.substring(0, cut) + " (" + KEY_COLOUR + shown(key) + "|r)" + tip.substring(cut);
	}
}
