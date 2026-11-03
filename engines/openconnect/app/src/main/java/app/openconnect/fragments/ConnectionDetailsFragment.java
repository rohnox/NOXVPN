/*
 * Copyright (c) 2026
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 */

package app.openconnect.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.infradead.libopenconnect.LibOpenConnect;

import app.openconnect.FragActivity;
import app.openconnect.R;
import app.openconnect.VpnProfile;
import app.openconnect.core.OpenConnectManagementThread;
import app.openconnect.core.OpenVpnService;
import app.openconnect.core.ProfileManager;
import app.openconnect.core.VPNConnector;

public class ConnectionDetailsFragment extends Fragment {

	private View mView;
	private VPNConnector mConn;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mView = inflater.inflate(R.layout.connection_details, container, false);
		mView.findViewById(R.id.details_log_row).setOnClickListener(
				view -> startFragment("LogFragment"));

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

	private void updateUI(OpenVpnService service) {
		if (service == null || mView == null) {
			return;
		}

		int state = service.getConnectionState();
		boolean connected = state == OpenConnectManagementThread.STATE_CONNECTED;
		writeText(R.id.details_connection_state, connected
				? getString(R.string.dashboard_connected)
				: state == OpenConnectManagementThread.STATE_DISCONNECTED
						? getString(R.string.dashboard_disconnected)
						: service.getConnectionStateName());
		setNetworkFieldsVisible(connected);

		if (connected && service.startTime != null) {
			writeText(R.id.details_connection_time, getString(
					R.string.connected_for,
					OpenVpnService.formatElapsedTime(service.startTime.getTime())));
			writeText(R.id.details_traffic, mConn.statsValid
					? mConn.getByteCountSummary()
					: getString(R.string.dashboard_traffic_loading));
		} else {
			writeText(R.id.details_connection_time,
					getString(R.string.connection_details_unavailable));
			writeText(R.id.details_traffic, "");
		}

		String server = service.serverName;
		VpnProfile profile = service.profile;
		if (profile == null) {
			profile = ProfileManager.get(service.getReconnectUUID());
		}
		if (TextUtils.isEmpty(server) && profile != null) {
			server = profile.mPrefs.getString("server_address", "");
		}
		writeText(R.id.details_server, valueOrUnknown(server));

		LibOpenConnect.IPInfo ip = service.ipInfo;
		writeText(R.id.details_local_ip4, ip == null ? getString(R.string.unknown)
				: valueOrUnknown(ip.addr));
		writeText(R.id.details_netmask, ip == null ? getString(R.string.unknown)
				: valueOrUnknown(ip.netmask));
		writeText(R.id.details_local_ip6, ip == null ? getString(R.string.unknown)
				: valueOrUnknown(ip.netmask6));
	}

	private void setNetworkFieldsVisible(boolean visible) {
		int visibility = visible ? View.VISIBLE : View.GONE;
		for (int id : new int[] {
				R.id.details_network_divider,
				R.id.details_local_ip4_row,
				R.id.details_local_ip4_divider,
				R.id.details_netmask_row,
				R.id.details_netmask_divider,
				R.id.details_local_ip6_row
		}) {
			mView.findViewById(id).setVisibility(visibility);
		}
	}

	private String valueOrUnknown(String value) {
		return TextUtils.isEmpty(value) ? getString(R.string.unknown) : value;
	}

	private void writeText(int id, CharSequence value) {
		((TextView)mView.findViewById(id)).setText(value);
	}

	private void startFragment(String fragmentName) {
		Intent intent = new Intent(getActivity(), FragActivity.class);
		intent.putExtra(FragActivity.EXTRA_FRAGMENT_NAME, fragmentName);
		startActivity(intent);
	}
}
