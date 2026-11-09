package com.etheller.warsmash.desktop.launcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.PointerPointer;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.TimeUtils;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.desktop.FfmpegMovieDecoders;
import com.etheller.warsmash.launcher.LauncherLog;
import com.etheller.warsmash.movie.MovieDecoders;
import com.etheller.warsmash.movie.MoviePicture;
import com.etheller.warsmash.movie.MovieTexture;

/**
 * A recorded menu or campaign scene of the game data, played over and over behind a launcher page as the device
 * plays it: read out of its WebM file by FFmpeg and decoded by the movies' picture decoder on a thread of its own, a
 * few pictures ahead, then drawn by the movies' shader. Without animations only its first picture is shown.
 */
final class SceneRecording implements Runnable {
	/** Pictures decoded ahead of the one on screen. */
	private static final int PICTURES = 3;
	/** The scenes are 1920 wide at 30 frames a second, which two threads decode with room to spare. */
	private static final int DECODER_THREADS = 2;
	private static final long STOP_WAIT_MILLIS = 1000;
	/** A picture this late means the window stood still; the clock moves on instead of hurrying to catch up. */
	private static final long LATE_MILLIS = 250;
	/** The shape of the scene recordings, until the first picture gives the real one. */
	private static final float RECORDED_ASPECT = 1920f / 810f;

	final String name;
	final boolean animated;
	private final DataSource data;
	private final String path;
	private final MovieTexture texture = new MovieTexture();
	private final BlockingQueue<MoviePicture> ready = new ArrayBlockingQueue<>(PICTURES);
	private final BlockingQueue<MoviePicture> free = new ArrayBlockingQueue<>(PICTURES);
	private final Thread thread;
	private volatile boolean cancelled;
	private long startedAt = -1;

	SceneRecording(final DataSource data, final String path, final String name, final boolean animated) {
		this.data = data;
		this.path = path;
		this.name = name;
		this.animated = animated;
		for (int i = 0; i < PICTURES; i++) {
			this.free.add(new MoviePicture());
		}
		this.thread = new Thread(this, "LauncherScene");
		this.thread.setDaemon(true);
		this.thread.start();
	}

	/** The width of the recording over its height. */
	float aspect() {
		return this.texture.hasPicture() ? this.texture.getWidth() / (float) this.texture.getHeight()
				: RECORDED_ASPECT;
	}

	/**
	 * Draws the picture due now so that it covers the place, cropped at the focus across (0 its left edge, 1 its
	 * right) and in the middle down; nothing until the first picture is decoded.
	 */
	void draw(final SpriteBatch batch, final float x, final float y, final float w, final float h, final float focus) {
		final MoviePicture due = pictureDue(TimeUtils.millis());
		if (due != null) {
			this.texture.upload(due);
			this.free.offer(due);
		}
		if (!this.texture.hasPicture() || (w <= 0) || (h <= 0)) {
			return;
		}
		final float imageW = this.texture.getWidth();
		final float imageH = this.texture.getHeight();
		final float scale = Math.max(w / imageW, h / imageH);
		final float shownW = w / scale;
		final float shownH = h / scale;
		final float left = (imageW - shownW) * focus;
		final float top = (imageH - shownH) / 2;
		this.texture.draw(batch, x, y, w, h, left / imageW, top / imageH, (left + shownW) / imageW,
				(top + shownH) / imageH);
	}

	/** The newest picture due by now; the ones before it are passed over. */
	private MoviePicture pictureDue(final long now) {
		if (this.startedAt < 0) {
			final MoviePicture first = this.ready.peek();
			if (first == null) {
				return null;
			}
			this.startedAt = now - first.getFrame();
		}
		MoviePicture due = null;
		MoviePicture head;
		while (((head = this.ready.peek()) != null) && ((this.startedAt + head.getFrame()) <= now)) {
			this.ready.poll();
			if (due != null) {
				this.free.offer(due);
			}
			due = head;
		}
		if ((due != null) && ((this.startedAt + due.getFrame()) < (now - LATE_MILLIS))) {
			this.startedAt = now - due.getFrame();
		}
		return due;
	}

	/** Stops the decoding and lets go of the pictures; on the render thread. */
	void close() {
		this.cancelled = true;
		this.thread.interrupt();
		try {
			this.thread.join(STOP_WAIT_MILLIS);
		}
		catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		if (this.thread.isAlive()) {
			LauncherLog.w("The scene " + this.name + " did not stop in time");
		}
		this.texture.dispose();
		this.ready.clear();
		this.free.clear();
	}

