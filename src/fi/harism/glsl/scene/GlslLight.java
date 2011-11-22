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

public class GlslLight {
	private float[] mPosition = new float[4];
	private float[] mProjPos = new float[4];
	private float[] mTempM = new float[16];

	public void getPosition(float[] pos, int posIdx) {
		pos[posIdx] = mProjPos[0];
		pos[posIdx + 1] = mProjPos[1];
		pos[posIdx + 2] = mProjPos[2];
	}

	public void setMVP(float[] viewM) {
		Matrix.setIdentityM(mTempM, 0);
		Matrix.translateM(mTempM, 0, mPosition[0], mPosition[1], mPosition[2]);
		Matrix.multiplyMM(mTempM, 0, viewM, 0, mTempM, 0);
		Matrix.multiplyMV(mProjPos, 0, mTempM, 0, mPosition, 0);
	}

	public void setPosition(float x, float y, float z) {
		mPosition[0] = x;
		mPosition[1] = y;
		mPosition[2] = z;
		mPosition[3] = 1;
	}
}
