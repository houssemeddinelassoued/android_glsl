package fi.harism.glsl;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import fi.harism.glsl.object.Cube;
import fi.harism.glsl.object.Cubes;

public class GlslScene {

	private static final int CUBE_SCROLLER_COUNT = 200;
	private static final int CUBE_ARCH_COUNT = 5;
	private static final float CUBE_SCROLLER_NEAR = 20f;
	private static final float CUBE_SCROLLER_FAR = -20f;
	private Cubes mCubes;

	private float[] mTempMatrix = new float[16];
	private float[] mModelMatrix = new float[16];
	private float[] mModelViewMatrix = new float[16];
	private float[] mModelViewProjectionMatrix = new float[16];
	private float[] mNormalMatrix = new float[16];

	private GlslShader mShader;

	public GlslScene(Context context) {
		mShader = new GlslShader(context);
		mCubes = new Cubes();

		/*
		 * Cube cube = mCubes.addCube(); cube.mScaling = 0.1f; cube.mPosition[0]
		 * = 0.8f;
		 * 
		 * cube = mCubes.addCube(); cube.mScaling = 0.1f; cube.mPosition[0] =
		 * -0.8f; cube.mRotation[0] = 90f;
		 */

		for (int idx = 0; idx < CUBE_SCROLLER_COUNT; ++idx) {
			Cube cube = mCubes.addCube();

			cube.mScaling = (float) (.3f * Math.random() + .3f);
			for (int r = 0; r < 3; ++r) {
				cube.mRotation[r] = (float) (360 * Math.random());
				cube.mRotationD[r] = (float) (180 * Math.random() - 90);
			}
			cube.mPosition[0] = (float) (2 * Math.random() - 1);
			cube.mPosition[1] = (float) (2 * Math.random() - 1);
			cube.mPosition[2] = (float) Math.random()
					* (CUBE_SCROLLER_NEAR - CUBE_SCROLLER_FAR)
					- CUBE_SCROLLER_NEAR;
			cube.mPositionD[2] = (float) (4 * Math.random() + 1);
		}

		for (int idx = 0; idx < CUBE_ARCH_COUNT; ++idx) {
			Cube cube = mCubes.addCube();

			double t = Math.PI * idx / (CUBE_ARCH_COUNT - 1);

			cube.mScaling = (float) (.6f * Math.random() + .3f);
			for (int r = 0; r < 3; ++r) {
				cube.mRotation[r] = (float) (360 * Math.random());
			}
			cube.mPosition[0] = (float) (2 * Math.cos(t));
			cube.mPosition[1] = (float) (2 * Math.sin(t));
		}

	}

	public void draw(float[] viewMatrix, float[] projectionMatrix) {

		GLES20.glClearColor(0.2f, 0.5f, 0.6f, 1.0f);
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glFrontFace(GLES20.GL_CCW);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glDepthFunc(GLES20.GL_LEQUAL);

		GLES20.glUseProgram(mShader.getProgram());

		mCubes.setPositionAttrib(mShader.getHandle("aPosition"));
		mCubes.setColorAttrib(mShader.getHandle("aColor"));
		mCubes.setNormalAttrib(mShader.getHandle("aNormal"));

		for (int idx = 0; idx < mCubes.getSize(); ++idx) {
			Cube cube = mCubes.getCube(idx);

			GlslUtils.setRotateM(mModelMatrix, cube.mRotation);
			Matrix.scaleM(mModelMatrix, 0, cube.mScaling, cube.mScaling,
					cube.mScaling);
			Matrix.setIdentityM(mTempMatrix, 0);
			Matrix.translateM(mTempMatrix, 0, cube.mPosition[0],
					cube.mPosition[1], cube.mPosition[2]);
			Matrix.multiplyMM(mModelMatrix, 0, mTempMatrix, 0, mModelMatrix, 0);

			Matrix.multiplyMM(mModelViewMatrix, 0, viewMatrix, 0, mModelMatrix,
					0);

			Matrix.invertM(mTempMatrix, 0, mModelViewMatrix, 0);
			Matrix.transposeM(mNormalMatrix, 0, mTempMatrix, 0);

			Matrix.multiplyMM(mModelViewProjectionMatrix, 0, projectionMatrix,
					0, mModelViewMatrix, 0);

			GLES20.glUniformMatrix4fv(mShader.getHandle("uMVPMatrix"), 1,
					false, mModelViewProjectionMatrix, 0);

			GLES20.glUniformMatrix4fv(mShader.getHandle("uNormalMatrix"), 1,
					false, mNormalMatrix, 0);

			mCubes.drawArrays();
		}

	}

	public void init() {
		mShader.loadProgram(R.string.shader_main_vertex,
				R.string.shader_main_fragment);
		mShader.addHandle("uMVPMatrix");
		mShader.addHandle("uNormalMatrix");
		mShader.addHandle("aPosition");
		mShader.addHandle("aNormal");
		mShader.addHandle("aColor");
	}

	public void update(float timeDiff) {
		for (int i = 0; i < mCubes.getSize(); ++i) {
			Cube cube = mCubes.getCube(i);
			for (int j = 0; j < 3; ++j) {
				cube.mPosition[j] += timeDiff * cube.mPositionD[j];
				while (cube.mPosition[j] < CUBE_SCROLLER_FAR) {
					cube.mPosition[j] += CUBE_SCROLLER_NEAR - CUBE_SCROLLER_FAR;
				}
				while (cube.mPosition[j] > CUBE_SCROLLER_NEAR) {
					cube.mPosition[j] -= CUBE_SCROLLER_NEAR - CUBE_SCROLLER_FAR;
				}

				cube.mRotation[j] += timeDiff * cube.mRotationD[j];
				while (cube.mRotation[j] < 0f) {
					cube.mRotation[j] += 360f;
				}
				while (cube.mRotation[j] > 360f) {
					cube.mRotation[j] -= 360f;
				}
			}
		}
	}

}
