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

	private static final int FLOAT_SIZE_BYTES = 4;
	private static final int DATA_STRIDE_FLOATS = 9;
	private static final int DATA_STRIDE_BYTES = DATA_STRIDE_FLOATS
			* FLOAT_SIZE_BYTES;
	private static final int DATA_POS_OFFSET = 0;
	private static final int DATA_COL_OFFSET = 3;
	private static final int DATA_NORMAL_OFFSET = 6;

	private FloatBuffer mVertexBuffer;

	public GlslBox() {
		ByteBuffer buffer = ByteBuffer
				.allocateDirect(6 * 6 * DATA_STRIDE_BYTES);
		mVertexBuffer = buffer.order(ByteOrder.nativeOrder()).asFloatBuffer();

		setNormal(0, 0f, 1f, 0f);
		setNormal(1, 0f, -1f, 0f);
		setNormal(2, -1f, 0f, 0f);
		setNormal(3, 1f, 0f, 0f);
		setNormal(4, 0f, 0f, -1f);
		setNormal(5, 0f, 0f, 1f);

		setSize(1f, 1f, 1f);
		setColor(.5f, .5f, .5f);
	}

	@Override
	public void render(GlslShaderIds ids) {

		super.render(ids);

		mVertexBuffer.position(DATA_POS_OFFSET);
		GLES20.glVertexAttribPointer(ids.aPosition, 3, GLES20.GL_FLOAT, false,
				DATA_STRIDE_BYTES, mVertexBuffer);
		GLES20.glEnableVertexAttribArray(ids.aPosition);

		mVertexBuffer.position(DATA_NORMAL_OFFSET);
		GLES20.glVertexAttribPointer(ids.aNormal, 3, GLES20.GL_FLOAT, false,
				DATA_STRIDE_BYTES, mVertexBuffer);
		GLES20.glEnableVertexAttribArray(ids.aNormal);

		mVertexBuffer.position(DATA_COL_OFFSET);
		GLES20.glVertexAttribPointer(ids.aColor, 3, GLES20.GL_FLOAT, false,
				DATA_STRIDE_BYTES, mVertexBuffer);
		GLES20.glEnableVertexAttribArray(ids.aColor);

		GLES20.glUniformMatrix4fv(ids.uMVMatrix, 1, false, getModelViewM(), 0);
		GLES20.glUniformMatrix4fv(ids.uMVPMatrix, 1, false,
				getModelViewProjM(), 0);
		GLES20.glUniformMatrix4fv(ids.uNormalMatrix, 1, false, getNormalM(), 0);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6 * 6);
	}

	public void setColor(float r, float g, float b) {
		for (int i = 0; i < 6; ++i) {
			setColor(i, r, g, b);
		}
	}

	public void setColor(int side, float r, float g, float b) {
		int i = DATA_COL_OFFSET + side * DATA_STRIDE_FLOATS * 6;
		for (int j = 0; j < 6; ++j, i += DATA_STRIDE_FLOATS) {
			mVertexBuffer.put(i, r);
			mVertexBuffer.put(i + 1, g);
			mVertexBuffer.put(i + 2, b);
		}
	}

	public void setSize(float width, float height, float depth) {
		float x1 = -width / 2f;
		float x2 = width / 2f;
		float y1 = height / 2f;
		float y2 = -height / 2f;
		float z1 = -depth / 2f;
		float z2 = depth / 2f;

		setSideCoordinates(0, 0, 2, 1, x1, z1, x2, z2, y1);
		setSideCoordinates(1, 0, 2, 1, x2, z1, x1, z2, y2);
		setSideCoordinates(2, 1, 2, 0, y2, z1, y1, z2, x1);
		setSideCoordinates(3, 1, 2, 0, y1, z1, y2, z2, x2);
		setSideCoordinates(4, 0, 1, 2, x2, y1, x1, y2, z1);
		setSideCoordinates(5, 0, 1, 2, x1, y1, x2, y2, z2);
	}

	private void setNormal(int side, float x, float y, float z) {
		int i = DATA_NORMAL_OFFSET + side * DATA_STRIDE_FLOATS * 6;
		for (int j = 0; j < 6; ++j, i += DATA_STRIDE_FLOATS) {
			mVertexBuffer.put(i, x);
			mVertexBuffer.put(i + 1, y);
			mVertexBuffer.put(i + 2, z);
		}
	}

	private void setSideCoordinates(int side, int is, int it, int iu, float s1,
			float t1, float s2, float t2, float u) {
		int i = DATA_POS_OFFSET + side * DATA_STRIDE_FLOATS * 6;

		mVertexBuffer.put(i + is, s1);
		mVertexBuffer.put(i + it, t1);
		mVertexBuffer.put(i + iu, u);
		i += DATA_STRIDE_FLOATS;

		mVertexBuffer.put(i + is, s1);
		mVertexBuffer.put(i + it, t2);
		mVertexBuffer.put(i + iu, u);
		i += DATA_STRIDE_FLOATS;

		mVertexBuffer.put(i + is, s2);
		mVertexBuffer.put(i + it, t1);
		mVertexBuffer.put(i + iu, u);
		i += DATA_STRIDE_FLOATS;

		mVertexBuffer.put(i + is, s1);
		mVertexBuffer.put(i + it, t2);
		mVertexBuffer.put(i + iu, u);
		i += DATA_STRIDE_FLOATS;

		mVertexBuffer.put(i + is, s2);
		mVertexBuffer.put(i + it, t2);
		mVertexBuffer.put(i + iu, u);
		i += DATA_STRIDE_FLOATS;

		mVertexBuffer.put(i + is, s2);
		mVertexBuffer.put(i + it, t1);
		mVertexBuffer.put(i + iu, u);
	}

}
