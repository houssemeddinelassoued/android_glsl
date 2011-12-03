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

/**
 * Static methods for matrix initialization.
 */
public class GlslMatrix {

	/**
	 * Initializes given matrix as perspective projection matrix.
	 * 
	 * @param m
	 *            Matrix for writing, should be float[16], or bigger.
	 * @param fovy
	 *            Field of view in degrees.
	 * @param aspect
	 *            Aspect ratio.
	 * @param zNear
	 *            Near clipping plane.
	 * @param zFar
	 *            Far clipping plane.
	 */
	public static final void setPerspectiveM(float[] m, float fovy,
			float aspect, float zNear, float zFar) {

		// First initialize matrix as identity matrix.
		Matrix.setIdentityM(m, 0);

		// Half the height.
		float h = zNear * (float) Math.tan(fovy * Math.PI / 360);
		// Half the width.
		float w = h * aspect;
		// Distance.
		float d = zFar - zNear;

		m[0] = zNear / w;
		m[5] = zNear / h;
		m[10] = -(zFar + zNear) / d;
		m[11] = -1;
		m[14] = (-2 * zNear * zFar) / d;
		m[15] = 0;
	}

	/**
	 * Sets given matrix to x-y-z -rotation matrix.
	 * 
	 * @param m
	 *            Matrix to contain rotation matrix. Should be float[16] or
	 *            bigger.
	 * @param r
	 *            Rotation values, float[3] where { x, y, z } is rotation around
	 *            corresponding axis.
	 */
	public static final void setRotateM(float[] m, float[] r) {

		double toRadians = Math.PI * 2 / 360;
		float sin0 = (float) Math.sin(r[0] * toRadians);
		float cos0 = (float) Math.cos(r[0] * toRadians);
		float sin1 = (float) Math.sin(r[1] * toRadians);
		float cos1 = (float) Math.cos(r[1] * toRadians);
		float sin2 = (float) Math.sin(r[2] * toRadians);
		float cos2 = (float) Math.cos(r[2] * toRadians);

		Matrix.setIdentityM(m, 0);

		m[0] = cos1 * cos2;
		m[1] = cos1 * sin2;
		m[2] = -sin1;

		m[4] = (-cos0 * sin2) + (sin0 * sin1 * cos2);
		m[5] = (cos0 * cos2) + (sin0 * sin1 * sin2);
		m[6] = sin0 * cos1;

		m[8] = (sin0 * sin2) + (cos0 * sin1 * cos2);
		m[9] = (-sin0 * cos2) + (cos0 * sin1 * sin2);
		m[10] = cos0 * cos1;

	}

}
