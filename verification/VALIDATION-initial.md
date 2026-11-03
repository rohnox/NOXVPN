# Validation — 2026-11-02

Target: iSpeedTopVPN APP, v2rayNG 2.3.14, F-Droid Universal. This is a source delivery with an unsuccessful build, not an install-tested application.

## Passed checks

- Official repository cloned; release tag 2.3.14 resolved to aa4b05503a504a3f2e43818e6ee8ebf1caaa3df0. Release metadata marks it non-prerelease.
- Inspected upstream root/UI/service AGENTS guides; fetched upstream/master and instruction comparison showed no differences. No instruction files modified.
- Reviewed GPL-3.0 LICENSE, app Gradle configuration, Compose state/actions, daemon lifecycle, MessageHelper, notification sampler, configuration statistics controls, manifest and shortcuts.
- All 127 resource XML files and AndroidManifest.xml parse as XML. This does **not** replace AAPT resource linking, compilation or Android lint.
- Added resource keys exist in all 10 configured locales plus default resources. New Persian and English text is supplied; other locales use the English text for new keys and retain their existing upstream translations.
- Static checks confirm application ID and all four static shortcuts use com.ispeedtopvpn.app. Original Kotlin namespace is retained for JNI and serialized-class compatibility. Runtime actions and FileProvider authorities already derive from applicationId upstream.
- Original root LICENSE retained unchanged; copy included as an app asset. Visible fork attribution added to drawer and About. Upstream notices and third-party source licenses retained.
- Every recursive native source tree was downloaded and its revision matched its parent gitlink; revisions are in verification/static-checks.log.
- `git diff --check` passed after changes. No user configurations or release signing key were used or added.

## Executed but unsuccessful

Gradle wrapper 9.8.2 was downloaded and executed using locally provisioned Temurin JDK 17. The upstream CI specifies JDK 21, so use JDK 21 for reproduction. The attempts below failed at project configuration, before Kotlin compilation, tests, SDK checks or native packaging:

```text
gradlew.bat :app:assemblePlaystoreDebug
gradlew.bat :app:testPlaystoreDebugUnitTest :app:compilePlaystoreDebugKotlin :app:assemblePlaystoreDebug
gradlew.bat :app:testFdroidDebugUnitTest :app:compileFdroidDebugKotlin :app:assembleFdroidDebug
```

Common error: Android application plugin 9.4.1 could not be resolved from Google, Maven Central or Gradle Plugin Portal. A direct HTTP HEAD request to `https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/9.4.1/gradle-9.4.1.pom` returned 404. See recorded logs in verification/. No fallback downgrade, prebuilt upstream APK relabeling or invented build success was used.

The first attempt to initialize submodules through Git's MSYS shell failed with Windows NtCreateDirectoryObject access denied. Direct Git clones/checkouts then successfully populated all recursive trees at the exact revisions. Android command-line tools download also returned 404; SDK/NDK installation and native builds were not completed.

## Not run

- `:app:testFdroidDebugUnitTest --tests com.v2ray.ang.dto.SessionTrafficTest` and `--tests com.v2ray.ang.ui.main.SessionDurationTest`: blocked by plugin resolution. Tests cover unavailable initial stats, accumulation, negative counter clamping, disabled availability and duration boundaries. These test files are supplied, not claimed passing.
- Full `:app:testFdroidDebugUnitTest`, `:app:compileFdroidDebugKotlin`, `:app:assembleFdroidDebug`: invoked, but their task bodies did not run because configuration failed. Final source remains uncompiled. Re-run after the environment resolves upstream tool artifacts.
- `:app:lintFdroidDebug`: blocked by the same configuration failure. Play Store tasks were also attempted and blocked; final target is F-Droid.
- HEV compilation, libv2ray AAR packaging, four-ABI ZIP inspection and APK installation: missing SDK/NDK inputs and no assembled APK. Full reproducible steps supplied in BUILD-FA.md and scripts/build-ispeed.sh.
- Emulator/device: cold start, VPN permission accept/deny, notification permission, repeated start, stop/repeated stop, stop during setup, revoke, process death, restart, network handover, screen-off/on, session-counter reset/recovery, notification stop/restart actions, proxy-only and root modes. No Android emulator, rooted device or server credentials available. Shared lifecycle implementations were retained; the sampling change requires these runtime checks.
- UI: Persian RTL and English LTR, light/dark and dynamic color enabled/disabled, single/double-column lists, small screens, landscape, large fonts, touch/keyboard/D-pad activation, TalkBack labels/order and semantics tree, recreation/restoration, empty/loading/error/populated state presentation. No Compose rendering or accessibility success is claimed.
- Import: link/clipboard, local file, QR, subscriptions update/replacement, latency/TCP/real ping, protocol handshakes, DNS/routing, persistence after reopening and live traffic totals: source paths retained but not exercised on device. No live server or subscription was invented.
- Release build, key generation, signing and certificate verification: not executed. Instructions use a private key outside the repository.

## Practical limits

Universal covers the four declared CPU ABIs; it requires Android 7/API 24 or newer. It does not guarantee compatibility with every device or Android vendor. New dashboard bytes are daemon-reported aggregate outbound traffic rather than billed device usage; proxy chains can count traffic at multiple outbounds. Stats disabled/unavailable display an em dash. Custom raw configs need the upstream statistics policy enabled. A running core is not proof of working Internet; use the existing connection test. The displayed server is the selected configuration, which can differ from the currently running one until restart. No security audit or dependency vulnerability scan is claimed.
