package com.etheller.warsmash.desktop;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avutil.AVChannelLayout;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.ffmpeg.global.swresample;
import org.bytedeco.ffmpeg.global.swscale;
import org.bytedeco.ffmpeg.swresample.SwrContext;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.PointerPointer;

import com.etheller.warsmash.movie.MovieDecoders;
import com.etheller.warsmash.movie.MoviePicture;

/** The movie decoders of the desktop: FFmpeg's VP9 decoder for the picture, FFmpeg's Vorbis and FLAC for the sound. */
public final class FfmpegMovieDecoders implements MovieDecoders {
	private static boolean quiet;

	private final int threads;

	/** Decoders that take every core, as a full screen movie needs. */
	public FfmpegMovieDecoders() {
		this(0);
	}

	/** @param threads the decoding threads of each picture decoder, 0 for one per core */
	public FfmpegMovieDecoders(final int threads) {
		this.threads = threads;
	}

	private static synchronized void quiet() {
		if (!quiet) {
			quiet = true;
			avutil.av_log_set_level(avutil.AV_LOG_ERROR);
		}
	}

	@Override
	public VideoDecoder openVideo(final String codec, final int width, final int height) throws IOException {
		if (!"VP90".equalsIgnoreCase(codec)) {
			return null;
		}
		quiet();
		return new Vp9Decoder(width, height, this.threads);
	}

	@Override
	public SoundDecoder openSound(final File file) throws IOException {
		quiet();
		return new FileSoundDecoder(file);
	}

	private static IOException error(final String what, final int code) {
		final BytePointer text = new BytePointer(256);
		avutil.av_strerror(code, text, 256);
		final String message = text.getString();
		text.close();
		return new IOException(what + ": " + message + " (" + code + ")");
	}

	private static final class Vp9Decoder implements VideoDecoder {
		private final AVCodecContext context;
		private final AVPacket packet;
		private final AVFrame frame;
		private SwsContext scaler;
		private AVFrame converted;

		Vp9Decoder(final int width, final int height, final int threads) throws IOException {
			final AVCodec codec = avcodec.avcodec_find_decoder(avcodec.AV_CODEC_ID_VP9);
			if (codec == null) {
				throw new IOException("No VP9 decoder");
			}
			this.context = avcodec.avcodec_alloc_context3(codec);
			this.context.width(width);
			this.context.height(height);
			// One thread per core: a 1920 wide movie takes more than one core can give at 24 frames a second.
			this.context.thread_count(threads);
			final int opened = avcodec.avcodec_open2(this.context, codec, (PointerPointer<?>) null);
			if (opened < 0) {
				avcodec.avcodec_free_context(this.context);
				throw error("Unable to open the VP9 decoder", opened);
			}
			this.packet = avcodec.av_packet_alloc();
			this.frame = avutil.av_frame_alloc();
		}

		@Override
		public void send(final ByteBuffer data, final long frameNumber) throws IOException {
			final int sent;
			if (data == null) {
				sent = avcodec.avcodec_send_packet(this.context, null);
			}
			else {
				final BytePointer bytes = new BytePointer(data.slice());
				this.packet.data(bytes);
				this.packet.size(data.remaining());
				this.packet.pts(frameNumber);
				this.packet.dts(frameNumber);
				sent = avcodec.avcodec_send_packet(this.context, this.packet);
				// The decoder copies a packet it does not own; the pointer is left before the buffer is reused.
				this.packet.data(null);
				this.packet.size(0);
				bytes.close();
			}
			if ((sent < 0) && (sent != avutil.AVERROR_EOF())) {
				throw error("The movie frame cannot be decoded", sent);
			}
		}

		@Override
		public int receive(final MoviePicture into) throws IOException {
			final int got = avcodec.avcodec_receive_frame(this.context, this.frame);
			if (got == avutil.AVERROR_EOF()) {
				return END;
			}
			if (got < 0) {
				return NOT_YET;
			}
			try {
				copy(into);
			}
			finally {
				avutil.av_frame_unref(this.frame);
			}
			return PICTURE;
		}

