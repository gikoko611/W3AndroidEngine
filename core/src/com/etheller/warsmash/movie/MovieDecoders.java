package com.etheller.warsmash.movie;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * The platform's decoders for {@link GdxMoviePlayer}: the compressed frames are read out of the AVI file by
 * {@link AviReader}, and only turning them into pictures and the sound file into samples is left to the platform.
 */
public interface MovieDecoders {
	/**
	 * @param codec the compression four-character code of the video, VP90 for the game's movies
	 * @return a decoder for the video, or null when the platform has none for its codec
	 */
	VideoDecoder openVideo(String codec, int width, int height) throws IOException;

	/** @return the sound as 16-bit stereo samples, or null when the platform cannot decode the file */
	SoundDecoder openSound(File file) throws IOException;

	interface VideoDecoder extends Closeable {
		int PICTURE = 1;
		int NOT_YET = 0;
		int END = -1;

		/**
		 * Sends one compressed frame, from its position up to its limit.
		 *
		 * @param data  the frame, or null after the last one so the decoder gives out the pictures it still holds
		 * @param frame the frame's number, from 0, which the picture made from it carries
		 */
		void send(ByteBuffer data, long frame) throws IOException;

		/**
		 * @return {@link #PICTURE} when the next picture was written into the given one, {@link #NOT_YET} when the
		 *         decoder needs more frames first, {@link #END} when every picture of the movie has been given out
		 */
		int receive(MoviePicture into) throws IOException;
	}

	interface SoundDecoder extends Closeable {
		int getSampleRate();

		/**
		 * Reads the next samples, left and right in turn.
		 *
		 * @return how many values were written, or -1 at the end of the sound
		 */
		int read(short[] samples) throws IOException;
	}
}
