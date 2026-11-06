# Embedded engine provenance and rebuild notes

The host remains a GPL-3.0 v2rayNG 2.3.14 fork (aa4b05503a504a3f2e43818e6ee8ebf1caaa3df0). Original notices remain. New adapters are under `V2rayNG/app/src/main/java/com/v2ray/ang/ui/protocols`. Android library wrappers are `V2rayNG/engine-openvpn` and `V2rayNG/engine-openconnect`. Full imported upstream trees are in `engines/`. Upstream Java/Kotlin code is compiled into the host, not launched in another installed app.

## OpenVPN

- Official source: https://github.com/schwabe/ics-openvpn/tree/v0.7.69 — tag commit f669267e2547fddca04d0bc699739137085eb633.
- Published native input: official v0.7.69 APK, SHA-256 `883b90c4640c94ed3fc1fb6e078341d85276fb66cecb277cb8cd4c3ba4426efb`, checked against the release API digest.
- Four ABIs: arm64-v8a, armeabi-v7a, x86, x86_64. Included: libopenvpn.so, libovpnexec.so, libovpnutil.so, libosslutil.so. OpenVPN 2 skeleton build; OpenVPN 3 and speed-test binaries are not included.
- License: `engines/openvpn/doc/LICENSE.txt`, GPL-2.0-or-later with linking exceptions. Preserved; copy embedded as `assets/ispeed-engines/OpenVPN-LICENSE.txt`.
- Host adaptations: checked profile-list persistence; internal host notification/challenge targets; package-scoped broadcasts; extracted JNI executable used on every supported API (24+); host-owned initialization/status IPC. Version 1.4.1 initializes the service-process log cache idempotently and reads status handshake/log frames on a cancellable worker rather than the UI callback. Legacy UI is included only where required by the skeleton service.

Pinned source dependencies: asio 8806a6803cde7054c3049d3666d3ec36786568c5; fmt 1be298e1bd68957e4cd352e1f676f00e07dcfb57; lz4 5ff839680134437dbf4678f3d0c7b371d84f4964; mbedtls b1c8e41ae3b36a9a88e0cbee10ed38a577b54726; openssl b280e0c0c81ce6f8d5291c1f8efb6712c357c28a; openvpn c29ee42e48df0de5993f7b256829196e67e76006; openvpn3 29f38575603890ca908551d3240406d089b0f4d0. These top-level dependencies are materialized in this archive. Further nested dependencies may require upstream recursive initialization.

For a native rebuild, obtain the exact upstream tag on Linux, run `git submodule update --init --recursive`, and use its Android CMake/NDK Gradle build for the skeleton flavor. Retain the integration changes listed above, and replace the four-ABI native inputs in `V2rayNG/engine-openvpn/src/main/jniLibs` with the matching outputs. Follow upstream native dependency instructions and retain their notices. This environment compiled the Java/Kotlin wrappers; it did **not** rebuild these OpenVPN native binaries.

Version 1.4.3 also supplies process-local default GlobalPreferences snapshots before any notification/launch access, while retaining managed overrides. No native inputs changed.

## OpenConnect (AnyConnect-compatible)

- Maintained fork source/release: https://github.com/pengyue-polaron/oconnect-android/tree/v1.17.0 . This is not the official Cisco application.
- Published v1.17.0 APK SHA-256 `6eb30e907f38554807d34407667be4b78d364da3b59003c90ec5032968afa6ca`, checked against the release API digest. The upstream APK is a debug release; only its native libraries were reused. Its Java code, application class and signature were not reused as an installed application.
- Included native inputs, all four ABIs: libopenconnect.so, libstoken.so, libcurl-bin.so, libcsd-wrapper.so.
- Corresponding OpenConnect source pinned at 8b702bf2dbaf11302ed98629214b1df5d50a12aa (openconnect/openconnect GitHub mirror), stoken at bc25aa48922064881f17201c6302ab2640e36763 (stoken-dev/stoken). Both included under `external/`.
- License: GPL-2.0-or-later plus applicable linking exception; preserve headers and `engines/openconnect/COPYING`. Copy embedded as `assets/ispeed-engines/OpenConnect-COPYING.txt`. Selecting GPL-3.0 under the or-later grant permits combination with the GPL-3.0 host; retain every component's other notices.
- Native Makefile and scripts remain in the source. They specify OpenConnect 9.21, stoken 0.93 and curl 8.21.0 (SHA-256 aa1b66a70eace83dc624508745646c08ae561de512ab403adffb93ac87fc72e6). Use Linux, the upstream NDK/toolchain prerequisites and source fetch steps; run the upstream native Makefile in its documented build environment, then replace the module's jniLibs. Transitive C dependencies still need upstream downloads. Native reproducibility was not executed here; WSL was unavailable.
- Excluded from compilation/package: ACRA application/reporter, Xposed hooks/jars/asset, duplicate SpongyCastle helper classes (the OpenVPN module supplies the shared public package once). No automatic crash uploads were introduced.
- Host adaptations: initialization without ACRA; unique notification ID 42002; internal-only components; host FileProvider authority; gold manager palette; host-only updates (manual and automatic standalone updater disabled); cold activity restoration returns to the host IO initialization gate, and an uninitialized cold service refuses to start. Existing native authentication and certificate confirmation callbacks retained.

