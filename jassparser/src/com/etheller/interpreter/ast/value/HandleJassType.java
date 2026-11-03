package com.etheller.interpreter.ast.value;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;

import com.etheller.interpreter.ast.value.visitor.SuperTypeVisitor;

public class HandleJassType implements JassType {
	private HandleJassType superType;
	private final String name;
	private HandleJassTypeConstructor constructorNative;
	private HandleJassTypeConstructor destructorNative;
	private ToIntFunction<Object> enumValue;
	private Map<Integer, UnknownEnumHandle> unknownEnumHandles;

	public HandleJassType(final HandleJassType superType, final String name) {
		this.superType = superType;
		this.name = name;
	}

	@Override
	public boolean isAssignableFrom(JassType valueType) {
		while (valueType != null) {
			if (this == valueType) {
				return true;
			}
			valueType = valueType.visit(SuperTypeVisitor.getInstance());
		}
		return false;
	}

	@Override
	public String getName() {
		return this.name;
	}

	public HandleJassType getSuperType() {
		return this.superType;
	}

	public void setSuperType(final HandleJassType superType) {
		this.superType = superType;
	}

	@Override
	public <TYPE> TYPE visit(final JassTypeVisitor<TYPE> visitor) {
		return visitor.accept(this);
	}

	@Override
	public boolean isNullable() {
		return true;
	}

	@Override
	public HandleJassValue getNullValue() {
		return new HandleJassValue(this, null);
	}

	public void setConstructorNative(final HandleJassTypeConstructor constructorNative) {
		this.constructorNative = constructorNative;
	}

	public void setDestructorNative(final HandleJassTypeConstructor constructorNative) {
		this.destructorNative = constructorNative;
	}

	public HandleJassTypeConstructor getConstructorNative() {
		return this.constructorNative;
	}

	public HandleJassTypeConstructor getDestructorNative() {
		return this.destructorNative;
	}

	public void setEnumValue(final ToIntFunction<Object> enumValue) {
		this.enumValue = enumValue;
	}

	public boolean isEnumZero(final Object javaValue) {
		return (this.enumValue != null) && (javaValue != null) && (this.enumValue.applyAsInt(javaValue) == 0);
	}

	public HandleJassValue getUnknownEnumValue(final int value) {
		if (this.unknownEnumHandles == null) {
			this.unknownEnumHandles = new HashMap<>();
		}
		return new HandleJassValue(this, this.unknownEnumHandles.computeIfAbsent(value, UnknownEnumHandle::new));
	}

}
