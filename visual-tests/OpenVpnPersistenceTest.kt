package com.v2ray.ang.ui.visual

import android.app.Application
import android.content.Context
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ProfileManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class OpenVpnPersistenceTest {
    @Test fun earlyDisconnectWaitsForBindingBeforeFinishing() {
        val controller = org.robolectric.Robolectric.buildActivity(de.blinkt.openvpn.activities.DisconnectVPN::class.java).create()
        val activity = controller.get()
        activity.onClick(org.mockito.Mockito.mock(android.content.DialogInterface::class.java), android.content.DialogInterface.BUTTON_POSITIVE)
        assertFalse(activity.isFinishing)
        val api = org.mockito.Mockito.mock(de.blinkt.openvpn.core.IOpenVPNServiceInternal::class.java)
        val binder = org.mockito.Mockito.mock(android.os.IBinder::class.java)
        org.mockito.Mockito.`when`(binder.queryLocalInterface(org.mockito.ArgumentMatchers.anyString())).thenReturn(api)
        val connection = de.blinkt.openvpn.activities.DisconnectVPN::class.java.getDeclaredField("mConnection").apply { isAccessible = true }.get(activity) as android.content.ServiceConnection
        connection.onServiceConnected(android.content.ComponentName(activity, de.blinkt.openvpn.core.OpenVPNService::class.java), binder)
        org.mockito.Mockito.verify(api).stopVPN(false)
        assertTrue(activity.isFinishing)
        controller.destroy()
    }
    @Test fun disconnectBinderQueuesWorkWithoutExecutingSocketStopOnCaller() {
        val service = org.robolectric.Robolectric.buildService(de.blinkt.openvpn.core.OpenVPNService::class.java).get()
        val handler = org.mockito.Mockito.mock(android.os.Handler::class.java)
        org.mockito.Mockito.`when`(handler.post(org.mockito.ArgumentMatchers.any(Runnable::class.java))).thenReturn(true)
        de.blinkt.openvpn.core.OpenVPNService::class.java.getDeclaredField("mCommandHandler").apply { isAccessible = true }.set(service, handler)
        val binder = de.blinkt.openvpn.core.OpenVPNService::class.java.getDeclaredField("mBinder").apply { isAccessible = true }.get(service) as android.os.IBinder
        val api = de.blinkt.openvpn.core.IOpenVPNServiceInternal.Stub.asInterface(binder)
        assertTrue(api.stopVPN(false))
        org.mockito.Mockito.verify(handler).post(org.mockito.ArgumentMatchers.any(Runnable::class.java))
    }
    private fun reset() {
        ProfileManager::class.java.getDeclaredField("instance").apply { isAccessible = true }.set(null, null)
    }

    @Test fun serializedProfileSurvivesFreshManagerAndStaleIndex() {
        val context = RuntimeEnvironment.getApplication()
        reset()
        val manager = ProfileManager.getInstance(context)
        val profile = VpnProfile("Imported fixture")
        ProfileManager.saveProfile(context, profile)
        manager.addProfile(profile)
        manager.saveProfileList(context)
        assertEquals(profile.uuidString, manager.reloadStoredProfile(context, profile.uuidString)!!.uuidString)
        context.getSharedPreferences("VPNList", Context.MODE_PRIVATE).edit()
            .putStringSet("vpnlist", emptySet()).putBoolean("didrecover", true).commit()
        reset()
        val restored = ProfileManager.getInstance(context).profiles.single { it.uuidString == profile.uuidString }
        assertEquals("Imported fixture", restored.name)
        assertEquals(profile.mVersion, restored.mVersion)
    }

    @Test fun missingProfileNotificationReturnsWithoutNullCrashOrPolling() {
        val context = RuntimeEnvironment.getApplication()
        reset()
        ProfileManager.getInstance(context)
        val start = System.nanoTime()
        ProfileManager.notifyProfileVersionChanged(context, java.util.UUID.randomUUID().toString(), 1)
        assertTrue("Missing profile must not poll cached preferences", (System.nanoTime() - start) / 1_000_000 < 1500)
        assertNull(ProfileManager.get(context, "../invalid", 1, 100))
    }

    @Test fun daemonWithAnAlreadyLoadedEmptyIndexReadsNewProfileByUuid() {
        val context = RuntimeEnvironment.getApplication()
        reset()
        ProfileManager.getInstance(context)
        val profile = VpnProfile("Late import fixture")
        ProfileManager.saveProfile(context, profile)
        // Deliberately do not update the cached manager/index, as in another process.
        assertEquals(profile.mVersion, ProfileManager.get(context, profile.uuidString, profile.mVersion, 100)!!.mVersion)
    }

    @Test fun removingProfileDoesNotRecoverItOnFreshManager() {
        val context = RuntimeEnvironment.getApplication()
        reset()
        val manager = ProfileManager.getInstance(context)
        val profile = VpnProfile("Delete fixture")
        ProfileManager.saveProfile(context, profile)
        manager.addProfile(profile)
        manager.saveProfileList(context)
        manager.removeProfile(context, profile)
        reset()
        assertFalse(ProfileManager.getInstance(context).profiles.any { it.uuidString == profile.uuidString })
        assertNull(ProfileManager.get(context, profile.uuidString))
    }

}
