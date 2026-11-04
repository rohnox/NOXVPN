package com.v2ray.ang.handler

import com.google.gson.Gson
import com.v2ray.ang.dto.entities.SubscriptionUsageInfo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SubscriptionUsageHistoryTest {
    private fun info(bytes: Long) = SubscriptionUsageInfo(upload = bytes / 2, download = bytes - bytes / 2, trafficAvailable = true)
    @Test fun combinesCountersAndReplacesSameDay() {
        val first = SubscriptionInfoParser.recordUsage(null, info(100), 86400)
        val next = SubscriptionInfoParser.recordUsage(first, info(250), 86401)
        assertEquals(1, next.dailyUsage!!.size)
        assertEquals(250L, next.dailyUsage!!.single().usedBytes)
        val tomorrow = SubscriptionInfoParser.recordUsage(next, info(300), 172800)
        assertEquals(listOf(250L, 300L), tomorrow.dailyUsage!!.map { it.usedBytes })
    }
    @Test fun retainsThirtyDaysWithoutFillingGapsOrHidingReset() {
        var history: SubscriptionUsageInfo? = null
        for (day in 1L..40L) history = SubscriptionInfoParser.recordUsage(history, info(day * 100), day * 86400)
        assertEquals(30, history!!.dailyUsage!!.size)
        val reset = SubscriptionInfoParser.recordUsage(history, info(5), 42L * 86400)
        assertEquals(5L, reset.dailyUsage!!.last().usedBytes)
        assertFalse(reset.dailyUsage!!.any { it.timestampSeconds / 86400 == 41L })
    }
    @Test fun missingCountersNeverRecordFakeZeroAndOldJsonIsReadable() {
        val old = Gson().fromJson("""{"upload":1,"download":2,"total":10}""", SubscriptionUsageInfo::class.java)
        assertNull(old.dailyUsage)
        val first = SubscriptionInfoParser.recordUsage(old, info(40), 86400)
        val missing = SubscriptionInfoParser.recordUsage(first, SubscriptionUsageInfo(trafficAvailable = false), 172800)
        assertEquals(first.dailyUsage, missing.dailyUsage)
        val roundTrip = Gson().fromJson(Gson().toJson(missing), SubscriptionUsageInfo::class.java)
        assertEquals(missing.dailyUsage, roundTrip.dailyUsage)
    }
    @Test fun zeroUsageIsARealReadingAndOverflowIsBounded() {
        assertEquals(0L, SubscriptionInfoParser.recordUsage(null, info(0), 1).dailyUsage!!.single().usedBytes)
        assertEquals(Long.MAX_VALUE, SubscriptionInfoParser.recordUsage(null, SubscriptionUsageInfo(upload = Long.MAX_VALUE, download = 1, trafficAvailable = true), 1).dailyUsage!!.single().usedBytes)
    }
    @Test fun staleOfflineHistoryIsNotShownAsCurrentMonth() {
        val first = SubscriptionInfoParser.recordUsage(null, info(100), 86400)
        assertTrue(SubscriptionInfoParser.recentUsage(first, 32L * 86400).isEmpty())
        assertTrue(SubscriptionInfoParser.recentUsage(first, 0).isEmpty())
    }
}
