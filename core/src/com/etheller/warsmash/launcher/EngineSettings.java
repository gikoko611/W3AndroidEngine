package com.etheller.warsmash.launcher;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.etheller.warsmash.datasources.GraphicsSettings;

/**
 * The engine's own settings as the launcher's Options page changes them, kept where the engine reads them: the Options
 * menu values in Options.dat of the current profile (the file and format of GameOptions, below Gdx.files.local), and
 * the touch thresholds and the in-game graphics in the [Touch] and [Graphics] sections of warsmash.ini.
 */
public class EngineSettings {
	private static final String TOUCH = "Touch";
	/** The sections the launcher writes, which a new configuration for other game data keeps. */
	private static final String[] OWN_SECTIONS = { TOUCH, GraphicsSettings.SECTION };

	private final File options;
	private final File ini;
	private final Map<String, Integer> values = new LinkedHashMap<>();

	/**
	 * @param saves   the engine's Save folder
	 * @param profile the current profile, whose Options.dat the engine reads
	 * @param ini     the engine configuration
	 */
	public EngineSettings(final File saves, final String profile, final File ini) {
		this.options = new File(Profiles.folder(saves, profile), "Options.dat");
		this.ini = ini;
		read();
	}

	private void read() {
		if (!this.options.isFile()) {
			return;
		}
		try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(this.options)))) {
			final int count = in.readInt();
			for (int i = 0; i < count; i++) {
				final String key = in.readUTF();
				this.values.put(key, in.readInt());
			}
		}
		catch (final IOException e) {
			LauncherLog.w("Unable to read " + this.options, e);
		}
	}

	/** Defaults are the engine's own, for a value the player never set. */
	public int number(final String key, final int fallback) {
		final Integer value = this.values.get(key);
		return value == null ? fallback : value;
	}

	public boolean flag(final String key, final boolean fallback) {
		final Integer value = this.values.get(key);
		return value == null ? fallback : value != 0;
	}

	public void setNumber(final String key, final int value) {
		this.values.put(key, value);
		save();
	}

	public void setFlag(final String key, final boolean value) {
		setNumber(key, value ? 1 : 0);
	}

	/** Written beside the file first, as the engine does, so that a full disk cannot take the old settings. */
	private void save() {
		final File partial = new File(this.options.getParentFile(), this.options.getName() + ".tmp");
		try {
			this.options.getParentFile().mkdirs();
			try (DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(partial)))) {
				out.writeInt(this.values.size());
				for (final Map.Entry<String, Integer> entry : this.values.entrySet()) {
					out.writeUTF(entry.getKey());
					out.writeInt(entry.getValue());
				}
			}
			if (!partial.renameTo(this.options)) {
				throw new IOException("Unable to store " + this.options);
			}
		}
		catch (final IOException e) {
			LauncherLog.e("Unable to write " + this.options, e);
			partial.delete();
		}
	}

	/** A [Touch] value of warsmash.ini, or the fallback when the file leaves it out. */
	public String touch(final String key, final String fallback) {
		return iniValue(TOUCH, key, fallback);
	}

	/** Sets a [Touch] value of warsmash.ini, adding the section if the file has none. */
	public void setTouch(final String key, final String value) {
		setIniValue(TOUCH, key, value);
	}

	/** A [Graphics] value of warsmash.ini, or the fallback when the file leaves it out. */
	public String graphics(final String key, final String fallback) {
		return iniValue(GraphicsSettings.SECTION, key, fallback);
	}

	public void setGraphics(final String key, final String value) {
		setIniValue(GraphicsSettings.SECTION, key, value);
	}

	private String iniValue(final String name, final String key, final String fallback) {
		String section = null;
		for (final String line : iniLines()) {
			final String trimmed = line.trim();
			if (trimmed.startsWith("[")) {
				section = trimmed;
			}
			else if (isSection(section, name) && trimmed.startsWith(key + "=")) {
				return trimmed.substring(key.length() + 1).trim();
			}
		}
		return fallback;
	}

	private void setIniValue(final String name, final String key, final String value) {
		final List<String> lines = iniLines();
		int section = -1;
		int end = lines.size();
		for (int i = 0; i < lines.size(); i++) {
			final String trimmed = lines.get(i).trim();
			if (trimmed.startsWith("[")) {
				if (section >= 0) {
					end = i;
					break;
				}
				if (isSection(trimmed, name)) {
					section = i;
				}
			}
		}
		final String entry = key + "=" + value;
		if (section < 0) {
			lines.add("");
			lines.add("[" + name + "]");
			lines.add(entry);
		}
		else {
			boolean replaced = false;
			for (int i = section + 1; i < end; i++) {
				if (lines.get(i).trim().startsWith(key + "=")) {
					lines.set(i, entry);
					replaced = true;
				}
			}
			if (!replaced) {
				int at = end;
				while ((at > (section + 1)) && lines.get(at - 1).trim().isEmpty()) {
					at--;
				}
				lines.add(at, entry);
			}
		}
		final File partial = new File(this.ini.getParentFile(), this.ini.getName() + ".part");
		try {
			Files.write(partial.toPath(), lines, StandardCharsets.UTF_8);
			if (!partial.renameTo(this.ini)) {
				throw new IOException("Unable to store " + this.ini);
			}
		}
		catch (final IOException e) {
			LauncherLog.e("Unable to write " + this.ini, e);
			partial.delete();
		}
	}

	private static boolean isSection(final String header, final String name) {
		return (header != null) && header.toLowerCase(Locale.ROOT).equals("[" + name.toLowerCase(Locale.ROOT) + "]");
	}

	private static boolean isOwnSection(final String header) {
		for (final String name : OWN_SECTIONS) {
			if (isSection(header, name)) {
				return true;
			}
		}
		return false;
	}

	private List<String> iniLines() {
		try {
			return new ArrayList<>(Files.readAllLines(this.ini.toPath(), StandardCharsets.UTF_8));
		}
		catch (final IOException e) {
			LauncherLog.w("Unable to read " + this.ini, e);
			return new ArrayList<>();
		}
	}

	/** The [Touch] and [Graphics] sections of an engine configuration, so that a new configuration can keep them. */
	public static String ownSections(final File ini) {
		if (!ini.isFile()) {
			return "";
		}
		final StringBuilder section = new StringBuilder();
		boolean inside = false;
		try {
			for (final String line : Files.readAllLines(ini.toPath(), StandardCharsets.UTF_8)) {
				final String trimmed = line.trim();
				if (trimmed.startsWith("[")) {
					inside = isOwnSection(trimmed);
				}
				if (inside && !trimmed.isEmpty()) {
					section.append(line).append('\n');
				}
			}
		}
		catch (final IOException e) {
			LauncherLog.w("Unable to read " + ini, e);
			return "";
		}
		return section.toString();
	}
}
