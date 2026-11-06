package com.etheller.warsmash.android;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.os.Build;
import android.util.Log;

/**
 * What the launcher and the engine tell each other: the Intent extras that start a game without the engine's menu,
 * and the result file the engine leaves when it gives the screen back. A game started from the launcher carries a
 * one-time token, so that Android restoring the engine's activity later does not start the game again by itself.
 */
final class LaunchContract {
	private static final String TAG = "Warsmash";
	/** Campaign number as scripts count campaigns (the named entries of the campaign list). */
	static final String EXTRA_CAMPAIGN = "campaign";
	static final String EXTRA_MISSION = "mission";
	/** easy, normal or hard. */
	static final String EXTRA_DIFFICULTY = "difficulty";
	/** Which version of the campaign maps is played: {@link #CAMPAIGN_MAPS_REFORGED} or classic (the default). */
	static final String EXTRA_CAMPAIGN_MAPS = "campaignmaps";
	static final String CAMPAIGN_MAPS_REFORGED = "reforged";
	static final String CAMPAIGN_MAPS_CLASSIC = "classic";
	/** A map of the custom game list, played with {@link #EXTRA_SLOTS} and {@link #EXTRA_OPTIONS}. */
	static final String EXTRA_CUSTOM_MAP = "custommap";
	static final String EXTRA_SLOTS = "slots";
	static final String EXTRA_OPTIONS = "options";
	/** A saved game of the current profile, by its name without the extension. */
	static final String EXTRA_SAVE_FILE = "savefile";
	/** The map the game starts on, whose loading screen the engine shows from its first frame on. */
	static final String EXTRA_MAP_FILE = "mapfile";
	/** The player profile the game is played with, by its name. */
	static final String EXTRA_PROFILE = "profile";
	static final String EXTRA_TOKEN = "launchToken";
	/** Given to the launcher when a game returns to it. */
	static final String EXTRA_RETURNED = "returned";

	static final String RESULT_FILE = "launcher-result.json";
	private static final String TOKEN_FILE = "launch-token";

	static final String SOURCE_CAMPAIGN = "campaign";
	static final String SOURCE_CUSTOM = "custom";
	static final String SOURCE_LOAD = "load";

	/**
	 * victory, defeat, tie, quit (left the game to the menu), exit (closed the program), error (not started) or crash
	 * (an exception ended the program).
	 */
	static final class Result {
		String result;
		String source;
		String map;
		String profile;
		String loadFailure;
		/** The race the player played, a random one as it was drawn; 0 when the engine did not tell. */
		int race;
		long finished;

		boolean failed() {
			return "error".equals(this.result) || (this.loadFailure != null);
		}

		boolean crashed() {
			return "crash".equals(this.result);
		}
	}

	private LaunchContract() {
	}

	static String newToken(final Context context) {
		final byte[] bytes = new byte[12];
		new SecureRandom().nextBytes(bytes);
		final StringBuilder token = new StringBuilder();
		for (final byte b : bytes) {
			token.append(String.format(Locale.ROOT, "%02x", b & 0xFF));
		}
		try {
			writeAtomically(new File(context.getFilesDir(), TOKEN_FILE), token.toString());
		}
		catch (final IOException e) {
			Log.e(TAG, "Unable to store the launch token", e);
		}
		return token.toString();
	}

	/** @return whether the token is the one the launcher gave out last; it cannot be used again */
	static boolean consumeToken(final Context context, final String token) {
		final File file = new File(context.getFilesDir(), TOKEN_FILE);
		if ((token == null) || !file.isFile()) {
			return false;
		}
		try {
			final String stored = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).trim();
			return token.equals(stored);
		}
		catch (final IOException e) {
			return false;
		}
		finally {
			file.delete();
		}
	}

	static void writeResult(final Context context, final Result result) {
		final JSONObject json = new JSONObject();
		try {
			json.put("version", 1);
			json.put("result", result.result);
			json.put("source", result.source);
			json.put("map", result.map);
			json.put("profile", result.profile);
			if (result.loadFailure != null) {
				json.put("loadFailure", result.loadFailure);
			}
			if (result.race > 0) {
				json.put("race", result.race);
			}
			json.put("finished", result.finished);
			writeAtomically(new File(context.getFilesDir(), RESULT_FILE), json.toString(2));
		}
		catch (final JSONException | IOException e) {
			Log.e(TAG, "Unable to write the game result", e);
		}
	}

	static boolean hasResult(final Context context) {
		return new File(context.getFilesDir(), RESULT_FILE).isFile();
	}

	/** Reads and removes the result file; null when there is none or it cannot be read. */
	static Result takeResult(final Context context) {
		final File file = new File(context.getFilesDir(), RESULT_FILE);
		if (!file.isFile()) {
			return null;
		}
		try {
			final JSONObject json = new JSONObject(
					new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
			final Result result = new Result();
			result.result = json.optString("result", "quit");
			result.source = json.optString("source", "");
			result.map = json.optString("map", "");
			result.profile = json.optString("profile", "");
			result.loadFailure = json.has("loadFailure") ? json.optString("loadFailure") : null;
			result.race = json.optInt("race");
			result.finished = json.optLong("finished");
			return result;
		}
		catch (final IOException | JSONException e) {
			Log.w(TAG, "Unable to read the game result", e);
			return null;
		}
		finally {
			file.delete();
		}
	}

	/**
	 * Whether the program ended by a crash after the given time, when a game left no result. A force stop, a swipe from
	 * the recent apps or the system reclaiming memory leave no result either, and are no crash; Android tells them
	 * apart from 11 on. Before that only a crash the engine caught itself (a crash result) can tell.
	 */
	static boolean crashedSince(final Context context, final long since) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
			return false;
		}
		final ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		if (manager == null) {
			return false;
		}
		try {
			final List<ApplicationExitInfo> exits = manager.getHistoricalProcessExitReasons(null, 0, 1);
			if (exits.isEmpty() || (exits.get(0).getTimestamp() < since)) {
				return false;
			}
			switch (exits.get(0).getReason()) {
			case ApplicationExitInfo.REASON_CRASH:
			case ApplicationExitInfo.REASON_CRASH_NATIVE:
			case ApplicationExitInfo.REASON_ANR:
			case ApplicationExitInfo.REASON_INITIALIZATION_FAILURE:
				return true;
			default:
				return false;
			}
		}
		catch (final RuntimeException e) {
			Log.w(TAG, "Unable to read how the game ended", e);
			return false;
		}
	}

	private static void writeAtomically(final File file, final String text) throws IOException {
		final File partial = new File(file.getParentFile(), file.getName() + ".part");
		Files.write(partial.toPath(), text.getBytes(StandardCharsets.UTF_8));
		if (!partial.renameTo(file)) {
			throw new IOException("Unable to store " + file);
		}
	}
}
