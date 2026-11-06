package com.v2ray.ang.ui.protocols

import android.content.Context
import app.openconnect.core.ProfileManager as CiscoProfiles
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.ConfigParser
import java.io.StringReader
import java.util.UUID

/** Engine files are connection-time caches. Subscription generations stay in a single MMKV commit. */
internal object SubscriptionProfileCache {
    fun openVpn(context: Context, profile: SubscriptionProfile) {
        val parser = ConfigParser(); parser.parseConfig(StringReader(profile.config))
        val vpn = parser.convertProfile().apply { setUUID(UUID.fromString(profile.id)); mName = profile.name }
        require(vpn.checkProfile(context) == de.blinkt.openvpn.R.string.no_error_found)
        val manager = ProfileManager.getInstance(context)
        val old = ProfileManager.get(context, profile.id)
        vpn.mVersion = (old?.mVersion ?: 0) + 1
        try { ProfileManager.saveProfile(context, vpn); manager.addProfile(vpn); manager.saveProfileList(context) }
        catch (error: Exception) {
            if (old == null) manager.removeProfile(context, vpn)
            else { ProfileManager.saveProfile(context, old); manager.addProfile(old); manager.saveProfileList(context) }
            throw error
        }
    }
    fun commit(context: Context, old: ProtocolSubscription?, records: List<ProtocolSubscription>, save: (List<ProtocolSubscription>) -> Unit = ProtocolSubscriptionRepository::save) {
        val manager = ProfileManager.getInstance(context)
        val open = old?.profiles.orEmpty().filter { it.protocol == ProfileProtocol.OpenVPN }.mapNotNull { ProfileManager.get(context, it.id) }
        val cisco = old?.profiles.orEmpty().filter { it.protocol == ProfileProtocol.Cisco }.mapNotNull { profile -> CiscoProfiles.snapshotSubscription(profile.id)?.let { profile.id to it } }.toMap()
        val files = buildMap<java.io.File, ByteArray> {
            open.forEach { profile -> listOf(".vp", ".cp", ".cpold").forEach { suffix ->
                val file = context.getFileStreamPath(profile.getUUIDString() + suffix)
                if (file.exists()) { require(file.length() <= OpenVpnImportPolicy.MAX_BYTES); put(file, file.readBytes()) }
            } }
            cisco.keys.forEach { id ->
                UUID.fromString(id)
                CiscoProfiles.fileSelectKeys.forEach { key ->
                    val file = java.io.File(context.filesDir, "cert.$id.$key")
                    if (file.exists()) { require(file.length() <= OpenVpnImportPolicy.MAX_BYTES); put(file, file.readBytes()) }
                }
            }
        }
        try {
            open.forEach { manager.removeProfile(context, it) }
            cisco.keys.forEach { check(CiscoProfiles.delete(it)) }
            save(records)
        } catch (error: Exception) {
            var rollbackError: Exception? = null
            open.forEach { profile -> try { ProfileManager.saveProfile(context, profile); manager.addProfile(profile) } catch (e: Exception) { rollbackError = e } }
            try { manager.saveProfileList(context) } catch (e: Exception) { rollbackError = e }
            cisco.forEach { (id, snapshot) -> try { CiscoProfiles.restoreSubscription(id, snapshot) } catch (e: Exception) { rollbackError = e } }
            files.forEach { (file, bytes) -> try { file.writeBytes(bytes) } catch (e: Exception) { rollbackError = e } }
            rollbackError?.let { error.addSuppressed(it) }
            throw error
        }
    }
}
