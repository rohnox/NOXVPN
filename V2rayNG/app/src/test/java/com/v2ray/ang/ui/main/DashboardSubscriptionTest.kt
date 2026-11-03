package com.v2ray.ang.ui.main

import com.google.gson.Gson
import com.v2ray.ang.dto.entities.SubscriptionUsageInfo
import com.v2ray.ang.handler.SubscriptionInfoParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DashboardSubscriptionTest {
    @Test fun `title only is not fabricated zero traffic or unlimited quota`() {
        val info = SubscriptionInfoParser.parse(null, "My plan", null, null) { it }!!
        assertFalse(hasSubscriptionTraffic(info))
        assertFalse(hasSubscriptionQuota(info))
    }
    @Test fun `reported zero usage and unlimited quota are real known values`() {
        val info = SubscriptionInfoParser.parse("upload=0;download=0;total=0", null, null, null) { it }!!
        assertTrue(hasSubscriptionTraffic(info))
        assertTrue(hasSubscriptionQuota(info))
        assertEquals(0L, info.used)
    }
    @Test fun `preceding released JSON remains readable without a migration`() {
        val old = Gson().fromJson("""{"upload":100,"download":200,"total":1000,"expire":1893456000}""", SubscriptionUsageInfo::class.java)
        assertTrue(hasSubscriptionTraffic(old))
        assertTrue(hasSubscriptionQuota(old))
        assertEquals(300L, old.used)
        assertNull(old.trafficAvailable)
    }
    @Test fun `partial header does not invent missing counters`() {
        val info = SubscriptionInfoParser.parse("total=1000;expire=1893456000", null, null, null) { it }!!
        assertFalse(hasSubscriptionTraffic(info))
        assertTrue(hasSubscriptionQuota(info))
        assertNull(SubscriptionInfoParser.parse(null, null, null, null) { it })
    }
}
