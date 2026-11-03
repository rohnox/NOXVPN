/*
 * Copyright (c) 2026 OConnect contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package app.openconnect.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class StreamUtils {
	private static final int BUFFER_SIZE = 65536;

	private StreamUtils() {
	}

	public static long copy(InputStream input, OutputStream output) throws IOException {
		byte[] buffer = new byte[BUFFER_SIZE];
		long total = 0;
		int length;
		while ((length = input.read(buffer)) != -1) {
			output.write(buffer, 0, length);
			total += length;
		}
		return total;
	}
}
