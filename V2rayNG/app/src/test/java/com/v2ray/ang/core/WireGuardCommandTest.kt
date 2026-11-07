package com.v2ray.ang.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WireGuardCommandTest {
    @Test fun repeatedStartKeepsExactlyTheSameActiveOrPendingProfile() {
        for (phase in listOf(WireGuardState.Phase.Up, WireGuardState.Phase.Connecting)) {
            assertEquals(WireGuardCommand.Keep, wireGuardCommand("one", false, WireGuardState("one", phase)))
            assertEquals(WireGuardCommand.Start, wireGuardCommand("two", false, WireGuardState("one", phase)))
            assertEquals(WireGuardCommand.Stop, wireGuardCommand("one", true, WireGuardState("one", phase)))
        }
    }
    @Test fun nullRestartStopsAndFailureCanBeRetried() {
        assertEquals(WireGuardCommand.Stop, wireGuardCommand(null, false, WireGuardState("one", WireGuardState.Phase.Up)))
        for (phase in listOf(WireGuardState.Phase.Down, WireGuardState.Phase.Failed)) assertEquals(WireGuardCommand.Start, wireGuardCommand("one", false, WireGuardState("one", phase)))
    }
    @Test fun teardownRejectsEveryNewStartWhileRepeatedStopsCanStillBeAcknowledged() {
        for (phase in WireGuardState.Phase.entries) {
            val state = WireGuardState("old", phase)
            assertEquals(WireGuardCommand.Ignore, wireGuardCommand("old", false, state, stopping = true))
            assertEquals(WireGuardCommand.Ignore, wireGuardCommand("new", false, state, stopping = true))
            assertEquals(WireGuardCommand.Stop, wireGuardCommand("old", true, state, stopping = true))
        }
    }
}
