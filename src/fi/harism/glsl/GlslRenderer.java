package fi.harism.glsl;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;

public class GlslRenderer implements GLSurfaceView.Renderer {

	private int mWidth, mHeight;

	private float[] mProjectionMatrix = new float[16];
	private float[] mViewMatrix = new float[16];

	private float mFPS = 0f;
	private long mLastRenderTime = 0;

	private GlslScene mGlslScene;
	private GlslFilters mGlslFilters;

	private boolean mResetFramebuffers = true;
	private GlslFramebuffer mGlslFramebuffer;

	public GlslRenderer(Context context) {
		mGlslScene = new GlslScene(context);
		mGlslFilters = new GlslFilters(context);
		mGlslFramebuffer = new GlslFramebuffer();

		mLastRenderTime = SystemClock.uptimeMillis();
		Matrix.setLookAtM(mViewMatrix, 0, 0f, 3f, 8f, 0f, 0f, 0f, 0f, 1.0f,
				0.0f);
	}

	public float getFPS() {
		return mFPS;
	}

	@Override
	public void onDrawFrame(GL10 glUnused) {

		float ratio = (float) mGlslFramebuffer.getWidth()
				/ mGlslFramebuffer.getHeight();
		//Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 20);
		GlslUtils.setPerspectiveM(mProjectionMatrix, 45f, ratio, 1f, 20f);

		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,
				mGlslFramebuffer.getFramebufferId());
		GLES20.glViewport(0, 0, mGlslFramebuffer.getWidth(),
				mGlslFramebuffer.getHeight());
		mGlslScene.draw(mViewMatrix, mProjectionMatrix);

		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glViewport(0, 0, mWidth, mHeight);
		mGlslFilters.draw(mGlslFramebuffer.getTextureId());

		long time = SystemClock.uptimeMillis();
		if (mLastRenderTime != 0) {
			long diff = time - mLastRenderTime;
			mFPS = 1000f / diff;
			mGlslScene.update(diff / 1000f);
		}
		mLastRenderTime = time;

		Matrix.rotateM(mViewMatrix, 0, 1f, 0f, 1f, 0f);
	}

	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {

		mWidth = width;
		mHeight = height;

		if (!mResetFramebuffers) {
			mGlslFramebuffer.reset();
		}
		mGlslFramebuffer.create(width, height);
	}

	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		mGlslScene.init();
		mGlslFilters.init();
		mResetFramebuffers = true;
		Matrix.setLookAtM(mViewMatrix, 0, 0f, 3f, 8f, 0f, 0f, 0f, 0f, 1.0f,
				0.0f);
	}

}
