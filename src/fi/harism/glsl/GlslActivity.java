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
import fi.harism.glsl.scene.GlslScene;

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
					fi.harism.glsl.prefs.GlslPreferenceActivity.class));
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

		private static final int TEX_OUT = 0;
		private static final int TEX_SCENE = 1;
		private static final int TEX_BOKEH1 = 0;
		private static final int TEX_BOKEH2 = 1;
		private static final int TEX_BOKEH3 = 2;

		private long mLastRenderTime = 0;

		private GlslScene mScene = new GlslScene();
		private GlslFilter mFilter = new GlslFilter();
		private GlslData mData = new GlslData();

		private boolean mResetFramebuffers;
		private GlslFbo mFbo = new GlslFbo();
		private GlslFbo mFboHalf = new GlslFbo();

		private boolean mDivideScreen;
		private boolean mLensBlurEnabled;

		private GlslShader mSceneShader = new GlslShader();

		public Renderer() {
			mLastRenderTime = SystemClock.uptimeMillis();
		}

		@Override
		public void onDrawFrame(GL10 glUnused) {

			mScene.setMVP(mData.mViewM, mData.mProjM);

			GLES20.glEnable(GLES20.GL_CULL_FACE);
			GLES20.glFrontFace(GLES20.GL_CCW);
			GLES20.glEnable(GLES20.GL_DEPTH_TEST);
			GLES20.glDepthFunc(GLES20.GL_LEQUAL);

			mFbo.bind();
			mFbo.bindTexture(TEX_SCENE);
			GLES20.glClearColor(0.2f, 0.3f, 0.5f, 1.0f);
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT
					| GLES20.GL_DEPTH_BUFFER_BIT);

			int lightCount = mScene.getLightCount();
			float lightPositions[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
					0, 0, 0 };
			for (int i = 0; i < lightCount; ++i) {
				mScene.getLightPosition(i, lightPositions, i * 4);
			}
			int shaderIds[] = mSceneShader.getHandles("uLightCount", "uLights",
					"uCocScale", "uCocBias");
			GLES20.glUseProgram(mSceneShader.getProgram());
			GLES20.glUniform1i(shaderIds[0], lightCount);
			GLES20.glUniform4fv(shaderIds[1], 4, lightPositions, 0);

			float zNear = mData.mZNear;
			float zFar = mData.mZFar;

			// TODO: Fixme
			// http://en.wikipedia.org/wiki/Angle_of_view
			float fLen = (float) ((180.0 * 3.0) / (Math.PI * mData.mFovY));
			float fPlane = (fLen * 2) * mData.mFocalPlane * 20f;
			float A = (fLen * 4) / mData.mFStop;

			float cocScale = (A * fLen * fPlane * (zFar - zNear))
					/ ((fPlane - fLen) * zNear * zFar);
			float cocBias = (A * fLen * (zNear - fPlane))
					/ ((fPlane + fLen) * zNear);

			GLES20.glUniform1f(shaderIds[2], cocScale);
			GLES20.glUniform1f(shaderIds[3], cocBias);

			mScene.draw(mData);

			if (mLensBlurEnabled) {
				mFilter.lensBlur(mFbo.getTexture(TEX_SCENE), mFboHalf,
						TEX_BOKEH1, TEX_BOKEH2, TEX_BOKEH3, mFbo, TEX_OUT,
						mData);
			} else {
				mFbo.bindTexture(TEX_OUT);
				mFilter.copy(mFbo.getTexture(TEX_SCENE));
			}

			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
			GLES20.glViewport(0, 0, mData.mViewWidth, mData.mViewHeight);
			if (mDivideScreen) {
				mFilter.setClipCoords(-1f, 1f, 0f, -1f);
				mFilter.copy(mFbo.getTexture(TEX_SCENE));
				mFilter.setClipCoords(0f, 1f, 1f, -1f);
				mFilter.copy(mFbo.getTexture(TEX_OUT));
				mFilter.setClipCoords(-1f, 1f, 1f, -1f);
			} else {
				mFilter.copy(mFbo.getTexture(TEX_OUT));
			}

			long time = SystemClock.uptimeMillis();
			if (mLastRenderTime != 0) {
				long diff = time - mLastRenderTime;
				mFps = 1000f / diff;
				mScene.animate();
			}
			mLastRenderTime = time;

		}

		@Override
		public void onSurfaceChanged(GL10 glUnused, int width, int height) {
			mData.mViewWidth = width;
			mData.mViewHeight = height;
			mData.mZNear = 1f;
			mData.mZFar = 101f;
			mData.mFovY = 45f;

			float ratio = (float) width / height;
			// Matrix.frustumM(mData.mProjM, 0, -ratio, ratio, -1, 1,
			// mData.mZNear, 20);
			GlslMatrix.setPerspectiveM(mData.mProjM, mData.mFovY, ratio,
					mData.mZNear, mData.mZFar);
			Matrix.setLookAtM(mData.mViewM, 0, 0f, 3f, -10f, 0f, 0f, 50f, 0f,
					1.0f, 0.0f);

			if (mResetFramebuffers) {
				mFbo.reset();
				mFboHalf.reset();
			}
			mResetFramebuffers = true;

			mFbo.init(width, height, 2, true);
			mFboHalf.init(width / 2, height / 2, 3, false);
		}

		@Override
		public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
			mFilter.init(GlslActivity.this);
			mResetFramebuffers = false;

			mSceneShader.setProgram(getString(R.string.shader_scene_vs),
					getString(R.string.shader_scene_fs));
			mSceneShader.addHandles("uMVMatrix", "uMVPMatrix", "uNormalMatrix",
					"aPosition", "aNormal", "aColor", "uLightCount", "uLights",
					"uCocScale", "uCocBias");

			int shaderIds[] = mSceneShader.getHandles("uMVMatrix",
					"uMVPMatrix", "uNormalMatrix", "aPosition", "aNormal",
					"aColor", "uLightCount", "uLights");
			mData.uMVMatrix = shaderIds[0];
			mData.uMVPMatrix = shaderIds[1];
			mData.uNormalMatrix = shaderIds[2];
			mData.aPosition = shaderIds[3];
			mData.aNormal = shaderIds[4];
			mData.aColor = shaderIds[5];
		}

		public void setPreferences(Context ctx, SharedPreferences preferences) {
			String key = ctx.getString(R.string.key_divide_screen);
			mDivideScreen = preferences.getBoolean(key, false);
			key = ctx.getString(R.string.key_lensblur_enable);
			mLensBlurEnabled = preferences.getBoolean(key, true);
			key = ctx.getString(R.string.key_lensblur_fstop);
			mData.mFStop = preferences.getFloat(key, 0);
			key = ctx.getString(R.string.key_lensblur_focal_plane);
			mData.mFocalPlane = preferences.getFloat(key, 0);
			key = ctx.getString(R.string.key_lensblur_steps);
			mData.mLensBlurRadius = (int) preferences.getFloat(key, 0);
			key = ctx.getString(R.string.key_lensblur_steps);
			mData.mLensBlurSteps = (int) preferences.getFloat(key, 0);
			mScene.setPreferences(ctx, preferences);
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
