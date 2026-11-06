package com.v2ray.ang.ui.protocols

import android.app.Application
import de.blinkt.openvpn.core.ProfileManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito
import org.mockito.kotlin.*
import java.io.File
import java.io.IOException

class SubscriptionCacheRollbackTest {
    @Test fun failedCatalogCommitRestoresCachedProfilesAndTheirPrivateFiles() {
        val context = mock<Application>()
        val manager = mock<ProfileManager>()
        val profile = mock<de.blinkt.openvpn.VpnProfile>()
        val openId = "26b95250-eae0-4f6b-bfe7-d339253b057d"
        val ciscoId = "06cf4224-a07d-41f3-b39e-7e30b0a787a2"
        val directory = kotlin.io.path.createTempDirectory("subscription-cache-test").toFile()
        whenever(context.filesDir).thenReturn(directory)
        whenever(context.getFileStreamPath(any())).thenAnswer { File(directory, it.getArgument<String>(0)) }
        whenever(profile.getUUIDString()).thenReturn(openId)
        val cp = File(directory, "$openId.cp").apply { writeText("fixture auth") }
        val cert = File(directory, "cert.$ciscoId.ca_certificate").apply { writeText("fixture cert") }
        val prefs = mapOf<String, Any>("profile_uuid" to ciscoId, "profile_name" to "Fixture")
        try {
            Mockito.mockStatic(ProfileManager::class.java).use { vpn ->
                Mockito.mockStatic(app.openconnect.core.ProfileManager::class.java).use { cisco ->
                    vpn.`when`<ProfileManager> { ProfileManager.getInstance(context) }.thenReturn(manager)
                    vpn.`when`<de.blinkt.openvpn.VpnProfile?> { ProfileManager.get(context, openId) }.thenReturn(profile)
                    cisco.`when`<Map<String, *>?> { app.openconnect.core.ProfileManager.snapshotSubscription(ciscoId) }.thenReturn(prefs)
                    cisco.`when`<Boolean> { app.openconnect.core.ProfileManager.delete(ciscoId) }.thenAnswer { cert.delete(); true }
                    doAnswer { cp.delete(); null }.whenever(manager).removeProfile(context, profile)
                    val old = ProtocolSubscription("s", "Fixture", "", 0, listOf(SubscriptionProfile(openId, "Open", ProfileProtocol.OpenVPN), SubscriptionProfile(ciscoId, "Cisco", ProfileProtocol.Cisco)))
                    assertThrows(IOException::class.java) { SubscriptionProfileCache.commit(context, old, emptyList()) { throw IOException("fixture storage") } }
                    assertEquals("fixture auth", cp.readText())
                    assertEquals("fixture cert", cert.readText())
                    verify(manager).addProfile(profile)
                    cisco.verify { app.openconnect.core.ProfileManager.restoreSubscription(ciscoId, prefs) }
                }
            }
        } finally {
            // All files here were explicitly created by this test in its unique temporary directory.
            directory.listFiles()?.forEach { it.delete() }; directory.delete()
        }
    }
}
