package com.etheller.interpreter.ast.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Values measured in the original game (1.29).
 */
class JassNumbersTest {

	private static void assertBits(final int expected, final double actual) {
		assertEquals(Integer.toHexString(expected), Integer.toHexString(Float.floatToRawIntBits((float) actual)));
	}

	private static double real(final int bits) {
		return Float.intBitsToFloat(bits);
	}

	@Test
	void literalsReadTheFractionWithTheGameReciprocal() {
		assertBits(0x3F800000, JassNumbers.parseLiteral("1.0"));
		assertBits(0x3DCCCCCE, JassNumbers.parseLiteral("0.1"));
		assertBits(0x3C23D70B, JassNumbers.parseLiteral("0.01"));
		assertBits(0x3EAAAAAB, JassNumbers.parseLiteral("0.33333333"));
		assertBits(0x3F800000, JassNumbers.parseLiteral("1.0000001"));
		assertBits(0x42F6E978, JassNumbers.parseLiteral("123.456"));
		assertBits(0x3E99999A, JassNumbers.parseLiteral("0.3"));
		assertBits(0x3F8020C4, JassNumbers.parseLiteral("1.001"));
		assertBits(0x3A83126F, JassNumbers.parseLiteral("0.001"));
		assertBits(0x40490FDA, JassNumbers.parseLiteral("3.14159265"));
		assertBits(0x4CEB79A2, JassNumbers.parseLiteral("123456789.0"));
		assertBits(0x4B800000, JassNumbers.parseLiteral("16777217.0"));
		assertBits(0x4B800001, JassNumbers.parseLiteral("16777219.0"));
		assertBits(0x3A03126F, JassNumbers.parseLiteral("0.0005"));
	}

	@Test
	void arithmeticTruncatesTowardZero() {
		assertBits(0x3EAAAAAB, JassNumbers.divide(1, 3));
		assertBits(0x3F2AAAAB, JassNumbers.divide(2, 3));
		assertBits(0xBEAAAAAB, JassNumbers.divide(-1, 3));
		assertBits(0x3DCCCCCE, JassNumbers.divide(1, 10));
		assertBits(0x3E124925, JassNumbers.divide(1, 7));
		assertBits(0x40555555, JassNumbers.divide(10, 3));
		final double tenth = JassNumbers.divide(1, 10);
		assertBits(0x3E99999A, JassNumbers.multiply(tenth, 3));
		assertBits(0x3F800000, JassNumbers.multiply(JassNumbers.divide(1, 3), 3));
		final double oneOneTenth = JassNumbers.divide(11, 10);
		assertBits(0x3F8CCCCD, oneOneTenth);
		assertBits(0x3F9AE148, JassNumbers.multiply(oneOneTenth, oneOneTenth));
		final double ulp = real(0x33800000);
		assertBits(0x3F800000, JassNumbers.add(1, ulp));
		assertBits(0x3F800000, JassNumbers.add(1, ulp * 3));
	}

	@Test
	void divisionByZeroThrows() {
		assertThrows(ArithmeticException.class, () -> JassNumbers.divide(1, 0));
		assertThrows(ArithmeticException.class, () -> JassNumbers.divide(0, 0));
	}

	@Test
	void integersBecomeRealsTowardZero() {
		assertBits(0x4CEB79A2, JassNumbers.fromInt(123456789));
		assertBits(0xCCEB79A2, JassNumbers.fromInt(-123456789));
		assertBits(0x4B800000, JassNumbers.fromInt(16777217));
		assertBits(0x4C000000, JassNumbers.fromInt(33554435));
		assertBits(0x4EFFFFFF, JassNumbers.fromInt(Integer.MAX_VALUE));
		assertBits(0xCF000000, JassNumbers.fromInt(Integer.MIN_VALUE));
	}

	@Test
	void equalityAllowsAThousandth() {
		final double thousandth = JassNumbers.parseLiteral("0.001");
		assertTrue(JassNumbers.equal(1, JassNumbers.add(1, JassNumbers.parseLiteral("0.0009"))));
		assertTrue(JassNumbers.equal(1, JassNumbers.add(1, JassNumbers.parseLiteral("0.00099"))));
		assertFalse(JassNumbers.equal(1, JassNumbers.add(1, JassNumbers.parseLiteral("0.00101"))));
		assertFalse(JassNumbers.equal(1, JassNumbers.add(1, JassNumbers.parseLiteral("0.0011"))));
		assertTrue(JassNumbers.equal(0, JassNumbers.parseLiteral("0.0009")));
		assertFalse(JassNumbers.equal(0, JassNumbers.parseLiteral("0.0011")));
		assertTrue(JassNumbers.equal(1000000, JassNumbers.add(1000000, JassNumbers.parseLiteral("0.0625"))));
		assertFalse(JassNumbers.equal(1000000, JassNumbers.add(1000000, 1)));
		assertFalse(JassNumbers.equal(0, JassNumbers.add(thousandth, thousandth)));
	}

