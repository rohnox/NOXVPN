package com.v2ray.ang.core

import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VpnHandoverQueueTest {
    @Test fun everyDirectedPairStopsPreviousOwnerBeforeReadyAndNeverStopsTarget() = runBlocking {
        for (previous in VpnEngine.entries) for (target in VpnEngine.entries.filter { it != previous }) {
            var active: VpnEngine? = previous
            val stopped = mutableListOf<VpnEngine>()
            val queue = VpnHandoverQueue()
            assertTrue(queue.prepare(queue.reserve(), target) {
                stopped += it
                if (active == it) active = null
                true
            })
            assertNull(active, "$previous -> $target")
            assertEquals(3, stopped.distinct().size)
            assertFalse(target in stopped)
        }
    }
    @Test fun failedOwnerStopsHandoverAndNeverReturnsReady() = runBlocking {
        for (target in VpnEngine.entries) {
            val queue = VpnHandoverQueue()
            val calls = mutableListOf<VpnEngine>()
            assertFalse(queue.prepare(queue.reserve(), target) { calls += it; false })
            assertEquals(listOf(enginesToStop(target).first()), calls)
        }
    }
    @Test fun newerRequestWaitsForExistingTeardownAndSupersedesItsStart() = runBlocking {
        val queue = VpnHandoverQueue()
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val first = async {
            queue.prepare(queue.reserve(), VpnEngine.Cisco) { entered.complete(Unit); release.await(); true }
        }
        entered.await()
        val secondToken = queue.reserve()
        var secondStops = 0
        val second = async { queue.prepare(secondToken, VpnEngine.Xray) { secondStops++; true } }
        yield()
        assertEquals(0, secondStops)
        release.complete(Unit)
        assertFalse(first.await()); assertTrue(second.await()); assertEquals(3, secondStops)
    }
    @Test fun staleQueuedRequestDoesNoWorkAndCancelledOwnerNeverReturnsReady() = runBlocking {
        val queue = VpnHandoverQueue()
        val stale = queue.reserve(); queue.reserve()
        assertFalse(queue.prepare(stale, VpnEngine.OpenVpn) { error("stale") })
        val entered = CompletableDeferred<Unit>()
        var ready = false
        val job = launch { ready = queue.prepare(queue.reserve(), VpnEngine.WireGuard) { entered.complete(Unit); awaitCancellation() } }
        entered.await(); job.cancelAndJoin(); assertFalse(ready)
    }
    @Test fun nextHandoverCannotPassPreviousCallerUntilItsStartWasDispatched() = runBlocking {
        val queue = VpnHandoverQueue()
        val ready = CompletableDeferred<Unit>(); val dispatched = CompletableDeferred<Unit>()
        val first = async { queue.prepare(queue.reserve(), VpnEngine.Cisco,
            dispatch = { ready.complete(Unit); dispatched.await() }, stop = { true }) }
        ready.await()
        val token = queue.reserve(); var stops = 0
        val second = async { queue.prepare(token, VpnEngine.Xray) { stops++; true } }
        yield(); assertEquals(0, stops)
        dispatched.complete(Unit)
        assertTrue(first.await()); assertTrue(second.await()); assertEquals(3, stops)
    }
    @Test fun stopRacingPendingStartCancelsItsReadinessThenStopsItsTarget() = runBlocking {
        val queue = VpnHandoverQueue()
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        var dispatches = 0
        val start = async { queue.prepare(queue.reserve(), VpnEngine.Xray, dispatch = { dispatches++ }) { entered.complete(Unit); release.await(); true } }
        entered.await()
        val token = queue.reserve(); var stopped: VpnEngine? = null
        val stop = async { queue.stop(token, VpnEngine.Xray) { stopped = it; true } }
        release.complete(Unit)
        assertFalse(start.await()); assertTrue(stop.await()); assertEquals(0, dispatches); assertEquals(VpnEngine.Xray, stopped)
    }
}
