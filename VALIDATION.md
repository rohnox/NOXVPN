# Special VPN 1.6.0 — validation

Package com.specialvpn.app; base versionCode773. See CHANGES-1.6.0.md.

## Executed

- Focused provider parser, dashboard timing, protocol action/status and Compose visual tests passed. Final full JVM/optional UI run:290 tests,286 passed,4 failed,0 skipped. The four unchanged RootProcessRunnerTest failures are Windows missing sh (CreateProcess error2). The full command exits failed; no test was disabled. Evidence:verification/1.6.0/all-tests.log and jvm-full/.
- New regressions cover the Tehran midnight boundary, explicit +03:30 query/response grouping, daily refresh delay, legacy JSON/new offset roundtrip, selected-plan-only foreground history updates, stopping on pause, stale selected-plan responses, exact connected UUID with duplicate profile names, conservative delete eligibility, and profile deletion surviving a fresh native profile manager.
- Compose tests require Update Sub in the top subscription summary. Inspected actual English and Persian RTL renders with the text and refresh icon; edit/share/delete stay on list cards. Renders use synthetic fixture data, not a customer connection.
- PasarGuard v5.4.1 source inspected at commit b56ffe369f542152c52c69733205baeaf3f6e4cd: get_user_usages groups using the requested start/end timezone. The app now requests thirty dates including today using a fixed offset snapshot. Returned provider samples alone are graphed; delayed current-day data is explicitly pending. Current fixed offset grouping does not reconstruct historical DST changes.
- Normal offline assembleFdroidDebug, assembleFdroidRelease and lintFdroidDebug passed (BUILD SUCCESSFUL). Lint:0 errors,371 warnings,1 hint. Both universal APKs passed signature and ZIP alignment verification. Release uses the existing certificate SHA256 3e70db5bbca2256d8df6b23d459302416e414c53a018d09d46778ddbc2c241a8. All60 native libraries remain byte-identical to1.4.3; permission capabilities, ELF alignment, licenses, bundled fonts and absence of project UI fixtures in Release DEX passed. Evidence:build-final.log, sign.log, debug-verify.log, apk-comparison.log, elf.log, source.log, font-and-test-scope.log and lint.xml.

## Not run

- Physical-device/emulator VPN, IPC reconnect races, native server connection and real midnight transition: no usable Android device/emulator attached. No successful live connection or installation is claimed.
- Device activity recreation/persistence after force-stop, TalkBack spoken labels/focus order, D-pad/keyboard activation of the new protocol controls, dynamic-color hardware and IME traversal: no usable device/emulator. JVM storage/policy and existing Compose interaction tests do not replace these checks.
- Provider-specific latency at midnight, historical daylight-saving offsets and production servers in different zones: no live customer endpoint queried in this change. The provider controls when today's history is published.

## Reproduce

Use BUILD-FA.md and RELEASE-FA.md. In this workspace, work/run-android.ps1 selects the pinned toolchain. Optional UI tests use scripts/visual-checks.init.gradle with -Pispeed.visualChecks=true. The private provider parser fixture remains outside the source archive and repository. No signing key, password, private profile or subscription token is included.
