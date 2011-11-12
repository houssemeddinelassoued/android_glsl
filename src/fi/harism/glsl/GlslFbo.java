package fi.harism.glsl;

import android.opengl.GLES20;

public class GlslFbo {

	private int mWidth, mHeight;
	private int mFramebufferHandle = 0;
	private int mRenderbufferHandle = -1;
	private int[] mTextureHandles = {};

	public void bind() {
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebufferHandle);
		GLES20.glViewport(0, 0, mWidth, mHeight);
	}

	public void bindTexture(int index) {
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
				GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,
				mTextureHandles[index], 0);
	}

	public int getFramebuffer() {
		return mFramebufferHandle;
	}

	public int getRenderbuffer() {
		return mRenderbufferHandle;
	}

	public int getTexture(int index) {
		return mTextureHandles[index];
	}

	public void init(int width, int height, int textures) {
		init(width, height, textures, false);
	}

	public void init(int width, int height, int textures,
			boolean genRenderbuffer) {
		mWidth = width;
		mHeight = height;

		int handle[] = { 0 };
		GLES20.glGenFramebuffers(1, handle, 0);
		mFramebufferHandle = handle[0];
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebufferHandle);

		mTextureHandles = new int[textures];
		GLES20.glGenTextures(textures, mTextureHandles, 0);
		for (int texture : mTextureHandles) {
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
					GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
					GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
					GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
					GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
					mWidth, mHeight, 0, GLES20.GL_RGBA,
					GLES20.GL_UNSIGNED_BYTE, null);
		}

		if (genRenderbuffer) {
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
		GLES20.glDeleteTextures(mTextureHandles.length, mTextureHandles, 0);
		mFramebufferHandle = mRenderbufferHandle = -1;
		mTextureHandles = new int[0];
	}

}