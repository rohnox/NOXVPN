# Final validation — 1.3.0

Official base remains v2rayNG 2.3.14, commit aa4b05503a504a3f2e43818e6ee8ebf1caaa3df0. Scope: CHANGES-1.3.0.md describes Default visibility, new subscription fetch and metallic black/gold. Internal records, imported configs, native core, protocols and release key retained.

## Executed

- Final :app:assembleFdroidRelease and targeted JVM command: BUILD SUCCESSFUL. Targeted main/subscription suite: 51 tests, zero failures, including seven new presentation/fetch-policy/reorder tests. No device UI test is implied by these policy tests.
- Signed universal Release: com.ispeedtopvpn.app, version 1.3.0, universal code 5075800, min API24, target37. All four ABIs/native libraries and GPL asset present. Brand labels, APK signatures v2/v3 and 16KiB ZIP alignment passed.
- APK SHA256: ac3e8e227bb2d2f37a995e07d9f48e5a8f62ff1c19400abc077728e7042b1387.
- Release certificate SHA256: 3e70db5bbca2256d8df6b23d459302416e414c53a018d09d46778ddbc2c241a8, identical to 1.2.1 Release. Package/code/certificate permit updating that Release without uninstalling. An actual update installation was not performed here. Debug uses another key.
- Static XML/resource locale keys, GPL unchanged, recursive native revisions and git diff --check passed. No dependency upgraded or tracker added. The source ZIP excludes build caches, Git metadata, local SDK settings and private signing files.

## Not run

Phone/emulator rendering, dark/light/dynamic/RTL/accessibility/keyboard/D-pad, stable drag focus and stored order after reopen, actual upgrade installation, new subscription fetch/refresh with a real provider, process-death/rotation scenarios, live VPN and Play Protect check of this APK. Newly added URL detection is owned by the existing subscription ViewModel; if it is recreated between saving and returning, manual Home refresh can be needed. The existing user option for dynamic wallpaper colors overrides brand colors; explicit light mode remains available. No user endpoints or credentials were supplied or fabricated.

The user confirmed the prior 1.2.1 Release installed/opened after a Play Protect scan and did not repeat the scan request. This is user-reported behavior for the prior version, not Google certification or proof of this APK's behavior. No APK was uploaded to VirusTotal and no appeal was submitted. Existing source/license/build guides and earlier reports remain historical.

Final full JVM suite: 163 tests, 159 passed, 4 failed. All four failures are unchanged RootProcessRunnerTest cases: Windows cannot find sh (CreateProcess error 2); no tests disabled. Full test task exits failed. Logs/XML retained.

Final :app:lintFdroidRelease: BUILD SUCCESSFUL; 0 errors, 359 warnings. Release is not debuggable. No production code was changed after the final build/tests. See verification/1.3.0-lint-final.log and retained lint XML.
