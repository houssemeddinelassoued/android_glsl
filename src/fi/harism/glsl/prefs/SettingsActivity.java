package fi.harism.glsl.prefs;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import fi.harism.glsl.R;

public class SettingsActivity extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
	}

	
	
}
