package fi.harism.glsl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.content.Context;
import android.opengl.GLES20;
import android.os.SystemClock;

public class GlslFilters {

	private GlslShader mCopyShader;
	private GlslShader mBlurShader;
	private GlslShader mBokehShader1;
	private GlslShader mBokehShader2;
	private GlslShader mBokehShader3;
	private FloatBuffer mTriangleVertices;

	private static final float[] mCoords = { -1f, 1f, 0, 1, -1f, -1f, 0, 0, 1f,
			1f, 1, 1, 1f, -1f, 1, 0 };

	public GlslFilters(Context context) {
		mCopyShader = new GlslShader(context);
		mBlurShader = new GlslShader(context);
		mBokehShader1 = new GlslShader(context);
		mBokehShader2 = new GlslShader(context);
		mBokehShader3 = new GlslShader(context);

		ByteBuffer buffer = ByteBuffer.allocateDirect(mCoords.length * 4);
		mTriangleVertices = buffer.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		mTriangleVertices.position(0);
		mTriangleVertices.put(mCoords);
	}

	public void blur(int textureId) {
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		GLES20.glUseProgram(mBlurShader.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

		drawRect(mBlurShader.getHandle("aPosition"),
				mBlurShader.getHandle("aTextureCoord"));
	}

	public void bokeh(GlslFramebuffer framebuffer, String src, String out,
			int w, int h) {
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		long time = SystemClock.uptimeMillis() % 10000L;
		float angle = (float) (Math.PI * time / 5000);
		float radius = 20f * (float) Math.sin(angle);
		float[][] dir = new float[3][2];
		for (int i = 0; i < 3; i++) {
			float a = angle + i * (float) Math.PI * 2 / 3;
			dir[i][0] = radius * (float) Math.sin(a) / w;
			dir[i][1] = radius * (float) Math.cos(a) / h;
		}

		GLES20.glUseProgram(mBokehShader1.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, framebuffer.getTexture(src));
		GLES20.glUniform2fv(mBokehShader1.getHandle("uDelta0"), 1, dir[0], 0);
		framebuffer.useTexture("tex1");
		drawRect(mBokehShader1.getHandle("aPosition"),
				mBokehShader1.getHandle("aTextureCoord"));

		GLES20.glUseProgram(mBokehShader2.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, framebuffer.getTexture(src));
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
				framebuffer.getTexture("tex1"));
		GLES20.glUniform1i(mBokehShader2.getHandle("sTexture1"), 1);
		GLES20.glUniform2fv(mBokehShader2.getHandle("uDelta0"), 1, dir[1], 0);
		framebuffer.useTexture("tex2");
		drawRect(mBokehShader2.getHandle("aPosition"),
				mBokehShader2.getHandle("aTextureCoord"));

		GLES20.glUseProgram(mBokehShader3.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
				framebuffer.getTexture("tex1"));
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
				framebuffer.getTexture("tex2"));
		GLES20.glUniform1i(mBokehShader3.getHandle("sTexture1"), 1);
		GLES20.glUniform2fv(mBokehShader3.getHandle("uDelta0"), 1, dir[1], 0);
		GLES20.glUniform2fv(mBokehShader3.getHandle("uDelta1"), 1, dir[2], 0);
		framebuffer.useTexture(out);
		drawRect(mBokehShader3.getHandle("aPosition"),
				mBokehShader3.getHandle("aTextureCoord"));

	}

	public void copy(int textureId) {
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		GLES20.glUseProgram(mCopyShader.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

		drawRect(mCopyShader.getHandle("aPosition"),
				mCopyShader.getHandle("aTextureCoord"));
	}

	public void init() {
		mCopyShader.loadProgram(R.string.shader_copy_vertex,
				R.string.shader_copy_fragment);
		mCopyShader.addHandle("aPosition");
		mCopyShader.addHandle("aTextureCoord");

		mBlurShader.loadProgram(R.string.shader_blur_vertex,
				R.string.shader_blur_fragment);
		mBlurShader.addHandle("aPosition");
		mBlurShader.addHandle("aTextureCoord");

		mBokehShader1.loadProgram(R.string.shader_bokeh_vertex,
				R.string.shader_bokeh1_fragment);
		mBokehShader1.addHandle("aPosition");
		mBokehShader1.addHandle("aTextureCoord");
		mBokehShader1.addHandle("sTexture0");
		mBokehShader1.addHandle("uDelta0");

		mBokehShader2.loadProgram(R.string.shader_bokeh_vertex,
				R.string.shader_bokeh2_fragment);
		mBokehShader2.addHandle("aPosition");
		mBokehShader2.addHandle("aTextureCoord");
		mBokehShader2.addHandle("sTexture0");
		mBokehShader2.addHandle("sTexture1");
		mBokehShader2.addHandle("uDelta0");

		mBokehShader3.loadProgram(R.string.shader_bokeh_vertex,
				R.string.shader_bokeh3_fragment);
		mBokehShader3.addHandle("aPosition");
		mBokehShader3.addHandle("aTextureCoord");
		mBokehShader3.addHandle("sTexture0");
		mBokehShader3.addHandle("sTexture1");
		mBokehShader3.addHandle("uDelta0");
		mBokehShader3.addHandle("uDelta1");

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

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	}

}
