package com.v2ray.ang.ui.visual

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import app.openconnect.core.EmbeddedRuntime
import com.v2ray.ang.core.LauncherManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class CiscoEntryTest {
    @Test fun browsingCiscoProfilesDoesNotSendStopBroadcast() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val app = activity.application
        val before = shadowOf(app).broadcastIntents.size
        LauncherManager.launchEmbeddedCisco(activity)
        assertEquals("app.openconnect.MainActivity", shadowOf(activity).nextStartedActivity.component!!.className)
        assertEquals(before, shadowOf(app).broadcastIntents.size)
    }
    @Test fun onlyConfirmedConnectionInvokesHostGateAndDuplicateResultsAreIgnored() {
        var gates = 0
        var continuation: Runnable? = null
        EmbeddedRuntime.setConnectionGate { _: Context, next: Runnable -> gates++; continuation = next }
        try {
            val activity = Robolectric.buildActivity(app.openconnect.api.GrantPermissionsActivity::class.java).get()
            val method = activity.javaClass.getDeclaredMethod("onActivityResult", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Intent::class.java).apply { isAccessible = true }
            method.invoke(activity, 0, Activity.RESULT_OK, null)
            assertEquals(1, gates)
            assertNotNull(continuation)
            assertFalse(activity.isFinishing)
            method.invoke(activity, 0, Activity.RESULT_OK, null)
            assertEquals(1, gates)
            val cancelled = Robolectric.buildActivity(app.openconnect.api.GrantPermissionsActivity::class.java).get()
            method.invoke(cancelled, 0, Activity.RESULT_CANCELED, null)
            assertEquals(1, gates)
            assertTrue(cancelled.isFinishing)
        } finally { EmbeddedRuntime.setConnectionGate(null) }
    }
}
