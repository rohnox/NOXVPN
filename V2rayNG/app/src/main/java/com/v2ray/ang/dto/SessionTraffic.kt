package com.v2ray.ang.dto

import java.io.Serializable

/** Daemon-produced aggregate counters; contains no endpoints, credentials or profile contents. */
data class SessionTraffic(
    val elapsedSeconds: Long = 0,
    val uploaded: Long = 0,
    val downloaded: Long = 0,
    val available: Boolean = false,
    val sampledAtElapsedMillis: Long = 0,
    val httpProxyPort: Int = 0,
) : Serializable {
    fun add(up: Long, down: Long, elapsed: Long, enabled: Boolean, sampledAtMillis: Long = 0) = copy(
        elapsedSeconds = elapsed.coerceAtLeast(0),
        uploaded = uploaded + up.coerceAtLeast(0),
        downloaded = downloaded + down.coerceAtLeast(0),
        available = enabled,
        sampledAtElapsedMillis = sampledAtMillis.coerceAtLeast(0),
    )
    /** A display clock anchored to a real daemon snapshot; bytes are never extrapolated. */
    fun elapsedAt(nowElapsedMillis: Long): Long {
        val base = elapsedSeconds.coerceAtLeast(0)
        val delta = if (sampledAtElapsedMillis > 0 && nowElapsedMillis >= sampledAtElapsedMillis)
            (nowElapsedMillis - sampledAtElapsedMillis) / 1000 else 0
        return if (base > Long.MAX_VALUE - delta) Long.MAX_VALUE else base + delta
    }
}

