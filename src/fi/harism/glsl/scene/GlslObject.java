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

import android.opengl.Matrix;
import fi.harism.glsl.GlslMatrix;

/**
 * Base class for handling object hierarchies etc.
 */
public class GlslObject implements GlslAnimator.RotationInterface,
		GlslAnimator.PathInterface {

	private static final float[] mModelM = new float[16];
	private final float[] mModelViewM = new float[16];
	private final float[] mModelViewProjM = new float[16];
	private final float[] mNormalM = new float[16];
	private static final float[] mTempM = new float[16];
	private final float[] mPosition = new float[3];
	private final float[] mRotation = new float[3];
	private float mScaling = 1f;
	private final Vector<GlslObject> mChildObjects = new Vector<GlslObject>();

	public void addChild(GlslObject obj) {
		mChildObjects.add(obj);
	}

	public final void getPosition(float[] position) {
		copy(mPosition, position, 3);
	}

	public final void getRotation(float[] rotation) {
		copy(mRotation, rotation, 3);
	}

	public void render(GlslShaderIds mData) {
		for (GlslObject obj : mChildObjects) {
			obj.render(mData);
		}
	}

	public final void setMVP(float[] mvM, float[] projM) {
		GlslMatrix.setRotateM(mModelM, mRotation);
		Matrix.scaleM(mModelM, 0, mScaling, mScaling, mScaling);
		Matrix.setIdentityM(mTempM, 0);
		Matrix.translateM(mTempM, 0, mPosition[0], mPosition[1], mPosition[2]);
		Matrix.multiplyMM(mModelM, 0, mTempM, 0, mModelM, 0);

		Matrix.multiplyMM(mModelViewM, 0, mvM, 0, mModelM, 0);
		Matrix.multiplyMM(mModelViewProjM, 0, projM, 0, mModelViewM, 0);

		Matrix.invertM(mTempM, 0, mModelViewM, 0);
		Matrix.transposeM(mNormalM, 0, mTempM, 0);

		for (GlslObject obj : mChildObjects) {
			obj.setMVP(mModelViewM, projM);
		}
	}

	public final void setPosition(float x, float y, float z) {
		float[] position = { x, y, z };
		copy(position, mPosition, 3);
	}

	@Override
	public final void setPosition(float[] position) {
		copy(position, mPosition, 3);
	}

	public final void setRotation(float x, float y, float z) {
		float[] rotation = { x, y, z };
		copy(rotation, mRotation, 3);
	}

	@Override
	public final void setRotation(float[] rotation) {
		copy(rotation, mRotation, 3);
	}

	public final void setScaling(float scaling) {
		mScaling = scaling;
	}

	private final void copy(float[] src, float[] dst, int count) {
		for (int i = 0; i < count; ++i) {
			dst[i] = src[i];
		}
	}

	protected final float[] getModelViewM() {
		return mModelViewM;
	}

	protected final float[] getModelViewProjM() {
		return mModelViewProjM;
	}

	protected final float[] getNormalM() {
		return mNormalM;
	}
}
