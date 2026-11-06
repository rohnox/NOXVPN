package com.v2ray.ang.ui.protocols

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.v2ray.ang.core.LauncherManager

/** Platform actions are owned by the activity; profile import remains in the ViewModel. */
internal sealed interface ProtocolAction {
    data object Import : ProtocolAction
    data object ImportWireGuard : ProtocolAction
    data class EditSubscription(val id: String?) : ProtocolAction
    data class EditorTitle(val text: String) : ProtocolAction
    data class EditorLink(val text: String) : ProtocolAction
    data object CancelEditor : ProtocolAction
    data object PasteEditor : ProtocolAction
    data object SaveEditor : ProtocolAction
    data class RefreshSubscription(val id: String) : ProtocolAction
    data class DeleteSubscription(val id: String) : ProtocolAction
    data class SelectSubscription(val id: String) : ProtocolAction
    data class ConnectManaged(val id: String) : ProtocolAction
    data object LaunchManaged : ProtocolAction
    data object DisconnectWireGuard : ProtocolAction
    data class Connect(val guid: String) : ProtocolAction
    data object Disconnect : ProtocolAction
    data class DisconnectProfile(val guid: String) : ProtocolAction
    data class Delete(val guid: String) : ProtocolAction
    data object Cisco : ProtocolAction
}

internal fun canHandleProtocolAction(state: ProtocolsState, action: ProtocolAction): Boolean = when (action) {
    ProtocolAction.Import -> state.openVpnReady && !state.busy
    is ProtocolAction.Connect -> state.openVpnReady && state.vpnMode && !state.busy && state.profiles.any { it.guid == action.guid } && !isConnectedOpenVpnProfile(state, action.guid)
    ProtocolAction.Disconnect -> state.openVpnReady && state.vpnMode &&
        (state.connected || state.status == com.v2ray.ang.R.string.protocol_state_connecting || state.status == com.v2ray.ang.R.string.protocol_state_input)
    is ProtocolAction.DisconnectProfile -> isConnectedOpenVpnProfile(state, action.guid) && canHandleProtocolAction(state, ProtocolAction.Disconnect)
    is ProtocolAction.Delete -> state.openVpnReady && !state.busy && !openVpnSessionActive(state) && state.profiles.any { it.guid == action.guid }
    ProtocolAction.Cisco -> state.ciscoReady && state.vpnMode && (!state.busy || state.ciscoActiveId != null)
    ProtocolAction.CancelEditor -> true
    is ProtocolAction.ConnectManaged -> state.vpnMode && !state.busy && state.subscriptions.flatMap { it.profiles }.firstOrNull { it.id == action.id }?.let { profile ->
        when (profile.protocol) { ProfileProtocol.OpenVPN -> state.openVpnReady; ProfileProtocol.Cisco -> state.ciscoReady; ProfileProtocol.WireGuard -> true }
    } == true
    ProtocolAction.DisconnectWireGuard -> state.wireguard.phase in listOf(com.v2ray.ang.core.WireGuardState.Phase.Up, com.v2ray.ang.core.WireGuardState.Phase.Connecting)
    else -> !state.busy
}

internal class ProtocolsHost(private val activity: ComponentActivity, private val model: () -> ProtocolsViewModel) {
    private var pendingWireGuard: String? = null
    private var restored = false
    private fun restorePending() {
        if (restored) return
        pendingWireGuard = activity.savedStateRegistry.consumeRestoredStateForKey("protocols-wireguard-consent")?.getString("id")
        restored = true
    }
    init { activity.savedStateRegistry.registerSavedStateProvider("protocols-wireguard-consent") { android.os.Bundle().apply { putString("id", pendingWireGuard) } } }
    private val consent = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        restorePending()
        val id = pendingWireGuard; pendingWireGuard = null
        if (result.resultCode == android.app.Activity.RESULT_OK && id != null && model().state.value.subscriptions.any { record -> record.profiles.any { it.id == id && it.protocol == ProfileProtocol.WireGuard } }) startWireGuard(id)
    }
    private fun startWireGuard(id: String) {
        val target = model()
        LauncherManager.launchWireGuard(activity, id) { error -> target.launchFailed(error) }
    }
    private val notifications = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { prepareWireGuardConsent() }
    private fun prepareWireGuardConsent() {
        restorePending()
        val id = pendingWireGuard ?: return
        try {
            val request = android.net.VpnService.prepare(activity)
            if (request == null) { pendingWireGuard = null; startWireGuard(id) } else consent.launch(request)
        } catch (error: Exception) { pendingWireGuard = null; model().launchFailed(error) }
    }
    private fun prepareWireGuard(id: String) {
        pendingWireGuard = id
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && androidx.core.content.ContextCompat.checkSelfPermission(activity, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            notifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        else prepareWireGuardConsent()
    }
    private val importWireGuard = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { model().importWireGuard(it) } }
    private val importFile = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { model().importProfile(it) }
    }

    fun onAction(action: ProtocolAction) {
        restorePending()
        if (!canHandleProtocolAction(model().state.value, action)) return
        when (action) {
            ProtocolAction.ImportWireGuard -> importWireGuard.launch(arrayOf("*/*"))
            is ProtocolAction.EditSubscription -> model().editSubscription(action.id)
            is ProtocolAction.EditorTitle -> model().changeEditor(title = action.text)
            is ProtocolAction.EditorLink -> model().changeEditor(link = action.text)
            ProtocolAction.CancelEditor -> model().cancelEditor()
            ProtocolAction.PasteEditor -> model().pasteEditorLink()
            ProtocolAction.SaveEditor -> model().saveEditor()
            is ProtocolAction.RefreshSubscription -> model().refreshSubscription(action.id)
            is ProtocolAction.DeleteSubscription -> model().deleteSubscription(action.id)
            is ProtocolAction.SelectSubscription -> model().selectSubscription(action.id)
            is ProtocolAction.ConnectManaged -> model().connectManaged(action.id)
            ProtocolAction.DisconnectWireGuard -> LauncherManager.stopWireGuard(activity)
            ProtocolAction.LaunchManaged -> {
                model().consumeManagedLaunch()?.let { profile -> when (profile.protocol) {
                    ProfileProtocol.OpenVPN -> LauncherManager.launchEmbeddedOpenVpn(activity, profile.id) { error -> model().launchFailed(error) }
                    ProfileProtocol.Cisco -> LauncherManager.launchCiscoProfile(activity, profile.id)
                    ProfileProtocol.WireGuard -> prepareWireGuard(profile.id)
                } }
            }
            ProtocolAction.Import -> importFile.launch(arrayOf("*/*"))
            is ProtocolAction.Connect -> LauncherManager.launchEmbeddedOpenVpn(activity, action.guid)
            ProtocolAction.Disconnect -> LauncherManager.disconnectEmbeddedOpenVpn(activity)
            is ProtocolAction.DisconnectProfile -> LauncherManager.disconnectEmbeddedOpenVpn(activity)
            is ProtocolAction.Delete -> model().deleteProfile(action.guid)
            ProtocolAction.Cisco -> LauncherManager.launchEmbeddedCisco(activity)
        }
    }
}
