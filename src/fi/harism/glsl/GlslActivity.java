package fi.harism.glsl;

import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;

public class GlslActivity extends Activity {

	private SurfaceView mSurfaceView;
	private Renderer mRenderer;

	private float mFps;
	private Timer mFpsTimer;
	private Runnable mFpsRunnable;

	private String mAppName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSurfaceView = new SurfaceView(this);
		mSurfaceView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR
				| GLSurfaceView.DEBUG_LOG_GL_CALLS);
		mSurfaceView.setEGLContextClientVersion(2);

		mRenderer = new Renderer(this);
		mSurfaceView.setRenderer(mRenderer);

		mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

		setContentView(mSurfaceView);

		mFpsRunnable = new FpsRunnable();
		mAppName = getString(R.string.app_name);

		PreferenceManager.setDefaultValues(this, R.xml.settings, false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		}
		return false;
	}

	@Override
	public void onPause() {
		super.onPause();
		mSurfaceView.onPause();
		mFpsTimer.cancel();
		mFpsTimer = null;
	}

	@Override
	public void onResume() {
		super.onResume();
		mSurfaceView.onResume();
		mFpsTimer = new Timer();
		mFpsTimer.scheduleAtFixedRate(new FpsTimerTask(), 0, 200);
	}

	private class FpsTimerTask extends TimerTask {
		@Override
		public void run() {
			runOnUiThread(mFpsRunnable);
		}
	}

	private class FpsRunnable implements Runnable {
		@Override
		public void run() {
			String fps = Float.toString(mFps);
			int separator = fps.indexOf('.');
			if (separator == -1) {
				separator = fps.indexOf(',');
			}
			if (separator != -1) {
				fps = fps.substring(0, separator + 2);
			}
			setTitle(mAppName + " (" + fps + "fps)");
		}
	}

	private class Renderer implements GLSurfaceView.Renderer {

		private int mWidth, mHeight;

		private float[] mProjectionMatrix = new float[16];
		private float[] mViewMatrix = new float[16];

		private long mLastRenderTime = 0;

		private GlslScene mGlslScene;
		private GlslFilters mGlslFilters;

		private boolean mResetFramebuffers = true;
		private GlslFramebuffer mGlslFramebufferScreen;
		private GlslFramebuffer mGlslFramebufferScreenHalf;

		public Renderer(Context context) {
			mGlslScene = new GlslScene(context);
			mGlslFilters = new GlslFilters(context);
			mGlslFramebufferScreen = new GlslFramebuffer();
			mGlslFramebufferScreenHalf = new GlslFramebuffer();

			mLastRenderTime = SystemClock.uptimeMillis();
			Matrix.setLookAtM(mViewMatrix, 0, 0f, 3f, 8f, 0f, 0f, 0f, 0f, 1.0f,
					0.0f);
		}

		@Override
		public void onDrawFrame(GL10 glUnused) {

			float ratio = (float) mWidth / mHeight;
			// Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 1,
			// 20);
			GlslUtils.setPerspectiveM(mProjectionMatrix, 45f, ratio, 1f, 20f);
			Matrix.setLookAtM(mViewMatrix, 0, 0f, 3f, 8f, 0f, 0f, 0f, 0f, 1.0f,
					0.0f);

			float rot = 360f * (SystemClock.uptimeMillis() % 6000L) / 6000;
			Matrix.rotateM(mViewMatrix, 0, rot, 0f, 1f, 0f);

			mGlslFramebufferScreen.useTexture("tex1");
			mGlslScene.draw(mViewMatrix, mProjectionMatrix);

			mGlslFilters.bokeh(mGlslFramebufferScreen.getTexture("tex1"),
					mGlslFramebufferScreenHalf, "tex1", "tex2", "tex3",
					mWidth / 2, mHeight / 2);

			GLES20.glViewport(0, 0, mWidth, mHeight);
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
			// mGlslFilters.copy(mGlslFramebufferScreen.getTexture("tex1"));
			// mGlslFilters.copy(mGlslFramebufferScreenHalf.getTexture("tex3"));
			mGlslFilters.blend(mGlslFramebufferScreenHalf.getTexture("tex3"),
					mGlslFramebufferScreen.getTexture("tex1"));

			long time = SystemClock.uptimeMillis();
			if (mLastRenderTime != 0) {
				long diff = time - mLastRenderTime;
				mFps = 1000f / diff;
				mGlslScene.update(diff / 1000f);
			}
			mLastRenderTime = time;

		}

		@Override
		public void onSurfaceChanged(GL10 glUnused, int width, int height) {
			mWidth = width;
			mHeight = height;
			if (!mResetFramebuffers) {
				mGlslFramebufferScreen.reset();
				mGlslFramebufferScreenHalf.reset();
			}
			mGlslFramebufferScreen.init(width, height);
			mGlslFramebufferScreen.addTexture("tex1");
			mGlslFramebufferScreen.addTexture("tex2");
			mGlslFramebufferScreenHalf.init(width / 2, height / 2);
			mGlslFramebufferScreenHalf.addTexture("tex1");
			mGlslFramebufferScreenHalf.addTexture("tex2");
			mGlslFramebufferScreenHalf.addTexture("tex3");
		}

		@Override
		public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
			mGlslScene.init();
			mGlslFilters.init();
			mResetFramebuffers = true;
		}
	}

	private class SurfaceView extends GLSurfaceView {

		public SurfaceView(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
		}

		public SurfaceView(Context context, AttributeSet attrs) {
			super(context, attrs);
			// TODO Auto-generated constructor stub
		}

	}
}
