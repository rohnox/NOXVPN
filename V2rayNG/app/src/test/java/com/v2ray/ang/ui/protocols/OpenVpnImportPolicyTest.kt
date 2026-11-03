package com.v2ray.ang.ui.protocols

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OpenVpnImportPolicyTest {
    @Test fun `embedded certificate and password prompt are permitted`() {
        val config = "client\nremote example.test 1194\nauth-user-pass\n<ca>\nDATA\n</ca>\n"
        assertEquals(config, OpenVpnImportPolicy.decode("test.OVPN", config.toByteArray()))
    }
    @Test fun `script and local control directives are rejected`() {
        for (directive in listOf("up /tmp/script", "plugin evil.so", "--management 127.0.0.1 7505", "log /tmp/out", "\"up\" /tmp/script", "config other.ovpn", "<unknown>\nplugin bad\n</unknown>")) {
            assertThrows(IllegalArgumentException::class.java) { OpenVpnImportPolicy.decode("a.ovpn", "client\n$directive".toByteArray()) }
        }
    }
    @Test fun `external certificates and credential files require an inline import instead`() {
        for (directive in listOf("ca external.crt", "key /data/key", "auth-user-pass creds.txt")) {
            assertThrows(IllegalArgumentException::class.java) { OpenVpnImportPolicy.decode("a.ovpn", directive.toByteArray()) }
        }
    }
    @Test fun `connection blocks still validate their directives`() {
        val valid = "<connection>\nremote example.test 1194\n</connection>"
        assertEquals(valid, OpenVpnImportPolicy.decode("a.ovpn", valid.toByteArray()))
        assertThrows(IllegalArgumentException::class.java) {
            OpenVpnImportPolicy.decode("a.ovpn", "<connection>\nup script\n</connection>".toByteArray())
        }
    }
    @Test fun `wrong extension empty oversized binary and unclosed inline content fail`() {
        assertThrows(IllegalArgumentException::class.java) { OpenVpnImportPolicy.decode("a.txt", "client".toByteArray()) }
        assertThrows(IllegalArgumentException::class.java) { OpenVpnImportPolicy.decode("a.ovpn", byteArrayOf()) }
        assertThrows(IllegalArgumentException::class.java) { OpenVpnImportPolicy.decode("a.ovpn", ByteArray(OpenVpnImportPolicy.MAX_BYTES + 1)) }
        assertThrows(IllegalArgumentException::class.java) { OpenVpnImportPolicy.decode("a.ovpn", "client\u0000".toByteArray()) }
        assertThrows(IllegalArgumentException::class.java) { OpenVpnImportPolicy.decode("a.ovpn", "<ca>\nDATA".toByteArray()) }
    }
}
