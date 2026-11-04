package com.v2ray.ang.ui.protocols

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.util.LogUtil
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.ConfigParser
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VpnStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.StringReader
import java.io.ByteArrayOutputStream
import app.openconnect.core.EmbeddedRuntime as CiscoRuntime

internal data class OpenVpnRow(val guid: String, val name: String)
internal data class ProtocolsState(
    val busy: Boolean = true, val openVpnReady: Boolean = false, val ciscoReady: Boolean = false,
    val vpnMode: Boolean = false, val profiles: List<OpenVpnRow> = emptyList(),
    val status: Int = R.string.protocol_state_unknown, val connected: Boolean = false,
    val received: Long? = null, val sent: Long? = null, val message: Int? = null
)

/** UI observes the upstream OpenVPN status IPC; it never queries Xray's native core. */
class ProtocolsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val _state = MutableStateFlow(ProtocolsState())
    internal val state = _state.asStateFlow()
    private val imports = Mutex()
    private val initialized = CompletableDeferred<Unit>()
    private var statusClient: OpenVpnStatusClient? = null

    init {
        viewModelScope.launch {
            try {
                val ready = withContext(Dispatchers.IO) {
                    val openVpn = initialize("openvpn") { de.blinkt.openvpn.core.EmbeddedRuntime.initialize(app) }
                    val cisco = initialize("openconnect") { CiscoRuntime.initialize(app) }
                    Triple(openVpn, cisco, SettingsManager.isVpnMode())
                }
                _state.update { it.copy(busy = false, openVpnReady = ready.first, ciscoReady = ready.second, vpnMode = ready.third) }
                if (ready.first) {
                    statusClient = OpenVpnStatusClient(app, { level ->
                        val (resource, connected) = openVpnUiStatus(level)
                        _state.update { it.copy(status = resource, connected = connected,
                            received = if (connected) it.received else null, sent = if (connected) it.sent else null) }
                    }, { received, sent ->
                        _state.update { if (it.connected) it.copy(received = received, sent = sent) else it }
                    }).also { it.start() }
                }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (e: Exception) {
                report("initialize", e)
                _state.update { it.copy(busy = false, message = R.string.protocol_unavailable) }
            } finally { initialized.complete(Unit) }
            if (_state.value.openVpnReady) reload()
        }
    }

    private fun initialize(engine: String, action: () -> Unit): Boolean = try {
        action(); true
    } catch (e: Exception) {
        report(engine, e); false
    } catch (e: LinkageError) {
        report(engine, e); false
    }

    internal fun reload() {
        viewModelScope.launch {
            initialized.await()
            statusClient?.refresh()
            imports.withLock {
                if (!_state.value.openVpnReady) return@withLock
                try {
                    val profiles = withContext(Dispatchers.IO) {
                        ProfileManager.getInstance(app).profiles.map { OpenVpnRow(it.getUUIDString(), it.getName()) }.sortedBy { it.name }
                    }
                    _state.update { it.copy(profiles = profiles) }
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (e: Exception) {
                    report("reload-openvpn", e)
                    _state.update { it.copy(message = R.string.protocol_unavailable) }
                }
            }
        }
    }

    internal fun importProfile(uri: Uri) {
        viewModelScope.launch {
            imports.withLock {
                initialized.await()
                _state.update { it.copy(busy = true, message = null) }
                try {
                    withContext(Dispatchers.IO) {
                        check(_state.value.openVpnReady)
                        val name = app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                            if (it.moveToFirst()) it.getString(0) else null
                        } ?: throw IllegalArgumentException()
                        val bytes = app.contentResolver.openInputStream(uri)?.use { input ->
                            val output = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                require(output.size() + count <= OpenVpnImportPolicy.MAX_BYTES)
                                output.write(buffer, 0, count)
                            }
                            output.toByteArray()
                        }
                            ?: throw IllegalArgumentException()
                        val text = OpenVpnImportPolicy.decode(name, bytes)
                        val parser = ConfigParser()
                        parser.parseConfig(StringReader(text))
                        val profile = parser.convertProfile()
                        profile.mName = name.substringBeforeLast('.').take(100)
                        require(profile.checkProfile(app) == de.blinkt.openvpn.R.string.no_error_found)
                        val manager = ProfileManager.getInstance(app)
                        try {
                            ProfileManager.saveProfile(app, profile)
                            manager.addProfile(profile)
                            manager.saveProfileList(app)
                        } catch (e: Exception) {
                            runCatching { manager.removeProfile(app, profile) }.onFailure { report("rollback-openvpn", it) }
                            app.deleteFile(profile.getUUIDString() + ".vp")
                            app.deleteFile(profile.getUUIDString() + ".cp")
                            throw e
                        }
                    }
                    reload()
                    _state.update { it.copy(message = R.string.protocol_import_done) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    report("import-openvpn", e)
                    _state.update { it.copy(message = R.string.protocol_import_failed) }
                } finally {
                    _state.update { it.copy(busy = false) }
                }
            }
        }
    }

    private fun report(phase: String, e: Throwable) {
        val safe = IllegalStateException(e.javaClass.simpleName).apply { stackTrace = e.stackTrace }
        LogUtil.e(AppConfig.TAG, "Embedded VPN phase=$phase", safe)
    }
    override fun onCleared() {
        statusClient?.close()
        statusClient = null
        super.onCleared()
    }
}
