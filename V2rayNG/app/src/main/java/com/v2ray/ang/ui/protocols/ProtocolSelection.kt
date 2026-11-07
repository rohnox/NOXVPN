package com.v2ray.ang.ui.protocols

import com.v2ray.ang.core.WireGuardState

internal data class ConnectedProtocol(val protocol: ProfileProtocol, val profileId: String) {
    val key: String get() = "$protocol/$profileId"
}

internal fun hasConnectedProtocol(state: ProtocolsState): Boolean = state.connected || state.ciscoActiveId != null || state.wireguard.phase == WireGuardState.Phase.Up

/** Do not guess during overlapping/partial engine callbacks; wait for one confirmed profile identity. */
internal fun connectedProtocol(state: ProtocolsState): ConnectedProtocol? {
    if ((state.connected && state.activeProfileId == null) || (state.wireguard.phase == WireGuardState.Phase.Up && state.wireguard.profileId == null)) return null
    val candidates = buildList {
        if (state.connected) state.activeProfileId?.let { add(ConnectedProtocol(ProfileProtocol.OpenVPN, it)) }
        state.ciscoActiveId?.let { add(ConnectedProtocol(ProfileProtocol.Cisco, it)) }
        if (state.wireguard.phase == WireGuardState.Phase.Up) state.wireguard.profileId?.let { add(ConnectedProtocol(ProfileProtocol.WireGuard, it)) }
    }
    return candidates.singleOrNull()
}
