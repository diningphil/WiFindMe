package it.sam.diningphil.samproject.UiFragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;

import it.sam.diningphil.samproject.MainActivity;
import it.sam.diningphil.samproject.R;


public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_xml);

        findPreference(getResources().getString(R.string.image_preference)).setOnPreferenceClickListener((MainActivity) getActivity());
    }
}