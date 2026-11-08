package com.etheller.interpreter.ast.util;

import java.nio.charset.StandardCharsets;

/**
 * The original game's StringHash: Bob Jenkins' lookup2 hash (initial value 0) of the UTF-8 bytes, with ASCII letters
 * upper-cased and '/' read as '\', so that it does not depend on case or on the kind of path separator.
 */
public final class JassStringHash {
	private JassStringHash() {
	}

	public static int hash(final String string) {
		if ((string == null) || string.isEmpty()) {
			return 0;
		}
		final byte[] key = string.getBytes(StandardCharsets.UTF_8);
		for (int i = 0; i < key.length; i++) {
			final byte b = key[i];
			if ((b >= 'a') && (b <= 'z')) {
				key[i] = (byte) (b - ('a' - 'A'));
			}
			else if (b == '/') {
				key[i] = '\\';
			}
		}
		final int[] abc = { 0x9e3779b9, 0x9e3779b9, 0 };
		int offset = 0;
		int length = key.length;
		while (length >= 12) {
			abc[0] += word(key, offset);
			abc[1] += word(key, offset + 4);
			abc[2] += word(key, offset + 8);
			mix(abc);
			offset += 12;
			length -= 12;
		}
		abc[2] += key.length;
		// The low byte of c holds the length, so the last bytes go into its upper three.
		switch (length) {
		case 11:
			abc[2] += (key[offset + 10] & 0xFF) << 24;
		case 10:
			abc[2] += (key[offset + 9] & 0xFF) << 16;
		case 9:
			abc[2] += (key[offset + 8] & 0xFF) << 8;
		case 8:
			abc[1] += (key[offset + 7] & 0xFF) << 24;
		case 7:
			abc[1] += (key[offset + 6] & 0xFF) << 16;
		case 6:
			abc[1] += (key[offset + 5] & 0xFF) << 8;
		case 5:
			abc[1] += key[offset + 4] & 0xFF;
		case 4:
			abc[0] += (key[offset + 3] & 0xFF) << 24;
		case 3:
			abc[0] += (key[offset + 2] & 0xFF) << 16;
		case 2:
			abc[0] += (key[offset + 1] & 0xFF) << 8;
		case 1:
			abc[0] += key[offset] & 0xFF;
		default:
			break;
		}
		mix(abc);
		return abc[2];
	}

	private static void mix(final int[] abc) {
		int a = abc[0];
		int b = abc[1];
		int c = abc[2];
		a -= b; a -= c; a ^= (c >>> 13);
		b -= c; b -= a; b ^= (a << 8);
		c -= a; c -= b; c ^= (b >>> 13);
		a -= b; a -= c; a ^= (c >>> 12);
		b -= c; b -= a; b ^= (a << 16);
		c -= a; c -= b; c ^= (b >>> 5);
		a -= b; a -= c; a ^= (c >>> 3);
		b -= c; b -= a; b ^= (a << 10);
		c -= a; c -= b; c ^= (b >>> 15);
		abc[0] = a;
		abc[1] = b;
		abc[2] = c;
	}

	private static int word(final byte[] key, final int offset) {
		return (key[offset] & 0xFF) | ((key[offset + 1] & 0xFF) << 8) | ((key[offset + 2] & 0xFF) << 16)
				| ((key[offset + 3] & 0xFF) << 24);
	}
}
