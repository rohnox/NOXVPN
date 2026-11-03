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
}
