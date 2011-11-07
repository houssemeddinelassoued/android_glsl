package fi.harism.glsl.object;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES20;

public final class GlslCube extends GlslObject {

	private static final int FLOAT_SIZE_BYTES = 4;
	private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 9 * FLOAT_SIZE_BYTES;
	private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
	private static final int TRIANGLE_VERTICES_DATA_COL_OFFSET = 3;
	private static final int TRIANGLE_VERTICES_DATA_NORMAL_OFFSET = 6;

	private static final float[][] mCubeVertices = {
			// X, Y, Z
			{ -.5f, .5f, .5f }, { .5f, .5f, .5f }, { .5f, -.5f, .5f },
			{ -.5f, -.5f, .5f },

			{ -.5f, .5f, -.5f }, { .5f, .5f, -.5f }, { .5f, -.5f, -.5f },
			{ -.5f, -.5f, -.5f }, };

	private static final float[][] mCubeColors = {
			// R, G, B
			{ 1f, 0f, 0f }, { 0f, 1f, 0f }, { 0f, 0f, 1f }, { 0f, 1f, 1f },
			{ 1f, 0f, 1f }, { 1f, 1f, 0f } };

	private static final float[][] mCubeNormals = {
			// X, Y, Z
			{ 1f, 0, 0 }, { -1f, 0, 0 }, { 0, 1f, 0 }, { 0, -1f, 0 },
			{ 0, 0, 1f }, { 0, 0, -1f } };

	private static final int[][][] mCubeIndices = {
			// { vertice indices }, { color index }
			{ { 3, 2, 0 }, { 0 }, { 4 } }, { { 0, 2, 1 }, { 0 }, { 4 } },
			{ { 6, 7, 5 }, { 1 }, { 5 } }, { { 5, 7, 4 }, { 1 }, { 5 } },
			{ { 7, 3, 4 }, { 2 }, { 1 } }, { { 4, 3, 0 }, { 2 }, { 1 } },
			{ { 2, 6, 1 }, { 3 }, { 0 } }, { { 1, 6, 5 }, { 3 }, { 0 } },
			{ { 0, 1, 4 }, { 4 }, { 2 } }, { { 4, 1, 5 }, { 4 }, { 2 } },
			{ { 7, 6, 3 }, { 5 }, { 3 } }, { { 3, 6, 2 }, { 5 }, { 3 } } };

	private FloatBuffer mTriangleVertices;

	public GlslCube() {
		ByteBuffer buffer = ByteBuffer.allocateDirect(3 * mCubeIndices.length
				* TRIANGLE_VERTICES_DATA_STRIDE_BYTES);
		mTriangleVertices = buffer.order(ByteOrder.nativeOrder())
				.asFloatBuffer();

		mTriangleVertices.position(0);
		for (int idx = 0; idx < mCubeIndices.length; ++idx) {
			int colorIndex = mCubeIndices[idx][1][0];
			int normalIndex = mCubeIndices[idx][2][0];
			for (int pidx = 0; pidx < 3; ++pidx) {
				int posIndex = mCubeIndices[idx][0][pidx];
				mTriangleVertices.put(mCubeVertices[posIndex]);
				mTriangleVertices.put(mCubeColors[colorIndex]);
				mTriangleVertices.put(mCubeNormals[normalIndex]);
			}
		}
		mTriangleVertices.position(0);
	}

	@Override
	public void animate(float timeDiff) {
		// TODO: Implement me
	}

	@Override
	public void draw(int mvpMId, int normalMId, int posId, int normalId,
			int colorId) {

		super.draw(mvpMId, normalMId, posId, normalId, colorId);

		mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
		GLES20.glVertexAttribPointer(posId, 3, GLES20.GL_FLOAT, false,
				TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
		GLES20.glEnableVertexAttribArray(posId);

		mTriangleVertices.position(TRIANGLE_VERTICES_DATA_NORMAL_OFFSET);
		GLES20.glVertexAttribPointer(normalId, 3, GLES20.GL_FLOAT, false,
				TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
		GLES20.glEnableVertexAttribArray(normalId);

		mTriangleVertices.position(TRIANGLE_VERTICES_DATA_COL_OFFSET);
		GLES20.glVertexAttribPointer(colorId, 3, GLES20.GL_FLOAT, false,
				TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
		GLES20.glEnableVertexAttribArray(colorId);

		GLES20.glUniformMatrix4fv(mvpMId, 1, false, getModelViewProjM(), 0);
		GLES20.glUniformMatrix4fv(normalMId, 1, false, getNormalM(), 0);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mCubeIndices.length * 3);
	}

	public void setColor(float r, float g, float b) {
		for (int i = TRIANGLE_VERTICES_DATA_COL_OFFSET; i < mTriangleVertices
				.limit(); i += 9) {
			mTriangleVertices.put(i, r);
			mTriangleVertices.put(i + 1, g);
			mTriangleVertices.put(i + 2, b);
		}
	}

}
