package de.shellfire.vpn.android;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;


public class PrefsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        initValues();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    private void initValues() {
        PreferenceScreen prefScreen = getPreferenceScreen();
        int count = prefScreen.getPreferenceCount();

        // Go through all of the preferences, and set up their preference summary.
        for (int i = 0; i < count; i++) {
            Preference pref = prefScreen.getPreference(i);
            // You don't need to set up preference summaries for checkbox preferences because
            // they are already set up in xml using summaryOff and summary On
            if (pref instanceof ListPreference) {
                Protocol defaultProtocol = VpnPreferences.getConnectionModeSelection(getActivity());
                if (defaultProtocol == null) {
                    pref.setSummary(Protocol.UDP.name());
                    ((ListPreference) pref).setValueIndex(0);
                } else {
                    pref.setSummary(defaultProtocol.toString());
                    if (defaultProtocol == Protocol.UDP) {
                        ((ListPreference) pref).setValueIndex(0);
                    } else {
                        ((ListPreference) pref).setValueIndex(1);
                    }
                }
            }
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) findPreference(key);
            String value = sharedPreferences.getString(listPreference.getKey(), "");
            int prefIndex = listPreference.findIndexOfValue(value);
            listPreference.setSummary(listPreference.getEntries()[prefIndex].toString());

            Protocol protocol = Protocol.valueOf(listPreference.getEntries()[prefIndex].toString());

            VpnPreferences.setConnectionProtocolChanged(ShellfireApplication.getContext(), true);
            VpnPreferences.setConnectionModeSelection(ShellfireApplication.getContext(), protocol);


            //reset 7 days for changing from TCP to UDP
            VpnPreferences.unsetMostRecentAutoSwitchFromUdpToTcpDate(getActivity());

        }
    }
}
