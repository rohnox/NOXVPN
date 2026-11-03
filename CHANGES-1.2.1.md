# Changes in 1.2.1

- Servers / Test latency dispatches TestRealAllServers instead of TestAllServers. MainViewModel sends onlyTcp=false to the existing real-ping worker, which tests the configured real-ping URL through each profile. Existing cancellation and result display are retained; the legacy TCP action remains available elsewhere.
- Home heading includes a localized, accessible refresh icon. It dispatches UpdateGroupSubscription with the existing empty-ID/all-subscriptions contract, irrespective of the currently selected server/group. The existing update owner handles IO, progress indicator, completion/error messages and refreshed groups. Empty subscriptions use the existing no-subscriptions message.
- Version 1.2.1, application code 757 (F-Droid universal code 5075700). No core/dependency changes.

Not run: device UI/TalkBack/keyboard/D-pad checks, real server latency comparison and end-to-end subscription refresh (no test device or provider supplied). No runtime success is claimed.
