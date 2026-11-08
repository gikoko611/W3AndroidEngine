package com.etheller.warsmash.launcher;

/**
 * A small store of named values that outlives the program: Android's shared preferences, or the libGDX preferences
 * on the desktop. The engine's own profile list is one such store, the launcher's choices another.
 */
public interface LauncherStore {
	String getString(String key, String fallback);

	int getInt(String key, int fallback);

	long getLong(String key, long fallback);

	boolean getBoolean(String key, boolean fallback);

	boolean contains(String key);

	/** The stored value as it was put (String, Integer, Long or Boolean), or null. */
	Object get(String key);

	Edit edit();

	/** Changes that take effect together. */
	interface Edit {
		Edit putString(String key, String value);

		Edit putInt(String key, int value);

		Edit putLong(String key, long value);

		Edit putBoolean(String key, boolean value);

		Edit remove(String key);

		/** Written before it returns, for values another process (the engine) reads next. */
		void commit();

		/** Written soon; the store reads the new values at once. */
		void apply();
	}
}
