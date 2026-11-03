/*
 * Copyright (c) 2026 OConnect contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package app.openconnect;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.view.View;
import android.view.Window;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public final class SystemBarInsets {
	private SystemBarInsets() {
	}

	public static void applyToDialog(Activity activity, Dialog dialog) {
		if (dialog == null || dialog.getWindow() == null) {
			return;
		}

		Window window = dialog.getWindow();
		WindowCompat.setDecorFitsSystemWindows(window, false);
		window.setStatusBarColor(Color.TRANSPARENT);
		window.setNavigationBarColor(Color.TRANSPARENT);
		WindowInsetsControllerCompat controller =
				WindowCompat.getInsetsController(window, window.getDecorView());
		boolean lightBars = activity.getResources().getBoolean(R.bool.oc_light_status_bar);
		controller.setAppearanceLightStatusBars(lightBars);
		controller.setAppearanceLightNavigationBars(lightBars);

		View root = dialog.findViewById(android.R.id.content);
		if (root == null) {
			root = window.getDecorView();
		}
		applyToView(root);
	}

	private static void applyToView(View root) {
		int initialLeft = root.getPaddingLeft();
		int initialTop = root.getPaddingTop();
		int initialRight = root.getPaddingRight();
		int initialBottom = root.getPaddingBottom();
		ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
			Insets insets = windowInsets.getInsets(
					WindowInsetsCompat.Type.systemBars() |
							WindowInsetsCompat.Type.displayCutout());
			view.setPadding(
					initialLeft + insets.left,
					initialTop + insets.top,
					initialRight + insets.right,
					initialBottom + insets.bottom);
			return windowInsets;
		});
		ViewCompat.requestApplyInsets(root);
	}
}
