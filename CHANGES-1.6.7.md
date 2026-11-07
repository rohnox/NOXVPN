# Special VPN 1.6.7

- Fix connection startup being blocked by a stop request to an inactive VPN service. Live owners acknowledge actual teardown; absent engines no longer need a service binding.
- Retain serialized switching between Xray, OpenVPN, Cisco and WireGuard, and distinguish preparation failure from native-engine initialization failure.
- Replace the oversized Xray navigation frame with a small group caption, fine rules and a soft selected-tab indicator. Keep all five destinations and localized labels.
- Preserve existing package, release signing identity, profiles and native dependencies.

See VALIDATION-1.6.7.md for executed checks and remaining limitations.
