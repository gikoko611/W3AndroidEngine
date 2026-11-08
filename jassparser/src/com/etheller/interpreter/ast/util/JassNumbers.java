package com.etheller.interpreter.ast.util;

import java.math.BigInteger;

/**
 * Numbers as the original game's script engine has them. Its reals are 32-bit floats, but its arithmetic is done in
 * software: results are truncated toward zero instead of rounded, division multiplies by an approximate reciprocal,
 * and values below the smallest normal float become zero. Reals are kept in doubles that always hold such a value.
 */
public final class JassNumbers {
	private static final double TWO_POW_31 = 2147483648.0;
	private static final double TWO_POW_128 = 0x1p128;
	private static final double EQUAL_TOLERANCE = 0.001f;
	private static final long LOW_MANTISSA_BITS = (1L << 29) - 1;

	private JassNumbers() {
	}

	/**
	 * Cuts a value to a float's 24-bit mantissa, toward zero. The exponent of the software float has no infinity:
	 * up to 2^129 it is still a number, and above that it runs into the sign bit.
	 */
	public static double chop(final double value) {
		if ((value == 0) || Double.isNaN(value) || Double.isInfinite(value)) {
			return Double.isNaN(value) ? 0 : value == 0 ? 0 : Math.copySign(TWO_POW_128, value);
		}
		final int exponent = Math.getExponent(value);
		if (exponent < -126) {
			return 0;
		}
		final double chopped = Double.longBitsToDouble(Double.doubleToRawLongBits(value) & ~LOW_MANTISSA_BITS);
		if (exponent > 128) {
			return -chopped * 0x1p-256;
		}
		return chopped;
	}

	/**
	 * The result of a native function as a real: the nearest float, with infinity kept as the first value past the
	 * float range and NaN read as zero.
	 */
	public static double round(final double value) {
		if (Double.isNaN(value)) {
			return 0;
		}
		final double magnitude = Math.abs(value);
		if (magnitude >= TWO_POW_128) {
			return chop(value);
		}
		if ((magnitude != 0) && (Math.getExponent(value) < -126)) {
			return 0;
		}
		return (float) value;
	}

	/**
	 * An integer as a real, as the original game converts it: toward zero, not to the nearest float.
	 */
	public static double fromInt(final int value) {
		return chop(value);
	}

	public static double add(final double left, final double right) {
		if (left == 0) {
			return right;
		}
		if (right == 0) {
			return left;
		}
		final int leftExponent = Math.getExponent(left);
		final int rightExponent = Math.getExponent(right);
		// The smaller operand is shifted out completely before it reaches the kept bits.
		if (Math.abs(leftExponent - rightExponent) > 22) {
			return leftExponent > rightExponent ? left : right;
		}
		return chop(left + right);
	}

	public static double subtract(final double left, final double right) {
		return add(left, -right);
	}

	public static double multiply(final double left, final double right) {
		return chop(left * right);
	}

	public static double divide(final double dividend, final double divisor) {
		if (divisor == 0) {
			throw new ArithmeticException("Division by zero");
		}
		if (isPowerOfTwo(divisor)) {
			return chop(dividend / divisor);
		}
		return chop(dividend * reciprocal(divisor));
	}

	/**
	 * The reciprocal the original game divides with: a little above the true one, by about 1.25 units in the last
	 * place, then cut to a float.
	 */
	private static double reciprocal(final double value) {
		final double exact = 1 / value;
		if (isPowerOfTwo(value)) {
			return exact;
		}
		return chop(exact + (1.25 * Math.copySign(Math.scalb(1.0, Math.getExponent(exact) - 23), exact)));
	}

	private static boolean isPowerOfTwo(final double value) {
		return (Double.doubleToRawLongBits(value) & 0x000FFFFFFFFFFFFFL) == 0;
	}

	public static boolean equal(final double left, final double right) {
		return Math.abs(subtract(left, right)) <= EQUAL_TOLERANCE;
	}

	/**
	 * A real literal in script source: the whole part and the fraction part are read separately and then added.
	 */
	public static double parseLiteral(final String text) {
		final int dot = text.indexOf('.');
		if (dot < 0) {
			return digitsAsReal(text);
		}
		final double whole = digitsAsReal(text.substring(0, dot));
		final String fraction = text.substring(dot + 1);
		if (fraction.isEmpty()) {
			return whole;
		}
		return add(whole, multiply(digitsAsReal(fraction), reciprocalOfPowerOfTen(fraction.length())));
	}

