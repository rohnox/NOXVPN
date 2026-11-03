# iSpeedTopVPN 1.5.1 — validation (2026-11-04)

Scope: replace the crowded dashboard presentation with the compact black/gold design, keep the independent Open/Cisco tab and use a local Persian/English font. See CHANGES-1.5.1.md. Previous reports under verification/1.5.0 are historical.

## Executed checks

- F-Droid Debug and Release APKs were actually assembled from the changed source. Final assembly/lint log: verification/1.5.1/build-final.log. Full checks use --continue so the known failing shell tests do not prevent APK and lint tasks from running.
- Normal JVM suite: 184 tests, 180 passed and 4 failed. The unchanged RootProcessRunnerTest cases cannot start sh on this Windows host (CreateProcess error2). The full-suite Gradle invocation exits failed. No tests disabled. XML and full-checks.log are retained.
- Eight optional UI checks passed using Robolectric 4.17 native graphics, Android API35, 393x852dp. They render English populated/empty Home, Persian RTL Home, light Home, OpenVPN empty/profile views, Cisco selection, and activation of the Open/Cisco bottom tab. They assert that server cards/import actions are visible and that the inactive OpenVPN disconnect action is absent. Snapshots and merged semantics trees are under renders/. Captures force a full window repaint after interaction to avoid a partial JVM-rendered frame.
- The screenshots use isolated test fixtures without endpoints, persistence, customer data or native VPN initialization. They are renders of the actual components, not live-connection/device tests. Optional dependencies and the test activity exist only when -Pispeed.visualChecks=true is explicitly supplied; the normal APK does not contain the project visual-test class.
- F-Droid Debug lint: 0 errors, 369 warnings, 1 hint. No new suppression. The font copyright/OFL asset is present, and all three packaged Vazirmatn font contents match their source hashes despite optimized APK resource paths.
- XML, application identity, original GPL, original recursive Xray/HEV source pins and git diff --check passed. Imported native source, connection launch paths and the earlier OpenVPN crash fixes are retained.
- All 60 native libraries and permission declarations in the signed APK match the delivered 1.4.3 release byte-for-byte / declaration-for-declaration. Four supported ABIs and required libraries/licenses are present. ACRA/Xposed runtime/initialization asset are absent.
- Release uses the existing certificate; v2/v3 signature and ZIP16KiB alignment pass. Packaged 64-bit ELF LOAD alignment is at least 16384. The private signing key/password remain outside the source, Git and release assets.

Identity: com.ispeedtopvpn.app, version 1.5.1, universal F-Droid code 5076400, min API24, compile/target37. Certificate SHA256: 3e70db5bbca2256d8df6b23d459302416e414c53a018d09d46778ddbc2c241a8. APK/source hashes are in the delivered SHA256SUMS-1.5.1.txt. The Git tree excludes generated native inputs; the complete source ZIP includes the same native build inputs used for the local APK. CI/release signing has not been configured or run on GitHub; no signing key was uploaded there.

Git publication checks application/adapter changes separately from vendored engine sources and raw reports. Imported documentation uses repeated-equals section headings that Git reports as conflict markers; these are upstream documentation headings and are preserved. Third-party license notices and raw verification output are retained verbatim via .gitattributes.

## Not run / limits

Not run: installation/update and live VPN connection on an actual phone for 1.5.1; system VPN consent, file picker and credential entry; background/revoke/reconnection; real activity recreation/process death and persistence across reopening; TalkBack spoken output; physical keyboard/D-pad activation; all screen sizes, large accessibility font sizes and single/double-column server layouts; dynamic-color on/off on a supported device; all runtime ABI/API/page-size combinations; Play Protect behavior for this APK. No usable physical-device/emulator session is established. JVM rendering and semantics assertions do not establish those device outcomes.

The user reported successful OpenVPN connection and no Play Protect prompt for 1.4.3. These are user-reported results for the older version, not a 1.5.1 test or Google certification. Native inputs were reused from checksummed upstream APKs, not rebuilt or independently security-audited here; provenance and rebuild limits are in NATIVE-ENGINES.md.

App data still comes from the actual feature owners: Xray session counters/chart, subscription-provided quota, upstream OpenVPN status/byte counts and the Cisco manager. No synthetic server/IP/history is shipped. Self-contained UTF-8 .ovpn imports remain limited to 2MiB with inline keys/certificates; external local files/scripts are rejected. Cisco advanced authentication screens remain upstream native forms. Android supports one active VPN per user; Xray backup does not include engine profile stores. Mirza/PasarGuard code login remains separate unimplemented work.
