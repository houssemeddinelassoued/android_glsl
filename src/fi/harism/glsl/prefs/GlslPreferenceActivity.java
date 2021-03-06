/*
   Copyright 2011 Harri Sm�tt

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

package fi.harism.glsl.prefs;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import fi.harism.glsl.R;

/**
 * Preferences Activity.
 */
public final class GlslPreferenceActivity extends PreferenceActivity implements
		Preference.OnPreferenceClickListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Populate preferences from preferences xml -file.
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		// Add listener to reset preference.
		String key = getString(R.string.key_reset);
		Preference p = getPreferenceScreen().findPreference(key);
		if (p != null) {
			p.setOnPreferenceClickListener(this);
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		// On click make sure it's from reset preference.
		String key = getString(R.string.key_reset);
		if (preference.getKey().equals(key)) {
			// Clear preferences.
			preference.getEditor().clear().commit();
			// Populate preferences with default values.
			PreferenceManager.setDefaultValues(GlslPreferenceActivity.this,
					R.xml.preferences, true);
			// Finish preferences activity and return back to main activity.
			finish();
			return true;
		}
		return false;
	}

}
