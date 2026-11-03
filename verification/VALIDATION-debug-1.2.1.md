# Validation — 1.2.1

Changes: Servers latency button now dispatches existing TestRealAllServers (onlyTcp=false) instead of TCP-only TestAllServers. Home refresh icon dispatches the existing all-subscriptions update contract, uses localized accessible text and existing progress/results. See CHANGES-1.2.1.md.

Executed: compileFdroidDebugKotlin and assembleFdroidDebug completed. Full unit suite ran 156 tests: 152 passed, four existing RootProcessRunnerTest cases failed because Windows cannot find sh. The combined --continue task therefore exits failed despite successful APK assembly. No tests disabled. Logs and XML are under verification/1.2.1-*.

APK: com.ispeedtopvpn.app, version 1.2.1, universal versionCode 5075700, min API24, target37. Signature and 16KiB ZIP alignment verified. Four ABIs/native libraries, GPL asset and brand labels checked. SHA256: 4a2e0bfd7ad15d7422b10f4d169978c3cc7d9bf548427c82c98e1b301a2556fb. XML/resources/GPL/native pins and git diff --check passed. No dependency/core upgrade. Previous detailed design and auto-selection reports remain historical under verification/.

Not run: device installation/UI, TalkBack, keyboard/D-pad, theme/RTL rendering, actual real-delay tests against live endpoints, subscription update success/failure against user's provider. No test device or subscription available. Release signing/build and current lint were not run; prior 1.2.0 lint result does not verify this version.

Play Protect: user supplied a screenshot blocking installation with an unfamiliar-developer message. This has not been resolved or reproduced on a test device. Standard APK signature verification does not imply Google approval or malware audit. The final APK manifest has no READ_SMS/RECEIVE_SMS, Accessibility service or notification-listener declaration. It retains Internet/network/VPN-related services, QR camera, notifications, boot, wake lock and installed-app query for per-app routing. Do not remove required functionality solely to change detection. Stable release signing and Google review/registration are distribution steps, not a guaranteed local fix. No APK was submitted to Google, no account registration performed, and no protection disabled. See https://developers.google.com/android/play-protect/warning-dev-guidance . Exact additional dialog details are pending from the user.

Final targeted main suite: 44 tests, 0 failures; targeted test + assemble command BUILD SUCCESSFUL. No additional production changes after that build.
