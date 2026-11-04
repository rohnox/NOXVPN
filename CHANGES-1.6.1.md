# Special VPN 1.6.1

Restore the exact 1.5.9 PasarGuard usage request/parser, config-import history integration and dashboard chart source. The provider's UTC daily buckets, date labels, day selection and 30-day request range behave as in 1.5.9. This supersedes the local-offset grouping introduced in 1.6.0.

Keep 1.6.0 OpenVPN profile deletion/UUID connection controls and the localized Update Sub button. Foreground metadata refresh remains, but calls the restored 1.5.9 UTC API/parser. It does not synthesize today's data or relabel an earlier bucket. The optional providerUsageZone field is retained for serialization compatibility but no longer controls this chart.

No customer credentials, endpoints, private profiles or signing key added. Native libraries, permissions, dependencies, package ID and release signer remain unchanged.
