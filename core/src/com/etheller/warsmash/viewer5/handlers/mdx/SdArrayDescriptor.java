package com.etheller.warsmash.viewer5.handlers.mdx;

public interface SdArrayDescriptor<TYPE> {
	SdArrayDescriptor<Object> GENERIC = new SdArrayDescriptor<Object>() {
		@Override
		public Object[] create(final int size) {
			return new Object[size];
		}
	};
	SdArrayDescriptor<float[]> FLOAT_ARRAY = new SdArrayDescriptor<float[]>() {
		private final float[][] empty = new float[0][];

		@Override
		public float[][] create(final int size) {
			return size == 0 ? this.empty : new float[size][];
		}
	};
	SdArrayDescriptor<long[]> LONG_ARRAY = new SdArrayDescriptor<long[]>() {
		private final long[][] empty = new long[0][];

		@Override
		public long[][] create(final int size) {
			return size == 0 ? this.empty : new long[size][];
		}
	};

	TYPE[] create(int size);
}
