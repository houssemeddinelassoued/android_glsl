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
public final class GlslMatrix {

	/**
	 * Fast inverse-transpose matrix calculation. See
	 * http://content.gpwiki.org/index.php/MathGem:Fast_Matrix_Inversion for
	 * more information. Only difference is that we do transpose at the same
	 * time and therefore we don't transpose upper-left 3x3 matrix leaving it
	 * intact. Also T is written into lowest row of destination matrix instead
	 * of last column.
	 * 
	 * @param dst
	 *            Destination matrix
	 * @param dstOffset
	 *            Destination matrix offset
	 * @param src
	 *            Source matrix
	 * @param srcOffset
	 *            Source matrix offset
	 */
	public static void invTransposeM(float[] dst, int dstOffset, float[] src,
			int srcOffset) {
		Matrix.setIdentityM(dst, dstOffset);

		// Copy top-left 3x3 matrix into dst matrix.
		dst[dstOffset + 0] = src[srcOffset + 0];
		dst[dstOffset + 1] = src[srcOffset + 1];
		dst[dstOffset + 2] = src[srcOffset + 2];
		dst[dstOffset + 4] = src[srcOffset + 4];
		dst[dstOffset + 5] = src[srcOffset + 5];
		dst[dstOffset + 6] = src[srcOffset + 6];
		dst[dstOffset + 8] = src[srcOffset + 8];
		dst[dstOffset + 9] = src[srcOffset + 9];
		dst[dstOffset + 10] = src[srcOffset + 10];

		// Calculate -(Ri dot T) into last row.
		dst[dstOffset + 3] = -(src[srcOffset + 0] * src[srcOffset + 12]
				+ src[srcOffset + 1] * src[srcOffset + 13] + src[srcOffset + 2]
				* src[srcOffset + 14]);
		dst[dstOffset + 7] = -(src[srcOffset + 4] * src[srcOffset + 12]
				+ src[srcOffset + 5] * src[srcOffset + 13] + src[srcOffset + 6]
				* src[srcOffset + 14]);
		dst[dstOffset + 11] = -(src[srcOffset + 8] * src[srcOffset + 12]
				+ src[srcOffset + 9] * src[srcOffset + 13] + src[srcOffset + 10]
				* src[srcOffset + 14]);
	}

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
	public static void setPerspectiveM(float[] m, float fovy, float aspect,
			float zNear, float zFar) {

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
	 * Calculates rotation matrix into given matrix array.
	 * 
	 * @param m
	 *            Matrix float array
	 * @param offset
	 *            Matrix start offset
	 * @param x
	 *            Rotation around x axis
	 * @param y
	 *            Rotation around y axis
	 * @param z
	 *            Rotation around z axis
	 */
	public static void setRotateM(float[] m, int offset, float x, float y,
			float z) {
		double toRadians = Math.PI * 2 / 360;
		double sin0 = Math.sin(x * toRadians);
		double cos0 = Math.cos(x * toRadians);
		double sin1 = Math.sin(y * toRadians);
		double cos1 = Math.cos(y * toRadians);
		double sin2 = Math.sin(z * toRadians);
		double cos2 = Math.cos(z * toRadians);

		Matrix.setIdentityM(m, offset);

		double sin1_cos2 = sin1 * cos2;
		double sin1_sin2 = sin1 * sin2;

		m[0 + offset] = (float) (cos1 * cos2);
		m[1 + offset] = (float) (cos1 * sin2);
		m[2 + offset] = (float) (-sin1);

		m[4 + offset] = (float) ((-cos0 * sin2) + (sin0 * sin1_cos2));
		m[5 + offset] = (float) ((cos0 * cos2) + (sin0 * sin1_sin2));
		m[6 + offset] = (float) (sin0 * cos1);

		m[8 + offset] = (float) ((sin0 * sin2) + (cos0 * sin1_cos2));
		m[9 + offset] = (float) ((-sin0 * cos2) + (cos0 * sin1_sin2));
		m[10 + offset] = (float) (cos0 * cos1);
	}
}
