package fi.harism.glsl;

import java.util.HashMap;

import android.opengl.GLES20;

public class GlslFramebuffer {

	private int mWidth, mHeight;
	private int mFramebufferHandle = -1;
	private int mRenderbufferHandle = -1;
	private HashMap<String, Integer> mTextureHandleMap;

	public GlslFramebuffer() {
		mTextureHandleMap = new HashMap<String, Integer>();
	}

	public void addTexture(String name) {
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

		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mWidth,
				mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

		mTextureHandleMap.put(name, handle[0]);
	}

	public int getFramebufferHandle() {
		return mFramebufferHandle;
	}

	public int getTexture(String name) {
		if (!mTextureHandleMap.containsKey(name)) {
			throw new RuntimeException("No texture handle " + name + "found.");
		}
		return mTextureHandleMap.get(name);
	}

	public void init(int width, int height, boolean depthBuffer) {
		mWidth = width;
		mHeight = height;

		int handle[] = { 0 };
		GLES20.glGenFramebuffers(1, handle, 0);
		mFramebufferHandle = handle[0];

		if (depthBuffer) {
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebufferHandle);
			GLES20.glGenRenderbuffers(1, handle, 0);
			mRenderbufferHandle = handle[0];
			GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER,
					mRenderbufferHandle);

			GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER,
					GLES20.GL_DEPTH_COMPONENT16, width, height);
			GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER,
					GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER,
					mRenderbufferHandle);
		}
	}

	public void reset() {
		int[] handle = { mFramebufferHandle };
		GLES20.glDeleteFramebuffers(1, handle, 0);
		handle[0] = mRenderbufferHandle;
		GLES20.glDeleteRenderbuffers(1, handle, 0);
		mFramebufferHandle = mRenderbufferHandle = -1;

		for (Integer textureId : mTextureHandleMap.values()) {
			handle[0] = textureId;
			GLES20.glDeleteTextures(1, handle, 0);
		}
		mTextureHandleMap.clear();
	}

	public void useTexture(String name) {
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebufferHandle);
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
				GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,
				getTexture(name), 0);
		GLES20.glViewport(0, 0, mWidth, mHeight);
	}

}
