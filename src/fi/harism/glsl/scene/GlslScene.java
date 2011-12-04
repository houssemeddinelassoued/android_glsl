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

/**
 * Class for encapsulating scene related objects, lights etc.
 */
public final class GlslScene {

	private GlslAnimator mAnimator = new GlslAnimator();
	private Vector<GlslObject> mObjects = new Vector<GlslObject>();
	private Vector<GlslLight> mLights = new Vector<GlslLight>();

	/**
	 * Updates objects based on their animation values.
	 * 
	 * @param time
	 *            current time in millis
	 */
	public void animate(long time) {
		mAnimator.animate(time);
	}

	/**
	 * Returns light with given index. Index is a value between [0,
	 * getLightCount() - 1].
	 * 
	 * @param idx
	 *            Light index
	 * @return Light with given index
	 */
	public GlslLight getLight(int idx) {
		return mLights.get(idx);
	}

	/**
	 * Returns current light count
	 * 
	 * @return number of lights
	 */
	public int getLightCount() {
		return mLights.size();
	}

	/**
	 * Helper method for creating example scene.
	 * 
	 * @param camera
	 *            Camera for adding animation to it.
	 * @param lightCount
	 *            Number of lights to add to the scene
	 */
	public void initSceneBoxes1(GlslCamera camera, int lightCount) {
		// Just in case.
		reset();

		GlslObject rootObject = new GlslObject();
		mObjects.add(rootObject);

		// Using negative size creates inverted box one can put camera inside.
		// Here we create bounding box for scene.
		GlslBox box = new GlslBox();
		box.setSize(-6, -6, -20);
		for (int i = 0; i < 6; ++i) {
			box.setColor(i, rand(.2f, 1f), rand(.2f, 1f), rand(.2f, 1f));
		}
		mObjects.add(box);

		// Put one box in the middle of scene.
		box = new GlslBox();
		box.setPosition(0f, 0f, 0f);
		box.setColor(rand(.2f, 1f), rand(.2f, 1f), rand(.2f, 1f));
		rootObject.addChild(box);
		mAnimator.setRotation(box, mAnimator.new RotationData(10000, -15000,
				17000));

		// Generate lights and assign random path to them.
		for (int i = 0; i < lightCount; ++i) {
			GlslLight light = new GlslLight();
			GlslAnimator.Path path = mAnimator.new Path();
			float x = rand(-2.8f, 2.8f);
			float y = rand(-2.8f, 2.8f);
			float z = rand(-9f, 9f);
			path.addPosition(x, y, z, 0);
			for (int j = 1; j < 10; ++j) {
				path.addPosition(rand(-2.8f, 2.8f), rand(-2.8f, 2.8f),
						rand(-9f, 9f), j * 4000);
			}
			path.addPosition(x, y, z, 40000);
			mAnimator.setPath(light, path);
			mLights.add(light);
		}

		// Generate random path for camera.
		GlslAnimator.Path path = mAnimator.new Path();
		float x = rand(-2.5f, 2.5f);
		float y = rand(-2.5f, 2.5f);
		float z = rand(-9f, 9f);
		path.addPosition(x, y, z, 0);
		for (int j = 1; j < 10; ++j) {
			path.addPosition(rand(-2.5f, 2.5f), rand(-2.5f, 2.5f),
					rand(-9f, 9f), j * 4000);
		}
		path.addPosition(x, y, z, 40000);
		mAnimator.setPath(camera, path);
	}

