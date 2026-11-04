# Special VPN 1.6.0

- Restore the localized Update Sub button with refresh icon in the selected subscription summary. Other summary actions stay on individual list cards.
- Add local OpenVPN profile deletion with confirmation. Deletion is disabled while the daemon is connecting, connected, awaiting input, or its state is unknown. Profiles remain identified by UUID even when names match.
- Connected profile checkmark and Connect/Disconnect label follow the daemon status and exact active UUID. Initial screen entry queries the daemon's last-connected UUID on IO; stale queries are invalidated on newer status or owner disposal.
- Request PasarGuard daily history using the phone's current fixed UTC offset and thirty calendar dates including today, instead of UTC boundaries. Persist the grouping offset as an optional backward-compatible field and use it when labeling daily points.
- Refresh provider history while Home is resumed, at most once a minute and at the local midnight boundary. This metadata-only refresh does not re-import profiles or change the VPN server. It stops when Home pauses and rejects results for a superseded subscription.
- If the provider has not supplied today's data, display an explicit pending message instead of inventing a zero sample. Provider update latency remains outside the app's control. Daily grouping follows the current fixed offset; historical DST transitions are not reconstructed.

No customer endpoint, profile, password, API key or private signing material is included. The public token-scoped history endpoint requires no administrator API key. Existing protocols, native binaries, licenses and package identity are preserved. See VALIDATION-1.6.0.md for executed checks and unrun device scenarios.
