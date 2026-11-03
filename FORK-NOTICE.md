# iSpeedTopVPN APP

Modified on 2026-11-02 from the official 2dust/v2rayNG release 2.3.14.

- Upstream: https://github.com/2dust/v2rayNG
- Release: https://github.com/2dust/v2rayNG/releases/tag/2.3.14
- Base commit: aa4b05503a504a3f2e43818e6ee8ebf1caaa3df0
- AndroidLibXrayLite: 76a6e2172210613fcb4aa32b6de55bcd9452ce43 (v26.10.21)
- HEV: c5584feca8f6df70ee53eb2879e034b849a3d15a

This is a modified fork, not an official v2rayNG build. Copyright in the original code remains with its original authors and contributors. The original LICENSE (GNU GPL v3) and source headers are retained. The modifications, including original vector branding, are distributed under GPL-3.0. Preserve this notice, the licenses and corresponding source when distributing modified binaries. Third-party components retain their respective licenses.

The Android application ID is com.ispeedtopvpn.app for both distributions. The internal Kotlin/JNI namespace intentionally remains com.v2ray.ang to retain native bindings and class compatibility. Existing installs of official v2rayNG remain separate. Manual export/import is required to move configurations; this fork never reads another application's private storage.

Brand changes: launcher/adaptive icon, editable SVG, splash, TV banner, notification mask, blue/turquoise/navy palettes, main title, drawer and About attribution. Added a labeled connection control, selected-server name, service-derived session duration and aggregate outbound upload/download. Existing import, QR, subscriptions, tests, protocols, DNS, routing and run-mode implementations are retained.

The upstream promotion and upstream-APK update entries are hidden in the fork's main navigation. No fork update URL, VPN provider, credentials, advertising, analytics or tracker has been added. Upstream source/feedback links still identify the original project. Automatic Android backup is disabled; explicit user-controlled configuration backup remains available.

Session counters are produced only by the daemon's existing sampler and carried through package-scoped MessageHelper IPC to MainRepository/MainViewModel. UI never loads or queries the core. Screen-off suppresses speed-notification updates while accounting continues; stop cancels sampling. Displayed bytes are aggregate outbound core traffic, including direct traffic, not the carrier's billed usage. Chained outbounds can count a byte at more than one outbound. Counters reset on service-session restart. Data shows an em dash when speed statistics are disabled or unavailable; enable speed display and reconnect to enable the upstream core statistics configuration. Custom raw configs must also enable their outbound statistics policy. Session duration describes core uptime and does not certify Internet reachability; the existing real-ping test checks reachability separately.


Frontend release 1.1.0: the main UI was redesigned as an original four-tab dashboard, server-card browser, subscription quota overview and controls hub. A bounded real-session rate history and explicit availability of panel counters were added with backward-compatible JSON tests. See REDESIGN-FA.md and VALIDATION.md for scope and limitations.

Version 1.2.0 adds cancellable real-ping automatic server selection, persisted-selection acknowledgement and request/generation guards. kotlinx-coroutines-test 1.11.0 is test-only; no production telemetry dependency was added.

Version 1.3.0: metallic black/gold theme and vector branding; empty internal Default plan hidden without deleting storage; stable-ID reorder and immediate fetching of newly added subscriptions in the subscriptions ViewModel. Existing GPL attribution retained.
