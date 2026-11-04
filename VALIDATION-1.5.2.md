# iSpeedTopVPN 1.5.2 — validation (2026-11-04)

Scope: cumulative traffic volume, provider quota/title, remaining days and automatic subscription refresh on MainActivity entry. See CHANGES-1.5.2.md.

## Executed

- Normal F-Droid Debug and Release assembly plus Debug lint completed BUILD SUCCESSFUL. Final log: verification/1.5.2/build-final.log. Optional UI-test dependencies/activity are absent from these normal builds.
- Normal JVM suite: 191 tests, 187 passed; four unchanged RootProcessRunnerTest failures because Windows cannot execute sh (CreateProcess error2). No tests disabled; the full-suite command exits failed. Seven new tests cover cumulative bytes, invalid/empty samples, provider-title fallback, remaining-day boundaries/overflow, entry throttling, concurrent updates, failed/empty updates and exception retry.
- Eight Robolectric native-graphics API35 checks passed at 393x852dp: populated/empty English home, Persian RTL, light home, OpenVPN empty/profile, Cisco selection and bottom navigation. The populated English check asserts provider title replaces import sub and remaining days are visible. These are actual components with isolated test fixtures, not live-connection/device tests. Renders and semantics are in verification/1.5.2/renders/.
- Final lint results are in verification/1.5.2/lint.xml. Initial missing Arabic/Russian plural categories and irrelevant Vietnamese/Chinese categories were fixed without suppressions. full-checks.log retains that earlier failed run; build-final.log records the corrected successful build. An initial edit overlapping analysis also caused a FIR analyzer failure; the stable final build passed.
- Release uses the existing signing certificate; signature/ZIP16KiB verification, four ABIs, licenses and native ELF alignment are checked in sign.log and elf.log. All 60 native libraries and permission declarations match release1.4.3. Font hashes match source and the project UI fixture is absent from Release DEX.
- Source XML, original GPL, original native gitlinks and diff checks passed. Canonical upstream guides fetched and compared; no instruction-file changes are published. No key, password, customer configuration, tracker or advertising added.

## Not run

- Physical-device/emulator install/update and actual VPN connection on 1.5.2: no usable device/emulator attached. A previous-version user screenshot is not a 1.5.2 test.
- Real subscription endpoint: no customer URL supplied for this task. Provider name requires Profile-Title; existing parsing supports plain/base64/percent-encoded values. Missing title retains the saved label. Quota/expiry depend on provider metadata.
- Device foreground/re-entry, rotation, permission-dialog return, process restoration, persisted subscriptions after restart and offline/online recovery; TalkBack spoken labels, touch/keyboard/D-pad, large font scale, dynamic color and both server-column settings: no device/emulator. JVM tests and renders cover only their explicit assertions.
- Google Play Protect scanning/certification for this APK: not performed; no approval claim.

The chart shows cumulative bytes of the current Xray connection, using the latest 60 in-memory observations. It is not 24-hour billing history. The separate quota card reports provider account counters, potentially including other devices. Entry events within 60 seconds and overlapping manual/automatic updates are coalesced. Automatic refresh runs on IO without a blocking overlay; failures preserve cached configurations, and manual refresh remains available.
