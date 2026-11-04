# Special VPN 1.6.1 — validation

Package com.specialvpn.app; base versionCode774. Scope: restore graph behavior from1.5.9 while retaining other1.6.0 fixes.

The following source files are byte-identical to the delivered1.5.9 source ZIP: PasarGuardUsage.kt, AngConfigManager.kt, ISpeedOverview.kt and PasarGuardUsageTest.kt. The foreground metadata refresh calls the restored UTC fetch/merge contract. The old provider parser regression tests, dashboard timing/selection tests and Compose chart tests are run with results under verification/1.6.1/.

## Not run

Physical-device installation, real VPN and live customer subscription fetch: no usable device/emulator attached. Provider UTC day boundaries are intentionally preserved. A new local date/fetch time does not guarantee a new UTC daily bucket. Source, test and build evidence do not certify live server behavior.

Build/sign instructions: BUILD-FA.md and RELEASE-FA.md. Private parser fixtures and signing keys remain outside the repository/source archive.

## Executed test results

Focused tests passed. Full JVM/optional Compose run:289 tests,285 passed,4 failed,0 skipped. The unchanged four RootProcessRunnerTest failures are Windows missing sh (CreateProcess error2); the full command exits failed and no test is disabled. Source identity against1.5.9 is recorded in graph-source-comparison.log. Test XML, logs and synthetic Compose renders are included under verification/1.6.1/.

Offline assembleFdroidDebug, assembleFdroidRelease and lintFdroidDebug passed (BUILD SUCCESSFUL). Both universal APKs passed signature and ZIP alignment checks. Release package is com.specialvpn.app, versionName1.6.1; certificate SHA256 3e70db5bbca2256d8df6b23d459302416e414c53a018d09d46778ddbc2c241a8 matches previous releases. All60 native libraries remain byte-identical to1.4.3. Permissions, ELF alignment, font/OFL assets and absence of project UI fixtures in Release DEX passed. Evidence:build-final.log, sign.log, debug-verify.log, apk-comparison.log, elf.log, source.log, font-and-test-scope.log and lint.xml.
