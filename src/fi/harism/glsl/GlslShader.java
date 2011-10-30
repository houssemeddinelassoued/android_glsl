package fi.harism.glsl;

import java.util.HashMap;

import android.content.Context;
import android.opengl.GLES20;

public class GlslShader {

	private int mProgram = 0;
	private Context mContext;
	private HashMap<String, Integer> mShaderHandleMap;

	public GlslShader(Context context) {
		mContext = context;
		mShaderHandleMap = new HashMap<String, Integer>();
	}

	public void addHandle(String name) {
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

	public int getHandle(String name) {
		if (mShaderHandleMap.containsKey(name)) {
			return mShaderHandleMap.get(name);
		}
		return -1;
	}

	public int getProgram() {
		return mProgram;
	}

	public void loadProgram(int vertexId, int fragmentId) {
		if (mProgram != 0) {
			GLES20.glDeleteProgram(mProgram);
			mProgram = 0;
		}
		String vertexSource = mContext.getString(vertexId);
		String fragmentSource = mContext.getString(fragmentId);
		mProgram = GlslUtils.createProgram(vertexSource, fragmentSource);
	}
}
