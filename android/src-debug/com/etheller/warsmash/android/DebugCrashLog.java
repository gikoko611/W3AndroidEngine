package com.etheller.warsmash.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.etheller.warsmash.WarsmashGdxMapScreen;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Debug;
import android.os.Process;
import android.util.Log;

/**
 * Debug builds only: a provider starts with the process, before any activity, and leaves a report in
 * files/crash/&lt;time&gt;.txt for every exception no thread caught (an OutOfMemoryError included): the stack trace,
 * the heap state, the screen and map, the GL errors seen and the end of the process's own log.
 */
public final class DebugCrashLog extends ContentProvider {
	private static final String TAG = "WarsmashCrash";
	private static final int LOG_LINES = 2000;
	private static final int GL_ERRORS_KEPT = 50;
	private static final long GL_CHECK_MILLIS = 1000;
	private static final ArrayDeque<String> GL_ERRORS = new ArrayDeque<>();
	// A game that has closed runs no more frames; one check at a time keeps checks from piling up behind it.
	private static final AtomicBoolean GL_CHECK_PENDING = new AtomicBoolean();
	// Freed when the report is written, so that writing it after an OutOfMemoryError has room.
	private static byte[] reserve;

	@Override
	public boolean onCreate() {
		final File dir = new File(getContext().getFilesDir(), "crash");
		reserve = new byte[4 << 20];
		final Thread.UncaughtExceptionHandler next = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
			reserve = null;
			try {
				write(dir, thread, error);
			}
			catch (final Throwable e) {
				Log.e(TAG, "Unable to write the crash report", e);
			}
			if (next != null) {
				next.uncaughtException(thread, error);
			}
		});
		startGlErrorCheck();
		return true;
	}

	/** glGetError waits for the GPU, so it is asked once a second rather than every frame. */
	private static void startGlErrorCheck() {
		final Thread checker = new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(GL_CHECK_MILLIS);
				}
				catch (final InterruptedException e) {
					return;
				}
				checkGlErrors();
			}
		}, "DebugGlErrorCheck");
		checker.setDaemon(true);
		checker.start();
	}

	// Only the identity of the game is kept between checks: a reference would keep a closed game's activity, and
	// with it the whole game, on the heap.
	private static int checkedApp;

	private static void checkGlErrors() {
		final Application app = Gdx.app;
		final int appId = System.identityHashCode(app);
		if (appId != checkedApp) {
			checkedApp = appId;
			GL_CHECK_PENDING.set(false);
		}
		if ((app == null) || (Gdx.gl == null) || !GL_CHECK_PENDING.compareAndSet(false, true)) {
			return;
		}
		app.postRunnable(() -> {
			GL_CHECK_PENDING.set(false);
			final int error = Gdx.gl.glGetError();
			if (error != GL20.GL_NO_ERROR) {
				final String line = time() + " GL error 0x" + Integer.toHexString(error) + " on " + describeScreen();
				Log.w(TAG, line);
				synchronized (GL_ERRORS) {
					if (GL_ERRORS.size() >= GL_ERRORS_KEPT) {
						GL_ERRORS.removeFirst();
					}
					GL_ERRORS.addLast(line);
				}
			}
		});
	}

	private static void write(final File dir, final Thread thread, final Throwable error) throws Exception {
		dir.mkdirs();
		final File file = new File(dir,
				new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date()) + ".txt");
		try (PrintWriter out = new PrintWriter(
				new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
			out.println("time: " + time());
			out.println("thread: " + thread.getName());
			out.println("screen: " + describeScreen());
			final Runtime runtime = Runtime.getRuntime();
			final long used = runtime.totalMemory() - runtime.freeMemory();
			out.println("java heap: used " + mb(used) + ", total " + mb(runtime.totalMemory()) + ", max "
					+ mb(runtime.maxMemory()));
			out.println("native heap: allocated " + mb(Debug.getNativeHeapAllocatedSize()) + ", size "
					+ mb(Debug.getNativeHeapSize()));
			final Debug.MemoryInfo memory = new Debug.MemoryInfo();
			Debug.getMemoryInfo(memory);
			out.println("pss: total " + memory.getTotalPss() + " KB, java " + memory.getMemoryStat("summary.java-heap")
					+ " KB, native " + memory.getMemoryStat("summary.native-heap") + " KB, graphics "
					+ memory.getMemoryStat("summary.graphics") + " KB");
			out.println();
			final StringWriter trace = new StringWriter();
			error.printStackTrace(new PrintWriter(trace));
			out.println(trace);
			out.println("GL errors (newest last):");
			synchronized (GL_ERRORS) {
				for (final String line : GL_ERRORS) {
					out.println(line);
				}
			}
			out.println();
			out.println("log (this process, last " + LOG_LINES + " lines):");
			out.flush();
			final java.lang.Process logcat = new ProcessBuilder("logcat", "-d", "-v", "threadtime", "-t",
					Integer.toString(LOG_LINES), "--pid=" + Process.myPid()).redirectErrorStream(true).start();
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(logcat.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					out.println(line);
				}
			}
		}
		Log.e(TAG, "Crash report: " + file);
	}

	private static String describeScreen() {
		try {
			final Application app = Gdx.app;
			if (app == null) {
				return "no game";
			}
			final ApplicationListener listener = app.getApplicationListener();
			if (!(listener instanceof Game)) {
				return String.valueOf(listener);
			}
			final Screen screen = ((Game) listener).getScreen();
			if (screen instanceof WarsmashGdxMapScreen) {
				final War3MapViewer viewer = ((WarsmashGdxMapScreen) screen).getViewer();
				return "map " + (viewer == null ? null : viewer.getMapFileName());
			}
			return screen == null ? "none" : screen.getClass().getSimpleName();
		}
		catch (final Throwable e) {
			return "unknown (" + e + ")";
		}
	}

	private static String time() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT).format(new Date());
	}

	private static String mb(final long bytes) {
		return (bytes >> 20) + " MB";
	}

	@Override
	public Cursor query(final Uri uri, final String[] projection, final String selection,
			final String[] selectionArgs, final String sortOrder) {
		return null;
	}

	@Override
	public String getType(final Uri uri) {
		return null;
	}

	@Override
	public Uri insert(final Uri uri, final ContentValues values) {
		return null;
	}

	@Override
	public int delete(final Uri uri, final String selection, final String[] selectionArgs) {
		return 0;
	}

	@Override
	public int update(final Uri uri, final ContentValues values, final String selection,
			final String[] selectionArgs) {
		return 0;
	}
}