## Build the host

Use JDK 21 and the SDK/NDK versions in BUILD-FA.md; set ANDROID_HOME, create `V2rayNG/local.properties` with your SDK location, then:

```sh
cd V2rayNG
./gradlew :app:testFdroidDebugUnitTest --tests 'com.v2ray.ang.ui.protocols.*'
./gradlew :app:assembleFdroidDebug :app:assembleFdroidRelease
./gradlew :app:lintFdroidRelease
```

The source archive includes the native build inputs used by the host. No signing private key is included. Use the existing release signing guide and an external private key for release. The delivered release uses the pre-existing host certificate; changing that certificate prevents in-place upgrades. Reproducing the signature requires the owner’s private key, not the public PEM.

The temporary local Maven cache helper used on this Windows host is outside the source archive and is not a runtime dependency. Production repository URLs were not replaced with loopback URLs. Build tools/native inputs were not represented as having passed a third-party security audit. See VALIDATION.md for actual checks and untested device behavior.

Version1.6.3 moves the host stop/handover request from Cisco profile-list navigation to the confirmed connection path. EmbeddedRuntime exposes a host-installed connection gate; AngApplication registers LauncherManager as its owner. GrantPermissionsActivity guards duplicate/pending callbacks and skips destroyed owners. Native inputs and persisted profile formats are unchanged.

## WireGuard added in Special VPN 1.6.5

- Official `com.wireguard.android:tunnel:1.0.20260102`, pinned in libs.versions.toml. The exact official Maven AAR/POM/module/source JAR is bundled in V2rayNG/vendor-maven for reproducible dependency resolution. Transitive annotation dependencies are supplied by the existing AndroidX graph; no library upgrade of existing dependencies is required.
- AAR SHA256: 2b9c16db026496123e4db695d26d03d1958a201096c7c4c89b21077dc70f3119. The binary is the official prebuilt, not claimed rebuilt here. Four ABIs include libwg-go.so, libwg-quick.so and libwg.so. Only the userspace GoBackend is used, without root.
- Corresponding Android source: engines/wireguard, official tag1.0.20260102 revision09b75c2bd37f749e2a8c85876394854113c74be7. Native submodule snapshots: wireguard-tools e2ecaaa739144997ccff89d6ad6ec81698ea6ced; elf-cleaner7efc05090675ec6161b7def862728086a26c3b1f. Android Java source JAR is also supplied.
- Go module source ZIPs in engines/wireguard/go-module-sources match each exact h1 checksum in the pinned libwg-go/go.sum. They include wireguard-go f333402bd9cb plus x/sys, x/crypto, x/net and wintun versions from go.mod. Original licenses are preserved in the source archives. Java tunnel library is Apache-2.0; native components retain their individual MIT/GPL notices.
- Host adaptations live outside the unchanged WireGuard source: consent and Xray handover via LauncherManager, foreground controller service, backend-owned connection state, private MMKV profile catalog and Compose UI. Library always-on is explicitly disabled; null controller restarts stop rather than silently reconnect.
- New Eylan subpage adapter uses only customer subscription URLs and same-origin download endpoints. No administrative API key is embedded. Limits: 8 subscriptions, 96 profiles per subscription, 2MiB per download and24MiB downloaded profile data per update; the full catalog is capped at32MiB. ZIP extraction and scripts are not supported.

The complete unchanged WireGuard upstream snapshot, pinned Go module source archives and vendor-maven inputs are included in the full source ZIP attached to this release. They are not duplicated in the Git tree; a Git checkout resolves the pinned official library from Maven Central. The host adaptations and dependency declarations are in Git.
The controller uses the existing FOREGROUND_SERVICE_SPECIAL_USE permission with an explicit VPN lifecycle subtype on Android 14+; no alarm permission is requested. Reference: https://developer.android.com/develop/background-work/services/fgs/service-types#special-use
