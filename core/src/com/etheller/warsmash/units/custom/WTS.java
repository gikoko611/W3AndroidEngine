package com.etheller.warsmash.units.custom;

public interface WTS {
	String KEY_PREFIX = "TRIGSTR_";

	String get(int key);

	WTS DO_NOTHING = new WTS() {
		@Override
		public String get(final int key) {
			return KEY_PREFIX + key;
		}
	};

	/**
	 * The text of a map string key, or the value itself when it is no key. As in the original, only the digits
	 * right after the prefix count ("TRIGSTR_0100x" is string 100, "TRIGSTR_+1" is string 0) and a key the table
	 * does not hold is an empty text.
	 */
	static String resolve(final WTS wts, final String value) {
		if ((wts == null) || (value == null) || !value.startsWith(KEY_PREFIX)) {
			return value;
		}
		long index = 0;
		for (int i = KEY_PREFIX.length(); (i < value.length()) && (index <= Integer.MAX_VALUE); i++) {
			final char c = value.charAt(i);
			if ((c < '0') || (c > '9')) {
				break;
			}
			index = (index * 10) + (c - '0');
		}
		final String text = index > Integer.MAX_VALUE ? null : wts.get((int) index);
		return text == null ? "" : text;
	}
}
