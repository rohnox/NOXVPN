# 1.5.3 — subscription traffic and update confirmation

The traffic plot now uses the subscription provider account counters, independent of the local VPN traffic-statistics setting. It combines upload and download into one consumption series and displays the last30 days of actual recorded observations. One latest reading per UTC day is saved inside the existing subscription usage JSON through MmkvManager after a successful subscription fetch. Missing days are not filled; account resets are shown as provider-counter drops. Old JSON has no dailyUsage field and remains readable. Missing current headers do not fabricate zero readings or erase genuine history.

The standard subscription response provides current account totals, not a full historical billing API. Recording starts with this version's first successful refresh. The graph's legend is explicitly subscription-cycle usage, not a claim that lifetime counters equal one month's traffic. The period can initially be partial. History outside30days is hidden even offline.

The separate quota panel directly beneath the graph has an explicit Subscription quota / سهمیه مصرف heading, followed by provider title, used/total/remaining values, progress and remaining days. Unknown limits remain unknown.

Successful manual or automatic subscription updates now show a green Subscription updated successfully / اشتراک با موفقیت به‌روزرسانی شد banner for six seconds. Failed/partially failed updates show their actual result and never a success message. Empty or disabled-only account updates show no false confirmation. An older timer cannot clear a newer notice. Existing one-minute entry coalescing and IO fetching remain.

Package and release key remain unchanged. Version1.5.3 / base versionCode766. No native-engine change, new permission, tracker or advertising.

## Protocol status fixes

OpenVPN host state now subscribes directly to the engine's IServiceStatus/IStatusCallbacks IPC and refreshes the daemon snapshot on tab resume, independent of the UI-process VpnStatus cache and log-history replay. Byte counters use the same direct callback. Log-pipe errors do not overwrite a real connected state. The client owns its binding/reader/cleanup and ignores callbacks after close.

LauncherManager delivers the existing Xray stop command as an ordered message before launching the OpenVPN consent/connection activity or the Cisco manager. The dashboard remains driven by the daemon's STOP_SUCCESS event; launch success or the generic Android VPN icon is not treated as Xray connection state. Opening the Cisco manager initiates this stop even if the user subsequently cancels Cisco selection. VPN/proxy/root start branches and native binaries are retained.

Service IPC, actual successful handover, repeated starts/stops, switching while connecting, cold process recovery and cancellation on real Android hardware require device testing. Mock IPC/JVM checks are not proof of a live VPN connection.
