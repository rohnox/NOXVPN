package com.v2ray.ang.ui.protocols

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.*
import android.os.*
import androidx.core.os.BundleCompat
import com.v2ray.ang.core.VpnEngine
import com.v2ray.ang.core.VpnHandoverQueue
import com.v2ray.ang.helper.MessageHelper
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implements
import org.robolectric.RobolectricTestRunner
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [24, 31, 35], shadows = [BinderResultReceiverShadow::class])
class OwnerStopIpcTest {
    class FixtureOwner : Service() { override fun onBind(intent: Intent?): IBinder? = null }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private fun drain() { shadowOf(Looper.getMainLooper()).idle() }
    @Test fun callbackSurvivesMessengerBundleWithFrameworkClassLoader() {
        var result: Int? = null
        val callback = object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(code: Int, data: Bundle?) { result = code }
        }
        val endpoint = MessageHelper.frameworkReceiver(callback)
        assertEquals(ResultReceiver::class.java, endpoint.javaClass)
        for (key in listOf("stopReply", "dispatchReceipt")) {
            val parcel = Parcel.obtain()
            val received = try {
                Bundle().apply { putParcelable(key, endpoint) }.writeToParcel(parcel, 0)
                parcel.setDataPosition(0)
                Bundle.CREATOR.createFromParcel(parcel).apply {
                    classLoader = ResultReceiver::class.java.classLoader
                }
            } finally { parcel.recycle() }
            val reply = BundleCompat.getParcelable(received, key, ResultReceiver::class.java)
            assertNotNull(reply)
            result = null
            reply!!.send(Activity.RESULT_OK, null)
            drain()
            assertEquals(Activity.RESULT_OK, result)
        }
    }
    @Before fun installedAppHasItsMergedAndroidxSignaturePermission() {
        val app = RuntimeEnvironment.getApplication()
        // Android grants the declaring app this AndroidX manifest permission at install time.
        // Robolectric's older SDK permission model needs that installed-app grant explicitly.
        shadowOf(app).grantPermissions(app.packageName + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
    }

    @Test fun realCoordinatorAllowsEachColdTargetWithoutInstantiatingAnyVpnOwner() {
        val app = RuntimeEnvironment.getApplication()
        val controller = Robolectric.buildService(com.v2ray.ang.service.VpnHandoverService::class.java).create()
        val service = controller.get()
        shadowOf(app).setComponentNameAndServiceForBindService(ComponentName(app, service.javaClass), service.onBind(Intent(app, service.javaClass)))
        try {
            for (engine in VpnEngine.entries) {
                var result: Boolean? = null
                MessageHelper.prepareVpnHandover(app, engine) { result = it }
                drain(); assertEquals("Coordinator cold target $engine", true, result)
                val started = shadowOf(app).nextStartedService
                if (started != null) assertEquals(service.javaClass.name, started.component!!.className)
            }
        } finally { scope.cancel(); controller.destroy() }
    }

    @Test fun coldOwnersCompleteWithoutAnyBindingEvenWhenAndroidWouldAcceptABindWithoutCallback() {
        val app = RuntimeEnvironment.getApplication()
        var binds = 0
        val context = object : ContextWrapper(app) {
            override fun bindService(intent: Intent, connection: ServiceConnection, flags: Int): Boolean { binds++; return true }
        }
        try {
            for (engine in VpnEngine.entries) {
                var result: Boolean? = null
                val job = scope.launch { result = MessageHelper.stopVpnOwner(context, engine) }
                drain()
                assertTrue("Cold $engine must not await a missing bind callback", job.isCompleted)
                assertEquals(true, result)
            }
            assertEquals(0, binds)
            assertNull(shadowOf(app).nextStartedService)
        } finally { scope.cancel() }
    }

    @Test fun liveOwnersMustAcknowledgeNativeCleanupAndUnregisteredOwnersAreImmediatelyAbsent() {
        val app = RuntimeEnvironment.getApplication()
        val controller = Robolectric.buildService(FixtureOwner::class.java).create()
        try {
            for (engine in VpnEngine.entries) {
                var reply: ResultReceiver? = null
                val endpoint = Messenger(Handler(Looper.getMainLooper()) { message ->
                    reply = BundleCompat.getParcelable(message.data, "stopReply", ResultReceiver::class.java); true
                })
                val registration = MessageHelper.registerVpnOwner(controller.get(), engine, endpoint.binder)
                var result: Boolean? = null
                val job = scope.launch { result = MessageHelper.stopVpnOwner(app, engine) }
                drain(); assertNotNull(reply); assertNull(result); assertFalse(job.isCompleted)
                registration.close()
                // Destruction/unregistration alone must not turn a claimed request into success.
                drain(); assertNull(result)
                reply!!.send(Activity.RESULT_OK, null)
                drain(); assertEquals(true, result); assertTrue(job.isCompleted)
                result = null
                scope.launch { result = MessageHelper.stopVpnOwner(app, engine) }
                drain(); assertEquals(true, result)
            }
        } finally { scope.cancel(); controller.destroy() }
    }

    @Test fun allTwelveHandoverDirectionsPassThroughRealOwnerAndAbsentOwnerBroadcasts() {
        val app = RuntimeEnvironment.getApplication()
        val controller = Robolectric.buildService(FixtureOwner::class.java).create()
        try {
            for (previous in VpnEngine.entries) for (target in VpnEngine.entries.filter { it != previous }) {
                var stopped = false; var dispatched = false
                val endpoint = Messenger(Handler(Looper.getMainLooper()) { message ->
                    stopped = true
                    BundleCompat.getParcelable(message.data, "stopReply", ResultReceiver::class.java)!!.send(Activity.RESULT_OK, null)
                    true
                })
                val registration = MessageHelper.registerVpnOwner(controller.get(), previous, endpoint.binder)
                try {
                    val queue = VpnHandoverQueue()
                    val job = scope.launch {
                        assertTrue(queue.prepare(queue.reserve(), target, dispatch = {
                            assertTrue("$previous -> $target", stopped); dispatched = true
                        }) { MessageHelper.stopVpnOwner(app, it) })
                    }
                    drain(); assertTrue(job.isCompleted); assertTrue(dispatched)
                } finally { registration.close() }
            }
            assertNull(shadowOf(app).nextStartedService)
        } finally { scope.cancel(); controller.destroy() }
    }

    @Test fun claimedStopFailureAndCancellationNeverPermitDispatch() {
        val app = RuntimeEnvironment.getApplication()
        val controller = Robolectric.buildService(FixtureOwner::class.java).create()
        var reply: ResultReceiver? = null
        val endpoint = Messenger(Handler(Looper.getMainLooper()) { message -> reply = BundleCompat.getParcelable(message.data, "stopReply", ResultReceiver::class.java); true })
        val registration = MessageHelper.registerVpnOwner(controller.get(), VpnEngine.Cisco, endpoint.binder)
        try {
            var result: Boolean? = null
            scope.launch { result = MessageHelper.stopVpnOwner(app, VpnEngine.Cisco) }
            drain(); assertNull(result)
            reply!!.send(Activity.RESULT_CANCELED, null); drain(); assertEquals(false, result)
            result = null
            val cancelled = scope.launch { result = MessageHelper.stopVpnOwner(app, VpnEngine.Cisco) }
            drain(); cancelled.cancel(); drain()
            reply!!.send(Activity.RESULT_OK, null); drain(); assertNull(result)
        } finally { registration.close(); scope.cancel(); controller.destroy() }
    }

    @Test fun liveOwnerThatNeverAcknowledgesTimesOutAsFailure() {
        val priority = LogUtil::class.java.getDeclaredField("cachedMinPriority").apply { isAccessible = true }
        val previousPriority = priority.getInt(null); priority.setInt(null, android.util.Log.ERROR)
        val app = RuntimeEnvironment.getApplication()
        val controller = Robolectric.buildService(FixtureOwner::class.java).create()
        val endpoint = Messenger(Handler(Looper.getMainLooper()) { true })
        val registration = MessageHelper.registerVpnOwner(controller.get(), VpnEngine.OpenVpn, endpoint.binder)
        try {
            var result: Boolean? = null
            scope.launch { result = MessageHelper.stopVpnOwner(app, VpnEngine.OpenVpn) }
            drain(); assertNull(result)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(12))
            assertEquals(false, result)
        } finally { registration.close(); scope.cancel(); controller.destroy(); priority.setInt(null, previousPriority) }
    }
}

// Robolectric 4.17's default ShadowResultReceiver.send calls onReceiveResult directly,
// bypassing the binder in a parcelled framework endpoint. Use Android's implementation;
// remove this shadow when Robolectric models ResultReceiver's remote send path itself.
@Implements(ResultReceiver::class)
class BinderResultReceiverShadow
