package fi.harism.glsl;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

public class GlslUtils {

	public static final int createProgram(String vertexSource,
			String fragmentSource) {
		int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
		if (vertexShader == 0) {
			return 0;
		}

		int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
		if (pixelShader == 0) {
			return 0;
		}

		int program = GLES20.glCreateProgram();
		if (program != 0) {
			GLES20.glAttachShader(program, vertexShader);
			GLES20.glAttachShader(program, pixelShader);
			GLES20.glLinkProgram(program);
			int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
			if (linkStatus[0] != GLES20.GL_TRUE) {
				Log.e("GlslUtils", GLES20.glGetProgramInfoLog(program));
				GLES20.glDeleteProgram(program);
				program = 0;
			}
		}
		return program;
	}

	public static final int loadShader(int shaderType, String source) {
		int shader = GLES20.glCreateShader(shaderType);
		if (shader != 0) {
			GLES20.glShaderSource(shader, source);
			GLES20.glCompileShader(shader);
			int[] compiled = new int[1];
			GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
			if (compiled[0] == 0) {
				Log.e("GlslUtils", GLES20.glGetShaderInfoLog(shader));
				GLES20.glDeleteShader(shader);
				shader = 0;
			}
		}
		return shader;
	}

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
