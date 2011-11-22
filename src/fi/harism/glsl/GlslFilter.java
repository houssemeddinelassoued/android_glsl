package fi.harism.glsl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.content.Context;
import android.opengl.GLES20;
import android.os.SystemClock;

public class GlslFilter {

	private static final int TEX_IDX_1 = 0;
	private static final int TEX_IDX_2 = 1;
	private static final int TEX_IDX_3 = 2;

	private GlslShader mCopy = new GlslShader();
	private GlslShader mBloomPass1 = new GlslShader();
	private GlslShader mBloomPass2 = new GlslShader();
	private GlslShader mBloomPass3 = new GlslShader();
	private GlslShader mLensBlurPass1 = new GlslShader();
	private GlslShader mLensBlurPass2 = new GlslShader();
	private GlslShader mLensBlurPass3 = new GlslShader();
	private GlslShader mLensBlurPass4 = new GlslShader();
	private GlslShader mLensBlurPass5 = new GlslShader();

	private GlslFbo mFboHalf = new GlslFbo();
	private GlslFbo mFboQuarter = new GlslFbo();
	private FloatBuffer mTriangleVertices;

	private static final float[] mCoords = { -1f, 1f, -1f, -1f, 1f, 1f, 1f, -1f };

	public GlslFilter() {
		ByteBuffer buffer = ByteBuffer.allocateDirect(mCoords.length * 4);
		mTriangleVertices = buffer.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		mTriangleVertices.position(0);
		mTriangleVertices.put(mCoords);
	}

	public void bloom(int texSrc, GlslFbo fboOut, int idxOut, GlslCamera camera) {

		float dx = 2f / mFboQuarter.getWidth();
		float dy = 2f / mFboQuarter.getHeight();

		mFboQuarter.bind();
		mFboQuarter.bindTexture(TEX_IDX_1);
		GLES20.glUseProgram(mBloomPass1.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texSrc);
		GLES20.glUniform1f(mBloomPass1.getHandle("uThreshold"),
				camera.mBloomThreshold);
		drawRect(mBloomPass1.getHandle("aPosition"));

		mFboQuarter.bindTexture(TEX_IDX_2);
		GLES20.glUseProgram(mBloomPass2.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
				mFboQuarter.getTexture(TEX_IDX_1));
		GLES20.glUniform2f(mBloomPass2.getHandle("uOffset"), dx, 0f);
		drawRect(mBloomPass2.getHandle("aPosition"));

		mFboQuarter.bindTexture(TEX_IDX_1);
		GLES20.glUseProgram(mBloomPass2.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
				mFboQuarter.getTexture(TEX_IDX_2));
		GLES20.glUniform2f(mBloomPass2.getHandle("uOffset"), 0f, dy);
		drawRect(mBloomPass2.getHandle("aPosition"));

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

	public void copy(int src) {
		GLES20.glUseProgram(mCopy.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, src);
		drawRect(mCopy.getHandle("aPosition"));
	}

	public void init(Context ctx) {
		mCopy.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_copy_fs));
		mCopy.addHandles("aPosition", "sTexture0");

		mBloomPass1.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_bloom_pass1_fs));
		mBloomPass1.addHandles("aPosition", "sTexture0", "uThreshold");
		mBloomPass2.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_bloom_pass2_fs));
		mBloomPass2.addHandles("aPosition", "sTexture0", "uOffset");
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

	public void init(int width, int height) {
		mFboHalf.init(width / 2, height / 2, 3);
		mFboQuarter.init(width / 4, height / 4, 3);
	}

	public void lensBlur(int texSrc, GlslFbo fboOut, int idxOut,
			GlslCamera camera) {

		float angle = (float) (Math.PI * (SystemClock.uptimeMillis() % 10000) / 5000);
		float stepRadius = camera.mCocRadius / camera.mBlurSteps;

		float[][] dir = new float[3][2];
		for (int i = 0; i < 3; i++) {
			double a = angle + i * Math.PI * 2 / 3;
			dir[i][0] = (float) (stepRadius * Math.sin(a) / mFboHalf.getWidth());
			dir[i][1] = (float) (stepRadius * Math.cos(a) / mFboHalf
					.getHeight());
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

	public void reset() {
		mFboHalf.reset();
		mFboQuarter.reset();
	}

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
