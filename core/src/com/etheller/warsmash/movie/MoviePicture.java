package com.etheller.warsmash.movie;

import java.nio.ByteBuffer;

/**
 * One decoded picture of a movie in its three YUV 4:2:0 planes, each kept row after row with no padding, and the
 * colour rules the planes are to be read with.
 */
public final class MoviePicture {
	private int width;
	private int height;
	private boolean fullRange;
	private boolean bt709;
	private long frame;
	private ByteBuffer y = ByteBuffer.allocateDirect(0);
	private ByteBuffer u = ByteBuffer.allocateDirect(0);
	private ByteBuffer v = ByteBuffer.allocateDirect(0);

	/**
	 * Sets the size and colours of the picture about to be written; the planes then hold exactly that many bytes, from
	 * position 0.
	 *
	 * @param fullRange true when the samples use all of 0 to 255, false for the video range of 16 to 235
	 * @param bt709     true for the colours of high definition video, false for those of standard definition
	 */
	public void setFormat(final int width, final int height, final boolean fullRange, final boolean bt709,
			final long frame) {
		this.width = width;
		this.height = height;
		this.fullRange = fullRange;
		this.bt709 = bt709;
		this.frame = frame;
		this.y = sized(this.y, width * height);
		final int chroma = getChromaWidth() * getChromaHeight();
		this.u = sized(this.u, chroma);
		this.v = sized(this.v, chroma);
	}

	private static ByteBuffer sized(final ByteBuffer buffer, final int bytes) {
		final ByteBuffer sized = buffer.capacity() >= bytes ? buffer : ByteBuffer.allocateDirect(bytes);
		sized.clear().limit(bytes);
		return sized;
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	public int getChromaWidth() {
		return (this.width + 1) / 2;
	}

	public int getChromaHeight() {
		return (this.height + 1) / 2;
	}

	public boolean isFullRange() {
		return this.fullRange;
	}

	public boolean isBt709() {
		return this.bt709;
	}

	/** @return the number of the movie's frame this picture shows, from 0 */
	public long getFrame() {
		return this.frame;
	}

	public ByteBuffer getY() {
		return this.y;
	}

	public ByteBuffer getU() {
		return this.u;
	}

	public ByteBuffer getV() {
		return this.v;
	}
}
