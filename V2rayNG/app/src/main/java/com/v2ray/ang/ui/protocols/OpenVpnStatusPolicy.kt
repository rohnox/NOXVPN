package com.v2ray.ang.ui.protocols

import com.v2ray.ang.R
import de.blinkt.openvpn.core.ConnectionStatus

internal fun openVpnUiStatus(level: ConnectionStatus?): Pair<Int, Boolean> = when (level) {
    ConnectionStatus.LEVEL_CONNECTED -> R.string.protocol_state_connected to true
    ConnectionStatus.LEVEL_NOTCONNECTED -> R.string.protocol_state_stopped to false
    ConnectionStatus.LEVEL_AUTH_FAILED -> R.string.protocol_state_auth_failed to false
    ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT -> R.string.protocol_state_input to false
    ConnectionStatus.LEVEL_START, ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED,
    ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET -> R.string.protocol_state_connecting to false
    else -> R.string.protocol_state_unknown to false
}

internal fun isConnectedOpenVpnProfile(state: ProtocolsState, guid: String): Boolean =
    state.connected && state.activeProfileId == guid

internal fun openVpnSessionActive(state: ProtocolsState): Boolean = state.connected || state.status in setOf(
    R.string.protocol_state_unknown, R.string.protocol_state_connecting, R.string.protocol_state_input
)
