package com.v2ray.ang.handler

import com.google.gson.JsonParser
import com.v2ray.ang.dto.UrlContentRequest
import com.v2ray.ang.dto.entities.SubscriptionUsageSample
import com.v2ray.ang.util.HttpUtil
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.time.Instant
import java.time.OffsetDateTime

/** PasarGuard's public, token-scoped subscription history API; no administrator credential. */
object PasarGuardUsage {
    fun sourceKey(subscriptionUrl: String): String? {
        val normalized = subscriptionUrl.toHttpUrlOrNull()?.newBuilder()?.fragment(null)?.build()?.toString() ?: return null
        return java.security.MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
    }

    fun merge(previous: com.v2ray.ang.dto.entities.SubscriptionUsageInfo?, current: com.v2ray.ang.dto.entities.SubscriptionUsageInfo,
              sourceUrl: String, history: List<SubscriptionUsageSample>?, nowSeconds: Long, historyZone: String = "UTC"): com.v2ray.ang.dto.entities.SubscriptionUsageInfo {
        val key = sourceKey(sourceUrl)
        val matches = key != null && key == previous?.providerUsageSource
        return current.copy(providerUsage = history ?: previous?.providerUsage?.takeIf { matches },
            providerUsageUpdatedAt = if (history != null) nowSeconds else previous?.providerUsageUpdatedAt?.takeIf { matches },
            providerUsageSource = key,
            providerUsageZone = if (history != null) historyZone else previous?.providerUsageZone?.takeIf { matches })
    }

    fun endpoint(subscriptionUrl: String, nowSeconds: Long, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): String? {
        val url = subscriptionUrl.toHttpUrlOrNull() ?: return null
        val parts = url.pathSegments.filter { it.isNotEmpty() }
        if (!url.isHttps || url.username.isNotEmpty() || url.password.isNotEmpty() || parts.size < 2 || parts[parts.lastIndex - 1] != "sub") return null
        return url.newBuilder().encodedPath(url.encodedPath.trimEnd('/') + "/usage")
            .removeAllQueryParameters("start").removeAllQueryParameters("end").removeAllQueryParameters("period").fragment(null)
            .addQueryParameter("start", periodStart(nowSeconds, zone).toString())
            .addQueryParameter("end", Instant.ofEpochSecond(nowSeconds.coerceAtLeast(0)).atOffset(offset(nowSeconds, zone)).toString())
            .addQueryParameter("period", "day").build().toString()
    }

    fun offset(nowSeconds: Long, zone: java.time.ZoneId): java.time.ZoneOffset = zone.rules.getOffset(Instant.ofEpochSecond(nowSeconds.coerceAtLeast(0)))

    fun periodStart(nowSeconds: Long, zone: java.time.ZoneId): OffsetDateTime {
        val offset = offset(nowSeconds, zone)
        return Instant.ofEpochSecond(nowSeconds.coerceAtLeast(0)).atOffset(offset).toLocalDate().minusDays(29).atStartOfDay().atOffset(offset)
    }

    /** The page selects one aggregate series; never add aggregate and per-node rows together. */
    fun parse(json: String, nowSeconds: Long, expectedOffset: java.time.ZoneOffset? = null): List<SubscriptionUsageSample>? = runCatching {
        require(json.length <= 1_048_576)
        val root = JsonParser.parseString(json).asJsonObject
        require(root["period"]?.asString == "day")
        val stats = root["stats"]?.asJsonObject ?: return null
        val selected = stats["-1"] ?: stats.entrySet().firstOrNull { it.value.isJsonArray }?.value ?: return null
        require(selected.isJsonArray && selected.asJsonArray.size() <= 32)
        val minimum = if (expectedOffset != null) periodStart(nowSeconds, expectedOffset).toEpochSecond() else (nowSeconds.coerceAtLeast(0) - 30 * 86400L) / 86400 * 86400
        val rows = selected.asJsonArray.map { element ->
            val row = element.asJsonObject
            val timestamp = OffsetDateTime.parse(row["period_start"].asString).toEpochSecond()
            if (expectedOffset != null) require(Instant.ofEpochSecond(timestamp).atOffset(expectedOffset).toLocalTime() == java.time.LocalTime.MIDNIGHT)
            val bytes = row["total_traffic"].asBigDecimal.longValueExact()
            require(bytes >= 0 && timestamp in minimum..nowSeconds)
            SubscriptionUsageSample(timestamp, bytes)
        }.sortedBy { it.timestampSeconds }
        require(rows.distinctBy { it.timestampSeconds }.size == rows.size)
        rows.fold(0L) { sum, point -> Math.addExact(sum, point.usedBytes) }
        rows
    }.getOrNull()

    fun fetch(request: UrlContentRequest, nowSeconds: Long, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): List<SubscriptionUsageSample>? {
        val target = endpoint(request.url.orEmpty(), nowSeconds, zone) ?: return null
        return HttpUtil.getBoundedSubscriptionJson(request.copy(url = target, timeout = 8000))?.let { parse(it, nowSeconds, offset(nowSeconds, zone)) }
    }

    fun total(points: List<SubscriptionUsageSample>): Long? = runCatching {
        points.fold(0L) { sum, point -> require(point.usedBytes >= 0); Math.addExact(sum, point.usedBytes) }
    }.getOrNull()
}
