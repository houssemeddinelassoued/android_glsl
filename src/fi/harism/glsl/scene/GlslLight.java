package fi.harism.glsl.scene;

import android.opengl.Matrix;

public class GlslLight {
	private float[] mPosition = new float[4];
	private float[] mProjPos = new float[4];
	private float[] mTempM = new float[16];

	public void getPosition(float[] pos, int posIdx) {
		pos[posIdx] = mProjPos[0];
		pos[posIdx + 1] = mProjPos[1];
		pos[posIdx + 2] = mProjPos[2];
	}

	public void setMVP(float[] viewM) {
		Matrix.setIdentityM(mTempM, 0);
		Matrix.translateM(mTempM, 0, mPosition[0], mPosition[1], mPosition[2]);
		Matrix.multiplyMM(mTempM, 0, viewM, 0, mTempM, 0);
		Matrix.multiplyMV(mProjPos, 0, mTempM, 0, mPosition, 0);
	}

	public void setPosition(float x, float y, float z) {
		mPosition[0] = x;
		mPosition[1] = y;
		mPosition[2] = z;
		mPosition[3] = 1;
	}
}
