package fi.harism.glsl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLES20;
import android.os.SystemClock;

public class GlslFilter {

	private GlslShader mAddShader = new GlslShader();
	private GlslShader mBlendShader = new GlslShader();
	private GlslShader mBokehShaderIn = new GlslShader();
	private GlslShader mBokehShaderOut = new GlslShader();
	private GlslShader mBokehShader1 = new GlslShader();
	private GlslShader mBokehShader2 = new GlslShader();
	private GlslShader mBokehShader3 = new GlslShader();
	private GlslShader mMultShader = new GlslShader();
	private GlslShader mCopyShader = new GlslShader();
	private FloatBuffer mTriangleVertices;

	private float mBokehPower;
	private int mBokehBlurSteps, mBokehBlurRadius;

	private static final float[] mCoords = { -1f, 1f, -1f, -1f, 1f, 1f, 1f, -1f };

	public GlslFilter() {
		ByteBuffer buffer = ByteBuffer.allocateDirect(mCoords.length * 4);
		mTriangleVertices = buffer.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		mTriangleVertices.position(0);
		mTriangleVertices.put(mCoords);
	}

	public void add(int src0, int src1) {
		GLES20.glUseProgram(mAddShader.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, src0);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, src1);
		GLES20.glUniform1i(mAddShader.getHandle("sTexture1"), 1);

		drawRect(mAddShader.getHandle("aPosition"));
	}

	public void blend(int src0, int src1) {
		GLES20.glUseProgram(mBlendShader.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, src0);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, src1);
		GLES20.glUniform1i(mBlendShader.getHandle("sTexture1"), 1);

		drawRect(mBlendShader.getHandle("aPosition"));
	}

	public void bokeh(int src, GlslFramebuffer framebuffer, String tmp1,
			String tmp2, String out, int w, int h) {
		float angle = (float) (Math.PI * (SystemClock.uptimeMillis() % 10000) / 5000);

		float[][] dir = new float[3][2];
		for (int i = 0; i < 3; i++) {
			float a = angle + i * (float) Math.PI * 2 / 3;
			dir[i][0] = mBokehBlurRadius * (float) Math.sin(a) / w;
			dir[i][1] = mBokehBlurRadius * (float) Math.cos(a) / h;
		}

		String pass1 = tmp1;
		String pass2 = tmp2;
		String pass3 = out;
		String pass4 = tmp1;

		GLES20.glUseProgram(mBokehShaderIn.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, src);
		GLES20.glUniform1f(mBokehShaderIn.getHandle("uPower"), mBokehPower);
		framebuffer.useTexture(pass1);
		drawRect(mBokehShaderIn.getHandle("aPosition"));

		GLES20.glUseProgram(mBokehShader1.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
				framebuffer.getTexture(pass1));
		GLES20.glUniform1i(mBokehShader1.getHandle("uSteps"), mBokehBlurSteps);
		GLES20.glUniform2fv(mBokehShader1.getHandle("uDelta0"), 1, dir[0], 0);
		framebuffer.useTexture(pass2);
		drawRect(mBokehShader1.getHandle("aPosition"));

		GLES20.glUseProgram(mBokehShader2.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
				framebuffer.getTexture(pass1));
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
				framebuffer.getTexture(pass2));
		GLES20.glUniform1i(mBokehShader2.getHandle("sTexture1"), 1);
		GLES20.glUniform1i(mBokehShader2.getHandle("uSteps"), mBokehBlurSteps);
		GLES20.glUniform2fv(mBokehShader2.getHandle("uDelta0"), 1, dir[1], 0);
		framebuffer.useTexture(pass3);
		drawRect(mBokehShader2.getHandle("aPosition"));

		GLES20.glUseProgram(mBokehShader3.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
				framebuffer.getTexture(pass2));
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
				framebuffer.getTexture(pass3));
		GLES20.glUniform1i(mBokehShader3.getHandle("sTexture1"), 1);
		GLES20.glUniform1i(mBokehShader3.getHandle("uSteps"), mBokehBlurSteps);
		GLES20.glUniform2fv(mBokehShader3.getHandle("uDelta0"), 1, dir[1], 0);
		GLES20.glUniform2fv(mBokehShader3.getHandle("uDelta1"), 1, dir[2], 0);
		framebuffer.useTexture(pass4);
		drawRect(mBokehShader3.getHandle("aPosition"));

		GLES20.glUseProgram(mBokehShaderOut.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
				framebuffer.getTexture(pass4));
		GLES20.glUniform1f(mBokehShaderOut.getHandle("uInvPower"),
				1.0f / mBokehPower);
		framebuffer.useTexture(out);
		drawRect(mBokehShaderOut.getHandle("aPosition"));
	}

	public void copy(int textureId) {
		GLES20.glUseProgram(mCopyShader.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

		drawRect(mCopyShader.getHandle("aPosition"));
	}

	public void copy(int textureId, float x1, float y1, float x2, float y2) {
		setClipCoords(x1, y1, x2, y2);
		copy(textureId);
		setClipCoords(-1, 1, 1, -1);
	}

	public void init(Context ctx) {
		mCopyShader.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_filter_copy_fs));
		mCopyShader.addHandles("aPosition", "sTexture0");

		mBlendShader.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_filter_blend_fs));
		mBlendShader.addHandles("aPosition", "sTexture0", "sTexture1");

		mBokehShaderIn.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_filter_bokeh_in_fs));
		mBokehShaderIn.addHandles("aPosition", "uPower", "sTexture0");

		mBokehShaderOut.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_filter_bokeh_out_fs));
		mBokehShaderOut.addHandles("aPosition", "uInvPower", "sTexture0");

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

		mMultShader.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_filter_mult_fs));
		mMultShader.addHandles("aPosition", "sTexture0", "sTexture1");

		mAddShader.setProgram(ctx.getString(R.string.shader_filter_vs),
				ctx.getString(R.string.shader_filter_add_fs));
		mAddShader.addHandles("aPosition", "sTexture0", "sTexture1");
	}

	public void mult(int src0, int src1) {
		GLES20.glUseProgram(mMultShader.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, src0);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, src1);
		GLES20.glUniform1i(mMultShader.getHandle("sTexture1"), 1);

		drawRect(mMultShader.getHandle("aPosition"));
	}

	public void setPreferences(Context ctx, SharedPreferences preferences) {
		String key = ctx.getString(R.string.key_bokeh_power);
		mBokehPower = (float) Math.pow(10, preferences.getFloat(key, 0f));
		key = ctx.getString(R.string.key_bokeh_radius);
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

	private void setClipCoords(float x1, float y1, float x2, float y2) {
		mTriangleVertices.put(0, x1);
		mTriangleVertices.put(1, y1);
		mTriangleVertices.put(2, x1);
		mTriangleVertices.put(3, y2);
		mTriangleVertices.put(4, x2);
		mTriangleVertices.put(5, y1);
		mTriangleVertices.put(6, x2);
		mTriangleVertices.put(7, y2);
	}

}
