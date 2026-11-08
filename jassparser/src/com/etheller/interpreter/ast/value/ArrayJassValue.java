package com.etheller.interpreter.ast.value;

import java.util.Arrays;

import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.util.HandleReferences;
import com.etheller.interpreter.ast.util.JassSettings;
import com.etheller.interpreter.ast.value.visitor.JassTypeGettingValueVisitor;

/**
 * A JASS array. As in the original game, an index outside 0 to {@link JassSettings#MAX_ARRAY_SIZE} - 1 is not an
 * error: a write there is dropped and a read gives the element type's default. The storage grows with the highest
 * index written, so the many arrays a map declares cost only what they hold.
 */
public class ArrayJassValue implements JassValue {
	private static final JassValue[] EMPTY = new JassValue[0];
	private JassValue[] data = EMPTY;
	private final ArrayJassType type;

	public ArrayJassValue(final ArrayJassType type) {
		this.type = type;
	}

	private JassValue defaultValue() {
		final JassType primitiveType = this.type.getPrimitiveType();
		if (primitiveType == JassType.INTEGER) {
			return IntegerJassValue.ZERO;
		}
		if (primitiveType == JassType.REAL) {
			return RealJassValue.ZERO;
		}
		if (primitiveType == JassType.BOOLEAN) {
			return BooleanJassValue.FALSE;
		}
		return null;
	}

	@Override
	public <TYPE> TYPE visit(final JassValueVisitor<TYPE> visitor) {
		return visitor.accept(this);
	}

	public void set(final GlobalScope globalScope, final int index, JassValue value) {
		final JassType primitiveType = this.type.getPrimitiveType();
		if (value == null) {
			if (primitiveType.isNullable()) {
				value = primitiveType.getNullValue();
			}
			else {
				throw new IllegalStateException(
						"Attempted to set " + this.type.getName() + " to null in array at index " + index + "!");
			}
		}
		final JassType valueType = value.visit(JassTypeGettingValueVisitor.getInstance());
		if (!primitiveType.isAssignableFrom(valueType)) {
			throw new IllegalStateException("Illegal type for assignment to " + primitiveType.getName() + " array: "
					+ (valueType == null ? "null" : valueType.getName()));
		}
		if ((index < 0) || (index >= JassSettings.MAX_ARRAY_SIZE)) {
			return;
		}
		if (index >= this.data.length) {
			final int oldLength = this.data.length;
			int newLength = Math.max(16, oldLength);
			while (newLength <= index) {
				newLength *= 2;
			}
			this.data = Arrays.copyOf(this.data, Math.min(newLength, JassSettings.MAX_ARRAY_SIZE));
			final JassValue defaultValue = defaultValue();
			if (defaultValue != null) {
				Arrays.fill(this.data, oldLength, this.data.length, defaultValue);
			}
		}
		HandleReferences.replace(this.data[index], value);
		this.data[index] = value;
	}

	public JassValue get(final int index) {
		if ((index < 0) || (index >= this.data.length)) {
			return defaultValue();
		}
		return this.data[index];
	}

	/** The elements stored so far; the ones past it read as the element type's default. */
	public int storedLength() {
		return this.data.length;
	}

	public ArrayJassType getType() {
		return this.type;
	}

}
