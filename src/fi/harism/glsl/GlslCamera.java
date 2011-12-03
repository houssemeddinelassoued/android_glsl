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

package fi.harism.glsl;

import android.opengl.Matrix;
import fi.harism.glsl.scene.GlslAnimator;

/**
 * Holder class for view and projection matrices plus additional parameters for
 * filters. Unlike pre OpenGL ES 2.0, all matrix calculations are made by
 * ourselves.
 */
public final class GlslCamera implements GlslAnimator.PathInterface {
	// Camera values:
	// Underlying surface width and height.
	public int mViewWidth, mViewHeight;
	// View matrix.
	public float[] mViewM = new float[16];
	// Projection matrix.
	public float[] mProjM = new float[16];

	// Projection matrix values:
	private float mRatio;
	private float mFovY;
	private float mZNear;
	private float mZFar;

	// View matrix values:
	private float mViewX, mViewY, mViewZ;
	private float mLookX, mLookY, mLookZ;
	private float mUpX, mUpY, mUpZ;

	// Lens blur preferences values:
	private float mFStop;
	private float mFocalPlane;
	public int mBlurSteps;
	// Lens blur calculated values:
	public float mAperture;
	public float mPlaneInFocus;
	public float mFocalLength;

	// Touch values for displacement filter:
	public float mTouchX, mTouchY;
	public float mTouchDX, mTouchDY;

	/**
	 * Calculates circle of confusion values based on given parameters.
	 * 
	 * @param fStop
	 *            F/Stop value
	 * @param focalPlane
	 *            Value between [0, 1]
	 */
	public void setLensBlur(float fStop, float focalPlane) {
		// Store fStop and focalPlane values.
		mFStop = fStop;
		mFocalPlane = focalPlane;

		// Aperture size is 1/fStop.
		mAperture = 1f / mFStop;
		// Plane in focus is a value between [zNear, zFar].
		mPlaneInFocus = mZNear + (mFocalPlane * (mZFar - mZNear));
		// Image plane distance from lense.
		float imageDist = (float) (mPlaneInFocus / (2.0 * Math.tan(mFovY
				* Math.PI / 360.0)));
		// 1/focalLength = 1/imageDist + 1/focalPlane
		mFocalLength = (imageDist * mPlaneInFocus)
				/ (imageDist + mPlaneInFocus);
	}

	@Override
	public void setPosition(float position[]) {
		setPosition(position[0], position[1], position[2]);
	}

	/**
	 * Updates view matrix 'camera location'. Look at position and up -vector
	 * remain unaffected.
	 * 
	 * @param x
	 *            camera x position
	 * @param y
	 *            camera y position
	 * @param z
	 *            camera z position
	 */
	public void setPosition(float x, float y, float z) {
		// Regenerate view matrix with these new values.
		mViewX = x;
		mViewY = y;
		mViewZ = z;
		setViewM(mViewX, mViewY, mViewZ, mLookX, mLookY, mLookZ, mUpX, mUpY,
				mUpZ);
	}

	/**
	 * Sets projection matrix.
	 * 
	 * @param ratio
	 *            width/height ratio
	 * @param fovY
	 *            field of view in degrees
	 * @param zNear
	 *            near clipping plane
	 * @param zFar
	 *            far clipping plane
	 */
	public void setProjectionM(float ratio, float fovY, float zNear, float zFar) {
		// Store projection matrix values.
		mRatio = ratio;
		mFovY = fovY;
		mZNear = zNear;
		mZFar = zFar;

		// Matrix.frustumM(mData.mProjM, 0, -ratio, ratio, -1, 1,
		// mData.mZNear, 20);
		GlslMatrix.setPerspectiveM(mProjM, mFovY, mRatio, mZNear, mZFar);
		// Update lens blur values as projection matrix affects them.
		setLensBlur(mFStop, mFocalPlane);
	}

	/**
	 * Generates view matrix.
	 * 
	 * @param x
	 *            camera x position
	 * @param y
	 *            camera y position
	 * @param z
	 *            camera z position
	 * @param lookX
	 *            camera look at x position
	 * @param lookY
	 *            camera look at y position
	 * @param lookZ
	 *            camera look at z position
	 * @param upX
	 *            up vector x
	 * @param upY
	 *            up vector y
	 * @param upZ
	 *            up vector z
	 */
	public void setViewM(float x, float y, float z, float lookX, float lookY,
			float lookZ, float upX, float upY, float upZ) {
		mViewX = x;
		mViewY = y;
		mViewZ = z;
		mLookX = lookX;
		mLookY = lookY;
		mLookZ = lookZ;
		mUpX = upX;
		mUpY = upY;
		mUpZ = upZ;

		Matrix.setLookAtM(mViewM, 0, mViewX, mViewY, mViewZ, mLookX, mLookY,
				mLookZ, mUpX, mUpY, mUpZ);
	}
}
