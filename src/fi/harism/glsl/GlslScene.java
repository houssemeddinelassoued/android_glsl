package fi.harism.glsl;

import android.content.Context;
import android.opengl.GLES20;
import fi.harism.glsl.object.Cube;
import fi.harism.glsl.object.Cubes;

public class GlslScene {

	private static final int CUBE_SCROLLER_COUNT = 500;
	private static final int CUBE_ARCH_COUNT = 10;
	private static final float CUBE_SCROLLER_NEAR = 20f;
	private static final float CUBE_SCROLLER_FAR = -20f;
	private Cubes mCubes;

	private float[] mRotateMatrix = new float[16];

	private GlslShader mShader;

	public GlslScene(Context context) {
		mShader = new GlslShader(context);

		mCubes = new Cubes(CUBE_SCROLLER_COUNT + CUBE_ARCH_COUNT);
		for (int idx = 0; idx < CUBE_SCROLLER_COUNT; ++idx) {
			Cube cube = mCubes.getCube(idx);

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

		for (int idx = CUBE_SCROLLER_COUNT; idx < mCubes.getSize(); ++idx) {
			Cube cube = mCubes.getCube(idx);

			double t = Math.PI * (idx - CUBE_SCROLLER_COUNT)
					/ (CUBE_ARCH_COUNT - 1);

			cube.mScaling = (float) (.6f * Math.random() + .3f);
			for (int r = 0; r < 3; ++r) {
				cube.mRotation[r] = (float) (360 * Math.random());
			}
			cube.mPosition[0] = (float) (2 * Math.cos(t));
			cube.mPosition[1] = (float) (2 * Math.sin(t));
		}

	}

	public void draw(float[] viewMatrix, float[] projectionMatrix) {

		GLES20.glClearColor(0.4f, 0.5f, 0.6f, 1.0f);
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glFrontFace(GLES20.GL_CCW);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glDepthFunc(GLES20.GL_LEQUAL);

		GLES20.glUseProgram(mShader.getProgram());

		GLES20.glUniformMatrix4fv(mShader.getHandle("uViewMatrix"), 1, false,
				viewMatrix, 0);

		GLES20.glUniformMatrix4fv(mShader.getHandle("uProjectionMatrix"), 1,
				false, projectionMatrix, 0);

		mCubes.setPositionAttrib(mShader.getHandle("aPosition"));
		mCubes.setColorAttrib(mShader.getHandle("aColor"));
		mCubes.setNormalAttrib(mShader.getHandle("aNormal"));

		int uRotation = mShader.getHandle("uRotationMatrix");
		int uTranslate = mShader.getHandle("uTranslateVector");
		int uScale = mShader.getHandle("uScaleFloat");

		for (int idx = 0; idx < mCubes.getSize(); ++idx) {
			Cube cube = mCubes.getCube(idx);
			GlslUtils.setRotateM(mRotateMatrix, cube.mRotation);

			GLES20.glUniformMatrix4fv(uRotation, 1, false, mRotateMatrix, 0);
			GLES20.glUniform3fv(uTranslate, 1, cube.mPosition, 0);
			GLES20.glUniform1f(uScale, cube.mScaling);

			mCubes.drawArrays();
		}

	}

	public void init() {
		mShader.loadProgram(R.string.shader_main_vertex,
				R.string.shader_main_fragment);
		mShader.addHandle("uProjectionMatrix");
		mShader.addHandle("uViewMatrix");
		mShader.addHandle("uRotationMatrix");
		mShader.addHandle("uScaleFloat");
		mShader.addHandle("uTranslateVector");
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
