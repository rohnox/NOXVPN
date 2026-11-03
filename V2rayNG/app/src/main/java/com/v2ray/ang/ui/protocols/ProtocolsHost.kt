package com.v2ray.ang.ui.protocols

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.v2ray.ang.core.LauncherManager

/** Platform actions are owned by the activity; profile import remains in the ViewModel. */
internal sealed interface ProtocolAction {
    data object Import : ProtocolAction
    data class Connect(val guid: String) : ProtocolAction
    data object Disconnect : ProtocolAction
    data object Cisco : ProtocolAction
}

internal fun canHandleProtocolAction(state: ProtocolsState, action: ProtocolAction): Boolean = when (action) {
    ProtocolAction.Import -> state.openVpnReady && !state.busy
    is ProtocolAction.Connect -> state.openVpnReady && state.vpnMode && !state.busy && state.profiles.any { it.guid == action.guid }
    ProtocolAction.Disconnect -> state.openVpnReady && state.vpnMode && !state.busy &&
        (state.connected || state.status == com.v2ray.ang.R.string.protocol_state_connecting || state.status == com.v2ray.ang.R.string.protocol_state_input)
    ProtocolAction.Cisco -> state.ciscoReady && state.vpnMode && !state.busy
}

internal class ProtocolsHost(private val activity: ComponentActivity, private val model: () -> ProtocolsViewModel) {
    private val importFile = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { model().importProfile(it) }
    }

    fun onAction(action: ProtocolAction) {
        if (!canHandleProtocolAction(model().state.value, action)) return
        when (action) {
            ProtocolAction.Import -> importFile.launch(arrayOf("*/*"))
            is ProtocolAction.Connect -> LauncherManager.launchEmbeddedOpenVpn(activity, action.guid)
            ProtocolAction.Disconnect -> LauncherManager.disconnectEmbeddedOpenVpn(activity)
            ProtocolAction.Cisco -> LauncherManager.launchEmbeddedCisco(activity)
        }
    }
}
