package com.etheller.warsmash.viewer5.handlers.blp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * The decoded mip levels of an image as RGBA bytes, the largest first, each level half the size of the one above
 * (at least one pixel).
 */
public final class MipLevels {
	private final List<int[]> sizes = new ArrayList<>();
	private final List<ByteBuffer> pixels = new ArrayList<>();

	void add(final int width, final int height, final byte[] rgba) {
		final ByteBuffer buffer = ByteBuffer.allocateDirect(rgba.length).order(ByteOrder.nativeOrder());
		buffer.put(rgba);
		buffer.position(0);
		add(width, height, buffer);
	}

	void add(final int width, final int height, final ByteBuffer rgba) {
		this.sizes.add(new int[] { width, height });
		this.pixels.add(rgba);
	}

	public int count() {
		return this.pixels.size();
	}

	public int getWidth(final int level) {
		return this.sizes.get(level)[0];
	}

	public int getHeight(final int level) {
		return this.sizes.get(level)[1];
	}

	/**
	 * @return direct buffer of width * height RGBA pixels, rows from the top
	 */
	public ByteBuffer getRgba(final int level) {
		return this.pixels.get(level);
	}

	/** The bytes of every level together. */
	public long byteCount() {
		long bytes = 0;
		for (final int[] size : this.sizes) {
			bytes += (long) size[0] * size[1] * 4;
		}
		return bytes;
	}

	/** The width or height of a level of an image with this full size. */
	static int size(final int full, final int level) {
		return Math.max(1, full >> level);
	}

	/** How many levels a full chain has, down to one pixel. */
	static int levelCount(final int width, final int height) {
		int count = 1;
		for (int size = Math.max(width, height); size > 1; size >>= 1) {
			count++;
		}
		return count;
	}

	/** The first level of a chain within the size limit (0 for no limit), never past the last level there is. */
	static int firstWithin(final int width, final int height, final int maxSize, final int levels) {
		int first = 0;
		while ((maxSize > 0) && (first < (levels - 1)) && (Math.max(size(width, first), size(height, first)) > maxSize)) {
			first++;
		}
		return first;
	}
}
