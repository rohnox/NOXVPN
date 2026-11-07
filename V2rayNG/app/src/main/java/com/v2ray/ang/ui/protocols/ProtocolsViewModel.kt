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
import kotlinx.coroutines.ensureActive
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
import app.openconnect.core.ProfileManager as CiscoProfiles

internal data class OpenVpnRow(val guid: String, val name: String)
internal data class SubscriptionEditor(val id: String? = null, val title: String = "", val link: String = "")
internal data class ManagedLaunch(val id: String, val protocol: ProfileProtocol)
internal data class ProtocolsState(
    val busy: Boolean = true, val openVpnReady: Boolean = false, val ciscoReady: Boolean = false,
    val vpnMode: Boolean = false, val profiles: List<OpenVpnRow> = emptyList(),
    val status: Int = R.string.protocol_state_unknown, val connected: Boolean = false,
    val activeProfileId: String? = null,
    val received: Long? = null, val sent: Long? = null, val message: Int? = null,
    val subscriptions: List<ProtocolSubscription> = emptyList(), val selectedSubscriptionId: String? = null,
    val editor: SubscriptionEditor? = null, val launch: ManagedLaunch? = null,
    val wireguard: com.v2ray.ang.core.WireGuardState = com.v2ray.ang.core.WireGuardState(),
    val ciscoActiveId: String? = null
)

