package fi.harism.glsl;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class GlslPreferenceActivity extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

}
