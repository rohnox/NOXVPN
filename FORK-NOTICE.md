# Special VPN

Modified fork of the official [2dust/v2rayNG 2.3.14](https://github.com/2dust/v2rayNG/releases/tag/2.3.14), initially modified on 2026-11-02.

- Base commit: aa4b05503a504a3f2e43818e6ee8ebf1caaa3df0
- AndroidLibXrayLite: 76a6e2172210613fcb4aa32b6de55bcd9452ce43 (v26.10.21)
- HEV: c5584feca8f6df70ee53eb2879e034b849a3d15a

This is a modified fork, not an official v2rayNG build. Copyright in the original code remains with its original authors and contributors. The original LICENSE (GNU GPL v3) and source headers are retained. Modifications, including project branding, are distributed under GPL-3.0. Preserve this notice, licenses and corresponding source when distributing modified binaries. Third-party components retain their respective licenses; see NATIVE-ENGINES.md.

The application ID is com.specialvpn.app. Internal Kotlin/JNI namespaces intentionally remain com.v2ray.ang for native bindings and class compatibility. Official v2rayNG installations remain separate; migrate configurations with explicit export/import. This application never reads another application's private storage.

Special VPN adds a Persian/English dashboard, subscription quota and compatible provider history, real-delay testing, exit IP/country presentation, dedicated navigation and optional GitHub release updates. OpenVPN, OpenConnect and WireGuard use their own embedded engines and licenses. No server, administrative credential, advertisement or tracker is bundled. Update checks use this project's public repository; Android requires user confirmation for installation. Backup/export remains user controlled.

Session counters come from the running engine and may differ from provider billing. Subscription quota/history depends on metadata supplied by the provider. Core uptime and an enabled tunnel do not independently prove internet reachability. See VALIDATION.md for executed checks and limitations.
