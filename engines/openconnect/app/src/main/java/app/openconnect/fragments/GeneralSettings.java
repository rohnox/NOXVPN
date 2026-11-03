/*
 * Adapted from OpenVPN for Android
 * Copyright (c) 2012-2013, Arne Schwabe
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * In addition, as a special exception, the copyright holders give
 * permission to link the code of portions of this program with the
 * OpenSSL library.
 */

package app.openconnect.fragments;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.content.ComponentName;
import android.Manifest.permission;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Build;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.service.quicksettings.TileService;
import android.view.View;
import app.openconnect.BuildConfig;
import app.openconnect.FragActivity;
import app.openconnect.PreferenceScreenStyler;
import app.openconnect.R;
import app.openconnect.VpnProfile;
import app.openconnect.api.ExternalAppDatabase;
import app.openconnect.core.DeviceStateReceiver;
import app.openconnect.core.ProfileManager;
import app.openconnect.core.QuickSettingsTileService;
import app.openconnect.update.GitHubUpdateChecker;
import app.openconnect.update.VersionComparator;

public class GeneralSettings extends PreferenceFragment
		implements OnPreferenceClickListener, OnClickListener, OnSharedPreferenceChangeListener {

	private ExternalAppDatabase mExtapp;
	private PreferenceManager mPrefs;
	private Preference mCheckForUpdates;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.general_settings);
		configureQuickSettingsPreference();
		configureUpdatePreferences();
        configureAdvancedSettings();

		mExtapp = new ExternalAppDatabase(getActivity());

		for (String s : new String[] { "netchangereconnect", "screenoff" }) {
			findPreference(s).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference arg0, Object arg1) {
					Intent intent = new Intent(DeviceStateReceiver.PREF_CHANGED);
					intent.setPackage(getActivity().getPackageName());
					getActivity().sendBroadcast(intent, permission.ACCESS_NETWORK_STATE);
					return true;
				}
			});
		}

		mPrefs = getPreferenceManager();
        SharedPreferences sp = mPrefs.getSharedPreferences();
        for (Map.Entry<String,?> entry : sp.getAll().entrySet()) {
            this.onSharedPreferenceChanged(sp, entry.getKey());
        }

		/*
		Preference clearapi = findPreference("clearapi");
		clearapi.setOnPreferenceClickListener(this);
		setClearApiSummary();
		*/
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		PreferenceScreenStyler.apply(this);
	}

    private void configureAdvancedSettings() {
        Preference preference = findPreference("advanced_general_settings");
        if (preference == null) {
            return;
        }
        preference.setOnPreferenceClickListener(clicked -> {
            Intent intent = new Intent(getActivity(), FragActivity.class);
            intent.putExtra(FragActivity.EXTRA_FRAGMENT_NAME, "AdvancedGeneralSettings");
            startActivity(intent);
            return true;
        });
    }

	private void configureUpdatePreferences() {
		mCheckForUpdates = findPreference("check_for_updates");
		if (mCheckForUpdates == null) {
			return;
		}
		updateCheckSummary();
		mCheckForUpdates.setOnPreferenceClickListener(preference -> {
			if (getActivity() == null) {
				return true;
			}
			preference.setEnabled(false);
			preference.setSummary(R.string.update_checking);
			GitHubUpdateChecker.check(getActivity(), true, (result, error) -> {
				if (!isAdded()) {
					return;
				}
				preference.setEnabled(true);
				updateCheckSummary();
			});
			return true;
		});
	}

	private void updateCheckSummary() {
		if (mCheckForUpdates == null || getActivity() == null) {
			return;
		}
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		String latest = preferences.getString(GitHubUpdateChecker.PREF_LAST_VERSION, "");
		long lastCheck = preferences.getLong(GitHubUpdateChecker.PREF_LAST_CHECK, 0);
		String checked = lastCheck == 0
				? getString(R.string.update_never_checked)
				: DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
						.format(new Date(lastCheck));
		if (!latest.isEmpty() &&
				VersionComparator.compare(BuildConfig.VERSION_NAME, latest) >= 0) {
			mCheckForUpdates.setSummary(getString(
					R.string.update_status_current_summary,
					BuildConfig.VERSION_NAME,
					checked));
		} else {
			String latestStatus = latest.isEmpty()
					? getString(R.string.update_latest_unknown)
					: latest;
			mCheckForUpdates.setSummary(getString(
					R.string.update_status_summary,
					BuildConfig.VERSION_NAME,
					latestStatus,
					checked));
		}
	}

    @Override
	public void onResume() {
        super.onResume();
        configureQuickSettingsPreference();
        updateCheckSummary();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
    	Preference pref = findPreference(key);
		if (pref instanceof ListPreference) {
			/* update all spinner prefs so the summary shows the current value */
			ListPreference lpref = (ListPreference)pref;
			lpref.setValue(sp.getString(key, ""));
			pref.setSummary(lpref.getEntry());
		}
		if (ProfileManager.QUICK_SETTINGS_PROFILE.equals(key)) {
			requestQuickSettingsRefresh();
		}
    }

	private void configureQuickSettingsPreference() {
		ListPreference preference = (ListPreference)findPreference(
				ProfileManager.QUICK_SETTINGS_PROFILE);
		if (preference == null) {
			return;
		}

		List<CharSequence> entries = new ArrayList<CharSequence>();
		List<CharSequence> values = new ArrayList<CharSequence>();
		entries.add(getString(R.string.quick_settings_last_used));
		values.add(ProfileManager.QUICK_SETTINGS_LAST_USED);
		entries.add(getString(R.string.quick_settings_choose_each_time));
		values.add(ProfileManager.QUICK_SETTINGS_CHOOSE);

		List<VpnProfile> profiles = new ArrayList<VpnProfile>(ProfileManager.getProfiles());
		Collections.sort(profiles);
		for (VpnProfile profile : profiles) {
			entries.add(profile.getName());
			values.add(profile.getUUIDString());
		}

		preference.setEntries(entries.toArray(new CharSequence[0]));
		preference.setEntryValues(values.toArray(new CharSequence[0]));
		ProfileManager.getQuickSettingsProfileUUID();
		preference.setValue(ProfileManager.getQuickSettingsProfileMode());
		preference.setSummary(preference.getEntry());
	}

	private void requestQuickSettingsRefresh() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && getActivity() != null) {
			TileService.requestListeningState(getActivity(),
					new ComponentName(getActivity(), QuickSettingsTileService.class));
		}
	}

	private void setClearApiSummary() {
		Preference clearapi = findPreference("clearapi");

		if(mExtapp.getExtAppList().isEmpty()) {
			clearapi.setEnabled(false);
			clearapi.setSummary(R.string.no_external_app_allowed);
		} else { 
			clearapi.setEnabled(true);
			clearapi.setSummary(getString(R.string.allowed_apps,getExtAppList(", ")));
		}
	}

	private String getExtAppList(String delim) {
		ApplicationInfo app;
		PackageManager pm = getActivity().getPackageManager();

		String applist=null;
		for (String packagename : mExtapp.getExtAppList()) {
			try {
				app = pm.getApplicationInfo(packagename, 0);
				if (applist==null)
					applist = "";
				else
					applist += delim;
				applist+=app.loadLabel(pm);

			} catch (NameNotFoundException e) {
				// App not found. Remove it from the list
				mExtapp.removeApp(packagename);
			}
		}

		return applist;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) { 
		if(preference.getKey().equals("clearapi")){
			Builder builder = new AlertDialog.Builder(getActivity());
			builder.setPositiveButton(R.string.clear, this);
			builder.setNegativeButton(android.R.string.cancel, null);
			builder.setMessage(getString(R.string.clearappsdialog,getExtAppList("\n")));
			builder.show();
		}
			
		return true;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if( which == Dialog.BUTTON_POSITIVE){
			mExtapp.clearAllApiApps();
			setClearApiSummary();
		}
	}



}
