package fi.harism.glsl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.content.Context;
import android.opengl.GLES20;
import android.os.SystemClock;

public class GlslFilter {

	private static final int TEX_BOKEH1 = 0;
	private static final int TEX_BOKEH2 = 1;
	private static final int TEX_BOKEH3 = 2;

	private GlslShader mLensBlurIn = new GlslShader();
	private GlslShader mLensBlurInHdr = new GlslShader();
	private GlslShader mLensBlurOut = new GlslShader();
	private GlslShader mLensBlur1 = new GlslShader();
	private GlslShader mLensBlur2 = new GlslShader();
	private GlslShader mLensBlur3 = new GlslShader();
	private GlslShader mCopy = new GlslShader();
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

		mLensBlurIn.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_lensblur_in_fs));
		mLensBlurIn.addHandles("aPosition", "sTexture0");
		mLensBlurInHdr.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_lensblur_hdr_in_fs));
		mLensBlurInHdr.addHandles("aPosition", "sTexture0");
		mLensBlurOut.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_lensblur_out_fs));
		mLensBlurOut.addHandles("aPosition", "sTextureSrc", "sTextureBokeh",
				"sTextureBokehHdr", "sTexture1");
		mLensBlur1.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_lensblur_1_fs));
		mLensBlur1.addHandles("aPosition", "uSteps", "sTexture0", "uDelta0");
		mLensBlur2.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_lensblur_2_fs));
		mLensBlur2.addHandles("aPosition", "uSteps", "sTexture0", "sTexture1",
				"uDelta0", "uDelta1");
		mLensBlur3.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_lensblur_3_fs));
		mLensBlur3.addHandles("aPosition", "uSteps", "sTexture0", "sTexture1",
				"uDelta0", "uDelta1");
	}

	public void init(int width, int height) {
		mFboHalf.init(width / 2, height / 2, 3);
		mFboQuarter.init(width / 4, height / 4, 3);
	}

	public void lensBlur(int texSrc, GlslFbo fboOut, int idxOut,
			GlslCamera camera) {
		mFboHalf.bind();
		mFboHalf.bindTexture(TEX_BOKEH3);
		GLES20.glUseProgram(mLensBlurIn.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texSrc);
		drawRect(mLensBlurIn.getHandle("aPosition"));

		lensBlur(mFboHalf.getTexture(TEX_BOKEH3), mFboHalf, TEX_BOKEH1,
				TEX_BOKEH2, TEX_BOKEH3, camera);

		mFboQuarter.bind();
		mFboQuarter.bindTexture(TEX_BOKEH3);
		GLES20.glUseProgram(mLensBlurInHdr.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texSrc);
		drawRect(mLensBlurInHdr.getHandle("aPosition"));

		lensBlur(mFboQuarter.getTexture(TEX_BOKEH3), mFboQuarter, TEX_BOKEH1,
				TEX_BOKEH2, TEX_BOKEH3, camera);

		fboOut.bind();
		fboOut.bindTexture(idxOut);
		GLES20.glUseProgram(mLensBlurOut.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texSrc);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
				mFboHalf.getTexture(TEX_BOKEH3));
		GLES20.glUniform1i(mLensBlurOut.getHandle("sTextureBokeh"), 1);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
				mFboQuarter.getTexture(TEX_BOKEH3));
		GLES20.glUniform1i(mLensBlurOut.getHandle("sTextureBokehHdr"), 2);
		drawRect(mLensBlurOut.getHandle("aPosition"));

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

	private void lensBlur(int texSrc, GlslFbo fbo, int idxStep1, int idxStep2,
			int idxStep3, GlslCamera camera) {
		float angle = (float) (Math.PI * (SystemClock.uptimeMillis() % 10000) / 5000);
		float stepRadius = camera.mCocRadius / camera.mBlurSteps;

		float[][] dir = new float[3][2];
		for (int i = 0; i < 3; i++) {
			double a = angle + i * Math.PI * 2 / 3;
			dir[i][0] = (float) (stepRadius * Math.sin(a) / fbo.getWidth());
			dir[i][1] = (float) (stepRadius * Math.cos(a) / fbo.getHeight());
		}

		fbo.bindTexture(idxStep1);
		GLES20.glUseProgram(mLensBlur1.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texSrc);
		GLES20.glUniform1f(mLensBlur1.getHandle("uSteps"), camera.mBlurSteps);
		GLES20.glUniform2fv(mLensBlur1.getHandle("uDelta0"), 1, dir[0], 0);
		drawRect(mLensBlur1.getHandle("aPosition"));

		fbo.bindTexture(idxStep2);
		GLES20.glUseProgram(mLensBlur2.getProgram());
		// GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		// GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
		// fboTmp.getTexture(idxPass1));
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fbo.getTexture(idxStep1));
		GLES20.glUniform1i(mLensBlur2.getHandle("sTexture1"), 1);
		GLES20.glUniform1f(mLensBlur2.getHandle("uSteps"), camera.mBlurSteps);
		GLES20.glUniform2fv(mLensBlur2.getHandle("uDelta0"), 1, dir[0], 0);
		GLES20.glUniform2fv(mLensBlur2.getHandle("uDelta1"), 1, dir[1], 0);
		drawRect(mLensBlur2.getHandle("aPosition"));

		fbo.bindTexture(idxStep3);
		GLES20.glUseProgram(mLensBlur3.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fbo.getTexture(idxStep2));
		// GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		// GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
		// fboTmp.getTexture(idxPass2));
		GLES20.glUniform1i(mLensBlur3.getHandle("sTexture1"), 1);
		GLES20.glUniform1f(mLensBlur3.getHandle("uSteps"), camera.mBlurSteps);
		GLES20.glUniform2fv(mLensBlur3.getHandle("uDelta0"), 1, dir[2], 0);
		GLES20.glUniform2fv(mLensBlur3.getHandle("uDelta1"), 1, dir[1], 0);
		drawRect(mLensBlur3.getHandle("aPosition"));
	}

}
