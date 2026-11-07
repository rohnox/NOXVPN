# Physical-device checks

Signed Release 1.6.7 was installed over the existing installation without clearing data on an arm64 Android 16 / API 36 phone. The user performed taps because the device blocks ADB touch injection. ADB was used to inspect this application's service records and UI only. Raw logs, screenshots, account details and device identifiers are private and excluded from source.

- WireGuard: user confirmed connected and working internet; native controller and GoBackend VPN service active, UI offered Disconnect.
- WireGuard → Cisco without manual disconnect: Cisco service active, both WireGuard VPN owners absent; UI showed Connected and Disconnect. User confirmed connection, and separately reported working Cisco internet.
- Cisco → OpenVPN without manual disconnect: Cisco service absent, OpenVPN VPN service active; host UI showed Connected profile and Disconnect. User confirmed connection; independent traffic verification was not performed.
- OpenVPN → Xray without manual disconnect: Xray CoreVpnService active, OpenVPN VPN service absent. The passive OpenVPNStatusService remained, which is not an active VPN owner. User confirmed connection; independent traffic verification was not performed.

These checks preceded the final navigation-only redesign. The redesign changes presentation, not engine ownership. Not run: all remaining directed pairs on the physical device, teardown failure injection, process death/revoke, always-on startup, and device accessibility/keyboard/dynamic-color checks. Simulated tests of all directed pairs do not replace these unrun device scenarios.
