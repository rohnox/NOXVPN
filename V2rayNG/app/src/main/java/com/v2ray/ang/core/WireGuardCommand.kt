package com.v2ray.ang.core

internal enum class WireGuardCommand { Stop, Keep, Start, Ignore }
internal fun wireGuardCommand(id: String?, stop: Boolean, state: WireGuardState, stopping: Boolean = false): WireGuardCommand = when {
    stop || id == null -> WireGuardCommand.Stop
    stopping -> WireGuardCommand.Ignore
    state.profileId == id && state.phase in listOf(WireGuardState.Phase.Up, WireGuardState.Phase.Connecting) -> WireGuardCommand.Keep
    else -> WireGuardCommand.Start
}
