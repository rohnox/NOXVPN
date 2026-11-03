/*
 * Adapted from OpenVPN for Android
 * Copyright (c) 2012-2013, Arne Schwabe
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

package app.openconnect;

import android.content.Intent;
import android.os.Bundle;
import app.openconnect.core.OpenVpnService;
import app.openconnect.core.VPNConnector;
import app.openconnect.fragments.*;
import app.openconnect.update.GitHubUpdateChecker;

public class MainActivity extends ToolbarActivity {

	public static final String TAG = "OConnect";

	private VPNConnector mConn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		if (!app.openconnect.core.EmbeddedRuntime.isInitialized()) {
            super.onCreate(null);
            app.openconnect.core.EmbeddedRuntime.returnToHost(this);
            return;
        }
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_dashboard);
		applySystemBarInsets();
		setLightSystemBars(false);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.replace(R.id.content_frame, new StatusFragment())
					.commit();
		}

		FeedbackFragment.recordUse(this, false);
	}

	private void updateUI(OpenVpnService service) {
		service.startActiveDialog(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
        if (isFinishing() || !app.openconnect.core.EmbeddedRuntime.isInitialized()) return;
		// Fork updates are distributed by the host; do not contact the upstream app updater.

		mConn = new VPNConnector(this, true) {
			@Override
			public void onUpdate(OpenVpnService service) {
				updateUI(service);
			}
		};
	}

	@Override
	protected void onPause() {
		if (mConn != null) {
			mConn.stopActiveDialog();
			mConn.unbind();
		}
		super.onPause();
	}

	public void showLogTab() {
		Intent intent = new Intent(this, FragActivity.class);
		intent.putExtra(FragActivity.EXTRA_FRAGMENT_NAME, "LogFragment");
		startActivity(intent);
	}

	public void retryLastConnection() {
		if (mConn != null && mConn.service != null &&
				mConn.service.getReconnectUUID() != null) {
			mConn.service.startReconnectActivity(this);
		}
	}
}
