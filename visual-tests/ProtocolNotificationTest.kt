package com.v2ray.ang.ui.visual

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import app.openconnect.core.OpenConnectManagementThread
import app.openconnect.core.OpenVpnService
import com.v2ray.ang.R
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.OpenVPNService
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Builds notifications only; no service lifecycle, tunnel, engine initialization or live endpoint. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class ProtocolNotificationTest {
    @Test fun ciscoTitleDoesNotInheritTheOpenVpnAppResource() {
        val service = Robolectric.buildService(OpenVpnService::class.java).get()
        val state = OpenVpnService::class.java.getDeclaredField("mConnectionState").apply { isAccessible = true }
        val build = OpenVpnService::class.java.getDeclaredMethod("buildNotification").apply { isAccessible = true }
        state.setInt(service, OpenConnectManagementThread.STATE_CONNECTED)
        val connected = build.invoke(service) as Notification
        assertEquals("Cisco connected", connected.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(R.drawable.ic_ispeed_notification, connected.smallIcon.resId)
        state.setInt(service, OpenConnectManagementThread.STATE_DISCONNECTED)
        val disconnected = build.invoke(service) as Notification
        assertEquals("Cisco", disconnected.extras.getString(Notification.EXTRA_TITLE))
        assertFalse(disconnected.extras.getString(Notification.EXTRA_TITLE)!!.contains("connected"))
    }
    @Test fun openVpnShowsConnectedOnlyForAnActualConnectedStatus() {
        val service = Robolectric.buildService(OpenVPNService::class.java).get()
        val method = OpenVPNService::class.java.getDeclaredMethod("showNotification", String::class.java, String::class.java, String::class.java, Long::class.javaPrimitiveType, ConnectionStatus::class.java, android.content.Intent::class.java).apply { isAccessible = true }
        val notifications = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = OpenVPNService.NOTIFICATION_CHANNEL_BG_ID
        method.invoke(service, "Fixture status", null, channel, 0L, ConnectionStatus.LEVEL_CONNECTED, null)
        val connected = shadowOf(notifications).getNotification(channel.hashCode())
        assertEquals("OpenVPN connected", connected.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(R.drawable.ic_ispeed_notification, connected.smallIcon.resId)
        method.invoke(service, "Fixture status", null, channel, 0L, ConnectionStatus.LEVEL_NOTCONNECTED, null)
        assertEquals("OpenVPN", shadowOf(notifications).getNotification(channel.hashCode()).extras.getString(Notification.EXTRA_TITLE))
    }
    @Test @Config(qualifiers = "fa-rIR")
    fun localizedTitlesAndNewApplicationIdentityArePresent() {
        val context = RuntimeEnvironment.getApplication()
        assertEquals("Special VPN", context.getString(R.string.app_name))
        assertEquals("سیسکو", context.getString(app.openconnect.R.string.ispeed_cisco_name))
        assertEquals("سیسکو متصل است", context.getString(app.openconnect.R.string.ispeed_cisco_connected))
        assertEquals("اوپن‌وی‌پی‌ان متصل است", context.getString(de.blinkt.openvpn.R.string.ispeed_openvpn_connected))
    }
    @Test fun quickSettingsEditorShowsSpecialVpnWithItsOwnSmallIcon() {
        val context = RuntimeEnvironment.getApplication()
        val info = context.packageManager.getServiceInfo(android.content.ComponentName(context, com.v2ray.ang.service.QSTileService::class.java), android.content.pm.PackageManager.ComponentInfoFlags.of(0))
        assertEquals("Special VPN", info.loadLabel(context.packageManager))
        assertEquals(R.drawable.ic_ispeed_notification, info.icon)
    }

}
