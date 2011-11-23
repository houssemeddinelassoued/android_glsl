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
import android.opengl.Matrix;

public class GlslLight implements GlslAnimator.PathInterface {
	private float[] mPosition = new float[4];
	private float[] mViewPos = new float[4];
	private float[] mTempM = new float[16];

	private FloatBuffer mTriangleVertices;

	public GlslLight() {
		ByteBuffer buffer = ByteBuffer.allocateDirect(3 * 2 * 4 * 4);
		mTriangleVertices = buffer.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		mTriangleVertices.position(0);
	}

	public void getPosition(float[] viewPos, int startIdx) {
		viewPos[startIdx] = mViewPos[0];
		viewPos[startIdx + 1] = mViewPos[1];
		viewPos[startIdx + 2] = mViewPos[2];
	}

	public void render(GlslShaderIds ids, float x, float y, float z, float size) {
		mTriangleVertices.position(0);

		mTriangleVertices.put(x - size);
		mTriangleVertices.put(y + size);
		mTriangleVertices.put(z);
		mTriangleVertices.put(-1);
		mTriangleVertices.put(1);

		mTriangleVertices.put(x - size);
		mTriangleVertices.put(y - size);
		mTriangleVertices.put(z);
		mTriangleVertices.put(-1);
		mTriangleVertices.put(-1);

		mTriangleVertices.put(x + size);
		mTriangleVertices.put(y + size);
		mTriangleVertices.put(z);
		mTriangleVertices.put(1);
		mTriangleVertices.put(1);

		mTriangleVertices.put(x + size);
		mTriangleVertices.put(y - size);
		mTriangleVertices.put(z);
		mTriangleVertices.put(1);
		mTriangleVertices.put(-1);

		mTriangleVertices.position(0);
		GLES20.glVertexAttribPointer(ids.aLightPosition, 3, GLES20.GL_FLOAT,
				false, 5 * 4, mTriangleVertices);
		GLES20.glEnableVertexAttribArray(ids.aLightPosition);

		mTriangleVertices.position(3);
		GLES20.glVertexAttribPointer(ids.aLightTexPosition, 2, GLES20.GL_FLOAT,
				false, 5 * 4, mTriangleVertices);
		GLES20.glEnableVertexAttribArray(ids.aLightTexPosition);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	}

	public void setMVP(float[] viewM) {
		Matrix.setIdentityM(mTempM, 0);
		Matrix.translateM(mTempM, 0, mPosition[0], mPosition[1], mPosition[2]);
		Matrix.multiplyMM(mTempM, 0, viewM, 0, mTempM, 0);
		Matrix.multiplyMV(mViewPos, 0, mTempM, 0, mPosition, 0);
	}

	@Override
	public void setPosition(float position[]) {
		mPosition[0] = position[0];
		mPosition[1] = position[1];
		mPosition[2] = position[2];
		mPosition[3] = 1;
	}

	public void setPosition(float x, float y, float z) {
		mPosition[0] = x;
		mPosition[1] = y;
		mPosition[2] = z;
		mPosition[3] = 1;
	}
}
