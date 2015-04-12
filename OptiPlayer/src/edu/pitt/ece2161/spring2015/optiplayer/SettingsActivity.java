package edu.pitt.ece2161.spring2015.optiplayer;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * This activity is used to display preferences.
 * The res/xml/preferences.xml file configures the content of this activity.
 * 
 * @author Brian Rupert
 */
public class SettingsActivity extends Activity implements OnSharedPreferenceChangeListener {
	
	public static final String KEY_PREF_DEBUGMODE = "pref_debugmode";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
    
    public class SettingsFragment extends PreferenceFragment {
    	
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
        }
        
        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(SettingsActivity.this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(SettingsActivity.this);
        }

    }
    
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(KEY_PREF_DEBUGMODE)) {
			boolean isDebugMode = sharedPreferences.getBoolean(key, false);
            AppSettings.getInstance().setDebugMode(isDebugMode);
        }
	}
}