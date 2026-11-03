# Final validation — auto-connect 1.2.0 — 2026-11-02

Base: official v2rayNG 2.3.14, commit aa4b05503a504a3f2e43818e6ee8ebf1caaa3df0. Original GPL and author notices retained. Version 1.2.0 / F-Droid universal code 5075600; package com.ispeedtopvpn.app. Android minimum API 24, target 37.

## Executed checks

- Final compileFdroidDebugKotlin and assembleFdroidDebug succeeded. APK signature verification and 16 KiB ZIP alignment passed. All four ABIs contain the three expected native libraries and GPL asset. Labels match the brand. Debug certificate matches previous releases; actual installation/update not tested.
- APK SHA256: aaa1469a3592ef7b706a1554e0770be693c81776229e3b7c939fbf76165ba490.
- Targeted main suite: 44 tests, zero failures. Includes 20 new selection/model tests: ranking positive fresh responses, empty/all-failed inputs, deadline, duplicate/stale results, cancel, manual override, changed configuration, persistence acknowledgement, activation consumption and existing connection state.
- Full suite: 156 tests, 152 passed, four failed. All four are existing RootProcessRunnerTest cases: Windows cannot find sh (CreateProcess error 2). Tests were not disabled. See auto-full-tests.log and auto-full-tests XML.
- lintFdroidDebug succeeded with warnings, zero errors. Removed redundant partial values-en resources; default English resources remain the English source. XML parsing, locale keys, identity, GPL unchanged, git diff --check and recursive native revision checks passed.
- Build inputs/toolchain remain as documented in BUILD-FA.md and verification/VALIDATION-before-auto.md. Added kotlinx-coroutines-test 1.11.0 only to test dependencies. In-process Kotlin and preloaded Byte Buddy test agent were host workarounds, not shipped APK dependencies.

## Behavior

Auto-connect tests imported configurations using the existing real-ping backend (onlyTcp=false). It ranks successful positive measurements, revalidates unchanged configuration and persisted selection, then uses the existing launcher/VPN permission flow. No cached latency is substituted for a missing result. Probe deadline is 120 seconds; partial successful results may be selected. Cancellation/manual actions invalidate old request results. Backgrounding cancels auto-selection except configuration changes. Rotation retention is implemented through ViewModel ownership.

No actual running state is fabricated. A successful test/selection does not itself set connected state. Existing service events remain authoritative. Current VPN is not deliberately stopped while probing. Lowest measured latency does not guarantee country, bandwidth or future quality. Subscription quota still comes from provider headers; session chart still uses actual daemon snapshots. No credentials, endpoints, trackers or advertising were added.

## Not run / limitations

No usable Android device/emulator or real subscription was available. Actual installation, rendering, RTL, accessibility, lifecycle/rotation/background races, permissions accept/deny, real VPN auto-connect/cancel/reconnect, server reachability, subscription quota matching, QR/file/clipboard import, DNS/routing, protocol interoperability, root/proxy modes and network handover were not exercised end to end. JVM tests and compilation do not replace these device checks.

Release build/signing, Play Store variant and vulnerability audit were not run. This is a Debug APK. Release signing instructions are included; no private signing key is included. See AUTO-CONNECT-FA.md for user guidance and verification/VALIDATION-before-auto.md for prior dashboard implementation details. Older reports are historical.
