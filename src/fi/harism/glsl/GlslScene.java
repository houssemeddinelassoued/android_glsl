package fi.harism.glsl;

import android.content.Context;
import android.opengl.GLES20;
import fi.harism.glsl.object.GlslCube;
import fi.harism.glsl.object.GlslObject;

public class GlslScene {

	private static final int CUBE_SCROLLER_COUNT = 20;
	private static final int CUBE_ARCH_COUNT = 10;
	private static final float CUBE_SCROLLER_NEAR = 20f;
	private static final float CUBE_SCROLLER_FAR = -20f;

	private GlslObject mCubes;
	private GlslShader mShader;

	public GlslScene() {
		mShader = new GlslShader();
		mCubes = new GlslObject();

		GlslCube cube = new GlslCube();
		cube.setScaling(15f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mCubes.addChildObject(cube);

		cube = new GlslCube();
		cube.setScaling(1f);
		cube.setPosition(5f, 0f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mCubes.addChildObject(cube);

		cube = new GlslCube();
		cube.setScaling(1f);
		cube.setPosition(-5f, 0f, 0f);
		cube.setRotation(90f, 0f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mCubes.addChildObject(cube);

		for (int idx = 0; idx < CUBE_SCROLLER_COUNT; ++idx) {
			cube = new GlslCube();

			cube.setScaling((float) (.4f * Math.random() + .8f));
			cube.setRotation((float) (360 * Math.random()),
					(float) (360 * Math.random()),
					(float) (360 * Math.random()));
			cube.setPosition((float) (2 * Math.random() - 1),
					(float) (2 * Math.random() - 1), (float) Math.random()
							* (CUBE_SCROLLER_NEAR - CUBE_SCROLLER_FAR)
							- CUBE_SCROLLER_NEAR);
			cube.setColor((float) Math.random(), (float) Math.random(),
					(float) Math.random());
			mCubes.addChildObject(cube);
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
			mCubes.addChildObject(cube);
		}
	}

	public void draw(float[] viewM, float[] projM) {

		GLES20.glClearColor(0.1f, 0.3f, 0.5f, 1.0f);
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

		// GLES20.glEnable(GLES20.GL_CULL_FACE);
		// GLES20.glFrontFace(GLES20.GL_CCW);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glDepthFunc(GLES20.GL_LEQUAL);

		GLES20.glUseProgram(mShader.getProgram());

		int posId = mShader.getHandle("aPosition");
		int normalId = mShader.getHandle("aNormal");
		int colorId = mShader.getHandle("aColor");
		int mvpMId = mShader.getHandle("uMVPMatrix");
		int normalMId = mShader.getHandle("uNormalMatrix");

		mCubes.draw(viewM, projM, mvpMId, normalMId, posId, normalId, colorId);
	}

	public void init(Context ctx) {
		mShader.setProgram(ctx.getString(R.string.shader_main_vertex),
				ctx.getString(R.string.shader_main_fragment));
		mShader.addHandles("uMVPMatrix", "uNormalMatrix", "aPosition",
				"aNormal", "aColor");
	}

	public void update(float timeDiff) {
		mCubes.animate(timeDiff);
	}

}
