package com.v2ray.ang.ui.protocols

import com.v2ray.ang.R
import de.blinkt.openvpn.core.ConnectionStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OpenVpnStatusPolicyTest {
    @Test fun `only a connected engine state is connected`() {
        for (level in ConnectionStatus.entries) {
            assertEquals(level == ConnectionStatus.LEVEL_CONNECTED, openVpnUiStatus(level).second)
        }
        assertEquals(R.string.protocol_state_connected, openVpnUiStatus(ConnectionStatus.LEVEL_CONNECTED).first)
        assertEquals(R.string.protocol_state_unknown to false, openVpnUiStatus(null))
    }
    @Test fun `failure input disconnect and setup have distinct presentation`() {
        assertEquals(R.string.protocol_state_auth_failed, openVpnUiStatus(ConnectionStatus.LEVEL_AUTH_FAILED).first)
        assertEquals(R.string.protocol_state_input, openVpnUiStatus(ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT).first)
        assertEquals(R.string.protocol_state_stopped, openVpnUiStatus(ConnectionStatus.LEVEL_NOTCONNECTED).first)
        for (level in listOf(ConnectionStatus.LEVEL_START, ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED, ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET)) {
            assertEquals(R.string.protocol_state_connecting, openVpnUiStatus(level).first)
        }
    }
    @Test fun `challenge flags and malformed prompts are handled without exposing data in logs`() {
        assertEquals("Enter code: now", openVpnChallengeMessage("R,E:Enter code: now"))
        assertNull(openVpnChallengeMessage(null))
        assertNull(openVpnChallengeMessage("R"))
        assertNull(openVpnChallengeMessage("E:Not a response prompt"))
        assertNull(openVpnChallengeMessage("R:" + "x".repeat(4096)))
    }
}
