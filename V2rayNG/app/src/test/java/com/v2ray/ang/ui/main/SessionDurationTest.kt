package com.v2ray.ang.ui.main

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SessionDurationTest {
    @Test
    fun `formats zero negative and long sessions`() {
        assertEquals("00:00:00", formatSessionDuration(0))
        assertEquals("00:00:00", formatSessionDuration(-10))
        assertEquals("01:01:01", formatSessionDuration(3661))
        assertEquals("100:00:00", formatSessionDuration(360000))
    }
}
