/*
   Copyright 2011 Harri Smått

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package fi.harism.glsl.scene;

import java.util.Vector;

import fi.harism.glsl.GlslCamera;

public final class GlslScene {

	private GlslAnimator mAnimator = new GlslAnimator();
	private Vector<GlslObject> mObjects = new Vector<GlslObject>();
	private Vector<GlslLight> mLights = new Vector<GlslLight>();

	public void animate(long time) {
		mAnimator.animate(time);
	}

	public void draw(GlslShaderIds mData) {
		for (GlslObject object : mObjects) {
			object.draw(mData);
		}
	}

	public GlslLight getLight(int idx) {
		return mLights.get(idx);
	}

	public int getLightCount() {
		return mLights.size();
	}

	public void initSceneBoxes1(int lightCount) {
		reset();

		GlslObject rootObject = new GlslObject();
		mObjects.add(rootObject);
		mAnimator.setRotation(rootObject, 3000, 5000, 6000);

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
		mAnimator.setRotation(cube, 4000, -5000, 7000);

		for (int i = 0; i < lightCount; ++i) {
			GlslLight light = new GlslLight();
			light.setPosition((float) (Math.random() * 8f) - 4f,
					(float) (Math.random() * 8f) - 4f,
					(float) (Math.random() * 8f) - 4f);
			mLights.add(light);
		}
	}

	public void initSceneBoxes2(GlslCamera camera, int lightCount) {
		reset();

		final int CUBE_SCROLLER_COUNT = 50;
		final int CUBE_ARCH_COUNT = 10;
		final float CUBE_SCROLLER_NEAR = 30f;
		final float CUBE_SCROLLER_FAR = -30f;

		GlslObject rootObject = new GlslObject();
		mObjects.add(rootObject);
		// mAnimator.setRotation(rootObject, 0, 20000, 0);

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
			GlslLight light = new GlslLight();
			light.setPosition(0f, 1f, i * 8);
			mLights.add(light);
		}

		Vector<GlslAnimator.PathElement> path = new Vector<GlslAnimator.PathElement>();
		path.add(mAnimator.new PathElement(0, 3, -20, 0));
		path.add(mAnimator.new PathElement(-20, 3, -10, 5000));
		path.add(mAnimator.new PathElement(-20, 6, 10, 10000));
		path.add(mAnimator.new PathElement(0, 8, 20, 15000));
		path.add(mAnimator.new PathElement(20, 3, 10, 20000));
		path.add(mAnimator.new PathElement(20, 0, -10, 25000));
		path.add(mAnimator.new PathElement(0, 3, -20, 30000));
		mAnimator.setPath(camera, path);
	}

	public void reset() {
		mAnimator.clear();
		mObjects.clear();
		mLights.clear();
	}

	public void setMVP(float[] viewM, float[] projM) {
		for (GlslObject object : mObjects) {
			object.setMVP(viewM, projM);
		}
		for (GlslLight light : mLights) {
			light.setMVP(viewM);
		}
	}

}
