package com.v2ray.ang.ui.protocols

import android.app.Application
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.StringReader

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class ProtocolPrivateFormatTest {
    @Test fun downloadedOvpnAndWireguardProfilesValidateWithoutConnecting() {
        val ovpn = System.getenv("SPECIAL_OVPN_FIXTURE")
        val wireguard = System.getenv("SPECIAL_WG_FIXTURE")
        assumeTrue(!ovpn.isNullOrBlank() && !wireguard.isNullOrBlank())
        val text = OpenVpnImportPolicy.decode("fixture.ovpn", File(ovpn!!).readBytes())
        val parser = de.blinkt.openvpn.core.ConfigParser(); parser.parseConfig(StringReader(text))
        assertEquals(de.blinkt.openvpn.R.string.no_error_found, parser.convertProfile().checkProfile(RuntimeEnvironment.getApplication()))
        val wg = com.wireguard.config.Config.parse(File(wireguard!!).inputStream().buffered().use { it.readBytes().inputStream() })
        assertFalse(wg.peers.isEmpty())
    }
}
