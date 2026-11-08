package com.etheller.warsmash.testing;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Builds engine objects without their constructors (a simulation or a unit needs a loaded map otherwise) and sets
 * the few fields a test needs.
 */
public final class TestObjects {
	private static final Object UNSAFE;
	private static final Method ALLOCATE;
	static {
		try {
			final Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
			final Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			UNSAFE = theUnsafe.get(null);
			ALLOCATE = unsafeClass.getMethod("allocateInstance", Class.class);
		}
		catch (final ReflectiveOperationException exc) {
			throw new ExceptionInInitializerError(exc);
		}
	}

	private TestObjects() {
	}

	public static <T> T allocate(final Class<T> type) {
		try {
			return type.cast(ALLOCATE.invoke(UNSAFE, type));
		}
		catch (final ReflectiveOperationException exc) {
			throw new IllegalStateException(exc);
		}
	}

	public static void set(final Object object, final String fieldName, final Object value) {
		try {
			field(object.getClass(), fieldName).set(object, value);
		}
		catch (final ReflectiveOperationException exc) {
			throw new IllegalStateException(exc);
		}
	}

	public static Object get(final Object object, final String fieldName) {
		try {
			return field(object.getClass(), fieldName).get(object);
		}
		catch (final ReflectiveOperationException exc) {
			throw new IllegalStateException(exc);
		}
	}

	public static Object call(final Object object, final String methodName, final Class<?>[] parameterTypes,
			final Object... arguments) {
		try {
			for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
				try {
					final Method method = type.getDeclaredMethod(methodName, parameterTypes);
					method.setAccessible(true);
					return method.invoke(object, arguments);
				}
				catch (final NoSuchMethodException exc) {
					// declared further up
				}
			}
			throw new NoSuchMethodException(methodName);
		}
		catch (final ReflectiveOperationException exc) {
			throw new IllegalStateException(exc);
		}
	}

	private static Field field(final Class<?> start, final String fieldName) throws NoSuchFieldException {
		for (Class<?> type = start; type != null; type = type.getSuperclass()) {
			try {
				final Field field = type.getDeclaredField(fieldName);
				field.setAccessible(true);
				return field;
			}
			catch (final NoSuchFieldException exc) {
				// declared further up
			}
		}
		throw new NoSuchFieldException(fieldName);
	}
}
