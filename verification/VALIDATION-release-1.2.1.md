# Signed Release validation — 1.2.1

No production code, native library, dependency or permission was changed in this release-preparation step. It builds the source used for 1.2.1 Debug as F-Droid Release, then aligns and signs the universal APK using a dedicated RSA3072 release key outside the repository. No obfuscation was added; existing release isMinifyEnabled=false retained.

## Executed

- :app:assembleFdroidRelease — BUILD SUCCESSFUL.
- :app:dependencies --configuration fdroidReleaseRuntimeClasspath — BUILD SUCCESSFUL; resolved tree saved as release-dependencies.log. No unresolved dependency markers, advertising SDK matches in the named search, Mockito/Byte Buddy/coroutines-test or debug-only UI tooling found in that runtime tree. This is not a comprehensive vulnerability scan.
- apksigner verify: v2 and v3 valid; zipalign -c -P16 4 passed. Native libraries present for arm64-v8a, armeabi-v7a, x86 and x86_64; embedded GPL and all brand labels checked.
- aapt2 manifest: com.ispeedtopvpn.app, versionName 1.2.1, universal versionCode 5075700, minimum API24, target API37. application-debuggable absent; Release is not debuggable.
- APK SHA256: c4c56459af4e0a03e273e8f53fcf98970eceede01ff908903a3da429a05c0f39.
- Release certificate SHA256: 3e70db5bbca2256d8df6b23d459302416e414c53a018d09d46778ddbc2c241a8. Public PEM is supplied; private PKCS12/password are outside source and outputs. This certificate differs from Debug: direct update from Debug will fail; back up configurations before uninstalling Debug.
- Prior unchanged-source Debug validation: targeted main tests 44/44 passed; full 156 tests had 152 passes and four existing RootProcessRunnerTest failures because Windows lacks sh. Not rerun during packaging/signing. Historical logs retained.

## Review and limitations

See RELEASE-FA.md and verification/release-permissions-and-dependencies.md. No sensitive user configurations were included. Existing networking/QR/per-app/VPN permissions retained. Signing is not Google approval. No VirusTotal upload, Play Protect appeal, developer identity registration, release hosting or Play Store publication was performed.

Not run: actual Release installation, Debug-to-Release backup/restore, phone/emulator UI and VPN/proxy/root execution, real subscription/latency tests, Play Protect detection/acceptance of this exact Release APK, complete vulnerability audit and Play Store build. User screenshots demonstrate a warning on the earlier Debug APK, not this Release APK.

Final :app:lintFdroidRelease — BUILD SUCCESSFUL, 0 errors and 359 warnings. Static XML/resources/GPL/git diff/native-pin checks also passed. Reports retained in verification/. No code changes were made to hide lint findings.
