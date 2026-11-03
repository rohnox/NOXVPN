# 1.4.3 — OpenVPN startup and Home quick actions

- Keep the OpenVPN/Cisco feature, its Home shortcut, native libraries, connection managers and OpenVPN `.ovpn` import button.
- Remove only the two Home Quick Access buttons marked in the user's screenshot: Add configuration and Test latency. Other Xray import/subscription/server/latency actions remain available in their existing screens. The status text under the shortcut is retained.
- Fix a deterministic OpenVPN startup failure: `GlobalPreferences.instance` was null because the embedded host does not run the original `ICSOpenVPNApplication` restrictions setup. Building the foreground notification calls `getForceConnected()`, which threw `RuntimeException: Global preferences instance is not set`. The failing call was reproduced on the JVM before the change; it returns false after the change.
- Give every process a safe default preferences snapshot (minimal UI false, force-connected false, initial managed import false). Keep managed overrides through the existing setter; publish snapshots through a volatile reference. New tests load independent copies of the actual Java class to check cold-process defaults and process-isolated managed overrides.
- Preserve the 1.4.1 asynchronous status-pipe/cache initialization fix. No native binary, protocol, server, user configuration, DNS/routing field or dependency version was changed.

The temporary whole-feature removal was undone before this release. No APK with that removal was delivered. Version 1.4.2 was an interrupted intermediate build, not a delivered release. See VALIDATION.md for actual build/test results and device limitations. The existing release key is retained for updates.
