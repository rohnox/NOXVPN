package com.v2ray.ang.ui.protocols

import com.v2ray.ang.core.WireGuardState
import org.junit.Assert.*
import org.junit.Test

class ProtocolSelectionTest {
    @Test fun onlyConfirmedSingleProfileChoosesDefaultTab() {
        assertNull(connectedProtocol(ProtocolsState()))
        assertNull(connectedProtocol(ProtocolsState(wireguard = WireGuardState("wg", WireGuardState.Phase.Connecting))))
        assertEquals(ConnectedProtocol(ProfileProtocol.OpenVPN, "ovpn"), connectedProtocol(ProtocolsState(connected = true, activeProfileId = "ovpn")))
        assertEquals(ConnectedProtocol(ProfileProtocol.Cisco, "cisco"), connectedProtocol(ProtocolsState(ciscoActiveId = "cisco")))
        assertEquals(ConnectedProtocol(ProfileProtocol.WireGuard, "wg"), connectedProtocol(ProtocolsState(wireguard = WireGuardState("wg", WireGuardState.Phase.Up))))
        assertNull(connectedProtocol(ProtocolsState(connected = true)))
        assertNull(connectedProtocol(ProtocolsState(connected = true, activeProfileId = "ovpn", ciscoActiveId = "cisco")))
    }
    @Test fun ciscoBridgeUsesConfirmedOwnerAndRemovalStopsCallbacks() {
        val values = mutableListOf<String?>()
        val listener = app.openconnect.core.EmbeddedRuntime.ConnectionStatusListener { values += it }
        app.openconnect.core.EmbeddedRuntime.publishConnectionStatus(null, false)
        app.openconnect.core.EmbeddedRuntime.addStatusListener(listener)
        try {
            app.openconnect.core.EmbeddedRuntime.publishConnectionStatus("profile", false)
            assertNull(app.openconnect.core.EmbeddedRuntime.getConnectedProfileId())
            app.openconnect.core.EmbeddedRuntime.publishConnectionStatus("profile", true)
            assertEquals("profile", app.openconnect.core.EmbeddedRuntime.getConnectedProfileId())
            app.openconnect.core.EmbeddedRuntime.publishConnectionStatus(null, false)
            assertEquals(listOf(null, null, "profile", null), values)
        } finally { app.openconnect.core.EmbeddedRuntime.removeStatusListener(listener) }
        app.openconnect.core.EmbeddedRuntime.publishConnectionStatus(null, false)
        assertEquals(4, values.size)
    }
}
