package fi.harism.glsl;

import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import fi.harism.glsl.object.GlslScene;

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

		private static final String TEX_OUT = "out";
		private static final String TEX_LIGHT1 = "light1";
		private static final String TEX_LIGHT2 = "light2";
		private static final String TEX_BOKEH1 = "bokeh1";
		private static final String TEX_BOKEH2 = "bokeh2";
		private static final String TEX_BOKEH3 = "bokeh3";

		private int mWidth, mHeight;

		private float[] mProjM = new float[16];
		private float[] mViewM = new float[16];

		private long mLastRenderTime = 0;

		private GlslScene mScene = new GlslScene();
		private GlslFilter mFilter = new GlslFilter();

		private boolean mResetFramebuffers;
		private GlslFramebuffer mFboScreen = new GlslFramebuffer();
		private GlslFramebuffer mFboScreenHalf = new GlslFramebuffer();

		private boolean mDivideScreen;
		private boolean mBokehEnabled;

		private GlslShader mMainShader = new GlslShader();
		private GlslShader mLightShader = new GlslShader();

		public Renderer() {
			mLastRenderTime = SystemClock.uptimeMillis();
		}

		@Override
		public void onDrawFrame(GL10 glUnused) {

			float ratio = (float) mWidth / mHeight;
			// Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 1,
			// 20);
			GlslUtils.setPerspectiveM(mProjM, 45f, ratio, 1f, 21f);
			Matrix.setLookAtM(mViewM, 0, 0f, 3f, -8f, 0f, 0f, 0f, 0f, 1.0f,
					0.0f);

			float rot = 360f * (SystemClock.uptimeMillis() % 6000L) / 6000;
			Matrix.rotateM(mViewM, 0, rot, 0f, 1f, 0f);

			mScene.setMVP(mViewM, mProjM);

			GLES20.glEnable(GLES20.GL_CULL_FACE);
			GLES20.glFrontFace(GLES20.GL_CCW);
			GLES20.glEnable(GLES20.GL_DEPTH_TEST);
			GLES20.glDepthFunc(GLES20.GL_LEQUAL);

			String lightTextureIn = TEX_LIGHT1;
			String lightTextureOut = TEX_LIGHT2;

			mFboScreen.useTexture(lightTextureOut);
			GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT
					| GLES20.GL_DEPTH_BUFFER_BIT);
			mFboScreen.useTexture(lightTextureIn);
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

			int lightIds[] = mLightShader.getHandles("uMVMatrix", "uMVPMatrix",
					"uNormalMatrix", "aPosition", "aNormal", "uLightPos");
			GLES20.glUseProgram(mLightShader.getProgram());
			for (int i = 0; i < mScene.getLightCount(); ++i) {
				float lightPos[] = { 0, 0, 0 };
				mScene.getLightPosition(i, lightPos);
				mFboScreen.useTexture(lightTextureIn);
				GLES20.glUniform3fv(lightIds[5], 1, lightPos, 0);
				GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
						mFboScreen.getTexture(lightTextureOut));
				mScene.draw(lightIds[0], lightIds[1], lightIds[2], lightIds[3],
						lightIds[4], -1);

				String in = lightTextureIn;
				lightTextureIn = lightTextureOut;
				lightTextureOut = in;
			}

			String sceneTexture = lightTextureIn;
			int mainIds[] = mMainShader.getHandles("uMVPMatrix", "aPosition",
					"aColor");
			mFboScreen.useTexture(sceneTexture);
			GLES20.glUseProgram(mMainShader.getProgram());
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
					mFboScreen.getTexture(lightTextureOut));
			mScene.draw(-1, mainIds[0], -1, mainIds[1], -1, mainIds[2]);

			// mFboScreen.useTexture(sceneTexture);
			// mFilter.copy(mFboScreen.getTexture(lightTextureOut));

			if (mBokehEnabled) {
				mFilter.bokeh(mFboScreen.getTexture(sceneTexture),
						mFboScreenHalf, TEX_BOKEH1, TEX_BOKEH2, TEX_BOKEH3,
						mWidth / 2, mHeight / 2);

				mFboScreen.useTexture(TEX_OUT);
				mFilter.blend(mFboScreenHalf.getTexture(TEX_BOKEH3),
						mFboScreen.getTexture(sceneTexture));
			} else {
				mFboScreen.useTexture(TEX_OUT);
				mFilter.copy(mFboScreen.getTexture(sceneTexture));
			}

			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
			if (mDivideScreen) {
				mFilter.setClipCoords(-1f, 1f, 0f, -1f);
				mFilter.copy(mFboScreen.getTexture(sceneTexture));
				mFilter.setClipCoords(0f, 1f, 1f, -1f);
				mFilter.copy(mFboScreen.getTexture(TEX_OUT));
				mFilter.setClipCoords(-1f, 1f, 1f, -1f);
			} else {
				GLES20.glViewport(0, 0, mWidth, mHeight);
				mFilter.copy(mFboScreen.getTexture(TEX_OUT));
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
			mFboScreen.addTexture(TEX_LIGHT1);
			mFboScreen.addTexture(TEX_LIGHT2);
			mFboScreen.addTexture(TEX_OUT);
			mFboScreenHalf.init(width / 2, height / 2);
			mFboScreenHalf.addTexture(TEX_BOKEH1);
			mFboScreenHalf.addTexture(TEX_BOKEH2);
			mFboScreenHalf.addTexture(TEX_BOKEH3);
		}

		@Override
		public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
			mScene.init(GlslActivity.this);
			mFilter.init(GlslActivity.this);
			mResetFramebuffers = false;

			mMainShader.setProgram(getString(R.string.shader_render_scene_vs),
					getString(R.string.shader_render_scene_fs));
			mMainShader.addHandles("uMVPMatrix", "aPosition", "aColor");

			mLightShader.setProgram(getString(R.string.shader_render_light_vs),
					getString(R.string.shader_render_light_fs));
			mLightShader.addHandles("uMVMatrix", "uMVPMatrix", "uNormalMatrix",
					"uLightPos", "aPosition", "aNormal");
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
