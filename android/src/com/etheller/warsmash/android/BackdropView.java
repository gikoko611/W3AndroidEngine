package com.etheller.warsmash.android;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * An original menu or campaign scene: its first frame at once, and its recording over it while the screen shows it,
 * or, without a recording, the live 3D scene drawn behind the launcher once it is ready.
 * The recording is scaled to cover the view, cropped around a horizontal focus point like the first frame, silent and
 * looped. Without a recording, or when the device cannot play it, the first frame (or its stand-in) simply stays.
 */
final class BackdropView extends FrameLayout implements TextureView.SurfaceTextureListener {
	private static final String TAG = "Warsmash";
	private static final long FADE_MILLIS = 150;

	private final File recording;
	private final float focus;
	private final View still;
	private TextureView video;
	private MediaPlayer player;
	private Surface surface;
	private int videoWidth;
	private int videoHeight;

	BackdropView(final Context context, final Drawable poster, final File recording, final float focus) {
		super(context);
		this.recording = recording;
		this.focus = focus;
		this.still = new View(context);
		this.still.setBackground(poster);
		addView(this.still, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
	}

	boolean hasRecording() {
		return this.recording != null;
	}

	/** Starts the recording from its first frame; the view must be in a window that draws hardware layers. */
	void play() {
		if ((this.recording == null) || (this.video != null)) {
			return;
		}
		this.video = new TextureView(getContext());
		this.video.setAlpha(0);
		this.video.setSurfaceTextureListener(this);
		addView(this.video, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
	}

	/** The scene is drawn live behind this view (its 3D model): the still fades away and lets it show through. */
	void reveal() {
		this.still.animate().alpha(0).setDuration(FADE_MILLIS);
	}

	/** Stops and lets go of the decoder; the first frame shows again until the next play. */
	void stop() {
		release();
		if (this.video != null) {
			removeView(this.video);
			this.video = null;
		}
	}

	@Override
	public void onSurfaceTextureAvailable(final SurfaceTexture texture, final int width, final int height) {
		this.surface = new Surface(texture);
		final MediaPlayer player = new MediaPlayer();
		this.player = player;
		try {
			player.setSurface(this.surface);
			player.setDataSource(this.recording.getAbsolutePath());
			player.setLooping(true);
			player.setVolume(0, 0);
			player.setOnVideoSizeChangedListener((mp, w, h) -> {
				this.videoWidth = w;
				this.videoHeight = h;
				fit();
			});
			player.setOnInfoListener((mp, what, extra) -> {
				if ((what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) && (this.video != null)) {
					this.video.animate().alpha(1).setDuration(FADE_MILLIS);
				}
				return false;
			});
			player.setOnErrorListener((mp, what, extra) -> {
				Log.w(TAG, "Backdrop " + this.recording.getName() + " cannot play: " + what + "/" + extra);
				stop();
				return true;
			});
			player.setOnPreparedListener(MediaPlayer::start);
			player.prepareAsync();
		}
		catch (final IOException | RuntimeException e) {
			Log.w(TAG, "Backdrop " + this.recording.getName() + " cannot play", e);
			post(this::stop);
		}
	}

	@Override
	public void onSurfaceTextureSizeChanged(final SurfaceTexture texture, final int width, final int height) {
		fit();
	}

	@Override
	public boolean onSurfaceTextureDestroyed(final SurfaceTexture texture) {
		release();
		return true;
	}

	@Override
	public void onSurfaceTextureUpdated(final SurfaceTexture texture) {
	}

	/** The texture view stretches the picture to its size; this undoes the stretch and crops as the poster does. */
	private void fit() {
		final TextureView view = this.video;
		if ((view == null) || (this.videoWidth <= 0) || (this.videoHeight <= 0) || (view.getWidth() <= 0)
				|| (view.getHeight() <= 0)) {
			return;
		}
		final float width = view.getWidth();
		final float height = view.getHeight();
		final float viewAspect = width / height;
		final float videoAspect = this.videoWidth / (float) this.videoHeight;
		final Matrix matrix = new Matrix();
		if (videoAspect > viewAspect) {
			final float scale = videoAspect / viewAspect;
			matrix.setScale(scale, 1);
			matrix.postTranslate(-((scale * width) - width) * this.focus, 0);
		}
		else {
			final float scale = viewAspect / videoAspect;
			matrix.setScale(1, scale);
			matrix.postTranslate(0, -((scale * height) - height) / 2);
		}
		view.setTransform(matrix);
	}

	private void release() {
		if (this.player != null) {
			try {
				this.player.release();
			}
			catch (final RuntimeException e) {
				Log.w(TAG, "Unable to release the backdrop player", e);
			}
			this.player = null;
		}
		if (this.surface != null) {
			this.surface.release();
			this.surface = null;
		}
	}
}
