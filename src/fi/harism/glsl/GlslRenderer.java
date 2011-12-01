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

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.content.SharedPreferences;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import fi.harism.glsl.scene.GlslLight;
import fi.harism.glsl.scene.GlslScene;
import fi.harism.glsl.scene.GlslShaderIds;

/**
 * Renderer class for handling GLSurfaceView.Renderer methods.
 */
public final class GlslRenderer implements GLSurfaceView.Renderer {

	public static final int SCENE_BOXES1 = 0;
	public static final int SCENE_BOXES2 = 1;

	private static final int TEX_IDX_SCENE = 0;
	private static final int TEX_IDX_OUT_1 = 1;
	private static final int TEX_IDX_OUT_2 = 2;

	private Activity mOwnerActivity;
	private long mRenderTime = 0;
	private long mAnimationTime = 0;
	private long mAnimationPauseTime = 0;
	private float mFps = 0f;

	private GlslScene mScene = new GlslScene();
	private GlslFilter mFilter = new GlslFilter();
	private GlslCamera mCamera = new GlslCamera();
	private GlslShaderIds mShaderIds = new GlslShaderIds();

	private boolean mResetFramebuffers;
	private GlslFbo mFbo = new GlslFbo();

	private boolean mAnimationPaused;
	private boolean mDivideScreen;
	private boolean mBloomEnabled;
	private boolean mLensBlurEnabled;

	private GlslShader mSceneShader = new GlslShader();
	private GlslShader mLightShader = new GlslShader();

	public GlslRenderer() {
		mRenderTime = SystemClock.uptimeMillis();
	}

	public float getFps() {
		return mFps;
	}

