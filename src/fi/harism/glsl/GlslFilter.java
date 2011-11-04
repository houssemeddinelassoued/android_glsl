package fi.harism.glsl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.content.Context;
import android.opengl.GLES20;
import android.os.SystemClock;

public class GlslFilter {

	private CopyFilter mCopyFilter;
	private BlendFilter mBlendFilter;
	private BokehFilter mBokehFilter;
	private FloatBuffer mTriangleVertices;

	private static final float[] mCoords = { -1f, 1f, 0, 1, -1f, -1f, 0, 0, 1f,
			1f, 1, 1, 1f, -1f, 1, 0 };

	public GlslFilter() {
		mCopyFilter = new CopyFilter();
		mBlendFilter = new BlendFilter();
		mBokehFilter = new BokehFilter();

		ByteBuffer buffer = ByteBuffer.allocateDirect(mCoords.length * 4);
		mTriangleVertices = buffer.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		mTriangleVertices.position(0);
		mTriangleVertices.put(mCoords);
	}

	public void blend(int src0, int src1) {
		mBlendFilter.draw(src0, src1);
	}

	public void bokeh(int src, GlslFramebuffer framebuffer, String tmp1,
			String tmp2, String out, int w, int h) {
		mBokehFilter.draw(src, framebuffer, tmp1, tmp2, out, w, h);
	}

	public void copy(int textureId) {
		mCopyFilter.draw(textureId);
	}

	public void init(Context ctx) {
		mCopyFilter.init(ctx);
		mBlendFilter.init(ctx);
		mBokehFilter.init(ctx);
	}

