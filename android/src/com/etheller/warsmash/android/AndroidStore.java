package com.etheller.warsmash.android;

import android.content.SharedPreferences;

import com.etheller.warsmash.launcher.LauncherStore;

/** Shared preferences as the launcher's stores. */
final class AndroidStore implements LauncherStore {
	private final SharedPreferences preferences;

	AndroidStore(final SharedPreferences preferences) {
		this.preferences = preferences;
	}

	@Override
	public String getString(final String key, final String fallback) {
		return this.preferences.getString(key, fallback);
	}

	@Override
	public int getInt(final String key, final int fallback) {
		return this.preferences.getInt(key, fallback);
	}

	@Override
	public long getLong(final String key, final long fallback) {
		return this.preferences.getLong(key, fallback);
	}

	@Override
	public boolean getBoolean(final String key, final boolean fallback) {
		return this.preferences.getBoolean(key, fallback);
	}

	@Override
	public boolean contains(final String key) {
		return this.preferences.contains(key);
	}

	@Override
	public Object get(final String key) {
		return this.preferences.getAll().get(key);
	}

	@Override
	public Edit edit() {
		final SharedPreferences.Editor editor = this.preferences.edit();
		return new Edit() {
			@Override
			public Edit putString(final String key, final String value) {
				editor.putString(key, value);
				return this;
			}

			@Override
			public Edit putInt(final String key, final int value) {
				editor.putInt(key, value);
				return this;
			}

			@Override
			public Edit putLong(final String key, final long value) {
				editor.putLong(key, value);
				return this;
			}

			@Override
			public Edit putBoolean(final String key, final boolean value) {
				editor.putBoolean(key, value);
				return this;
			}

			@Override
			public Edit remove(final String key) {
				editor.remove(key);
				return this;
			}

			@Override
			public void commit() {
				editor.commit();
			}

			@Override
			public void apply() {
				editor.apply();
			}
		};
	}
}
