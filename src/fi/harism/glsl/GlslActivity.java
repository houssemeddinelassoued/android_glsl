package fi.harism.glsl;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import fi.harism.glsl.scene.GlslLight;
import fi.harism.glsl.scene.GlslScene;
import fi.harism.glsl.scene.GlslShaderIds;

public final class GlslActivity extends Activity {

	private MusicPlayer mMusicPlayer;
	private Renderer mRenderer;
	private SurfaceView mSurfaceView;

	private Timer mFpsTimer;
	private Runnable mFpsRunnable;

	private String mAppName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getLastNonConfigurationInstance() != null) {
			GlslActivity self = (GlslActivity) getLastNonConfigurationInstance();
			mRenderer = self.mRenderer;
			mMusicPlayer = self.mMusicPlayer;
		} else {
			mRenderer = new Renderer();
			mMusicPlayer = new MusicPlayer(500, 50);
		}

		mSurfaceView = new SurfaceView(this);
		mSurfaceView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR
				| GLSurfaceView.DEBUG_LOG_GL_CALLS);
		mSurfaceView.setEGLContextClientVersion(2);
		mSurfaceView.setRenderer(mRenderer);
		mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

		mAppName = getString(R.string.app_name);
		mFpsRunnable = new FpsRunnable();

		PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		ViewGroup container = (ViewGroup) findViewById(R.id.layout_container);
		container.addView(mSurfaceView);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_about:
			Dialog dlg = new Dialog(this);
			dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dlg.setContentView(R.layout.about);
			dlg.show();
			return true;
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

		mMusicPlayer.stop();
	}

	@Override
	public void onResume() {
		super.onResume();
		mSurfaceView.onResume();
		mFpsTimer = new Timer();

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		mRenderer.setPreferences(this, prefs);

		String key = getString(R.string.key_show_title);
		if (prefs.getBoolean(key, true)) {
			findViewById(R.id.layout_header).setVisibility(View.VISIBLE);
			mFpsTimer.scheduleAtFixedRate(new FpsTimerTask(), 0, 500);
		} else {
			findViewById(R.id.layout_header).setVisibility(View.GONE);
		}

		key = getString(R.string.key_play_music);
		if (prefs.getBoolean(key, true)) {
			try {
				mMusicPlayer.start(getResources().openRawResourceFd(
						R.raw.mosaik_01_leandi).getFileDescriptor());
			} catch (IOException ex) {
				mMusicPlayer.stop();
			}
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return this;
	}

	private class FpsRunnable implements Runnable {
		@Override
		public void run() {
			String fps = Float.toString(mRenderer.getFps());
			int separator = fps.indexOf('.');
			if (separator == -1) {
				separator = fps.indexOf(',');
			}
			if (separator != -1) {
				fps = fps.substring(0, separator + 2);
			}
			TextView tv = (TextView) findViewById(R.id.layout_title);
			tv.setText(mAppName + " (" + fps + "fps)");
		}
	}

	private class FpsTimerTask extends TimerTask {
		@Override
		public void run() {
			runOnUiThread(mFpsRunnable);
		}
	}

	private final class MusicPlayer implements MediaPlayer.OnPreparedListener {

		private MediaPlayer mMediaPlayer;
		private int mMediaPlayerPosition;

		private int mFadeTime;
		private int mFadeInterval;

		public MusicPlayer(int fadeTime, int fadeInterval) {
			mFadeTime = fadeTime;
			mFadeInterval = fadeInterval;
		}

		@Override
		public void onPrepared(MediaPlayer mp) {
			mMediaPlayer.setVolume(0f, 0f);
			mMediaPlayer.setLooping(true);
			mMediaPlayer.seekTo(mMediaPlayerPosition);
			mMediaPlayer.start();
			new FadeInTimer(mMediaPlayer, mFadeTime, mFadeInterval).start();
		}

		public void start(FileDescriptor fileDescriptor) throws IOException {
			if (mMediaPlayer != null) {
				new FadeOutTimer(mMediaPlayer, mFadeTime, mFadeInterval)
						.start();
			}
			mMediaPlayer = new MediaPlayer();
			mMediaPlayer.setOnPreparedListener(this);
			mMediaPlayer.setDataSource(fileDescriptor);
			mMediaPlayer.prepareAsync();
		}

		public void stop() {
			if (mMediaPlayer != null) {
				mMediaPlayerPosition = mMediaPlayer.getCurrentPosition()
						+ mFadeTime;
				new FadeOutTimer(mMediaPlayer, mFadeTime, mFadeInterval)
						.start();
				mMediaPlayer = null;
			}
		}

		private class FadeInTimer extends CountDownTimer {

			private MediaPlayer mMediaPlayerIn;
			private long mFadeTime;

			public FadeInTimer(MediaPlayer mediaPlayerIn, long fadeTime,
					long fadeInterval) {
				super(fadeTime, fadeInterval);
				mMediaPlayerIn = mediaPlayerIn;
				mFadeTime = fadeTime;
			}

			@Override
			public void onFinish() {
				mMediaPlayerIn.setVolume(1f, 1f);
			}

			@Override
			public void onTick(long millisUntilFinish) {
				float v = (float) millisUntilFinish / mFadeTime;
				mMediaPlayerIn.setVolume(1f - v, 1f - v);
			}

		}

		private class FadeOutTimer extends CountDownTimer {

			private MediaPlayer mMediaPlayerOut;
			private long mFadeTime;

			public FadeOutTimer(MediaPlayer mediaPlayerOut, long fadeTime,
					long fadeInterval) {
				super(fadeTime, fadeInterval);
				mMediaPlayerOut = mediaPlayerOut;
				mFadeTime = fadeTime;
			}

			@Override
			public void onFinish() {
				mMediaPlayerOut.release();
				mMediaPlayerOut = null;
			}

			@Override
			public void onTick(long millisUntilFinish) {
				float v = (float) millisUntilFinish / mFadeTime;
				mMediaPlayerOut.setVolume(v, v);
			}

		}
	}

	private final class Renderer implements GLSurfaceView.Renderer {

		public static final int SCENE_BOXES1 = 0;
		public static final int SCENE_BOXES2 = 1;

		private static final int TEX_OUT = 0;
		private static final int TEX_SCENE = 1;

		private long mRenderTime = 0;
		private long mLastRenderTime = 0;

		private GlslScene mScene = new GlslScene();
		private GlslFilter mFilter = new GlslFilter();
		private GlslCamera mCamera = new GlslCamera();
		private GlslShaderIds mShaderIds = new GlslShaderIds();

		private boolean mResetFramebuffers;
		private GlslFbo mFbo = new GlslFbo();

		private boolean mDivideScreen;
		private boolean mBloomEnabled;
		private boolean mLensBlurEnabled;

		private GlslShader mSceneShader = new GlslShader();

		public Renderer() {
			mRenderTime = mLastRenderTime = SystemClock.uptimeMillis();
		}

		public float getFps() {
			return 1000f / (mRenderTime - mLastRenderTime);
		}

		@Override
		public void onDrawFrame(GL10 glUnused) {

			mScene.setMVP(mCamera.mViewM, mCamera.mProjM);

			GLES20.glEnable(GLES20.GL_CULL_FACE);
			GLES20.glFrontFace(GLES20.GL_CCW);
			GLES20.glEnable(GLES20.GL_DEPTH_TEST);
			GLES20.glDepthFunc(GLES20.GL_LEQUAL);

			int lightCount = mScene.getLightCount();
			float lightPositions[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
					0, 0, 0 };
			for (int i = 0; i < lightCount; ++i) {
				GlslLight light = mScene.getLight(i);
				light.getPosition(lightPositions, i * 4);
			}
			int shaderIds[] = mSceneShader.getHandles("uLightCount", "uLights",
					"uCocScale", "uCocBias");
			GLES20.glUseProgram(mSceneShader.getProgram());
			GLES20.glUniform1i(shaderIds[0], lightCount);
			GLES20.glUniform4fv(shaderIds[1], 4, lightPositions, 0);

			GLES20.glUniform1f(shaderIds[2], mCamera.mCocScale);
			GLES20.glUniform1f(shaderIds[3], mCamera.mCocBias);

			mFbo.bind();
			mFbo.bindTexture(TEX_SCENE);
			GLES20.glClearColor(0.2f, 0.3f, 0.5f, 1.0f);
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT
					| GLES20.GL_DEPTH_BUFFER_BIT);
			mScene.draw(mShaderIds);

			if (mBloomEnabled) {
				mFilter.bloom(mFbo.getTexture(TEX_SCENE), mFbo, TEX_OUT,
						mCamera);
			}

			if (mLensBlurEnabled) {
				mFilter.lensBlur(mFbo.getTexture(TEX_SCENE), mFbo, TEX_OUT,
						mCamera);
			}

			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
			GLES20.glViewport(0, 0, mCamera.mViewWidth, mCamera.mViewHeight);
			if (mDivideScreen) {
				mFilter.setClipCoords(-1f, 1f, 0f, -1f);
				mFilter.copy(mFbo.getTexture(TEX_SCENE));
				mFilter.setClipCoords(0f, 1f, 1f, -1f);
				mFilter.copy(mFbo.getTexture(TEX_OUT));
				mFilter.setClipCoords(-1f, 1f, 1f, -1f);
			} else {
				mFilter.copy(mFbo.getTexture(TEX_OUT));
			}

			mLastRenderTime = mRenderTime;
			mRenderTime = SystemClock.uptimeMillis();
			mScene.animate();
		}

		@Override
		public void onSurfaceChanged(GL10 glUnused, int width, int height) {
			mCamera.mViewWidth = width;
			mCamera.mViewHeight = height;
			float ratio = (float) width / height;

			mCamera.setProjectionM(ratio, 45f, 1f, 101f);
			mCamera.setViewM(0f, 3f, -10f, 0f, 0f, 50f, 0f, 1.0f, 0.0f);

			if (mResetFramebuffers) {
				mFbo.reset();
				mFilter.reset();
			}
			mResetFramebuffers = true;

			mFbo.init(width, height, 2, true);
			mFilter.init(width, height);
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
			mShaderIds.uMVMatrix = shaderIds[0];
			mShaderIds.uMVPMatrix = shaderIds[1];
			mShaderIds.uNormalMatrix = shaderIds[2];
			mShaderIds.aPosition = shaderIds[3];
			mShaderIds.aNormal = shaderIds[4];
			mShaderIds.aColor = shaderIds[5];
		}

		public void setPreferences(Context ctx, SharedPreferences preferences) {
			String key = ctx.getString(R.string.key_divide_screen);
			mDivideScreen = preferences.getBoolean(key, false);
			key = ctx.getString(R.string.key_lensblur_enable);
			mLensBlurEnabled = preferences.getBoolean(key, true);

			key = ctx.getString(R.string.key_lensblur_steps);
			mCamera.mBlurSteps = (int) preferences.getFloat(key, 0);
			key = ctx.getString(R.string.key_lensblur_fstop);
			float fStop = preferences.getFloat(key, 0);
			key = ctx.getString(R.string.key_lensblur_focal_plane);
			float focalPlane = preferences.getFloat(key, 0) / 100f;
			mCamera.setLensBlur(fStop, focalPlane);

			key = ctx.getString(R.string.key_bloom_enable);
			mBloomEnabled = preferences.getBoolean(key, true);
			key = ctx.getString(R.string.key_bloom_threshold);
			mCamera.mBloomThreshold = preferences.getFloat(key, 50f) / 100f;

			key = ctx.getString(R.string.key_light_count);
			int lightCount = (int) preferences.getFloat(key, 1);
			key = ctx.getString(R.string.key_scene);
			int scene = Integer.parseInt(preferences.getString(key, "0"));
			switch (scene) {
			case SCENE_BOXES1:
				mScene.initSceneBoxes1(lightCount);
				break;
			case SCENE_BOXES2:
				mScene.initSceneBoxes2(lightCount);
				break;
			}
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