	/**
	 * Helper method for creating example scene.
	 * 
	 * @param camera
	 *            Camera for adding animation to it.
	 * @param lightCount
	 *            Number of lights to add to the scene
	 */
	public void initSceneBoxes2(GlslCamera camera, int lightCount) {
		reset();

		final int CUBE_ARCH_COUNT = 20;
		final int CUBE_SCROLLER_COUNT = 40;
		final float CUBE_SCROLLER_NEAR = 20f;
		final float CUBE_SCROLLER_FAR = -20f;

		GlslObject rootObject = new GlslObject();
		mObjects.add(rootObject);

		// One big flat box as ground object.
		GlslBox floor = new GlslBox();
		floor.setSize(100f, 2f, 100f);
		floor.setPosition(0, -1f, 0f);
		floor.setColor(.5f, .5f, .5f);
		rootObject.addChild(floor);

		// Generate 'scroller'.
		for (int idx = 0; idx < CUBE_SCROLLER_COUNT; ++idx) {
			GlslBox cube = new GlslBox();
			cube.setScaling(rand(.5f, 1.0f));
			cube.setRotation(rand(0f, 360f), rand(0f, 360f), rand(0f, 360f));
			cube.setPosition(rand(-1f, 1f), rand(0f, 1f), CUBE_SCROLLER_NEAR
					+ ((float) idx / CUBE_SCROLLER_COUNT)
					* (CUBE_SCROLLER_FAR - CUBE_SCROLLER_NEAR));
			cube.setColor(rand(.2f, 1f), rand(.2f, 1f), rand(.2f, 1f));
			rootObject.addChild(cube);
		}

		// Generate 'arch' or rotating circle of boxes.
		GlslObject archContainer = new GlslObject();
		rootObject.addChild(archContainer);
		mAnimator.setRotation(archContainer, mAnimator.new RotationData(0, 0,
				15000));
		for (int idx = 0; idx < CUBE_ARCH_COUNT; ++idx) {
			double t = 2 * Math.PI * idx / CUBE_ARCH_COUNT;
			GlslBox cube = new GlslBox();
			cube.setScaling(rand(0.5f, 1f));
			cube.setRotation(rand(0f, 360f), rand(0f, 360f), rand(0f, 360f));
			cube.setPosition((float) (3f * Math.cos(t)),
					(float) (3f * Math.sin(t)), 0f);
			cube.setColor(rand(.2f, 1f), rand(.2f, 1f), rand(.2f, 1f));
			archContainer.addChild(cube);
		}

		// Generate lights and assign random path to them.
		for (int i = 0; i < lightCount; ++i) {
			GlslLight light = new GlslLight();
			GlslAnimator.Path path = mAnimator.new Path();
			float x = rand(-5f, 5f);
			float y = rand(1f, 10f);
			float z = rand(-5f, 5f);
			path.addPosition(x, y, z, 0);
			for (int j = 1; j < 10; ++j) {
				path.addPosition(rand(-5f, 5f), rand(1f, 10f), rand(-5f, 5f),
						j * 4000);
			}
			path.addPosition(x, y, z, 40000);
			mAnimator.setPath(light, path);
			mLights.add(light);
		}

		// Assign random path to camera.
		GlslAnimator.Path path = mAnimator.new Path();
		float x = rand(-10f, 10f);
		float y = rand(1f, 10f);
		float z = rand(-10f, 10f);
		path.addPosition(x, y, z, 0);
		for (int j = 1; j < 10; ++j) {
			path.addPosition(rand(-10f, 10f), rand(1f, 10f), rand(-10f, 10f),
					j * 4000);
		}
		path.addPosition(x, y, z, 40000);
		mAnimator.setPath(camera, path);
	}

	/**
	 * Renders objects into the scene
	 * 
	 * @param mData
	 *            Shader id values to use
	 */
	public void render(GlslShaderIds mData) {
		for (GlslObject object : mObjects) {
			object.render(mData);
		}
	}

	/**
	 * Clears all objects from this scene.
	 */
	public void reset() {
		mAnimator.clear();
		mObjects.clear();
		mLights.clear();
	}

	/**
	 * Updates object hierarchy matrices with given view and projection
	 * matrices.
	 * 
	 * @param viewM
	 *            View matrix
	 * @param projM
	 *            Projection matrix
	 */
	public void setMVP(float[] viewM, float[] projM) {
		for (GlslObject object : mObjects) {
			object.setMVP(viewM, projM);
		}
		for (GlslLight light : mLights) {
			light.setMVP(viewM);
		}
	}

	/**
	 * Private helper method for calculating random values.
	 * 
	 * @param min
	 *            Minimum value
	 * @param max
	 *            Maximum value
	 * @return Value between [min, max)
	 */
	private float rand(float min, float max) {
		return (float) (min + Math.random() * (max - min));
	}

}
