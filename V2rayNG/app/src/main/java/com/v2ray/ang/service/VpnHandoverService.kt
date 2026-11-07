package com.v2ray.ang.service

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.os.*
import androidx.core.os.BundleCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.VpnEngine
import com.v2ray.ang.core.VpnHandoverQueue
import com.v2ray.ang.helper.MessageHelper
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.*

/** Bound, main-process owner serializes handovers from UI, tile and daemon callers.
 * Binding never starts an inactive VPN or an unrelated foreground service.
 */
class VpnHandoverService : Service() {
    private val owner = SupervisorJob()
    private val scope = CoroutineScope(owner + Dispatchers.Main.immediate)
    private val queue = VpnHandoverQueue()
    private val messenger = Messenger(Handler(Looper.getMainLooper()) { message ->
        val reply = BundleCompat.getParcelable(message.data, "stopReply", ResultReceiver::class.java)
        val target = VpnEngine.entries.getOrNull(message.arg1)
        val stopOnly = message.what == 2
        val token = queue.reserve()
        scope.launch {
            var success = false
            var replied = false
            try {
                if (target != null && stopOnly) success = queue.stop(token, target) { MessageHelper.stopVpnOwner(this@VpnHandoverService, it) }
                else if (target != null) success = queue.prepare(token, target,
                    stop = { MessageHelper.stopVpnOwner(this@VpnHandoverService, it) }, dispatch = {
                        val dispatched = CompletableDeferred<Unit>()
                        val receipt = object : ResultReceiver(Handler(Looper.getMainLooper())) {
                            override fun onReceiveResult(code: Int, data: Bundle?) { dispatched.complete(Unit) }
                        }
                        replied = true
                        reply?.send(Activity.RESULT_OK, Bundle().apply {
                            putParcelable("dispatchReceipt", MessageHelper.frameworkReceiver(receipt))
                            putLong("dispatchDeadline", SystemClock.elapsedRealtime() + 8_000L)
                        })
                        withTimeoutOrNull(8_000L) { dispatched.await() }
                    })
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                LogUtil.e(AppConfig.TAG, "VPN phase=handover operation=stop target=$target", IllegalStateException(error.javaClass.simpleName))
            } finally {
                if (!replied) reply?.send(if (success) Activity.RESULT_OK else Activity.RESULT_CANCELED, null)
            }
        }
        true
    })
    override fun onBind(intent: Intent?): IBinder = messenger.binder
    override fun onDestroy() {
        queue.reserve()
        owner.cancel()
        super.onDestroy()
    }
}
