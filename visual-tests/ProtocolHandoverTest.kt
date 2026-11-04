package com.v2ray.ang.ui.visual

import android.app.Application
import android.app.Activity
import org.robolectric.Robolectric
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Looper
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.LauncherManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class ProtocolHandoverTest {
    @Test fun stopCommandPrecedesOpenVpnLaunch() {
        val app = RuntimeEnvironment.getApplication()
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val commands = mutableListOf<Int>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                commands += intent!!.getIntExtra("key", -1)
                assertNull(shadowOf(app).nextStartedActivity)
            }
        }
        app.registerReceiver(receiver, IntentFilter(AppConfig.BROADCAST_ACTION_SERVICE), Context.RECEIVER_NOT_EXPORTED)
        try {
            LauncherManager.launchEmbeddedOpenVpn(activity, "test-profile")
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(listOf(AppConfig.MSG_STATE_STOP), commands)
            val next = shadowOf(app).nextStartedActivity
            assertEquals("de.blinkt.openvpn.LaunchVPN", next.component!!.className)
            assertEquals("test-profile", next.getStringExtra(de.blinkt.openvpn.LaunchVPN.EXTRA_KEY))
        } finally { app.unregisterReceiver(receiver) }
    }
    @Test fun absentXrayDaemonStillAllowsCiscoManager() {
        val app = RuntimeEnvironment.getApplication()
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        LauncherManager.launchEmbeddedCisco(activity)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals("app.openconnect.MainActivity", shadowOf(app).nextStartedActivity.component!!.className)
    }
}
