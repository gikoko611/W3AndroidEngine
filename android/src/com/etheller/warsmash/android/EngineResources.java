package com.etheller.warsmash.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Keeps the engine's own resources folder in step with the installed APK. The engine reads them as plain files, so
 * they are unpacked from the APK assets, again whenever the installed package changes.
 */
final class EngineResources {
	private static final String TAG = "Warsmash";
	private static final String ASSET_ROOT = "engineResources";
	static final String FOLDER = "resources";
	/**
	 * Kept beside the folder rather than inside it, so that the engine never sees it as data and a wiped folder is
	 * noticed.
	 */
	private static final String MANIFEST = "resources.manifest";

	private EngineResources() {
	}

	static File folder(final Context context) {
		return new File(context.getExternalFilesDir(null), FOLDER);
	}

	/**
	 * Unpacks the resources if the installed package differs from the one that last unpacked them, and removes files
	 * that an earlier package unpacked but this one no longer ships.
	 */
	static void ensureCurrent(final Context context) throws IOException {
		final File appFolder = context.getExternalFilesDir(null);
		final File target = new File(appFolder, FOLDER);
		final File manifest = new File(appFolder, MANIFEST);
		final String stamp = packageStamp(context);
		final List<String> previous = readManifest(manifest);
		if ((previous != null) && !previous.isEmpty() && previous.get(0).equals(stamp) && target.isDirectory()) {
			return;
		}
		final Set<String> shipped = new LinkedHashSet<>();
		listAssets(context, ASSET_ROOT, "", shipped);
		final Set<String> stale = new LinkedHashSet<>();
		if (previous != null) {
			stale.addAll(previous.subList(Math.min(1, previous.size()), previous.size()));
		}
		else {
			// Builds before the manifest left no record; what they unpacked is under the folders the APK ships, while
			// other folders (ProbeMaps, maps put there by hand) were never ours.
			for (final String top : topFolders(shipped)) {
				listFiles(new File(target, top), top, stale);
			}
		}
		stale.removeAll(shipped);
		if (manifest.exists() && !manifest.delete()) {
			throw new IOException("Unable to reset " + manifest);
		}
		for (final String path : shipped) {
			copyAsset(context, ASSET_ROOT + "/" + path, new File(target, path));
		}
		for (final String path : stale) {
			final File file = new File(target, path);
			if (file.isFile() && file.delete()) {
				pruneEmptyParents(file.getParentFile(), target);
			}
		}
		final List<String> lines = new ArrayList<>();
		lines.add(stamp);
		lines.addAll(shipped);
		final File partial = new File(appFolder, MANIFEST + ".part");
		Files.write(partial.toPath(), lines, StandardCharsets.UTF_8);
		if (!partial.renameTo(manifest)) {
			throw new IOException("Unable to store " + manifest);
		}
		Log.i(TAG, "Engine resources unpacked for " + stamp + ": " + shipped.size() + " files, " + stale.size()
				+ " removed");
	}

	/**
	 * Debug builds keep the same version code, so the install time is part of the stamp too.
	 */
	@SuppressWarnings("deprecation")
	private static String packageStamp(final Context context) throws IOException {
		try {
			final PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return info.versionCode + "/" + info.versionName + "/" + info.lastUpdateTime;
		}
		catch (final PackageManager.NameNotFoundException e) {
			throw new IOException("Own package not found", e);
		}
	}

	private static List<String> readManifest(final File manifest) throws IOException {
		if (!manifest.isFile()) {
			return null;
		}
		return Files.readAllLines(manifest.toPath(), StandardCharsets.UTF_8);
	}

	private static void listAssets(final Context context, final String assetPath, final String relative,
			final Set<String> paths) throws IOException {
		final String[] children = context.getAssets().list(assetPath);
		if ((children == null) || (children.length == 0)) {
			paths.add(relative);
			return;
		}
		for (final String child : children) {
			listAssets(context, assetPath + "/" + child, relative.isEmpty() ? child : relative + "/" + child, paths);
		}
	}

	private static Set<String> topFolders(final Set<String> paths) {
		final Set<String> folders = new LinkedHashSet<>();
		for (final String path : paths) {
			final int slash = path.indexOf('/');
			if (slash > 0) {
				folders.add(path.substring(0, slash));
			}
		}
		return folders;
	}

	private static void listFiles(final File folder, final String relative, final Set<String> paths) {
		final File[] children = folder.listFiles();
		if (children == null) {
			return;
		}
		for (final File child : children) {
			final String path = relative.isEmpty() ? child.getName() : relative + "/" + child.getName();
			if (child.isDirectory()) {
				listFiles(child, path, paths);
			}
			else {
				paths.add(path);
			}
		}
	}

	private static void copyAsset(final Context context, final String assetPath, final File target)
			throws IOException {
		target.getParentFile().mkdirs();
		try (InputStream in = context.getAssets().open(assetPath);
				OutputStream out = new FileOutputStream(target)) {
			final byte[] buffer = new byte[8192];
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
		}
	}

	private static void pruneEmptyParents(File folder, final File root) {
		while ((folder != null) && !folder.equals(root)) {
			final String[] left = folder.list();
			if ((left == null) || (left.length > 0) || !folder.delete()) {
				return;
			}
			folder = folder.getParentFile();
		}
	}
}
