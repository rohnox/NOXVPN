package app.openconnect.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VersionComparatorTest {
	@Test
	public void handlesGitHubTagsAndMissingPatchNumbers() {
		assertEquals(0, VersionComparator.compare("v1.12", "1.12.0"));
		assertTrue(VersionComparator.compare("v1.13.0", "1.12.9") > 0);
	}

	@Test
	public void stableReleaseWinsOverPrerelease() {
		assertTrue(VersionComparator.compare("1.13.0", "1.13.0-rc1") > 0);
		assertTrue(VersionComparator.compare("1.13.0-beta2", "1.13.0") < 0);
	}

	@Test
	public void ignoresBuildMetadata() {
		assertEquals(0, VersionComparator.compare("1.13.0+12", "v1.13.0+99"));
	}
}
