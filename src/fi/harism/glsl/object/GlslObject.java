package fi.harism.glsl.object;

import java.util.Vector;

import android.opengl.GLES20;
import android.opengl.Matrix;
import fi.harism.glsl.GlslUtils;

public class GlslObject {

	private float[] mModelM = new float[16];
	private float[] mModelViewM = new float[16];
	private float[] mModelViewProjM = new float[16];
	private float[] mNormalM = new float[16];
	private float[] mTempM = new float[16];
	private float[] mPosition = new float[3];
	private float[] mRotation = new float[3];
	private float mScaling = 1f;
	private Vector<GlslObject> mChildObjects = new Vector<GlslObject>();

	public void addChildObject(GlslObject obj) {
		mChildObjects.add(obj);
	}

	public void animate(float timeDiff) {
		for (GlslObject obj : mChildObjects) {
			obj.animate(timeDiff);
		}
	}

	public void draw(float[] mvM, float[] projM, int mvpId, int projId,
			int posId, int normalId, int colorId) {
		GlslUtils.setRotateM(mModelM, mRotation);
		Matrix.scaleM(mModelM, 0, mScaling, mScaling, mScaling);
		Matrix.setIdentityM(mTempM, 0);
		Matrix.translateM(mTempM, 0, mPosition[0], mPosition[1], mPosition[2]);
		Matrix.multiplyMM(mModelM, 0, mTempM, 0, mModelM, 0);

		Matrix.multiplyMM(mModelViewM, 0, mvM, 0, mModelM, 0);
		Matrix.multiplyMM(mModelViewProjM, 0, projM, 0, mModelViewM, 0);

		Matrix.invertM(mTempM, 0, mModelViewM, 0);
		Matrix.transposeM(mNormalM, 0, mTempM, 0);

		GLES20.glUniformMatrix4fv(mvpId, 1, false, mModelViewProjM, 0);
		GLES20.glUniformMatrix4fv(normalId, 1, false, mNormalM, 0);

		for (GlslObject obj : mChildObjects) {
			obj.draw(mModelViewM, projM, mvpId, normalId, posId, normalId,
					colorId);
		}
	}

	public void setPosition(float x, float y, float z) {
		mPosition[0] = x;
		mPosition[1] = y;
		mPosition[2] = z;
	}

	public void setRotation(float x, float y, float z) {
		mRotation[0] = x;
		mRotation[1] = y;
		mRotation[2] = z;
	}

	public void setScaling(float scaling) {
		mScaling = scaling;
	}
}
