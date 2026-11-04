# Special VPN 1.6.2 — validation

Scope:About source/feedback/channel/privacy links, release version, documentation and the selected slim update banner. Source/Feedback now target the private sabialballa/iSpeedTopVPN repository; viewers require repository access. Telegram is https://t.me/iSpeedTopVPN. Original license/translator attribution and upstream engine/update resources are preserved.

## Not run

Physical-device installation, external browser/Telegram app handoff, TalkBack, D-pad and live VPN connection:no usable device/emulator attached. URL constants and compiled APK inspection do not replace device interaction checks. Previous runtime caveats in VALIDATION-1.6.1.md remain applicable.

Build/test/sign evidence is recorded under verification/1.6.2/. Private signing material and customer data are excluded. README gallery is synthetic data rendered by the actual Compose UI.

## Executed

Offline compileFdroidDebugKotlin, assembleFdroidDebug, assembleFdroidRelease and lintFdroidDebug passed. Final full JVM/optional UI run:296 tests,292 passed,4 failed,0 skipped. The four unchanged RootProcessRunnerTest failures are Windows missing sh (CreateProcess error2); the full command exits failed and no test was disabled. Both universal APKs passed signature and ZIP alignment checks. Release signer SHA256 3e70db5bbca2256d8df6b23d459302416e414c53a018d09d46778ddbc2c241a8 is unchanged. All60 native libraries remain byte-identical to1.4.3; permissions, ELF alignment, fonts/OFL and absence of project UI test fixtures in Release DEX passed. See all-tests.log, jvm-full/, compile.log, build-final.log, sign.log, debug-verify.log, apk-comparison.log, elf.log, source.log, font-and-test-scope.log and lint.xml.

## In-app update behavior

SpecialUpdateTest verifies newer/equal/older versions, multi-digit comparisons, four-ABI universal Release selection, stable/pre-release filtering, and rejection of Debug/foreign-host/upstream download URLs. DashboardSubscriptionsTest verifies entry checks,15-minute throttling, dismissing the same version, quiet failures, current/no-update state and canceling a pending result on ViewModel close. PremiumVisualTest verifies the actual slim banner in English/Persian, navigation to the release notes/download screen, and dismissing it. English/Persian synthetic renders were inspected. No new installation permission or background polling service is added; browser download and Android/user install confirmation remain explicit.

Public GitHub visibility is required for unauthenticated production checks. A live device upgrade and a future newer release on a phone are Not run. The current release should not display a newer-version banner for itself. Source/README release instructions are in UPDATES-FA.md.
