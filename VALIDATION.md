# Special VPN 1.6.5 — validation

Package com.specialvpn.app; base versionCode 778, universal versionCode 5077800. F-Droid universal distribution; Android 7+. L2TP is excluded. Xray provider daily history keeps its 1.5.9 UTC behavior.

## Executed

Targeted protocol and WireGuard command tests passed. Full JVM/Compose run: 327 total, 323 passed, 4 failed, 0 skipped. All four failures are unchanged RootProcessRunnerTest cases requiring a missing Windows sh executable. The full test command exits FAILED; no tests were disabled. Full XML results and logs accompany the source in verification/1.6.5.

Tests cover strict HTTPS Eylan customer URLs, same-origin download restrictions, stable profile UUIDs, supported protocol mapping, empty/invalid manifests, no ZIP following, credential-safe diagnostics, Gson catalog round-trip and manual-profile isolation. Repository update tests assert one commit after complete validation and retention of the old catalog on partial download, invalid config, duplicate URL or persistence failure. Engine-cache rollback restores cached profiles, settings and certificate files after a failed primary save. Cisco prefill tests preserve entered values and leave OTP/secondary password fields untouched.

WireGuard command tests cover repeated-start decisions, switching, stop/null restart and retry. Robolectric host tests verify cancelled consent sends no Xray stop or WireGuard start, and restored pending stable profile identity dispatches the explicit foreground-service command only after consent. These test dispatch, not a running native tunnel.

Compose checks exercise English dark/light, Persian RTL, populated/empty subscription screens, editing/clipboard actions, stable-ID Connect/Update, keyboard and D-pad tab activation. Synthetic fixture renders and semantics were inspected; they are not customer statistics. Privately supplied OpenVPN and WireGuard downloads passed their official format parsers. Private fixtures and credentials are outside the published source; no network connection was initiated by these tests.

## Not run

Android installation/upgrade from 1.6.4; actual provider subscription import/update/delete and screen reopen; real OpenVPN/Cisco/WireGuard connection, peer handshake and internet reachability; VPN consent cancellation/recreation on a device; cold start, duplicate starts, failed setup after resource acquisition, normal/repeated stop and stop during setup; revoke/process death/background and handover races across VPN/proxy/root modes. No usable device/emulator was established: ADB cannot create its required Windows user .android directory (Access denied), including after the session permission grant. JVM tests do not substitute for these scenarios.

TalkBack spoken labels/state/focus order, touch/keyboard/D-pad activation on a device, first-to-last form traversal and save with the real IME visible, dynamic color and device rotation/restoration: Not run for the same device blocker. No real-server or runtime compatibility success is claimed.

## Operational limits

The official prebuilt WireGuard userspace backend is embedded; native source was not rebuilt on this host. Up means the backend enabled the local tunnel, not that the remote peer handshake or internet reachability was independently measured. Always-on and automatic service restart are disabled for the new backend. Android permits one active VPN; the new launch route uses the existing Xray stop contract after consent.

Eylan adapter support is schema-based: customer /sub/token/username pages with the documented supported download-data structure and same-origin standalone OpenVPN/WireGuard files. Other panel versions, external downloads, ZIP packages, scripts and non-self-contained OpenVPN certificates are not supported. Limits: 8 subscriptions, 96 profiles each, 2 MiB per response, 24 MiB profile downloads per update, 32 MiB catalog. Updating requires the Update button; no automatic protocol-subscription refresh on app entry is claimed. Active managed profiles must be disconnected before replacing/deleting their subscription. Manual profile imports remain available.

Private catalog/configs/credentials use app-private storage and existing engine owners; no independent at-rest encryption claim. No administrative API key, customer configuration, tracker, advertisement or private release key was added to distributed artifacts. Original notices are retained. Full unchanged WireGuard source and dependency inputs are in the attached source ZIP; Git contains host adaptations and pinned Maven declarations.

## Final build and artifact checks

Normal (non-visual-fixture) :app:assembleFdroidDebug, :app:assembleFdroidRelease and :app:lintFdroidDebug completed successfully. Lint: 0 errors, 373 warnings, 1 hint. An initial foreground-service declaration error was corrected to the existing specialUse VPN lifecycle permission/subtype; the failed initial log is retained. No lint error was suppressed.

Both universal APK signatures and ZIP alignment passed. Release certificate SHA256: 3e70db5bbca2256d8df6b23d459302416e414c53a018d09d46778ddbc2c241a8. All 60 existing native libraries match 1.4.3 byte for byte; 12 new libraries match the pinned official WireGuard AAR across arm64-v8a, armeabi-v7a, x86 and x86_64. ELF alignment checks passed. Permission declarations match the preceding APK. Fonts/OFL, XML/locales, original GPL license, pinned existing native source revisions, Go module source checksums and release test-fixture exclusion passed. Source and public certificate are provided without a private signing key.
