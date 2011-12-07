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

package fi.harism.glsl.scene;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES20;

public final class GlslBox extends GlslObject {

	private static final int FACE_COUNT = 6;
	private static final int VERTICES_PER_FACE = 8;
	private static final int FLOATS_PER_VERTEX = 3;
	private static final int FLOAT_SIZE_BYTES = 4;

	private FloatBuffer mVertexBuffer;
	private FloatBuffer mNormalBuffer;
	private FloatBuffer mColorBuffer;
	private ByteBuffer mIndexBuffer;
	private ByteBuffer mShadowIndexBuffer;

	public GlslBox() {
		int sz = FACE_COUNT * VERTICES_PER_FACE * FLOATS_PER_VERTEX
				* FLOAT_SIZE_BYTES;
		ByteBuffer buffer = ByteBuffer.allocateDirect(sz);
		mVertexBuffer = buffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
		buffer = ByteBuffer.allocateDirect(sz);
		mNormalBuffer = buffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
		buffer = ByteBuffer.allocateDirect(sz);
		mColorBuffer = buffer.order(ByteOrder.nativeOrder()).asFloatBuffer();

		mIndexBuffer = ByteBuffer.allocateDirect(6 * 6);
		mIndexBuffer.position(0);
		for (int i = 0; i < 6; ++i) {
			mIndexBuffer.put((byte) (i * 8 + 0));
			mIndexBuffer.put((byte) (i * 8 + 2));
			mIndexBuffer.put((byte) (i * 8 + 4));
			mIndexBuffer.put((byte) (i * 8 + 2));
			mIndexBuffer.put((byte) (i * 8 + 6));
			mIndexBuffer.put((byte) (i * 8 + 4));
		}

		mShadowIndexBuffer = ByteBuffer.allocateDirect(6 * 6 * 6);
		mShadowIndexBuffer.position(0);
		for (int i = 0; i < 6; ++i) {
			mShadowIndexBuffer.put((byte) (i * 8 + 0));
			mShadowIndexBuffer.put((byte) (i * 8 + 1));
			mShadowIndexBuffer.put((byte) (i * 8 + 2));
			mShadowIndexBuffer.put((byte) (i * 8 + 1));
			mShadowIndexBuffer.put((byte) (i * 8 + 3));
			mShadowIndexBuffer.put((byte) (i * 8 + 2));

			mShadowIndexBuffer.put((byte) (i * 8 + 2));
			mShadowIndexBuffer.put((byte) (i * 8 + 3));
			mShadowIndexBuffer.put((byte) (i * 8 + 6));
			mShadowIndexBuffer.put((byte) (i * 8 + 3));
			mShadowIndexBuffer.put((byte) (i * 8 + 7));
			mShadowIndexBuffer.put((byte) (i * 8 + 6));

			mShadowIndexBuffer.put((byte) (i * 8 + 6));
			mShadowIndexBuffer.put((byte) (i * 8 + 7));
			mShadowIndexBuffer.put((byte) (i * 8 + 4));
			mShadowIndexBuffer.put((byte) (i * 8 + 7));
			mShadowIndexBuffer.put((byte) (i * 8 + 5));
			mShadowIndexBuffer.put((byte) (i * 8 + 4));

			mShadowIndexBuffer.put((byte) (i * 8 + 4));
			mShadowIndexBuffer.put((byte) (i * 8 + 5));
			mShadowIndexBuffer.put((byte) (i * 8 + 0));
			mShadowIndexBuffer.put((byte) (i * 8 + 5));
			mShadowIndexBuffer.put((byte) (i * 8 + 1));
			mShadowIndexBuffer.put((byte) (i * 8 + 0));
		}

		setNormal(0, 0f, 0f, 1f);
		setNormal(1, 0f, 0f, -1f);
		setNormal(2, 0f, 1f, 0f);
		setNormal(3, 0f, -1f, 0f);
		setNormal(4, 1f, 0f, 0f);
		setNormal(5, -1f, 0f, 0f);

		setSize(1f, 1f, 1f);
		setColor(.5f, .5f, .5f);
	}