/** UI observes the upstream OpenVPN status IPC; it never queries Xray's native core. */
class ProtocolsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val _state = MutableStateFlow(ProtocolsState())
    internal val state = _state.asStateFlow()
    private val imports = Mutex()
    private val initialized = CompletableDeferred<Unit>()
    private var statusClient: OpenVpnStatusClient? = null
    private var subscriptionJob: kotlinx.coroutines.Job? = null
    private var launchJob: kotlinx.coroutines.Job? = null
    private var reloadJob: kotlinx.coroutines.Job? = null
    private val ciscoStatusListener = CiscoRuntime.ConnectionStatusListener { id -> _state.update { it.copy(ciscoActiveId = id) } }
    private val wireguardJob = viewModelScope.launch {
        com.v2ray.ang.core.WireGuardRuntime.state.collect { value -> _state.update { it.copy(wireguard = value) } }
    }

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
                        _state.update { it.copy(status = resource, connected = connected, activeProfileId = null,
                            received = if (connected) it.received else null, sent = if (connected) it.sent else null) }
                    }, { received, sent ->
                        _state.update { if (it.connected) it.copy(received = received, sent = sent) else it }
                    }, { uuid -> _state.update { it.copy(activeProfileId = uuid) } }).also { it.start() }
                }
                if (ready.second) CiscoRuntime.addStatusListener(ciscoStatusListener)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (e: Exception) {
                report("initialize", e)
                _state.update { it.copy(busy = false, message = R.string.protocol_unavailable) }
            } finally { initialized.complete(Unit) }
            reload()
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
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch {
            initialized.await()
            statusClient?.refresh()
            imports.withLock {
                try {
                    val subscriptions = withContext(Dispatchers.IO) { ProtocolSubscriptionRepository.read() }
                    val selected = _state.value.selectedSubscriptionId?.takeIf { id -> subscriptions.any { it.id == id } }
                        ?: withContext(Dispatchers.IO) { com.v2ray.ang.handler.MmkvManager.decodeSettingsString("protocol_selected_subscription_v1") }?.takeIf { id -> subscriptions.any { it.id == id } }
                        ?: subscriptions.firstOrNull()?.id
                    val vpnMode = withContext(Dispatchers.IO) { SettingsManager.isVpnMode() }
                    _state.update { it.copy(subscriptions = subscriptions, selectedSubscriptionId = selected, ciscoActiveId = if (it.ciscoReady) CiscoRuntime.getConnectedProfileId() else null, vpnMode = vpnMode) }
                    val profiles = if (!_state.value.openVpnReady) emptyList() else withContext(Dispatchers.IO) {
                        val managed = subscriptions.flatMap { it.profiles }.map { it.id }.toSet()
                        ProfileManager.getInstance(app).apply { refreshVPNList(app) }.profiles.filterNot { it.getUUIDString() in managed }.map { OpenVpnRow(it.getUUIDString(), it.getName()) }.sortedBy { it.name }
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

    internal fun deleteProfile(guid: String) {
        viewModelScope.launch {
            initialized.await()
            imports.withLock {
                if (!canHandleProtocolAction(_state.value, ProtocolAction.Delete(guid))) {
                    _state.update { it.copy(message = R.string.protocol_delete_stop_first) }
                    return@withLock
                }
                _state.update { it.copy(busy = true, message = null) }
                try {
                    val rows = withContext(Dispatchers.IO) {
                        val manager = ProfileManager.getInstance(app)
                        val profile = ProfileManager.get(app, guid) ?: error("Profile unavailable")
                        manager.removeProfile(app, profile)
                        check(listOf(".vp", ".cp", ".cpold").none { app.getFileStreamPath(guid + it).exists() })
                        manager.refreshVPNList(app)
                        val managed = _state.value.subscriptions.flatMap { it.profiles }.map { it.id }.toSet()
                        manager.profiles.filterNot { it.getUUIDString() in managed }.map { OpenVpnRow(it.getUUIDString(), it.getName()) }.sortedBy { it.name }
                    }
                    _state.update { it.copy(profiles = rows, message = R.string.protocol_delete_done) }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) {
                    report("delete-openvpn", error)
                    _state.update { it.copy(message = R.string.protocol_delete_failed) }
                    reload()
                } finally { _state.update { it.copy(busy = false) } }
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
                            check(manager.reloadStoredProfile(app, profile.getUUIDString())?.mVersion == profile.mVersion)
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


    internal fun editSubscription(id: String?) {
        if (_state.value.busy) return
        val item = _state.value.subscriptions.firstOrNull { it.id == id }
        _state.update { it.copy(editor = SubscriptionEditor(item?.id, item?.title.orEmpty(), item?.url.orEmpty()), message = null) }
    }
    internal fun changeEditor(title: String? = null, link: String? = null) {
        _state.update { state -> state.copy(editor = state.editor?.let { it.copy(title = title ?: it.title, link = link ?: it.link) }) }
    }
    internal fun cancelEditor() { subscriptionJob?.cancel(); launchJob?.cancel(); _state.update { it.copy(editor = null, launch = null) } }
    internal fun pasteEditorLink() {
        val manager = app.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val text = manager.primaryClip?.getItemAt(0)?.coerceToText(app)?.toString().orEmpty()
        changeEditor(link = text.take(4096))
    }
    internal fun saveEditor() { _state.value.editor?.let { updateSubscription(it.id, it.title, it.link) } }
    internal fun refreshSubscription(id: String) {
        _state.value.subscriptions.firstOrNull { it.id == id && it.url.isNotBlank() }?.let { updateSubscription(it.id, it.title, it.url) }
    }
    private fun updateSubscription(id: String?, title: String, link: String) {
        if (subscriptionJob?.isActive == true) return
        val stableId = id ?: java.util.UUID.randomUUID().toString()
        subscriptionJob = viewModelScope.launch {
            initialized.await()
            _state.update { it.copy(busy = true, message = null) }
            try {
                ProtocolSubscriptionRepository.mutation.withLock {
                    val records = withContext(Dispatchers.IO) { ProtocolSubscriptionRepository.read() }
                    val old = records.firstOrNull { it.id == stableId }
                    check(id == null || old != null)
                    val replacement = ProtocolSubscriptionRepository.replace(app, records, stableId, title, link) { previous, next ->
                        withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) {
                            checkMutable(previous)
                            SubscriptionProfileCache.commit(app, previous, next)
                        }
                    }
                    _state.update { it.copy(subscriptions = records.filterNot { item -> item.id == stableId } + replacement, selectedSubscriptionId = stableId, editor = null, message = R.string.protocol_sub_done) }
                }
                reload()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { report("subscription-update profile=$stableId", error); _state.update { it.copy(message = R.string.protocol_sub_failed) } }
            finally { _state.update { it.copy(busy = false) } }
        }
    }
    private fun checkMutable(record: ProtocolSubscription?) {
        val ids = record?.profiles.orEmpty().map { it.id }.toSet()
        check(_state.value.activeProfileId !in ids || !openVpnSessionActive(_state.value))
        check(com.v2ray.ang.core.WireGuardRuntime.state.value.let { it.profileId !in ids || it.phase in listOf(com.v2ray.ang.core.WireGuardState.Phase.Down, com.v2ray.ang.core.WireGuardState.Phase.Failed) })
        if (_state.value.ciscoReady) check(CiscoProfiles.getLastConnectedVpn()?.uuidString !in ids)
    }
    internal fun selectSubscription(id: String) {
        if (_state.value.subscriptions.none { it.id == id }) return
        launchJob?.cancel()
        launchJob = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { check(com.v2ray.ang.handler.MmkvManager.encodeSettings("protocol_selected_subscription_v1", id)) }
                _state.update { it.copy(selectedSubscriptionId = id) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { report("select-subscription", error); _state.update { it.copy(message = R.string.protocol_sub_failed) } }
        }
    }
    internal fun deleteSubscription(id: String) {
        if (subscriptionJob?.isActive == true) return
        subscriptionJob = viewModelScope.launch {
            initialized.await(); _state.update { it.copy(busy = true, message = null) }
            try {
                ProtocolSubscriptionRepository.mutation.withLock {
                    val records = withContext(Dispatchers.IO) { ProtocolSubscriptionRepository.read() }
                    val old = records.firstOrNull { it.id == id } ?: return@withLock
                    withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) { checkMutable(old); SubscriptionProfileCache.commit(app, old, records.filterNot { it.id == id }) }
                    _state.update { it.copy(subscriptions = records.filterNot { item -> item.id == id }, selectedSubscriptionId = records.firstOrNull { item -> item.id != id }?.id, message = null) }
                }
                reload()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { report("delete-subscription profile=$id", error); _state.update { it.copy(message = R.string.top_delete_connected) } }
            finally { _state.update { it.copy(busy = false) } }
        }
    }
    internal fun connectManaged(id: String) {
        if (launchJob?.isActive == true || _state.value.busy || !_state.value.vpnMode) return
        launchJob = viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            try {
                val profile = withContext(Dispatchers.IO) {
                    check(SettingsManager.isVpnMode())
                    val record = ProtocolSubscriptionRepository.read().firstOrNull { it.profiles.any { p -> p.id == id } } ?: error("Missing profile")
                    val item = record.profiles.first { it.id == id }
                    checkMutable(record.copy(profiles = listOf(item)))
                    when (item.protocol) {
                        ProfileProtocol.OpenVPN -> SubscriptionProfileCache.openVpn(app, item)
                        ProfileProtocol.Cisco -> { check(_state.value.ciscoReady); CiscoProfiles.importSubscription(item.id, item.name, item.address, item.username, item.password) }
                        ProfileProtocol.WireGuard -> com.wireguard.config.Config.parse(item.config.byteInputStream())
                    }
                    item
                }
                _state.update { it.copy(launch = ManagedLaunch(profile.id, profile.protocol)) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { report("prepare-managed-profile profile=$id", error); _state.update { it.copy(message = R.string.protocol_sub_failed) } }
            finally { _state.update { it.copy(busy = false) } }
        }
    }
    internal fun launchFailed(error: Throwable) { report("launch-managed-vpn", error); _state.update { it.copy(launch = null, message = R.string.protocol_unavailable) } }
    internal fun consumeManagedLaunch(): ManagedLaunch? {
        val value = _state.value.launch ?: return null
        _state.update { it.copy(launch = null) }
        return value.takeIf { _state.value.vpnMode && _state.value.subscriptions.any { record -> record.profiles.any { it.id == value.id } } }
    }
    internal fun importWireGuard(uri: Uri) {
        if (subscriptionJob?.isActive == true) return
        subscriptionJob = viewModelScope.launch {
            initialized.await(); _state.update { it.copy(busy = true, message = null) }
            try {
                val record = withContext(Dispatchers.IO) {
                    val name = app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null } ?: error("Missing file")
                    require(name.endsWith(".conf", true))
                    val bytes = app.contentResolver.openInputStream(uri)?.use { input -> ProtocolSubscriptionRepository.readProfileBytes(input) } ?: error("Missing content")
                    require(bytes.size in 1..OpenVpnImportPolicy.MAX_BYTES)
                    val config = com.wireguard.config.Config.parse(bytes.inputStream()).toWgQuickString()
                    val id = java.util.UUID.randomUUID().toString()
                    ProtocolSubscription(id, name.removeSuffix(".conf").take(100), "", 0, listOf(SubscriptionProfile(java.util.UUID.randomUUID().toString(), name.take(100), ProfileProtocol.WireGuard, config)))
                }
                ProtocolSubscriptionRepository.mutation.withLock {
                    withContext(Dispatchers.IO) { ProtocolSubscriptionRepository.save(ProtocolSubscriptionRepository.read() + record) }
                }
                _state.update { it.copy(selectedSubscriptionId = record.id, message = R.string.protocol_sub_done) }; reload()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { report("import-wireguard", error); _state.update { it.copy(message = R.string.protocol_sub_failed) } }
            finally { _state.update { it.copy(busy = false) } }
        }
    }

    private fun report(phase: String, e: Throwable) {
        val safe = IllegalStateException(e.javaClass.simpleName).apply { stackTrace = e.stackTrace }
        LogUtil.e(AppConfig.TAG, "Embedded VPN phase=$phase", safe)
    }
    override fun onCleared() {
        CiscoRuntime.removeStatusListener(ciscoStatusListener)
        subscriptionJob?.cancel(); launchJob?.cancel(); reloadJob?.cancel(); wireguardJob.cancel()
        statusClient?.close()
        statusClient = null
        super.onCleared()
    }
}
