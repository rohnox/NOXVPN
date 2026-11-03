# Design QA

## Comparison target

- Source visual truth: `/home/pengyue/.codex/generated_images/019f6a1d-f7d3-7852-a42a-a6657f715925/call_Oc59mUTLp5GwUYSJwpU2W7vn.png`
- Implementation screenshot: `/home/pengyue/Codespace/openconnect-next-android/screenshots/verification/settings-redesign/13-profile-main-final.png`
- Full-view comparison: `/home/pengyue/Codespace/openconnect-next-android/screenshots/verification/settings-redesign/14-design-comparison-full.png`
- Focused comparison: `/home/pengyue/Codespace/openconnect-next-android/screenshots/verification/settings-redesign/15-design-comparison-focus.png`
- Viewport: Android emulator, 1080 x 2340 px at 440 dpi, approximately 393 x 851 dp
- State: Chinese locale, dark theme, profile `Example`, server `vpn.example.com`, profile editor main screen

## Findings

- No actionable P0, P1, or P2 findings remain.
- [P3] The generated reference has a faint vignette/glow behind some surfaces, while the implementation uses the app's existing solid Material 3 surface tokens.
  - Location: full profile editor background.
  - Evidence: the side-by-side full-view comparison shows identical hierarchy and grouping but slightly different background treatment.
  - Impact: minor stylistic difference only; the native implementation is more consistent with the existing light/dark theme system and avoids a one-off decorative effect.
  - Follow-up: keep the current token-based background unless the whole app adopts the vignette treatment.
- The earlier roomier preference-row treatment was resolved in the app-wide consistency pass with a shared compact row while retaining 48 dp minimum touch targets.

## Required fidelity surfaces

- Fonts and typography: passed. Native Android sans typography, title/body hierarchy, weights, line height, Chinese wrapping, and value contrast match the design intent. No truncation was observed.
- Spacing and layout rhythm: passed. Top app bar, section header, two basic fields, two navigation rows, and persistent bottom save action preserve the target composition. All controls remain visible at approximately 393 x 851 dp.
- Colors and visual tokens: passed. Existing OConnect Material 3 dark tokens map cleanly to the target blue accent, near-black surface, blue icon containers, secondary text, and dividers. Light theme was also verified in `16-profile-main-light.png`.
- Image quality and asset fidelity: passed. The screen contains no raster imagery. Navigation, security, settings, chevron, log, share, and expand icons use crisp Android vector assets aligned to the existing icon system.
- Copy and content: passed. Chinese labels and summaries are coherent, existing functions remain present, unset certificate/key values read `未设置`, and the save action is explicit.
- Icons: passed. Both navigation icons are centered in tonal circles, chevrons align to row centers, and toolbar/back/overflow icons remain native and accessible.
- States and interactions: passed. Back navigation, profile editing, authentication, profile advanced settings, overall settings, overall advanced settings, switches, FAQ expand/collapse, log empty state, log sharing, error close, error view-log, and error retry were exercised.
- Accessibility: passed for the tested scope. Tap targets are at least 48 dp, navigation and expand controls have content descriptions, text contrast is strong in light and dark themes, and long summaries wrap without overlap.

## Comparison history

### Iteration 1

- Earlier findings: none at P0/P1/P2 after the first normalized full-view and focused comparison.
- Fixes made from this comparison: none required.
- Post-fix evidence: the final full-view and focused comparison images listed above.
- Additional functional polish completed before the comparison: independent titled subpages replaced untitled nested preference dialogs; unset states, modern switches, direct error recovery, log empty/share states, FAQ disclosure, and update status details were verified on the emulator.

## Verification

- `./gradlew testDebugUnitTest lintDebug assembleDebug`: passed.
- Debug APK installation on the Android 36 emulator: passed.
- Runtime crash check during settings navigation and failed-connection retry: no fatal exception observed in logcat.
- Additional screenshots:
  - `03-authentication.png`
  - `04-profile-advanced.png`
  - `06-general-advanced.png`
  - `07-log-empty.png`
  - `08-faq-collapsed.png`
  - `10-connection-result.png`
  - `11-error-view-log.png`
  - `12-retry-result.png`
  - `16-profile-main-light.png`
  - `17-general-settings-final.png`

