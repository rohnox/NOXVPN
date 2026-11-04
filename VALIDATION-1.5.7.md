# Special VPN 1.5.7 — validation

VersionName 1.5.7 / base versionCode770. Application ID com.specialvpn.app. Source folder and repository retain their historical names. Changed behavior is listed in CHANGES-1.5.7.md.

## Executed

- Final full JVM/optional Robolectric run: 268 tests,264 passed,4 failed,0 skipped. All four failures are unchanged RootProcessRunnerTest cases: Windows cannot execute sh (CreateProcess error2). The full test command exits failed; no tests were disabled. Results: verification/1.5.7/jvm-full/ and all-tests.log.
- Exit-IP parser rejects nonliteral, malformed and oversized responses; explicit loopback HTTP proxy, six-second timeout, disabled redirects/retry and invalid-port rejection passed. ViewModel tests cover disconnected state, daemon port use, success, missing port, lookup failure/retry, in-flight cancellation on daemon stop. UI tests cover populated/loading/unavailable/disconnected and refresh action dispatch. All addresses in test screenshots are reserved synthetic examples, not live IP results.
- Core accounting tests cover absent/null objects, custom policy preservation, malformed policy and generated configuration. SessionTraffic tests cover monotonic timer advancement and legacy/backward clock handling. The Compose timer test advances one second between daemon samples; it does not extrapolate traffic bytes.
- Notification tests construct actual framework notifications with synthetic state: Cisco/OpenVPN connected and disconnected titles, dedicated small icons, Persian strings, new app name and Quick Settings manifest identity. No live native session is started by these tests.
- Compose renders include the selected original PNG, compact gold-outline update button, real component layout and exit-IP row, English/Persian RTL and existing dark/light/empty/populated subscription/protocol fixtures. Rendered values are synthetic. The subscription quota remains below the usage chart and scrolls into view.

## Not run

- Physical Android installation, Samsung Android12+ statistics, wall-clock observation of the live timer, Xray/OpenVPN/Cisco connections, engine handover, notification/tile updates during live connections, start/stop/restart races in VPN/proxy/root modes: no usable device/emulator is attached. JVM tests do not prove those device behaviors.
- Live public exit-IP lookup through a running Xray session, custom proxy credentials, IPv6 mobile networks and provider outages: no live device tunnel available. Custom routing can use different exits for other destinations; unavailable proxy listeners show Unavailable.
- TalkBack spoken labels/focus, actual touch/keyboard/D-pad navigation, dynamic color, large-font/narrow screens, both server-column layouts, activity/process recreation and actual IME traversal: no device/emulator available. Existing fixture interaction assertions prove only their stated component behavior.
- Old-package configuration migration is not implemented. New package installs independently; user-managed export/import is required. Actual backup transfer was not tested.
- Play Protect certification or scanning of this APK: not performed. Signing and a successful build do not guarantee absence of system warnings.

## Build and artifact inspection

- Normal `:app:assembleFdroidDebug :app:assembleFdroidRelease :app:lintFdroidDebug` completed BUILD SUCCESSFUL after optional UI-test dependencies were excluded. Lint: zero errors,370 warnings,one hint (same counts as1.5.6, no new suppression). Results: build-final.log and lint.xml.
- Release signed with existing certificate SHA256 `3e70db5bbca2256d8df6b23d459302416e414c53a018d09d46778ddbc2c241a8`; v2/v3 signature and ZIP16KiB alignment checks passed. Both APK identities, all four ABI inputs and absence of ACRA/Xposed passed. Debug was independently inspected. Results: sign.log and debug-verify.log.
- All60 native libraries are byte-identical to1.4.3; ELF alignment, bundled font content/OFL asset and absence of project UI-test fixture classes in Release DEX passed. Permission capabilities match the preceding build after normalizing the application-specific receiver permission to the explicitly changed package ID. No new permission capability was added.
- Resource XML, original GPL license, `git diff --check` and recursive native source pins passed. New labels supplied in all ten configured locales. The selected reference PNG and runtime PNG are identical copies.

Artifacts: SpecialVPN-1.5.7-universal-release.apk, SpecialVPN-1.5.7-universal-debug.apk, SpecialVPN-1.5.7-source.zip and public certificate. SHA256SUMS-1.5.7.txt records hashes. The complete source ZIP retains its historical iSpeedTopVPN top-level folder and includes native sources/build inputs. No customer subscription response, administrator credential, private signing material or real exit IP is included.
