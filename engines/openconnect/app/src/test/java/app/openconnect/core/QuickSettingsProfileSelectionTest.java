package app.openconnect.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class QuickSettingsProfileSelectionTest {
	private final List<String> profiles = Arrays.asList("profile-a", "profile-b");

	@Test
	public void fixedProfileWinsOverLastUsedProfile() {
		assertEquals("profile-b", QuickSettingsProfileSelection.resolve(
				"profile-b", "profile-a", profiles));
	}

	@Test
	public void lastUsedModePreservesExistingBehavior() {
		assertEquals("profile-a", QuickSettingsProfileSelection.resolve(
				ProfileManager.QUICK_SETTINGS_LAST_USED, "profile-a", profiles));
	}

	@Test
	public void chooseModeOpensTheAppInsteadOfConnecting() {
		assertNull(QuickSettingsProfileSelection.resolve(
				ProfileManager.QUICK_SETTINGS_CHOOSE, "profile-a", profiles));
	}

	@Test
	public void deletedProfileCannotBeResolved() {
		assertNull(QuickSettingsProfileSelection.resolve(
				"deleted-profile", "profile-a", profiles));
	}
}
