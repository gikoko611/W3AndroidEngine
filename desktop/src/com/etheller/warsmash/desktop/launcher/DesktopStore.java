package com.etheller.warsmash.desktop.launcher;

import com.badlogic.gdx.Preferences;
import com.etheller.warsmash.launcher.LauncherStore;

/** The launcher's stores on the desktop: libGDX preferences, as the engine keeps its own. */
final class DesktopStore implements LauncherStore {
	private final Preferences preferences;

	DesktopStore(final Preferences preferences) {
		this.preferences = preferences;
	}

	@Override
	public String getString(final String key, final String fallback) {
		return this.preferences.contains(key) ? this.preferences.getString(key) : fallback;
	}

	@Override
	public int getInt(final String key, final int fallback) {
		return this.preferences.getInteger(key, fallback);
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
		return this.preferences.get().get(key);
	}

	@Override
	public Edit edit() {
		return new Edit() {
			@Override
			public Edit putString(final String key, final String value) {
				DesktopStore.this.preferences.putString(key, value);
				return this;
			}

			@Override
			public Edit putInt(final String key, final int value) {
				DesktopStore.this.preferences.putInteger(key, value);
				return this;
			}

			@Override
			public Edit putLong(final String key, final long value) {
				DesktopStore.this.preferences.putLong(key, value);
				return this;
			}

			@Override
			public Edit putBoolean(final String key, final boolean value) {
				DesktopStore.this.preferences.putBoolean(key, value);
				return this;
			}

			@Override
			public Edit remove(final String key) {
				DesktopStore.this.preferences.remove(key);
				return this;
			}

			@Override
			public void commit() {
				DesktopStore.this.preferences.flush();
			}

			@Override
			public void apply() {
				DesktopStore.this.preferences.flush();
			}
		};
	}
}
