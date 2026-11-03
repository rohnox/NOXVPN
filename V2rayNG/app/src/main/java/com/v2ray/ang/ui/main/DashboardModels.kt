package com.v2ray.ang.ui.main

import com.v2ray.ang.dto.SessionTraffic
import com.v2ray.ang.dto.entities.SubscriptionUsageInfo

/** UI-owned, bounded session history. Nothing is persisted or read from the native core here. */
internal data class TrafficRatePoint(val seconds: Long, val up: Double, val down: Double)

internal fun appendTrafficHistory(history: List<SessionTraffic>, sample: SessionTraffic): List<SessionTraffic> {
    if (!sample.available || sample.elapsedSeconds < 0) return emptyList()
    val last = history.lastOrNull() ?: return listOf(sample)
    if (sample.elapsedSeconds < last.elapsedSeconds || sample.uploaded < last.uploaded || sample.downloaded < last.downloaded) {
        return listOf(sample)
    }
    if (sample.elapsedSeconds == last.elapsedSeconds) return history.dropLast(1) + sample
    return (history + sample).takeLast(60)
}

internal fun trafficRates(history: List<SessionTraffic>): List<TrafficRatePoint> = history.zipWithNext().mapNotNull { (a, b) ->
    val interval = b.elapsedSeconds - a.elapsedSeconds
    if (!a.available || !b.available || interval <= 0 || b.uploaded < a.uploaded || b.downloaded < a.downloaded) null
    else TrafficRatePoint(b.elapsedSeconds, (b.uploaded - a.uploaded) / interval.toDouble(), (b.downloaded - a.downloaded) / interval.toDouble())
}

internal fun MainUiState.withTraffic(sample: SessionTraffic): MainUiState = if (!isRunning) this else copy(
    sessionTraffic = sample,
    trafficHistory = appendTrafficHistory(trafficHistory, sample)
)

internal fun hasSubscriptionTraffic(info: SubscriptionUsageInfo): Boolean =
    info.trafficAvailable ?: (info.upload > 0 || info.download > 0)

internal fun hasSubscriptionQuota(info: SubscriptionUsageInfo): Boolean =
    info.quotaAvailable ?: (info.total > 0)
