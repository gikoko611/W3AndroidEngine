package com.etheller.warsmash.android;

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

import android.content.Context;
import android.util.Log;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CSaveStorage;

/**
 * The engine's own settings as the launcher's Options page changes them, kept where the engine reads them: the Options
 * menu values in Options.dat of the current profile (the file and format of GameOptions, in the private files folder
 * that Gdx.files.local is on Android), and the touch thresholds in the [Touch] section of warsmash.ini.
 */
final class EngineSettings {
	private static final String TAG = "Warsmash";
	/** The libGDX preferences the engine keeps its profiles in, and the profile it starts with without one. */
	private static final String ENGINE_PREFERENCES = "WarsmashWC3Engine";
	private static final String CURRENT_PROFILE = "CurrentProfile";
	private static final String DEFAULT_PROFILE = "WorldEdit";
	private static final String TOUCH = "Touch";

	private final File options;
	private final File ini;
	private final Map<String, Integer> values = new LinkedHashMap<>();

	EngineSettings(final Context context) {
		final String profile = context.getSharedPreferences(ENGINE_PREFERENCES, Context.MODE_PRIVATE)
				.getString(CURRENT_PROFILE, DEFAULT_PROFILE);
		final String safe = CSaveStorage.safeName(profile);
		this.options = new File(new File(new File(context.getFilesDir(), "Save"), safe.isEmpty() ? "Player" : safe),
				"Options.dat");
		this.ini = SkinCache.iniFile(context);
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
			Log.w(TAG, "Unable to read " + this.options, e);
		}
	}

	/** Defaults are the engine's own, for a value the player never set. */
	int number(final String key, final int fallback) {
		final Integer value = this.values.get(key);
		return value == null ? fallback : value;
	}

	boolean flag(final String key, final boolean fallback) {
		final Integer value = this.values.get(key);
		return value == null ? fallback : value != 0;
	}

	void setNumber(final String key, final int value) {
		this.values.put(key, value);
		save();
	}

	void setFlag(final String key, final boolean value) {
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
			Log.e(TAG, "Unable to write " + this.options, e);
			partial.delete();
		}
	}

	/** A [Touch] value of warsmash.ini, or the fallback when the file leaves it out. */
	String touch(final String key, final String fallback) {
		String section = null;
		for (final String line : iniLines()) {
			final String trimmed = line.trim();
			if (trimmed.startsWith("[")) {
				section = trimmed;
			}
			else if (isTouch(section) && trimmed.startsWith(key + "=")) {
				return trimmed.substring(key.length() + 1).trim();
			}
		}
		return fallback;
	}

	/** Sets a [Touch] value of warsmash.ini, adding the section if the file has none. */
	void setTouch(final String key, final String value) {
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
				if (isTouch(trimmed)) {
					section = i;
				}
			}
		}
		final String entry = key + "=" + value;
		if (section < 0) {
			lines.add("");
			lines.add("[" + TOUCH + "]");
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
			Log.e(TAG, "Unable to write " + this.ini, e);
			partial.delete();
		}
	}

	private static boolean isTouch(final String header) {
		return (header != null) && header.toLowerCase(Locale.ROOT).equals("[" + TOUCH.toLowerCase(Locale.ROOT) + "]");
	}

	private List<String> iniLines() {
		try {
			return new ArrayList<>(Files.readAllLines(this.ini.toPath(), StandardCharsets.UTF_8));
		}
		catch (final IOException e) {
			Log.w(TAG, "Unable to read " + this.ini, e);
			return new ArrayList<>();
		}
	}

	/** The [Touch] section of an engine configuration, so that a new configuration can keep it. */
	static String touchSection(final File ini) {
		if (!ini.isFile()) {
			return "";
		}
		final StringBuilder section = new StringBuilder();
		boolean inside = false;
		try {
			for (final String line : Files.readAllLines(ini.toPath(), StandardCharsets.UTF_8)) {
				final String trimmed = line.trim();
				if (trimmed.startsWith("[")) {
					inside = isTouch(trimmed);
				}
				if (inside && !trimmed.isEmpty()) {
					section.append(line).append('\n');
				}
			}
		}
		catch (final IOException e) {
			Log.w(TAG, "Unable to read " + ini, e);
			return "";
		}
		return section.toString();
	}
}
