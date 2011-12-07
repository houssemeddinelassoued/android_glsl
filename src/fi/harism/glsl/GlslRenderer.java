/*
   Copyright 2011 Harri SmŒtt

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
	// Flag for whether shadows should be rendered.
	private boolean mShadowsEnabled;
	// Ambient, diffuse and specular factors for lightning.
	private float mAmbientFactor, mDiffuseFactor, mSpecularFactor;

	// Shader for rendering ambient lighted scene.
	private GlslShader mAmbientShader = new GlslShader();
	private GlslShaderIds mAmbientShaderIds = new GlslShaderIds();
	// Shader for rendering diffuse and specular lighted scene.
	private GlslShader mDiffuseSpecularShader = new GlslShader();
	private GlslShaderIds mDiffuseSpecularShaderIds = new GlslShaderIds();
	// Shader for rendering lights into scene.
	private GlslShader mLightShader = new GlslShader();
	// Shader for rendering shadow volumes.
	private GlslShader mShadowShader = new GlslShader();
	private GlslShaderIds mShadowShaderIds = new GlslShaderIds();

	/**
	 * Constructor.
	 */
	public GlslRenderer() {
		mRenderTime = SystemClock.uptimeMillis();
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
		// Setup GLES20 rendering options.
		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glDisable(GLES20.GL_STENCIL_TEST);
		GLES20.glFrontFace(GLES20.GL_CCW);
		GLES20.glDepthFunc(GLES20.GL_LEQUAL);

		// Bind scene texture and trigger clear before recalculating scene
		// matrices. Renderer emulates HDR rendering by reserving certain amount
		// of color space for HDR values. Currently this means that {R,G,B} -->
		// {R/3,G/3,B/3} where 'regular' colors are within [0, 1/3] range and
		// values [1, 3] map to higher [1/3, 3/3] range - and therefore clear
		// color is divided by 3.
		mFbo.bind();
		mFbo.bindTexture(TEX_IDX_SCENE);
		GLES20.glClearColor(0.2f / 3f, 0.3f / 3f, 0.5f / 3f, 1.0f);
		GLES20.glClear(GLES20.GL_STENCIL_BUFFER_BIT
				| GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

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
		mScene.updateMatrices(mCamera.mViewM, mCamera.mProjM);

		// Render ambient lighted scene. Ambient color covers all objects no
		// matter are they in shadow or not. Also depth information stored
		// during this pass is used for rendering shadow volumes.
		renderAmbient();
		// Render diffuse and specular lighted scene. This pass includes also
		// optional
		// shadow volume calculations.
		renderDiffuseSpecular();
		// Render light objects into scene.
		renderLightObjects();

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
		// Apply tone mapping before anti-aliasing.
		// TODO: It may be better if tone mapping was done as part of bloom and
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

		// Bind "screen FBO" into use. This is the final pass where FBOs are
		// rendered on screen.
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glViewport(0, 0, mCamera.mViewWidth, mCamera.mViewHeight);
		// If screen is divided copy original scene texture on left side.
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
		mFbo.init(width, height, 3, true, true);
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
		key = mOwnerActivity.getString(R.string.key_ambient_factor);
		mAmbientFactor = prefs.getFloat(key, 1f);
		key = mOwnerActivity.getString(R.string.key_diffuse_factor);
		mDiffuseFactor = prefs.getFloat(key, 1f);
		key = mOwnerActivity.getString(R.string.key_specular_factor);
		mSpecularFactor = prefs.getFloat(key, 1f);
		key = mOwnerActivity.getString(R.string.key_shadows_enable);
		mShadowsEnabled = prefs.getBoolean(key, false);

		// Initiate shaders for rendering scene.
		mAmbientShader.setProgram(
				mOwnerActivity.getString(R.string.shader_scene_vs),
				mOwnerActivity.getString(R.string.shader_scene_ambient_fs));
		// Get ids for uniforms/attributes needed for rendering.
		int shaderIds[] = mAmbientShader
				.getHandles("uModelViewM", "uModelViewProjM", "uNormalM",
						"aPosition", "aNormal", "aColor");
		mAmbientShaderIds.uModelViewM = shaderIds[0];
		mAmbientShaderIds.uModelViewProjM = shaderIds[1];
		mAmbientShaderIds.uNormalM = shaderIds[2];
		mAmbientShaderIds.aPosition = shaderIds[3];
		mAmbientShaderIds.aNormal = shaderIds[4];
		mAmbientShaderIds.aColor = shaderIds[5];

		key = mOwnerActivity.getString(R.string.key_light_model);
		int lightModel = Integer.parseInt(prefs.getString(key, "1"));
		switch (lightModel) {
		case 0:
			mDiffuseSpecularShader.setProgram(mOwnerActivity
					.getString(R.string.shader_scene_vs), mOwnerActivity
					.getString(R.string.shader_scene_blinn_phong_fs));
			break;
		case 1:
			mDiffuseSpecularShader.setProgram(
					mOwnerActivity.getString(R.string.shader_scene_vs),
					mOwnerActivity.getString(R.string.shader_scene_phong_fs));
		}

		// Get ids for uniforms/attributes needed for rendering.
		shaderIds = mDiffuseSpecularShader
				.getHandles("uModelViewM", "uModelViewProjM", "uNormalM",
						"aPosition", "aNormal", "aColor");
		mDiffuseSpecularShaderIds.uModelViewM = shaderIds[0];
		mDiffuseSpecularShaderIds.uModelViewProjM = shaderIds[1];
		mDiffuseSpecularShaderIds.uNormalM = shaderIds[2];
		mDiffuseSpecularShaderIds.aPosition = shaderIds[3];
		mDiffuseSpecularShaderIds.aNormal = shaderIds[4];
		mDiffuseSpecularShaderIds.aColor = shaderIds[5];

		// Instantiate light rendering shader.
		mLightShader.setProgram(
				mOwnerActivity.getString(R.string.shader_light_vs),
				mOwnerActivity.getString(R.string.shader_light_fs));
		// Instantiate shadow rendering shader.
		mShadowShader.setProgram(
				mOwnerActivity.getString(R.string.shader_shadow_volume_vs),
				mOwnerActivity.getString(R.string.shader_shadow_volume_fs));
		shaderIds = mShadowShader.getHandles("uModelViewM", "uModelViewProjM",
				"uNormalM", "aPosition", "aNormal");
		mShadowShaderIds.uModelViewM = shaderIds[0];
		mShadowShaderIds.uModelViewProjM = shaderIds[1];
		mShadowShaderIds.uNormalM = shaderIds[2];
		mShadowShaderIds.aPosition = shaderIds[3];
		mShadowShaderIds.aNormal = shaderIds[4];
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
	 * Renders ambient lighted scene. We calculate CoC values and store them
	 * into alpha during this pass. Make sure alpha values are unaffected after
	 * this pass until lens blur has been applied.
	 */
	private void renderAmbient() {
		// Initiate ambient shader with values that do not change.
		mAmbientShader.useProgram();
		GLES20.glUniform1f(mAmbientShader.getHandle("uAmbientFactor"),
				mAmbientFactor);
		GLES20.glUniform1f(mAmbientShader.getHandle("uAperture"),
				mCamera.mAperture);
		GLES20.glUniform1f(mAmbientShader.getHandle("uFocalLength"),
				mCamera.mFocalLength);
		GLES20.glUniform1f(mAmbientShader.getHandle("uPlaneInFocus"),
				mCamera.mPlaneInFocus);
		mScene.render(mAmbientShaderIds);
	}

	/**
	 * Renders diffuse and specular lighted scene plus applies optional shadow
	 * volumes.
	 */
	private void renderDiffuseSpecular() {
		GLES20.glDepthMask(false);
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE);
		GLES20.glEnable(GLES20.GL_STENCIL_TEST);

		for (GlslLight light : mScene.getLights()) {
			// Draw shadow volume into stencil buffer.
			if (mShadowsEnabled) {
				mShadowShader.useProgram();
				GLES20.glColorMask(false, false, false, false);
				GLES20.glDisable(GLES20.GL_CULL_FACE);
				GLES20.glStencilFunc(GLES20.GL_ALWAYS, 0x00, 0xFFFFFFFF);
				GLES20.glStencilOpSeparate(GLES20.GL_FRONT, GLES20.GL_KEEP,
						GLES20.GL_KEEP, GLES20.GL_INCR_WRAP);
				GLES20.glStencilOpSeparate(GLES20.GL_BACK, GLES20.GL_KEEP,
						GLES20.GL_KEEP, GLES20.GL_DECR_WRAP);
				GLES20.glUniformMatrix4fv(mShadowShader.getHandle("uProjM"), 1,
						false, mCamera.mProjM, 0);
				GLES20.glUniform3fv(mShadowShader.getHandle("uLightPosition"),
						1, light.getPosition(), 0);
				mScene.renderShadow(mShadowShaderIds);
				GLES20.glEnable(GLES20.GL_CULL_FACE);
			}
			// Initiate diffuse/specular shader with values that do not change
			// during actual rendering. We add these color values into scene
			// using blending during this pass.
			mDiffuseSpecularShader.useProgram();
			// Just in case disable writing to alpha channel in order to make
			// sure it remains unaffected.
			GLES20.glColorMask(true, true, true, false);
			// This is a minor optimization. Clear stencil buffer during
			// rendering the stencilled scene so we don't have to call
			// glClear(..) separarately. This shouldn't cause any visible
			// artifacts as we assume that stencil buffer is the same size as
			// render target texture.
			GLES20.glStencilFunc(GLES20.GL_EQUAL, 0x00, 0xFFFFFFFF);
			GLES20.glStencilOp(GLES20.GL_ZERO, GLES20.GL_ZERO, GLES20.GL_ZERO);
			GLES20.glUniform1f(
					mDiffuseSpecularShader.getHandle("uDiffuseFactor"),
					mDiffuseFactor);
			GLES20.glUniform1f(
					mDiffuseSpecularShader.getHandle("uSpecularFactor"),
					mSpecularFactor);
			GLES20.glUniform3fv(
					mDiffuseSpecularShader.getHandle("uLightPosition"), 1,
					light.getPosition(), 0);
			mScene.render(mDiffuseSpecularShaderIds);
		}

		GLES20.glDisable(GLES20.GL_BLEND);
		GLES20.glDisable(GLES20.GL_STENCIL_TEST);
		GLES20.glDepthMask(true);
		GLES20.glColorMask(true, true, true, true);
	}

	/**
	 * Renders light objects into the scene. Lights are rendered on top of
	 * already rendered scene and CoC values for light pixels are updated
	 * accordingly.
	 */
	private void renderLightObjects() {
		ByteBuffer b = ByteBuffer.allocateDirect(3 * 4);
		FloatBuffer buffer = b.order(ByteOrder.nativeOrder()).asFloatBuffer();

		mLightShader.useProgram();
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_ONE_MINUS_SRC_COLOR);
		GLES20.glUniformMatrix4fv(mLightShader.getHandle("uProjM"), 1, false,
				mCamera.mProjM, 0);
		GLES20.glUniform1f(mLightShader.getHandle("uPointRadius"), 0.1f);
		GLES20.glUniform1f(mLightShader.getHandle("uViewWidth"),
				mFbo.getWidth());
		GLES20.glUniform1f(mLightShader.getHandle("uAperture"),
				mCamera.mAperture);
		GLES20.glUniform1f(mLightShader.getHandle("uFocalLength"),
				mCamera.mFocalLength);
		GLES20.glUniform1f(mLightShader.getHandle("uPlaneInFocus"),
				mCamera.mPlaneInFocus);

		GLES20.glVertexAttribPointer(mLightShader.getHandle("aPosition"), 3,
				GLES20.GL_FLOAT, false, 0, buffer);
		GLES20.glEnableVertexAttribArray(mLightShader.getHandle("aPosition"));

		for (GlslLight light : mScene.getLights()) {
			buffer.position(0);
			buffer.put(light.getPosition(), 0, 3);
			buffer.position(0);
			GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
		}

		GLES20.glDisable(GLES20.GL_BLEND);
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
