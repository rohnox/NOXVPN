package com.v2ray.ang.service

import android.app.Service
import android.app.PendingIntent
import android.content.Intent
import android.os.IBinder
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
    private var backend: GoBackend? = null
    @Volatile private var tunnel: Tunnel? = null
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val stop = PendingIntent.getService(this, 0, Intent(this, WireGuardControllerService::class.java).setAction(STOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        NotificationHelper.startForeground(this, NotificationChannelType.WIREGUARD, getString(R.string.protocol_wireguard), getString(R.string.protocol_wg_connecting),
            NotificationCompat.Action(0, getString(R.string.protocol_stop), stop))
        val id = intent?.getStringExtra(PROFILE)
        val decision = wireGuardCommand(id, intent?.action == STOP, WireGuardRuntime.state.value)
        if (decision == WireGuardCommand.Stop) {
            val token = generation.incrementAndGet(); val previous = command; previous?.cancel()
            command = scope.launch { previous?.join(); down(); if (generation.get() == token) stopSelf() }
            return START_NOT_STICKY
        }
        val state = WireGuardRuntime.state.value
        if (decision == WireGuardCommand.Keep) {
            NotificationHelper.updateNotification(NotificationChannelType.WIREGUARD, this, getString(R.string.protocol_wireguard), getString(if (state.phase == WireGuardState.Phase.Up) R.string.top_online else R.string.protocol_wg_connecting))
            return START_NOT_STICKY
        }
        val token = generation.incrementAndGet(); val previous = command; previous?.cancel()
        command = scope.launch {
            previous?.join()
            try {
                down(); ensureActive()
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
        return START_NOT_STICKY
    }
    private fun down() {
        val active = tunnel; tunnel = null
        if (active != null) try { backend?.setState(active, Tunnel.State.DOWN, null) }
        catch (error: Exception) { LogUtil.e(AppConfig.TAG, "WireGuard mode=VPN operation=stop", IllegalStateException(error.javaClass.simpleName)) }
        if (WireGuardRuntime.state.value.phase != WireGuardState.Phase.Failed) WireGuardRuntime.mutableState.value = WireGuardState()
    }
    override fun onDestroy() {
        generation.incrementAndGet(); val previous = command; previous?.cancel()
        // Teardown is the last owner job; it can only release resources and never reconnect.
        scope.launch { previous?.join(); down(); owner.cancel() }
        NotificationHelper.stopForeground(this)
        super.onDestroy()
    }
    companion object { const val PROFILE = "wireguard_profile"; const val STOP = "wireguard_stop" }
}
