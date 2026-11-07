package com.v2ray.ang.ui.protocols

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.os.ResultReceiver
import androidx.core.content.IntentCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.core.WireGuardRuntime
import com.v2ray.ang.core.WireGuardState
import com.v2ray.ang.service.WireGuardControllerService
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.Tunnel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.launch

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class WireGuardHandoverTest {
    private fun handoverReplies(app: Application): MutableList<ResultReceiver> {
        val replies = mutableListOf<ResultReceiver>()
        val binder = android.os.Messenger(android.os.Handler(Looper.getMainLooper()) { message ->
            replies += androidx.core.os.BundleCompat.getParcelable(message.data, "stopReply", ResultReceiver::class.java)!!
            true
        }).binder
        shadowOf(app).setComponentNameAndServiceForBindService(android.content.ComponentName(app, com.v2ray.ang.service.VpnHandoverService::class.java), binder)
        return replies
    }
    @Test fun bothConfirmedRoutesWaitForCommonHandoverAcknowledgementBeforeContinuing() {
        for (cisco in listOf(true, false)) {
            val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
            val app = activity.application
            val before = shadowOf(app).broadcastIntents.size
            val replies = handoverReplies(app)
            var starts = 0
            if (cisco) LauncherManager.prepareEmbeddedCiscoConnection(activity) { starts++ }
            else LauncherManager.prepareEmbeddedOpenVpnConnection(activity) { starts++ }
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(com.v2ray.ang.service.VpnHandoverService::class.java.name, shadowOf(app).nextStartedService.component!!.className)
            assertEquals(0, starts); assertEquals(before, shadowOf(app).broadcastIntents.size)
            val reply = replies.single()
            reply.send(Activity.RESULT_OK, null)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(1, starts)
            assertTrue(shadowOf(app).broadcastIntents.drop(before).all { it.`package` == AppConfig.ANG_PACKAGE })
            assertEquals(before, shadowOf(app).broadcastIntents.size)
            activity.finish()
        }
    }
    @Test fun destroyedCallerCannotLaunchAnotherVpnAfterLateStopReply() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        var starts = 0
        val replies = handoverReplies(activity.application)
        LauncherManager.prepareEmbeddedOpenVpnConnection(activity) { starts++ }
        shadowOf(Looper.getMainLooper()).idle()
        activity.finish()
        replies.single().send(Activity.RESULT_OK, null)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, starts)
    }
    private fun set(service: WireGuardControllerService, field: String, value: Any?) {
        service.javaClass.getDeclaredField(field).apply { isAccessible = true }.set(service, value)
    }
    private fun pumpUntil(condition: () -> Boolean) {
        val end = System.nanoTime() + TimeUnit.SECONDS.toNanos(4)
        while (!condition() && System.nanoTime() < end) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(5) }
        assertTrue("Service callback deadline", condition())
    }
    @Test fun serviceRepliesAfterActualBackendDownDeduplicatesStopsAndRejectsRestartDuringTeardown() {
        val controller = Robolectric.buildService(WireGuardControllerService::class.java).create()
        val service = controller.get()
        val entered = CountDownLatch(1); val release = CountDownLatch(1)
        val backend = mock<Backend>()
        val tunnel = object : Tunnel { override fun getName() = "fixture"; override fun onStateChange(state: Tunnel.State) {} }
        whenever(backend.setState(tunnel, Tunnel.State.DOWN, null)).thenAnswer {
            entered.countDown(); check(release.await(3, TimeUnit.SECONDS)); Tunnel.State.DOWN
        }
        set(service, "backend", backend); set(service, "tunnel", tunnel)
        WireGuardRuntime.mutableState.value = WireGuardState("fixture", WireGuardState.Phase.Up)
        val replies = AtomicInteger()
        val reply = object : ResultReceiver(android.os.Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(code: Int, data: Bundle?) {
                assertEquals(Activity.RESULT_OK, code)
                assertEquals(WireGuardState.Phase.Down, WireGuardRuntime.state.value.phase)
                replies.incrementAndGet()
            }
        }
        val stop = Intent(service, WireGuardControllerService::class.java).setAction(WireGuardControllerService.STOP).putExtra(WireGuardControllerService.STOP_REPLY, reply)
        try {
            service.onStartCommand(stop, 0, 1)
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            assertEquals(WireGuardState.Phase.Up, WireGuardRuntime.state.value.phase)
            service.onStartCommand(stop, 0, 2)
            service.onStartCommand(Intent(service, WireGuardControllerService::class.java).putExtra(WireGuardControllerService.PROFILE, "other"), 0, 3)
            assertEquals(0, replies.get())
            release.countDown()
            pumpUntil { replies.get() == 2 }
            verify(backend, times(1)).setState(tunnel, Tunnel.State.DOWN, null)
            verify(backend, never()).setState(any(), eq(Tunnel.State.UP), anyOrNull())
        } finally {
            release.countDown(); controller.destroy(); WireGuardRuntime.mutableState.value = WireGuardState()
        }
    }
    @Test fun openVpnConsentCancellationAndDuplicateResultsDoNotInvokeAnotherHandover() {
        var gates = 0; var pending: Runnable? = null
        de.blinkt.openvpn.core.EmbeddedRuntime.setConnectionGate { _, connect -> gates++; pending = connect }
        try {
            val activity = Robolectric.buildActivity(de.blinkt.openvpn.LaunchVPN::class.java).get()
            val profile = mock<de.blinkt.openvpn.VpnProfile>()
            de.blinkt.openvpn.core.ProfileManager::class.java.getDeclaredField("tmpprofile").apply { isAccessible = true }.set(null, profile)
            activity.javaClass.getDeclaredField("mSelectedProfile").apply { isAccessible = true }.set(activity, profile)
            val result = activity.javaClass.getDeclaredMethod("onActivityResult", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Intent::class.java).apply { isAccessible = true }
            result.invoke(activity, 70, Activity.RESULT_OK, null)
            result.invoke(activity, 70, Activity.RESULT_OK, null)
            pumpUntil { gates == 1 }
            activity.finish(); pending!!.run()
            assertNull(shadowOf(activity.application).nextStartedService)
            val cancelled = Robolectric.buildActivity(de.blinkt.openvpn.LaunchVPN::class.java).get()
            result.invoke(cancelled, 70, Activity.RESULT_CANCELED, null)
            assertEquals(1, gates); assertTrue(cancelled.isFinishing)
        } finally {
            de.blinkt.openvpn.core.ProfileManager::class.java.getDeclaredField("tmpprofile").apply { isAccessible = true }.set(null, null)
            de.blinkt.openvpn.core.EmbeddedRuntime.setConnectionGate(null)
        }
    }
    @Test fun failedStopReplyDoesNotStopXrayOrStartCisco() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        var starts = 0
        val before = shadowOf(activity.application).broadcastIntents.size
        val replies = handoverReplies(activity.application)
        LauncherManager.prepareEmbeddedCiscoConnection(activity) { starts++ }
        shadowOf(Looper.getMainLooper()).idle()
        replies.single().send(Activity.RESULT_CANCELED, null)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, starts); assertEquals(before, shadowOf(activity.application).broadcastIntents.size)
        assertTrue(activity.isFinishing)
    }
    @Test fun destructionDuringStopStillJoinsEveryOlderSetupBeforeNativeCleanup() {
        val controller = Robolectric.buildService(WireGuardControllerService::class.java).create()
        val service = controller.get()
        val entered = CountDownLatch(1); val release = CountDownLatch(1); val down = CountDownLatch(1)
        val backend = mock<Backend>()
        val tunnel = object : Tunnel { override fun getName() = "fixture"; override fun onStateChange(state: Tunnel.State) {} }
        whenever(backend.setState(tunnel, Tunnel.State.DOWN, null)).thenAnswer { down.countDown(); Tunnel.State.DOWN }
        set(service, "backend", backend); set(service, "tunnel", tunnel)
        WireGuardRuntime.mutableState.value = WireGuardState("fixture", WireGuardState.Phase.Connecting)
        val fixtureOwner = kotlinx.coroutines.SupervisorJob()
        val fixtureScope = kotlinx.coroutines.CoroutineScope(fixtureOwner + kotlinx.coroutines.Dispatchers.IO)
        val setup = fixtureScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { entered.countDown(); check(release.await(3, TimeUnit.SECONDS)) }
        }
        @Suppress("UNCHECKED_CAST")
        val jobs = service.javaClass.getDeclaredField("setupJobs").apply { isAccessible = true }.get(service) as MutableList<kotlinx.coroutines.Job>
        jobs += setup
        val replies = AtomicInteger()
        val reply = object : ResultReceiver(android.os.Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(code: Int, data: Bundle?) { assertEquals(Activity.RESULT_CANCELED, code); replies.incrementAndGet() }
        }
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            service.onStartCommand(Intent(service, WireGuardControllerService::class.java).setAction(WireGuardControllerService.STOP).putExtra(WireGuardControllerService.STOP_REPLY, reply), 0, 1)
            controller.destroy()
            assertFalse("Native cleanup must wait for setup ownership", down.await(100, TimeUnit.MILLISECONDS))
            release.countDown()
            pumpUntil { down.count == 0L && replies.get() == 1 }
            assertEquals(WireGuardState.Phase.Down, WireGuardRuntime.state.value.phase)
            verify(backend, times(1)).setState(tunnel, Tunnel.State.DOWN, null)
        } finally { release.countDown(); fixtureOwner.cancel(); WireGuardRuntime.mutableState.value = WireGuardState() }
    }

    @Test fun coldStopAcknowledgesNoOwnedTunnelWithoutStartingABackend() {
        val controller = Robolectric.buildService(WireGuardControllerService::class.java).create()
        val service = controller.get(); var result: Int? = null
        val reply = object : ResultReceiver(android.os.Handler(Looper.getMainLooper())) { override fun onReceiveResult(code: Int, data: Bundle?) { result = code } }
        try {
            service.onStartCommand(Intent(service, WireGuardControllerService::class.java).setAction(WireGuardControllerService.STOP).putExtra(WireGuardControllerService.STOP_REPLY, reply), 0, 1)
            pumpUntil { result != null }
            assertEquals(Activity.RESULT_OK, result)
            assertEquals(WireGuardState.Phase.Down, WireGuardRuntime.state.value.phase)
            assertNull(service.javaClass.getDeclaredField("backend").apply { isAccessible = true }.get(service))
        } finally { controller.destroy() }
    }
    @Test fun backendStopFailureIsNotAcknowledgedAsSuccessOrReportedAsConnected() {
        val priority = com.v2ray.ang.util.LogUtil::class.java.getDeclaredField("cachedMinPriority").apply { isAccessible = true }
        val oldPriority = priority.getInt(null); priority.setInt(null, android.util.Log.ERROR)
        val controller = Robolectric.buildService(WireGuardControllerService::class.java).create()
        val service = controller.get(); var result: Int? = null
        val backend = mock<Backend>()
        val tunnel = object : Tunnel { override fun getName() = "fixture"; override fun onStateChange(state: Tunnel.State) {} }
        whenever(backend.setState(tunnel, Tunnel.State.DOWN, null)).thenThrow(IllegalStateException("secret-placeholder"))
        set(service, "backend", backend); set(service, "tunnel", tunnel)
        WireGuardRuntime.mutableState.value = WireGuardState("fixture", WireGuardState.Phase.Up)
        val reply = object : ResultReceiver(android.os.Handler(Looper.getMainLooper())) { override fun onReceiveResult(code: Int, data: Bundle?) { result = code } }
        try {
            service.onStartCommand(Intent(service, WireGuardControllerService::class.java).setAction(WireGuardControllerService.STOP).putExtra(WireGuardControllerService.STOP_REPLY, reply), 0, 1)
            pumpUntil { result != null }
            assertEquals(Activity.RESULT_CANCELED, result)
            assertEquals(WireGuardState.Phase.Failed, WireGuardRuntime.state.value.phase)
            assertFalse(org.robolectric.shadows.ShadowLog.getLogsForTag(AppConfig.TAG).any { it.msg.contains("secret-placeholder") || it.throwable?.message?.contains("secret-placeholder") == true })
        } finally {
            controller.destroy()
            val owner = service.javaClass.getDeclaredField("owner").apply { isAccessible = true }.get(service) as kotlinx.coroutines.Job
            pumpUntil { owner.isCompleted }
            priority.setInt(null, oldPriority); WireGuardRuntime.mutableState.value = WireGuardState()
        }
    }

}
