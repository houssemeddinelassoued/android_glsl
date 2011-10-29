package fi.harism.glsl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.content.Context;
import android.opengl.GLES20;

public class GlslWorld {

	private static final int FLOAT_SIZE_BYTES = 4;
	private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 6 * FLOAT_SIZE_BYTES;
	private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
	private static final int TRIANGLE_VERTICES_DATA_COL_OFFSET = 3;

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

	private static final int[][][] mCubeIndices = {
			// { vertice indices }, { color index }
			{ { 3, 2, 0 }, { 0 } }, { { 0, 2, 1 }, { 0 } },
			{ { 6, 7, 5 }, { 0 } }, { { 5, 7, 4 }, { 0 } },
			{ { 7, 3, 4 }, { 1 } }, { { 4, 3, 0 }, { 1 } },
			{ { 2, 6, 1 }, { 1 } }, { { 1, 6, 5 }, { 1 } },
			{ { 0, 1, 4 }, { 2 } }, { { 4, 1, 5 }, { 2 } },
			{ { 7, 6, 3 }, { 2 } }, { { 3, 6, 2 }, { 2 } } };

	private FloatBuffer mTriangleVertices;
	private Context mContext;

	private int mProgram;
	private int maScaleFloatHandle;
	private int maMovVectorHandle;
	private int maRotVectorHandle;
	private int muMVPMatrixHandle;
	private int maPositionHandle;
	private int maColorHandle;

	private CubeData[] mCubeData = new CubeData[100];

	public GlslWorld(Context context) {
		mContext = context;

		ByteBuffer buffer = ByteBuffer.allocateDirect(3 * mCubeIndices.length
				* TRIANGLE_VERTICES_DATA_STRIDE_BYTES);
		mTriangleVertices = buffer.order(ByteOrder.nativeOrder())
				.asFloatBuffer();

		mTriangleVertices.position(0);
		for (int idx = 0; idx < mCubeIndices.length; ++idx) {
			int colorIndex = mCubeIndices[idx][1][0];
			for (int pidx = 0; pidx < 3; ++pidx) {
				int posIndex = mCubeIndices[idx][0][pidx];
				mTriangleVertices.put(mCubeVertices[posIndex]);
				mTriangleVertices.put(mCubeColors[colorIndex]);
			}
		}
		mTriangleVertices.position(0);

		mCubeData[0] = new CubeData();
		for (int idx = 1; idx < mCubeData.length; ++idx) {
			mCubeData[idx] = new CubeData();
			mCubeData[idx].mMovX = (float) (8 * Math.random() - 4);
			mCubeData[idx].mMovZ = (float) (8 * Math.random() - 4);
			mCubeData[idx].mScale = (float) (.3f * Math.random() + .1f);
			mCubeData[idx].mRotDX = (float) (2 * Math.random() - 1);
			mCubeData[idx].mRotDY = (float) (2 * Math.random() - 1);
			mCubeData[idx].mRotDZ = (float) (2 * Math.random() - 1);
		}

	}

	public void onDrawFrame(float[] worldMatrix) {
		GLES20.glClearColor(0.4f, 0.5f, 0.6f, 1.0f);
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

		GLES20.glUseProgram(mProgram);

		mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
		GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT,
				false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
		GLES20.glEnableVertexAttribArray(maPositionHandle);

		mTriangleVertices.position(TRIANGLE_VERTICES_DATA_COL_OFFSET);
		GLES20.glVertexAttribPointer(maColorHandle, 3, GLES20.GL_FLOAT, false,
				TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
		GLES20.glEnableVertexAttribArray(maColorHandle);

		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glFrontFace(GLES20.GL_CCW);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glDepthFunc(GLES20.GL_LEQUAL);

		for (int idx = 0; idx < mCubeData.length; ++idx) {
			CubeData cubeData = mCubeData[idx];

			cubeData.mRotX += cubeData.mRotDX;
			while (cubeData.mRotX < 0f) {
				cubeData.mRotX += 360f;
			}
			while (cubeData.mRotX > 360f) {
				cubeData.mRotX -= 360f;
			}
			cubeData.mRotY += cubeData.mRotDY;
			while (cubeData.mRotY < 0f) {
				cubeData.mRotY += 360f;
			}
			while (cubeData.mRotY > 360f) {
				cubeData.mRotY -= 360f;
			}
			cubeData.mRotZ += cubeData.mRotDZ;
			while (cubeData.mRotZ < 0f) {
				cubeData.mRotZ += 360f;
			}
			while (cubeData.mRotZ > 360f) {
				cubeData.mRotZ -= 360f;
			}

			GLES20.glVertexAttrib1f(maScaleFloatHandle, cubeData.mScale);
			GLES20.glVertexAttrib3f(maMovVectorHandle, cubeData.mMovX,
					cubeData.mMovY, cubeData.mMovZ);
			GLES20.glVertexAttrib3f(maRotVectorHandle, cubeData.mRotX,
					cubeData.mRotY, cubeData.mRotZ);

			GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, worldMatrix,
					0);
			GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mCubeIndices.length * 3);
		}
	}

	public void onSurfaceCreated() {
		String vertexShader = mContext.getString(R.string.shader_vertex);
		String fragmentShader = mContext.getString(R.string.shader_fragment);
		mProgram = GlslUtils.createProgram(vertexShader, fragmentShader);
		if (mProgram == 0) {
			return;
		}

		maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
		if (maPositionHandle == -1) {
			throw new RuntimeException(
					"Could not get attrib location for aPosition");
		}
		maColorHandle = GLES20.glGetAttribLocation(mProgram, "aColor");
		if (maColorHandle == -1) {
			throw new RuntimeException(
					"Could not get attrib location for aColor");
		}
		maScaleFloatHandle = GLES20
				.glGetAttribLocation(mProgram, "aScaleFloat");
		if (maScaleFloatHandle == -1) {
			throw new RuntimeException(
					"Could not get attrib location for aScaleFloat");
		}
		maRotVectorHandle = GLES20.glGetAttribLocation(mProgram, "aRotVector");
		if (maRotVectorHandle == -1) {
			throw new RuntimeException(
					"Could not get attrib location for aRotVector");
		}
		maMovVectorHandle = GLES20.glGetAttribLocation(mProgram, "aMovVector");
		if (maMovVectorHandle == -1) {
			throw new RuntimeException(
					"Could not get attrib location for aMovVector");
		}

		muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
		if (muMVPMatrixHandle == -1) {
			throw new RuntimeException(
					"Could not get attrib location for uMVPMatrix");
		}
	}

	private class CubeData {
		public float mMovX = 0;
		public float mMovY = 0;
		public float mMovZ = 0;

		public float mScale = 1f;

		public float mRotX = 0;
		public float mRotY = 0;
		public float mRotZ = 0;
		public float mRotDX = 0;
		public float mRotDY = 0;
		public float mRotDZ = 0;
	}

}
