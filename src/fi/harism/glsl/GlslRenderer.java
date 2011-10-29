package fi.harism.glsl;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;

public class GlslRenderer implements GLSurfaceView.Renderer {

	private float[] mWorldMatrix = new float[16];
	private float[] mProjMatrix = new float[16];
	private float[] mViewMatrix = new float[16];

	private float mFPS = 0f;
	private long mLastRenderTime = 0;

	private GlslWorld mGlslWorld;

	public GlslRenderer(Context context) {
		mGlslWorld = new GlslWorld(context);
	}

	public float getFPS() {
		return mFPS;
	}

	@Override
	public void onDrawFrame(GL10 glUnused) {
		Matrix.multiplyMM(mWorldMatrix, 0, mProjMatrix, 0, mViewMatrix, 0);
		mGlslWorld.onDrawFrame(mWorldMatrix);

		long time = SystemClock.uptimeMillis();
		long diff = time - mLastRenderTime;
		mFPS = 1000f / diff;
		mLastRenderTime = time;

		Matrix.rotateM(mViewMatrix, 0, 1f, 0f, 1f, 0f);
	}

	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
		float ratio = (float) width / height;
		Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 100);
	}

	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		mGlslWorld.onSurfaceCreated();
		mLastRenderTime = SystemClock.uptimeMillis();
		Matrix.setLookAtM(mViewMatrix, 0, 0, 3, 5, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
	}

}
