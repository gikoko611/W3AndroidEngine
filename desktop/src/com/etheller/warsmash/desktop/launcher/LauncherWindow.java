package com.etheller.warsmash.desktop.launcher;

import java.io.File;

import org.lwjgl.opengl.Display;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglFileHandle;
import com.badlogic.gdx.backends.lwjgl.LwjglPreferences;

/** The launcher's window: fullscreen or a window of the size and place the player left it at. */
public final class LauncherWindow {
	private LauncherWindow() {
	}

	/** The window the program opens with, from the launcher's preferences; before the application starts. */
	public static void configure(final LwjglApplicationConfiguration config, final boolean forceWindow) {
		final Preferences preferences = new LwjglPreferences(new LwjglFileHandle(
				new File(config.preferencesDirectory, LauncherScreen.PREFERENCES), config.preferencesFileType));
		final DisplayMode desktop = LwjglApplicationConfiguration.getDesktopDisplayMode();
		config.fullscreen = !forceWindow && preferences.getBoolean(LauncherScreen.KEY_FULLSCREEN, false);
		if (config.fullscreen) {
			config.width = desktop.width;
			config.height = desktop.height;
			return;
		}
		final int[] stored = stored(preferences);
		if (stored != null) {
			config.width = Math.min(desktop.width, Math.max(LauncherScreen.MIN_WIDTH, stored[2]));
			config.height = Math.min(desktop.height, Math.max(LauncherScreen.MIN_HEIGHT, stored[3]));
			if ((stored[0] >= 0) && (stored[1] >= 0) && ((stored[0] + 100) < desktop.width)
					&& ((stored[1] + 100) < desktop.height)) {
				config.x = stored[0];
				config.y = stored[1];
			}
		}
		else {
			// A first start: most of the screen, in the 16:9 the pages are drawn for, and never below 1280x720.
			config.width = Math.max(LauncherScreen.MIN_WIDTH, Math.min(desktop.width - 160, ((desktop.height - 160) * 16) / 9));
			config.height = Math.max(LauncherScreen.MIN_HEIGHT, (config.width * 9) / 16);
			config.width = Math.min(config.width, desktop.width);
			config.height = Math.min(config.height, desktop.height);
		}
	}

	private static int[] stored(final Preferences preferences) {
		final String[] parts = preferences.getString(LauncherScreen.KEY_WINDOW, "").split(",");
		if (parts.length != 4) {
			return null;
		}
		try {
			final int[] values = new int[4];
			for (int i = 0; i < 4; i++) {
				values[i] = Integer.parseInt(parts[i].trim());
			}
			return values;
		}
		catch (final NumberFormatException e) {
			return null;
		}
	}

	/** Keeps the window's place and size for the next start; a fullscreen window keeps the last window's. */
	static void remember(final Preferences preferences) {
		if (Gdx.graphics.isFullscreen() || !Display.isCreated()) {
			return;
		}
		final String value = Display.getX() + "," + Display.getY() + "," + Gdx.graphics.getWidth() + ","
				+ Gdx.graphics.getHeight();
		if (!value.equals(preferences.getString(LauncherScreen.KEY_WINDOW, ""))) {
			preferences.putString(LauncherScreen.KEY_WINDOW, value);
			preferences.flush();
		}
	}

	static void setFullscreen(final Preferences preferences, final boolean fullscreen) {
		preferences.putBoolean(LauncherScreen.KEY_FULLSCREEN, fullscreen);
		preferences.flush();
		if (fullscreen) {
			remember(preferences);
			Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
			return;
		}
		final int[] stored = stored(preferences);
		final DisplayMode desktop = Gdx.graphics.getDisplayMode();
		final int width = stored == null ? Math.max(LauncherScreen.MIN_WIDTH, (desktop.width * 3) / 4) : stored[2];
		final int height = stored == null ? Math.max(LauncherScreen.MIN_HEIGHT, (desktop.height * 3) / 4) : stored[3];
		Gdx.graphics.setWindowedMode(Math.min(desktop.width, width), Math.min(desktop.height, height));
		if ((stored != null) && (stored[0] >= 0) && (stored[1] >= 0)) {
			Display.setLocation(stored[0], stored[1]);
		}
	}
}
