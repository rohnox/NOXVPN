package com.v2ray.ang.ui.main

import com.v2ray.ang.dto.SessionTraffic
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DashboardModelsTest {
    private fun sample(seconds: Long, up: Long = seconds * 100, down: Long = seconds * 300) = SessionTraffic(seconds, up, down, true)
    @Test fun `no synthetic samples while disconnected or unavailable`() {
        assertEquals(MainUiState(), MainUiState().withTraffic(sample(3)))
        assertTrue(appendTrafficHistory(listOf(sample(3)), SessionTraffic()).isEmpty())
        assertTrue(trafficRates(emptyList()).isEmpty())
        assertTrue(trafficRates(listOf(sample(3))).isEmpty())
    }
    @Test fun `real elapsed intervals determine measured rates`() {
        val rates = trafficRates(listOf(sample(3), sample(6), sample(12)))
        assertEquals(2, rates.size)
        assertEquals(100.0, rates.last().up)
        assertEquals(300.0, rates.last().down)
        val state = MainUiState(isRunning = true).withTraffic(sample(3)).withTraffic(sample(6))
        assertEquals(2, state.trafficHistory.size)
        assertEquals(sample(6), state.sessionTraffic)
    }
    @Test fun `restart counter reset and duplicate timestamps never invent a rate`() {
        assertEquals(listOf(sample(1)), appendTrafficHistory(listOf(sample(30)), sample(1)))
        assertEquals(1, appendTrafficHistory(listOf(sample(3)), sample(3)).size)
        assertEquals(1, appendTrafficHistory(listOf(sample(3)), sample(6, 0, 0)).size)
        assertTrue(trafficRates(listOf(sample(6), sample(3))).isEmpty())
    }
    @Test fun `history stays bounded and rejects invalid elapsed times`() {
        val history = (1L..100L).fold(emptyList<SessionTraffic>()) { acc, seconds -> appendTrafficHistory(acc, sample(seconds)) }
        assertEquals(60, history.size)
        assertEquals(41L, history.first().elapsedSeconds)
        assertTrue(appendTrafficHistory(history, sample(-1)).isEmpty())
    }

    @Test fun `visible usage refresh wakes at local midnight and remains bounded`() {
        val zone = java.time.ZoneId.of("Asia/Tehran")
        val start = java.time.Instant.parse("2026-11-04T20:29:50Z").toEpochMilli()
        assertEquals(10_000L, usageRefreshDelay(start, start, zone))
        assertEquals(1_000L, usageRefreshDelay(start, start + 15_000, zone))
        assertEquals(60_000L, usageRefreshDelay(start + 60_000, start + 60_000, zone))
    }
}
