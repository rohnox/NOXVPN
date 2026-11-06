package com.v2ray.ang.core

internal enum class WireGuardCommand { Stop, Keep, Start }
internal fun wireGuardCommand(id: String?, stop: Boolean, state: WireGuardState): WireGuardCommand = when {
    stop || id == null -> WireGuardCommand.Stop
    state.profileId == id && state.phase in listOf(WireGuardState.Phase.Up, WireGuardState.Phase.Connecting) -> WireGuardCommand.Keep
    else -> WireGuardCommand.Start
}
