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

import java.util.HashMap;
import java.util.Vector;

public final class GlslAnimator {

	private HashMap<RotationInterface, RotationData> mRotationMap = new HashMap<RotationInterface, RotationData>();
	private HashMap<PathInterface, Vector<PathElement>> mPathMap = new HashMap<PathInterface, Vector<PathElement>>();

	public void animate(long time) {
		for (RotationInterface object : mRotationMap.keySet()) {
			RotationData data = mRotationMap.get(object);
			rotate(object, data, time);
		}
		for (PathInterface object : mPathMap.keySet()) {
			Vector<PathElement> path = mPathMap.get(object);
			move(object, path, time);
		}
	}

	public void clear() {
		mPathMap.clear();
		mRotationMap.clear();
	}

	public void setPath(PathInterface object, Vector<PathElement> path) {
		mPathMap.put(object, path);
	}

	public void setRotation(RotationInterface object, long x, long y, long z) {
		RotationData data = mRotationMap.get(object);
		if (data == null) {
			data = new RotationData();
			mRotationMap.put(object, data);
		}
		data.mRotationTime[0] = x;
		data.mRotationTime[1] = y;
		data.mRotationTime[2] = z;
	}

	private void move(PathInterface obj, Vector<PathElement> path, long time) {
		time = time % path.lastElement().mTime;
		if (time < 0) {
			time += path.lastElement().mTime;
		}
		PathElement PE0 = null, PE1 = null, PE2 = null, PE3 = null;
		for (PathElement elem : path) {
			if (elem.mTime > time) {
				PE2 = elem;
				break;
			}
		}

		int idx = path.indexOf(PE2);
		if (idx - 1 >= 0)
			PE1 = path.elementAt(idx - 1);
		else
			PE1 = path.elementAt(path.size() - 1);
		if (idx - 2 >= 0)
			PE0 = path.elementAt(idx - 2);
		else
			PE0 = path.elementAt(path.size() - 2);
		if (idx + 1 < path.size())
			PE3 = path.elementAt(idx + 1);
		else
			PE3 = path.elementAt(0);

		float t = (float) (time - PE1.mTime) / (float) (PE2.mTime - PE1.mTime);
		float pos[] = { 0, 0, 0 };
		for (int i = 0; i < 3; ++i) {
			float p0 = PE0.mPos[i];
			float p1 = PE1.mPos[i];
			float p2 = PE2.mPos[i];
			float p3 = PE3.mPos[i];
			pos[i] = (2 * p1) + (-p0 + p2) * t;
			pos[i] += (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t;
			pos[i] += (-p0 + 3 * p1 - 3 * p2 + p3) * t * t * t;
			pos[i] *= 0.5f;
		}
		obj.setPosition(pos);
	}

	private void rotate(RotationInterface obj, RotationData data, long time) {
		float[] rotation = { 0, 0, 0 };
		for (int i = 0; i < 3; ++i) {
			if (data.mRotationTime[i] != 0) {
				rotation[i] = (360f * (time % data.mRotationTime[i]))
						/ data.mRotationTime[i];
			}
		}
		obj.setRotation(rotation);
	}

	public class PathElement {
		private float[] mPos = new float[3];
		private long mTime;

		public PathElement(float x, float y, float z, long time) {
			mPos[0] = x;
			mPos[1] = y;
			mPos[2] = z;
			mTime = time;
		}
	}

	public interface PathInterface {
		public void setPosition(float position[]);
	}

	public interface RotationInterface {
		public void setRotation(float rotation[]);
	}

	private final class RotationData {
		public long[] mRotationTime = new long[3];
	}
}
