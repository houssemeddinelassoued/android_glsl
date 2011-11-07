package fi.harism.glsl.object;

import java.util.Vector;

import android.opengl.Matrix;
import fi.harism.glsl.GlslUtils;

public class GlslObject {

	private static final float[] mModelM = new float[16];
	private final float[] mModelViewM = new float[16];
	private final float[] mModelViewProjM = new float[16];
	private final float[] mNormalM = new float[16];
	private static final float[] mTempM = new float[16];
	private final float[] mPosition = new float[3];
	private final float[] mRotation = new float[3];
	private float mScaling = 1f;
	private final Vector<GlslObject> mChildObjects = new Vector<GlslObject>();

	public void addChildObject(GlslObject obj) {
		mChildObjects.add(obj);
	}

	public void animate(float timeDiff) {
		for (GlslObject obj : mChildObjects) {
			obj.animate(timeDiff);
		}
	}

	public void draw(int mvpId, int projId, int posId, int normalId, int colorId) {
		for (GlslObject obj : mChildObjects) {
			obj.draw(mvpId, normalId, posId, normalId, colorId);
		}
	}

	public final void recalculate(float[] mvM, float[] projM) {
		GlslUtils.setRotateM(mModelM, mRotation);
		Matrix.scaleM(mModelM, 0, mScaling, mScaling, mScaling);
		Matrix.setIdentityM(mTempM, 0);
		Matrix.translateM(mTempM, 0, mPosition[0], mPosition[1], mPosition[2]);
		Matrix.multiplyMM(mModelM, 0, mTempM, 0, mModelM, 0);

		Matrix.multiplyMM(mModelViewM, 0, mvM, 0, mModelM, 0);
		Matrix.multiplyMM(mModelViewProjM, 0, projM, 0, mModelViewM, 0);

		Matrix.invertM(mTempM, 0, mModelViewM, 0);
		Matrix.transposeM(mNormalM, 0, mTempM, 0);

		for (GlslObject obj : mChildObjects) {
			obj.recalculate(mModelViewM, projM);
		}
	}

	public final void setPosition(float x, float y, float z) {
		mPosition[0] = x;
		mPosition[1] = y;
		mPosition[2] = z;
	}

	public final void setRotation(float x, float y, float z) {
		mRotation[0] = x;
		mRotation[1] = y;
		mRotation[2] = z;
	}

	public final void setScaling(float scaling) {
		mScaling = scaling;
	}

	protected final float[] getModelViewProjM() {
		return mModelViewProjM;
	}

	protected final float[] getNormalM() {
		return mNormalM;
	}
}
