# OConnect app-wide design audit

## Audit scope

- Product: OConnect for Android
- Goal: connect to and manage an SSL VPN with a clean primary flow while retaining advanced configuration depth
- Mode: combined UX, visual consistency, and screenshot-based accessibility audit
- Evidence viewport: Android emulator, 1080 × 2340 px at 440 dpi, Simplified Chinese, dark theme

## Captured flow

1. `01-profile-list.png` — Profile list. Healthy core action, but the legacy tabs and bottom reconnect action duplicate navigation and connection entry.
2. `02-log.png` — Disconnected log. Clear empty state, but the status card repeats the same disconnected context and consumes first-screen space.
3. `03-faq.png` — FAQ list. Functional disclosure controls, but seven separate outlined cards create excessive visual weight.
4. `04-profile-editor-main.png` — Profile editor. Good separation of basic/authentication/advanced concerns, but the filled category strip and full-width dividers conflict with the connected dashboard.
5. `05-profile-auth.png` — Authentication settings. Complete feature coverage, but large rows expose too many technical controls with weak progressive disclosure.
6. `06-profile-advanced.png` — Advanced profile settings. Appropriate advanced depth, but long summaries and heavy separators make scanning difficult.
7. `07-overflow-menu.png` — Main overflow. Menu labels are understandable; About and FAQ are split across different navigation models.
8. `08-general-settings.png` — General settings. Useful grouping, but oversized preference rows and the update status block create a dense, document-like page.
9. `09-general-advanced.png` — Advanced app settings. Low-frequency compatibility controls are correctly separated, but disabled states and category bars lack compact hierarchy.
10. `10-about.png` — About. Trust information is present, but three large link buttons plus long explanatory and license cards are redundant.
11. `11-add-profile.png` — Add profile. Gateway input is clear; asking users to choose among three automatic-login policies before the first connection is premature.
12. `12-connection-entry.png` — Login prompt. Two password inputs share the same label and the generic server message remains in English, creating a high-risk ambiguity.
13. `13-securid.png` — SecurID. Empty state explains the technical setup but exposes a raw localhost URL and provides no clear action.

## Strengths

- High-frequency connection actions are already distinct from configuration actions.
- Advanced profile and app controls are separated into dedicated subpages.
- Major controls have practical mobile touch targets.
- Dark theme contrast is generally strong.
- Empty, disconnected, and error-oriented copy exists instead of blank screens.

## Priority findings

- [P1] Ambiguous duplicate password fields in the login prompt.
  - Evidence: `12-connection-entry.png`.
  - Impact: users cannot tell which credential belongs in each field.
  - Fix: disambiguate repeated labels, remove generic duplicate guidance, and keep save/automatic-login explanation inline.
- [P2] Visual tokens and surface hierarchy drift between the connected dashboard and legacy pages.
  - Evidence: `01-profile-list.png`, `04-profile-editor-main.png`, `08-general-settings.png`.
  - Impact: the product feels like several interfaces stitched together.
  - Fix: map all dark-theme tokens to the dashboard palette and replace heavy category fills with compact labels and grouped surfaces.
- [P2] Premature advanced choices in profile creation.
  - Evidence: `11-add-profile.png`.
  - Impact: a first-time user must understand credential reuse before connecting once.
  - Fix: create profiles with the safe recommended default and keep alternate policies in Authentication settings.
- [P2] Repeated disconnected and reconnect information.
  - Evidence: `01-profile-list.png`, `02-log.png`.
  - Impact: repeated messages compete with the primary profile action.
  - Fix: remove the duplicate reconnect footer and avoid a separate disconnected status banner when the log is empty.
- [P2] FAQ, About, and SecurID use too many containers or too much explanatory copy.
  - Evidence: `03-faq.png`, `10-about.png`, `13-securid.png`.
  - Impact: low-frequency support pages feel heavier than the core product.
  - Fix: consolidate FAQ into one grouped list, reduce About to identity/link/license essentials, and give SecurID a concise actionable empty state.

## Accessibility limits

- Screenshots confirm contrast, visible labels, hierarchy, and approximate target size only.
- TalkBack reading order, switch announcements, focus restoration, dynamic text scaling, and external keyboard traversal require runtime checks after implementation.

## Implementation resolution

- Unified the dark palette with the approved connected dashboard: near-black background, one restrained surface color, neutral icon wells, blue only for active states and primary actions, and consistent 16 dp content-card corners.
- Removed duplicate reconnect and disconnected-state messaging from the profile and empty-log screens.
- Simplified profile creation to one required gateway field; credential reuse remains available after creation under Authentication.
- Reworked all preference pages around a compact 68 dp row component with short summaries, quiet category labels, no list dividers, and dedicated subpages for advanced depth.
- Consolidated FAQ into one disclosure list, shortened About to identity/support/license essentials, and replaced the SecurID technical error with an actionable empty state.
- Clarified the login dialog with localized labels, numbered repeated password fields, a short save/automatic-login explanation, and a dialog surface matching the rest of the app.
- Corrected update status semantics so an installed version newer than the last recorded release is still shown as current.

## Refined evidence

1. `26-profile-list-final.png` — Clean profile entry with one server summary and no duplicate reconnect footer.
2. `27-add-profile-final.png` — Single-field creation dialog with a disabled confirm action until required input is present.
3. `28-general-settings-final.png` — Common settings fit on one screen with shorter descriptions and retained switches.
4. `29-about-final.png` — Three concise grouped surfaces for identity, support, and licensing.
5. `30-securid-final.png` — Clear empty state with a direct return-to-profile action.
6. `31-profile-editor-final.png` — Basic profile information and two deeper configuration entrances share the same hierarchy.
7. `32-profile-auth-final.png` — Certificate, token, and credential controls remain complete while fitting on one screen.
8. `33-profile-advanced-final.png` — Technical protocol controls remain available in the dedicated advanced screen.
9. `34-login-final.png` — Duplicate password prompts are disambiguated and the generic English message is removed.
10. `35-faq-expanded-final.png` — Disclosure interaction, wrapping, and grouped-card behavior verified.
11. `36-login-wide-final.png` — Login dialog now aligns with the main content width while retaining safe margins and the same field hierarchy.

## Final comparison and validation

- Primary before/after comparison: `app-wide-primary-comparison.png`.
- Support before/after comparison: `app-wide-support-comparison.png`.
- Login-width comparison: `login-width-comparison.png`.
- Runtime viewport: Android emulator, 1080 × 2340 px at 440 dpi, Simplified Chinese, dark theme.
- Verified profile editing, Authentication and Advanced navigation, FAQ expansion, SecurID return action, add-profile validation, login cancel, and password visibility controls.
- `./gradlew testDebugUnitTest lintDebug assembleDebug`: passed.
- Recent runtime log check: no fatal exception from the application.
- No P0, P1, or P2 design findings remain in the audited flows.