## Implementation checklist

- [x] Faithful profile editor hierarchy
- [x] Persistent bottom save action
- [x] Independent authentication and advanced pages with back navigation
- [x] Common versus advanced overall settings separation
- [x] Explicit unset states and modern switches
- [x] Direct retry, view-log, and close actions for connection failures
- [x] Log empty state and share action
- [x] Collapsible FAQ cards
- [x] Installed/latest/last-checked update summary
- [x] Dark and light theme verification

## Follow-up polish

- No additional density reduction is recommended without usability evidence.

final result: passed

## App-wide consistency pass

- Visual source of truth: the approved connected dashboard in `screenshots/verification/dashboard-connected-refined-v2.png`.
- Audit baseline: `screenshots/verification/app-wide-audit/01-profile-list.png` through `13-securid.png`.
- Final implementation evidence: `screenshots/verification/app-wide-audit/26-profile-list-final.png` through `36-login-wide-final.png`.
- Viewport and state: Android emulator, 1080 × 2340 px, 440 dpi, Simplified Chinese, dark theme.
- Primary before/after comparison: `screenshots/verification/app-wide-audit/app-wide-primary-comparison.png`.
- Support before/after comparison: `screenshots/verification/app-wide-audit/app-wide-support-comparison.png`.
- Detailed audit: `screenshots/verification/app-wide-audit/audit.md`.

### Findings and fixes

- Removed duplicate reconnect, disconnected-state, and login guidance so each screen has one clear status and one clear next action.
- Moved advanced choices out of profile creation while preserving them in Authentication and Advanced settings.
- Replaced legacy preference density with a shared compact row, quiet section labels, shorter summaries, neutral icon wells, and no list dividers.
- Unified page and dialog surfaces with the dashboard palette, 16 dp content-card corners, 20 dp dialog corners, and blue reserved for active or primary controls.
- Consolidated FAQ, reduced About to three essential groups, and converted SecurID from a technical warning into an actionable empty state.
- Localized and numbered repeated login fields and reduced the automatic-login explanation to one inline sentence.
- Corrected update-status wording when the installed build is already current.

### Required fidelity surfaces

- Typography: passed. Headings, labels, summaries, technical values, and Chinese wrapping form a consistent hierarchy without truncation.
- Spacing and layout rhythm: passed. Common settings, Authentication, and core profile editing now fit comfortably in one viewport while preserving touch targets.
- Colors and surfaces: passed. The dashboard, lists, settings, support pages, empty states, and dialogs use the same background, surface, outline, text, and accent roles.
- Icons: passed. Existing Android vectors remain crisp, use neutral wells where appropriate, and preserve accessible content descriptions.
- Copy and content: passed. Repeated or premature guidance is removed; technical depth remains in clearly named subpages.
- States and interactions: passed. Add validation, FAQ disclosure, SecurID recovery, profile navigation, switches, login cancel, and password visibility were exercised.
- Accessibility: passed for the tested visual/runtime scope. Text contrast is strong, primary tap targets are at least 48 dp, disabled actions are visibly distinct, and runtime labels expose meaningful Chinese text.

### Verification

- `./gradlew testDebugUnitTest lintDebug assembleDebug`: passed.
- Debug APK installation: passed.
- Emulator navigation across all audited pages: passed.
- Recent logcat check: no fatal application exception.
- No P0, P1, or P2 visual or interaction findings remain.

### Login dialog width refinement

- Earlier implementation: `screenshots/verification/app-wide-audit/34-login-final.png`.
- Refined implementation: `screenshots/verification/app-wide-audit/36-login-wide-final.png`.
- Side-by-side comparison: `screenshots/verification/app-wide-audit/login-width-comparison.png`.
- Increased the phone dialog from roughly 54% to 88% of the 1080 px viewport, matching the horizontal axis of the dashboard card.
- Kept 24 dp safe margins on phones and a 560 dp maximum width for larger screens.
- Forced the expanded dialog surface to remain opaque and rounded after runtime resizing.
- Field order, labels, visibility controls, checkboxes, buttons, and login behavior remain unchanged.
- No P0, P1, or P2 findings remain in the refined state.

