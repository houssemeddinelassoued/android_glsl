package fi.harism.glsl;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public final class GlslPreferenceActivity extends PreferenceActivity implements
		Preference.OnPreferenceClickListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		String key = getString(R.string.key_reset);
		Preference p = getPreferenceScreen().findPreference(key);
		if (p != null) {
			p.setOnPreferenceClickListener(this);
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String key = getString(R.string.key_reset);
		if (preference.getKey().equals(key)) {
			preference.getEditor().clear().commit();
			PreferenceManager.setDefaultValues(GlslPreferenceActivity.this,
					R.xml.preferences, true);
			finish();
			return true;
		}
		return false;
	}

}
