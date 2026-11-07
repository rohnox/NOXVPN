# Special VPN 1.6.8 — validation

Package com.specialvpn.app; base versionCode 781, universal 5078100. Native libraries, signing identity and profiles are unchanged.

## Reproduced crash and fix

The connected API 36 phone had repeated main-process BadParcelableException / ClassNotFoundException crashes while VpnHandoverService read stopReply from a Messenger Bundle sent by the daemon-process Quick Settings tile. The named class was MessageHelper's anonymous ResultReceiver subclass. UI-originated calls in the same process had masked this cross-process serialization problem.

The sender now parcels that callback into a framework ResultReceiver endpoint before adding it to an IPC payload. The owner-stop callback and the coordinator dispatch receipt use the same framework-only representation. Binder still routes results to the original callback, and actual native-stop acknowledgements, deadlines and dispatch ordering are retained. No class-loader relaxation or exception swallowing was added.

## Executed checks

- 27 targeted tests passed: 21 owner/IPC cases across Robolectric API 24, 31 and 35, and six handover-queue cases. The regression test writes and reads callback Bundles with the framework class loader and verifies delivery.
- Full suite: 375 tests, 371 passed, four unchanged RootProcessRunnerTest failures caused by the unavailable Windows sh executable, zero skipped. No tests were disabled. Robolectric 4.17's default ResultReceiver shadow bypasses remote Binder delivery; the three affected test fixtures use Android's actual ResultReceiver implementation instead. This test-only adapter is excluded from production.
- Signed Release installed over 1.6.7 on the user's API 36 arm64 phone without clearing data. The user tapped the Quick Settings toggle to connect: user confirmed connection without a crash, and ADB observed CoreVpnService active in the daemon process. The user tapped again to disconnect: user confirmed no crash, and ADB observed CoreVpnService absent. Quick Settings and passive OpenVPN status services remaining are not active VPN owners. The crash-buffer entry count stayed unchanged across both actions.

Raw phone logs, device identifiers and account/configuration data are private and excluded from this source package. Physical test actions were performed by the user because this device blocks injected touch events.

## Not run

Repeated rapid tile presses, lock-screen/consent-revoked and background-restriction scenarios on a physical device; physical proxy-only/root-mode toggle checks; other Android versions and vendors; the complete 12-direction native-engine handover matrix after this change. Simulated IPC checks do not establish these runtime behaviors. The device check confirms connect/disconnect and no new crash, not independent packet or internet-reachability measurement.

Final build and signature verification logs accompany this version under verification/1.6.8.

Normal Debug/Release assembly and lintFdroidDebug passed in 3m 23s. Lint: zero errors, 373 warnings, one hint. Both APK signatures and alignment passed. Final Release SHA-256: 3ea8192336e9c3cc219ac6883df69caca923e5d7329684d93e08ac25bf37aead.