		private void copy(final MoviePicture into) throws IOException {
			final int width = this.frame.width();
			final int height = this.frame.height();
			final int colorspace = this.frame.colorspace();
			final boolean bt709 = (colorspace == avutil.AVCOL_SPC_BT709)
					|| ((colorspace != avutil.AVCOL_SPC_BT470BG) && (colorspace != avutil.AVCOL_SPC_SMPTE170M)
							&& (height >= 720));
			final long pts = this.frame.best_effort_timestamp() != avutil.AV_NOPTS_VALUE
					? this.frame.best_effort_timestamp()
					: this.frame.pts();
			into.setFormat(width, height, this.frame.color_range() == avutil.AVCOL_RANGE_JPEG, bt709, pts);
			AVFrame source = this.frame;
			if (this.frame.format() != avutil.AV_PIX_FMT_YUV420P) {
				source = convert(width, height);
			}
			copyPlane(source, 0, height, width, into.getY());
			copyPlane(source, 1, into.getChromaHeight(), into.getChromaWidth(), into.getU());
			copyPlane(source, 2, into.getChromaHeight(), into.getChromaWidth(), into.getV());
		}

		/** Other layouts (a 10-bit profile, 4:4:4) are brought to 8-bit 4:2:0. */
		private AVFrame convert(final int width, final int height) throws IOException {
			this.scaler = swscale.sws_getCachedContext(this.scaler, width, height, this.frame.format(), width, height,
					avutil.AV_PIX_FMT_YUV420P, swscale.SWS_BILINEAR, null, null, (DoublePointer) null);
			if (this.scaler == null) {
				throw new IOException("The movie's pixel format " + this.frame.format() + " cannot be converted");
			}
			if ((this.converted == null) || (this.converted.width() != width)
					|| (this.converted.height() != height)) {
				if (this.converted != null) {
					avutil.av_frame_free(this.converted);
				}
				this.converted = avutil.av_frame_alloc();
				this.converted.format(avutil.AV_PIX_FMT_YUV420P);
				this.converted.width(width);
				this.converted.height(height);
				final int allocated = avutil.av_frame_get_buffer(this.converted, 0);
				if (allocated < 0) {
					throw error("Unable to convert the movie picture", allocated);
				}
			}
			swscale.sws_scale(this.scaler, this.frame.data(), this.frame.linesize(), 0, height,
					this.converted.data(), this.converted.linesize());
			return this.converted;
		}

		private static void copyPlane(final AVFrame source, final int plane, final int rows, final int rowBytes,
				final ByteBuffer into) {
			final int stride = source.linesize(plane);
			final ByteBuffer bytes = source.data(plane).capacity((long) stride * rows).asByteBuffer();
			into.clear();
			if (stride == rowBytes) {
				bytes.limit(rowBytes * rows);
				into.put(bytes);
			}
			else {
				for (int row = 0; row < rows; row++) {
					bytes.limit((row * stride) + rowBytes).position(row * stride);
					into.put(bytes);
				}
			}
			into.flip();
		}

		@Override
		public void close() {
			avcodec.av_packet_free(this.packet);
			avutil.av_frame_free(this.frame);
			if (this.converted != null) {
				avutil.av_frame_free(this.converted);
			}
			if (this.scaler != null) {
				swscale.sws_freeContext(this.scaler);
			}
			avcodec.avcodec_free_context(this.context);
		}
	}

	/** A sound file, decoded and mixed down to stereo at its own rate. */
	private static final class FileSoundDecoder implements SoundDecoder {
		private final AVFormatContext format;
		private final AVCodecContext context;
		private final AVPacket packet;
		private final AVFrame frame;
		private final SwrContext resampler;
		private final int stream;
		private final int sampleRate;
		private BytePointer output = new BytePointer(0L);
		private final PointerPointer<BytePointer> outputPlanes = new PointerPointer<>(1L);
		private int outputSamples;
		private short[] pending = new short[0];
		private int pendingStart;
		private int pendingEnd;
		private boolean inputDone;
		private boolean decoderDone;

