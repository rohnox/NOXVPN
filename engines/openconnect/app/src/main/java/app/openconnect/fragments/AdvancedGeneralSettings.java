/*
 * Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package app.openconnect.fragments;

import java.io.File;
import java.util.Map;

import android.Manifest.permission;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;

import app.openconnect.PreferenceScreenStyler;
import app.openconnect.R;
import app.openconnect.core.DeviceStateReceiver;

public class AdvancedGeneralSettings extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.advanced_general_settings);

        Preference loadTun = findPreference("loadTunModule");
        if (loadTun != null && !isTunModuleAvailable()) {
            loadTun.setEnabled(false);
        }

        Preference traceLog = findPreference("trace_log");
        if (traceLog != null) {
            traceLog.setOnPreferenceChangeListener((preference, newValue) -> {
                Intent intent = new Intent(DeviceStateReceiver.PREF_CHANGED);
                intent.setPackage(getActivity().getPackageName());
                getActivity().sendBroadcast(intent, permission.ACCESS_NETWORK_STATE);
                return true;
            });
        }

        SharedPreferences preferences = getPreferenceScreen().getSharedPreferences();
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            onSharedPreferenceChanged(preferences, entry.getKey());
        }
        onSharedPreferenceChanged(preferences, "timestamp_format");
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        PreferenceScreenStyler.apply(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        Preference preference = findPreference(key);
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            String value = preferences.getString(key, "");
            if (!value.isEmpty()) {
                listPreference.setValue(value);
            }
            preference.setSummary(listPreference.getEntry());
        }
    }

    private boolean isTunModuleAvailable() {
        return new File("/system/lib/modules/tun.ko").length() > 10;
    }
}
