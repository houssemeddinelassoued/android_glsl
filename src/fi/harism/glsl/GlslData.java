package fi.harism.glsl;

public final class GlslData {
	// Rendering shader ids.
	public int uMVMatrix = -1;
	public int uMVPMatrix = -1;
	public int uNormalMatrix = -1;
	public int aPosition = -1;
	public int aNormal = -1;
	public int aColor = -1;
	// Camera values.
	public int mViewWidth;
	public int mViewHeight;
	public float mZNear;
	public float mZFar;
	public float[] mViewM = new float[16];
	public float[] mProjM = new float[16];
	// Lens blur values.
	public int mLensBlurSteps;
	public int mLensBlurRadius;
}
