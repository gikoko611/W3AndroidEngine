package com.etheller.warsmash.viewer5.handlers.w3x.environment;

import com.badlogic.gdx.graphics.GL30;
import com.etheller.warsmash.util.RenderMathUtils;

public class Shapes {
	public static final Shapes INSTANCE = new Shapes();

	float[][] quadVertices = { { 1, 1 }, { 0, 1 }, { 0, 0 }, { 1, 0 } };

	int[][] quadIndices = { { 1, 3, 0 }, { 2, 3, 1 } };

	// Buffers are owned by the caller: on Android the process outlives the GL context between games, and names kept
	// in a static from the previous context point at other objects' data in the next one.
	public int createQuadVertexBuffer(final GL30 gl) {
		final int buffer = gl.glGenBuffer();
		gl.glBindBuffer(GL30.GL_ARRAY_BUFFER, buffer);
		gl.glBufferData(GL30.GL_ARRAY_BUFFER, this.quadVertices.length * 8, RenderMathUtils.wrapPairs(this.quadVertices),
				GL30.GL_STATIC_DRAW);
		return buffer;
	}

	public int createQuadIndexBuffer(final GL30 gl) {
		final int buffer = gl.glGenBuffer();
		gl.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, buffer);
		gl.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, this.quadIndices.length * 3 * 4,
				RenderMathUtils.wrap(this.quadIndices), GL30.GL_STATIC_DRAW);
		return buffer;
	}
}
