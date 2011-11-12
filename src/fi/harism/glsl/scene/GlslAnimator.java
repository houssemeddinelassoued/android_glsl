package fi.harism.glsl.scene;

import java.util.HashMap;

import android.os.SystemClock;

public final class GlslAnimator {

	private long mLastTime = -1;
	private HashMap<GlslObject, AnimData> mObjectMap = new HashMap<GlslObject, AnimData>();

	public void animate() {

		long time = SystemClock.uptimeMillis();
		double timeDiff = mLastTime == -1 ? 0 : time - mLastTime;
		timeDiff /= 1000;
		mLastTime = time;

		for (GlslObject object : mObjectMap.keySet()) {
			AnimData data = mObjectMap.get(object);

			float[] rotation = { 0, 0, 0 };
			object.getRotation(rotation);
			for (int i = 0; i < 3; ++i) {
				rotation[i] += timeDiff * data.mRotationD[i];
				while (rotation[i] < 0f)
					rotation[i] += 360f;
				while (rotation[i] > 360f)
					rotation[i] -= 360f;
			}
			object.setRotation(rotation);
		}
	}

	public void clear() {
		mObjectMap.clear();
	}

	public void setRotation(GlslObject object, float x, float y, float z) {
		AnimData data = mObjectMap.get(object);
		if (data == null) {
			data = new AnimData();
			mObjectMap.put(object, data);
		}
		data.mRotationD[0] = x;
		data.mRotationD[1] = y;
		data.mRotationD[2] = z;
	}

	private final class AnimData {
		public float[] mRotationD = new float[3];
	}
}
