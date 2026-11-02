package com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.GameOptions;

/**
 * Where a player's saved games, game caches and campaign progress live: Save/&lt;profile&gt; in the application's own
 * storage (Gdx.files.local), which is the working folder on desktop and the private files folder on Android.
 */
public final class CSaveStorage {
	public static final String SAVE_EXTENSION = ".w3z";
	private static final String CURRENT_PROFILE_FALLBACK = "Player";
	private static String profile = CURRENT_PROFILE_FALLBACK;

	private CSaveStorage() {
	}

	public static void setProfile(final String profileName) {
		final String safe = safeName(profileName);
		final String next = safe.isEmpty() ? CURRENT_PROFILE_FALLBACK : safe;
		if (!next.equals(profile)) {
			profile = next;
			CGameCache.forgetLoaded();
			CCampaignProgress.forgetLoaded();
			GameOptions.reload();
		}
	}

	public static FileHandle profileFile(final String name) {
		return Gdx.files.local("Save/" + profile + "/" + name);
	}

	public static FileHandle savedGame(final String name) {
		return profileFile(fileNameOf(name));
	}

	/**
	 * A script's name for a save can have folders and its own extension; only the last part names the file.
	 */
	private static String fileNameOf(final String name) {
		String baseName = name.replace('\\', '/');
		baseName = baseName.substring(baseName.lastIndexOf('/') + 1);
		if (baseName.toLowerCase().endsWith(SAVE_EXTENSION)) {
			baseName = baseName.substring(0, baseName.length() - SAVE_EXTENSION.length());
		}
		return safeName(baseName) + SAVE_EXTENSION;
	}

	/** Names as typed by a player must stay inside the save folder and be legal on every file system. */
	public static String safeName(final String name) {
		return name.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1f]", "_").replaceAll("^\\.+", "").trim();
	}

	public static List<String> savedGameNames() {
		final List<String> names = new ArrayList<>();
		final FileHandle folder = profileFile("");
		if (folder.isDirectory()) {
			for (final FileHandle file : folder.list(SAVE_EXTENSION)) {
				names.add(file.nameWithoutExtension());
			}
		}
		Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
		return names;
	}

	public interface Writer {
		void write(DataOutputStream out) throws IOException;
	}

	public interface Reader {
		void read(DataInputStream in) throws IOException;
	}

	/** Written beside the target first: a crash or a full disk must not take the previous file with it. */
	public static boolean writeAtomically(final FileHandle file, final Writer writer) {
		final FileHandle temporary = file.sibling(file.name() + ".tmp");
		try {
			file.parent().mkdirs();
			try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(temporary.write(false)))) {
				writer.write(out);
			}
			temporary.moveTo(file);
			return true;
		}
		catch (final Exception exc) {
			exc.printStackTrace();
			if (temporary.exists()) {
				temporary.delete();
			}
			return false;
		}
	}

	public static boolean read(final FileHandle file, final Reader reader) {
		if (!file.exists()) {
			return false;
		}
		try (DataInputStream in = new DataInputStream(new BufferedInputStream(file.read()))) {
			reader.read(in);
			return true;
		}
		catch (final Exception exc) {
			exc.printStackTrace();
			return false;
		}
	}
}
