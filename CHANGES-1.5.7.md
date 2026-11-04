# Special VPN 1.5.7

- Display name changed to Special VPN; new application ID `com.specialvpn.app`. This installs separately from the old package. Existing configurations are not automatically migrated. The upstream source namespaces, licenses and authorship remain intact.
- Selected gold S/open-hexagon logo used for launcher, splash and header. Editable SVG companion and original selected PNG included in branding/. Dedicated monochrome icon used for notifications and Quick Settings, including runtime tile updates.
- Home timer renders every second from monotonic daemon snapshots. Upload/download accounting is independent of the notification-speed preference; bytes remain real core counters.
- Cisco titles use unique resources to avoid the OpenVPN library name collision. Connected notifications identify Cisco or OpenVPN from actual engine state.
- Home subscription update control is a compact outlined gold pill with localized text.
- Home shows the public exit IP after Xray connects, with manual refresh. A bounded HTTPS request to api64.ipify.org uses the daemon-reported local proxy port; there is no direct-network fallback or redirect. The result is transient, cleared on disconnect and never logged. Failure shows Unavailable. The IP describes the route to this provider; custom routing can select a different exit for other sites. Custom configurations without the expected proxy listener cannot supply this check.

No server credentials, customer subscription URLs, signing keys, trackers, advertisements or additional permissions were added. See VALIDATION-1.5.7.md for executed checks and device limitations.
