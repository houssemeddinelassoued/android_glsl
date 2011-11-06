package fi.harism.glsl.object;

public interface GlslObject {

	public void animate(float time);

	public void drawObject();

	public void setColorAttrib(int colorHandle);

	public void setModelM(float[] m);

	public void setNormalAttrib(int normalHandle);

	public void setPositionAttrib(int positionHandle);
}
