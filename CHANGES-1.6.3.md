# Special VPN 1.6.3

- Remove Configuration tools from the Controls screen. The normal settings routes remain and the manual Check for update route is accessible directly.
- Browsing Cisco profiles no longer sends Xray a stop command. A host-supplied EmbeddedRuntime gate calls LauncherManager/MessageHelper only after the user confirms the actual Cisco connection/VPN-consent path. Cancelled consent does not invoke it; duplicate activity results are ignored and a destroyed/finishing activity cannot start a late connection. Actual daemon events continue to own the Xray status.
- On app entry, wait for the entry subscription refresh and selected-group load, then dispatch the existing CoreTestService real-delay test with onlyTcp=false. Re-entry uses the existing60-second entry gate; active/manual tests are not replaced. Pending entry work is cancelled on competing user actions and ViewModel disposal. Empty groups do not start a service. Storage/dispatch failures reset test state and log sanitized diagnostics.
- Server delay presentation distinguishes positive results, untested zero, and failed negative results. Failed tests show a red cross and localized Unreachable label on regular, compact and mini cards. This reports the last test outcome, not a permanent claim about the server.

No native binary, protocol, package identity, stored profile format, permission or dependency was changed. Release uses the existing signing key. GitHub update banner and1.5.9 UTC provider history behavior are preserved.
