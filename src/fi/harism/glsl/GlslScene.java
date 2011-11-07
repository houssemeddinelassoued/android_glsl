package fi.harism.glsl;

import java.util.Vector;

import android.content.Context;
import fi.harism.glsl.object.GlslCube;
import fi.harism.glsl.object.GlslLight;
import fi.harism.glsl.object.GlslObject;

public final class GlslScene {

	private GlslObject mObject;
	private Vector<GlslLight> mLights = new Vector<GlslLight>();

	public void draw(int mvpMId, int normalMId, int posId, int normalId,
			int colorId) {
		if (mObject != null) {
			mObject.draw(mvpMId, normalMId, posId, normalId, colorId);
		}
	}

	public Vector<GlslLight> getLights() {
		return mLights;
	}

	public void init(Context ctx) {
		initCubeScene();
	}

	public void setMVP(float[] viewM, float[] projM) {
		if (mObject != null) {
			mObject.setMVP(viewM, projM);
		}
	}

	public void update(float timeDiff) {
		mObject.animate(timeDiff);
	}

	private void initCubeScene() {
		final int CUBE_SCROLLER_COUNT = 20;
		final int CUBE_ARCH_COUNT = 10;
		final float CUBE_SCROLLER_NEAR = 20f;
		final float CUBE_SCROLLER_FAR = -20f;

		mObject = new GlslObject();

		GlslCube cube = new GlslCube();
		cube.setScaling(15f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObject.addChildObject(cube);

		cube = new GlslCube();
		cube.setScaling(1f);
		cube.setPosition(5f, 0f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObject.addChildObject(cube);

		cube = new GlslCube();
		cube.setScaling(1f);
		cube.setPosition(-5f, 0f, 0f);
		cube.setRotation(90f, 0f, 0f);
		cube.setColor((float) Math.random(), (float) Math.random(),
				(float) Math.random());
		mObject.addChildObject(cube);

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
			mObject.addChildObject(cube);
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
			mObject.addChildObject(cube);
		}

		GlslLight light = new GlslLight();
		light.setPosition(0, 0, -5);
		light.setDirection(0, 0, 5);
		mLights.clear();
		mLights.add(light);
	}

}
