# v1.16.0 release UX audit

## Scope

- Viewport: Android emulator, 1080 × 2340 px at 440 dpi.
- Locale and theme: Simplified Chinese, dark theme.
- Profiles and data: saved `NYU` profile for real disconnected and authentication states; a temporary QA-only `Example` fixture for a deterministic connected-state preview. The fixture is not included in the release source or APK.
- Audit evidence was captured during this release run.

## Critical finding resolved

- [P1] The first release candidate still returned disconnected users to the legacy top-tab profile list.
  - Pre-fix evidence: `01-profile-list.png`.
  - Impact: the app had two different home-page mental models depending on connection state.
  - Fix: `MainActivity` now owns a dedicated dashboard-only layout and always hosts `StatusFragment`. Profile management, logs, FAQ, and settings open as focused secondary pages instead of home tabs.
  - Post-fix evidence: `21-dashboard-dedicated-layout.png`.
  - Direct comparison: `homepage-before-after.png`.

## Final checks

- Splash to home: passed. The launch sequence resolves directly into the dashboard; the main activity no longer inflates the tabbed layout.
- Disconnected dashboard: passed. Selected profile, server, primary connect action, details, settings, and help/about are visible without redundant guidance.
- Profile management: passed. The dashboard profile row opens the standalone profile list with back, add, edit, and overflow actions.
- Add profile: passed. The focused dialog asks only for the gateway address and leaves certificates, tokens, and authentication choices for the profile editor.
- Authentication: passed. The dialog is wide enough for labels and fields, remains opaque and rounded, and cancel returns to the dashboard.
- Connection details: passed. The disconnected state says `未连接`, retains the server name, hides meaningless unknown network fields, and exposes logs as the single diagnostic entry.
- Logs: passed. The nested log page opens from Connection details and keeps retry/reconnect context visible.
- Settings: passed. Common controls are compact, advanced controls remain available, and update status is legible.
- FAQ and About: passed. Help remains a single dashboard entry with two clear destinations.
- SecurID and update check: passed. Both remain available in the dashboard overflow menu.
- Connected dashboard: passed. Status badge, profile, duration, traffic, disconnect action, and secondary destinations remain visible on the first screen.
- Accessibility: passed for the tested scope. Primary targets meet 48 dp minimums, text contrast is strong, icons have descriptions where they convey actions, and secondary text wraps without overlap.

## Evidence index

- `10-login-on-dashboard.png`: authentication dialog over the dashboard.
- `13-disconnected-details-refined.png`: refined disconnected Connection details.
- `14-log-from-details.png`: log reached from Connection details.
- `15-settings-from-dashboard.png`: settings reached from the dashboard.
- `16-help-dialog-from-dashboard.png`: Help & About chooser.
- `17-faq-from-dashboard.png`: FAQ destination.
- `18-add-profile-dialog.png`: focused add-profile flow.
- `19-connected-dashboard-preview.png`: deterministic connected state.
- `21-dashboard-dedicated-layout.png`: final disconnected dashboard.
- `22-dashboard-overflow.png`: update and SecurID actions.
- `23-about-version-1.16.0.png`: final in-app version and About layout.
- `24-final-runtime-dashboard.png`: final APK runtime smoke test.
- `homepage-before-after.png`: rejected tabbed home versus final dashboard.

## Result

No P0, P1, or P2 UX, visual, or interaction findings remain.

final result: passed