	@Override
	public void run() {
		try {
			play(file());
		}
		catch (final IOException | RuntimeException | LinkageError e) {
			if (!this.cancelled) {
				LauncherLog.w("The scene " + this.name + " cannot play", e);
			}
		}
		catch (final InterruptedException e) {
			// closed
		}
	}

	/** The recording as a file FFmpeg can open: the data's own, else a copy that goes when the launcher does. */
	private File file() throws IOException {
		final File file = this.data.getFile(this.path);
		if ((file != null) && file.isFile()) {
			return file;
		}
		final File copy = File.createTempFile("scene", ".webm");
		copy.deleteOnExit();
		try (InputStream in = this.data.getResourceAsStream(this.path)) {
			if (in == null) {
				throw new IOException("No " + this.path);
			}
			Files.copy(in, copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		return copy;
	}

	private void play(final File file) throws IOException, InterruptedException {
		final AVFormatContext format = new AVFormatContext(null);
		final int opened = avformat.avformat_open_input(format, file.getAbsolutePath(), null, null);
		if (opened < 0) {
			throw new IOException("Unable to open " + file + " (" + opened + ")");
		}
		final AVPacket packet = avcodec.av_packet_alloc();
		try {
			if (avformat.avformat_find_stream_info(format, (PointerPointer<?>) null) < 0) {
				throw new IOException("Unable to read " + file);
			}
			final int stream = avformat.av_find_best_stream(format, avutil.AVMEDIA_TYPE_VIDEO, -1, -1, (AVCodec) null,
					0);
			if (stream < 0) {
				throw new IOException("No picture in " + file);
			}
			final AVStream video = format.streams(stream);
			final AVCodecParameters parameters = video.codecpar();
			if (parameters.codec_id() != avcodec.AV_CODEC_ID_VP9) {
				throw new IOException("The scene " + file + " is not VP9");
			}
			final AVRational base = video.time_base();
			final AVRational rate = video.avg_frame_rate();
			final long frameMillis = (rate.num() > 0) && (rate.den() > 0) ? (1000L * rate.den()) / rate.num() : 33;
			final MovieDecoders decoders = new FfmpegMovieDecoders(DECODER_THREADS);
			long loopStart = 0;
			long last = 0;
			while (!this.cancelled) {
				try (MovieDecoders.VideoDecoder decoder = decoders.openVideo("VP90", parameters.width(),
						parameters.height())) {
					while (!this.cancelled) {
						if (avformat.av_read_frame(format, packet) < 0) {
							decoder.send(null, -1);
							receive(decoder);
							break;
						}
						try {
							if ((packet.stream_index() == stream) && (packet.size() > 0)) {
								final long pts = packet.pts() != avutil.AV_NOPTS_VALUE ? packet.pts() : packet.dts();
								final long millis = loopStart
										+ ((Math.max(0, pts) * 1000L * base.num()) / Math.max(1, base.den()));
								last = Math.max(last, millis);
								final ByteBuffer bytes = packet.data().capacity(packet.size()).asByteBuffer();
								decoder.send(bytes, millis);
							}
						}
						finally {
							avcodec.av_packet_unref(packet);
						}
						if (!receive(decoder)) {
							return;
						}
					}
				}
				// Each round starts a new decoder, as the last one was told the recording had ended.
				loopStart = last + frameMillis;
				if (avformat.av_seek_frame(format, stream, 0, avformat.AVSEEK_FLAG_BACKWARD) < 0) {
					return;
				}
			}
		}
		finally {
			avcodec.av_packet_free(packet);
			avformat.avformat_close_input(format);
		}
	}

	/**
	 * Hands on the pictures the decoder has, waiting while enough are ready; after the end of the recording, all it
	 * still holds.
	 *
	 * @return false once nothing more is to be decoded: the first picture of a still scene is there
	 */
	private boolean receive(final MovieDecoders.VideoDecoder decoder) throws IOException, InterruptedException {
		while (!this.cancelled) {
			final MoviePicture picture = this.free.take();
			final int got = decoder.receive(picture);
			if (got == MovieDecoders.VideoDecoder.PICTURE) {
				this.ready.put(picture);
				if (!this.animated) {
					return false;
				}
				continue;
			}
			this.free.put(picture);
			return true;
		}
		return false;
	}
}
