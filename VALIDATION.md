# Special VPN 1.6.4 — validation

Package: com.specialvpn.app. Base versionCode: 777; universal versionCode: 5077700.

## Executed

Targeted DashboardSubscriptionsTest and SpecialUpdateTest passed. They cover successful manual/group refresh dispatching onlyTcp=false after refresh/reload; failed, skipped, missing and empty updates; no duplicate test after quiet app-entry refresh; existing entry cancellation/throttling; trusted SpecialVPN release paths and rejection of debug/upstream/foreign downloads.

Full JVM/Compose test run: 306 total, 302 passed, 4 failed, 0 skipped. All failures are unchanged RootProcessRunnerTest cases requiring an unavailable Windows sh executable. Full command therefore exits FAILED. No tests were disabled. Logs/XML are in verification/1.6.4.

Repository rename to sabialballa/SpecialVPN was observed in GitHub UI. Anonymous GitHub API for the new repository returned HTTP 200 and asset URLs under the new path. About source/privacy links and the release API/path validator now use that path. Earlier installed APKs have the old strict path validator; direct installation of 1.6.4 is required for a reliable migration.

## Not run

Physical-device real delay after manual refresh, refresh during active VPN/ongoing tests, activity recreation/background races, Android upgrade installation and subsequent in-app upgrade: no usable device/emulator is attached. No actual phone installation, native VPN connection or real customer server test is claimed. JVM tests use fixture data. No UI layout or accessibility semantics changed.

Provider daily history retains the 1.5.9 UTC behavior. No native binaries, permissions, protocols, package identity, signing key or stored profile format changed.

Debug/Release assembly and lint completed successfully. Lint: 0 errors, 373 warnings, 1 hint. Both APK signatures and ZIP alignment passed. All 60 native libraries match release 1.4.3 byte for byte; permission declarations unchanged. Font hashes/license and release DEX test-fixture exclusion, ELF, resource XML/locales and pinned native sources passed. See verification/1.6.4.

