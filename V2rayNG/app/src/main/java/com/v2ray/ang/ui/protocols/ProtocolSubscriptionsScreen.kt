package com.v2ray.ang.ui.protocols

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.core.WireGuardState
import com.v2ray.ang.ui.main.DashboardPanel
import com.v2ray.ang.ui.main.subscriptionDaysRemaining
import com.v2ray.ang.extension.toTrafficString

internal fun protocolLabel(protocol: ProfileProtocol): Int = when (protocol) {
    ProfileProtocol.OpenVPN -> R.string.protocol_openvpn
    ProfileProtocol.Cisco -> R.string.protocol_cisco_short
    ProfileProtocol.WireGuard -> R.string.protocol_wireguard
}

// ExperimentalMaterial3Api: the approved modal sheet has no stable draggable modal equivalent.
// Remove this opt-in when ModalBottomSheet is stable in the pinned Compose BOM.
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun SubscriptionEditorDialog(state: ProtocolsState, onAction: (ProtocolAction) -> Unit) {
    val editor = state.editor ?: return
    ModalBottomSheet(onDismissRequest = { onAction(ProtocolAction.CancelEditor) }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().consumeWindowInsets(WindowInsets.navigationBars).padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(if (editor.id == null) R.string.top_add_subscription else R.string.protocol_edit_sub), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.protocol_sub_hint), style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(editor.title, { onAction(ProtocolAction.EditorTitle(it)) }, label = { Text(stringResource(R.string.protocol_sub_title)) }, enabled = !state.busy, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(editor.link, { onAction(ProtocolAction.EditorLink(it)) }, label = { Text(stringResource(R.string.protocol_sub_link)) }, enabled = !state.busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), maxLines = 4, modifier = Modifier.fillMaxWidth())
            state.message?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Button(onClick = { onAction(ProtocolAction.SaveEditor) }, enabled = !state.busy && editor.link.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(stringResource(if (editor.id == null) R.string.top_add_subscription else R.string.top_update_short)) }
            OutlinedButton(border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .65f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary), onClick = { onAction(ProtocolAction.PasteEditor) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.menu_item_import_config_clipboard)) }
            TextButton(onClick = { onAction(ProtocolAction.CancelEditor) }, modifier = Modifier.align(Alignment.End)) { Text(stringResource(android.R.string.cancel)) }
        }
    }
}

@Composable internal fun ProtocolSubscriptionCard(state: ProtocolsState, onAction: (ProtocolAction) -> Unit) {
    val record = state.subscriptions.firstOrNull { it.id == state.selectedSubscriptionId } ?: state.subscriptions.firstOrNull() ?: return
    var deleting by rememberSaveable(record.id) { mutableStateOf(false) }
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text(stringResource(R.string.top_delete_subscription)) },
        text = { Text(stringResource(R.string.top_delete_subscription_confirm, record.title)) },
        confirmButton = { TextButton(onClick = { deleting = false; onAction(ProtocolAction.DeleteSubscription(record.id)) }) { Text(stringResource(R.string.top_delete_short)) } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text(stringResource(android.R.string.cancel)) } })
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.subscriptions.size > 1) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.subscriptions.forEach { item ->
                Surface(shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, if (item.id == record.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.selectable(selected = item.id == record.id, enabled = !state.busy, role = Role.RadioButton) { onAction(ProtocolAction.SelectSubscription(item.id)) }) {
                    Text(item.title, Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                }
            }
        }
        DashboardPanel(Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .65f), androidx.compose.foundation.shape.RoundedCornerShape(24.dp))) {
            Text(stringResource(if (record.url.isBlank()) R.string.protocol_manual else R.string.protocol_sub_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(record.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (record.updatedAt > 0) Text(stringResource(R.string.protocol_updated), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            if (record.url.isNotBlank()) ProtocolAccountDetails(record.info)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileProtocol.entries.forEach { kind -> Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Text(stringResource(R.string.protocol_count, record.profiles.count { it.protocol == kind }, stringResource(protocolLabel(kind))), Modifier.padding(horizontal = 10.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
                } }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .65f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary), onClick = { onAction(ProtocolAction.RefreshSubscription(record.id)) }, enabled = !state.busy && record.url.isNotBlank(), modifier = Modifier.weight(1f)) {
                    Icon(painterResource(R.drawable.ic_refresh_24dp), null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.top_update_short))
                }
                IconButton(onClick = { onAction(ProtocolAction.EditSubscription(record.id)) }, enabled = !state.busy && record.url.isNotBlank()) { Icon(painterResource(R.drawable.ic_edit_24dp), stringResource(R.string.protocol_edit_sub)) }
                IconButton(onClick = { deleting = true }, enabled = !state.busy) { Icon(painterResource(R.drawable.ic_delete_24dp), stringResource(R.string.top_delete_named, record.title)) }
            }
        }
    }
}

