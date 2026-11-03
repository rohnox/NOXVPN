# iSpeedTopVPN 1.4.0 — verification, 2026-11-03

Embedded OpenVPN 2 and OpenConnect (AnyConnect-compatible), alongside existing Xray features. Base: v2rayNG 2.3.14, aa4b05503a504a3f2e43818e6ee8ebf1caaa3df0. Package com.ispeedtopvpn.app; version 1.4.0; universal version code 5075900; min API 24; target/compile API 37. Original GPL notices retained. See PROTOCOLS-FA.md, CHANGES-1.4.0.md and NATIVE-ENGINES.md.

## Executed

- Debug/unsigned Release assembly, final focused tests and host Release lint: BUILD SUCCESSFUL (build-tests-lint.log). All 8 new protocol tests passed: import size/UTF-8/inline validation, script/control/external-file rejection including quoted directive bypass, connection block validation, actual connected-state mapping and challenge parsing. These do not prove connectivity.
- Full JVM suite: 171 tests, 167 passed, 4 failed. All four are unchanged RootProcessRunnerTest cases: Windows cannot find sh (CreateProcess error 2). No tests disabled; full task exits failed. Full run preceded the last Cisco cold-restoration guard; the 8 protocol tests were repeated afterward and passed. XML and logs retained in verification/1.4.0/jvm-full, jvm-protocols-final and full-checks-before-final.log.
- Host :app:lintFdroidRelease: 0 errors, 361 warnings, 1 hint. Two new warnings concern empty super.onCleared calls. No warnings hidden. Engine lint analysis ran as dependencies; this is not a device test.
- Resource XML and configured locale keys, host identity/shortcuts, original GPL text, git diff --check and existing recursive Xray/HEV source pins checked.
- Native ELF checks: packaged files parse as ELF; all LOAD segments of arm64-v8a/x86_64 libraries align to at least 16384 bytes. Four-ABI native presence, embedded host/engine licenses, branded labels, signature and ZIP alignment checked. Scripts: verify-apk.ps1 and check-engine-elf.ps1.

The last packaging pass removes an unused upstream Xposed initialization asset. No production Java/Kotlin changed after the final focused tests. Final artifact logs: final-package.log, sign-final.log, elf-final.log and badging.log. The APK verification script rejects that asset and ACRA/Xposed runtime classes. Release certificate SHA-256: 3e70db5bbca2256d8df6b23d459302416e414c53a018d09d46778ddbc2c241a8, same as 1.3.0. APK v2/v3 signature and 16KiB ZIP alignment checked. Checksums supplied separately in SHA256SUMS-1.4.0.txt. Private key/password remain outside source.

## Environment and provenance

Windows, JDK 21, original Gradle 9.8.2 / AGP 9.4.1 / Kotlin 2.4.21, SDK/build-tools 37 and existing NDK 30.0.16248370. No downgrade. In-process Kotlin, two workers, already-declared ByteBuddy test agent (self-attach restricted). Direct Java dependency networking was unreliable; a temporary loopback-only Maven cache fetched the same official HTTPS artifacts. A refresh cleared a stale missing error_prone_annotations artifact. Production repository URLs remain unchanged; helper is outside source/APK.

New engine Java/Kotlin wrappers were compiled. New engine native libraries were reused from checksummed published upstream APKs, NOT rebuilt here. Source trees, top-level pinned native dependencies and upstream build scripts are included; native rebuild needs Linux tooling and possibly recursive/transitive source fetches. WSL unavailable. Exact upstream sources, digests, licenses and rebuild limits: NATIVE-ENGINES.md. No claim of bit-reproducibility or independent native security audit.

## Not run / boundaries

No phone/emulator installation or live VPN test. ADB could not initialize its default .android directory under filesystem restrictions; no live account/configuration from the browser was copied or used. Consent, foreground service/notifications, actual OpenVPN/Cisco auth/MFA/certificates, revocation, process restoration, engine switching, ABI/page-size runtime and traffic accuracy: NOT RUN. Full matrix: verification/1.4.0/DEVICE-CHECKLIST.md. Same-certificate upgrade eligibility is checked statically; actual update installation and Play Protect acceptance for this APK are untested.

The hub initializes on IO, observes OpenVPN status IPC, marks connected only at LEVEL_CONNECTED and clears traffic outside that state. Import/command submission never mark connected. Cisco has its own manager/status. Cold Cisco activity restoration returns through the host initialization gate; an uninitialized cold service stops. Actual lifecycle still needs device verification.

Importer accepts self-contained UTF-8 .ovpn files up to 2MiB and supports import/connect/disconnect. Inline certificates/keys required; executable/control directives and external local files rejected. Cisco advanced forms remain inherited compatibility UI with gold accent; full Persian/custom Compose redesign of every inherited form is not completed. Cisco Always-on disabled. Existing Xray backup does not include new profile stores. Main dashboard, auto-selection, quota, routing and DNS remain Xray-specific; no synthetic cross-engine metrics. Android allows one VPN per user.

No server/default subscription/customer credential, ads or tracking added. ACRA Application and Xposed runtime excluded; standalone Cisco automatic/manual updater blocked; imported components internal. Local native logs may contain server details: redact before sharing. Backend login codes, Mirza/PasarGuard integration and HTML subscription adapters remain unimplemented separate work.

Older reports are historical. Prior user installation/Play Protect feedback does not verify this build.