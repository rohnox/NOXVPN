package com.v2ray.ang.ui.protocols

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.UUID

internal enum class ProfileProtocol { OpenVPN, Cisco, WireGuard }
internal data class SubscriptionProfile(val id: String, val name: String, val protocol: ProfileProtocol, val config: String = "", val address: String = "", val username: String = "", val password: String = "") { override fun toString() = "SubscriptionProfile(id=$id, protocol=$protocol)" }
internal data class ProtocolSubscription(val id: String, val title: String, val url: String, val updatedAt: Long, val profiles: List<SubscriptionProfile>) { override fun toString() = "ProtocolSubscription(id=$id, profiles=${profiles.size})" }
internal data class ProfileDownload(val id: String, val name: String, val protocol: ProfileProtocol, val url: HttpUrl)
internal data class SubscriptionManifest(val downloads: List<ProfileDownload>, val cisco: List<SubscriptionProfile>)

/** Only customer subpage endpoints are used, never the administrative API or arbitrary ZIP links. */
internal object EylanSubscription {
    const val MAX_PROFILES = 96
    fun source(text: String): HttpUrl {
        val url = text.trim().toHttpUrl()
        require(url.isHttps && url.username.isEmpty() && url.password.isEmpty() && url.fragment == null && url.query == null)
        require(url.pathSegments.size == 3 && url.pathSegments[0] == "sub" && url.pathSegments.drop(1).all { it.matches(Regex("[A-Za-z0-9_-]{1,200}")) })
        return url
    }
    fun manifestUrl(source: HttpUrl): HttpUrl = source.newBuilder().encodedPath("/").addPathSegment("get_subpage_download_data").addPathSegment(source.pathSegments[1]).addPathSegment(source.pathSegments[2]).build()
    fun profileId(subscriptionId: String, kind: ProfileProtocol, identity: String): String = UUID.nameUUIDFromBytes("$subscriptionId/$kind/$identity".toByteArray()).toString()
    fun parse(id: String, source: HttpUrl, json: String): SubscriptionManifest {
        require(json.length <= 2 * 1024 * 1024)
        val root = JsonParser.parseString(json).asJsonObject
        require(root.get("success")?.asBoolean == true)
        val username = root.text("username").ifBlank { source.pathSegments[2] }
        require(username == source.pathSegments[2])
        fun endpoint(vararg segments: String): HttpUrl = source.newBuilder().encodedPath("/").apply { segments.forEach { addPathSegment(it) } }.build()
        val downloads = mutableListOf<ProfileDownload>()
        val ovpn = root.getAsJsonObject("openvpn")
        if (ovpn?.get("enabled")?.asBoolean == true) for (groupValue in ovpn.getAsJsonArray("groups")) {
            val group = groupValue.asJsonObject
            for (endpointValue in group.getAsJsonArray("endpoints")) {
                val e = endpointValue.asJsonObject
                val type = e.text("type")
                val name = e.text("name").take(100).ifBlank { group.text("server_title").take(100) }
                if (type == "multi_server") {
                    val url = endpoint("download_multi_ovpn", username)
                    downloads += ProfileDownload(profileId(id, ProfileProtocol.OpenVPN, url.encodedPath), name, ProfileProtocol.OpenVPN, url)
                } else {
                    require(type.matches(Regex("[a-z_]{1,40}")))
                    val ports = e.getAsJsonArray("ports") ?: group.getAsJsonArray("ports")
                    for (portValue in ports) {
                        val p = portValue.asJsonObject
                        val port = p.text("port").toInt(); require(port in 1..65535)
                        val protocol = p.text("protocol").lowercase(); require(protocol in listOf("tcp", "udp"))
                        val endpointId = e.text("id"); require(endpointId.matches(Regex("[A-Za-z0-9_-]{1,100}")))
                        val url = endpoint("download_specific_ovpn", username, type, endpointId, port.toString(), protocol)
                        downloads += ProfileDownload(profileId(id, ProfileProtocol.OpenVPN, url.encodedPath), "$name · $port/$protocol", ProfileProtocol.OpenVPN, url)
                    }
                }
            }
        }
        val cisco = mutableListOf<SubscriptionProfile>()
        root.getAsJsonObject("cisco")?.takeIf { it.get("enabled")?.asBoolean == true }?.let { section ->
            for (value in section.getAsJsonArray("servers")) {
                val s = value.asJsonObject
                val address = s.text("address").let { if (it.startsWith("https://")) it else "https://$it" }.toHttpUrl()
                require(address.isHttps && address.username.isEmpty() && address.password.isEmpty() && address.fragment == null)
                cisco += SubscriptionProfile(profileId(id, ProfileProtocol.Cisco, address.toString()), s.text("name").take(100), ProfileProtocol.Cisco, address = address.toString(), username = section.text("username"), password = section.text("password"))
            }
        }
        fun wireguard(section: JsonObject?) {
            if (section == null || section.get("enabled")?.asBoolean == false) return
            for (value in section.getAsJsonArray("servers") ?: return) {
                val s = value.asJsonObject
                val url = source.resolve(s.text("download_url")) ?: error("Missing download")
                require(url.isHttps && url.host == source.host && url.port == source.port && url.username.isEmpty() && url.password.isEmpty() && url.fragment == null)
                downloads += ProfileDownload(profileId(id, ProfileProtocol.WireGuard, url.encodedPath + "?" + url.encodedQuery.orEmpty()), s.text("name").take(100), ProfileProtocol.WireGuard, url)
            }
        }
        wireguard(root.getAsJsonObject("wireguard"))
        root.getAsJsonArray("wg_instances")?.forEach { wireguard(it.asJsonObject) }
        require(downloads.size + cisco.size in 1..MAX_PROFILES)
        require((downloads.map { it.id } + cisco.map { it.id }).distinct().size == downloads.size + cisco.size)
        return SubscriptionManifest(downloads, cisco)
    }
    private fun JsonObject.text(key: String): String = get(key)?.takeUnless { it.isJsonNull }?.asString.orEmpty()
}
