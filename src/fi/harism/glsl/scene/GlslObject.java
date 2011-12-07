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

	// Local model matrix.
	private final float[] mModelM = new float[16];
	// World model-view matrix.
	private final float[] mModelViewM = new float[16];
	// World model-view-projection matrix.
	private final float[] mModelViewProjM = new float[16];
	// World normal matrix.
	private final float[] mNormalM = new float[16];
	// Temporary matrix needed for normal matrix calculation.
	private static final float[] mTempM = new float[16];

	// If true, mModelM will be recalculated on next call to updateMatrices().
	private boolean mRecalculateModelM;
	// Local scaling matrix.
	private final float[] mScaleM = new float[16];
	// Local rotation matrix.
	private final float[] mRotateM = new float[16];
	// Local translation matrix.
	private final float[] mTranslateM = new float[16];

	// Child objects.
	private final Vector<GlslObject> mChildObjects = new Vector<GlslObject>();

	/**
	 * Default constructor.
	 */
	public GlslObject() {
		// Simply set all matrices to identity.
		Matrix.setIdentityM(mModelM, 0);
		Matrix.setIdentityM(mScaleM, 0);
		Matrix.setIdentityM(mRotateM, 0);
		Matrix.setIdentityM(mTranslateM, 0);
	}

	/**
	 * Add child object to this object.
	 * 
	 * @param obj
	 *            Child object to add
	 */
	public void addChild(GlslObject obj) {
		mChildObjects.add(obj);
	}

	/**
	 * Render object. Classes that overwrite this method should call
	 * super.render(..).
	 * 
	 * @param ids
	 *            Shader attribute/uniform handles for rendering.
	 */
	public void render(GlslShaderIds ids) {
		for (GlslObject obj : mChildObjects) {
			obj.render(ids);
		}
	}

	/**
	 * Method for rendering shadow silhouette. Classes that override this method
	 * should call super.renderShadow(..).
	 * 
	 * @param ids
	 *            Shader attribute/uniform handles for rendering
	 */
	public void renderShadow(GlslShaderIds ids) {
		for (GlslObject object : mChildObjects) {
			object.renderShadow(ids);
		}
	}

	/**
	 * Set position for this object. Object position is relative to its parent
	 * object, and camera view if this is the root object.
	 * 
	 * @param x
	 *            Object x coordinate
	 * @param y
	 *            Object y coordinate
	 * @param z
	 *            Object z coordinate
	 */
	public final void setPosition(float x, float y, float z) {
		Matrix.setIdentityM(mTranslateM, 0);
		Matrix.translateM(mTranslateM, 0, x, y, z);
		mRecalculateModelM = true;
	}

	@Override
	public final void setPosition(float[] position) {
		setPosition(position[0], position[1], position[2]);
	}

	/**
	 * Sets rotation for this object. Rotation is relative to object's parent
	 * object.
	 * 
	 * @param x
	 *            Rotation around x axis
	 * @param y
	 *            Rotation around y axis
	 * @param z
	 *            Rotation around z axis
	 */
	public final void setRotation(float x, float y, float z) {
		GlslMatrix.setRotateM(mRotateM, 0, x, y, z);
		mRecalculateModelM = true;
	}

	@Override
	public final void setRotation(float[] rotation) {
		setRotation(rotation[0], rotation[1], rotation[2]);
	}

	/**
	 * Set scaling factor for this object.
	 * 
	 * @param scale
	 *            Scaling factor
	 */
	public final void setScaling(float scale) {
		Matrix.setIdentityM(mScaleM, 0);
		Matrix.scaleM(mScaleM, 0, scale, scale, scale);
		mRecalculateModelM = true;
	}

	/**
	 * Updates matrices based on given Model View and Projection matrices. This
	 * method should be called before any rendering takes place, and most likely
	 * after scene has been animated.
	 * 
	 * @param mvM
	 *            Model View matrix
	 * @param projM
	 *            Projection matrix
	 */
	public void updateMatrices(float[] mvM, float[] projM) {
		if (mRecalculateModelM) {
			Matrix.multiplyMM(mModelM, 0, mScaleM, 0, mRotateM, 0);
			Matrix.multiplyMM(mModelM, 0, mTranslateM, 0, mModelM, 0);
			mRecalculateModelM = false;
		}

		// Add local model matrix to global model-view matrix.
		Matrix.multiplyMM(mModelViewM, 0, mvM, 0, mModelM, 0);
		// Apply projection matrix to global model-view matrix.
		Matrix.multiplyMM(mModelViewProjM, 0, projM, 0, mModelViewM, 0);
		// Fast inverse-transpose calculation.
		GlslMatrix.invTransposeM(mNormalM, 0, mModelViewM, 0);
		
		for (GlslObject obj : mChildObjects) {
			obj.updateMatrices(mModelViewM, projM);
		}
	}

	/**
	 * Getter for model-view matrix. This matrix is calculated on call to
	 * updateMatrices(..) which should be called before actual rendering takes
	 * place.
	 * 
	 * @return Current model-view matrix
	 */
	protected float[] getModelViewM() {
		return mModelViewM;
	}

	/**
	 * Getter for model-view-projection matrix. This matrix is calculated on
	 * call to updateMatrices(..) which should be called before actual rendering
	 * takes place.
	 * 
	 * @return Current model-view-projection matrix
	 */
	protected float[] getModelViewProjM() {
		return mModelViewProjM;
	}

	/**
	 * Getter for normal matrix. This matrix is calculated on call to
	 * updateMatrices(..) which should be called before actual rendering takes
	 * place.
	 * 
	 * @return Current normal matrix
	 */
	protected float[] getNormalM() {
		return mNormalM;
	}
}
