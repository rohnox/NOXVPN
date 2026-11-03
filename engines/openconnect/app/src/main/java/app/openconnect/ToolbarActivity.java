/*
 * Copyright (c) 2026
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package app.openconnect;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.google.android.material.appbar.MaterialToolbar;

public class ToolbarActivity extends Activity {
	protected MaterialToolbar mToolbar;
	private Fragment mToolbarMenuFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
		getWindow().setStatusBarColor(Color.TRANSPARENT);
		getWindow().setNavigationBarColor(Color.TRANSPARENT);
		boolean lightBars = getResources().getBoolean(R.bool.oc_light_status_bar);
		setLightSystemBars(lightBars);
	}

	protected void setLightSystemBars(boolean lightBars) {
		WindowInsetsControllerCompat controller =
				WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
		controller.setAppearanceLightStatusBars(lightBars);
		controller.setAppearanceLightNavigationBars(lightBars);
	}

	protected void setupToolbar(int toolbarId, CharSequence title, boolean showBack) {
		mToolbar = (MaterialToolbar)findViewById(toolbarId);
		mToolbar.setTitle(title);
		if (showBack) {
			mToolbar.setNavigationIcon(R.drawable.ic_arrow_back_24);
			mToolbar.setNavigationContentDescription(R.string.navigate_up);
			mToolbar.setNavigationOnClickListener(v -> finish());
		}
		mToolbar.setOnMenuItemClickListener(this::onToolbarMenuItemSelected);
		applySystemBarInsets();
	}

	protected void applySystemBarInsets() {
		ViewGroup content = (ViewGroup)findViewById(android.R.id.content);
		if (content == null || content.getChildCount() == 0) {
			return;
		}
		View root = content.getChildAt(0);
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

	@Override
	public void setTitle(CharSequence title) {
		super.setTitle(title);
		if (mToolbar != null) {
			mToolbar.setTitle(title);
		}
	}

	@Override
	public void setTitle(int titleId) {
		setTitle(getText(titleId));
	}

	protected void setToolbarMenuFragment(Fragment fragment) {
		mToolbarMenuFragment = fragment;
		invalidateToolbarMenu();
	}

	protected void invalidateToolbarMenu() {
		if (mToolbar == null) {
			return;
		}
		Menu menu = mToolbar.getMenu();
		menu.clear();
		onCreateToolbarMenu(menu);
		if (mToolbarMenuFragment != null) {
			mToolbarMenuFragment.onCreateOptionsMenu(menu, getMenuInflater());
		}
	}

	protected void onCreateToolbarMenu(Menu menu) {
	}

	protected boolean onToolbarMenuItemSelected(MenuItem item) {
		if (mToolbarMenuFragment != null && mToolbarMenuFragment.onOptionsItemSelected(item)) {
			return true;
		}
		return onOptionsItemSelected(item);
	}
}