	@Override
	public void onDrawFrame(GL10 glUnused) {

		long lastRenderTime = mRenderTime;
		mRenderTime = SystemClock.uptimeMillis();
		mFps = 1000f / (mRenderTime - lastRenderTime);

		if (!mAnimationPaused) {
			mAnimationTime += mRenderTime - lastRenderTime;
		}
		mScene.animate(mAnimationTime);
		mScene.setMVP(mCamera.mViewM, mCamera.mProjM);

		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glFrontFace(GLES20.GL_CCW);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glDepthFunc(GLES20.GL_LEQUAL);

		int lightCount = mScene.getLightCount();
		float lightPositions[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		for (int i = 0; i < lightCount; ++i) {
			GlslLight light = mScene.getLight(i);
			light.getPosition(lightPositions, i * 3);
		}
		int shaderIds[] = mSceneShader.getHandles("uLightCount", "uLights",
				"uCocScale", "uCocBias");
		mSceneShader.useProgram();
		GLES20.glUniform1i(shaderIds[0], lightCount);
		GLES20.glUniform3fv(shaderIds[1], 4, lightPositions, 0);
		GLES20.glUniform1f(shaderIds[2], mCamera.mCocScale);
		GLES20.glUniform1f(shaderIds[3], mCamera.mCocBias);

		mFbo.bind();
		mFbo.bindTexture(TEX_IDX_SCENE);
		GLES20.glClearColor(0.2f, 0.3f, 0.5f, 1.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		mScene.draw(mShaderIds);

		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE);
		mLightShader.useProgram();
		GLES20.glUniformMatrix4fv(mShaderIds.uLightPMatrix, 1, false,
				mCamera.mProjM, 0);
		for (int i = 0; i < lightCount; ++i) {
			float pos[] = { 0, 0, 0 };
			GlslLight light = mScene.getLight(i);
			light.getPosition(pos, 0);
			light.render(mShaderIds, pos[0], pos[1], pos[2], 0.1f);
		}
		GLES20.glDisable(GLES20.GL_BLEND);

		int texIdxIn = TEX_IDX_SCENE;
		int texIdxOut = TEX_IDX_OUT_1;
		if (mBloomEnabled) {
			mFilter.bloom(mFbo.getTexture(texIdxIn), mFbo, texIdxOut, mCamera);
			texIdxIn = texIdxOut;
			texIdxOut = texIdxIn == TEX_IDX_OUT_1 ? TEX_IDX_OUT_2
					: TEX_IDX_OUT_1;
		}
		if (mLensBlurEnabled) {
			mFilter.lensBlur(mFbo.getTexture(texIdxIn), mFbo, texIdxOut,
					mCamera);
			texIdxIn = texIdxOut;
			texIdxOut = texIdxIn == TEX_IDX_OUT_1 ? TEX_IDX_OUT_2
					: TEX_IDX_OUT_1;
		}

		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glViewport(0, 0, mCamera.mViewWidth, mCamera.mViewHeight);
		if (mDivideScreen) {
			mFilter.setClipCoords(-1f, 1f, 0f, -1f);
			mFilter.copy(mFbo.getTexture(TEX_IDX_SCENE));
			mFilter.setClipCoords(0f, 1f, 1f, -1f);

			if (mCamera.mTouchDX == 0 && mCamera.mTouchDY == 0) {
				mFilter.copy(mFbo.getTexture(texIdxIn));
			} else {
				mFilter.displace(mFbo.getTexture(texIdxIn), mCamera);
			}
			mFilter.setClipCoords(-1f, 1f, 1f, -1f);
		} else {
			if (mCamera.mTouchDX == 0 && mCamera.mTouchDY == 0) {
				mFilter.copy(mFbo.getTexture(texIdxIn));
			} else {
				mFilter.displace(mFbo.getTexture(texIdxIn), mCamera);
			}
		}

	}

	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		mCamera.mViewWidth = width;
		mCamera.mViewHeight = height;
		float ratio = (float) width / height;

		mCamera.setProjectionM(ratio, 45f, .1f, 20f);
		mCamera.setViewM(0f, 3f, -10f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

		if (mResetFramebuffers) {
			mFbo.reset();
			mFilter.reset();
		}
		mResetFramebuffers = true;

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
		mFbo.init(width, height, 3, true);
		mFilter.init(width, height);
	}

	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		mResetFramebuffers = false;
		mFilter.init(mOwnerActivity);

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
		key = mOwnerActivity.getString(R.string.key_bloom_threshold);
		mCamera.mBloomThreshold = prefs.getFloat(key, 50f) / 100f;

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
		mSceneShader.addHandles("uMVMatrix", "uMVPMatrix", "uNormalMatrix",
				"aPosition", "aNormal", "aColor", "uLightCount", "uLights",
				"uCocScale", "uCocBias");

		int shaderIds[] = mSceneShader.getHandles("uMVMatrix", "uMVPMatrix",
				"uNormalMatrix", "aPosition", "aNormal", "aColor",
				"uLightCount", "uLights");
		mShaderIds.uMVMatrix = shaderIds[0];
		mShaderIds.uMVPMatrix = shaderIds[1];
		mShaderIds.uNormalMatrix = shaderIds[2];
		mShaderIds.aPosition = shaderIds[3];
		mShaderIds.aNormal = shaderIds[4];
		mShaderIds.aColor = shaderIds[5];

		mLightShader.setProgram(
				mOwnerActivity.getString(R.string.shader_light_vs),
				mOwnerActivity.getString(R.string.shader_light_fs));
		mLightShader.addHandles("uPMatrix", "aPosition", "aTexPosition");

		shaderIds = mLightShader.getHandles("uPMatrix", "aPosition",
				"aTexPosition");
		mShaderIds.uLightPMatrix = shaderIds[0];
		mShaderIds.aLightPosition = shaderIds[1];
		mShaderIds.aLightTexPosition = shaderIds[2];
	}

	/**
	 * Updates renderer drag position.
	 * 
	 * @param x1
	 *            First touch point x
	 * @param y1
	 *            First touch point y
	 * @param x2
	 *            Current touch point x
	 * @param y2
	 *            Current touch point y
	 */
	public void onTouch(float x1, float y1, float x2, float y2) {
		// On first call initiate paused state.
		if (!mAnimationPaused) {
			mAnimationPaused = true;
			mAnimationPauseTime = mAnimationTime;
		}

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
	}

	/**
	 * Method for releasing renderer from touch mode.
	 */
	public void onTouchRelease() {
		mAnimationPaused = false;
		new ReleaseDragTimer(mCamera, 300, 30).start();
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
