package fi.harism.glsl;

import android.opengl.Matrix;

public class GlslMatrix {

	public static final void setPerspectiveM(float[] m, float fovy,
			float aspect, float zNear, float zFar) {

		Matrix.setIdentityM(m, 0);

		float xymax = zNear * (float) Math.tan(fovy * Math.PI / 360);
		float ymin = -xymax;
		float xmin = -xymax;

		float width = xymax - xmin;
		float height = xymax - ymin;

		float depth = zFar - zNear;
		float q = -(zFar + zNear) / depth;
		float qn = -2 * (zFar * zNear) / depth;

		float w = 2 * zNear / width;
		w = w / aspect;
		float h = 2 * zNear / height;

		m[0] = w;
		m[5] = h;
		m[10] = q;
		m[11] = -1;
		m[14] = qn;
		m[15] = 0;
	}

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
