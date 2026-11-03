package com.v2ray.ang.dto

import java.io.Serializable

/** Daemon-produced aggregate counters; contains no endpoints, credentials or profile contents. */
data class SessionTraffic(
    val elapsedSeconds: Long = 0,
    val uploaded: Long = 0,
    val downloaded: Long = 0,
    val available: Boolean = false,
) : Serializable {
    fun add(up: Long, down: Long, elapsed: Long, enabled: Boolean) = copy(
        elapsedSeconds = elapsed.coerceAtLeast(0),
        uploaded = uploaded + up.coerceAtLeast(0),
        downloaded = downloaded + down.coerceAtLeast(0),
        available = enabled,
    )
}
