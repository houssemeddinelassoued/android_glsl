package fi.harism.glsl;

import android.opengl.GLES20;

public class GlslFramebuffer {

	private static final int FRAMEBUFFER_ID = 0;
	private static final int TEXTURE_ID = 1;
	private static final int RENDERBUFFER_ID = 2;

	private int mWidth;
	private int mHeight;

	private int[] mHandles = new int[3];

	public GlslFramebuffer() {
	}

	public void create(int width, int height) {
		mWidth = width;
		mHeight = height;

		GLES20.glGenFramebuffers(1, mHandles, FRAMEBUFFER_ID);
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,
				mHandles[FRAMEBUFFER_ID]);

		GLES20.glGenTextures(1, mHandles, TEXTURE_ID);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mHandles[TEXTURE_ID]);
		/*
		 * GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
		 * GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		 * GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
		 * GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		 */
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width,
				height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
				GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,
				mHandles[TEXTURE_ID], 0);

		GLES20.glGenRenderbuffers(1, mHandles, RENDERBUFFER_ID);
		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER,
				mHandles[RENDERBUFFER_ID]);
		GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER,
				GLES20.GL_DEPTH_COMPONENT16, width, height);
		GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER,
				GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER,
				mHandles[RENDERBUFFER_ID]);
	}

	public int getFramebufferId() {
		return mHandles[FRAMEBUFFER_ID];
	}

	public int getHeight() {
		return mHeight;
	}

	public int getRenderbufferId() {
		return mHandles[RENDERBUFFER_ID];
	}

	public int getTextureId() {
		return mHandles[TEXTURE_ID];
	}

	public int getWidth() {
		return mWidth;
	}

	public void reset() {
		GLES20.glDeleteFramebuffers(1, mHandles, FRAMEBUFFER_ID);
		GLES20.glDeleteTextures(1, mHandles, TEXTURE_ID);
		GLES20.glDeleteRenderbuffers(1, mHandles, RENDERBUFFER_ID);
	}

}
