# 1.4.0 — embedded OpenVPN and OpenConnect

- Added a Persian/English Compose connection hub accessible from Home and the drawer. Existing Xray dashboard explicitly labels its engine; no cross-engine synthetic status/traffic was added.
- Added bounded, self-contained `.ovpn` SAF import, upstream profile persistence, actual OpenVPN status IPC/traffic observation, native VPN launch/disconnect and a masked dynamic challenge response screen. Local scripts, external control directives and separate certificate/key files are rejected by this initial importer.
- Added an embedded OpenConnect profile/authentication manager for AnyConnect-compatible endpoints, using actual native VPN service/authentication and certificate confirmation. Inherited advanced forms are retained. Main UI keeps black/gold branding; Cisco manager accent is gold.
- Packaged four-ABI native inputs for both engines; kept original author and license notices. See NATIVE-ENGINES.md for exact upstream revisions, checksums and native rebuild limits.
- Disabled the fork's independent updater, omitted its ACRA and Xposed runtime, constrained imported components to internal use, avoided notification ID conflicts, and corrected host activity/FileProvider targets.
- Kept existing subscription, real-delay, auto-selection and Xray features. No default server, customer account, panel credential, tracking or advertising service was added.

See VALIDATION.md for executed checks and PROTOCOLS-FA.md for use and device-test limitations. Existing Xray export does not back up the new engines' profiles. Backend login codes and panel integration remain separate work.
