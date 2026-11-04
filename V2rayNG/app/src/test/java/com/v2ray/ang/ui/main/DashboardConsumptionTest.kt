package com.v2ray.ang.ui.main

import com.v2ray.ang.dto.SessionTraffic
import com.v2ray.ang.dto.entities.SubscriptionUsageInfo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DashboardConsumptionTest {
    @Test fun volumeIsCumulativeBytes() {
        val points = trafficVolumes(listOf(SessionTraffic(1, 1024, 4096, true), SessionTraffic(11, 2048, 8192, true)))
        assertEquals(1024.0, points.first().up)
        assertEquals(8192.0, points.last().down)
        assertEquals(11L, points.last().seconds)
        assertTrue(trafficVolumes(listOf(SessionTraffic(), SessionTraffic(-1, 0, 0, true), SessionTraffic(1, -1, 1, true))).isEmpty())
        assertTrue(trafficVolumes(emptyList()).isEmpty())
    }
    @Test fun panelNameOverridesImportPlaceholder() {
        assertEquals("iSpecialVPN", subscriptionTitle(SubscriptionUsageInfo(title = " iSpecialVPN "), "import sub"))
        assertEquals("My plan", subscriptionTitle(SubscriptionUsageInfo(title = " "), "My plan"))
        assertEquals("My plan", subscriptionTitle(null, "My plan"))
    }
    @Test fun remainingDaysRoundUp() {
        assertNull(subscriptionDaysRemaining(null, 100))
        assertNull(subscriptionDaysRemaining(0, 100))
        assertNull(subscriptionDaysRemaining(-1, 100))
        assertEquals(0, subscriptionDaysRemaining(100, 100))
        assertEquals(0, subscriptionDaysRemaining(99, 100))
        assertEquals(1, subscriptionDaysRemaining(101, 100))
        assertEquals(1, subscriptionDaysRemaining(86500, 100))
        assertEquals(2, subscriptionDaysRemaining(86501, 100))
        assertEquals(Int.MAX_VALUE, subscriptionDaysRemaining(Long.MAX_VALUE, 0))
    }
    @Test fun entryRefreshCoalescesRapidEvents() {
        val gate = SubscriptionEntryRefresh()
        assertTrue(gate.shouldRefresh(100))
        assertFalse(gate.shouldRefresh(100))
        assertFalse(gate.shouldRefresh(60099))
        assertTrue(gate.shouldRefresh(60100))
        assertTrue(gate.shouldRefresh(0))
    }
    @Test fun providerTotalUsesTwoDecimalBinaryUnits() {
        assertEquals("2.15 GB", formatHistoryTraffic((2.15 * 1024 * 1024 * 1024).toLong(), java.util.Locale.ENGLISH))
        assertEquals("0 B", formatHistoryTraffic(0, java.util.Locale.ENGLISH))
        assertEquals("600.00 MB", formatHistoryTraffic(600L * 1024 * 1024, java.util.Locale.ENGLISH))
    }
}
