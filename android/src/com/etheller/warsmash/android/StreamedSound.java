package com.etheller.warsmash.android;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import com.badlogic.gdx.audio.Sound;

/**
 * A sound decoded while it plays, for what a SoundPool cannot hold: long dialogue and ambient loops, and sounds of
 * more than two channels. Every play has its own thread that decodes the file into an AudioTrack, so it starts as soon
 * as the first samples are decoded.
 */
final class StreamedSound implements Sound {
	private static final String TAG = "Warsmash";
	private static final int MAX_VOICES = 8;
	private static final long DEQUEUE_TIMEOUT_MICROS = 10000;
	private static final long WRITE_RETRY_MILLIS = 10;
	private static final int TRACK_BUFFER_MILLIS = 250;

	private static final List<Voice> VOICES = new ArrayList<>();
	private static final Map<String, String> DECODERS = new HashMap<>();
	private static boolean inBackground;

	private final PooledSounds owner;
	private final String path;
	private final Map<Long, Voice> plays = new LinkedHashMap<>();
	private boolean disposed;

	StreamedSound(final PooledSounds owner, final String path) {
		this.owner = owner;
		this.path = path;
	}

	/** Pauses every streamed sound with the activity, as the backend does with its own. */
	static void pauseAll() {
		setInBackground(true);
	}

	static void resumeAll() {
		setInBackground(false);
	}

	private static void setInBackground(final boolean background) {
		final List<Voice> voices;
		synchronized (VOICES) {
			inBackground = background;
			voices = new ArrayList<>(VOICES);
		}
		for (final Voice voice : voices) {
			voice.applyState();
		}
	}

	private static boolean isInBackground() {
		synchronized (VOICES) {
			return inBackground;
		}
	}

	private synchronized long start(final float volume, final float pitch, final float pan, final boolean looping) {
		if (this.disposed) {
			return -1;
		}
		final Voice voice = new Voice(this, volume, pitch, pan, looping);
		Voice dropped = null;
		synchronized (VOICES) {
			if (VOICES.size() >= MAX_VOICES) {
				for (final Voice playing : VOICES) {
					if (!playing.looping) {
						dropped = playing;
						break;
					}
				}
				if (dropped == null) {
					return -1;
				}
				VOICES.remove(dropped);
			}
			VOICES.add(voice);
		}
		if (dropped != null) {
			dropped.stop();
		}
		final long id = this.owner.nextPlayId();
		voice.id = id;
		this.plays.put(id, voice);
		final Thread thread = new Thread(voice, "SoundStream");
		thread.setDaemon(true);
		thread.start();
		return id;
	}

	private synchronized void ended(final Voice voice) {
		this.plays.remove(voice.id);
	}

	private synchronized List<Voice> voices() {
		return new ArrayList<>(this.plays.values());
	}

	private synchronized Voice voice(final long soundId) {
		return this.plays.get(soundId);
	}

	@Override
	public long play() {
		return start(1, 1, 0, false);
	}

	@Override
	public long play(final float volume) {
		return start(volume, 1, 0, false);
	}

	@Override
	public long play(final float volume, final float pitch, final float pan) {
		return start(volume, pitch, pan, false);
	}

	@Override
	public long loop() {
		return start(1, 1, 0, true);
	}

	@Override
	public long loop(final float volume) {
		return start(volume, 1, 0, true);
	}

	@Override
	public long loop(final float volume, final float pitch, final float pan) {
		return start(volume, pitch, pan, true);
	}

	@Override
	public void stop() {
		final List<Voice> voices;
		synchronized (this) {
			voices = new ArrayList<>(this.plays.values());
			this.plays.clear();
		}
		for (final Voice voice : voices) {
			voice.stop();
		}
	}

	@Override
	public void pause() {
		for (final Voice voice : voices()) {
			voice.setPaused(true);
		}
	}

	@Override
	public void resume() {
		for (final Voice voice : voices()) {
			voice.setPaused(false);
		}
	}

	@Override
	public void dispose() {
		synchronized (this) {
			this.disposed = true;
		}
		stop();
	}

