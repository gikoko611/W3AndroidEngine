package com.etheller.warsmash.android;

import android.content.Context;

/** The engine settings of the current profile, in the private files and the configuration on Android. */
final class EngineSettings extends com.etheller.warsmash.launcher.EngineSettings {
	EngineSettings(final Context context) {
		super(Profiles.saves(context), GameCatalog.currentProfile(context), SkinCache.iniFile(context));
	}
}
