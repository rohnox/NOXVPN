# Final validation — 2026-11-02

**F-Droid Universal Debug APK built successfully from the modified v2rayNG 2.3.14 source.** Earlier download failures were temporary and are superseded by this result. Initial logs are retained only as history.

## Passed

- Gradle `:app:compileFdroidDebugKotlin` and `:app:assembleFdroidDebug`. Final app-label correction rebuilt successfully: `verification/build-final-apk.log`. No upstream dependency was downgraded.
- New tests: SessionTrafficTest (3) and SessionDurationTest (1): four passed, zero failed. See `verification/build-delivery.log` and `verification/unit-tests-new/`.
- APK manifest analysis: `com.ispeedtopvpn.app`, version 2.3.14, versionCode 5075400, minimum SDK 24, target SDK 37; iSpeedTopVPN APP label in default, English, Persian and every other packaged locale. F-Droid's separate app-name override was corrected.
- APK ZIP inspection: libgojni.so, libhev-socks5-tunnel.so and libhevsockstun.so are present and nonempty for all four ABIs: arm64-v8a, armeabi-v7a, x86, x86_64. Embedded GPL license is present.
- `apksigner verify --verbose --print-certs` and `zipalign -c -P 16 4`: passed. This validates signature and ZIP alignment, not device installation or ELF runtime behavior. Android Debug certificate SHA-256: f99539d83f0d6fa82e111a8e1106aafed898c68684a53c24b3e2f4b77dcbcde8.
- HEV JNI and standalone executable compiled for every ABI with NDK 30.0.16248370. Windows Git symlink placeholders were materialized in a temporary staging tree; original native source was not modified. Portable source ZIP materializes those header links. A nonfatal native Git version-label warning reflects the staging tree's lack of Git metadata; pinned revisions were checked separately.
- Official AAR v26.10.21 SHA-256 matched its GitHub release digest: e56ec3b06485fe7b37af26948673951edf719c50d6bbc618b8539045952adde4.
- Android command-line tools archive SHA-256 matched official checksum: 90ae805d20434428bffcb699c290860f19bb5f66a67e6b330067e3de801fb04a.
- Native gitlinks, GPL retention, resource XML parsing, locale keys, dedicated identity/static shortcuts and `git diff --check` passed. Logs: `verification/static-checks.log`.

## Full unit suite

128 tests ran: **124 passed, four failed**. All four failures were existing `com.v2ray.ang.root.RootProcessRunnerTest` methods. Windows could not create the required `sh` process (CreateProcess error 2). Those tests exercise deadlines, inherited stdout, output limits and failure status. They were not changed or suppressed. Re-run them on Linux with sh/sleep/yes/head available. Full JUnit XML and log: `verification/unit-tests-full/` and `verification/unit-tests-full.log`.

The restricted host blocked Kotlin daemon connections and Mockito self-attach. Compilation used the supported in-process strategy; the already-declared Byte Buddy agent was loaded explicitly at test JVM startup via `verification/test-agent.init.gradle` and `-Dispeed.test.agent=<cached agent JAR>`. The agent is test-only and not packaged into the APK. PowerShell requires the full property argument quoted: `'-Pkotlin.compiler.execution.strategy=in-process'`.

## Build inputs

JDK 21.0.13, Gradle 9.8.2, AGP 9.4.1, Kotlin 2.4.21, SDK Platform 37.0 revision 2, NDK 30.0.16248370. AGP installed its default Build Tools 36.0.0; verification used Build Tools 37.0.0. All versions declared in the source were retained. Gradle reported upstream deprecations and packaged some native libraries without stripping debug symbols; assembly and signature verification still passed.

The final source archive includes generated AAR/SO build inputs as well as their pinned source trees and build scripts. It excludes Git metadata, build caches and private signing keys. No release key was generated. Debug signing is for installation/testing and is not a store release signature.

## Not run

- Installation/launch on Android, live VPN/server handshakes, traffic comparison, connection/stop/reconnect. ADB could not initialize its default user directory in this restricted host. No device or live server success is claimed; no credentials were supplied or invented.
- Service scenarios across VPN/proxy-only/root modes: cold/repeated starts, permission denial/grant, notifications/actions, repeated stop, stop during setup, revoke, process death, restart/network handover and races, screen-off/on and counter recovery. Requires emulator/device.
- UI/device scenarios: Persian RTL/English LTR, light/dark/dynamic color, single/double column, small screens/landscape/large fonts, touch/keyboard/D-pad, semantics/TalkBack, empty/loading/error/populated state, recreation and persistence.
- End-to-end clipboard/link/file/QR import, subscriptions, latency/TCP/real ping, DNS/routing and protocol interoperability. Original implementations retained; compilation and unit tests are not end-to-end verification.
- Android lint, complete Linux suite retry, final Play Store variant, release build/signing and vulnerability audit. Release signing remains documented in BUILD-FA.md.

## Limits

Universal covers four CPU ABIs, requiring Android 7/API 24 or newer. The UI reports daemon core-running status, which is not proof of Internet reachability; use the existing real-ping test. Session traffic aggregates outbound counters, including direct traffic; chains can count bytes at multiple outbounds. Disabled/unavailable statistics display an em dash. Custom raw configs must enable their outbound statistics policy. Counters reset with the daemon session. Selected profile and running profile can differ until restart. No tracker, ad provider or live configuration was added.
