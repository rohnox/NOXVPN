/*
 * Copyright (c) 2013, Kevin Cernekee
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

package app.openconnect.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import app.openconnect.VpnProfile;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

public class ProfileManager {
	public static final String TAG = "OConnect";

	public static String fileSelectKeys[] =
		{ "ca_certificate", "user_certificate", "private_key", "custom_csd_wrapper" };

	private static final String PROFILE_PFX = "profile-";
	public static final String QUICK_SETTINGS_PROFILE = "quick_settings_profile";
	public static final String QUICK_SETTINGS_LAST_USED = "__last_used__";
	public static final String QUICK_SETTINGS_CHOOSE = "__choose__";
	public static final String LAST_USED_PROFILE = "service_mUUID";
	private static HashMap<String,VpnProfile> mProfiles;

	private static Context mContext;
	private static SharedPreferences mAppPrefs;

	private static final String ON_BOOT_PROFILE = "onBootProfile";
	private static final String RESTART_ON_BOOT = "restartvpnonboot" + "_FIXME"; // FIXME

	private static VpnProfile mLastConnectedVpn=null;

	public static void init(Context context) {
		mContext = context;
		mAppPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		mProfiles = new HashMap<String, VpnProfile>();

		File prefsdir = new File(context.getApplicationInfo().dataDir, "shared_prefs");
		if (prefsdir.exists() && prefsdir.isDirectory()) {
			String[] preferenceFiles = prefsdir.list();
			if (preferenceFiles == null) {
				return;
			}
			for (String s : preferenceFiles) {
				if (s.startsWith(PROFILE_PFX)) {
					String preferenceName = s.replaceFirst("\\.xml$", "");
					SharedPreferences p = context.getSharedPreferences(preferenceName,
							Activity.MODE_PRIVATE);
					VpnProfile entry = new VpnProfile(p);
					if (!entry.isValid()) {
						Log.w(TAG, "removing bogus profile '" + s + "'");
						deleteSharedPreferences(preferenceName);
					} else {
						mProfiles.put(entry.getUUIDString(), entry);
					}
				}
			}
		}
	}

	public synchronized static Collection<VpnProfile> getProfiles() {
		return new ArrayList<VpnProfile>(mProfiles.values());
	}

	public synchronized static VpnProfile get(String key) {
		return key == null ? null : mProfiles.get(key);
	}

	public static String getPrefsName(String uuid) {
		return PROFILE_PFX + uuid;
	}

	private static String capitalize(String in) {
		if (in.length() <= 4) {
			// These are almost always abbreviations
			return in.toUpperCase(Locale.getDefault());
		} else {
			// Longer names -> capitalize first letter only
			return Character.toUpperCase(in.charAt(0)) + in.substring(1);
		}
	}

	private static String makeProfName(String s, int index) {
		String orig = s;
		String suffix;

		if (index > 0) {
			suffix = " (" + index + ")";
		} else {
			suffix = "";
		}

		// leave IP addresses alone
		if ((s.matches("[0-9.]+") && s.matches(".*\\..*")) ||
			(s.matches("[0-9a-fA-F:]+") && s.matches(".*:.*"))) {
			return s + suffix;
		}

		// try to parse the hostname out of an URL
		if (s.matches(".*/.*")) {
			if (!s.matches("https://.*")) {
				s = "https://" + s;
			}

			s = Uri.parse(s).getHost();
			if (s == null || s.trim().equals("")) {
				// failed
				return orig + suffix;
			}
		}

		String ss[] = s.split("\\.");
		if (ss.length < 2) {
			// unqualified hostname (or junk)
			return capitalize(s) + suffix;
		}

		// Try to find the first private part of the FQDN.
		// This should probably use something like the Apache Public Suffix List, but it's not
		// worth the trouble right now.
		int i = ss.length - 1;
		if (ss[i].length() <= 2 && i > 1) {
			// if the TLD looks like a country code, check for a public SLD like .co
			String sld = ss[i - 1];
			if (sld.length() <= 2 || sld.equals("com")) {
				i--;
			}
		}

		s = ss[i - 1];
		if (s.length() < 2) {
			return orig + suffix;
		} else {
			return capitalize(s) + suffix;
		}
	}

	public synchronized static VpnProfile create(String hostname) {
		String profName;

		// generate a non-conflicting name if necessary
		for (int i = 0; ; i++) {
			profName = makeProfName(hostname, i);
			if (getProfileByName(profName) == null) {
				break;
			}
		}

		String uuid = UUID.randomUUID().toString();
		SharedPreferences p = mContext.getSharedPreferences(getPrefsName(uuid), Activity.MODE_PRIVATE);
		p.edit().putString("server_address", hostname).commit();

		VpnProfile profile = new VpnProfile(p, uuid, profName);
		mProfiles.put(uuid, profile);
		return profile;
	}

	public synchronized static VpnProfile getProfileByName(String name) {
		String lower = name.toLowerCase(Locale.getDefault());
		for (VpnProfile vpnp : mProfiles.values()) {
			String vname = vpnp.getName().toLowerCase(Locale.getDefault());
			if(vname.equals(lower)) {
				return vpnp;
			}
		}
		return null;
	}

	private static String getCertFilename(VpnProfile profile, String key) {
		return 	"cert." + profile.getUUIDString() + "." + key;
	}

	public static String getCertPath() {
		return mContext.getFilesDir().getPath() + File.separator;
	}

	public synchronized static void deleteFilePref(VpnProfile profile, String key) {
		String oldVal = profile.mPrefs.getString(key, null);
		if (getCertFilename(profile, key).equals(oldVal)) {
			File f = new File(getCertPath() + oldVal);
			if (!f.delete()) {
				Log.w(TAG, "error deleting " + oldVal);
			}
		}
	}

	public synchronized static String storeFilePref(VpnProfile profile, String key, String fromPath) {
		try (FileInputStream in = new FileInputStream(fromPath)) {
			return storeFilePref(profile, key, in, fromPath);
		} catch (IOException e) {
			Log.e(TAG, "error opening " + fromPath, e);
			return null;
		}
	}

	public synchronized static String storeFilePref(VpnProfile profile, String key,
			ContentResolver resolver, Uri uri) {
		try (InputStream in = resolver.openInputStream(uri)) {
			if (in == null) {
				return null;
			}
			return storeFilePref(profile, key, in, uri.toString());
		} catch (IOException | SecurityException e) {
			Log.e(TAG, "error opening " + uri, e);
			return null;
		}
	}

	private static String storeFilePref(VpnProfile profile, String key, InputStream in,
			String sourceName) {
		String filename = getCertFilename(profile, key);
		String toPath = getCertPath() + filename;
		String tempPath = toPath + ".tmp";

		try (FileOutputStream out = new FileOutputStream(tempPath)) {
			File outFile = new File(tempPath);
			StreamUtils.copy(in, out);
			outFile.setExecutable(true);

			File destination = new File(toPath);
			File backup = new File(toPath + ".bak");
			backup.delete();
			if (destination.exists() && !destination.renameTo(backup)) {
				throw new IOException("Unable to preserve " + destination);
			}
			if (!outFile.renameTo(destination)) {
				backup.renameTo(destination);
				throw new IOException("Unable to move imported file into place");
			}
			backup.delete();
			return filename;
		} catch (IOException e) {
			Log.e(TAG, "error copying " + sourceName + " -> " + toPath, e);
			new File(tempPath).delete();
			return null;
		}
	}

	private static boolean deleteSharedPreferences(String preferenceName) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			return mContext.deleteSharedPreferences(preferenceName);
		}

		SharedPreferences preferences = mContext.getSharedPreferences(
				preferenceName, Activity.MODE_PRIVATE);
		preferences.edit().clear().commit();
		File file = new File(mContext.getApplicationInfo().dataDir + File.separator +
				"shared_prefs" + File.separator + preferenceName + ".xml");
		return !file.exists() || file.delete();
	}

	public synchronized static boolean delete(String uuid) {
		VpnProfile profile = get(uuid);
		if (profile == null) {
			Log.w(TAG, "error looking up profile " + uuid);
			return false;
		}

		for (String key : fileSelectKeys) {
			deleteFilePref(profile, key);
		}

		mProfiles.remove(uuid);

		String selectedProfile = mAppPrefs.getString(
				QUICK_SETTINGS_PROFILE, QUICK_SETTINGS_LAST_USED);
		SharedPreferences.Editor appPreferences = mAppPrefs.edit();
		if (uuid.equals(selectedProfile)) {
			appPreferences.putString(QUICK_SETTINGS_PROFILE, QUICK_SETTINGS_CHOOSE);
		}
		if (uuid.equals(mAppPrefs.getString(LAST_USED_PROFILE, null))) {
			appPreferences.remove(LAST_USED_PROFILE);
		}
		appPreferences.apply();

		if (deleteSharedPreferences(PROFILE_PFX + uuid)) {
			Log.i(TAG, "deleted profile " + uuid);
			return true;
		} else {
			Log.w(TAG, "error deleting profile " + uuid);
			return false;
		}
	}

	public synchronized static void setConnectedVpnProfileDisconnected() {
		mLastConnectedVpn = null;
		mAppPrefs.edit()
			.remove(ON_BOOT_PROFILE)
			.commit();
	}

	public synchronized static void setConnectedVpnProfile(VpnProfile connectedProfile) {
		mLastConnectedVpn = connectedProfile;
		mAppPrefs.edit()
			.putString(ON_BOOT_PROFILE, connectedProfile.getUUIDString())
			.commit();
	}

	public synchronized static VpnProfile getOnBootProfile() {
		if (!mAppPrefs.getBoolean(RESTART_ON_BOOT, false)) {
			return null;
		}
		return get(mAppPrefs.getString(ON_BOOT_PROFILE, null));
	}

	public static VpnProfile getLastConnectedVpn() {
		return mLastConnectedVpn;
	}

	public synchronized static String getQuickSettingsProfileMode() {
		return mAppPrefs.getString(QUICK_SETTINGS_PROFILE, QUICK_SETTINGS_LAST_USED);
	}

	public synchronized static String getQuickSettingsProfileUUID() {
		String configuredValue = getQuickSettingsProfileMode();
		Set<String> availableUuids = new HashSet<String>(mProfiles.keySet());
		String resolved = QuickSettingsProfileSelection.resolve(configuredValue,
				mAppPrefs.getString(LAST_USED_PROFILE, null), availableUuids);

		if (!QUICK_SETTINGS_LAST_USED.equals(configuredValue) &&
				!QUICK_SETTINGS_CHOOSE.equals(configuredValue) &&
				resolved == null) {
			mAppPrefs.edit()
					.putString(QUICK_SETTINGS_PROFILE, QUICK_SETTINGS_CHOOSE)
					.apply();
		}
		return resolved;
	}

	public synchronized static VpnProfile getQuickSettingsProfile() {
		return get(getQuickSettingsProfileUUID());
	}

}
