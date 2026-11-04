package com.v2ray.ang.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SessionTrafficTest {
    @Test
    fun `initial counters are explicitly unavailable`() {
        val state = SessionTraffic()
        assertEquals(0L, state.elapsedSeconds)
        assertEquals(0L, state.uploaded)
        assertEquals(0L, state.downloaded)
        assertFalse(state.available)
    }

    @Test
    fun `accumulates deltas and ignores negative native counters`() {
        val state = SessionTraffic().add(40, 90, 3, true).add(-1, 30, 6, true)
        assertEquals(40L, state.uploaded)
        assertEquals(120L, state.downloaded)
        assertEquals(6L, state.elapsedSeconds)
        assertTrue(state.available)
    }

    @Test
    fun `disabled stats stay unavailable and time is nonnegative`() {
        val state = SessionTraffic().add(0, 0, -1, false)
        assertEquals(0L, state.elapsedSeconds)
        assertFalse(state.available)
    }
    @Test fun monotonicPresentationAdvancesEachSecondWithoutInventingBytes() {
        val sample = SessionTraffic(3, 40, 90, true, 10000)
        assertEquals(3L, sample.elapsedAt(10000))
        assertEquals(4L, sample.elapsedAt(11000))
        assertEquals(5L, sample.elapsedAt(12000))
        assertEquals(6L, sample.elapsedAt(13000))
        assertEquals(40L, sample.uploaded)
        assertEquals(90L, sample.downloaded)
        assertEquals(3, sample.elapsedAt(9000))
    }
    @Test fun legacySnapshotAndMissingCountersStillHaveAnHonestClock() {
        assertEquals(7L, SessionTraffic(7).elapsedAt(99999))
        val unavailable = SessionTraffic(7, available = false, sampledAtElapsedMillis = 1000)
        assertEquals(8L, unavailable.elapsedAt(2000))
        assertFalse(unavailable.available)
        assertEquals(Long.MAX_VALUE, SessionTraffic(Long.MAX_VALUE, sampledAtElapsedMillis = 1).elapsedAt(5000))
    }

}
