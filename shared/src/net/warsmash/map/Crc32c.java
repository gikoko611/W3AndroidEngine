package net.warsmash.map;

import java.nio.ByteBuffer;
import java.util.zip.Checksum;

/**
 * CRC-32C (Castagnoli), the same values as java.util.zip.CRC32C, which Android has only from API 34.
 */
public final class Crc32c implements Checksum {
	private static final int[] TABLE = new int[256];

	static {
		for (int n = 0; n < 256; n++) {
			int c = n;
			for (int k = 0; k < 8; k++) {
				c = (c & 1) != 0 ? (c >>> 1) ^ 0x82F63B78 : c >>> 1;
			}
			TABLE[n] = c;
		}
	}

	private int crc = 0xFFFFFFFF;

	@Override
	public void update(final int b) {
		this.crc = (this.crc >>> 8) ^ TABLE[(this.crc ^ b) & 0xFF];
	}

	@Override
	public void update(final byte[] b, final int off, final int len) {
		int c = this.crc;
		for (int i = off; i < (off + len); i++) {
			c = (c >>> 8) ^ TABLE[(c ^ b[i]) & 0xFF];
		}
		this.crc = c;
	}

	@Override
	public void update(final ByteBuffer buffer) {
		int c = this.crc;
		while (buffer.hasRemaining()) {
			c = (c >>> 8) ^ TABLE[(c ^ buffer.get()) & 0xFF];
		}
		this.crc = c;
	}

	@Override
	public long getValue() {
		return (~this.crc) & 0xFFFFFFFFL;
	}

	@Override
	public void reset() {
		this.crc = 0xFFFFFFFF;
	}
}
