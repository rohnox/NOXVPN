package com.v2ray.ang.helper

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.*
import androidx.core.content.ContextCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.SubscriptionUpdateMessage
import com.v2ray.ang.dto.TestServiceMessage
import com.v2ray.ang.service.CoreTestService
import com.v2ray.ang.service.CoreVpnService
import com.v2ray.ang.service.SubscriptionUpdateService
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.core.VpnEngine
import com.v2ray.ang.service.VpnHandoverService
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import java.io.Serializable

object MessageHelper {
    const val EXTRA_REQUEST_ID = "requestId"

    /** Messenger contract shared with embedded modules: what=1, stopReply=ResultReceiver.
     * RESULT_OK means native cleanup completed, not merely that a command was enqueued.
     */
    internal fun prepareVpnHandover(ctx: Context, target: VpnEngine, onReady: (Boolean) -> Unit) {
        boundResult(ctx.applicationContext, Intent(ctx, VpnHandoverService::class.java),
            Context.BIND_AUTO_CREATE, target.ordinal, 40_000L, false, result = onReady)
    }

    internal fun stopVpnHandover(ctx: Context, target: VpnEngine) {
        boundResult(ctx.applicationContext, Intent(ctx, VpnHandoverService::class.java),
            Context.BIND_AUTO_CREATE, target.ordinal, 40_000L, false, command = 2) { success ->
            if (!success) LogUtil.e(AppConfig.TAG, "VPN phase=handover operation=user-stop target=$target", IllegalStateException("Stop not acknowledged"))
        }
    }

    internal suspend fun stopVpnOwner(ctx: Context, engine: VpnEngine): Boolean = withContext(Dispatchers.Main.immediate) {
        val service = when (engine) {
            VpnEngine.Xray -> CoreVpnService::class.java
            VpnEngine.OpenVpn -> de.blinkt.openvpn.core.OpenVPNService::class.java
            VpnEngine.Cisco -> app.openconnect.core.OpenVpnService::class.java
            VpnEngine.WireGuard -> com.v2ray.ang.service.WireGuardControllerService::class.java
        }
        suspendCancellableCoroutine { continuation ->
            val request = boundResult(ctx, Intent(ctx, service).setAction(ctx.packageName + ".STOP_FOR_HANDOVER"),
                0, 0, 12_000L, true) { if (continuation.isActive) continuation.resume(it) }
            continuation.invokeOnCancellation { android.os.Handler(android.os.Looper.getMainLooper()).post { request.close() } }
        }
    }

