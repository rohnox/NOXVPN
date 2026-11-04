# Special VPN 1.5.8 — validation

Scope: country of the actual exit IP, dedicated runtime Quick Settings icon, subscription edit navigation and clipboard/QR sharing. Package com.specialvpn.app; base versionCode771. See CHANGES-1.5.8.md.

## Checks

Country parsing tests validate matching IPv4/IPv6 addresses, ISO country codes and rejection of malformed, mismatched and oversized data. ViewModel tests retain the successful IP while a country lookup is pending and reject its late result after stop. Existing bounded proxy-only request, invalid-port and cancellation tests remain enabled. No customer IP or configuration was queried during these tests.

Subscription tests resolve the URL by stable ID, trim it, reject missing/blank records, check clipboard success/failure, QR failure/success and dismissal. Compose tests dispatch Edit, copy and QR for the selected subscription ID. Existing selection, deletion, keyboard and D-pad tests remain enabled; their virtual-list lookup scrolls before accessing an off-screen card. The existing editor still owns validation, persistence and settings-change notifications.

The Quick Settings regression test calls setState(INACTIVE) and captures the actual Tile icon resource. This covers the density-resource override missed by the earlier manifest-only test. The production change is a dedicated resource reference only; no service state/lifecycle decision changed.

## Not run

- Physical-device install/update and Quick Settings active/inactive transitions on Xiaomi/Samsung: no usable device/emulator attached. If the pinned tile belongs to the older, separate app package, remove that tile and add Special VPN.
- Live geolocation over a real VPN, provider outage/rate-limit behavior and IP-country accuracy: no device tunnel. Country is approximate; failure leaves the valid IP visible with an unknown country. IPv6 parser coverage does not prove mobile network connectivity.
- Actual subscription editor save/close/reopen, Android clipboard paste into another application, QR scan by another phone, process recreation and platform IME behavior: not run on hardware. The existing editor/storage/QR encoder is reused.
- TalkBack spoken labels/focus and hardware touch/keyboard/D-pad, dynamic color, large text and both server columns: no device available. JVM fixture assertions are limited to their covered behavior.
- Play Protect scanning/certification: not performed.

Final combined run:277 tests,273 passed,4 failed,0 skipped,0 errors. The four unchanged RootProcessRunnerTest failures are caused by Windows missing sh (CreateProcess error2). The full test command exits failed; no tests are disabled. Results: verification/1.5.8/all-tests.log and jvm-full/. The focused parser/ViewModel run also completed BUILD SUCCESSFUL before the final expanded suite.

Country-flag tests reject unknown/lowercase/invalid codes. The decorative badge and full-card flag background with a gradient scrim are enabled only for a running connection with a known country; disconnected/loading/unavailable states retain the original background. The emoji is excluded from accessibility speech. Compose renders inspected in English dark/light and Persian RTL include the country badge and full-card flag, with legible controls and rounded clipping and subscription edit/share actions, plus existing English/Persian RTL, light/dark and populated/empty fixtures. Screenshots contain reserved/synthetic values; they are not live geolocation results. QR generation reuses the existing encoder, with no new dependency or external transmission.

Normal F-Droid Debug/Release assembly and Debug lint completed BUILD SUCCESSFUL with optional UI-test dependencies excluded. Lint: zero errors,371 warnings,one hint. The added warning is the upstream ic_stat_name resource becoming unused after the runtime icon fix; no suppression was added.

Release v2/v3 signatures, ZIP16KiB alignment, both APK identities, all four ABI inputs, absence of ACRA/Xposed, font/OFL packaging and absence of UI fixtures in Release DEX passed. All60 native libraries match1.4.3 byte-for-byte; ELF alignment and permission capabilities are retained (application-specific receiver permission normalized for the package rename in1.5.7). Original license, resource XML, source diff and recursive native source pins passed.

Outputs: SpecialVPN-1.5.8-universal-release.apk, SpecialVPN-1.5.8-universal-debug.apk and SpecialVPN-1.5.8-source.zip. Existing release certificate SHA256 remains `3e70db5bbca2256d8df6b23d459302416e414c53a018d09d46778ddbc2c241a8`. SHA256SUMS-1.5.8.txt records artifact hashes. The source archive keeps the historical iSpeedTopVPN top folder and includes native sources/inputs. Private subscription links, configurations and signing keys are excluded. Signing/build logs are under verification/1.5.8/.
