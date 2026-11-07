package com.v2ray.ang.service

import android.app.Service
import android.app.PendingIntent
import android.content.Intent
import android.os.IBinder
import android.os.ResultReceiver
import androidx.core.app.NotificationCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.wireGuardCommand
import com.v2ray.ang.core.WireGuardCommand
import com.v2ray.ang.core.WireGuardRuntime
import com.v2ray.ang.core.WireGuardState
import com.v2ray.ang.enums.NotificationChannelType
import com.v2ray.ang.helper.NotificationHelper
import com.v2ray.ang.ui.protocols.ProtocolSubscriptionRepository
import com.v2ray.ang.ui.protocols.ProfileProtocol
import com.v2ray.ang.util.LogUtil
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicLong

/** Foreground owner for the official userspace backend; the library retains Android VpnService ownership. */
class WireGuardControllerService : Service() {
    private val owner = SupervisorJob()
    private val scope = CoroutineScope(owner + Dispatchers.IO)
    private val generation = AtomicLong()
    private var command: Job? = null
    // Keep every unfinished setup: cancelling a queued command must not hide an older native setup.
    private val setupJobs = mutableListOf<Job>()
    private var backend: com.wireguard.android.backend.Backend? = null
    private var stopping = false
    private var completedStop: Boolean? = null
    private val stopReplies = mutableListOf<ResultReceiver>()
    @Volatile private var tunnel: Tunnel? = null
    private var handoverRegistration: AutoCloseable? = null
    private val handover = android.os.Messenger(android.os.Handler(android.os.Looper.getMainLooper()) { message ->
        requestStop(androidx.core.os.BundleCompat.getParcelable(message.data, "stopReply", ResultReceiver::class.java))
        true
    })
    override fun onBind(intent: Intent?): IBinder? =
        if (intent?.action == packageName + ".STOP_FOR_HANDOVER") handover.binder else null
    override fun onCreate() {
        super.onCreate()
        handoverRegistration = com.v2ray.ang.helper.MessageHelper.registerVpnOwner(this, com.v2ray.ang.core.VpnEngine.WireGuard, handover.binder)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val stop = PendingIntent.getService(this, 0, Intent(this, WireGuardControllerService::class.java).setAction(STOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        NotificationHelper.startForeground(this, NotificationChannelType.WIREGUARD, getString(R.string.protocol_wireguard), getString(if (intent?.action == STOP || stopping) R.string.protocol_stop else R.string.protocol_wg_connecting),
            NotificationCompat.Action(0, getString(R.string.protocol_stop), stop))
        val id = intent?.getStringExtra(PROFILE)
        val decision = wireGuardCommand(id, intent?.action == STOP, WireGuardRuntime.state.value, stopping)
        if (decision == WireGuardCommand.Stop) {
            val reply = intent?.let { androidx.core.content.IntentCompat.getParcelableExtra(it, STOP_REPLY, ResultReceiver::class.java) }
            requestStop(reply)
            return START_NOT_STICKY
        }
        if (decision == WireGuardCommand.Ignore) return START_NOT_STICKY
        val state = WireGuardRuntime.state.value
        if (decision == WireGuardCommand.Keep) {
            NotificationHelper.updateNotification(NotificationChannelType.WIREGUARD, this, getString(R.string.protocol_wireguard), getString(if (state.phase == WireGuardState.Phase.Up) R.string.top_online else R.string.protocol_wg_connecting))
            return START_NOT_STICKY
        }
        val token = generation.incrementAndGet()
        setupJobs.removeAll { it.isCompleted }
        val previous = setupJobs.toList(); previous.forEach { it.cancel() }
        command = scope.launch {
            previous.joinAll()
            try {
                check(down()); ensureActive()
                WireGuardRuntime.mutableState.value = WireGuardState(id, WireGuardState.Phase.Connecting)
                require(id != null)
                val profile = ProtocolSubscriptionRepository.read().flatMap { it.profiles }.firstOrNull { it.id == id && it.protocol == ProfileProtocol.WireGuard } ?: error("Profile unavailable")
                val config = Config.parse(profile.config.byteInputStream())
                val engine = backend ?: GoBackend(applicationContext).also { backend = it }
                val active = object : Tunnel {
                    override fun getName() = "sp" + id.replace("-", "").take(12)
                    override fun onStateChange(newState: Tunnel.State) {
                        if (generation.get() != token || tunnel !== this) return
                        WireGuardRuntime.mutableState.value = WireGuardState(id, if (newState == Tunnel.State.UP) WireGuardState.Phase.Up else WireGuardState.Phase.Down)
                        if (newState == Tunnel.State.DOWN) stopSelf()
                    }
                }
                tunnel = active
                ensureActive()
                engine.setState(active, Tunnel.State.UP, config)
                ensureActive()
                if (generation.get() != token) throw CancellationException()
                NotificationHelper.updateNotification(NotificationChannelType.WIREGUARD, this@WireGuardControllerService, getString(R.string.protocol_wireguard), getString(R.string.top_online))
            } catch (cancelled: CancellationException) { withContext(NonCancellable) { down() }; throw cancelled }
            catch (error: Throwable) {
                down()
                if (generation.get() == token) {
                    WireGuardRuntime.mutableState.value = WireGuardState(id, WireGuardState.Phase.Failed)
                    LogUtil.e(AppConfig.TAG, "WireGuard mode=VPN operation=start profile=$id", IllegalStateException(error.javaClass.simpleName))
                    stopSelf()
                }
            }
        }
        setupJobs += requireNotNull(command)
        return START_NOT_STICKY
    }
    private fun requestStop(reply: ResultReceiver?) {
        if (stopping && completedStop == true) {
            reply?.send(android.app.Activity.RESULT_OK, null)
            return
        }
        if (reply != null) stopReplies += reply
        if (stopping) return
        stopping = true
        completedStop = null
        generation.incrementAndGet()
        val previous = setupJobs.toList(); previous.forEach { it.cancel() }
        command = scope.launch {
            var stopped = false
            try {
                previous.joinAll()
                stopped = down()
                if (stopped) WireGuardRuntime.mutableState.value = WireGuardState()
            } finally {
                withContext(NonCancellable + Dispatchers.Main.immediate) {
                    completedStop = stopped
                    if (!stopped) stopping = false
                    replyToStops(stopped)
                    // A failed DOWN must keep its owner reachable for retry, never masquerade as absent.
                    if (stopped) stopSelf()
                }
            }
        }
    }
    private fun replyToStops(stopped: Boolean) {
        val replies = stopReplies.toList(); stopReplies.clear()
        for (reply in replies) try { reply.send(if (stopped) android.app.Activity.RESULT_OK else android.app.Activity.RESULT_CANCELED, null) }
        catch (error: Exception) { LogUtil.e(AppConfig.TAG, "WireGuard mode=VPN phase=handover operation=stop-reply", IllegalStateException(error.javaClass.simpleName)) }
    }
    private fun down(): Boolean {
        val active = tunnel; tunnel = null
        if (active != null) try { check(requireNotNull(backend).setState(active, Tunnel.State.DOWN, null) == Tunnel.State.DOWN) }
        catch (error: Exception) {
            tunnel = active
            WireGuardRuntime.mutableState.value = WireGuardState(WireGuardRuntime.state.value.profileId, WireGuardState.Phase.Failed)
            LogUtil.e(AppConfig.TAG, "WireGuard mode=VPN operation=stop profile=${WireGuardRuntime.state.value.profileId}", IllegalStateException(error.javaClass.simpleName))
            return false
        }
        if (WireGuardRuntime.state.value.phase != WireGuardState.Phase.Failed) WireGuardRuntime.mutableState.value = WireGuardState()
        return true
    }
    override fun onDestroy() {
        handoverRegistration?.close(); handoverRegistration = null
        stopping = true
        generation.incrementAndGet()
        val previous = (setupJobs + listOfNotNull(command)).distinct(); previous.forEach { it.cancel() }
        // Teardown is the last owner job; it can only release resources and never reconnect.
        scope.launch { previous.joinAll(); down(); withContext(Dispatchers.Main.immediate) { replyToStops(false) }; owner.cancel() }
        NotificationHelper.stopForeground(this)
        super.onDestroy()
    }
    companion object { const val PROFILE = "wireguard_profile"; const val STOP = "wireguard_stop"; const val STOP_REPLY = "wireguard_stop_reply" }
}
