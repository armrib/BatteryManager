package com.aware.plugin.batteryManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.aware.Aware;
import com.aware.plugin.template.R;

public class Settings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String STATUS_PLUGIN = "status_plugin_batteryManager";

    public static final String FREQUENCY_PLUGIN = "frequency_batteryManager";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        syncSettings();
    }

    private void syncSettings() {
        //Make sure to load the latest values
        CheckBoxPreference status = (CheckBoxPreference) findPreference(STATUS_PLUGIN);
        status.setChecked(Aware.getSetting(this, STATUS_PLUGIN).equals("true"));

        EditTextPreference frequency = (EditTextPreference) findPreference(FREQUENCY_PLUGIN);
        if( Aware.getSetting(getApplicationContext(), FREQUENCY_PLUGIN).length() == 0 ) {
            Aware.setSetting(getApplicationContext(), FREQUENCY_PLUGIN, 5);
        }
        frequency.setSummary(Aware.getSetting(getApplicationContext(), FREQUENCY_PLUGIN) + " minutes");
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncSettings();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference setting = (Preference) findPreference(key);

        if (setting.getKey().equals(STATUS_PLUGIN)) {
            boolean is_active = sharedPreferences.getBoolean(key, false);
            Aware.setSetting(this, key, is_active);
            if (is_active) {
                Aware.startPlugin(this, getPackageName());
            } else {
                Aware.stopPlugin(this, getPackageName());
            }
        }

        if( setting.getKey().equals(FREQUENCY_PLUGIN)) {
            setting.setSummary(sharedPreferences.getString(key, "5") + " minutes");
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, "5"));
        }

        //Apply the new settings
        Intent apply = new Intent(Aware.ACTION_AWARE_REFRESH);
        sendBroadcast(apply);
    }
}
