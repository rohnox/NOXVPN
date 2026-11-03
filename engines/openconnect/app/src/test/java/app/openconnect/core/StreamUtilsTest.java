package app.openconnect.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Test;

public class StreamUtilsTest {
	@Test
	public void copyDoesNotTruncateFilesLargerThanOneBuffer() throws Exception {
		byte[] source = new byte[200000];
		for (int index = 0; index < source.length; index++) {
			source[index] = (byte)(index % 251);
		}

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		long copied = StreamUtils.copy(new ByteArrayInputStream(source), output);

		assertEquals(source.length, copied);
		assertArrayEquals(source, output.toByteArray());
	}
}