	@Override
	public void render(GlslShaderIds ids) {
		super.render(ids);

		int stride = FLOATS_PER_VERTEX * FLOAT_SIZE_BYTES;
		mVertexBuffer.position(0);
		GLES20.glVertexAttribPointer(ids.aPosition, 3, GLES20.GL_FLOAT, false,
				stride, mVertexBuffer);
		GLES20.glEnableVertexAttribArray(ids.aPosition);

		mNormalBuffer.position(0);
		GLES20.glVertexAttribPointer(ids.aNormal, 3, GLES20.GL_FLOAT, false,
				stride, mNormalBuffer);
		GLES20.glEnableVertexAttribArray(ids.aNormal);

		mColorBuffer.position(0);
		GLES20.glVertexAttribPointer(ids.aColor, 3, GLES20.GL_FLOAT, false,
				stride, mColorBuffer);
		GLES20.glEnableVertexAttribArray(ids.aColor);

		GLES20.glUniformMatrix4fv(ids.uMVMatrix, 1, false, getModelViewM(), 0);
		GLES20.glUniformMatrix4fv(ids.uMVPMatrix, 1, false,
				getModelViewProjM(), 0);
		GLES20.glUniformMatrix4fv(ids.uNormalMatrix, 1, false, getNormalM(), 0);

		mIndexBuffer.position(0);
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6 * 6,
				GLES20.GL_UNSIGNED_BYTE, mIndexBuffer);
	}

	@Override
	public void renderShadow(int uMVMatrix, int uMVPMatrix, int uNormalMatrix,
			int aPosition, int aNormal) {
		super.renderShadow(uMVMatrix, uMVPMatrix, uNormalMatrix, aPosition,
				aNormal);

		int stride = FLOATS_PER_VERTEX * FLOAT_SIZE_BYTES;
		mVertexBuffer.position(0);
		GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false,
				stride, mVertexBuffer);
		GLES20.glEnableVertexAttribArray(aPosition);

		mNormalBuffer.position(0);
		GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false,
				stride, mNormalBuffer);
		GLES20.glEnableVertexAttribArray(aNormal);

		GLES20.glUniformMatrix4fv(uMVMatrix, 1, false, getModelViewM(), 0);
		GLES20.glUniformMatrix4fv(uMVPMatrix, 1, false, getModelViewProjM(), 0);
		GLES20.glUniformMatrix4fv(uNormalMatrix, 1, false, getNormalM(), 0);

		mShadowIndexBuffer.position(0);
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6 * 4 * 6,
				GLES20.GL_UNSIGNED_BYTE, mShadowIndexBuffer);
	}

	public void setColor(float r, float g, float b) {
		for (int i = 0; i < FACE_COUNT; ++i) {
			setColor(i, r, g, b);
		}
	}

	public void setColor(int face, float r, float g, float b) {
		int i = face * VERTICES_PER_FACE * FLOATS_PER_VERTEX;
		for (int j = 0; j < VERTICES_PER_FACE; ++j) {
			mColorBuffer.put(i + (j * FLOATS_PER_VERTEX), r);
			mColorBuffer.put(i + (j * FLOATS_PER_VERTEX) + 1, g);
			mColorBuffer.put(i + (j * FLOATS_PER_VERTEX) + 2, b);
		}
	}

	public void setSize(float width, float height, float depth) {
		float w = width / 2f;
		float h = height / 2f;
		float d = depth / 2f;

		setSideCoordinates(0, 0, 1, 2, -w, h, w, -h, d);
		setSideCoordinates(1, 0, 1, 2, w, h, -w, -h, -d);
		setSideCoordinates(2, 0, 2, 1, -w, -d, w, d, h);
		setSideCoordinates(3, 0, 2, 1, w, -d, -w, d, -h);
		setSideCoordinates(4, 2, 1, 0, d, h, -d, -h, w);
		setSideCoordinates(5, 2, 1, 0, -d, h, d, -h, -w);
	}

	private void setNormal(int face, float x, float y, float z) {
		int i = face * VERTICES_PER_FACE * FLOATS_PER_VERTEX;
		for (int j = 0; j < VERTICES_PER_FACE; j += 2) {
			mNormalBuffer.put(i + (j * FLOATS_PER_VERTEX), x);
			mNormalBuffer.put(i + (j * FLOATS_PER_VERTEX) + 1, y);
			mNormalBuffer.put(i + (j * FLOATS_PER_VERTEX) + 2, z);
			mNormalBuffer.put(i + (j * FLOATS_PER_VERTEX) + 3, 0);
			mNormalBuffer.put(i + (j * FLOATS_PER_VERTEX) + 4, 0);
			mNormalBuffer.put(i + (j * FLOATS_PER_VERTEX) + 5, 0);
		}
	}

	private void setSideCoordinates(int face, int is, int it, int iu, float s1,
			float t1, float s2, float t2, float u) {
		int i = face * VERTICES_PER_FACE * FLOATS_PER_VERTEX;

		mVertexBuffer.put(i + is, s1);
		mVertexBuffer.put(i + it, t1);
		mVertexBuffer.put(i + iu, u);
		mVertexBuffer.put(i + is + 3, s1);
		mVertexBuffer.put(i + it + 3, t1);
		mVertexBuffer.put(i + iu + 3, u);
		i += 2 * FLOATS_PER_VERTEX;

		mVertexBuffer.put(i + is, s1);
		mVertexBuffer.put(i + it, t2);
		mVertexBuffer.put(i + iu, u);
		mVertexBuffer.put(i + is + 3, s1);
		mVertexBuffer.put(i + it + 3, t2);
		mVertexBuffer.put(i + iu + 3, u);
		i += 2 * FLOATS_PER_VERTEX;

		mVertexBuffer.put(i + is, s2);
		mVertexBuffer.put(i + it, t1);
		mVertexBuffer.put(i + iu, u);
		mVertexBuffer.put(i + is + 3, s2);
		mVertexBuffer.put(i + it + 3, t1);
		mVertexBuffer.put(i + iu + 3, u);
		i += 2 * FLOATS_PER_VERTEX;

		mVertexBuffer.put(i + is, s2);
		mVertexBuffer.put(i + it, t2);
		mVertexBuffer.put(i + iu, u);
		mVertexBuffer.put(i + is + 3, s2);
		mVertexBuffer.put(i + it + 3, t2);
		mVertexBuffer.put(i + iu + 3, u);
	}

}
