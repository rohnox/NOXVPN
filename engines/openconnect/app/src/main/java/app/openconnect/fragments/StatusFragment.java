/*
 * Copyright (c) 2013, Kevin Cernekee
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 */

package app.openconnect.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import app.openconnect.ConnectionEditorActivity;
import app.openconnect.FragActivity;
import app.openconnect.R;
import app.openconnect.VpnProfile;
import app.openconnect.api.GrantPermissionsActivity;
import app.openconnect.core.OpenConnectManagementThread;
import app.openconnect.core.OpenVpnService;
import app.openconnect.core.ProfileManager;
import app.openconnect.core.VPNConnector;
import app.openconnect.update.GitHubUpdateChecker;

public class StatusFragment extends Fragment {

	private static final int MENU_CHECK_UPDATES = 1;
	private static final int MENU_SECURID = 2;

	private View mView;
	private VPNConnector mConn;
	private MaterialButton mDisconnectButton;
	private VpnProfile mSelectedProfile;
	private int mConnectionState = OpenConnectManagementThread.STATE_DISCONNECTED;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mView = inflater.inflate(R.layout.status, container, false);
		mDisconnectButton = (MaterialButton)mView.findViewById(R.id.disconnect_button);

		styleWordmark();
		mDisconnectButton.setOnClickListener(view -> handlePrimaryAction());

		mView.findViewById(R.id.current_profile_row).setOnClickListener(
				view -> handleProfileRow());
		mView.findViewById(R.id.connection_details_row).setOnClickListener(
				view -> startFragment("ConnectionDetailsFragment"));
		mView.findViewById(R.id.dashboard_settings_row).setOnClickListener(
				view -> startFragment("GeneralSettings"));
		mView.findViewById(R.id.dashboard_help_row).setOnClickListener(
				view -> showHelpAndAbout());
		mView.findViewById(R.id.dashboard_overflow).setOnClickListener(
				this::showOverflowMenu);

		mConn = new VPNConnector(getActivity(), false) {
			@Override
			public void onUpdate(OpenVpnService service) {
				updateUI(service);
			}
		};

