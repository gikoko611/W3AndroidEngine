package com.etheller.interpreter.ast.value;

public class IntegerJassValue implements JassValue {
	private static final int CACHE_LOW = -128;
	private static final int CACHE_HIGH = 1024;
	private static final IntegerJassValue[] CACHE = new IntegerJassValue[(CACHE_HIGH - CACHE_LOW) + 1];
	static {
		for (int i = 0; i < CACHE.length; i++) {
			CACHE[i] = new IntegerJassValue(i + CACHE_LOW);
		}
	}
	public static final IntegerJassValue ZERO = CACHE[-CACHE_LOW];
	private final int value;

	public IntegerJassValue(final int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	@Override
	public <TYPE> TYPE visit(final JassValueVisitor<TYPE> visitor) {
		return visitor.accept(this);
	}

	@Override
	public String toString() {
		return Integer.toString(this.value);
	}

	public static IntegerJassValue of(final int value) {
		if ((value >= CACHE_LOW) && (value <= CACHE_HIGH)) {
			return CACHE[value - CACHE_LOW];
		}
		return new IntegerJassValue(value);
	}
}
