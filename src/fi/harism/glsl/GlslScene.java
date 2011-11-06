package fi.harism.glsl;

import java.util.Vector;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import fi.harism.glsl.object.GlslCube;

public class GlslScene {

	private static final int CUBE_SCROLLER_COUNT = 20;
	private static final int CUBE_ARCH_COUNT = 10;
	private static final float CUBE_SCROLLER_NEAR = 20f;
	private static final float CUBE_SCROLLER_FAR = -20f;

	private float[] mTempMatrix = new float[16];
	private float[] mModelMatrix = new float[16];
	private float[] mModelViewMatrix = new float[16];
	private float[] mModelViewProjectionMatrix = new float[16];
	private float[] mNormalMatrix = new float[16];

	private Vector<GlslCube> mCubes;

	private GlslShader mShader;

	public GlslScene() {
		mShader = new GlslShader();
		mCubes = new Vector<GlslCube>();

		GlslCube cube = new GlslCube();
		cube.setScaling(15f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mCubes.add(cube);

		cube = new GlslCube();
		cube.setScaling(1f);
		cube.setPosition(5f, 0f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mCubes.add(cube);

		cube = new GlslCube();
		cube.setScaling(1f);
		cube.setPosition(-5f, 0f, 0f);
		cube.setRotation(90f, 0f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mCubes.add(cube);

		for (int idx = 0; idx < CUBE_SCROLLER_COUNT; ++idx) {
			cube = new GlslCube();

			cube.setScaling((float) (.4f * Math.random() + .8f));
			cube.setRotation((float) (360 * Math.random()),
					(float) (360 * Math.random()),
					(float) (360 * Math.random()));
			cube.setRotationD((float) (180 * Math.random() - 90),
					(float) (180 * Math.random() - 90),
					(float) (180 * Math.random() - 90));
			cube.setPosition((float) (2 * Math.random() - 1),
					(float) (2 * Math.random() - 1), (float) Math.random()
							* (CUBE_SCROLLER_NEAR - CUBE_SCROLLER_FAR)
							- CUBE_SCROLLER_NEAR);
			cube.setPositionD(0f, 0f, (float) (4 * Math.random() + 1));
			cube.setColor((float) Math.random(), (float) Math.random(),
					(float) Math.random());
			mCubes.add(cube);
		}

		for (int idx = 0; idx < CUBE_ARCH_COUNT; ++idx) {
			cube = new GlslCube();

			double t = Math.PI * idx / (CUBE_ARCH_COUNT - 1);

			cube.setScaling((float) (.6f * Math.random() + .6f));
			cube.setRotation((float) (360 * Math.random()),
					(float) (360 * Math.random()),
					(float) (360 * Math.random()));
			cube.setPosition((float) (3 * Math.cos(t)),
					(float) (3 * Math.sin(t)), 0f);
			cube.setColor((float) Math.random(), (float) Math.random(),
					(float) Math.random());
			mCubes.add(cube);
		}
	}

	public void draw(float[] viewMatrix, float[] projectionMatrix) {

		GLES20.glClearColor(0.1f, 0.3f, 0.5f, 1.0f);
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

		// GLES20.glEnable(GLES20.GL_CULL_FACE);
		// GLES20.glFrontFace(GLES20.GL_CCW);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glDepthFunc(GLES20.GL_LEQUAL);

		GLES20.glUseProgram(mShader.getProgram());

		for (GlslCube cube : mCubes) {
			cube.setModelM(mModelMatrix);
			cube.setPositionAttrib(mShader.getHandle("aPosition"));
			cube.setColorAttrib(mShader.getHandle("aColor"));
			cube.setNormalAttrib(mShader.getHandle("aNormal"));

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

			cube.drawObject();
		}

	}

	public void init(Context ctx) {
		mShader.setProgram(ctx.getString(R.string.shader_main_vertex),
				ctx.getString(R.string.shader_main_fragment));
		mShader.addHandles("uMVPMatrix", "uNormalMatrix", "aPosition",
				"aNormal", "aColor");
	}

	public void update(float timeDiff) {
		for (GlslCube cube : mCubes) {
			cube.animate(timeDiff);
		}
	}

}
