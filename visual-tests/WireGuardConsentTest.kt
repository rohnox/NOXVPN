package com.v2ray.ang.ui.protocols

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import com.v2ray.ang.AppConfig
import com.v2ray.ang.service.WireGuardControllerService
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], shadows = [BinderResultReceiverShadow::class])
class WireGuardConsentTest {
    class ConsentActivity : ComponentActivity() {
        internal lateinit var host: ProtocolsHost
        override fun onCreate(state: Bundle?) { super.onCreate(state); host = ProtocolsHost(this) { supplied } }
        companion object { lateinit var supplied: ProtocolsViewModel }
    }
    private fun fixture(): ProtocolsViewModel {
        val profile = SubscriptionProfile("5f5c7c2b-77d3-4393-85d1-1dfac11ef8c7", "Fixture", ProfileProtocol.WireGuard)
        val record = ProtocolSubscription("fixture", "Example", "", 0, listOf(profile))
        val model = mock<ProtocolsViewModel>()
        whenever(model.state).thenReturn(MutableStateFlow(ProtocolsState(busy = false, vpnMode = true, subscriptions = listOf(record))))
        whenever(model.consumeManagedLaunch()).thenReturn(ManagedLaunch(profile.id, ProfileProtocol.WireGuard))
        ConsentActivity.supplied = model
        return model
    }
    @Test fun consentCancellationDoesNotStopXrayOrStartWireguard() {
        fixture()
        val controller = Robolectric.buildActivity(ConsentActivity::class.java).setup()
        val activity = controller.get(); val app = activity.application
        shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        val before = shadowOf(app).broadcastIntents.size
        Mockito.mockStatic(VpnService::class.java).use { vpn ->
            vpn.`when`<Intent?> { VpnService.prepare(activity) }.thenReturn(Intent("fixture.VPN_CONSENT"))
            activity.host.onAction(ProtocolAction.LaunchManaged)
            assertEquals(before, shadowOf(app).broadcastIntents.size)
            val request = shadowOf(activity).nextStartedActivityForResult
            assertNotNull(request)
            activity.activityResultRegistry.dispatchResult(request.requestCode, Activity.RESULT_CANCELED, null)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(before, shadowOf(app).broadcastIntents.size)
            assertNull(shadowOf(app).nextStartedService)
        }
        controller.pause().stop().destroy()
    }
    @Test fun restoredConsentStartsOnlyTheExplicitProfileAfterConfirmation() {
        fixture()
        val controller = Robolectric.buildActivity(ConsentActivity::class.java).setup()
        val activity = controller.get(); val app = activity.application
        shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        var stopReply: android.os.ResultReceiver? = null
        val binder = android.os.Messenger(android.os.Handler(Looper.getMainLooper()) { message ->
            stopReply = androidx.core.os.BundleCompat.getParcelable(message.data, "stopReply", android.os.ResultReceiver::class.java)
            true
        }).binder
        shadowOf(app).setComponentNameAndServiceForBindService(android.content.ComponentName(app, com.v2ray.ang.service.VpnHandoverService::class.java), binder)
        Mockito.mockStatic(VpnService::class.java).use { vpn ->
            vpn.`when`<Intent?> { VpnService.prepare(activity) }.thenReturn(Intent("fixture.VPN_CONSENT"))
            activity.host.onAction(ProtocolAction.LaunchManaged)
            val request = shadowOf(activity).nextStartedActivityForResult
            assertNotNull(request)
            val bundle = Bundle(); controller.saveInstanceState(bundle).pause().stop().destroy()
            val restored = Robolectric.buildActivity(ConsentActivity::class.java).create(bundle).start().resume().visible()
            restored.get().activityResultRegistry.dispatchResult(request.requestCode, Activity.RESULT_OK, null)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(com.v2ray.ang.service.VpnHandoverService::class.java.name, shadowOf(app).nextStartedService.component!!.className)
            assertNotNull(stopReply)
            stopReply!!.send(Activity.RESULT_OK, null)
            shadowOf(Looper.getMainLooper()).idle()
            val intent = shadowOf(app).nextStartedService
            assertNotNull(intent)
            assertEquals(WireGuardControllerService::class.java.name, intent.component!!.className)
            assertEquals("5f5c7c2b-77d3-4393-85d1-1dfac11ef8c7", intent.getStringExtra(WireGuardControllerService.PROFILE))
            assertFalse(shadowOf(app).broadcastIntents.any { it.action == AppConfig.BROADCAST_ACTION_SERVICE })
            restored.pause().stop().destroy()
        }
    }
}