	@Override
	public void stop(final long soundId) {
		final Voice voice;
		synchronized (this) {
			voice = this.plays.remove(soundId);
		}
		if (voice != null) {
			voice.stop();
		}
	}

	@Override
	public void pause(final long soundId) {
		final Voice voice = voice(soundId);
		if (voice != null) {
			voice.setPaused(true);
		}
	}

	@Override
	public void resume(final long soundId) {
		final Voice voice = voice(soundId);
		if (voice != null) {
			voice.setPaused(false);
		}
	}

	@Override
	public void setLooping(final long soundId, final boolean looping) {
		final Voice voice = voice(soundId);
		if (voice != null) {
			voice.looping = looping;
		}
	}

	@Override
	public void setPitch(final long soundId, final float pitch) {
		final Voice voice = voice(soundId);
		if (voice != null) {
			voice.setPitch(pitch);
		}
	}

	@Override
	public void setVolume(final long soundId, final float volume) {
		final Voice voice = voice(soundId);
		if (voice != null) {
			voice.volume = volume;
		}
	}

	@Override
	public void setPan(final long soundId, final float pan, final float volume) {
		final Voice voice = voice(soundId);
		if (voice != null) {
			voice.pan = pan;
			voice.volume = volume;
		}
	}

	/**
	 * Some vendor decoders refuse files the platform's own decoders play (one FLAC decoder fails on four channels), so
	 * the platform's software decoder is used where there is one.
	 */
	private static MediaCodec createDecoder(final String mime) throws IOException {
		String name;
		synchronized (DECODERS) {
			name = DECODERS.get(mime);
			if (name == null) {
				name = "";
				for (final MediaCodecInfo info : new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()) {
					final String codecName = info.getName();
					if (info.isEncoder()
							|| !(codecName.startsWith("c2.android.") || codecName.startsWith("OMX.google."))) {
						continue;
					}
					for (final String type : info.getSupportedTypes()) {
						if (type.equalsIgnoreCase(mime)) {
							name = codecName;
							break;
						}
					}
					if (!name.isEmpty()) {
						break;
					}
				}
				DECODERS.put(mime, name);
			}
		}
		return name.isEmpty() ? MediaCodec.createDecoderByType(mime) : MediaCodec.createByCodecName(name);
	}

	private static final class Voice implements Runnable {
		private final StreamedSound sound;
		private long id;
		private volatile float volume;
		private volatile float pan;
		private volatile boolean looping;
		private float pitch;
		private boolean paused;
		private boolean stopped;
		private AudioTrack track;
		private int trackSampleRate;
		private boolean trackPlaying;

		private Voice(final StreamedSound sound, final float volume, final float pitch, final float pan,
				final boolean looping) {
			this.sound = sound;
			this.volume = volume;
			this.pitch = pitch;
			this.pan = pan;
			this.looping = looping;
		}

		private synchronized void stop() {
			this.stopped = true;
			if (this.track != null) {
				this.track.pause();
				this.track.flush();
				this.trackPlaying = false;
			}
			notifyAll();
		}

		private synchronized void setPaused(final boolean paused) {
			this.paused = paused;
			applyState();
		}

		private synchronized void setPitch(final float pitch) {
			this.pitch = pitch;
			applyPitch();
		}

		private synchronized boolean held() {
			return this.paused || this.stopped || isInBackground();
		}

		private synchronized void applyState() {
			if (this.track != null) {
				final boolean held = held();
				if (held && this.trackPlaying) {
					this.track.pause();
					this.trackPlaying = false;
				}
				else if (!held && !this.trackPlaying) {
					this.track.play();
					this.trackPlaying = true;
				}
			}
			notifyAll();
		}

		private void applyPitch() {
			if ((this.track != null) && (this.pitch > 0)) {
				try {
					this.track.setPlaybackRate(Math.round(this.trackSampleRate * this.pitch));
				}
				catch (final IllegalArgumentException e) {
					// Out of the track's range: the sound plays at its own speed.
				}
			}
		}

		/** @return false when the voice was stopped */
		private synchronized boolean waitWhileHeld() {
			while (held() && !this.stopped) {
				try {
					wait();
				}
				catch (final InterruptedException e) {
					return false;
				}
			}
			return !this.stopped;
		}

