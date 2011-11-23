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

import java.util.HashMap;

import android.opengl.GLES20;
import android.util.Log;

/**
 * Helper class for handling shaders.
 */
public final class GlslShader {

	private int mProgram = 0;
	private final HashMap<String, Integer> mShaderHandleMap = new HashMap<String, Integer>();

	/**
	 * Searches for given attribute/uniform names for loaded shader and stores
	 * their ids for later use. getHandle() and getHandles() should be called
	 * only after calling this method.
	 * 
	 * @param names
	 *            List of attribute/uniform names to search for.
	 */
	public void addHandles(String... names) {
		for (String name : names) {
			int handle = GLES20.glGetAttribLocation(mProgram, name);
			if (handle == -1) {
				handle = GLES20.glGetUniformLocation(mProgram, name);
			}
			if (handle == -1) {
				Log.d("GlslShader", "Could not get attrib location for " + name);
			}
			mShaderHandleMap.put(name, handle);
		}
	}

	/**
	 * Get id for given handle name.
	 * 
	 * @param name
	 *            Name of handle.
	 * @return Id for given handle or -1 if none found.
	 */
	public int getHandle(String name) {
		if (mShaderHandleMap.containsKey(name)) {
			return mShaderHandleMap.get(name);
		}
		Log.d("GlslShader", "Attribute handle " + name + " not found.");
		return -1;
	}

	/**
	 * Get array of ids with given names. Returned array is sized to given
	 * amount name elements.
	 * 
	 * @param names
	 *            List of handle names.
	 * @return array of handle ids.
	 */
	public int[] getHandles(String... names) {
		int[] res = new int[names.length];
		for (int i = 0; i < names.length; ++i) {
			res[i] = getHandle(names[i]);
		}
		return res;
	}

	/**
	 * Getter for program id loaded into this shader object.
	 * 
	 * @return program id.
	 */
	public int getProgram() {
		return mProgram;
	}

	/**
	 * Compiles vertex and fragment shaders and links them into a progtam one
	 * can use for rendering.
	 * 
	 * @param vertexSource
	 *            String presentation for vertex shader
	 * @param fragmentSource
	 *            String presentation for fragment shader
	 */
	public void setProgram(String vertexSource, String fragmentSource) {
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
				String error = GLES20.glGetProgramInfoLog(program);
				GLES20.glDeleteProgram(program);
				throw new RuntimeException(error);
			}
		}
		mProgram = program;
		mShaderHandleMap.clear();
	}

	/**
	 * Helper method for compiling a shader.
	 * 
	 * @param shaderType
	 *            Type of shader to compile
	 * @param source
	 *            String presentation for shader
	 * @return id for compiled shader
	 */
	private int loadShader(int shaderType, String source) {
		int shader = GLES20.glCreateShader(shaderType);
		if (shader != 0) {
			GLES20.glShaderSource(shader, source);
			GLES20.glCompileShader(shader);
			int[] compiled = new int[1];
			GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
			if (compiled[0] == 0) {
				String error = GLES20.glGetShaderInfoLog(shader);
				GLES20.glDeleteShader(shader);
				throw new RuntimeException(error);
			}
		}
		return shader;
	}

}
