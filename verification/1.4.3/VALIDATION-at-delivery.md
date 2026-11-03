# iSpeedTopVPN 1.4.3 — verification (2026-11-03)

Scope: keep OpenVPN/Cisco and fix the OpenVPN connection startup failure; remove only the two Home Quick Access controls marked in the screenshot (Add configuration and Test latency). OpenVPN .ovpn file import and its Home/menu entry remain. Other Xray import/server/latency actions are unchanged. See CHANGES-1.4.3.md. No whole-feature removal was delivered; that temporary change was undone.

## Executed

- The old GlobalPreferences Java class was compiled and its cold getForceConnected() call reproduced RuntimeException: Global preferences instance is not set (preferences-before.log). After adding a process-local default snapshot the identical probe returns forceConnected=false (preferences-after.log). Foreground notification construction calls this getter; the embedded host does not run the original restrictions Application setup.
- Full JVM suite: 177 tests, 173 passed, 4 failed. Only unchanged RootProcessRunnerTest cases fail because Windows cannot find sh (CreateProcess error2). No tests disabled; full test task exits failed. Fourteen protocol tests pass, including two new tests using isolated classloaders to model cold, independent processes and managed overrides. XML/logs retained under verification/1.4.3/jvm-full and full-checks.log.
- After the final UI edit, :app:assembleFdroidDebug, :app:assembleFdroidRelease and :app:lintFdroidRelease: BUILD SUCCESSFUL (build-final.log). Lint: 0 errors, 359 warnings, 1 hint. No suppression added for this change.
- Source check confirms the Quick Access panel retains OpenVPN/Cisco and neither marked Xray add/test control (ui-scope.log). XML, host identity/shortcuts, GPL, recursive Xray/HEV source pins and git diff --check passed (source-check.log).
- Final Release signed with the existing key. APK v2/v3 signature, ZIP16KiB alignment, four-ABI native presence, license assets, all locale app labels, and absence of ACRA/Xposed runtime/initialization asset passed (sign.log). All packaged 64-bit ELF LOAD segments align to at least16384 bytes (elf.log).

Identity: com.ispeedtopvpn.app; name1.4.3; universal code5076200; minAPI24, target/compile37; Release not debuggable. Certificate SHA-256: 3e70db5bbca2256d8df6b23d459302416e414c53a018d09d46778ddbc2c241a8, same as earlier releases. APK SHA-256: 494051c5e388f3efabc884937e6989f322b7cbffe6a3627ca3aba36087a08ffa. Artifact sums: SHA256SUMS-1.4.3.txt. No private signing material in source.

Source ZIP is the complete verified 1.4.1 source archive updated with changed/new source, tests, guides and reports. Native inputs/dependencies are unchanged. Native engines were reused from checksummed published upstream APKs, NOT rebuilt or independently security-audited here. Origins/licenses/rebuild limits: NATIVE-ENGINES.md.

## Not run / limits

Not run: actual installation/update and Play Protect behavior for1.4.3; confirm the connection crash is resolved on the user's phone; actual VPN consent/authentication/traffic; Binder/process reconnection; background, revoke, rotate, process death; all runtime ABI/API/page-size combinations; render/click/TalkBack/focus checks of the changed panel. No established Android device/emulator testing session is available. Build/JVM checks do not establish those outcomes. User reported the 1.4.1 protocol hub opens but OpenVPN connection crashes; this is not runtime verification of1.4.3.

Defaults for GlobalPreferences are minimalUi=false, forceConnected=false, initialImport=false in every process; managed setters remain supported and publish a volatile snapshot. The 1.4.1 asynchronous status-pipe/cache fix remains. No new native binary, permission, server, credential, tracking SDK or advertising added. Existing Xray modes, data fields, subscription/auto-select/routing/DNS behavior retained.

Existing boundaries remain: self-contained UTF-8 .ovpn import up to2MiB with inline certificates/keys; scripts/external local files rejected; inherited Cisco advanced forms with incomplete Persian/Compose redesign; Cisco Always-on disabled; Xray backup excludes new profile stores; dashboard/quota/auto-select/routing/DNS are Xray-specific; one Android VPN per user. Backend Mirza/PasarGuard code login/HTML adapters remain separate unimplemented work.

Play Protect review is handled separately under outputs/play-protect-review/1.4.3. No Google approval or guaranteed warning removal is claimed. Older reports are historical.