package com.v2ray.ang.ui.protocols

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Customer subpage account metadata, separate from Xray's subscription-userinfo headers. */
internal data class ProtocolSubscriptionInfo(
    val username: String,
    val usedBytes: Long?,
    val limitBytes: Long?,
    val unlimitedTraffic: Boolean,
    val expireSeconds: Long?,
    val unlimitedTime: Boolean,
    val pendingDays: Int?
) {
    val remainingBytes: Long? get() = if (limitBytes != null && usedBytes != null) (limitBytes - usedBytes).coerceAtLeast(0) else null
    override fun toString() = "ProtocolSubscriptionInfo(account details omitted)"
}

internal fun parseProtocolSubscriptionInfo(json: String, expectedUsername: String): ProtocolSubscriptionInfo {
    require(json.length <= 2 * 1024 * 1024)
    val user = JsonParser.parseString(json).asJsonObject.getAsJsonObject("user") ?: error("Missing account metadata")
    val username = user.get("username")?.asString ?: error("Missing account identity")
    require(username == expectedUsername)
    fun JsonObject.number(key: String): BigDecimal? = runCatching { get(key)?.takeUnless { it.isJsonNull }?.asBigDecimal }.getOrNull()?.takeIf { it.signum() >= 0 }
    fun bytes(key: String): Long? = runCatching { user.number(key)?.longValueExact() }.getOrNull()
    val up = bytes("upload_bytes"); val down = bytes("download_bytes")
    val used = if (up != null && down != null) runCatching { Math.addExact(up, down) }.getOrNull() else null
    val limit = user.number("data_limit")
    val unit = user.get("data_limit_unit")?.takeUnless { it.isJsonNull }?.asString
    val multiplier = when (unit) { "MB" -> 1048576L; "GB" -> 1073741824L; else -> null }
    val limitBytes = if (limit != null && multiplier != null) runCatching { limit.multiply(BigDecimal.valueOf(multiplier)).longValueExact() }.getOrNull() else null
    val expiryElement = user.get("expiry_date_iso")
    val expiry = expiryElement?.takeUnless { it.isJsonNull }?.asString
    val expires = expiry?.let { value -> runCatching {
        // The provider's date-only field is parsed as UTC midnight by its web subpage too.
        if (value.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toEpochSecond()
        else Instant.parse(value).epochSecond
    }.getOrNull() }
    val waiting = user.get("activation_type")?.asString == "flexible_days" && (user.get("first_connection_at") == null || user.get("first_connection_at").isJsonNull)
    val pending = if (waiting)
        runCatching { user.number("pending_activation_days")?.intValueExact() }.getOrNull() else null
    return ProtocolSubscriptionInfo(username, used, limitBytes, limit?.signum() == 0 || user.get("data_limit")?.isJsonNull == true, expires,
        expiryElement?.isJsonNull == true && !waiting, pending)
}