	/**
	 * S2R: an optional sign, digits, a point and more digits, up to the first other character. All the digits are
	 * read as one number, which is then scaled by the fraction's power of ten.
	 */
	public static double stringToReal(final String string) {
		if (string == null) {
			return 0;
		}
		final int length = string.length();
		int i = 0;
		boolean negative = false;
		if ((i < length) && ((string.charAt(i) == '-') || (string.charAt(i) == '+'))) {
			negative = string.charAt(i) == '-';
			i++;
		}
		final StringBuilder digits = new StringBuilder();
		while ((i < length) && isDigit(string.charAt(i))) {
			digits.append(string.charAt(i++));
		}
		int fractionDigits = 0;
		if ((i < length) && (string.charAt(i) == '.')) {
			i++;
			while ((i < length) && isDigit(string.charAt(i))) {
				digits.append(string.charAt(i++));
				fractionDigits++;
			}
		}
		double value = digitsAsReal(digits.toString());
		if (fractionDigits > 0) {
			value = multiply(value, reciprocalOfPowerOfTen(fractionDigits));
		}
		return negative ? -value : value;
	}

	private static double reciprocalOfPowerOfTen(final int exponent) {
		if (exponent > 45) {
			return 0;
		}
		return reciprocal(StrictMath.pow(10, exponent));
	}

	private static double digitsAsReal(final String digits) {
		if (digits.isEmpty()) {
			return 0;
		}
		if (digits.length() <= 15) {
			return chop(Long.parseLong(digits));
		}
		final BigInteger value = new BigInteger(digits);
		final int shift = value.bitLength() - 24;
		return chop(Math.scalb(value.shiftRight(shift).doubleValue(), shift));
	}

	private static boolean isDigit(final char c) {
		return (c >= '0') && (c <= '9');
	}

	/**
	 * R2SW. The digits come from the same software arithmetic as everything else, so they show the float's error
	 * (1/3 with 9 digits is 0.333333312). Numbers of 2^31 and above are divided by ten until they fit an integer,
	 * and the digits lost that way are printed as zeros.
	 */
	public static String format(final double value, final int width, int precision) {
		if (Math.abs(value) >= TWO_POW_128) {
			return pad(value < 0 ? "-inf" : "inf", width);
		}
		if (precision < 0) {
			precision = 1;
		}
		else if (precision > 9) {
			precision = 9;
		}
		double magnitude = Math.abs(value);
		int zeros = 0;
		while (magnitude >= TWO_POW_31) {
			magnitude = divide(magnitude, 10);
			zeros++;
		}
		long whole = (long) magnitude;
		final double fraction = subtract(magnitude, whole);
		final long scale = (long) StrictMath.pow(10, precision);
		long digits = (long) add(multiply(fraction, scale), 0.5);
		if (digits >= scale) {
			whole++;
			digits -= scale;
		}
		if (zeros > 0) {
			digits = 0;
		}
		final StringBuilder builder = new StringBuilder();
		if (value < 0) {
			builder.append('-');
		}
		builder.append(whole);
		for (int i = 0; i < zeros; i++) {
			builder.append('0');
		}
		builder.append('.');
		if (precision == 0) {
			builder.append('0');
		}
		else {
			final String digitText = Long.toString(digits);
			for (int i = digitText.length(); i < precision; i++) {
				builder.append('0');
			}
			builder.append(digitText);
		}
		return pad(builder.toString(), width);
	}

	private static String pad(final String text, final int width) {
		if (text.length() >= (width + 1)) {
			return text;
		}
		final StringBuilder builder = new StringBuilder(width + 1);
		for (int i = text.length(); i < (width + 1); i++) {
			builder.append(' ');
		}
		return builder.append(text).toString();
	}

	/**
	 * S2I: like C's strtol in base 10. Leading white space, a sign, then digits up to the first other character;
	 * out of range clamps to the integer limits; nothing to read gives 0.
	 */
	public static int stringToInt(final String string) {
		if (string == null) {
			return 0;
		}
		final int length = string.length();
		int i = 0;
		while ((i < length) && isSpace(string.charAt(i))) {
			i++;
		}
		boolean negative = false;
		if ((i < length) && ((string.charAt(i) == '-') || (string.charAt(i) == '+'))) {
			negative = string.charAt(i) == '-';
			i++;
		}
		long value = 0;
		while ((i < length) && isDigit(string.charAt(i))) {
			if (value <= Integer.MAX_VALUE) {
				value = (value * 10) + (string.charAt(i) - '0');
			}
			i++;
		}
		if (negative) {
			value = -value;
		}
		return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
	}

	private static boolean isSpace(final char c) {
		return (c == ' ') || ((c >= '\t') && (c <= '\r'));
	}
}
