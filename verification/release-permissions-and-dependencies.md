# Release review scope

Application: iSpeedTopVPN APP, com.ispeedtopvpn.app, version 1.2.1. Based on official GPL v2rayNG 2.3.14 (aa4b05503a504a3f2e43818e6ee8ebf1caaa3df0). This is a limited source/manifest/dependency review, not a malware certification or comprehensive vulnerability audit.

## Permissions retained

| Permission / capability | Purpose |
| --- | --- |
| INTERNET, ACCESS_NETWORK_STATE, CHANGE_NETWORK_STATE, ACCESS_LOCAL_NETWORK | VPN/proxy networking, connectivity and local-network operation |
| FOREGROUND_SERVICE, FOREGROUND_SERVICE_SPECIAL_USE | User-visible VPN, proxy, root and test services |
| POST_NOTIFICATIONS | Connection notification; not notification-listener access |
| CAMERA | Optional user-initiated QR import |
| QUERY_ALL_PACKAGES | Installed-app selection for per-app routing; Google Play distribution may require a policy declaration |
| RECEIVE_BOOT_COMPLETED | Existing optional connection/startup behavior |
| WAKE_LOCK | Background work/service lifetime |
| Application-specific receiver permission | AndroidX non-exported receiver protection |

No READ_SMS, RECEIVE_SMS, contacts, microphone, accessibility-service or notification-listener permission was observed. The VPN service is non-exported and declares BIND_VPN_SERVICE. Quick Settings tile declares BIND_QUICK_SETTINGS_TILE. Existing launcher/import, widget, boot and Tasker integration entry points are retained; no new exported component was added for this release.

## Dependency/source observations

- Runtime libraries are the existing AndroidX/Compose/Lifecycle/WorkManager/CameraX stack, MMKV, Gson, OkHttp, Kotlin coroutines, ZXing, Coil and reorderable, plus libv2ray AAR and HEV native libraries. Exact resolved transitives are in release-dependencies.log.
- No advertising/analytics SDK was added. Searches of declared dependencies for Firebase/Crashlytics, AppsFlyer, Adjust, Facebook and AdMob found no matches; this limited search does not prove the absence of all possible tracking behavior.
- Mockito, Byte Buddy agent and kotlinx-coroutines-test are test tooling, not Release runtime dependencies. Compose inspection tooling is debug-only.
- The AAR and native input provenance is recorded in the source validation reports. No native code upgrade is included here.
- No user server, subscription or credentials are embedded in this build. Network functions retain upstream defaults: real-delay probes to generate_204 URLs, IP lookup, DNS and optional upstream update/assets/WebDAV paths. These are functional network operations and must not be represented as an app that never contacts third parties.
- The inherited app-update checker points at upstream v2rayNG releases. Those are not signed updates for this fork. Distribute iSpeedTopVPN updates using the dedicated release certificate; no fork update hosting URL has been supplied.
- Existing cleartext and user-installed CA trust support is retained for compatibility with user-supplied networking configurations. It is a security tradeoff; this review does not certify arbitrary imported configurations or their providers.

## Boundaries

No secrets or private signing keys belong in a Google/VirusTotal submission. Only upload the exact signed public APK, and identify it by its SHA256. Device installation, live VPN and Play Protect acceptance were not tested here. Signing and static checks do not establish Google approval. No online scanner submission has been made as part of preparing this packet.
