# 1.5.2 — traffic volume and subscription information

- Replace the live bytes/second plot with cumulative upload/download volume of the current Xray connection. Samples are real service counters, bounded to the latest 60 observations while this screen model lives. It is not a 24-hour billing history. The subscription billing quota remains a separate panel directly below the graph.
- Prefer the decoded panel Profile-Title over the locally entered subscription label in the dashboard, server group labels and subscription list. Keep the saved label as fallback without rewriting user data. Plain, base64 and percent-encoded titles use the existing parser.
- Show remaining days, rounded up for a partial day, instead of an expiry date. Show expired explicitly; missing/no-expiry metadata is not invented. Refresh the visible countdown every minute.
- On MainActivity entry, refresh enabled URL subscriptions through the existing data source on IO, without blocking the screen or displaying empty-account toasts. Coalesce entry events within 60 seconds and overlapping manual/automatic updates. No automatic service restart is introduced. Failed requests retain existing cached configurations; the refresh control remains available.
- Keep package ID, signing identity, native engines, VPN modes and permissions unchanged. Version 1.5.2 / base versionCode 765.

Validation results and device-only limitations are recorded in verification/1.5.2 and VALIDATION-1.5.2.md.