		private synchronized boolean isStopped() {
			return this.stopped;
		}

		@Override
		public void run() {
			Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
			MediaExtractor extractor = null;
			MediaCodec codec = null;
			try {
				extractor = new MediaExtractor();
				extractor.setDataSource(this.sound.path);
				MediaFormat format = null;
				for (int i = 0; i < extractor.getTrackCount(); i++) {
					final MediaFormat trackFormat = extractor.getTrackFormat(i);
					final String mime = trackFormat.getString(MediaFormat.KEY_MIME);
					if ((mime != null) && mime.startsWith("audio/")) {
						extractor.selectTrack(i);
						format = trackFormat;
						break;
					}
				}
				if (format == null) {
					return;
				}
				codec = createDecoder(format.getString(MediaFormat.KEY_MIME));
				format.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT);
				codec.configure(format, null, null, 0);
				codec.start();
				decode(extractor, codec, format);
			}
			catch (final Exception e) {
				Log.e(TAG, "Unable to play " + this.sound.path, e);
			}
			finally {
				if (codec != null) {
					try {
						codec.stop();
					}
					catch (final IllegalStateException e) {
						// Already failed; released below.
					}
					codec.release();
				}
				if (extractor != null) {
					extractor.release();
				}
				synchronized (this) {
					this.stopped = true;
					if (this.track != null) {
						this.track.release();
						this.track = null;
					}
				}
				synchronized (VOICES) {
					VOICES.remove(this);
				}
				this.sound.ended(this);
			}
		}

