package fi.harism.glsl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.content.Context;
import android.opengl.GLES20;

public class GlslFilters {

	private GlslShader mShader;
	private FloatBuffer mTriangleVertices;

	private static final float[] mCoords = { -1f, 1f, 0, 1, -1f, -1f, 0, 0, 1f,
			1f, 1, 1, 1f, -1f, 1, 0 };

	public GlslFilters(Context context) {
		mShader = new GlslShader(context);

		ByteBuffer buffer = ByteBuffer.allocateDirect(mCoords.length * 4);
		mTriangleVertices = buffer.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		mTriangleVertices.position(0);
		mTriangleVertices.put(mCoords);
	}

	public void draw(int textureId) {

		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		GLES20.glUseProgram(mShader.getProgram());
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

		mTriangleVertices.position(0);
		GLES20.glVertexAttribPointer(mShader.getHandle("aPosition"), 2,
				GLES20.GL_FLOAT, false, 4 * 4, mTriangleVertices);
		mTriangleVertices.position(2);
		GLES20.glEnableVertexAttribArray(mShader.getHandle("aPosition"));
		GLES20.glVertexAttribPointer(mShader.getHandle("aTextureCoord"), 2,
				GLES20.GL_FLOAT, false, 4 * 4, mTriangleVertices);
		GLES20.glEnableVertexAttribArray(mShader.getHandle("aTextureCoord"));

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	}

	public void init() {
		mShader.loadProgram(R.string.shader_copy_vertex,
				R.string.shader_copy_fragment);
		mShader.addHandle("aPosition");
		mShader.addHandle("aTextureCoord");
	}

}
