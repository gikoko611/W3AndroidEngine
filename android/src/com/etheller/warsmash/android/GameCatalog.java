package com.etheller.warsmash.android;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import com.etheller.warsmash.launcher.LauncherImages;
import com.etheller.warsmash.launcher.Profiles;
import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.util.ArgbImage;

/** The launcher's catalog on Android: the private files, the cache folder and answers on the main thread. */
final class GameCatalog extends com.etheller.warsmash.launcher.GameCatalog {
	GameCatalog(final Context context, final DataTable ini, final String dataKey) {
		this(context.getApplicationContext(), ini, dataKey, new Handler(Looper.getMainLooper()));
	}

	private GameCatalog(final Context context, final DataTable ini, final String dataKey, final Handler main) {
		super(ini, dataKey, com.etheller.warsmash.android.Profiles.saves(context), () -> currentProfile(context),
				new File(new File(context.getCacheDir(), "catalog"), "maps.txt"), main::post);
	}

	/** The engine's current player profile, as its menu chose it (the engine's preferences). */
	static String currentProfile(final Context context) {
		return currentProfile(
				new AndroidStore(context.getSharedPreferences(Profiles.ENGINE_PREFERENCES, Context.MODE_PRIVATE)));
	}

	/** A picture of the game data as a bitmap; null stays null. */
	static Bitmap bitmap(final ArgbImage image) {
		if (image == null) {
			return null;
		}
		final Bitmap bitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
		// setPixels takes straight colours and multiplies them by alpha as bitmaps keep them.
		bitmap.setPixels(LauncherImages.pixels(image), 0, image.getWidth(), 0, 0, image.getWidth(),
				image.getHeight());
		return bitmap;
	}
}
