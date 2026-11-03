package com.v2ray.ang.ui.main

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DashboardNavigationTest {
    @Test fun `subscriptions precede independent protocols tab`() {
        assertEquals(listOf(DashboardTab.Overview, DashboardTab.Servers, DashboardTab.Plans, DashboardTab.Protocols, DashboardTab.Controls), DashboardTab.entries.toList())
    }
    @Test fun `saved tabs restore by stable name with safe fallback`() {
        DashboardTab.entries.forEach { assertEquals(it, dashboardTab(it.name)) }
        assertEquals(DashboardTab.Overview, dashboardTab(""))
        assertEquals(DashboardTab.Overview, dashboardTab("removed-tab"))
    }
}
