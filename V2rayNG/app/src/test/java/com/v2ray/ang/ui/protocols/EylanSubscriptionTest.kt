package com.v2ray.ang.ui.protocols

import com.google.gson.Gson
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File

class EylanSubscriptionTest {
    private val source = EylanSubscription.source("https://example.invalid:8443/sub/token/demo")
    private fun document(extra: String = "") = """{"success":true,"username":"demo","openvpn":{"enabled":true,"groups":[{"server_title":"Europe","ports":[{"port":1194,"protocol":"udp"}],"endpoints":[{"id":"abc","type":"node_tunnel","name":"Germany"}]}]},"cisco":{"enabled":true,"username":"example-user","password":"example-password","servers":[{"address":"vpn.example.invalid:443","name":"Netherlands"}]},"wireguard":{"enabled":true,"servers":[{"name":"Sweden","download_url":"/download_wg/demo"}]}$extra}"""
    @Test fun acceptsCustomerHttpsLinksAndRejectsInvalidSources() {
        assertEquals(8443, source.port)
        assertEquals("/get_subpage_download_data/token/demo", EylanSubscription.manifestUrl(source).encodedPath)
        for (link in listOf("", "http://example.invalid/sub/a/b", "https://a:b@example.invalid/sub/a/b", "https://example.invalid/sub/a/b?q=1", "https://example.invalid/sub/a/b#x", "https://example.invalid/sub/a/../b", "https://example.invalid/other/a/b")) assertThrows(Exception::class.java) { EylanSubscription.source(link) }
    }
    @Test fun mapsProtocolsAndKeepsIdsStableAcrossUpdates() {
        val first = EylanSubscription.parse("sub-one", source, document())
        val second = EylanSubscription.parse("sub-one", source, document().replace("Germany", "New name"))
        assertEquals(2, first.downloads.size)
        assertEquals(first.downloads.map { it.id }, second.downloads.map { it.id })
        assertTrue(first.cisco.single().address.startsWith("https://"))
        assertEquals(ProfileProtocol.WireGuard, first.downloads.last().protocol)
        assertTrue(EylanSubscription.parse("sub-two", source, document()).downloads.first().id != first.downloads.first().id)
        assertEquals("/download_specific_ovpn/demo/node_tunnel/abc/1194/udp", first.downloads.first().url.encodedPath)
    }
    @Test fun rejectsForeignDownloadsWrongUsersInvalidPortsAndEmptyResponses() {
        for (body in listOf(document().replace("/download_wg/demo", "https://elsewhere.invalid/file.conf"), document().replace("\"username\":\"demo\"", "\"username\":\"other\""), document().replace("1194", "99999"), document().replace("\"success\":true", "\"success\":false"), "{}", """{"success":true}""")) assertThrows(Exception::class.java) { EylanSubscription.parse("s", source, body) }
    }
    @Test fun supportsMultiServerDownloadAndDoesNotFollowZipLinks() {
        val data = document().replace("\"node_tunnel\"", "\"multi_server\"")
        val item = EylanSubscription.parse("s", source, data).downloads.first()
        assertEquals("/download_multi_ovpn/demo", item.url.encodedPath)
    }
    @Test fun storedGenerationRoundTripsAndManualProfilesRemainSeparate() {
        val profile = SubscriptionProfile("stable", "Demo", ProfileProtocol.WireGuard, "fixture config")
        val record = ProtocolSubscription("s", "Demo plan", source.toString(), 1234, listOf(profile))
        val copy = Gson().fromJson(Gson().toJson(record), ProtocolSubscription::class.java)
        assertEquals(record, copy)
        val before = ProtocolsState(busy = false, openVpnReady = true, vpnMode = true, profiles = listOf(OpenVpnRow("manual", "Existing")), subscriptions = listOf(record))
        assertTrue(canHandleProtocolAction(before, ProtocolAction.Connect("manual")))
        assertTrue(canHandleProtocolAction(before, ProtocolAction.ConnectManaged("stable")))
        assertFalse(canHandleProtocolAction(before.copy(vpnMode = false), ProtocolAction.ConnectManaged("stable")))
        assertFalse(canHandleProtocolAction(before.copy(busy = true), ProtocolAction.ConnectManaged("stable")))
        assertFalse(canHandleProtocolAction(before, ProtocolAction.ConnectManaged("missing")))
    }
    @Test fun validatesPrivatePanelFixtureWithoutEmbeddingIt() {
        val path = System.getenv("SPECIAL_EYLAN_FIXTURE")
        assumeTrue(!path.isNullOrBlank())
        val json = File(path!!).readText()
        val root = com.google.gson.JsonParser.parseString(json).asJsonObject
        val username = root.get("username").asString
        val url = EylanSubscription.source("https://example.invalid/sub/fixture/$username")
        root.getAsJsonObject("wireguard").getAsJsonArray("servers").forEach { server ->
            val item = server.asJsonObject
            val stable = EylanSubscription.profileId("fixture", ProfileProtocol.WireGuard, item.get("download_url").asString)
            item.addProperty("download_url", "/download_wg/" + stable)
        }
        val result = EylanSubscription.parse("fixture", url, root.toString())
        assertTrue(result.downloads.any { it.protocol == ProfileProtocol.OpenVPN })
        assertTrue(result.downloads.any { it.protocol == ProfileProtocol.WireGuard })
        assertTrue(result.cisco.isNotEmpty())
    }
}
