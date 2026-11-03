/*
 * Copyright (c) 2026 OConnect contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package app.openconnect.core;

import java.util.Collection;

public final class QuickSettingsProfileSelection {
	private QuickSettingsProfileSelection() {
	}

	public static String resolve(String configuredValue, String lastUsedUuid,
			Collection<String> availableUuids) {
		if (ProfileManager.QUICK_SETTINGS_CHOOSE.equals(configuredValue)) {
			return null;
		}

		String candidate = ProfileManager.QUICK_SETTINGS_LAST_USED.equals(configuredValue)
				? lastUsedUuid : configuredValue;
		if (candidate == null || candidate.equals("")) {
			return null;
		}
		return availableUuids.contains(candidate) ? candidate : null;
	}
}