	@Test
	void stringToRealReadsAllDigitsAsOneNumber() {
		assertBits(0x3DCCCCCE, JassNumbers.stringToReal("0.1"));
		assertBits(0x3FC00001, JassNumbers.stringToReal("1.5"));
		assertBits(0x3FC00001, JassNumbers.stringToReal("1.5x"));
		assertBits(0x3FC00001, JassNumbers.stringToReal("1.5e2"));
		assertBits(0x4CEB79A2, JassNumbers.stringToReal("123456789"));
		assertBits(0x3E99999A, JassNumbers.stringToReal("0.3"));
		assertBits(0xBDCCCCCE, JassNumbers.stringToReal("-0.1"));
		assertBits(0x3EAAAAAB, JassNumbers.stringToReal("0.33333333"));
		assertBits(0x40490FDB, JassNumbers.stringToReal("3.14159265"));
		assertBits(0x40200000, JassNumbers.stringToReal("+2.5"));
		assertBits(0x3F99999A, JassNumbers.stringToReal("1.2.3"));
		assertBits(0xBF000000, JassNumbers.stringToReal("-.5"));
		assertBits(0x3F800000, JassNumbers.stringToReal("1."));
		assertEquals(0, JassNumbers.stringToReal(" 5"));
		assertEquals(0, JassNumbers.stringToReal("."));
		assertEquals(0, JassNumbers.stringToReal("0x10"));
	}

	@Test
	void stringToIntIsStrtol() {
		assertEquals(Integer.MAX_VALUE, JassNumbers.stringToInt("2147483647"));
		assertEquals(Integer.MAX_VALUE, JassNumbers.stringToInt("99999999999"));
		assertEquals(Integer.MIN_VALUE, JassNumbers.stringToInt("-2147483649"));
		assertEquals(1, JassNumbers.stringToInt("1e5"));
		assertEquals(0, JassNumbers.stringToInt("--5"));
		assertEquals(0, JassNumbers.stringToInt("- 5"));
		assertEquals(-5, JassNumbers.stringToInt(" -5"));
		assertEquals(5, JassNumbers.stringToInt("\t5"));
		assertEquals(7, JassNumbers.stringToInt("007"));
		assertEquals(0, JassNumbers.stringToInt("0x10"));
	}

	@Test
	void formatShowsTheFloatError() {
		final double third = JassNumbers.divide(1, 3);
		assertEquals("0.333333312", JassNumbers.format(third, 1, 9));
		assertEquals("0.100000008", JassNumbers.format(JassNumbers.divide(1, 10), 1, 9));
		assertEquals("0.333333312", JassNumbers.format(third, 1, 20));
		assertEquals("0.063", JassNumbers.format(0.0625, 1, 3));
		assertEquals("-0.063", JassNumbers.format(-0.0625, 1, 3));
		assertEquals("0.38", JassNumbers.format(0.375, 1, 2));
		assertEquals("3.0", JassNumbers.format(2.5, 1, 0));
		assertEquals("4.0", JassNumbers.format(3.5, 1, 0));
		assertEquals("2.5", JassNumbers.format(2.5, 1, -1));
		assertEquals("         0.33", JassNumbers.format(third, 12, 2));
		assertEquals(21, JassNumbers.format(2.5, 20, 3).length());
		assertEquals("-1.500", JassNumbers.format(-1.5, 0, 3));
		assertEquals("0.000", JassNumbers.format(0x1p-25, 0, 3));
		assertEquals("-0.000", JassNumbers.format(-0x1p-25, 0, 3));
		assertEquals("281475040000000.000", JassNumbers.format(0x1p48, 0, 3));
		assertEquals("20282433280000000000000000000000.000", JassNumbers.format(0x1p104, 0, 3));
		assertEquals("123456784.000", JassNumbers.format(JassNumbers.fromInt(123456789), 0, 3));
		assertEquals("123456.781",
				JassNumbers.format(JassNumbers.parseLiteral("123456.789"), 0, 3));
		assertEquals("inf", JassNumbers.format(JassNumbers.round(Double.POSITIVE_INFINITY), 0, 3));
	}

	@Test
	void stringHashIsLookup2() {
		assertEquals(0, JassStringHash.hash(""));
		assertEquals(-1587459251, JassStringHash.hash("a"));
		assertEquals(-1587459251, JassStringHash.hash("A"));
		assertEquals(1043745117, JassStringHash.hash("abc"));
		assertEquals(-1563733934, JassStringHash.hash("Hello World"));
		assertEquals(736755776, JassStringHash.hash("Units\\Human\\Footman"));
		assertEquals(736755776, JassStringHash.hash("units/human/footman"));
		assertEquals(-242600650, JassStringHash.hash("0"));
		assertEquals(-1220873124, JassStringHash.hash("1234567890"));
		assertEquals(-1636555145, JassStringHash.hash(" "));
		assertEquals(-183744035, JassStringHash.hash("\u0105\u010d\u0119\u0117\u012f\u0161\u0173\u016b\u017e"));
		assertEquals(-229411443, JassStringHash.hash(
				"The quick brown fox jumps over the lazy dog. The quick brown fox jumps over the lazy dog!"));
		assertEquals(314524338, JassStringHash.hash("hfoo"));
		assertEquals(-612616966, JassStringHash.hash("Key"));
	}
}
