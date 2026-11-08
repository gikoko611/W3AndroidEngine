package com.etheller.warsmash.android;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.media.MediaPlayer;
import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

/**
 * Music whose player is opened and prepared on a background thread: MediaPlayer.prepare() takes about a tenth of a
 * second, which the backend's own music spends on the thread that draws, so every change of track dropped frames. What
 * is asked of the track before it is ready (play, position, volume, looping) is kept and applied once it is. The
 * backend pauses its own music when the game leaves the screen; these are paused and resumed by
 * {@link #pauseAll()} and {@link #resumeAll()} the same way.
 */
final class PreparedMusic implements Music {
	private static final String TAG = "Warsmash";
	private static final ExecutorService PREPARER = Executors.newSingleThreadExecutor(runnable -> {
		final Thread thread = new Thread(runnable, "MusicPrepare");
		thread.setDaemon(true);
		return thread;
	});
	private static final List<PreparedMusic> OPEN = new ArrayList<>();

	private MediaPlayer player;
	private boolean disposed;
	/** Asked to play and not paused, stopped or finished since. */
	private boolean playing;
	/** Playing when the game left the screen. */
	private boolean pausedInBackground;
	private boolean looping;
	private float volume = 1;
	private float pan;
	/** Seconds to start from, asked for before the track was ready; negative for none. */
	private float pendingPosition = -1;
	private OnCompletionListener listener;

	/** @param path a real file; the game data is read only on the thread that asks for the music */
	PreparedMusic(final String path) {
		synchronized (OPEN) {
			OPEN.add(this);
		}
		PREPARER.execute(() -> prepare(path));
	}

	private void prepare(final String path) {
		synchronized (this) {
			if (this.disposed) {
				return;
			}
		}
		final MediaPlayer prepared = new MediaPlayer();
		try {
			prepared.setDataSource(path);
			prepared.prepare();
		}
		catch (final Exception e) {
			Log.e(TAG, "Unable to open the music", e);
			prepared.release();
			return;
		}
		synchronized (this) {
			if (this.disposed) {
				prepared.release();
				return;
			}
			this.player = prepared;
			prepared.setOnCompletionListener(finished -> completed());
			prepared.setLooping(this.looping);
			applyVolume();
			if (this.pendingPosition >= 0) {
				prepared.seekTo((int) (this.pendingPosition * 1000));
				this.pendingPosition = -1;
			}
			if (this.playing && !this.pausedInBackground) {
				prepared.start();
			}
		}
	}

	private void completed() {
		final OnCompletionListener completion;
		synchronized (this) {
			if (this.looping || this.disposed) {
				return;
			}
			this.playing = false;
			completion = this.listener;
		}
		if ((completion != null) && (Gdx.app != null)) {
			Gdx.app.postRunnable(() -> completion.onCompletion(this));
		}
	}

	private void applyVolume() {
		float left = this.volume;
		float right = this.volume;
		if (this.pan < 0) {
			right *= 1 - Math.abs(this.pan);
		}
		else if (this.pan > 0) {
			left *= 1 - Math.abs(this.pan);
		}
		this.player.setVolume(left, right);
	}

	@Override
	public synchronized void play() {
		if (this.disposed) {
			return;
		}
		this.playing = true;
		if ((this.player != null) && !this.pausedInBackground && !this.player.isPlaying()) {
			this.player.start();
		}
	}

	@Override
	public synchronized void pause() {
		this.playing = false;
		if ((this.player != null) && this.player.isPlaying()) {
			this.player.pause();
		}
	}

	/** Stops and goes back to the start; the player stays prepared, so playing again needs no new preparation. */
	@Override
	public synchronized void stop() {
		this.playing = false;
		this.pendingPosition = -1;
		if (this.player != null) {
			if (this.player.isPlaying()) {
				this.player.pause();
			}
			this.player.seekTo(0);
		}
	}

	/**
	 * A track asked to play counts as playing while it is prepared, as the backend's music did, whose preparation held
	 * the caller until it was done; a track that could not be opened counts as playing, too, as it left no track
	 * before, so a list of tracks does not hurry through it.
	 */
	@Override
	public synchronized boolean isPlaying() {
		if ((this.player == null) || this.pausedInBackground) {
			return this.playing && !this.disposed;
		}
		return this.playing && this.player.isPlaying();
	}

	@Override
	public synchronized void setLooping(final boolean isLooping) {
		this.looping = isLooping;
		if (this.player != null) {
			this.player.setLooping(isLooping);
		}
	}

	@Override
	public synchronized boolean isLooping() {
		return this.looping;
	}

	@Override
	public synchronized void setVolume(final float volume) {
		this.volume = volume;
		this.pan = 0;
		if (this.player != null) {
			applyVolume();
		}
	}

	@Override
	public synchronized float getVolume() {
		return this.volume;
	}

	@Override
	public synchronized void setPan(final float pan, final float volume) {
		this.pan = pan;
		this.volume = volume;
		if (this.player != null) {
			applyVolume();
		}
	}

	@Override
	public synchronized void setPosition(final float position) {
		if (this.player != null) {
			this.player.seekTo((int) (position * 1000));
		}
		else {
			this.pendingPosition = position;
		}
	}

	@Override
	public synchronized float getPosition() {
		if (this.player != null) {
			return this.player.getCurrentPosition() / 1000f;
		}
		return Math.max(0, this.pendingPosition);
	}

	@Override
	public synchronized void setOnCompletionListener(final OnCompletionListener listener) {
		this.listener = listener;
	}

	@Override
	public void dispose() {
		synchronized (OPEN) {
			OPEN.remove(this);
		}
		synchronized (this) {
			if (this.disposed) {
				return;
			}
			this.disposed = true;
			this.playing = false;
			if (this.player != null) {
				this.player.release();
				this.player = null;
			}
		}
	}

	private synchronized void background(final boolean away) {
		if (away) {
			if ((this.player != null) && this.player.isPlaying()) {
				this.player.pause();
			}
			this.pausedInBackground = true;
		}
		else {
			this.pausedInBackground = false;
			if ((this.player != null) && this.playing && !this.player.isPlaying()) {
				this.player.start();
			}
		}
	}

	private static List<PreparedMusic> open() {
		synchronized (OPEN) {
			return new ArrayList<>(OPEN);
		}
	}

	static void pauseAll() {
		for (final PreparedMusic music : open()) {
			music.background(true);
		}
	}

	static void resumeAll() {
		for (final PreparedMusic music : open()) {
			music.background(false);
		}
	}
}
