package fi.harism.glsl.object;

public class GlslLight {
	private float[] mPosition = new float[3];
	private float[] mDirection = new float[3];

	public void setDirection(float x, float y, float z) {
		mDirection[0] = x;
		mDirection[1] = y;
		mDirection[2] = z;
	}

	public void setPosition(float x, float y, float z) {
		mPosition[0] = x;
		mPosition[1] = y;
		mPosition[2] = z;
	}
}
