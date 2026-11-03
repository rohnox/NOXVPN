/*
 * Copyright (c) 2026 OConnect contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package app.openconnect.update;

import java.util.Locale;

public final class VersionComparator {
	private VersionComparator() {
	}

	public static int compare(String left, String right) {
		Version leftVersion = Version.parse(left);
		Version rightVersion = Version.parse(right);

		int length = Math.max(leftVersion.parts.length, rightVersion.parts.length);
		for (int index = 0; index < length; index++) {
			long leftPart = index < leftVersion.parts.length ? leftVersion.parts[index] : 0;
			long rightPart = index < rightVersion.parts.length ? rightVersion.parts[index] : 0;
			if (leftPart != rightPart) {
				return Long.compare(leftPart, rightPart);
			}
		}

		if (leftVersion.preRelease == null && rightVersion.preRelease != null) {
			return 1;
		}
		if (leftVersion.preRelease != null && rightVersion.preRelease == null) {
			return -1;
		}
		if (leftVersion.preRelease == null) {
			return 0;
		}
		return leftVersion.preRelease.compareTo(rightVersion.preRelease);
	}

	private static final class Version {
		private final long[] parts;
		private final String preRelease;

		private Version(long[] parts, String preRelease) {
			this.parts = parts;
			this.preRelease = preRelease;
		}

		private static Version parse(String input) {
			String value = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
			if (value.startsWith("v")) {
				value = value.substring(1);
			}

			int buildIndex = value.indexOf('+');
			if (buildIndex >= 0) {
				value = value.substring(0, buildIndex);
			}

			String preRelease = null;
			int preReleaseIndex = value.indexOf('-');
			if (preReleaseIndex >= 0) {
				preRelease = value.substring(preReleaseIndex + 1);
				value = value.substring(0, preReleaseIndex);
			}

			String[] rawParts = value.split("\\.");
			long[] parts = new long[rawParts.length];
			for (int index = 0; index < rawParts.length; index++) {
				String numeric = rawParts[index].replaceFirst("[^0-9].*$", "");
				try {
					parts[index] = numeric.isEmpty() ? 0 : Long.parseLong(numeric);
				} catch (NumberFormatException ignored) {
					parts[index] = 0;
				}
			}
			return new Version(parts, preRelease);
		}
	}
}
