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

/** Cumulative bytes of this connection, never a speed or provider history. */
internal fun trafficVolumes(history: List<SessionTraffic>): List<TrafficRatePoint> = history
    .filter { it.available && it.elapsedSeconds >= 0 && it.uploaded >= 0 && it.downloaded >= 0 }
    .map { TrafficRatePoint(it.elapsedSeconds, it.uploaded.toDouble(), it.downloaded.toDouble()) }

internal fun subscriptionTitle(info: SubscriptionUsageInfo?, fallback: String): String =
    info?.title?.trim()?.takeIf { it.isNotEmpty() } ?: fallback

/** Match the provider page's binary units and two-decimal totals. */
internal fun formatHistoryTraffic(bytes: Long, locale: java.util.Locale): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB", "EB")
    var value = bytes.coerceAtLeast(0).toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) { value /= 1024; unit++ }
    val formatter = java.text.NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = if (unit == 0) 0 else 2
        maximumFractionDigits = if (unit == 0) 0 else 2
        isGroupingUsed = false
    }
    return formatter.format(value) + " " + units[unit]
}

/** Null means missing expiry; zero means expired. Round a partial day up. */
internal fun subscriptionDaysRemaining(expireSeconds: Long?, nowSeconds: Long): Int? {
    if (expireSeconds == null || expireSeconds <= 0) return null
    if (expireSeconds <= nowSeconds) return 0
    val seconds = expireSeconds - nowSeconds.coerceAtLeast(0)
    return (seconds / 86400 + if (seconds % 86400 > 0) 1 else 0).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}

/** Suppress repeat entry events caused by permission dialogs, pickers or rotation. */
internal class SubscriptionEntryRefresh {
    private var lastEntryMillis: Long? = null
    fun shouldRefresh(nowMillis: Long): Boolean {
        val last = lastEntryMillis
        if (last != null && nowMillis >= last && nowMillis - last < 60_000) return false
        lastEntryMillis = nowMillis
        return true
    }
}

internal fun resolveDashboardSubscription(groups: List<com.v2ray.ang.dto.GroupMapItem>, preferred: String?, connected: String): String {
    val plans = groups.filter { it.id.isNotEmpty() && it.id != com.v2ray.ang.AppConfig.DEFAULT_SUBSCRIPTION_ID }
    return plans.firstOrNull { it.id == preferred }?.id ?: plans.firstOrNull { it.id == connected }?.id ?: plans.firstOrNull()?.id.orEmpty()
}
internal fun dashboardPlan(state: MainUiState): com.v2ray.ang.dto.GroupMapItem? =
    state.groups.firstOrNull { it.id == resolveDashboardSubscription(state.groups, state.dashboardSubscriptionId, state.selectedSubscriptionId) && it.id.isNotEmpty() }

internal fun nearestProviderDay(points: List<com.v2ray.ang.dto.entities.SubscriptionUsageSample>, fraction: Float): Long? {
    if (points.isEmpty() || !fraction.isFinite()) return null
    val start = points.minOf { it.timestampSeconds }.toDouble()
    val end = points.maxOf { it.timestampSeconds }.toDouble()
    val target = start + (end - start) * fraction.coerceIn(0f, 1f)
    return points.minByOrNull { kotlin.math.abs(it.timestampSeconds - target) }?.timestampSeconds
}