final result: passed

## Connected dashboard design QA

- Source visual truth: `/home/pengyue/.codex/generated_images/019f6b41-303d-7c13-af78-58866ec6fb1e/call_7CxDZOFNyr3LKBrjV6uy2eOd.png`
- Iteration baseline: `screenshots/verification/dashboard-connected-final.png`
- Implementation screenshot: `screenshots/verification/dashboard-connected-refined-v2.png`
- Viewport: Android emulator, 1080 × 2340 px, 440 dpi
- State: Simplified Chinese, connected, populated profile, populated traffic metrics
- Full-view comparison: `screenshots/verification/dashboard-refinement-comparison.png`
- Focused hero comparison: `screenshots/verification/dashboard-refinement-hero-comparison.png`
- Focused card comparison: `screenshots/verification/dashboard-card-comparison.png`
- Connection details evidence: `screenshots/verification/connection-details.png`

### Findings and fixes

- The first pass made the hero too tall on a high-density Android device and pushed the lower actions below the first screen. Reduced hero spacing, type scale, button height, and row heights while preserving the visual hierarchy.
- Added connection duration, received/sent totals, and live transfer rates to the hero so key connection information is available without opening another screen.
- Removed the secondary gray copy beneath “连接详情”.
- Kept settings and help/about inside the same primary action card.
- Moved logs into the connection details screen and verified the connection-details-to-log navigation on the emulator.
- Replaced the generic shield with a connected shield/check mark and retained accessible labels and standard touch targets.
- The implementation intentionally differs from the source by using a slightly denser hero and no connection-details subtitle, matching the approved product changes.

### Iteration 2

- Removed the blue divider beneath the disconnect action. The hero and action card now separate through 28 dp of whitespace, reducing visual noise without merging the two sections.
- Moved the connected shield from a detached block above the heading into a 28 dp status badge after “已连接”.
- Restored a shared left alignment for the status heading, profile name, duration, traffic metrics, button, and card.
- Compared both left-of-title and right-of-title badge placements on the emulator. The right-side badge was retained because the left-side version displaced the primary heading from the page alignment axis.
- No P0, P1, or P2 findings remain. Typography, spacing, colors, vector icon quality, copy, touch targets, and the connected interaction state pass at the tested viewport.

### Verification

- `./gradlew testDebugUnitTest lintDebug assembleDebug`: passed
- Emulator launch and full-screen screenshot
- Connection details navigation
- Nested log navigation

final result: passed

## v1.16.0 release audit

- Final audit: `screenshots/verification/release-audit-v1.16.0/audit.md`.
- Before/after home-page evidence: `screenshots/verification/release-audit-v1.16.0/homepage-before-after.png`.
- Final disconnected home: `screenshots/verification/release-audit-v1.16.0/21-dashboard-dedicated-layout.png`.
- Final connected home: `screenshots/verification/release-audit-v1.16.0/19-connected-dashboard-preview.png`.
- Viewport and state: Android emulator, 1080 × 2340 px, 440 dpi, Simplified Chinese, dark theme.

### Final finding and resolution

- A P1 release blocker was found during the last pass: disconnected users still landed on the old profile/log/FAQ tab layout.
- Replaced the MainActivity tab host with a dedicated dashboard-only layout. The same dashboard now owns disconnected, connecting, waiting-for-input, and connected states.
- Profile management, logs, settings, FAQ, About, and SecurID remain available as focused secondary destinations.
- Refined disconnected Connection details to use `未连接` consistently and hide unavailable IP, subnet, and IPv6 rows.
- Verified the wider login dialog, nested log entry, settings, FAQ, add-profile dialog, connected dashboard, and dashboard overflow after the home-page correction.

### Verification

- `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease`: passed.
- Debug APK installation and launch: passed.
- Final runtime navigation and crash check: passed.
- No P0, P1, or P2 findings remain.

final result: passed