    /** A bounded binding owns its timeout, reply and unbind; flag=0 never creates a VPN owner. */
    private fun boundResult(ctx: Context, intent: Intent, flags: Int, target: Int, timeout: Long,
                            absentIsStopped: Boolean, command: Int = 1, result: (Boolean) -> Unit): AutoCloseable {
        val handler = Handler(Looper.getMainLooper())
        var finished = false
        var bound = false
        lateinit var connection: ServiceConnection
        lateinit var timeoutTask: Runnable
        fun complete(success: Boolean, receipt: ResultReceiver? = null) {
            if (finished) return
            finished = true
            handler.removeCallbacks(timeoutTask)
            try { result(success) }
            catch (error: Exception) {
                LogUtil.e(AppConfig.TAG, "VPN phase=handover operation=dispatch-callback component=${intent.component?.className}", IllegalStateException(error.javaClass.simpleName))
            }
            finally {
                receipt?.send(Activity.RESULT_OK, null)
                if (bound) { ctx.unbindService(connection); bound = false }
            }
        }
        timeoutTask = Runnable {
            LogUtil.e(AppConfig.TAG, "VPN phase=handover operation=reply-timeout component=${intent.component?.className}", IllegalStateException("Stop acknowledgement timed out"))
            complete(false)
        }
        val receiver = object : ResultReceiver(handler) {
            override fun onReceiveResult(code: Int, data: Bundle?) {
                val receipt = data?.let { androidx.core.os.BundleCompat.getParcelable(it, "dispatchReceipt", ResultReceiver::class.java) }
                val timely = data == null || !data.containsKey("dispatchDeadline") || SystemClock.elapsedRealtime() < data.getLong("dispatchDeadline")
                complete(code == Activity.RESULT_OK && timely, receipt)
            }
        }
        connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                if (finished) return
                try {
                    Messenger(binder).send(Message.obtain().apply {
                        what = command; arg1 = target; data = Bundle().apply { putParcelable("stopReply", receiver) }
                    })
                } catch (error: Exception) {
                    LogUtil.e(AppConfig.TAG, "VPN phase=handover operation=stop-ipc component=${name?.className}", IllegalStateException(error.javaClass.simpleName))
                    complete(false)
                }
            }
            override fun onServiceDisconnected(name: ComponentName?) = complete(false)
            override fun onNullBinding(name: ComponentName?) = complete(false)
            override fun onBindingDied(name: ComponentName?) = complete(false)
        }
        handler.postDelayed(timeoutTask, timeout)
        try {
            bound = ctx.bindService(intent, connection, flags)
            if (!bound) complete(absentIsStopped)
        } catch (error: Exception) {
            LogUtil.e(AppConfig.TAG, "VPN phase=handover operation=bind component=${intent.component?.className}", IllegalStateException(error.javaClass.simpleName))
            complete(false)
        }
        return AutoCloseable { complete(false) }
    }

    /** The WireGuard service replies only after its backend teardown, including an already-stopped owner. */
    internal fun stopWireGuardForResult(ctx: Context, onStopped: (Boolean) -> Unit) {
        val receiver = object : android.os.ResultReceiver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: android.os.Bundle?) {
                onStopped(resultCode == Activity.RESULT_OK)
            }
        }
        try {
            ContextCompat.startForegroundService(ctx, Intent(ctx, com.v2ray.ang.service.WireGuardControllerService::class.java)
                .setAction(com.v2ray.ang.service.WireGuardControllerService.STOP)
                .putExtra(com.v2ray.ang.service.WireGuardControllerService.STOP_REPLY, receiver))
        } catch (error: Exception) {
            LogUtil.e(AppConfig.TAG, "WireGuard mode=VPN phase=handover operation=stop-dispatch", IllegalStateException(error.javaClass.simpleName))
            onStopped(false)
        }
    }

    /**
     * Sends a message to the service.
     *
     * @param ctx The context.
     * @param what The message identifier.
     * @param content The message content.
     */
    fun sendMsg2Service(ctx: Context, what: Int, content: Serializable) {
        sendMsg(ctx, AppConfig.BROADCAST_ACTION_SERVICE, what, content)
    }

    /**
     * Sends an ordered service message and reports whether a daemon receiver handled it.
     * With no running daemon, the initial canceled result reaches [onResult] unchanged.
     */
    internal fun sendMsg2ServiceForResult(
        ctx: Context,
        what: Int,
        content: Serializable,
        onResult: (handled: Boolean) -> Unit,
    ) {
        val resultReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                onResult(resultCode == Activity.RESULT_OK)
            }
        }
        try {
            ctx.sendOrderedBroadcast(
                messageIntent(AppConfig.BROADCAST_ACTION_SERVICE, what, content),
                null,
                resultReceiver,
                null,
                Activity.RESULT_CANCELED,
                null,
                null,
            )
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to send ordered message to service", e)
            onResult(false)
        }
    }

    /**
     * Sends a message to the UI.
     *
     * @param ctx The context.
     * @param what The message identifier.
     * @param content The message content.
     */
    fun sendMsg2UI(ctx: Context, what: Int, content: Serializable, requestId: String? = null) {
        sendMsg(ctx, AppConfig.BROADCAST_ACTION_ACTIVITY, what, content, requestId)
    }

    /**
     * Sends a message to the test service.
     *
     * @param ctx The context.
     * @param message The test service message containing key, subscriptionId, and serverGuids.
     */
    fun sendMsg2TestService(ctx: Context, message: TestServiceMessage, requestId: String? = null) {
        try {
            val intent = Intent()
            intent.component = ComponentName(ctx, CoreTestService::class.java)
            intent.putExtra("content", message)
            requestId?.let { intent.putExtra(EXTRA_REQUEST_ID, it) }
            when (message.key) {
                AppConfig.MSG_MEASURE_CONFIG_START -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(ctx, intent)
                    } else {
                        ctx.startService(intent)
                    }
                }

                AppConfig.MSG_MEASURE_CONFIG_CANCEL -> {
                    // Do not wake up service just to cancel; stop only if it is already running.
                    ctx.stopService(intent)
                }

                else -> {
                    ctx.startService(intent)
                }
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to send message to test service", e)
            if (message.key == AppConfig.MSG_MEASURE_CONFIG_START) {
                sendMsg2UI(ctx, AppConfig.MSG_MEASURE_CONFIG_CANCEL, "", requestId)
            }
        }
    }

    /**
     * Sends a message to the subscription service.
     *
     * @param ctx The context.
     * @param message The subscription service message containing key and subId.
     */
    fun sendMsg2SubscriptionService(ctx: Context, message: SubscriptionUpdateMessage) {
        try {
            val intent = Intent()
            intent.component = ComponentName(ctx, SubscriptionUpdateService::class.java)
            intent.putExtra("content", message)
            when (message.key) {
                AppConfig.MSG_SUB_UPDATE_START -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(ctx, intent)
                    } else {
                        ctx.startService(intent)
                    }
                }

                AppConfig.MSG_SUB_UPDATE_CANCEL -> {
                    ctx.stopService(intent)
                }

                else -> {
                    ctx.startService(intent)
                }
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to send message to subscription service", e)
        }
    }

    /**
     * Sends a message with the specified action.
     *
     * @param ctx The context.
     * @param action The action string.
     * @param what The message identifier.
     * @param content The message content.
     */
    private fun sendMsg(ctx: Context, action: String, what: Int, content: Serializable, requestId: String? = null) {
        try {
            ctx.sendBroadcast(messageIntent(action, what, content).apply {
                requestId?.let { putExtra(EXTRA_REQUEST_ID, it) }
            })
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to send message with action: $action", e)
        }
    }

    private fun messageIntent(action: String, what: Int, content: Serializable): Intent =
        Intent(action).apply {
            `package` = AppConfig.ANG_PACKAGE
            putExtra("key", what)
            putExtra("content", content)
        }
}
