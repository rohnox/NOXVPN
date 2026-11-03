# iSpeedTopVPN 1.5.0 — verification (2026-11-03)

Scope: modernize the black/gold dashboard and add an independent OpenVPN/Cisco bottom tab, with Subscriptions in the center and Open/Cisco between Subscriptions and Controls. See CHANGES-1.5.0.md. Earlier reports under verification/1.4.3 describe the prior release.

## Executed

- F-Droid Debug and Release assemblies completed on the final source. APKs were actually generated, not inferred from compilation. Final isolated assembly/lint output is retained in verification/1.5.0/build-final.log.
- Targeted regressions: DashboardNavigationTest (2 cases) and ProtocolActionsTest (4 cases) all passed. Tab order and stable saved-name fallback, cold/busy/failed readiness, unknown/deleted/reordered GUIDs, engine isolation and non-VPN restrictions are covered.
- Full JVM suite: 183 tests, 179 passed, 4 failed. The unchanged RootProcessRunnerTest cases fail with CreateProcess error2 because sh is unavailable on this Windows host. The full-checks Gradle invocation therefore exits failed; it used --continue so assemblies and lint completed. No tests were disabled. Full XML is under verification/1.5.0/jvm-full.
- :app:lintFdroidDebug completed: 0 errors, 362 warnings, 1 hint. No new suppression was added. The initial offline lint attempt could not resolve uncached Android test dependencies; a subsequent online run resolved them without dependency-version changes. Both logs and the final lint report are retained.
- Original GPL license, source XML, application identity, recursive original native source pins, and git diff --check passed. New protocol labels occur exactly once in all ten packaged locales.
- Byte comparison with the delivered 1.4.3 source ZIP confirms the OpenVPN crash fixes, StatusLogReader/StatusListener/service, ProtocolsViewModel and LauncherManager are unchanged. The new route uses the existing actual profile/status/byte-count data and the same platform launch paths.
- All 60 native libraries in the signed APK are byte-identical to the delivered 1.4.3 APK. APK permission declarations are identical. APK packaging contains the four supported ABIs, required engine libraries and original host/engine license assets; no ACRA/Xposed runtime or initialization asset was found.
- Release signed with the existing key; v2/v3 verification and ZIP16KiB alignment pass. All packaged 64-bit ELF LOAD segments have alignment at least 16384. Release is not debuggable.

Identity: com.ispeedtopvpn.app; version 1.5.0; universal F-Droid code 5076300; min API24, target/compile37. Certificate SHA256: 3e70db5bbca2256d8df6b23d459302416e414c53a018d09d46778ddbc2c241a8. Release APK SHA256: 561d0e37eaef0792be9364327beb1b063ac00bef5c6a753162addf03bc021164. Artifact sums are in SHA256SUMS-1.5.0.txt beside the delivered files. Private signing material is outside the source and is excluded from its archive.

## Not run

- Installation/update of 1.5.0, actual VPN consent/authentication/traffic and real endpoint connectivity from the new tab. No usable device/emulator session is established. adb devices was attempted; the sandbox prevented creation of its default user .android directory (adb-device-check.log). No emulator is installed in the available SDK.
- Touch, keyboard Enter/Space, D-pad activation and focus order; TalkBack/semantics-tree and spoken state; real empty/loading/error/populated UI presentation; Persian RTL and English LTR rendered appearance; large fonts and single/double-column server layouts; light/dark and dynamic-color on/off rendering/interactions.
- Activity recreation/process death, restoring the selected bottom tab and inner OpenVPN/Cisco selector, returning from the file picker/authentication/native Cisco manager, background/revoke/reconnection, and reopening stored profiles on an actual device. Pure saved-name/GUID checks are JVM-tested; they do not establish framework recreation success.
- All runtime ABI/API/page-size combinations, and Play Protect behavior for this new APK. User reported OpenVPN connection and absence of the Play Protect prompt for 1.4.3; those are user-reported results for the previous release, not a 1.5.0 device test or Google certification.

The initial generated design images were concepts. Their sample traffic, quota, IP address and server data are not embedded. The actual chart uses available Xray session rate samples; quota is shown only when the subscription supplies it. OpenVPN has its own status and measured byte counts; Cisco status/authentication remains in its internal manager. No fake 24-hour chart or custom Cisco authentication form was introduced. Legacy Cisco advanced forms are not fully redesigned or localized.

Native binaries were reused from checksummed upstream published APKs, not rebuilt or independently security-audited in this task. Origins, corresponding source and rebuild limitations: NATIVE-ENGINES.md. Self-contained UTF-8 .ovpn import remains limited to 2MiB with inline certificates/keys; scripts/external certificate files are rejected. Android allows one active VPN per user; existing Xray backups do not include the engine profile stores. Mirza/PasarGuard code login remains separate unimplemented work. No server, credential, tracker or advertising added.
