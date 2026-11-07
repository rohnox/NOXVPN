# Special VPN 1.6.7 — validation

F-Droid universal, com.specialvpn.app, base versionCode 780 / universal 5078000. Native inputs and release signer are unchanged.

## Startup regression

The installed 1.6.6 release reproduced a reply-timeout for CoreVpnService while Android's service record had no live process. A non-creating bind could be accepted without delivering a callback; treating that as a live owner blocked startup. Version 1.6.7 registers a non-exported stop receiver only for the lifetime of each actual VPN owner. An absent owner completes without a native-service bind; an existing owner claims the ordered broadcast and replies only after its real stop handler finishes. Cancellation, timeout and failed teardown still abort the new dispatch. No native engine is started to ask an absent engine to stop.

## Tests

Before the navigation redesign, 80 targeted tests passed, including 18 owner-channel cases across Robolectric API 24, 31 and 35. All 12 directed handovers are exercised with simulated owners, plus absent owners, real coordinator IPC, cancellation, failure and timeout. The full JVM run completed 372 tests: 368 passed and four unchanged RootProcessRunnerTest cases failed because Windows lacks the required sh executable. No tests were disabled. These results do not imply all native handovers were tested on a device.

After the final navigation redesign, all 42 PremiumVisualTest cases passed. English, Persian/RTL, light and wide renders were generated; phone and wide navigation renders were inspected. Tests include touch selection, keyboard Enter and D-pad center activation. Navigation actions, domain identifiers and localized labels were retained. The thick Xray frame was replaced with a small caption and fine rules; the selected tab uses a soft gold indicator.

Physical-device installation and three consecutive handovers are recorded in [DEVICE-CHECKS.md](verification/1.6.7/DEVICE-CHECKS.md). Customer data and raw device logs are not included. That report distinguishes active VPN owners from passive status services and user traffic confirmation from independent traffic testing.

## Not run

Remaining physical-device handover pairs; process-death/revoke/always-on scenarios and native teardown failure injection; device TalkBack spoken output and focus order; device keyboard, IME and dynamic-color permutations. The full JVM suite has the four Windows sh failures above. Synthetic appearance and IPC tests do not prove these unrun scenarios.

Build, lint, signature, source-package and final artifact details are recorded in the accompanying verification logs when executed. Do not interpret a prior artifact hash as the hash of a later navigation rebuild.

## Final navigation build

Normal Debug and Release assembly plus lintFdroidDebug passed in 3m 7s after the redesign. Release signature and alignment checks passed with the existing signer. Final Release SHA-256: a39d04230f91d3ccdb2277d0ecedc1bdf8b1f00a6e69dad1a204dc48ccf29617. The rebuilt Release installed successfully over the existing phone installation, and the host UI exposed Xray plus all five navigation destinations. Final navigation device TalkBack and keyboard checks remain Not run.
