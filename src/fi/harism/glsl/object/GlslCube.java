package fi.harism.glsl.object;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES20;
import android.opengl.Matrix;
import fi.harism.glsl.GlslUtils;

public final class GlslCube implements GlslObject {

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
	private float[] mTempM;
	private float[] mPosition = new float[3];
	private float[] mPositionD = new float[3];
	private float[] mRotation = new float[3];
	private float[] mRotationD = new float[3];
	private float mScaling;

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

		mTempM = new float[16];
		mPosition = new float[3];
		mPositionD = new float[3];
		mRotation = new float[3];
		mRotationD = new float[3];
	}

	@Override
	public void animate(float timeDiff) {
		// TODO: Fixme
		for (int j = 0; j < 3; ++j) {
			mPosition[j] += timeDiff * mPositionD[j];
			while (mPosition[j] < -20) {
				mPosition[j] += 40;
			}
			while (mPosition[j] > 20) {
				mPosition[j] -= 40;
			}

			mRotation[j] += timeDiff * mRotationD[j];
			while (mRotation[j] < 0f) {
				mRotation[j] += 360f;
			}
			while (mRotation[j] > 360f) {
				mRotation[j] -= 360f;
			}
		}
	}

	@Override
	public void drawObject() {
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

	@Override
	public void setColorAttrib(int aColorHandle) {
		mTriangleVertices.position(TRIANGLE_VERTICES_DATA_COL_OFFSET);
		GLES20.glVertexAttribPointer(aColorHandle, 3, GLES20.GL_FLOAT, false,
				TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
		GLES20.glEnableVertexAttribArray(aColorHandle);
	}

	@Override
	public void setModelM(float[] modelM) {
		GlslUtils.setRotateM(modelM, mRotation);
		Matrix.scaleM(modelM, 0, mScaling, mScaling, mScaling);
		Matrix.setIdentityM(mTempM, 0);
		Matrix.translateM(mTempM, 0, mPosition[0], mPosition[1], mPosition[2]);
		Matrix.multiplyMM(modelM, 0, mTempM, 0, modelM, 0);
	}

	@Override
	public void setNormalAttrib(int aNormalHandle) {
		mTriangleVertices.position(TRIANGLE_VERTICES_DATA_NORMAL_OFFSET);
		GLES20.glVertexAttribPointer(aNormalHandle, 3, GLES20.GL_FLOAT, false,
				TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
		GLES20.glEnableVertexAttribArray(aNormalHandle);
	}

	public void setPosition(float x, float y, float z) {
		mPosition[0] = x;
		mPosition[1] = y;
		mPosition[2] = z;
	}

	@Override
	public void setPositionAttrib(int aPositionHandle) {
		mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
		GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT,
				false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
		GLES20.glEnableVertexAttribArray(aPositionHandle);
	}

	public void setPositionD(float dx, float dy, float dz) {
		mPositionD[0] = dx;
		mPositionD[1] = dy;
		mPositionD[2] = dz;
	}

	public void setRotation(float x, float y, float z) {
		mRotation[0] = x;
		mRotation[1] = y;
		mRotation[2] = z;
	}

	public void setRotationD(float dx, float dy, float dz) {
		mRotationD[0] = dx;
		mRotationD[1] = dy;
		mRotationD[2] = dz;
	}

	public void setScaling(float scaling) {
		mScaling = scaling;
	}

}
