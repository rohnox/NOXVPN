# 1.5.4 — actual PasarGuard subscription history

The previous graph could record account-counter observations but did not call the provider's historical usage API. The supplied subscription page does expose history. This version fetches the same same-origin token-scoped /sub/<token>/usage endpoint with start=30days ago and period=day, parses stats[-1] (or the page-compatible first available series), and plots each period_start/total_traffic point. The chart total is the sum of actual daily upload+download usage, independently of the account-cycle used/total/remaining quota card beneath it.

- No administrator API key, cookies from a browser or hardcoded customer link. All requests derive from the customer's saved subscription URL, use normal TLS verification, preserve the port, and never follow redirects for history. Network data is JSON-only, bounded to1MiB, with an8second request budget per attempt. Existing optional app proxy is used first, with direct fallback when needed.
- Persist an optional provider history cache and fetch timestamp in the existing subscription JSON. Unsupported/failed history does not break config import or invent a monthly total. Last successful cache is retained with its actual fetch time; a source-URL digest prevents displaying a prior account's history after changing the subscription link. Legacy dailyUsage remains a clearly labeled fallback for other providers.
- Distinguish a valid empty provider result (zero usage) from unavailable history. Reject malformed periods/dates/counts, duplicate timestamps and overflow; do not double-count an aggregate with per-node series.
- Retain explicit quota heading, provider title, remaining days, update confirmation and the OpenVPN/Cisco fixes. Remove configured request-header values from debug logging.
- Version1.5.4 / base versionCode767, same package, signing identity, native engines and permissions.

Private response HTML/JSON and the sample subscription URL were kept outside source/artifacts under work/ and are not published. The optional real-response parser check takes a private fixture path from the environment; no customer URL or data is embedded in test source.
