# Final validation — custom dashboard 1.1.0 — 2026-11-02

## Built and verified

- `:app:assembleFdroidDebug` passed for the final source; `verification/dashboard-final-build.log` records BUILD SUCCESSFUL. No dependency was upgraded or downgraded for the redesign.
- Dedicated main Compose UI: overview, server cards, plan cards and controls with bottom navigation, new palettes and connection panel. Existing import/service/protocol handlers retained. Advanced protocol and settings forms remain based on the original project; this is a custom frontend on GPL source, not an independently invented VPN engine.
- `aapt2 dump badging`: com.ispeedtopvpn.app; versionName 1.1.0; versionCode 5075500; min SDK 24; target SDK 37. Correct application labels for default/English/Persian and all packaged locales.
- Universal APK contains nonempty libgojni.so, libhev-socks5-tunnel.so and libhevsockstun.so for arm64-v8a, armeabi-v7a, x86 and x86_64, plus the embedded GPL license.
- `apksigner verify --verbose --print-certs` and `zipalign -c -P 16 4` passed. Debug certificate matches the prior APK: SHA-256 f99539d83f0d6fa82e111a8e1106aafed898c68684a53c24b3e2f4b77dcbcde8. Package/version/certificate support updating the previous install; an actual update installation was not performed.
- XML parse, locale resource keys, package/static shortcuts, original GPL LICENSE, recursive native source revisions and `git diff --check` passed. See verification/dashboard-static-checks.log and dashboard-apk-verification.log.
- Native inputs unchanged from the preceding verified build: official AndroidLibXrayLite v26.10.21 AAR and HEV built from c5584feca8f6df70ee53eb2879e034b849a3d15a for all four ABIs. AAR SHA-256 e56ec3b06485fe7b37af26948673951edf719c50d6bbc618b8539045952adde4. Base application source remains official v2rayNG 2.3.14 at aa4b05503a504a3f2e43818e6ee8ebf1caaa3df0.

## Tests

Targeted main UI/model and subscription parser suite: **33 tests passed, zero failed**. Includes DashboardModelsTest and DashboardSubscriptionTest, covering initial/unavailable/disconnected data, measured intervals, duplicate samples, counter/time resets, bounded history, partial subscription headers, genuine zero/unlimited values and reading the preceding released JSON format. Existing main-screen helper and header parser tests were also included.

Full suite on final source: **136 tests ran, 132 passed, four failed**. All failures were unchanged RootProcessRunnerTest cases because Windows could not find `sh` (CreateProcess error 2). No test suppression or production shell workaround was added. Re-run those cases in Linux with sh/sleep/yes/head. Logs and complete JUnit XML: verification/dashboard-full-tests.log and verification/dashboard-tests/.

Build ran with JDK 21.0.13, Gradle 9.8.2, AGP 9.4.1, Kotlin 2.4.21 and the existing SDK/NDK inputs. In this sandbox, Kotlin compilation required the in-process strategy and a workspace-local java.io.tmpdir; Mockito used the already-declared Byte Buddy agent preloaded with the retained test init script. These are host/test workarounds, not APK dependencies.

## Data and behavior boundaries

- Subscription usage is from cached panel response headers and is refreshed through the existing update action. Traffic/quota availability metadata was added as nullable fields so omitted headers do not fabricate a zero or unlimited account. Previous JSON remains readable. No field was renamed or deleted.
- Live-device chart uses daemon snapshots, never demo samples. Rates use measured elapsed-time deltas. History is bounded to 60 samples, is not permanent storage and resets on disconnect or counter/time reset. Unknown stats show an empty state, not a zero graph.
- UI running state is the daemon's existing acknowledgement; it does not certify Internet reachability. Existing real-ping action remains available. Device aggregate outbound counters can count bytes at more than one chained outbound and differ from panel/billed usage.
- No live endpoint, subscription, credentials, tracker, advertisement or new telemetry service was added. No user configuration was used in reports.

## Not run

- Actual device/emulator rendering and interactions: empty/loading/populated states, light/dark/dynamic color, Persian RTL/English LTR, single/double columns, large fonts and landscape; touch/keyboard/D-pad activation; semantics tree/TalkBack; IME traversal, saved-state restoration and focus after list replacement. Compilation does not verify appearance or accessibility.
- Actual package update/install, connection/stop/restart, VPN and notification permissions/actions, process death, revoke, setup/teardown races, network handover and root/proxy-only scenarios. No usable Android test device/emulator or live server was available.
- End-to-end subscription refresh against the user's provider, quota matching, clipboard/file/QR import, DNS/routing and protocol interoperability. Original backend paths retained and parser/model tests passed, but no real subscription was supplied.
- Android lint, final Play Store variant, release build/signing, vulnerability audit. Release steps remain documented; no private release signing key is in the source archive. This is a Debug APK for installation/testing.

The source archive includes full source, portable materialized native headers, verified AAR/SO build inputs and reports. Git metadata, build caches and private signing keys are excluded. Earlier build reports under verification/ are historical and do not override this report.