		return mView;
	}

	@Override
	public void onDestroyView() {
		if (mConn != null) {
			mConn.unbind();
		}
		super.onDestroyView();
	}

	private void styleWordmark() {
		TextView wordmarkView = (TextView)mView.findViewById(R.id.dashboard_wordmark);
		String wordmarkText = getString(R.string.app);
		SpannableString wordmark = new SpannableString(wordmarkText);
		int accentStart = wordmarkText.lastIndexOf("Next");
		if (accentStart >= 0) {
			wordmark.setSpan(
					new ForegroundColorSpan(ContextCompat.getColor(
							getActivity(), R.color.dashboard_accent)),
					accentStart,
					wordmarkText.length(),
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		wordmarkView.setText(wordmark);
	}

	private void updateUI(OpenVpnService service) {
		if (service == null || mView == null) {
			return;
		}

		int state = service.getConnectionState();
		mConnectionState = state;
		mSelectedProfile = resolveProfile(service);

		if (state == OpenConnectManagementThread.STATE_CONNECTED) {
			showConnectedState(service);
		} else if (state == OpenConnectManagementThread.STATE_DISCONNECTED) {
			showDisconnectedState(service);
		} else {
			showProgressState(service);
		}
	}

	private void showConnectedState(OpenVpnService service) {
		setStatusBadgeVisible(true);
		setConnectionDetailsVisible(true);
		setTrafficVisible(true);
		writeText(R.id.current_profile_label, getString(R.string.dashboard_current_profile));
		writeProfile(service);
			writeText(R.id.connection_state, getString(R.string.dashboard_connected));
			writeText(R.id.connection_time, service.startTime == null
					? getString(R.string.connection_status_progress_message)
					: OpenVpnService.formatElapsedTime(service.startTime.getTime()));
			writeText(R.id.connection_traffic, mConn.statsValid
					? mConn.getByteCountSummary()
					: getString(R.string.dashboard_traffic_loading));
			mDisconnectButton.setText(R.string.disconnect);
			mDisconnectButton.setIconResource(R.drawable.ic_power_settings_new_24);
	}

	private void showProgressState(OpenVpnService service) {
		setStatusBadgeVisible(false);
		setConnectionDetailsVisible(true);
		setTrafficVisible(true);
		writeText(R.id.current_profile_label, getString(R.string.dashboard_current_profile));
		writeProfile(service);
			writeText(R.id.connection_state, service.getConnectionStateName());
			writeText(R.id.connection_time,
					getString(R.string.connection_status_progress_message));
			writeText(R.id.connection_traffic, getServerName(service, mSelectedProfile));
			mDisconnectButton.setText(R.string.disconnect);
			mDisconnectButton.setIconResource(R.drawable.ic_close_24);
	}

	private void showDisconnectedState(OpenVpnService service) {
		boolean hasProfile = mSelectedProfile != null;
		setStatusBadgeVisible(false);
		setConnectionDetailsVisible(hasProfile);
		setTrafficVisible(false);
		writeText(R.id.connection_state, getString(R.string.dashboard_disconnected));
		writeText(R.id.current_profile_label, getString(R.string.dashboard_vpn_profiles));

		if (hasProfile) {
			writeText(R.id.connection_profile, mSelectedProfile.getName());
			writeText(R.id.connection_time, getString(R.string.dashboard_ready_to_connect));
			writeText(R.id.current_profile_name, mSelectedProfile.getName());
			writeText(R.id.current_profile_server,
					getServerName(service, mSelectedProfile));
			mDisconnectButton.setText(R.string.dashboard_connect);
			mDisconnectButton.setIconResource(R.drawable.ic_power_settings_new_24);
		} else {
			writeText(R.id.connection_profile, getString(R.string.dashboard_no_profiles));
			writeText(R.id.connection_time, getString(R.string.dashboard_add_profile_hint));
			writeText(R.id.current_profile_name, getString(R.string.dashboard_no_profiles));
			writeText(R.id.current_profile_server,
					getString(R.string.dashboard_add_profile_hint));
			mDisconnectButton.setText(R.string.empty_profiles_cta);
			mDisconnectButton.setIconResource(R.drawable.ic_add_24);
		}
	}

	private void writeProfile(OpenVpnService service) {
		String profileName = mSelectedProfile == null
				? getString(R.string.unknown)
				: mSelectedProfile.getName();
		writeText(R.id.connection_profile, profileName);
		writeText(R.id.current_profile_name, profileName);
		writeText(R.id.current_profile_server, getServerName(service, mSelectedProfile));
	}

	private VpnProfile resolveProfile(OpenVpnService service) {
		if (service.profile != null) {
			return service.profile;
		}
		VpnProfile reconnect = ProfileManager.get(service.getReconnectUUID());
		if (reconnect != null) {
			return reconnect;
		}
		List<VpnProfile> profiles = new ArrayList<VpnProfile>(
				ProfileManager.getProfiles());
		if (profiles.isEmpty()) {
			return null;
		}
		Collections.sort(profiles);
		return profiles.get(0);
	}

	private void setStatusBadgeVisible(boolean visible) {
		mView.findViewById(R.id.connection_status_badge).setVisibility(
				visible ? View.VISIBLE : View.GONE);
	}

	private void setConnectionDetailsVisible(boolean visible) {
		mView.findViewById(R.id.connection_details_divider).setVisibility(
				visible ? View.VISIBLE : View.GONE);
		mView.findViewById(R.id.connection_details_row).setVisibility(
				visible ? View.VISIBLE : View.GONE);
	}

	private void setTrafficVisible(boolean visible) {
		mView.findViewById(R.id.connection_traffic).setVisibility(
				visible ? View.VISIBLE : View.GONE);
	}

	private String getServerName(OpenVpnService service, VpnProfile profile) {
		String server = service.serverName;
		if (TextUtils.isEmpty(server) && profile != null) {
			server = profile.mPrefs.getString("server_address", "");
		}
		if (TextUtils.isEmpty(server)) {
			return getString(R.string.profile_server_not_set);
		}
		return server;
	}

	private void writeText(int id, CharSequence value) {
		TextView textView = (TextView)mView.findViewById(id);
		textView.setText(value);
	}

	private void handlePrimaryAction() {
		if (mConn == null || mConn.service == null) {
			return;
		}
		if (mConnectionState != OpenConnectManagementThread.STATE_DISCONNECTED) {
			mConn.service.stopVPN();
			return;
		}
		if (mSelectedProfile == null) {
			openProfiles(true);
			return;
		}
		Intent intent = new Intent(getActivity(), GrantPermissionsActivity.class);
		String pkg = getActivity().getPackageName();
		intent.putExtra(pkg + GrantPermissionsActivity.EXTRA_UUID,
				mSelectedProfile.getUUID().toString());
		intent.setAction(Intent.ACTION_MAIN);
		startActivity(intent);
	}

	private void handleProfileRow() {
		if (mConnectionState == OpenConnectManagementThread.STATE_DISCONNECTED) {
			openProfiles(false);
		} else {
			editCurrentProfile();
		}
	}

	private void editCurrentProfile() {
		if (mSelectedProfile == null) {
			return;
		}
		String prefix = getActivity().getPackageName();
		Intent intent = new Intent(getActivity(), ConnectionEditorActivity.class)
				.putExtra(prefix + ".profileUUID", mSelectedProfile.getUUID().toString())
				.putExtra(prefix + ".profileName", mSelectedProfile.getName());
		startActivity(intent);
	}

	private void openProfiles(boolean openAddProfile) {
		Intent intent = new Intent(getActivity(), FragActivity.class);
		intent.putExtra(FragActivity.EXTRA_FRAGMENT_NAME, "VPNProfileList");
		intent.putExtra(FragActivity.EXTRA_OPEN_ADD_PROFILE, openAddProfile);
		startActivity(intent);
	}

	private void startFragment(String fragmentName) {
		Intent intent = new Intent(getActivity(), FragActivity.class);
		intent.putExtra(FragActivity.EXTRA_FRAGMENT_NAME, fragmentName);
		startActivity(intent);
	}

	private void showHelpAndAbout() {
		CharSequence[] items = {
				getString(R.string.faq),
				getString(R.string.about_openconnect)
		};
		new AlertDialog.Builder(getActivity())
				.setTitle(R.string.dashboard_help_about)
				.setItems(items, (dialog, which) -> startFragment(
						which == 0 ? "FaqFragment" : "AboutFragment"))
				.show();
	}

	private void showOverflowMenu(View anchor) {
		PopupMenu popup = new PopupMenu(getActivity(), anchor);
		popup.getMenu().add(0, MENU_CHECK_UPDATES, 0, R.string.check_for_updates);
		popup.getMenu().add(0, MENU_SECURID, 1, R.string.securid_info);
		popup.setOnMenuItemClickListener(item -> {
			if (item.getItemId() == MENU_CHECK_UPDATES) {
				GitHubUpdateChecker.checkManually(getActivity());
				return true;
			}
			if (item.getItemId() == MENU_SECURID) {
				startFragment("TokenParentFragment");
				return true;
			}
			return false;
		});
		popup.show();
	}
}
