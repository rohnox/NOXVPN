/*
 * Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package app.openconnect;

import android.app.Activity;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.ListView;

public final class PreferenceScreenStyler {

	private PreferenceScreenStyler() {
	}

	public static void apply(PreferenceFragment fragment) {
		View root = fragment.getView();
		Activity activity = fragment.getActivity();
		if (root == null || activity == null) {
			return;
		}
		root.post(() -> {
			if (fragment.getActivity() == null) {
				return;
			}
			ListView list = (ListView)root.findViewById(android.R.id.list);
			if (list == null) {
				return;
			}
			int horizontalPadding = dp(activity, 16);
			list.setDivider(null);
			list.setDividerHeight(0);
			list.setClipToPadding(false);
			list.setPadding(horizontalPadding, 0, horizontalPadding, dp(activity, 24));
			list.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		});
	}

	private static int dp(Activity activity, int value) {
		return Math.round(value * activity.getResources().getDisplayMetrics().density);
	}
}
