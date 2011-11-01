package fi.harism.glsl;

import java.util.HashMap;

import android.opengl.GLES20;

public class GlslFramebuffer {

	private int mFramebufferHandle;
	private int mRenderbufferHandle;
	private HashMap<String, Integer> mTextureHandleMap;

	public GlslFramebuffer() {
		mTextureHandleMap = new HashMap<String, Integer>();
	}

	public void addTexture(String name, int width, int height) {
		int handle[] = { 0 };
		GLES20.glGenTextures(1, handle, 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, handle[0]);

		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
				GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
				GLES20.GL_CLAMP_TO_EDGE);

		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width,
				height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

		mTextureHandleMap.put(name, handle[0]);
	}

	public void create(int width, int height) {
		int handle[] = { 0 };
		GLES20.glGenFramebuffers(1, handle, 0);
		mFramebufferHandle = handle[0];
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebufferHandle);

		GLES20.glGenRenderbuffers(1, handle, 0);
		mRenderbufferHandle = handle[0];
		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mRenderbufferHandle);

		GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER,
				GLES20.GL_DEPTH_COMPONENT16, width, height);
		GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER,
				GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER,
				mRenderbufferHandle);
	}

	public int getFramebufferHandle() {
		return mFramebufferHandle;
	}

	public int getTexture(String name) {
		if (!mTextureHandleMap.containsKey(name)) {
			throw new RuntimeException("No texture handle " + name);
		}
		return mTextureHandleMap.get(name);
	}

	public void reset() {
		int[] handle = { mFramebufferHandle };
		GLES20.glDeleteFramebuffers(1, handle, 0);
		handle[0] = mRenderbufferHandle;
		GLES20.glDeleteRenderbuffers(1, handle, 0);

		Integer texHandles[] = (Integer[]) mTextureHandleMap.values().toArray();
		for (int i = 0; i < texHandles.length; ++i) {
			handle[0] = texHandles[i];
			GLES20.glDeleteTextures(1, handle, 0);
		}
		mTextureHandleMap.clear();
	}

	public void useTexture(String name) {
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebufferHandle);
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
				GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,
				getTexture(name), 0);
	}

}
