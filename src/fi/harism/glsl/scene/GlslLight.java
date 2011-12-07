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

import android.opengl.Matrix;

/**
 * Light data holder.
 */
public class GlslLight implements GlslAnimator.PathInterface {
	// Model position.
	private float[] mPosition = new float[4];
	// Model-View position.
	private float[] mViewPos = new float[4];

	/**
	 * Getter for View matrix multiplied position.
	 * 
	 * @return float[] array containing { x, y, z, w }
	 */
	public float[] getPosition() {
		return mViewPos;
	}

	@Override
	public void setPosition(float position[]) {
		mPosition[0] = position[0];
		mPosition[1] = position[1];
		mPosition[2] = position[2];
		mPosition[3] = 1;
	}

	public void setPosition(float x, float y, float z) {
		mPosition[0] = x;
		mPosition[1] = y;
		mPosition[2] = z;
		mPosition[3] = 1;
	}

	/**
	 * Calculates view position for light.
	 * 
	 * @param viewM
	 *            View matrix.
	 */
	public void updateMatrices(float[] viewM) {
		Matrix.multiplyMV(mViewPos, 0, viewM, 0, mPosition, 0);
		for (int i = 0; i < 3; ++i) {
			mViewPos[i] /= mViewPos[3];
		}
	}
}
