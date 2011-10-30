package fi.harism.glsl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.content.Context;
import android.opengl.GLES20;

public class GlslWorld {

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
			{ { 6, 7, 5 }, { 0 }, { 5 } }, { { 5, 7, 4 }, { 0 }, { 5 } },
			{ { 7, 3, 4 }, { 1 }, { 1 } }, { { 4, 3, 0 }, { 1 }, { 1 } },
			{ { 2, 6, 1 }, { 1 }, { 0 } }, { { 1, 6, 5 }, { 1 }, { 0 } },
			{ { 0, 1, 4 }, { 2 }, { 2 } }, { { 4, 1, 5 }, { 2 }, { 2 } },
			{ { 7, 6, 3 }, { 2 }, { 3 } }, { { 3, 6, 2 }, { 2 }, { 3 } } };

	private FloatBuffer mTriangleVertices;
	private Context mContext;

	private int mProgram;

	private int muViewMatrixHandle;
	private int muProjectionMatrixHandle;

	private int muTranslateVectorHandle;
	private int muRotationMatrixHandle;
	private int muScaleFloatHandle;
	private int maPositionHandle;
	private int maColorHandle;
	private int maNormalHandle;

	private static final int CUBE_SCROLLER_COUNT = 50;
	private static final int CUBE_CURVE_COUNT = 10;

	private static final float CUBE_SCROLLER_NEAR = 20f;
	private static final float CUBE_SCROLLER_FAR = -20f;

	private CubeData[] mCubeData = new CubeData[CUBE_SCROLLER_COUNT
			+ CUBE_CURVE_COUNT];

	private float[] mRotateMatrix = new float[16];

	public GlslWorld(Context context) {
		mContext = context;

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

		for (int idx = 0; idx < CUBE_SCROLLER_COUNT; ++idx) {
			CubeData cubeData = new CubeData();
			mCubeData[idx] = cubeData;

			cubeData.mScale = (float) (.3f * Math.random() + .3f);
			for (int r = 0; r < 3; ++r) {
				cubeData.mRotate[r] = (float) (360 * Math.random());
				cubeData.mRotateD[r] = (float) (180 * Math.random() - 90);
			}
			cubeData.mTranslate[0] = (float) (2 * Math.random() - 1);
			cubeData.mTranslate[1] = (float) (2 * Math.random() - 1);
			cubeData.mTranslate[2] = (float) Math.random()
					* (CUBE_SCROLLER_NEAR - CUBE_SCROLLER_FAR)
					- CUBE_SCROLLER_NEAR;
			cubeData.mTranslateD[2] = (float) (4 * Math.random() + 1);
		}

		for (int idx = CUBE_SCROLLER_COUNT; idx < mCubeData.length; ++idx) {
			CubeData cubeData = new CubeData();
			mCubeData[idx] = cubeData;

			double t = Math.PI * (idx - CUBE_SCROLLER_COUNT)
					/ (CUBE_CURVE_COUNT - 1);

			cubeData.mScale = (float) (.6f * Math.random() + .3f);
			for (int r = 0; r < 3; ++r) {
				cubeData.mRotate[r] = (float) (360 * Math.random());
			}
			cubeData.mTranslate[0] = (float) (2 * Math.cos(t));
			cubeData.mTranslate[1] = (float) (2 * Math.sin(t));
		}

	}

	public void onDrawFrame(float[] viewMatrix, float[] projectionMatrix) {
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

		mTriangleVertices.position(TRIANGLE_VERTICES_DATA_NORMAL_OFFSET);
		GLES20.glVertexAttribPointer(maNormalHandle, 3, GLES20.GL_FLOAT, false,
				TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
		GLES20.glEnableVertexAttribArray(maNormalHandle);

		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glFrontFace(GLES20.GL_CCW);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glDepthFunc(GLES20.GL_LEQUAL);

		for (int idx = 0; idx < mCubeData.length; ++idx) {
			CubeData cubeData = mCubeData[idx];
			GlslUtils.setRotateM(mRotateMatrix, cubeData.mRotate);

			GLES20.glUniformMatrix4fv(muViewMatrixHandle, 1, false, viewMatrix,
					0);
			GLES20.glUniformMatrix4fv(muProjectionMatrixHandle, 1, false,
					projectionMatrix, 0);
			GLES20.glUniformMatrix4fv(muRotationMatrixHandle, 1, false,
					mRotateMatrix, 0);
			GLES20.glUniform3fv(muTranslateVectorHandle, 1,
					cubeData.mTranslate, 0);
			GLES20.glUniform1f(muScaleFloatHandle, cubeData.mScale);
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
		maNormalHandle = GLES20.glGetAttribLocation(mProgram, "aNormal");
		if (maNormalHandle == -1) {
			throw new RuntimeException(
					"Could not get attrib location for aNormal");
		}
		muViewMatrixHandle = GLES20.glGetUniformLocation(mProgram,
				"uViewMatrix");
		if (muViewMatrixHandle == -1) {
			throw new RuntimeException(
					"Could not get attrib location for uViewMatrix");
		}
		muProjectionMatrixHandle = GLES20.glGetUniformLocation(mProgram,
				"uProjectionMatrix");
		if (muProjectionMatrixHandle == -1) {
			throw new RuntimeException(
					"Could not get attrib location for uProjectionMatrix");
		}
		muRotationMatrixHandle = GLES20.glGetUniformLocation(mProgram,
				"uRotationMatrix");
		if (muRotationMatrixHandle == -1) {
			throw new RuntimeException(
					"Could not get attrib location for uRotationMatrix");
		}
		muTranslateVectorHandle = GLES20.glGetUniformLocation(mProgram,
				"uTranslateVector");
		if (muTranslateVectorHandle == -1) {
			throw new RuntimeException(
					"Could not get attrib location for uTranslateVector");
		}
		muScaleFloatHandle = GLES20.glGetUniformLocation(mProgram,
				"uScaleFloat");
		if (muScaleFloatHandle == -1) {
			throw new RuntimeException(
					"Could not get attrib location for uScaleFloat");
		}

	}

	public void updateScene(float timeDiff) {
		for (int i = 0; i < mCubeData.length; ++i) {
			CubeData cube = mCubeData[i];
			for (int j = 0; j < 3; ++j) {
				cube.mTranslate[j] += timeDiff * cube.mTranslateD[j];
				while (cube.mTranslate[j] < CUBE_SCROLLER_FAR) {
					cube.mTranslate[j] += CUBE_SCROLLER_NEAR
							- CUBE_SCROLLER_FAR;
				}
				while (cube.mTranslate[j] > CUBE_SCROLLER_NEAR) {
					cube.mTranslate[j] -= CUBE_SCROLLER_NEAR
							- CUBE_SCROLLER_FAR;
				}

				cube.mRotate[j] += timeDiff * cube.mRotateD[j];
				while (cube.mRotate[j] < 0f) {
					cube.mRotate[j] += 360f;
				}
				while (cube.mRotate[j] > 360f) {
					cube.mRotate[j] -= 360f;
				}
			}
		}
	}

	private class CubeData {
		public float mScale = 1f;
		private float[] mTranslate = new float[3];
		private float[] mTranslateD = new float[3];
		private float[] mRotate = new float[3];
		private float[] mRotateD = new float[3];
	}

}
