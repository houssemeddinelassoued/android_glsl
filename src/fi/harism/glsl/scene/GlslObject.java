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

	// Scaling factor.
	private float mScaling = 1f;
	// Model matrix, only needed temporarily.
	private static final float[] mModelM = new float[16];
	// Model View matrix.
	private final float[] mModelViewM = new float[16];
	// Model View Projection matrix.
	private final float[] mModelViewProjM = new float[16];
	// Normal matrix.
	private final float[] mNormalM = new float[16];
	// Temporary matrix.
	private static final float[] mTempM = new float[16];
	// Position { x, y, z }.
	private final float[] mPosition = new float[3];
	// Rotation { x, y, z }.
	private final float[] mRotation = new float[3];
	// Child objects.
	private final Vector<GlslObject> mChildObjects = new Vector<GlslObject>();

	/**
	 * Add child object to this object.
	 * 
	 * @param obj
	 *            Child object to add
	 */
	public final void addChild(GlslObject obj) {
		mChildObjects.add(obj);
	}

	/**
	 * Getter for position.
	 * 
	 * @param position
	 *            Array to which copy position { x, y, z }
	 */
	public final void getPosition(float[] position) {
		copy(mPosition, position, 3);
	}

	/**
	 * Getter for rotation.
	 * 
	 * @param rotation
	 *            Array to which copy rotation { x, y, z }
	 */
	public final void getRotation(float[] rotation) {
		copy(mRotation, rotation, 3);
	}

	/**
	 * Render object. Classes that implement this method should call
	 * super.render(..).
	 * 
	 * @param mData
	 *            Shader handles for rendering.
	 */
	public void render(GlslShaderIds mData) {
		for (GlslObject obj : mChildObjects) {
			obj.render(mData);
		}
	}

	/**
	 * Method for rendering shadow silhouette.
	 * 
	 * @param ids
	 *            Shader attribute/uniform ids needed for rendering
	 */
	public void renderShadow(GlslShaderIds ids) {
		for (GlslObject object : mChildObjects) {
			object.renderShadow(ids);
		}
	}

	/**
	 * Update matrices based on given Model View and Projection matrices.
	 * 
	 * @param mvM
	 *            Model View matrix
	 * @param projM
	 *            Projection matrix
	 */
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

	/**
	 * Set position for this object.
	 * 
	 * @param x
	 *            X coordinate
	 * @param y
	 *            y coordinate
	 * @param z
	 *            z coordinate
	 */
	public final void setPosition(float x, float y, float z) {
		float[] position = { x, y, z };
		copy(position, mPosition, 3);
	}

	@Override
	public final void setPosition(float[] position) {
		copy(position, mPosition, 3);
	}

	/**
	 * Set rotation for this object.
	 * 
	 * @param x
	 *            Rotation around x -axis
	 * @param y
	 *            Rotation around y -axis
	 * @param z
	 *            Rotation around z -axis
	 */
	public final void setRotation(float x, float y, float z) {
		float[] rotation = { x, y, z };
		copy(rotation, mRotation, 3);
	}

	@Override
	public final void setRotation(float[] rotation) {
		copy(rotation, mRotation, 3);
	}

	/**
	 * Set scaling factor for this object.
	 * 
	 * @param scaling
	 *            Scaling factor
	 */
	public final void setScaling(float scaling) {
		mScaling = scaling;
	}

	/**
	 * Helper method for copying float arrays.
	 * 
	 * @param src
	 *            Source values
	 * @param dst
	 *            Destination values
	 * @param count
	 *            Number of items
	 */
	private final void copy(float[] src, float[] dst, int count) {
		for (int i = 0; i < count; ++i) {
			dst[i] = src[i];
		}
	}

	/**
	 * Getter for Model View matrix.
	 * 
	 * @return Model View matrix
	 */
	protected final float[] getModelViewM() {
		return mModelViewM;
	}

	/**
	 * Getter for Model View Projection matrix.
	 * 
	 * @return Model View Projection matrix
	 */
	protected final float[] getModelViewProjM() {
		return mModelViewProjM;
	}

	/**
	 * Getter for Normal matrix.
	 * 
	 * @return Normal matrix
	 */
	protected final float[] getNormalM() {
		return mNormalM;
	}
}
