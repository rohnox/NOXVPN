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
              sourceUrl: String, history: List<SubscriptionUsageSample>?, nowSeconds: Long): com.v2ray.ang.dto.entities.SubscriptionUsageInfo {
        val key = sourceKey(sourceUrl)
        val matches = key != null && key == previous?.providerUsageSource
        return current.copy(providerUsage = history ?: previous?.providerUsage?.takeIf { matches },
            providerUsageUpdatedAt = if (history != null) nowSeconds else previous?.providerUsageUpdatedAt?.takeIf { matches },
            providerUsageSource = key)
    }

    fun endpoint(subscriptionUrl: String, nowSeconds: Long): String? {
        val url = subscriptionUrl.toHttpUrlOrNull() ?: return null
        val parts = url.pathSegments.filter { it.isNotEmpty() }
        if (!url.isHttps || url.username.isNotEmpty() || url.password.isNotEmpty() || parts.size < 2 || parts[parts.lastIndex - 1] != "sub") return null
        return url.newBuilder().encodedPath(url.encodedPath.trimEnd('/') + "/usage")
            .removeAllQueryParameters("start").removeAllQueryParameters("period").fragment(null)
            .addQueryParameter("start", Instant.ofEpochSecond(nowSeconds.coerceAtLeast(0) - 30 * 86400L).toString())
            .addQueryParameter("period", "day").build().toString()
    }

    /** The page selects one aggregate series; never add aggregate and per-node rows together. */
    fun parse(json: String, nowSeconds: Long): List<SubscriptionUsageSample>? = runCatching {
        require(json.length <= 1_048_576)
        val root = JsonParser.parseString(json).asJsonObject
        require(root["period"]?.asString == "day")
        val stats = root["stats"]?.asJsonObject ?: return null
        val selected = stats["-1"] ?: stats.entrySet().firstOrNull { it.value.isJsonArray }?.value ?: return null
        require(selected.isJsonArray && selected.asJsonArray.size() <= 32)
        val minimum = (nowSeconds.coerceAtLeast(0) - 30 * 86400L) / 86400 * 86400
        val rows = selected.asJsonArray.map { element ->
            val row = element.asJsonObject
            val timestamp = OffsetDateTime.parse(row["period_start"].asString).toEpochSecond()
            val bytes = row["total_traffic"].asBigDecimal.longValueExact()
            require(bytes >= 0 && timestamp in minimum..nowSeconds)
            SubscriptionUsageSample(timestamp, bytes)
        }.sortedBy { it.timestampSeconds }
        require(rows.distinctBy { it.timestampSeconds }.size == rows.size)
        rows.fold(0L) { sum, point -> Math.addExact(sum, point.usedBytes) }
        rows
    }.getOrNull()

    fun fetch(request: UrlContentRequest, nowSeconds: Long): List<SubscriptionUsageSample>? {
        val target = endpoint(request.url.orEmpty(), nowSeconds) ?: return null
        return HttpUtil.getBoundedSubscriptionJson(request.copy(url = target, timeout = 8000))?.let { parse(it, nowSeconds) }
    }

    fun total(points: List<SubscriptionUsageSample>): Long? = runCatching {
        points.fold(0L) { sum, point -> require(point.usedBytes >= 0); Math.addExact(sum, point.usedBytes) }
    }.getOrNull()
}
