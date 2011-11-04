package fi.harism.glsl;

import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;

public final class GlslActivity extends Activity {

	private SurfaceView mSurfaceView;
	private Renderer mRenderer;

	private float mFps;
	private Timer mFpsTimer;
	private Runnable mFpsRunnable;

	private String mAppName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mRenderer = new Renderer();
		mSurfaceView = new SurfaceView(this);
		mSurfaceView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR
				| GLSurfaceView.DEBUG_LOG_GL_CALLS);
		mSurfaceView.setEGLContextClientVersion(2);
		mSurfaceView.setRenderer(mRenderer);
		mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

		mAppName = getString(R.string.app_name);
		mFpsRunnable = new FpsRunnable();

		PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
		setContentView(mSurfaceView);
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
			startActivity(new Intent(this,
					fi.harism.glsl.GlslPreferenceActivity.class));
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
		mRenderer.setPreferences(this,
				PreferenceManager.getDefaultSharedPreferences(this));
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

	private class FpsTimerTask extends TimerTask {
		@Override
		public void run() {
			runOnUiThread(mFpsRunnable);
		}
	}

	private final class Renderer implements GLSurfaceView.Renderer {

		private int mWidth, mHeight;

		private float[] mProjectionMatrix = new float[16];
		private float[] mViewMatrix = new float[16];

		private long mLastRenderTime = 0;

		private GlslScene mScene;
		private GlslFilter mFilter;

		private boolean mResetFramebuffers;
		private GlslFramebuffer mFboScreen;
		private GlslFramebuffer mFboScreenHalf;

		private boolean mDivideScreen;
		private boolean mBokehEnabled;

		public Renderer() {
			mScene = new GlslScene();
			mFilter = new GlslFilter();
			mFboScreen = new GlslFramebuffer();
			mFboScreenHalf = new GlslFramebuffer();

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

			mFboScreen.useTexture("tex1");
			mScene.draw(mViewMatrix, mProjectionMatrix);

			if (mBokehEnabled) {
				mFilter.bokeh(mFboScreen.getTexture("tex1"), mFboScreenHalf,
						"tex1", "tex2", "tex3", mWidth / 2, mHeight / 2);

				mFboScreen.useTexture("tex2");
				mFilter.blend(mFboScreenHalf.getTexture("tex3"),
						mFboScreen.getTexture("tex1"));
			} else {
				mFboScreen.useTexture("tex2");
				mFilter.copy(mFboScreen.getTexture("tex1"));
			}

			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
			if (mDivideScreen) {
				GLES20.glViewport(0, 0, mWidth / 2, mHeight);
				mFilter.copy(mFboScreen.getTexture("tex1"), 0f, 0f, .5f, 1f);
				GLES20.glViewport(mWidth / 2, 0, mWidth / 2, mHeight);
				mFilter.copy(mFboScreen.getTexture("tex2"), .5f, 0f, 1f, 1f);
			} else {
				GLES20.glViewport(0, 0, mWidth, mHeight);
				mFilter.copy(mFboScreen.getTexture("tex2"));
			}

			long time = SystemClock.uptimeMillis();
			if (mLastRenderTime != 0) {
				long diff = time - mLastRenderTime;
				mFps = 1000f / diff;
				mScene.update(diff / 1000f);
			}
			mLastRenderTime = time;

		}

		@Override
		public void onSurfaceChanged(GL10 glUnused, int width, int height) {
			mWidth = width;
			mHeight = height;

			if (mResetFramebuffers) {
				mFboScreen.reset();
				mFboScreenHalf.reset();
			}
			mResetFramebuffers = true;

			mFboScreen.init(width, height);
			mFboScreen.addTexture("tex1");
			mFboScreen.addTexture("tex2");
			mFboScreenHalf.init(width / 2, height / 2);
			mFboScreenHalf.addTexture("tex1");
			mFboScreenHalf.addTexture("tex2");
			mFboScreenHalf.addTexture("tex3");
		}

		@Override
		public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
			mScene.init(GlslActivity.this);
			mFilter.init(GlslActivity.this);
			mResetFramebuffers = false;
		}

		public void setPreferences(Context ctx, SharedPreferences preferences) {
			String key = ctx.getString(R.string.key_divide_screen);
			mDivideScreen = preferences.getBoolean(key, false);
			key = ctx.getString(R.string.key_bokeh_enable);
			mBokehEnabled = preferences.getBoolean(key, true);
			mFilter.setPreferences(ctx, preferences);
		}
	}

	private final class SurfaceView extends GLSurfaceView {

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