		FileSoundDecoder(final File file) throws IOException {
			final AVFormatContext format = new AVFormatContext(null);
			final int opened = avformat.avformat_open_input(format, file.getAbsolutePath(), null, null);
			if (opened < 0) {
				throw error("Unable to open the movie sound " + file, opened);
			}
			this.format = format;
			AVCodecContext context = null;
			SwrContext resampler = null;
			try {
				final int info = avformat.avformat_find_stream_info(format, (PointerPointer<?>) null);
				if (info < 0) {
					throw error("Unable to read the movie sound " + file, info);
				}
				this.stream = avformat.av_find_best_stream(format, avutil.AVMEDIA_TYPE_AUDIO, -1, -1,
						(AVCodec) null, 0);
				if (this.stream < 0) {
					throw error("No sound in " + file, this.stream);
				}
				final AVCodec codec = avcodec
						.avcodec_find_decoder(format.streams(this.stream).codecpar().codec_id());
				if (codec == null) {
					throw new IOException("No decoder for the sound of " + file);
				}
				context = avcodec.avcodec_alloc_context3(codec);
				avcodec.avcodec_parameters_to_context(context, format.streams(this.stream).codecpar());
				final int decoder = avcodec.avcodec_open2(context, codec, (PointerPointer<?>) null);
				if (decoder < 0) {
					throw error("Unable to open the sound decoder of " + file, decoder);
				}
				this.sampleRate = context.sample_rate();
				final AVChannelLayout stereo = new AVChannelLayout();
				avutil.av_channel_layout_default(stereo, 2);
				resampler = new SwrContext(null);
				// The surround tracks are mixed down to the two speakers, as the device's player does.
				int set = swresample.swr_alloc_set_opts2(resampler, stereo, avutil.AV_SAMPLE_FMT_S16, this.sampleRate,
						context.ch_layout(), context.sample_fmt(), this.sampleRate, 0, null);
				if (set >= 0) {
					set = swresample.swr_init(resampler);
				}
				avutil.av_channel_layout_uninit(stereo);
				if (set < 0) {
					throw error("Unable to mix down the sound of " + file, set);
				}
			}
			catch (final IOException | RuntimeException e) {
				if (resampler != null) {
					swresample.swr_free(resampler);
				}
				if (context != null) {
					avcodec.avcodec_free_context(context);
				}
				avformat.avformat_close_input(format);
				throw e;
			}
			this.context = context;
			this.resampler = resampler;
			this.packet = avcodec.av_packet_alloc();
			this.frame = avutil.av_frame_alloc();
		}

		@Override
		public int getSampleRate() {
			return this.sampleRate;
		}

		@Override
		public int read(final short[] samples) throws IOException {
			while (this.pendingStart >= this.pendingEnd) {
				if (!decodeMore()) {
					return -1;
				}
			}
			final int count = Math.min(samples.length & ~1, this.pendingEnd - this.pendingStart);
			System.arraycopy(this.pending, this.pendingStart, samples, 0, count);
			this.pendingStart += count;
			return count;
		}

		/** @return false at the end of the sound */
		private boolean decodeMore() throws IOException {
			while (true) {
				final int got = avcodec.avcodec_receive_frame(this.context, this.frame);
				if (got >= 0) {
					try {
						convert(this.frame.extended_data(), this.frame.nb_samples());
					}
					finally {
						avutil.av_frame_unref(this.frame);
					}
					return true;
				}
				if (got == avutil.AVERROR_EOF()) {
					if (this.decoderDone) {
						return false;
					}
					this.decoderDone = true;
					// The resampler may still hold a few samples.
					convert(null, 0);
					return this.pendingEnd > 0;
				}
				if (this.inputDone) {
					return false;
				}
				final int read = avformat.av_read_frame(this.format, this.packet);
				if (read < 0) {
					this.inputDone = true;
					avcodec.avcodec_send_packet(this.context, null);
					continue;
				}
				try {
					if (this.packet.stream_index() == this.stream) {
						final int sent = avcodec.avcodec_send_packet(this.context, this.packet);
						if (sent < 0) {
							throw error("The movie sound cannot be decoded", sent);
						}
					}
				}
				finally {
					avcodec.av_packet_unref(this.packet);
				}
			}
		}

		private void convert(final PointerPointer<?> input, final int inputSamples) throws IOException {
			final int room = swresample.swr_get_out_samples(this.resampler, inputSamples);
			if (room > this.outputSamples) {
				this.output.close();
				this.output = new BytePointer((long) room * 4);
				this.outputSamples = room;
			}
			this.outputPlanes.put(0, this.output);
			final int converted = swresample.swr_convert(this.resampler, this.outputPlanes, room, input, inputSamples);
			if (converted < 0) {
				throw error("The movie sound cannot be mixed down", converted);
			}
			final int values = converted * 2;
			if (this.pending.length < values) {
				this.pending = new short[values];
			}
			this.output.position(0).limit((long) values * 2).asByteBuffer().order(java.nio.ByteOrder.nativeOrder())
					.asShortBuffer().get(this.pending, 0, values);
			this.output.limit(this.output.capacity());
			this.pendingStart = 0;
			this.pendingEnd = values;
		}

		@Override
		public void close() {
			avcodec.av_packet_free(this.packet);
			avutil.av_frame_free(this.frame);
			swresample.swr_free(this.resampler);
			avcodec.avcodec_free_context(this.context);
			avformat.avformat_close_input(this.format);
			this.outputPlanes.close();
			this.output.close();
		}
	}
}
