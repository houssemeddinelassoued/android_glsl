package fi.harism.glsl.scene;

import java.util.Vector;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.Matrix;
import fi.harism.glsl.GlslUtils;
import fi.harism.glsl.R;

public final class GlslScene {

	public static final int SCENE_BOXES1 = 1;
	public static final int SCENE_BOXES2 = 2;
	public static final int SCENE_BOXES3 = 3;

	private Vector<GlslObject> mObjects = new Vector<GlslObject>();;

	private Vector<Light> mLights = new Vector<Light>();

	public void animate(float timeDiff) {
		for (GlslObject object : mObjects) {
			object.animate(timeDiff);
		}
		for (Light light : mLights) {
			light.animate(timeDiff);
		}
	}

	public void draw(int mMId, int mvpMId, int normalMId, int posId,
			int normalId, int colorId) {
		for (GlslObject object : mObjects) {
			object.draw(mMId, mvpMId, normalMId, posId, normalId, colorId);
		}
	}

	public int getLightCount() {
		return mLights.size();
	}

	public void getLightPosition(int idx, float[] pos) {
		mLights.get(idx).getPosition(pos);
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
		GlslCube cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(15f, 0f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObjects.add(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(-15f, 0f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObjects.add(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(0f, 15f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObjects.add(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(0f, -15f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObjects.add(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(0f, 0f, 15f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObjects.add(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(0f, 0f, -15f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObjects.add(cube);

		cube = new GlslCube();
		cube.setScaling(2f);
		cube.setPosition(0f, 0f, 0f);
		cube.setRotationD(15f, 20f, 11f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObjects.add(cube);

		for (int i = 0; i < lightCount; ++i) {
			Light light = new Light();
			light.setPosition(8f, 0f, 0f);
			light.setRotationD(0f, (float) (10 * Math.random()) + 5f, 0f);
			mLights.add(light);
		}
	}

	private void initSceneBoxes2(int lightCount) {
		GlslCube cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(15f, 0f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObjects.add(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(-15f, 0f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObjects.add(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(0f, 15f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObjects.add(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(0f, -15f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObjects.add(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(0f, 0f, 15f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObjects.add(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(0f, 0f, -15f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObjects.add(cube);

		cube = new GlslCube();
		cube.setScaling(4f);
		cube.setPosition(0f, 0f, 0f);
		cube.setRotationD(15f, 20f, 11f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObjects.add(cube);

		for (int i = 0; i < lightCount; ++i) {
			Light light = new Light();
			light.setPosition(8f, 0f, 0f);
			light.setRotationD(0f, (float) (10 * Math.random()) + 5f, 0f);
			mLights.add(light);
		}
	}

	private void initSceneBoxes3(int lightCount) {
		final int CUBE_SCROLLER_COUNT = 20;
		final int CUBE_ARCH_COUNT = 10;
		final float CUBE_SCROLLER_NEAR = 20f;
		final float CUBE_SCROLLER_FAR = -20f;

		GlslCube cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(15f, 0f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObjects.add(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(-15f, 0f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObjects.add(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(0f, 15f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObjects.add(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(0f, -15f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObjects.add(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(0f, 0f, 15f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObjects.add(cube);

		cube = new GlslCube();
		cube.setScaling(15f);
		cube.setPosition(0f, 0f, -15f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObjects.add(cube);

		cube = new GlslCube();
		cube.setScaling(1f);
		cube.setPosition(5f, 0f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObjects.add(cube);

		cube = new GlslCube();
		cube.setScaling(1f);
		cube.setPosition(-5f, 0f, 0f);
		cube.setRotation(90f, 0f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObjects.add(cube);

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
			mObjects.add(cube);
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
			mObjects.add(cube);
		}

		for (int i = 0; i < lightCount; ++i) {
			Light light = new Light();
			light.setPosition(8f, 0f, 0f);
			light.setRotationD(0f, (float) (10 * Math.random()) + 5f, 0f);
			mLights.add(light);
		}
	}

	private final class Light {
		private float[] mPosition = new float[4];
		private float[] mRotation = new float[3];
		private float[] mRotationD = new float[3];

		private float[] mProjPos = new float[4];

		private float[] mTempM = new float[16];

		public void animate(float timeDiff) {
			for (int i = 0; i < 3; ++i) {
				mRotation[i] += mRotationD[i] * timeDiff;
				while (mRotation[i] < 0f)
					mRotation[i] += 360f;
				while (mRotation[i] > 360f)
					mRotation[i] -= 360f;
			}
		}

		public void getPosition(float[] pos) {
			pos[0] = mProjPos[0];
			pos[1] = mProjPos[1];
			pos[2] = mProjPos[2];
		}

		public void setMVP(float[] viewM) {
			GlslUtils.setRotateM(mTempM, mRotation);
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

		public void setRotation(float x, float y, float z) {
			mRotation[0] = x;
			mRotation[1] = y;
			mRotation[2] = z;
		}

		public void setRotationD(float x, float y, float z) {
			mRotationD[0] = x;
			mRotationD[1] = y;
			mRotationD[2] = z;
		}
	}

}