		private void decode(final MediaExtractor extractor, final MediaCodec codec, final MediaFormat inputFormat) {
			final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
			int channels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
			int sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
			int encoding = AudioFormat.ENCODING_PCM_16BIT;
			short[] stereo = new short[0];
			long framesWritten = 0;
			boolean inputDone = false;
			while (!isStopped()) {
				if (!inputDone) {
					final int inputIndex = codec.dequeueInputBuffer(DEQUEUE_TIMEOUT_MICROS);
					if (inputIndex >= 0) {
						final ByteBuffer input = codec.getInputBuffer(inputIndex);
						int size = extractor.readSampleData(input, 0);
						if ((size < 0) && this.looping) {
							extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
							size = extractor.readSampleData(input, 0);
						}
						if (size < 0) {
							codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
							inputDone = true;
						}
						else {
							codec.queueInputBuffer(inputIndex, 0, size, extractor.getSampleTime(), 0);
							extractor.advance();
						}
					}
				}
				final int outputIndex = codec.dequeueOutputBuffer(info, DEQUEUE_TIMEOUT_MICROS);
				if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
					final MediaFormat outputFormat = codec.getOutputFormat();
					channels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
					sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
					encoding = outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)
							? outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
							: AudioFormat.ENCODING_PCM_16BIT;
				}
				else if (outputIndex >= 0) {
					if (info.size > 0) {
						final ByteBuffer output = codec.getOutputBuffer(outputIndex);
						output.position(info.offset);
						output.limit(info.offset + info.size);
						final int frameBytes = channels * (encoding == AudioFormat.ENCODING_PCM_FLOAT ? 4 : 2);
						final int frames = info.size / frameBytes;
						if (stereo.length < (frames * 2)) {
							stereo = new short[frames * 2];
						}
						toStereo(output.slice().order(ByteOrder.nativeOrder()), encoding, channels, frames, stereo);
						if (!ensureTrack(sampleRate) || !write(stereo, frames * 2)) {
							codec.releaseOutputBuffer(outputIndex, false);
							return;
						}
						framesWritten += frames;
					}
					codec.releaseOutputBuffer(outputIndex, false);
					if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
						drain(framesWritten, sampleRate);
						return;
					}
				}
			}
		}

		private void toStereo(final ByteBuffer pcm, final int encoding, final int channels, final int frames,
				final short[] stereo) {
			final float volume = Math.max(0, this.volume);
			final float pan = this.pan;
			final float left = pan > 0 ? volume * (1 - pan) : volume;
			final float right = pan < 0 ? volume * (1 + pan) : volume;
			final ShortBuffer shorts = encoding == AudioFormat.ENCODING_PCM_FLOAT ? null : pcm.asShortBuffer();
			final FloatBuffer floats = encoding == AudioFormat.ENCODING_PCM_FLOAT ? pcm.asFloatBuffer() : null;
			// Channels past the first two are folded in by side (front left, front right, back left, back right...).
			final float sideGain = channels > 2 ? (float) (1 / Math.sqrt((channels + 1) / 2)) : 1;
			int at = 0;
			for (int frame = 0; frame < frames; frame++) {
				float l = 0;
				float r = 0;
				for (int channel = 0; channel < channels; channel++) {
					final float sample = shorts != null ? shorts.get(at++) : floats.get(at++) * 32767f;
					if ((channel & 1) == 0) {
						l += sample;
					}
					else {
						r += sample;
					}
				}
				if (channels == 1) {
					r = l;
				}
				stereo[frame * 2] = clamp(l * sideGain * left);
				stereo[(frame * 2) + 1] = clamp(r * sideGain * right);
			}
		}

		private static short clamp(final float sample) {
			return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(sample)));
		}

		private synchronized boolean ensureTrack(final int sampleRate) {
			if (this.stopped) {
				return false;
			}
			if ((this.track != null) && (this.trackSampleRate == sampleRate)) {
				return true;
			}
			if (this.track != null) {
				this.track.release();
				this.track = null;
				this.trackPlaying = false;
			}
			final int minimum = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO,
					AudioFormat.ENCODING_PCM_16BIT);
			final int size = Math.max(minimum, ((sampleRate * TRACK_BUFFER_MILLIS) / 1000) * 4);
			this.track = new AudioTrack.Builder()
					.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
							.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
					.setAudioFormat(new AudioFormat.Builder().setSampleRate(sampleRate)
							.setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
							.setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
					.setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(size).build();
			this.trackSampleRate = sampleRate;
			applyPitch();
			applyState();
			return true;
		}

		/** @return false when the voice was stopped */
		private boolean write(final short[] samples, final int count) {
			int written = 0;
			while (written < count) {
				if (!waitWhileHeld()) {
					return false;
				}
				final int result;
				synchronized (this) {
					if (this.stopped || (this.track == null)) {
						return false;
					}
					result = this.track.write(samples, written, count - written, AudioTrack.WRITE_NON_BLOCKING);
				}
				if (result < 0) {
					Log.e(TAG, "Unable to play " + this.sound.path + ": " + result);
					return false;
				}
				written += result;
				if (written < count) {
					SystemClock.sleep(WRITE_RETRY_MILLIS);
				}
			}
			return true;
		}

		/** Waits until what was written has been heard. */
		private void drain(final long framesWritten, final int sampleRate) {
			long lastPosition = -1;
			long lastProgress = SystemClock.uptimeMillis();
			while (true) {
				final long waitStarted = SystemClock.uptimeMillis();
				if (!waitWhileHeld()) {
					return;
				}
				final long position;
				synchronized (this) {
					if (this.track == null) {
						return;
					}
					position = this.track.getPlaybackHeadPosition() & 0xFFFFFFFFL;
				}
				if (position >= framesWritten) {
					return;
				}
				final long now = SystemClock.uptimeMillis();
				if ((position != lastPosition) || ((now - waitStarted) > WRITE_RETRY_MILLIS)) {
					lastPosition = position;
					lastProgress = now;
				}
				if ((position == 0) || ((now - lastProgress) > (TRACK_BUFFER_MILLIS * 2))) {
					// A track starts once its buffer is full, so a sound shorter than that has not started yet; a
					// stopped track plays what it was given, but no longer tells how far it got.
					synchronized (this) {
						if (this.track != null) {
							this.track.stop();
						}
					}
					SystemClock.sleep((((framesWritten - position) * 1000) / sampleRate) + TRACK_BUFFER_MILLIS);
					return;
				}
				SystemClock.sleep(WRITE_RETRY_MILLIS * 2);
			}
		}
	}
}
