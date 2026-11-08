package com.etheller.warsmash.android;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;

/** The profiles as the launcher keeps them on Android: the engine's preferences and Save in the private files. */
final class Profiles extends com.etheller.warsmash.launcher.Profiles {
	Profiles(final Context context, final SharedPreferences launcher) {
		super(new AndroidStore(context.getSharedPreferences(ENGINE_PREFERENCES, Context.MODE_PRIVATE)),
				new AndroidStore(launcher), saves(context));
	}

	/** Save in the private files, which Gdx.files.local is on Android. */
	static File saves(final Context context) {
		return new File(context.getFilesDir(), "Save");
	}
}
