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
	 * Renders objects into the scene
	 * 
	 * @param mData
	 *            Shader id values to use
	 */
	public void draw(GlslShaderIds mData) {
		for (GlslObject object : mObjects) {
			object.draw(mData);
		}
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
		reset();

		GlslObject rootObject = new GlslObject();
		mObjects.add(rootObject);

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
		mAnimator.setRotation(cube, mAnimator.new RotationData(10000, -15000,
				17000));

		for (int i = 0; i < lightCount; ++i) {
			GlslLight light = new GlslLight();
			light.setPosition((float) (Math.random() * 8f) - 4f,
					(float) (Math.random() * 8f) - 4f,
					(float) (Math.random() * 8f) - 4f);
			mLights.add(light);
		}
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

		final int CUBE_SCROLLER_COUNT = 20;
		final int CUBE_ARCH_COUNT = 10;
		final float CUBE_SCROLLER_NEAR = 10f;
		final float CUBE_SCROLLER_FAR = -10f;

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

			cube.setScaling(rand(.8f, 1.2f));
			cube.setRotation(rand(0f, 360f), rand(0f, 360f), rand(0f, 360f));
			cube.setPosition(rand(-1f, 1f), rand(-1f, 1f),
					rand(CUBE_SCROLLER_NEAR, CUBE_SCROLLER_FAR));
			cube.setColor(rand(.2f, 1f), rand(.2f, 1f), rand(.2f, 1f));
			rootObject.addChild(cube);
		}

		for (int idx = 0; idx < CUBE_ARCH_COUNT; ++idx) {
			GlslCube cube = new GlslCube();

			double t = Math.PI * idx / (CUBE_ARCH_COUNT - 1);

			cube.setScaling(rand(1f, 2f));
			cube.setRotation(rand(0f, 360f), rand(0f, 360f), rand(0f, 360f));
			cube.setPosition((float) (5 * Math.cos(t)),
					(float) (5 * Math.sin(t)), 0f);
			cube.setColor(rand(.2f, 1f), rand(.2f, 1f), rand(.2f, 1f));
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