@Composable private fun ProtocolAccountDetails(info: ProtocolSubscriptionInfo?) {
    val absent = stringResource(R.string.ispeed_unavailable)
    val now by produceState(System.currentTimeMillis() / 1000, info?.expireSeconds) {
        while (true) { value = System.currentTimeMillis() / 1000; kotlinx.coroutines.delay(60_000) }
    }
    val days = subscriptionDaysRemaining(info?.expireSeconds, now)
    val remainingTime = when {
        info?.pendingDays != null -> pluralStringResource(R.plurals.top_days_remaining, info.pendingDays, info.pendingDays) + " · " + stringResource(R.string.protocol_activation_pending)
        info?.unlimitedTime == true -> stringResource(R.string.top_unlimited)
        days == null -> absent
        days == 0 -> stringResource(R.string.top_plan_expired)
        else -> pluralStringResource(R.plurals.top_days_remaining, days, days)
    }
    val total = if (info?.unlimitedTraffic == true) stringResource(R.string.top_unlimited) else info?.limitBytes?.toTrafficString() ?: absent
    val remaining = if (info?.unlimitedTraffic == true) stringResource(R.string.top_unlimited) else info?.remainingBytes?.toTrafficString() ?: absent
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.protocol_account_username) + ": " + (info?.username ?: absent), style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.top_used to (info?.usedBytes?.toTrafficString() ?: absent), R.string.top_total to total, R.string.top_remaining to remaining).forEach { (label, value) ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr), fontWeight = FontWeight.Bold)
                }
            }
        }
        if (info?.usedBytes != null && info.limitBytes != null && info.limitBytes > 0) LinearProgressIndicator(
            progress = { (info.usedBytes.toDouble() / info.limitBytes).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        Text(remainingTime, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable internal fun ManagedProfiles(state: ProtocolsState, protocol: ProfileProtocol, onAction: (ProtocolAction) -> Unit) {
    val record = state.subscriptions.firstOrNull { it.id == state.selectedSubscriptionId } ?: state.subscriptions.firstOrNull()
    val profiles = record?.profiles.orEmpty().filter { it.protocol == protocol }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (profiles.isEmpty()) Text(stringResource(R.string.protocol_no_subprofiles), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        profiles.forEach { profile -> key(profile.id) {
            val connected = when (protocol) {
                ProfileProtocol.OpenVPN -> isConnectedOpenVpnProfile(state, profile.id)
                ProfileProtocol.Cisco -> state.ciscoActiveId == profile.id
                ProfileProtocol.WireGuard -> state.wireguard.profileId == profile.id && state.wireguard.phase == WireGuardState.Phase.Up
            }
            val pending = protocol == ProfileProtocol.WireGuard && state.wireguard.profileId == profile.id && state.wireguard.phase == WireGuardState.Phase.Connecting
            val action = if (protocol == ProfileProtocol.WireGuard && (connected || pending)) ProtocolAction.DisconnectWireGuard
                else if (protocol == ProfileProtocol.OpenVPN && connected) ProtocolAction.Disconnect
                else if (protocol == ProfileProtocol.Cisco && connected) ProtocolAction.Cisco else ProtocolAction.ConnectManaged(profile.id)
            DashboardPanel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(painterResource(if (connected) R.drawable.ic_fab_check else R.drawable.ic_top_shield), null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text(profile.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(stringResource(protocolLabel(protocol)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (connected) Text(stringResource(R.string.protocol_profile_connected), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    OutlinedButton(border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .65f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary), onClick = { onAction(action) }, enabled = canHandleProtocolAction(state, action)) {
                        Text(stringResource(if (connected || pending) { if (protocol == ProfileProtocol.Cisco) R.string.protocol_cisco_manage else R.string.protocol_stop } else R.string.top_connect))
                    }
                }
                if (pending) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (protocol == ProfileProtocol.WireGuard && state.wireguard.profileId == profile.id && state.wireguard.phase == WireGuardState.Phase.Failed) Text(stringResource(R.string.protocol_wireguard_failed), color = MaterialTheme.colorScheme.error)
            }
        } }
    }
}
