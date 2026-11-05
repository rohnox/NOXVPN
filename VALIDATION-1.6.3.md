# Special VPN 1.6.3 — validation

Package com.specialvpn.app; base versionCode776. See CHANGES-1.6.3.md.

Targeted regressions cover Cisco profile browsing with no stop broadcast, confirmed/cancelled/duplicate connection gating, real-service entry testing with onlyTcp=false, stable GUID success/failure results after the task process persistence contract, entry throttling, empty groups and storage failure. Compose tests exercise real Controls navigation with Configuration tools absent and the manual updater accessible, plus positive/untested/failed delay presentation. Synthetic renders were inspected; they are not real customer measurements.

## Not run

Physical-device native VPN handover (browse without stopping Xray, cancelled consent, successful Cisco connect, repeated Connect, stopping during setup, activity recreation/background races), real server delay testing and Android upgrade installation:no usable Android device/emulator attached. A JVM test or APK assembly does not verify these scenarios. Proxy-only/root behavior uses the unchanged shared stop contract; protocols UI remains gated to VPN mode. TalkBack, D-pad, IME/device restoration and dynamic-color hardware checks remain Not run for the same blocker.

The public1.6.2 release and anonymous GitHub API were verified before this change. Current1.6.3 update metadata/asset verification is recorded separately after publication. For an end-to-end upgrade test, install1.6.2 first and relaunch it after1.6.3 is published. A current1.6.3 install should not show an update for itself.

Build/sign instructions:BUILD-FA.md and RELEASE-FA.md. Update publication instructions:UPDATES-FA.md. Keys and customer credentials are not included.

## Executed results

Fdroid Debug and Release assembly and lint completed successfully. Lint: 0 errors, 373 warnings, 1 hint. Final JVM/Compose run: 303 tests, 299 passed, 4 existing RootProcessRunnerTest failures caused by missing Windows sh executable; none skipped. Targeted regressions passed. Both APK signatures/alignment verified; package com.specialvpn.app, universal versionCode 5077600. All 60 native libraries match 1.4.3 byte for byte, permission declarations unchanged. ELF, bundled fonts, XML/locales, native source pins and release test-fixture exclusion checks passed. Logs and renders are under verification/1.6.3.

