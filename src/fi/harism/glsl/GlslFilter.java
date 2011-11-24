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

import android.content.Context;
import android.opengl.GLES20;
import android.os.SystemClock;

/**
 * Class for handling 2d filter shaders.
 */
public class GlslFilter {

	// Named texture indexes.
	private static final int TEX_IDX_1 = 0;
	private static final int TEX_IDX_2 = 1;
	private static final int TEX_IDX_3 = 2;

	// Shader instances.
	private GlslShader mCopy = new GlslShader();
	private GlslShader mDisplace = new GlslShader();
	private GlslShader mBloomPass1 = new GlslShader();
	private GlslShader mBloomPass2 = new GlslShader();
	private GlslShader mBloomPass3 = new GlslShader();
	private GlslShader mLensBlurPass1 = new GlslShader();
	private GlslShader mLensBlurPass2 = new GlslShader();
	private GlslShader mLensBlurPass3 = new GlslShader();
	private GlslShader mLensBlurPass4 = new GlslShader();
	private GlslShader mLensBlurPass5 = new GlslShader();

	// Half sized FBO
	private GlslFbo mFboHalf = new GlslFbo();
	// Quarter sized FBO
	private GlslFbo mFboQuarter = new GlslFbo();

	// Buffer for 2d coordinates.
	private FloatBuffer mTriangleVertices;
	private static final float[] mCoords = { -1f, 1f, -1f, -1f, 1f, 1f, 1f, -1f };

	public GlslFilter() {
		ByteBuffer buffer = ByteBuffer.allocateDirect(mCoords.length * 4);
		mTriangleVertices = buffer.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		mTriangleVertices.position(0);
		mTriangleVertices.put(mCoords);
	}

	/**
	 * Bloom filter.
	 * 
	 * @param texSrc
	 *            Source texture id.
	 * @param fboOut
	 *            Output FBO.
	 * @param idxOut
	 *            Output FBO texture index.
	 * @param camera
	 *            Camera instance.
	 */
	public void bloom(int texSrc, GlslFbo fboOut, int idxOut, GlslCamera camera) {

		float ratioX = 1f;
		float ratioY = (float) mFboHalf.getWidth() / mFboHalf.getHeight();
		if (ratioY > ratioX) {
			ratioX = (float) mFboHalf.getHeight() / mFboHalf.getWidth();
			ratioY = 1f;
		}

		float steps = 5;
		float dx = ratioX / (50f * steps);
		float dy = ratioY / (50f * steps);

		// First pass reads color values exceeding given threshold into
		// TEX_IDX_1 texture.
		mFboQuarter.bind();
		mFboQuarter.bindTexture(TEX_IDX_1);
		GLES20.glUseProgram(mBloomPass1.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texSrc);
		GLES20.glUniform1f(mBloomPass1.getHandle("uThreshold"),
				camera.mBloomThreshold);
		drawRect(mBloomPass1.getHandle("aPosition"));

		// Second pass blurs TEX_IDX_1 horizontally.
		mFboQuarter.bindTexture(TEX_IDX_2);
		GLES20.glUseProgram(mBloomPass2.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
				mFboQuarter.getTexture(TEX_IDX_1));
		GLES20.glUniform2f(mBloomPass2.getHandle("uOffset"), dx, 0f);
		GLES20.glUniform1f(mBloomPass2.getHandle("uSteps"), steps);
		drawRect(mBloomPass2.getHandle("aPosition"));

		// Third pass blurs TEX_IDX_2 vertically.
		mFboQuarter.bindTexture(TEX_IDX_1);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
				mFboQuarter.getTexture(TEX_IDX_2));
		GLES20.glUniform2f(mBloomPass2.getHandle("uOffset"), 0f, dy);
		drawRect(mBloomPass2.getHandle("aPosition"));