	private void drawRect(int positionHandle, int texCoordHandle) {
		mTriangleVertices.position(0);
		GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false,
				4 * 4, mTriangleVertices);
		mTriangleVertices.position(2);
		GLES20.glEnableVertexAttribArray(positionHandle);
		GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false,
				4 * 4, mTriangleVertices);
		GLES20.glEnableVertexAttribArray(texCoordHandle);

		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	}

	private class BlendFilter {

		private GlslShader mShader;

		public BlendFilter() {
			mShader = new GlslShader();
		}

		public void draw(int src0, int src1) {
			GLES20.glUseProgram(mShader.getProgram());
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, src0);

			GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, src1);
			GLES20.glUniform1i(mShader.getHandle("sTexture1"), 1);

			drawRect(mShader.getHandle("aPosition"),
					mShader.getHandle("aTextureCoord"));
		}

		public void init(Context ctx) {
			mShader.setProgram(ctx.getString(R.string.shader_blend_vertex),
					ctx.getString(R.string.shader_blend_fragment));
			mShader.addHandles("aPosition", "aTextureCoord", "sTexture0",
					"sTexture1");
		}

	}

	private class BokehFilter {

		private GlslShader mShaderIn;
		private GlslShader mShaderOut;
		private GlslShader mShader1;
		private GlslShader mShader2;
		private GlslShader mShader3;

		public BokehFilter() {
			mShaderIn = new GlslShader();
			mShaderOut = new GlslShader();
			mShader1 = new GlslShader();
			mShader2 = new GlslShader();
			mShader3 = new GlslShader();
		}

		public void draw(int src, GlslFramebuffer framebuffer, String tmp1,
				String tmp2, String out, int w, int h) {
			float angle = (float) (Math.PI
					* (SystemClock.uptimeMillis() % 10000) / 5000);
			float radius = 10f; // * (float) Math.sin(angle);
			float[][] dir = new float[3][2];
			for (int i = 0; i < 3; i++) {
				float a = angle + i * (float) Math.PI * 2 / 3;
				dir[i][0] = radius * (float) Math.sin(a) / w;
				dir[i][1] = radius * (float) Math.cos(a) / h;
			}

			String pass1 = tmp1;
			String pass2 = tmp2;
			String pass3 = out;
			String pass4 = tmp1;

			GLES20.glUseProgram(mShaderIn.getProgram());
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, src);
			framebuffer.useTexture(pass1);
			drawRect(mShaderIn.getHandle("aPosition"),
					mShaderIn.getHandle("aTextureCoord"));

			GLES20.glUseProgram(mShader1.getProgram());
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
					framebuffer.getTexture(pass1));
			GLES20.glUniform2fv(mShader1.getHandle("uDelta0"), 1, dir[0], 0);
			framebuffer.useTexture(pass2);
			drawRect(mShader1.getHandle("aPosition"),
					mShader1.getHandle("aTextureCoord"));

			GLES20.glUseProgram(mShader2.getProgram());
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
					framebuffer.getTexture(pass1));
			GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
					framebuffer.getTexture(pass2));
			GLES20.glUniform1i(mShader2.getHandle("sTexture1"), 1);
			GLES20.glUniform2fv(mShader2.getHandle("uDelta0"), 1, dir[1], 0);
			framebuffer.useTexture(pass3);
			drawRect(mShader2.getHandle("aPosition"),
					mShader2.getHandle("aTextureCoord"));

			GLES20.glUseProgram(mShader3.getProgram());
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
					framebuffer.getTexture(pass2));
			GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
					framebuffer.getTexture(pass3));
			GLES20.glUniform1i(mShader3.getHandle("sTexture1"), 1);
			GLES20.glUniform2fv(mShader3.getHandle("uDelta0"), 1, dir[1], 0);
			GLES20.glUniform2fv(mShader3.getHandle("uDelta1"), 1, dir[2], 0);
			framebuffer.useTexture(pass4);
			drawRect(mShader3.getHandle("aPosition"),
					mShader3.getHandle("aTextureCoord"));

			GLES20.glUseProgram(mShaderOut.getProgram());
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
					framebuffer.getTexture(pass4));
			framebuffer.useTexture(out);
			drawRect(mShaderOut.getHandle("aPosition"),
					mShaderOut.getHandle("aTextureCoord"));
		}

		public void init(Context ctx) {
			mShaderIn.setProgram(ctx.getString(R.string.shader_bokeh_vertex),
					ctx.getString(R.string.shader_bokeh_in_fragment));
			mShaderIn.addHandles("aPosition", "aTextureCoord", "sTexture0");

			mShaderOut.setProgram(ctx.getString(R.string.shader_bokeh_vertex),
					ctx.getString(R.string.shader_bokeh_out_fragment));
			mShaderOut.addHandles("aPosition", "aTextureCoord", "sTexture0");

			mShader1.setProgram(ctx.getString(R.string.shader_bokeh_vertex),
					ctx.getString(R.string.shader_bokeh1_fragment));
			mShader1.addHandles("aPosition", "aTextureCoord", "sTexture0",
					"uDelta0");

			mShader2.setProgram(ctx.getString(R.string.shader_bokeh_vertex),
					ctx.getString(R.string.shader_bokeh2_fragment));
			mShader2.addHandles("aPosition", "aTextureCoord", "sTexture0",
					"sTexture1", "uDelta0");

			mShader3.setProgram(ctx.getString(R.string.shader_bokeh_vertex),
					ctx.getString(R.string.shader_bokeh3_fragment));
			mShader3.addHandles("aPosition", "aTextureCoord", "sTexture0",
					"sTexture1", "uDelta0", "uDelta1");

		}

	}

	private class CopyFilter {

		private GlslShader mShader;

		public CopyFilter() {
			mShader = new GlslShader();
		}

		public void draw(int textureId) {
			GLES20.glUseProgram(mShader.getProgram());
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

			drawRect(mShader.getHandle("aPosition"),
					mShader.getHandle("aTextureCoord"));
		}

		public void init(Context ctx) {
			mShader.setProgram(ctx.getString(R.string.shader_copy_vertex),
					ctx.getString(R.string.shader_copy_fragment));
			mShader.addHandles("aPosition", "aTextureCoord", "sTexture0");
		}
	}

}