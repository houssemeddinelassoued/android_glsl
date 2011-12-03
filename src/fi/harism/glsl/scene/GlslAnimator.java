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

/**
 * Animator class for creating various animations on scene.
 */
public final class GlslAnimator {

	// Map for holding rotation objects and related rotation information.
	private HashMap<RotationInterface, RotationData> mRotationMap = new HashMap<RotationInterface, RotationData>();
	// Map for holding path objects and related path information.
	private HashMap<PathInterface, Path> mPathMap = new HashMap<PathInterface, Path>();

	/**
	 * Animates all objects within this animator according to given time. Time
	 * value can be any value e.g. starting from zero or system uptime.
	 * 
	 * @param time
	 *            Current time value
	 */
	public void animate(long time) {
		for (RotationInterface object : mRotationMap.keySet()) {
			RotationData data = mRotationMap.get(object);
			rotate(object, data, time);
		}
		for (PathInterface object : mPathMap.keySet()) {
			Path path = mPathMap.get(object);
			move(object, path.mPath, time);
		}
	}

	/**
	 * Removes all objects from this animator.
	 */
	public void clear() {
		mPathMap.clear();
		mRotationMap.clear();
	}

	/**
	 * Sets path for given object. Path should be an array of PathElements in
	 * increasing order based on time value.
	 * 
	 * @param object
	 *            Object to receive position updates.
	 * @param path
	 *            Array of path elements.
	 */
	public void setPath(PathInterface object, Path path) {
		mPathMap.put(object, path);
	}

	/**
	 * Sets rotation for given object.
	 * 
	 * @param object
	 *            Object to receive rotation updates.
	 * @param data
	 *            Rotation information.
	 */
	public void setRotation(RotationInterface object, RotationData data) {
		mRotationMap.put(object, data);
	}

	/**
	 * Private method for updating object's position based on path set to it.
	 * 
	 * @param obj
	 *            Current object.
	 * @param path
	 *            Path to follow.
	 * @param time
	 *            Current time.
	 */
	private void move(PathInterface obj, Vector<PathElement> path, long time) {
		time = time % path.lastElement().mTime;
		if (time < 0) {
			time += path.lastElement().mTime;
		}

		float pos[] = { 0, 0, 0 };
		switch (path.size()) {
		// There's nothing we can do
		case 0: {
			break;
		}
		// Simply set position
		case 1: {
			PathElement PE = path.firstElement();
			for (int i = 0; i < 3; ++i) {
				pos[i] = PE.mPos[i];
			}
			obj.setPosition(pos);
			break;
		}
		// With two path elements do linear interpolation between path elements
		case 2: {
			PathElement PE0 = path.firstElement(), PE1 = path.lastElement();
			float t = (float) (time - PE0.mTime)
					/ (float) (PE1.mTime - PE0.mTime);
			for (int i = 0; i < 3; ++i) {
				pos[i] = PE0.mPos[i] + t * (PE1.mPos[i] - PE0.mPos[i]);
			}
		}
		// Given three, or more, path elements we iterate over Catmull-Rom
		// spline calculated based on given path element positions.
		default: {
			PathElement PE0 = null, PE1 = null, PE2 = null, PE3 = null;

			// Search for current path element, which is the one having current
			// time closing in.
			for (PathElement elem : path) {
				if (elem.mTime > time) {
					PE2 = elem;
					break;
				}
			}

			int idx = path.indexOf(PE2);
			// Find previous path element
			if (idx - 1 >= 0)
				PE1 = path.elementAt(idx - 1);
			else
				PE1 = path.elementAt(path.size() - 1);
			// Find element two steps ahead.
			if (idx - 2 >= 0)
				PE0 = path.elementAt(idx - 2);
			else
				PE0 = path.elementAt(path.size() - 2);
			// Find next path element.
			if (idx + 1 < path.size())
				PE3 = path.elementAt(idx + 1);
			else
				PE3 = path.elementAt(0);

			// We are iterating between PE1 and PE2.
			float t = (float) (time - PE1.mTime)
					/ (float) (PE2.mTime - PE1.mTime);

			// Calculate spline values for each component.
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
		}
	}

	/**
	 * Private method for updating an object's rotation.
	 * 
	 * @param obj
	 *            current object
	 * @param data
	 *            rotation information
	 * @param time
	 *            current time
	 */
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

	/**
	 * Path for animating object positions.
	 */
	public final class Path {
		private Vector<PathElement> mPath = new Vector<PathElement>();

		/**
		 * Adds new position to this path. Positions should be given with
		 * increasing time stamp. And time starting from zero for first element.
		 * 
		 * @param x
		 *            X coordinate at given time.
		 * @param y
		 *            Y coordinate at given time.
		 * @param z
		 *            Z coordinate at given time.
		 * @param time
		 *            Time in milliseconds.
		 */
		public void addPosition(float x, float y, float z, long time) {
			mPath.add(new PathElement(x, y, z, time));
		}
	}

	/**
	 * Interface for updating object's position based on given path.
	 */
	public interface PathInterface {
		/**
		 * Callback method for updating position. Array contains three elements,
		 * x, y and z, in that particular order.
		 * 
		 * @param position
		 *            Array containing new position.
		 */
		public void setPosition(float position[]);
	}

	/**
	 * Class for storing rotation time values.
	 */
	public final class RotationData {
		private long[] mRotationTime = new long[3];

		/**
		 * Rotation data takes three paremeters, timeX, timeY and timeZ. These
		 * values are in milliseconds and present the time a full 360-degree
		 * rotation consumes. Values may be negative for counter clockwise
		 * rotation.
		 * 
		 * @param timeX
		 *            Rotation time for x axis
		 * @param timeY
		 *            Rotation time for y axis
		 * @param timeZ
		 *            Rotation time for z axis
		 */
		public RotationData(long timeX, long timeY, long timeZ) {
			mRotationTime[0] = timeX;
			mRotationTime[1] = timeY;
			mRotationTime[2] = timeZ;
		}
	}

	/**
	 * Interface for rotation animation.
	 */
	public interface RotationInterface {
		/**
		 * Callback method which is called from GlslAnimator to update rotation
		 * values. Array contains three elements, x, y, and z, in that
		 * particular order.
		 * 
		 * @param rotation
		 *            Array containing x, y and z values.
		 */
		public void setRotation(float rotation[]);
	}

	/**
	 * Path element for creating movement paths for objects.
	 */
	private final class PathElement {
		private float[] mPos = new float[3];
		private long mTime;

		/**
		 * PathElement takes four parameters. X, y and z is the position at
		 * particular time. Time parameter should start from zero for the first
		 * elements, and increase for the latter ones giving the the when that
		 * particular point is reached. Time is given in milliseconds.
		 * 
		 * @param x
		 *            position x
		 * @param y
		 *            position y
		 * @param z
		 *            position z
		 * @param time
		 *            time value in millis
		 */
		public PathElement(float x, float y, float z, long time) {
			mPos[0] = x;
			mPos[1] = y;
			mPos[2] = z;
			mTime = time;
		}
	}
}
