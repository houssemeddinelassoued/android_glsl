package fi.harism.glsl.scene;

import java.util.Vector;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.Matrix;
import fi.harism.glsl.GlslData;
import fi.harism.glsl.R;

public final class GlslScene {

	public static final int SCENE_BOXES1 = 1;
	public static final int SCENE_BOXES2 = 2;
	public static final int SCENE_BOXES3 = 3;

	private GlslAnimator mAnimator = new GlslAnimator();
	private Vector<GlslObject> mObjects = new Vector<GlslObject>();
	private Vector<Light> mLights = new Vector<Light>();

	public void animate() {
		mAnimator.animate();
	}

	public void draw(GlslData mData) {
		for (GlslObject object : mObjects) {
			object.draw(mData);
		}
	}

	public int getLightCount() {
		return mLights.size();
	}

	public void getLightPosition(int idx, float[] pos, int posIdx) {
		mLights.get(idx).getPosition(pos, posIdx);
	}

	public void setMVP(float[] viewM, float[] projM) {
		for (GlslObject object : mObjects) {
			object.setMVP(viewM, projM);
		}
		for (Light light : mLights) {
			light.setMVP(viewM);
		}
	}

	public void setPreferences(Context ctx, SharedPreferences preferences) {
		String key = ctx.getString(R.string.key_scene);
		int scene = Integer.parseInt(preferences.getString(key, "0"));
		key = ctx.getString(R.string.key_light_count);
		int lightCount = (int) preferences.getFloat(key, 0);

		mAnimator.clear();
		mObjects.clear();
		mLights.clear();

		switch (scene) {
		case 0:
			initSceneBoxes1(lightCount);
			break;
		case 1:
			initSceneBoxes2(lightCount);
			break;
		case 2:
			initSceneBoxes3(lightCount);
		}
	}

	private void initSceneBoxes1(int lightCount) {
		GlslObject rootObject = new GlslObject();
		mObjects.add(rootObject);
		mAnimator.setRotation(rootObject, 3f, 20f, 6f);

		GlslCube cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(15f, 0f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		rootObject.addChild(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(-15f, 0f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		rootObject.addChild(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(0f, 15f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		rootObject.addChild(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(0f, -15f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		rootObject.addChild(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(0f, 0f, 15f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		rootObject.addChild(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(0f, 0f, -15f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		rootObject.addChild(cube);

		cube = new GlslCube();
		cube.setScaling(2f);
		cube.setPosition(0f, 0f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		rootObject.addChild(cube);
		mAnimator.setRotation(cube, 15f, -5f, 7f);

		for (int i = 0; i < lightCount; ++i) {
			Light light = new Light();
			light.setPosition((float) (Math.random() * 8f) - 4f,
					(float) (Math.random() * 8f) - 4f,
					(float) (Math.random() * 8f) - 4f);
			mLights.add(light);
		}
	}

	private void initSceneBoxes2(int lightCount) {
		GlslObject rootObject = new GlslObject();
		mObjects.add(rootObject);
		mAnimator.setRotation(rootObject, 3f, 20f, 6f);

		GlslCube cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(15f, 0f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		rootObject.addChild(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(-15f, 0f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		rootObject.addChild(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(0f, 15f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		rootObject.addChild(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(0f, -15f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		rootObject.addChild(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(0f, 0f, 15f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		rootObject.addChild(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(0f, 0f, -15f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		rootObject.addChild(cube);

		cube = new GlslCube();
		cube.setScaling(4f);
		cube.setPosition(0f, 0f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		rootObject.addChild(cube);
		mAnimator.setRotation(cube, 8f, 12f, -10f);

		for (int i = 0; i < lightCount; ++i) {
			Light light = new Light();
			light.setPosition((float) (Math.random() * 8f) - 4f,
					(float) (Math.random() * 8f) - 4f,
					(float) (Math.random() * 8f) - 4f);
			mLights.add(light);
		}
	}

	private void initSceneBoxes3(int lightCount) {
		final int CUBE_SCROLLER_COUNT = 100;
		final int CUBE_ARCH_COUNT = 10;
		final float CUBE_SCROLLER_NEAR = 90f;
		final float CUBE_SCROLLER_FAR = -90f;

		GlslObject rootObject = new GlslObject();
		mObjects.add(rootObject);
		mAnimator.setRotation(rootObject, 0f, 20f, 0f);

		GlslCube floor = new GlslCube();
		floor.setScaling(200f);
		floor.setPosition(0, -101f, 0f);
		floor.setColor(.5f, .5f, .5f);
		rootObject.addChild(floor);

		for (int idx = 0; idx < CUBE_SCROLLER_COUNT; ++idx) {
			GlslCube cube = new GlslCube();

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
			rootObject.addChild(cube);
		}

		for (int idx = 0; idx < CUBE_ARCH_COUNT; ++idx) {
			GlslCube cube = new GlslCube();

			double t = Math.PI * idx / (CUBE_ARCH_COUNT - 1);

			cube.setScaling((float) (1f * Math.random() + 1f));
			cube.setRotation((float) (360 * Math.random()),
					(float) (360 * Math.random()),
					(float) (360 * Math.random()));
			cube.setPosition((float) (5 * Math.cos(t)),
					(float) (5 * Math.sin(t)), 0f);
			cube.setColor((float) Math.random(), (float) Math.random(),
					(float) Math.random());
			rootObject.addChild(cube);
		}

		for (int i = 0; i < lightCount; ++i) {
			Light light = new Light();
			light.setPosition(0f, 1f, i * 8);
			mLights.add(light);
		}
	}

	private final class Light {
		private float[] mPosition = new float[4];
		private float[] mProjPos = new float[4];
		private float[] mTempM = new float[16];

		public void getPosition(float[] pos, int posIdx) {
			pos[posIdx] = mProjPos[0];
			pos[posIdx + 1] = mProjPos[1];
			pos[posIdx + 2] = mProjPos[2];
		}

		public void setMVP(float[] viewM) {
			Matrix.setIdentityM(mTempM, 0);
			Matrix.translateM(mTempM, 0, mPosition[0], mPosition[1],
					mPosition[2]);
			Matrix.multiplyMM(mTempM, 0, viewM, 0, mTempM, 0);
			Matrix.multiplyMV(mProjPos, 0, mTempM, 0, mPosition, 0);
		}

		public void setPosition(float x, float y, float z) {
			mPosition[0] = x;
			mPosition[1] = y;
			mPosition[2] = z;
			mPosition[3] = 1;
		}
	}

}
