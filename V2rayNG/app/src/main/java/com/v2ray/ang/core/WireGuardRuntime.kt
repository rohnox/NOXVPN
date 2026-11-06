package com.v2ray.ang.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class WireGuardState(val profileId: String? = null, val phase: Phase = Phase.Down) {
    enum class Phase { Down, Connecting, Up, Failed }
}
/** Published exclusively by WireGuardControllerService from real backend lifecycle events. */
internal object WireGuardRuntime {
    internal val mutableState = MutableStateFlow(WireGuardState())
    val state = mutableState.asStateFlow()
}