		// Fourth pass combines source texture and calculated bloom texture into
		// given output texture.
		fboOut.bind();
		fboOut.bindTexture(idxOut);
		GLES20.glUseProgram(mBloomPass3.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texSrc);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
				mFboQuarter.getTexture(TEX_IDX_1));
		GLES20.glUniform1i(mBloomPass3.getHandle("sTexture1"), 1);
		drawRect(mBloomPass3.getHandle("aPosition"));
	}

	/**
	 * Copy filter copies given texture id as a source into currently binded
	 * FBO.
	 * 
	 * @param src
	 *            Source texture id.
	 */
	public void copy(int src) {
		GLES20.glUseProgram(mCopy.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, src);
		drawRect(mCopy.getHandle("aPosition"));
	}

	/**
	 * Renders a displaced image of source texture into currently active FBO.
	 * 
	 * @param texSrc
	 *            Source texture id.
	 * @param camera
	 *            Camera for retrieving displacement values.
	 */
	public void displace(int texSrc, GlslCamera camera) {
		GLES20.glUseProgram(mDisplace.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texSrc);
		GLES20.glUniform2f(mDisplace.getHandle("uPosition"), camera.mTouchX,
				camera.mTouchY);
		GLES20.glUniform2f(mDisplace.getHandle("uDiff"), camera.mTouchDX,
				camera.mTouchDY);
		drawRect(mDisplace.getHandle("aPosition"));
	}

	/**
	 * Initialized shaders.
	 * 
	 * @param ctx
	 *            Context to read shader sources from.
	 */
	public void init(Context ctx) {
		mCopy.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_copy_fs));
		mCopy.addHandles("aPosition", "sTexture0");

		mDisplace.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_displace_fs));
		mDisplace.addHandles("aPosition", "sTexture0", "uPosition", "uDiff");

		mBloomPass1.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_bloom_pass1_fs));
		mBloomPass1.addHandles("aPosition", "sTexture0", "uThreshold");
		mBloomPass2.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_bloom_pass2_fs));
		mBloomPass2.addHandles("aPosition", "sTexture0", "uOffset", "uSteps");
		mBloomPass3.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_bloom_pass3_fs));
		mBloomPass3.addHandles("aPosition", "sTexture0", "sTexture1");

		mLensBlurPass1.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_lensblur_pass1_fs));
		mLensBlurPass1.addHandles("aPosition", "sTexture0");
		mLensBlurPass2.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_lensblur_pass2_fs));
		mLensBlurPass2
				.addHandles("aPosition", "uSteps", "sTexture0", "uDelta0");
		mLensBlurPass3.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_lensblur_pass3_fs));
		mLensBlurPass3.addHandles("aPosition", "uSteps", "sTexture0",
				"sTexture1", "uDelta0", "uDelta1");
		mLensBlurPass4.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_lensblur_pass4_fs));
		mLensBlurPass4.addHandles("aPosition", "uSteps", "sTexture0",
				"sTexture1", "uDelta0", "uDelta1");
		mLensBlurPass5.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_lensblur_pass5_fs));
		mLensBlurPass5.addHandles("aPosition", "sTextureSrc", "sTextureBokeh",
				"sTextureBokehHdr", "sTexture1");
	}

	/**
	 * Initializes internal FBOs.
	 * 
	 * @param width
	 *            parent width
	 * @param height
	 *            parent height
	 */
	public void init(int width, int height) {
		mFboHalf.init(width / 2, height / 2, 3);
		mFboQuarter.init(width / 4, height / 4, 3);
	}

	/**
	 * Lens blur filter.
	 * 
	 * @param texSrc
	 *            Source texture id.
	 * @param fboOut
	 *            Output FBO.
	 * @param idxOut
	 *            Output FBO texture index.
	 * @param camera
	 *            Camera instance.
	 */
	public void lensBlur(int texSrc, GlslFbo fboOut, int idxOut,
			GlslCamera camera) {

		float ratioX = 1f;
		float ratioY = (float) mFboHalf.getWidth() / mFboHalf.getHeight();
		if (ratioY > ratioX) {
			ratioX = (float) mFboHalf.getHeight() / mFboHalf.getWidth();
			ratioY = 1f;
		}

		float angle = (float) (Math.PI * (SystemClock.uptimeMillis() % 10000) / 5000);
		float stepRadius = Math.min(camera.mCocRadius, 50f) / camera.mBlurSteps;

		float[][] dir = new float[3][2];
		for (int i = 0; i < 3; i++) {
			double a = angle + i * Math.PI * 2 / 3;
			dir[i][0] = (float) (stepRadius * Math.sin(a) * ratioX / 1000f);
			dir[i][1] = (float) (stepRadius * Math.cos(a) * ratioY / 1000f);
		}

		mFboHalf.bind();
		mFboHalf.bindTexture(TEX_IDX_3);
		GLES20.glUseProgram(mLensBlurPass1.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texSrc);
		drawRect(mLensBlurPass1.getHandle("aPosition"));

		mFboHalf.bindTexture(TEX_IDX_1);
		GLES20.glUseProgram(mLensBlurPass2.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
				mFboHalf.getTexture(TEX_IDX_3));
		GLES20.glUniform1f(mLensBlurPass2.getHandle("uSteps"),
				camera.mBlurSteps);
		GLES20.glUniform2fv(mLensBlurPass2.getHandle("uDelta0"), 1, dir[0], 0);
		drawRect(mLensBlurPass2.getHandle("aPosition"));

		mFboHalf.bindTexture(TEX_IDX_2);
		GLES20.glUseProgram(mLensBlurPass3.getProgram());
		// GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		// GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
		// mFboHalf.getTexture(TEX_IDX_3));
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
				mFboHalf.getTexture(TEX_IDX_1));
		GLES20.glUniform1i(mLensBlurPass3.getHandle("sTexture1"), 1);
		GLES20.glUniform1f(mLensBlurPass3.getHandle("uSteps"),
				camera.mBlurSteps);
		GLES20.glUniform2fv(mLensBlurPass3.getHandle("uDelta0"), 1, dir[0], 0);
		GLES20.glUniform2fv(mLensBlurPass3.getHandle("uDelta1"), 1, dir[1], 0);
		drawRect(mLensBlurPass3.getHandle("aPosition"));

		mFboHalf.bindTexture(TEX_IDX_3);
		GLES20.glUseProgram(mLensBlurPass4.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
				mFboHalf.getTexture(TEX_IDX_2));
		// GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		// GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
		// mFboHalf.getTexture(TEX_IDX_1));
		// GLES20.glUniform1i(mLensBlurPass3.getHandle("sTexture1"), 1);
		GLES20.glUniform1i(mLensBlurPass4.getHandle("sTexture1"), 1);
		GLES20.glUniform1f(mLensBlurPass4.getHandle("uSteps"),
				camera.mBlurSteps);
		GLES20.glUniform2fv(mLensBlurPass4.getHandle("uDelta0"), 1, dir[2], 0);
		GLES20.glUniform2fv(mLensBlurPass4.getHandle("uDelta1"), 1, dir[1], 0);
		drawRect(mLensBlurPass4.getHandle("aPosition"));

		fboOut.bind();
		fboOut.bindTexture(idxOut);
		GLES20.glUseProgram(mLensBlurPass5.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texSrc);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
				mFboHalf.getTexture(TEX_IDX_3));
		GLES20.glUniform1i(mLensBlurPass5.getHandle("sTextureBokeh"), 1);
		drawRect(mLensBlurPass5.getHandle("aPosition"));
	}

	/**
	 * Resets internal FBOs.
	 */
	public void reset() {
		mFboHalf.reset();
		mFboQuarter.reset();
	}

	/**
	 * Updates clipping coordinates filters are applied into. Values are between
	 * [-1, 1], where (-1, 1) is top left corner and (1, -1) lower right corner.
	 * 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 */
	public void setClipCoords(float x1, float y1, float x2, float y2) {
		mTriangleVertices.put(0, x1);
		mTriangleVertices.put(1, y1);
		mTriangleVertices.put(2, x1);
		mTriangleVertices.put(3, y2);
		mTriangleVertices.put(4, x2);
		mTriangleVertices.put(5, y1);
		mTriangleVertices.put(6, x2);
		mTriangleVertices.put(7, y2);
	}

	/**
	 * Private helper method to execute filters.
	 * 
	 * @param positionHandle
	 *            Position attribute id to feed current vertex shader with.
	 */
	private void drawRect(int positionHandle) {
		mTriangleVertices.position(0);
		GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false,
				2 * 4, mTriangleVertices);
		GLES20.glEnableVertexAttribArray(positionHandle);

		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	}

}
