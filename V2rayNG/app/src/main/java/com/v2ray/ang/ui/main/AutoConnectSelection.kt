package com.v2ray.ang.ui.main

/** The ranked result set lives beyond bulk-test completion, until activation is consumed/cancelled. */
internal class AutoConnectSelection {
    data class Choice(val requestId: String, val guid: String, val delayMillis: Long)
    private var activeId: String? = null
    private var candidates = emptyList<String>()
    private val results = linkedMapOf<String, Long>()
    private var ready: Choice? = null
    val requestId: String? get() = activeId ?: ready?.requestId
    val completedCount: Int get() = results.size
    fun isActive(id: String): Boolean = activeId == id

    fun begin(id: String, guids: List<String>) {
        cancel()
        activeId = id
        candidates = guids.distinct()
    }

    fun record(id: String, guid: String, delayMillis: Long): Boolean {
        if (id != activeId || guid !in candidates || guid in results) return false
        results[guid] = delayMillis
        return true
    }

    fun finish(id: String, stillValid: Set<String>): Choice? {
        if (id != activeId) return null
        activeId = null
        ready = candidates.filter { it in stillValid }.mapNotNull { guid ->
            results[guid]?.takeIf { it > 0 }?.let { Choice(id, guid, it) }
        }.minByOrNull { it.delayMillis }
        return ready
    }

    fun consume(id: String): Choice? = ready?.takeIf { it.requestId == id }?.also { ready = null }

    fun cancel() {
        activeId = null
        ready = null
        candidates = emptyList()
        results.clear()
    }
}

/** A one-shot UI activation after the selected GUID has been validated and persisted. */
data class ServerActivation(val generation: Long, val guid: String, val startIfStopped: Boolean, val autoRequestId: String? = null)
