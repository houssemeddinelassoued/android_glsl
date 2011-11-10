package fi.harism.glsl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLES20;
import android.os.SystemClock;

public class GlslFilter {

	private GlslShader mBokehShaderIn = new GlslShader();
	private GlslShader mBokehShaderOut = new GlslShader();
	private GlslShader mBokehShader1 = new GlslShader();
	private GlslShader mBokehShader2 = new GlslShader();
	private GlslShader mBokehShader3 = new GlslShader();
	private GlslShader mCopyShader = new GlslShader();
	private FloatBuffer mTriangleVertices;

	private int mBokehBlurSteps, mBokehBlurRadius;

	private static final float[] mCoords = { -1f, 1f, -1f, -1f, 1f, 1f, 1f, -1f };

	public GlslFilter() {
		ByteBuffer buffer = ByteBuffer.allocateDirect(mCoords.length * 4);
		mTriangleVertices = buffer.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		mTriangleVertices.position(0);
		mTriangleVertices.put(mCoords);
	}

	public void bokeh(int texSrc, GlslFramebuffer fboTmp, int idxTmp1,
			int idxTmp2, int idxTmp3, GlslFramebuffer fboOut, int idxOut,
			int w, int h) {
		float angle = (float) (Math.PI * (SystemClock.uptimeMillis() % 10000) / 5000);

		float[][] dir = new float[3][2];
		for (int i = 0; i < 3; i++) {
			float a = angle + i * (float) Math.PI * 2 / 3;
			dir[i][0] = mBokehBlurRadius * (float) Math.sin(a) / w;
			dir[i][1] = mBokehBlurRadius * (float) Math.cos(a) / h;
		}

		int idxPass1 = idxTmp1;
		int idxPass2 = idxTmp2;
		int idxPass3 = idxTmp3;
		int idxPass4 = idxTmp1;
		fboTmp.bind();

		fboTmp.bindTexture(idxPass1);
		GLES20.glUseProgram(mBokehShaderIn.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texSrc);
		drawRect(mBokehShaderIn.getHandle("aPosition"));

		fboTmp.bindTexture(idxPass2);
		GLES20.glUseProgram(mBokehShader1.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTmp.getTexture(idxPass1));
		GLES20.glUniform1i(mBokehShader1.getHandle("uSteps"), mBokehBlurSteps);
		GLES20.glUniform2fv(mBokehShader1.getHandle("uDelta0"), 1, dir[0], 0);
		drawRect(mBokehShader1.getHandle("aPosition"));

		fboTmp.bindTexture(idxPass3);
		GLES20.glUseProgram(mBokehShader2.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTmp.getTexture(idxPass1));
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTmp.getTexture(idxPass2));
		GLES20.glUniform1i(mBokehShader2.getHandle("sTexture1"), 1);
		GLES20.glUniform1i(mBokehShader2.getHandle("uSteps"), mBokehBlurSteps);
		GLES20.glUniform2fv(mBokehShader2.getHandle("uDelta0"), 1, dir[1], 0);
		drawRect(mBokehShader2.getHandle("aPosition"));

		fboTmp.bindTexture(idxPass4);
		GLES20.glUseProgram(mBokehShader3.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTmp.getTexture(idxPass2));
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTmp.getTexture(idxPass3));
		GLES20.glUniform1i(mBokehShader3.getHandle("sTexture1"), 1);
		GLES20.glUniform1i(mBokehShader3.getHandle("uSteps"), mBokehBlurSteps);
		GLES20.glUniform2fv(mBokehShader3.getHandle("uDelta0"), 1, dir[1], 0);
		GLES20.glUniform2fv(mBokehShader3.getHandle("uDelta1"), 1, dir[2], 0);
		drawRect(mBokehShader3.getHandle("aPosition"));

		fboOut.bind();
		fboOut.bindTexture(idxOut);
		GLES20.glUseProgram(mBokehShaderOut.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTmp.getTexture(idxPass4));
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texSrc);
		GLES20.glUniform1i(mBokehShader3.getHandle("sTexture1"), 1);
		drawRect(mBokehShaderOut.getHandle("aPosition"));
	}

	public void copy(int src) {
		GLES20.glUseProgram(mCopyShader.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, src);
		drawRect(mCopyShader.getHandle("aPosition"));
	}

	public void init(Context ctx) {
		mCopyShader.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_filter_copy_fs));
		mCopyShader.addHandles("aPosition", "sTexture0");

		mBokehShaderIn.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_filter_bokeh_in_fs));
		mBokehShaderIn.addHandles("aPosition", "sTexture0");

		mBokehShaderOut.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_filter_bokeh_out_fs));
		mBokehShaderOut.addHandles("aPosition", "sTexture0", "sTexture1");

		mBokehShader1.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_filter_bokeh1_fs));
		mBokehShader1.addHandles("aPosition", "uSteps", "sTexture0", "uDelta0");

		mBokehShader2.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_filter_bokeh2_fs));
		mBokehShader2.addHandles("aPosition", "uSteps", "sTexture0",
				"sTexture1", "uDelta0");

		mBokehShader3.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_filter_bokeh3_fs));
		mBokehShader3.addHandles("aPosition", "uSteps", "sTexture0",
				"sTexture1", "uDelta0", "uDelta1");

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

	public void setPreferences(Context ctx, SharedPreferences preferences) {
		String key = ctx.getString(R.string.key_bokeh_radius);
		mBokehBlurRadius = (int) preferences.getFloat(key, 0);
		key = ctx.getString(R.string.key_bokeh_steps);
		mBokehBlurSteps = (int) preferences.getFloat(key, 0);
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
