package com.v2ray.ang.ui.protocols

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ProtocolActionsTest {
    @Test fun `only daemon reported connected uuid can disconnect and show a check`() {
        val state = ready.copy(activeProfileId = "a", profiles = listOf(OpenVpnRow("a", "Same name"), OpenVpnRow("b", "Same name")))
        assertTrue(isConnectedOpenVpnProfile(state, "a"))
        assertFalse(isConnectedOpenVpnProfile(state, "b"))
        assertTrue(canHandleProtocolAction(state, ProtocolAction.DisconnectProfile("a")))
        assertFalse(canHandleProtocolAction(state, ProtocolAction.DisconnectProfile("b")))
        assertFalse(canHandleProtocolAction(state, ProtocolAction.Connect("a")))
        assertFalse(isConnectedOpenVpnProfile(state.copy(connected = false), "a"))
    }

    @Test fun `delete requires confirmed idle engine and current profile identity`() {
        val state = ready.copy(connected = false, status = com.v2ray.ang.R.string.protocol_state_stopped)
        assertTrue(canHandleProtocolAction(state, ProtocolAction.Delete("a")))
        assertFalse(canHandleProtocolAction(state, ProtocolAction.Delete("missing")))
        for (candidate in listOf(state.copy(busy = true), state.copy(connected = true),
            state.copy(status = com.v2ray.ang.R.string.protocol_state_connecting), state.copy(status = com.v2ray.ang.R.string.protocol_state_unknown))) {
            assertFalse(canHandleProtocolAction(candidate, ProtocolAction.Delete("a")))
        }
    }
    private val ready = ProtocolsState(busy = false, openVpnReady = true, ciscoReady = true, vpnMode = true, connected = true,
        profiles = listOf(OpenVpnRow("a", "A"), OpenVpnRow("b", "B")))
    private val actions = listOf(ProtocolAction.Import, ProtocolAction.Connect("a"), ProtocolAction.Disconnect, ProtocolAction.Cisco)

    @Test fun `cold loading failed and busy states cannot launch engines`() {
        for (state in listOf(ProtocolsState(), ProtocolsState(busy = false))) {
            actions.forEach { assertFalse(canHandleProtocolAction(state, it)) }
        }
        val busy = ready.copy(busy = true)
        actions.filterNot { it == ProtocolAction.Disconnect }.forEach { assertFalse(canHandleProtocolAction(busy, it)) }
        assertTrue(canHandleProtocolAction(busy, ProtocolAction.Disconnect))
        assertTrue(canHandleProtocolAction(busy.copy(wireguard = com.v2ray.ang.core.WireGuardState("wg", com.v2ray.ang.core.WireGuardState.Phase.Up)), ProtocolAction.DisconnectWireGuard))
    }
    @Test fun `ready operations use profile identity and reject empty unknown or removed IDs`() {
        actions.forEach { assertTrue(canHandleProtocolAction(ready, it)) }
        assertTrue(canHandleProtocolAction(ready.copy(profiles = ready.profiles.reversed()), ProtocolAction.Connect("a")))
        for (id in listOf("", "missing", "a")) {
            assertFalse(canHandleProtocolAction(ready.copy(profiles = emptyList()), ProtocolAction.Connect(id)))
        }
    }
    @Test fun `proxy and root modes still permit import but never embedded VPN launch`() {
        val state = ready.copy(vpnMode = false)
        assertTrue(canHandleProtocolAction(state, ProtocolAction.Import))
        actions.drop(1).forEach { assertFalse(canHandleProtocolAction(state, it)) }
    }
    @Test fun `engine failures remain isolated`() {
        val noOpenVpn = ready.copy(openVpnReady = false)
        actions.take(3).forEach { assertFalse(canHandleProtocolAction(noOpenVpn, it)) }
        assertTrue(canHandleProtocolAction(noOpenVpn, ProtocolAction.Cisco))
        val noCisco = ready.copy(ciscoReady = false)
        assertFalse(canHandleProtocolAction(noCisco, ProtocolAction.Cisco))
        actions.take(3).forEach { assertTrue(canHandleProtocolAction(noCisco, it)) }
    }
    @Test fun `disconnected profiles cannot request disconnect but pending authentication can be canceled`() {
        val stopped = ready.copy(connected = false)
        assertFalse(canHandleProtocolAction(stopped, ProtocolAction.Disconnect))
        for (status in listOf(com.v2ray.ang.R.string.protocol_state_connecting, com.v2ray.ang.R.string.protocol_state_input)) {
            assertTrue(canHandleProtocolAction(stopped.copy(status = status), ProtocolAction.Disconnect))
        }
    }}
