package com.v2ray.ang.handler

import com.google.gson.Gson
import com.v2ray.ang.dto.entities.SubscriptionUsageInfo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.time.Instant

class PasarGuardUsageTest {
    private val now = Instant.parse("2026-01-31T12:00:00Z").epochSecond
    @Test fun endpointStaysOnSubscriptionOriginAndRequestsDailyThirtyDayHistory() {
        val target = PasarGuardUsage.endpoint("https://example.invalid:2087/base/sub/test-token/", now)!!
        assertTrue(target.startsWith("https://example.invalid:2087/base/sub/test-token/usage?"))
        assertTrue(target.contains("period=day"))
        assertTrue(target.contains("2026-01-01T12%3A00%3A00Z"))
        assertNull(PasarGuardUsage.endpoint("http://example.invalid/sub/token", now))
        assertNull(PasarGuardUsage.endpoint("https://example.invalid/settings", now))
        assertNull(PasarGuardUsage.endpoint("https://user:password@example.invalid/sub/token", now))
    }
    @Test fun aggregateSeriesIsNotDoubleCountedWithPerNodeSeries() {
        val rows = PasarGuardUsage.parse("""{"period":"day","stats":{"-1":[{"period_start":"2026-01-31T00:00:00Z","total_traffic":200},{"period_start":"2026-01-30T00:00:00Z","total_traffic":100}],"1":[{"period_start":"2026-01-31T00:00:00Z","total_traffic":200}]}}""", now)!!
        assertEquals(listOf(100L, 200L), rows.map { it.usedBytes })
        assertEquals(300L, PasarGuardUsage.total(rows))
    }
    @Test fun emptyAggregateIsKnownZeroAndMissingStatsIsUnknown() {
        assertEquals(emptyList<Any>(), PasarGuardUsage.parse("""{"period":"day","stats":{"-1":[]}}""", now))
        assertEquals(0L, PasarGuardUsage.total(emptyList()))
        assertNull(PasarGuardUsage.parse("""{"period":"day"}""", now))
        assertNull(PasarGuardUsage.parse("<html>Not supported</html>", now))
        assertNull(PasarGuardUsage.parse("""{"period":"hour","stats":{"-1":[]}}""", now))
    }
    @Test fun rejectsBadCountsDatesDuplicatesAndOverflow() {
        fun row(date: String = "2026-01-31T00:00:00Z", amount: String = "1") = """{"period_start":"$date","total_traffic":$amount}"""
        fun parse(rows: String) = PasarGuardUsage.parse("""{"period":"day","stats":{"-1":[$rows]}}""", now)
        for (value in listOf("-1", "1.5", "9223372036854775808")) assertNull(parse(row(amount = value)))
        assertNull(parse(row("invalid")))
        assertNull(parse(row("2025-12-01T00:00:00Z")))
        assertNull(parse(row("2026-02-02T00:00:00Z")))
        assertNull(parse(row() + "," + row()))
        assertNull(parse(row(amount = Long.MAX_VALUE.toString()) + "," + row("2026-01-30T00:00:00Z")))
    }
    @Test fun precedingJsonAndNewProviderCacheRoundTripWithoutRenamingFields() {
        val gson = Gson()
        val previous = gson.fromJson("""{"upload":1,"download":2,"dailyUsage":[{"timestampSeconds":100,"usedBytes":3}]}""", SubscriptionUsageInfo::class.java)
        assertNull(previous.providerUsage)
        assertEquals(3L, previous.dailyUsage!!.single().usedBytes)
        val updated = previous.copy(providerUsage = emptyList(), providerUsageUpdatedAt = now)
        assertEquals(updated, gson.fromJson(gson.toJson(updated), SubscriptionUsageInfo::class.java))
    }
    @Test fun failedFetchRetainsOnlyMatchingSubscriptionCacheAndEmptySuccessReplacesIt() {
        val source = "https://example.invalid/sub/one"
        val points = listOf(com.v2ray.ang.dto.entities.SubscriptionUsageSample(now, 30))
        val previous = PasarGuardUsage.merge(null, SubscriptionUsageInfo(), source, points, now)
        val cached = PasarGuardUsage.merge(previous, SubscriptionUsageInfo(title = "Current"), source, null, now + 10)
        assertEquals(points, cached.providerUsage)
        assertEquals(now, cached.providerUsageUpdatedAt)
        assertEquals("Current", cached.title)
        val changed = PasarGuardUsage.merge(previous, SubscriptionUsageInfo(), "https://example.invalid/sub/two", null, now + 10)
        assertNull(changed.providerUsage)
        assertNull(changed.providerUsageUpdatedAt)
        val empty = PasarGuardUsage.merge(previous, SubscriptionUsageInfo(), source, emptyList(), now + 10)
        assertTrue(empty.providerUsage!!.isEmpty())
        assertEquals(now + 10, empty.providerUsageUpdatedAt)
        assertEquals(PasarGuardUsage.sourceKey(source), PasarGuardUsage.sourceKey(source + "#label"))
    }
    @Test
    @EnabledIfEnvironmentVariable(named = "ISPEED_PRIVATE_USAGE_FIXTURE", matches = ".+")
    fun suppliedProviderResponseMatchesProductionParser() {
        val json = java.io.File(System.getenv("ISPEED_PRIVATE_USAGE_FIXTURE")).readText()
        val rows = PasarGuardUsage.parse(json, System.currentTimeMillis() / 1000)
        assertNotNull(rows)
        assertEquals(System.getenv("ISPEED_PRIVATE_USAGE_EXPECTED").toLong(), PasarGuardUsage.total(rows!!))
    }
}
