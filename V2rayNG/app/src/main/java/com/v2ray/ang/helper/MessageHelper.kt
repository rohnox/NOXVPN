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
            target.ordinal, 40_000L, result = onReady)
    }

    internal fun stopVpnHandover(ctx: Context, target: VpnEngine) {
        boundResult(ctx.applicationContext, Intent(ctx, VpnHandoverService::class.java),
            target.ordinal, 40_000L, command = 2) { success ->
            if (!success) LogUtil.e(AppConfig.TAG, "VPN phase=handover operation=user-stop target=$target", IllegalStateException("Stop not acknowledged"))
        }
    }

    /** Registered only by a live service owner, in that owner's process and lifecycle. */
    internal fun registerVpnOwner(service: android.app.Service, engine: VpnEngine, endpoint: IBinder): AutoCloseable {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Initial OK means absent. A live owner claims the request and replies separately
                // only when teardown finishes; the ordered broadcast itself never blocks on JNI.
                resultCode = Activity.RESULT_CANCELED
                val reply = intent?.let { androidx.core.content.IntentCompat.getParcelableExtra(it, "stopReply", ResultReceiver::class.java) }
                try {
                    Messenger(endpoint).send(Message.obtain().apply { what = 1; data = Bundle().apply { putParcelable("stopReply", reply) } })
                } catch (error: Exception) {
                    LogUtil.e(AppConfig.TAG, "VPN phase=handover operation=owner-dispatch component=$engine", IllegalStateException(error.javaClass.simpleName))
                    reply?.send(Activity.RESULT_CANCELED, null)
                }
            }
        }
        ContextCompat.registerReceiver(service, receiver, android.content.IntentFilter(ownerStopAction(service, engine)), ContextCompat.RECEIVER_NOT_EXPORTED)
        return AutoCloseable { service.unregisterReceiver(receiver) }
    }

    private fun ownerStopAction(ctx: Context, engine: VpnEngine) = ctx.packageName + ".STOP_OWNER." + engine.name

    internal suspend fun stopVpnOwner(ctx: Context, engine: VpnEngine): Boolean = withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { continuation ->
            val handler = Handler(Looper.getMainLooper())
            val timeout = Runnable {
                LogUtil.e(AppConfig.TAG, "VPN phase=handover operation=owner-timeout component=$engine", IllegalStateException("Owner stop not acknowledged"))
                if (continuation.isActive) continuation.resume(false)
            }
            fun finish(success: Boolean) {
                handler.removeCallbacks(timeout)
                if (continuation.isActive) continuation.resume(success)
            }
            val reply = object : ResultReceiver(handler) {
                override fun onReceiveResult(code: Int, data: Bundle?) = finish(code == Activity.RESULT_OK)
            }
            val delivered = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (resultCode == Activity.RESULT_OK) finish(true)
                    // Otherwise the live owner owes a native-cleanup reply, not a bind callback.
                }
            }
            handler.postDelayed(timeout, 12_000L)
            continuation.invokeOnCancellation { handler.removeCallbacks(timeout) }
            try {
                ctx.sendOrderedBroadcast(Intent(ownerStopAction(ctx, engine)).setPackage(AppConfig.ANG_PACKAGE)
                    .putExtra("stopReply", reply), null, delivered, handler, Activity.RESULT_OK, null, null)
            } catch (error: Exception) {
                LogUtil.e(AppConfig.TAG, "VPN phase=handover operation=owner-stop component=$engine", IllegalStateException(error.javaClass.simpleName))
                finish(false)
            }
        }
    }

    /** Only the lightweight coordinator is bound. VPN owners use the lifecycle-owned stop channel. */
    private fun boundResult(ctx: Context, intent: Intent, target: Int, timeout: Long,
                            command: Int = 1, result: (Boolean) -> Unit): AutoCloseable {
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
                if (bound) {
                    try { ctx.unbindService(connection) }
                    catch (error: IllegalArgumentException) {
                        LogUtil.e(AppConfig.TAG, "VPN phase=handover operation=coordinator-unbind", IllegalStateException(error.javaClass.simpleName))
                    }
                    bound = false
                }
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
            val connected = ctx.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            // Android requires unbind even when bindService returns false.
            bound = true
            if (!connected) complete(false)
        } catch (error: Exception) {
            LogUtil.e(AppConfig.TAG, "VPN phase=handover operation=bind component=${intent.component?.className}", IllegalStateException(error.javaClass.simpleName))
            complete(false)
        }
        return AutoCloseable { complete(false) }
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
