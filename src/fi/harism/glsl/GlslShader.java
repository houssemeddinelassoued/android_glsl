package fi.harism.glsl;

import java.util.HashMap;

import android.opengl.GLES20;

public class GlslShader {

	private int mProgram = 0;
	private HashMap<String, Integer> mShaderHandleMap;

	public GlslShader() {
		mShaderHandleMap = new HashMap<String, Integer>();
	}

	public final void addHandles(String... names) {
		for (String name : names) {
			int handle = GLES20.glGetAttribLocation(mProgram, name);
			if (handle == -1) {
				handle = GLES20.glGetUniformLocation(mProgram, name);
			}
			if (handle == -1) {
				throw new RuntimeException("Could not get attrib location for "
						+ name);
			}
			mShaderHandleMap.put(name, handle);
		}
	}

	public final int getHandle(String name) {
		if (mShaderHandleMap.containsKey(name)) {
			return mShaderHandleMap.get(name);
		}
		return -1;
	}

	public final int getProgram() {
		return mProgram;
	}

	public final void setProgram(String vertexSource, String fragmentSource) {
		int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
		int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
		int program = GLES20.glCreateProgram();
		if (program != 0) {
			GLES20.glAttachShader(program, vertexShader);
			GLES20.glAttachShader(program, pixelShader);
			GLES20.glLinkProgram(program);
			int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
			if (linkStatus[0] != GLES20.GL_TRUE) {
				GLES20.glDeleteProgram(program);
				throw new RuntimeException(GLES20.glGetProgramInfoLog(program));
			}
		}
		mProgram = program;
	}

	private final int loadShader(int shaderType, String source) {
		int shader = GLES20.glCreateShader(shaderType);
		if (shader != 0) {
			GLES20.glShaderSource(shader, source);
			GLES20.glCompileShader(shader);
			int[] compiled = new int[1];
			GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
			if (compiled[0] == 0) {
				GLES20.glDeleteShader(shader);
				throw new RuntimeException(GLES20.glGetShaderInfoLog(shader));
			}
		}
		return shader;
	}

}
