/*
   Copyright 2011 Harri Smått

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package fi.harism.glsl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.content.SharedPreferences;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import fi.harism.glsl.scene.GlslLight;
import fi.harism.glsl.scene.GlslScene;
import fi.harism.glsl.scene.GlslShaderIds;

/**
 * Renderer class for handling GLSurfaceView.Renderer methods.
 */
public final class GlslRenderer implements GLSurfaceView.Renderer,
		View.OnTouchListener {

	// Internal scene ids (from preferences)
	private static final int SCENE_BOXES1 = 0;
	private static final int SCENE_BOXES2 = 1;

	// Texture indexes for mFbo.
	private static final int TEX_IDX_SCENE = 0;
	private static final int TEX_IDX_OUT_1 = 1;
	private static final int TEX_IDX_OUT_2 = 2;

	// Activity we belong to.
	private Activity mOwnerActivity;
	// FPS value.
	private float mFps = 0f;

	// Updated on every call to onDrawFrame(..).
	private long mRenderTime = 0;
	// Animation time.
	private long mAnimationTime = 0;
	// Pause time is used for stopping animation on touch events.
	private long mAnimationPauseTime = 0;

	// Scene instance.
	private GlslScene mScene = new GlslScene();
	// Filters instance.
	private GlslFilter mFilter = new GlslFilter();
	// Camera instance.
	private GlslCamera mCamera = new GlslCamera();
	// Shader ids instance.
	private GlslShaderIds mShaderIds = new GlslShaderIds();

	// FBO for rendering.
	private GlslFbo mFbo = new GlslFbo();

	// Flag for indicating animation is paused.
	private boolean mAnimationPaused;
	// Touch ACTION_DOWN coordinates.
	private float mTouchX, mTouchY;

	// Flag for whether screen should be divided.
	private boolean mDivideScreen;
	// Flag for whether bloom is enabled.
	private boolean mBloomEnabled;
	// Flag for whether lens blur is enabled.
	private boolean mLensBlurEnabled;
	// Flag for whether FXAA anti-aliasing is enabled.
	private boolean mFxaaEnabled;

	// Shader for rendering actual scene.
	private GlslShader mSceneShader = new GlslShader();
	// Shader for rendering lights into scene.
	private GlslShader mLightShader = new GlslShader();

	// Maximum number of lights.
	private static final int MAX_LIGHTS = 4;
	// Light size in bytes (3 floats * 4 bytes).
	private static final int LIGHT_BYTES = 3 * 4;
	// Buffer for storing light coordinates into.
	FloatBuffer mLightPositionBuffer;

	/**
	 * Constructor.
	 */
	public GlslRenderer() {
		mRenderTime = SystemClock.uptimeMillis();
		ByteBuffer buf = ByteBuffer.allocateDirect(LIGHT_BYTES * MAX_LIGHTS);
		mLightPositionBuffer = buf.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
	}

	/**
	 * Getter for FPS value.
	 * 
	 * @return FPS value.
	 */
	public float getFps() {
		return mFps;
	}

	@Override
	public void onDrawFrame(GL10 glUnused) {

		// Update render time and calculate FPS.
		long lastRenderTime = mRenderTime;
		mRenderTime = SystemClock.uptimeMillis();
		mFps = 1000f / (mRenderTime - lastRenderTime);

		// If animation is not paused, increment it with new/last render time
		// difference.
		if (!mAnimationPaused) {
			mAnimationTime += mRenderTime - lastRenderTime;
		}
		// Call scene for applying animation.
		mScene.animate(mAnimationTime);
		// Sets/calculates matrices for child objects etc.
		mScene.setMVP(mCamera.mViewM, mCamera.mProjM);

		// Setup GLES20 rendering options.
		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glFrontFace(GLES20.GL_CCW);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glDepthFunc(GLES20.GL_LEQUAL);

		// Fetch light positions into light position buffer.
		mLightPositionBuffer.position(0);
		int lightCount = mScene.getLightCount();
		float lightPosition[] = { 0, 0, 0 };
		for (int i = 0; i < lightCount; ++i) {
			GlslLight light = mScene.getLight(i);
			light.getPosition(lightPosition, 0);
			mLightPositionBuffer.put(lightPosition);
		}

		// Initiate scene shader with values that do not change.
		mSceneShader.useProgram();
		mLightPositionBuffer.position(0);
		GLES20.glUniform1i(mSceneShader.getHandle("uLightCount"), lightCount);
		GLES20.glUniform3fv(mSceneShader.getHandle("uLights"), lightCount,
				mLightPositionBuffer);
		GLES20.glUniform1f(mSceneShader.getHandle("uAperture"),
				mCamera.mAperture);
		GLES20.glUniform1f(mSceneShader.getHandle("uFocalLength"),
				mCamera.mFocalLength);
		GLES20.glUniform1f(mSceneShader.getHandle("uPlaneInFocus"),
				mCamera.mPlaneInFocus);

		// Bind scene texture, clear it and render the scene. Renderer emulates
		// HDR rendering by reserving certain amount of color space for HDR
		// values. Currently this means that { R, G, B } --> { R/3, G/3, B/3 }
		// and 'regular' colors are within [0, 1/3] range and values [1, 3] map
		// to higher [1/3, 3/3] range.
		mFbo.bind();
		mFbo.bindTexture(TEX_IDX_SCENE);
		GLES20.glClearColor(0.2f / 3f, 0.3f / 3f, 0.5f / 3f, 1.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		mScene.render(mShaderIds);

		// Draw lights into scene.
		mLightShader.useProgram();
		mLightPositionBuffer.position(0);
		GLES20.glUniformMatrix4fv(mLightShader.getHandle("uPMatrix"), 1, false,
				mCamera.mProjM, 0);
		GLES20.glVertexAttribPointer(mLightShader.getHandle("aPosition"), 3,
				GLES20.GL_FLOAT, false, 0, mLightPositionBuffer);
		GLES20.glEnableVertexAttribArray(mLightShader.getHandle("aPosition"));
		GLES20.glUniform1f(mLightShader.getHandle("uPointRadius"), 0.05f);
		GLES20.glUniform1f(mLightShader.getHandle("uViewWidth"),
				mFbo.getWidth());
		GLES20.glUniform1f(mLightShader.getHandle("uAperture"),
				mCamera.mAperture);
		GLES20.glUniform1f(mLightShader.getHandle("uFocalLength"),
				mCamera.mFocalLength);
		GLES20.glUniform1f(mLightShader.getHandle("uPlaneInFocus"),
				mCamera.mPlaneInFocus);
		GLES20.glDrawArrays(GLES20.GL_POINTS, 0, lightCount);

		// Variables for flipping input/output textures. Input texture should
		// always point to texture index used as source and output texture is
		// the one for rendering into.
		int texIdxIn = TEX_IDX_SCENE;
		int texIdxOut = TEX_IDX_OUT_1;
		// If lens blur is enabled.
		if (mLensBlurEnabled) {
			mFilter.lensBlur(mFbo.getTexture(texIdxIn), mFbo, texIdxOut,
					mCamera);
			// Swap texture in and out.
			texIdxIn = texIdxOut;
			texIdxOut = texIdxIn == TEX_IDX_OUT_1 ? TEX_IDX_OUT_2
					: TEX_IDX_OUT_1;
		}

		// If bloom is enabled.
		if (mBloomEnabled) {
			mFilter.bloom(mFbo.getTexture(texIdxIn), mFbo, texIdxOut);
			// Swap texture in and out.
			texIdxIn = texIdxOut;
			texIdxOut = texIdxIn == TEX_IDX_OUT_1 ? TEX_IDX_OUT_2
					: TEX_IDX_OUT_1;
		}
		// Apply tonemapping before anti-aliasing.
		// TODO: It may be better if tonemapping was done as part of bloom or
		// within else -clause if bloom is disabled.
		mFbo.bind();
		mFbo.bindTexture(texIdxOut);
		mFilter.tonemap(mFbo.getTexture(texIdxIn));
		// Swap texture in and out.
		texIdxIn = texIdxOut;
		texIdxOut = texIdxIn == TEX_IDX_OUT_1 ? TEX_IDX_OUT_2 : TEX_IDX_OUT_1;

		// If FXAA anti-aliasing is enabled.
		if (mFxaaEnabled) {
			mFbo.bind();
			mFbo.bindTexture(texIdxOut);
			mFilter.fxaa(mFbo.getTexture(texIdxIn), mFbo.getWidth(),
					mFbo.getHeight(), mFbo.getWidth(), mFbo.getHeight());
			// Swap texture in and out.
			texIdxIn = texIdxOut;
			texIdxOut = texIdxIn == TEX_IDX_OUT_1 ? TEX_IDX_OUT_2
					: TEX_IDX_OUT_1;
		}

		// Bind "screen FBO" into use.
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glViewport(0, 0, mCamera.mViewWidth, mCamera.mViewHeight);
		// If screen is divided copy original scene on left side.
		if (mDivideScreen) {
			mFilter.setClipCoords(-1f, 1f, 0f, -1f);
			mFilter.tonemap(mFbo.getTexture(TEX_IDX_SCENE));
			mFilter.setClipCoords(0f, 1f, 1f, -1f);
		}
		// Based on touch difference copy filtered scene to screen using plain
		// copy or displacement filter.
		if (mCamera.mTouchDX == 0 && mCamera.mTouchDY == 0) {
			mFilter.copy(mFbo.getTexture(texIdxIn));
		} else {
			mFilter.displace(mFbo.getTexture(texIdxIn), mCamera);
		}
		// Return filter clip bounds back to original values.
		mFilter.setClipCoords(-1f, 1f, 1f, -1f);
	}

	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		// Store view size.
		mCamera.mViewWidth = width;
		mCamera.mViewHeight = height;

		// Setup default projection and view matrices.
		float ratio = (float) width / height;
		mCamera.setProjectionM(ratio, 45f, .1f, 23f);
		mCamera.setViewM(0f, 3f, -10f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

		// Get shared preferences.
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(mOwnerActivity);
		String key = mOwnerActivity.getString(R.string.key_quality);
		int quality = Integer.parseInt(prefs.getString(key, "1"));
		switch (quality) {
		// High quality, do nothing.
		case 0:
			break;
		// Medium quality, use half sized textures.
		case 1:
			width /= 2;
			height /= 2;
			break;
		// Low quality, use third sized textures.
		case 2:
			width /= 3;
			height /= 3;
			break;
		}
		// Initialize mFbo and mFilter for rendering.
		mFbo.init(width, height, 3, true);
		mFilter.init(width, height);
	}

	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		// Initializes filter shaders.
		mFilter.init(mOwnerActivity);

		// Read preferences.
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(mOwnerActivity);
		String key = mOwnerActivity.getString(R.string.key_divide_screen);
		mDivideScreen = prefs.getBoolean(key, false);
		key = mOwnerActivity.getString(R.string.key_lensblur_enable);
		mLensBlurEnabled = prefs.getBoolean(key, true);

		key = mOwnerActivity.getString(R.string.key_lensblur_steps);
		mCamera.mBlurSteps = (int) prefs.getFloat(key, 0);
		key = mOwnerActivity.getString(R.string.key_lensblur_fstop);
		float fStop = prefs.getFloat(key, 0);
		key = mOwnerActivity.getString(R.string.key_lensblur_focal_plane);
		float focalPlane = prefs.getFloat(key, 0) / 100f;
		mCamera.setLensBlur(fStop, focalPlane);

		key = mOwnerActivity.getString(R.string.key_bloom_enable);
		mBloomEnabled = prefs.getBoolean(key, true);
		key = mOwnerActivity.getString(R.string.key_fxaa_enable);
		mFxaaEnabled = prefs.getBoolean(key, true);

		key = mOwnerActivity.getString(R.string.key_light_count);
		int lightCount = (int) prefs.getFloat(key, 1);
		key = mOwnerActivity.getString(R.string.key_scene);
		int scene = Integer.parseInt(prefs.getString(key, "0"));
		switch (scene) {
		case SCENE_BOXES1:
			mScene.initSceneBoxes1(mCamera, lightCount);
			break;
		case SCENE_BOXES2:
			mScene.initSceneBoxes2(mCamera, lightCount);
			break;
		}

		// Initiate scene shader.
		key = mOwnerActivity.getString(R.string.key_light_model);
		int lightModel = Integer.parseInt(prefs.getString(key, "1"));
		switch (lightModel) {
		case 0:
			mSceneShader.setProgram(mOwnerActivity
					.getString(R.string.shader_scene_vs), mOwnerActivity
					.getString(R.string.shader_scene_blinn_phong_fs));
			break;
		case 1:
			mSceneShader.setProgram(
					mOwnerActivity.getString(R.string.shader_scene_vs),
					mOwnerActivity.getString(R.string.shader_scene_phong_fs));
		}

		// Find/add required uniforms/attributes into mSceneShader.
		mSceneShader.addHandles("uMVMatrix", "uMVPMatrix", "uNormalMatrix",
				"aPosition", "aNormal", "aColor", "uLightCount", "uLights",
				"uAperture", "uFocalLength", "uPlaneInFocus");
		// Get ids for uniforms/attributes needed for rendering.
		int shaderIds[] = mSceneShader.getHandles("uMVMatrix", "uMVPMatrix",
				"uNormalMatrix", "aPosition", "aNormal", "aColor",
				"uLightCount", "uLights");
		mShaderIds.uMVMatrix = shaderIds[0];
		mShaderIds.uMVPMatrix = shaderIds[1];
		mShaderIds.uNormalMatrix = shaderIds[2];
		mShaderIds.aPosition = shaderIds[3];
		mShaderIds.aNormal = shaderIds[4];
		mShaderIds.aColor = shaderIds[5];

		// Instantiate light rendering shader.
		mLightShader.setProgram(
				mOwnerActivity.getString(R.string.shader_light_vs),
				mOwnerActivity.getString(R.string.shader_light_fs));
		mLightShader.addHandles("uPMatrix", "uPointRadius", "uViewWidth",
				"aPosition", "uAperture", "uFocalLength", "uPlaneInFocus");
	}

	@Override
	public boolean onTouch(View view, MotionEvent me) {
		switch (me.getAction()) {
		case MotionEvent.ACTION_DOWN:
			// On touch start stop animation and store touch position.
			mTouchX = me.getX();
			mTouchY = me.getY();
			mAnimationPaused = true;
			mAnimationPauseTime = mAnimationTime;
			return true;
		case MotionEvent.ACTION_MOVE:
			// (x1, y1) = start position, (x2, y2) = current position.
			float x1 = mTouchX;
			float y1 = mTouchY;
			float x2 = me.getX();
			float y2 = me.getY();

			// Readjust animation time based on drag position.
			float ratio = (float) mCamera.mViewWidth / mCamera.mViewHeight;
			mAnimationTime = (long) (mAnimationPauseTime + 5000 * ratio
					* ((x2 - x1) / mCamera.mViewWidth));

			// Map touch coordinates into texture coordinates.
			x1 /= mCamera.mViewWidth;
			x2 /= mCamera.mViewWidth;
			y1 = 1f - y1 / mCamera.mViewHeight;
			y2 = 1f - y2 / mCamera.mViewHeight;
			mCamera.mTouchX = x1;
			mCamera.mTouchY = y1;
			mCamera.mTouchDX = x1 - x2;
			mCamera.mTouchDY = y1 - y2;
			return true;
		case MotionEvent.ACTION_UP:
			// On touch end release animation and start release drag animation.
			mAnimationPaused = false;
			new ReleaseDragTimer(mCamera, 300, 30).start();
			return true;
		default:
			return false;
		}
	}

	/**
	 * Stores owner Activity for reading application preferences and accessing
	 * application Context based strings etc. This method should be called as
	 * soon as GlslRenderer has been created or retrieved using
	 * Activity.getLastNonConfigurationInstance().
	 * 
	 * @param activity
	 *            Owner Activity
	 */
	public void setOwnerActivity(Activity activity) {
		mOwnerActivity = activity;
	}

	/**
	 * Helper class to enable smooth transition from drag position.
	 */
	private class ReleaseDragTimer extends CountDownTimer {
		private GlslCamera mReleasedCamera;
		private long mTotalLength;

		/**
		 * Constructor.
		 * 
		 * @param camera
		 *            Camera for releasing
		 * @param millisInFuture
		 *            Length of transition
		 * @param countDownInterval
		 *            Transition interval
		 */
		public ReleaseDragTimer(GlslCamera camera, long millisInFuture,
				long countDownInterval) {
			super(millisInFuture, countDownInterval);
			mReleasedCamera = camera;
			mTotalLength = millisInFuture;
		}

		@Override
		public void onFinish() {
			mReleasedCamera.mTouchX = mReleasedCamera.mTouchY = mReleasedCamera.mTouchDX = mReleasedCamera.mTouchDY = 0f;
		}

		@Override
		public void onTick(long millisUntilFinished) {
			float c = (float) millisUntilFinished / mTotalLength;
			mReleasedCamera.mTouchDX *= c;
			mReleasedCamera.mTouchDY *= c;
		}

	}

}
