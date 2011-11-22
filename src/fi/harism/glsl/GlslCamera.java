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

public class GlslCamera {
	// Camera values.
	public int mViewWidth, mViewHeight;
	public float[] mViewM = new float[16];
	public float[] mProjM = new float[16];

	// Projection matrix
	private float mRatio;
	private float mFovY;
	private float mZNear;
	private float mZFar;

	// View matrix
	private float mViewX, mViewY, mViewZ;
	private float mLookX, mLookY, mLookZ;
	private float mUpX, mUpY, mUpZ;

	// Bloom values.
	public float mBloomThreshold;

	// Lens blur values.
	private float mFStop;
	private float mFocalPlane;
	public int mBlurSteps;
	public float mCocRadius;
	public float mCocScale;
	public float mCocBias;

	public void setLensBlur(float fStop, float focalPlane) {
		mFStop = fStop;
		mFocalPlane = focalPlane;

		float fLen = 0.16f / (float) (2 * Math.tan((mFovY * Math.PI) / 360.0));
		float fPlane = focalPlane * mZFar;
		float A = 400f / fStop;

		mCocScale = (A * fLen * fPlane * (mZFar - mZNear))
				/ ((fPlane - fLen) * mZNear * mZFar);
		mCocBias = (A * fLen * (mZNear - fPlane)) / ((fPlane + fLen) * mZNear);

		mCocRadius = Math.max(Math.abs(mCocScale + mCocBias),
				Math.abs(mCocBias));
		mCocRadius = Math.min(mCocRadius, 20f);
	}

	public void setPosition(float x, float y, float z) {
		mViewX = x;
		mViewY = y;
		mViewZ = z;
		setViewM(x, y, z, mLookX, mLookY, mLookZ, mUpX, mUpY, mUpZ);
	}

	public void setProjectionM(float ratio, float fovY, float zNear, float zFar) {
		mRatio = ratio;
		mFovY = fovY;
		mZNear = zNear;
		mZFar = zFar;

		// Matrix.frustumM(mData.mProjM, 0, -ratio, ratio, -1, 1,
		// mData.mZNear, 20);
		GlslMatrix.setPerspectiveM(mProjM, mFovY, mRatio, mZNear, mZFar);

		setLensBlur(mFStop, mFocalPlane);
	}

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

		Matrix.setLookAtM(mViewM, 0, x, y, z, lookX, lookY, lookZ, upX, upY,
				upZ);
	}
}
