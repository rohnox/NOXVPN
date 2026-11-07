package com.v2ray.ang.core

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

/** Only VPN owners participate; Xray proxy-only and root dispatch keep their existing path. */
internal enum class VpnEngine { Xray, OpenVpn, Cisco, WireGuard }

internal fun enginesToStop(target: VpnEngine): List<VpnEngine> = VpnEngine.entries.filter { it != target }

/** Policy belongs to the bound handover owner, shared across all caller processes. */
internal class VpnHandoverQueue {
    private val mutex = Mutex()
    private val generation = AtomicLong()
    fun reserve(): Long = generation.incrementAndGet()
    suspend fun stop(token: Long, target: VpnEngine, stop: suspend (VpnEngine) -> Boolean): Boolean = mutex.withLock {
        token == generation.get() && stop(target)
    }
    suspend fun prepare(token: Long, target: VpnEngine, dispatch: suspend () -> Unit = {},
                        stop: suspend (VpnEngine) -> Boolean): Boolean = mutex.withLock {
        if (token != generation.get()) return@withLock false
        for (engine in enginesToStop(target)) {
            if (token != generation.get() || !stop(engine)) return@withLock false
        }
        if (token != generation.get()) return@withLock false
        // Hold the queue until the caller has dispatched its start. A slow caller process must
        // not start an obsolete VPN after the next handover has already finished stopping owners.
        dispatch()
        true
    }
}
