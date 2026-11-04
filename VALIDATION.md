# Special VPN 1.5.9 — validation

Package com.specialvpn.app; base versionCode772. Scope: OpenVPN persistence/profile lookup/disconnect delivery, provider-title Remark prefill, simplified subscription summary, high-resolution flag artwork. See CHANGES-1.5.9.md.

## Executed

- Final full JVM/optional UI run:283 tests,279 passed,4 failed,0 skipped,0 errors. The four unchanged RootProcessRunnerTest failures are Windows missing sh (CreateProcess error2). The full test command exits failed; no test is disabled. Evidence: verification/1.5.9/all-tests.log and jvm-full/.
- OpenVpnPersistenceTest: serialized profile survives a fresh manager with an empty/stale index and didrecover already set; late imported UUID is read despite an already loaded empty manager; missing-profile notification returns without the null crash or ten-second polling; disconnect binder queues rather than executes native/socket work on the caller; early disconnect stays alive until binding delivers the stop command. These framework/storage tests do not start a native VPN or real server connection.
- Existing import-policy, status IPC, provider-title/fallback, subscription selection/deletion and UI-action tests pass. Updated UI assertions require no Update Sub text in the selected summary and continue checking list-card Edit/share/delete, import-dialog access and stable subscription IDs. The editor uses the same tested provider-title/fallback helper.
- All Java ISO country codes have bundled2048x1536 PNG artwork with a valid PNG header. SVG sources are flag-icons7.3.2 (MIT); source archive, license, metadata, PNG hashes and regeneration script are included. Runtime artwork loads on IO through the repository; UI renders a transient Bitmap. No runtime image dependency or network flag download was added.
- Inspected actual Compose renders for English/Persian RTL, light/dark, the sharp flag background and simplified subscription summary. Data and IPs are synthetic fixtures, not a customer's configuration or live geolocation result.

## Not run

- Install/update, actual OpenVPN import on a phone, closing/force-stopping/reopening the app, real connecting/connected/disconnected sessions, repeated stop, early stop during native startup, Android service process death, VPN permission/revoke and protocol handover: no usable physical device/emulator attached. JVM storage and binder tests are not proof of live VPN connectivity or of the reported device-specific freeze being eliminated.
- A live connection to the user's OpenVPN endpoint, credentials, certificates, network/DNS/TLS/cipher compatibility and server reachability: no private profile/endpoint was supplied for this check. No security setting was weakened or real configuration guessed.
- Actual Remark editing/saving, Android IME traversal, activity recreation, physical clipboard/QR sharing, TalkBack and hardware keyboard/D-pad, dynamic color and large-font/tablet layouts: no device available. Component tests cover only their named assertions.
- Actual country-provider availability and IP-country accuracy on a VPN, low-memory bitmap behavior on hardware, Quick Settings rendering on Xiaomi/Samsung and Play Protect scanning/certification: not run.

## Build and artifacts

- Normal F-Droid Debug/Release assembly and Debug lint completed BUILD SUCCESSFUL without optional UI-test dependencies in the app. Lint: zero errors,372 warnings,one hint. The added warning is the now-unused top_update_short string after simplifying the selected summary; no suppression was added.
- Release v2/v3 signing and ZIP16KiB alignment passed with the existing certificate SHA256 `3e70db5bbca2256d8df6b23d459302416e414c53a018d09d46778ddbc2c241a8`. Both APK identities, all four ABI inputs, absence of ACRA/Xposed, font/OFL assets and absence of project UI fixtures in Release DEX passed.
- All60 native shared libraries are byte-identical to1.4.3. Java/Kotlin integration changes are described above; no native binary was replaced. ELF alignment and permission capabilities match the preceding release. All26 runtime flag archives in the signed APK match the source hashes, and the MIT flag license is packaged.
- Resource XML, original GPL license, source diff and recursive native source pins passed. Evidence: verification/1.5.9/build-final.log, sign.log, debug-verify.log, apk-comparison.log, elf.log, font-and-test-scope.log, source.log and lint.xml.

Outputs: SpecialVPN-1.5.9-universal-release.apk, SpecialVPN-1.5.9-universal-debug.apk and SpecialVPN-1.5.9-source.zip. SHA256SUMS-1.5.9.txt records hashes. The archive retains the historical iSpeedTopVPN top folder and contains complete native source/build inputs, flag sources and artwork licenses. Private configurations, credentials and signing material are excluded from repository/archive output.
