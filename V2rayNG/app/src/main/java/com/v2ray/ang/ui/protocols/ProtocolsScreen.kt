package com.v2ray.ang.ui.protocols

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.extension.toTrafficString
import com.v2ray.ang.ui.main.DashboardPanel
import com.v2ray.ang.ui.main.PremiumButton

@Composable
internal fun ProtocolsRoute(model: ProtocolsViewModel, modifier: Modifier, onAction: (ProtocolAction) -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(model) {
        model.reload()
        onPauseOrDispose { }
    }
    LaunchedEffect(state.launch) { if (state.launch != null) onAction(ProtocolAction.LaunchManaged) }
    ProtocolsScreen(state, modifier, onAction)
}

@Composable
internal fun ProtocolsScreen(state: ProtocolsState, modifier: Modifier, onAction: (ProtocolAction) -> Unit) {
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    val connected = connectedProtocol(state)
    val anyConnected = hasConnectedProtocol(state)
    var protocol by rememberSaveable { mutableStateOf(connected?.protocol?.name ?: ProfileProtocol.OpenVPN.name) }
    var handledConnection by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(connected?.key, anyConnected) {
        if (connected != null && connected.key != handledConnection) {
            protocol = connected.protocol.name
            handledConnection = connected.key
        } else if (!anyConnected) handledConnection = null
    }
    val selectedProtocol = ProfileProtocol.valueOf(protocol)
    val cisco = selectedProtocol == ProfileProtocol.Cisco
    SubscriptionEditorDialog(state, onAction)
    var details by rememberSaveable { mutableStateOf(false) }
    if (details) AlertDialog(onDismissRequest = { details = false }, title = { Text(stringResource(if (cisco) R.string.protocol_cisco else R.string.protocol_import)) },
        text = { Text(stringResource(if (cisco) R.string.protocol_cisco_hint else R.string.protocol_import_hint)) },
        confirmButton = { TextButton(onClick = { details = false }) { Text(stringResource(android.R.string.ok)) } })
    val deleteTarget = state.profiles.firstOrNull { it.guid == pendingDelete }
    if (deleteTarget != null) AlertDialog(onDismissRequest = { pendingDelete = null },
        title = { Text(stringResource(R.string.protocol_delete_title)) },
        text = { Text(stringResource(R.string.protocol_delete_confirm, deleteTarget.name)) },
        confirmButton = { TextButton(onClick = { pendingDelete = null; onAction(ProtocolAction.Delete(deleteTarget.guid)) },
            enabled = canHandleProtocolAction(state, ProtocolAction.Delete(deleteTarget.guid))) { Text(stringResource(R.string.top_delete_short)) } },
        dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(android.R.string.cancel)) } })
    LazyColumn(modifier.imePadding(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            DashboardPanel {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ProfileEmblem()
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            stringResource(R.string.protocol_heading),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.protocol_sub_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                PremiumButton(
                    onClick = { onAction(ProtocolAction.EditSubscription(null)) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(painterResource(R.drawable.ic_top_up), null, Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.top_add_subscription))
                }
            }
        }
        item { ProtocolSubscriptionCard(state, onAction) }
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .4f))) {
                Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ProfileProtocol.entries.forEach { item -> ProtocolSelector(stringResource(protocolLabel(item)), selectedProtocol == item, connected?.protocol == item, Modifier.weight(1f)) { protocol = item.name } }
                }
            }
        }
        if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()); TextButton(onClick = { onAction(ProtocolAction.CancelEditor) }) { Text(stringResource(android.R.string.cancel)) } }
        if (!state.vpnMode && !state.busy) item { Text(stringResource(R.string.protocol_vpn_only), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        state.message?.let { message -> item { Text(stringResource(message), style = MaterialTheme.typography.bodySmall) } }
        item { ManagedProfiles(state, selectedProtocol, onAction) }
        if (selectedProtocol == ProfileProtocol.WireGuard) item {
            OutlinedButton(onClick = { onAction(ProtocolAction.ImportWireGuard) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .65f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)) { Text(stringResource(R.string.protocol_import_wireguard)) }
        }
        if (selectedProtocol == ProfileProtocol.OpenVPN) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)) {
                        Text(stringResource(state.status), Modifier.padding(horizontal = 12.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.weight(1f))
                    if (canHandleProtocolAction(state, ProtocolAction.Disconnect)) TextButton(onClick = { onAction(ProtocolAction.Disconnect) }) {
                        Text(stringResource(R.string.protocol_disconnect), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (state.received != null && state.sent != null) item {
                Text(stringResource(R.string.protocol_traffic, state.received.toTrafficString(), state.sent.toTrafficString()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(state.profiles, key = { it.guid }) { profile ->
                val connected = isConnectedOpenVpnProfile(state, profile.guid)
                val action = if (connected) ProtocolAction.DisconnectProfile(profile.guid) else ProtocolAction.Connect(profile.guid)
                DashboardPanel {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileEmblem()
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(stringResource(R.string.protocol_openvpn), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(profile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (connected) Icon(painterResource(R.drawable.ic_fab_check), stringResource(R.string.protocol_profile_connected), Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { pendingDelete = profile.guid }, enabled = canHandleProtocolAction(state, ProtocolAction.Delete(profile.guid))) {
                            Icon(painterResource(R.drawable.ic_delete_24dp), stringResource(R.string.protocol_delete_named, profile.name), Modifier.size(20.dp))
                        }
                    }
                    PremiumButton(onClick = { onAction(action) }, enabled = canHandleProtocolAction(state, action), modifier = Modifier.fillMaxWidth()) {
                        Icon(painterResource(R.drawable.ic_top_power), null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(if (connected) R.string.protocol_disconnect_profile else R.string.protocol_connect_profile, profile.name), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            item {
                DashboardPanel {
                    Text(stringResource(R.string.protocol_manual), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    if (state.profiles.isEmpty() && state.subscriptions.isEmpty() && !state.busy) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ProfileEmblem()
                            Text(stringResource(R.string.protocol_no_profiles), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    PremiumButton(onClick = { onAction(ProtocolAction.Import) }, enabled = canHandleProtocolAction(state, ProtocolAction.Import), modifier = Modifier.fillMaxWidth()) {
                        Icon(painterResource(R.drawable.ic_top_up), null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.protocol_import))
                    }
                    TextButton(onClick = { details = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.protocol_details), style = MaterialTheme.typography.labelMedium) }
                    if (!state.openVpnReady && !state.busy) Text(stringResource(R.string.protocol_unavailable), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        } else if (cisco) item {
            DashboardPanel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileEmblem()
                    Text(stringResource(R.string.protocol_cisco), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(stringResource(R.string.protocol_cisco_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                PremiumButton(onClick = { onAction(ProtocolAction.Cisco) }, enabled = canHandleProtocolAction(state, ProtocolAction.Cisco), modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.protocol_cisco_manage))
                }
                TextButton(onClick = { details = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.protocol_details), style = MaterialTheme.typography.labelMedium) }
                if (!state.ciscoReady && !state.busy) Text(stringResource(R.string.protocol_unavailable), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        item { Text(stringResource(R.string.protocol_credits), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f)) }
    }
}

@Composable
private fun ProtocolSelector(label: String, selected: Boolean, connected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = if (selected) listOf(Color(0xFFBC9441), Color(0xFFFFE5A0), Color(0xFFD7B361)) else listOf(Color.Transparent, Color.Transparent)
    val connectedLabel = stringResource(R.string.protocol_profile_connected)
    Surface(modifier = modifier.clip(CircleShape).selectable(selected = selected, role = Role.Tab, onClick = onClick).semantics {
        if (connected) stateDescription = connectedLabel
    }, shape = CircleShape, color = Color.Transparent) {
        Row(Modifier.background(Brush.linearGradient(colors)).heightIn(min = 42.dp).padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (connected) Box(Modifier.padding(end = 5.dp).size(5.dp).background(Color(0xFF51CF8F), CircleShape))
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (selected) Color(0xFF261E0D) else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ProfileEmblem() {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .1f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .3f))) {
        Icon(painterResource(R.drawable.ic_top_shield), null, Modifier.padding(12.dp).size(28.dp), tint = MaterialTheme.colorScheme.primary)
    }
}
