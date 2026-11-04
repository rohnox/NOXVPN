package com.v2ray.ang.ui.protocols

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil
import de.blinkt.openvpn.core.*
import kotlinx.coroutines.*

/** Screen-model-owned status IPC, independent of UI-process VpnStatus and log-cache replay. */
internal class OpenVpnStatusClient(
    context: Context,
    private val onState: (ConnectionStatus?) -> Unit,
    private val onTraffic: (Long, Long) -> Unit,
) : AutoCloseable {
    private val app = context.applicationContext
    private val owner = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val reader = StatusLogReader()
    @Volatile private var service: IServiceStatus? = null
    @Volatile private var closed = false
    private var bound = false
    private val callback = object : IStatusCallbacks.Stub() {
        override fun updateStateString(state: String?, message: String?, resId: Int, level: ConnectionStatus?, intent: Intent?) {
            if (!closed) onState(level)
        }
        override fun updateByteCount(received: Long, sent: Long) { if (!closed) onTraffic(received, sent) }
        override fun newLogItem(item: LogItem?) = Unit
        override fun connectedVPN(uuid: String?) = Unit
        override fun notifyProfileVersionChanged(uuid: String?, version: Int) = Unit
    }
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IServiceStatus.Stub.asInterface(binder)
            refresh()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            reader.cancel()
            if (!closed) onState(null)
        }
        override fun onBindingDied(name: ComponentName?) {
            if (bound) app.unbindService(this)
            bound = false
            service = null
            if (!closed) start()
        }
        override fun onNullBinding(name: ComponentName?) {
            if (bound) app.unbindService(this)
            bound = false
            if (!closed) onState(null)
        }
    }
    fun start() {
        if (closed || bound) return
        try {
            bound = app.bindService(Intent(app, OpenVPNStatusService::class.java), connection, Context.BIND_AUTO_CREATE)
            if (!bound) onState(null)
        } catch (error: RuntimeException) { report(error); onState(null) }
    }
    fun refresh() {
        val target = service ?: return
        if (closed) return
        // registerStatusCallback supplies a fresh daemon snapshot before returning the log pipe.
        // Draining the bounded cache pipe stays off main and is not required for connection state.
        reader.start({
            val pipe = target.registerStatusCallback(callback)
            if (closed || service !== target) {
                target.unregisterStatusCallback(callback)
                pipe.close()
                throw java.io.IOException("Status owner closed")
            }
            android.os.ParcelFileDescriptor.AutoCloseInputStream(pipe)
        }, { _ -> }, { error ->
            if (!closed) report(error)
        })
    }
    override fun close() {
        closed = true
        val target = service
        service = null
        if (bound) { app.unbindService(connection); bound = false }
        owner.launch {
            try { target?.unregisterStatusCallback(callback) }
            catch (error: Exception) { report(error) }
            finally { reader.close(); owner.cancel() }
        }
    }
    private fun report(error: Exception) {
        val safe = IllegalStateException(error.javaClass.simpleName).apply { stackTrace = error.stackTrace }
        LogUtil.e(AppConfig.TAG, "OpenVPN mode=VPN phase=status-IPC operation=observe", safe)
    }
}
