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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
    ProtocolsScreen(state, modifier, onAction)
}

@Composable
internal fun ProtocolsScreen(state: ProtocolsState, modifier: Modifier, onAction: (ProtocolAction) -> Unit) {
    var cisco by rememberSaveable { mutableStateOf(false) }
    var details by rememberSaveable { mutableStateOf(false) }
    if (details) AlertDialog(onDismissRequest = { details = false }, title = { Text(stringResource(if (cisco) R.string.protocol_cisco else R.string.protocol_import)) },
        text = { Text(stringResource(if (cisco) R.string.protocol_cisco_hint else R.string.protocol_import_hint)) },
        confirmButton = { TextButton(onClick = { details = false }) { Text(stringResource(android.R.string.ok)) } })
    LazyColumn(modifier.imePadding(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text(stringResource(R.string.protocol_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .4f))) {
                Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ProtocolSelector(stringResource(R.string.protocol_openvpn), !cisco, Modifier.weight(1f)) { cisco = false }
                    ProtocolSelector(stringResource(R.string.protocol_cisco_short), cisco, Modifier.weight(1f)) { cisco = true }
                }
            }
        }
        if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (!state.vpnMode && !state.busy) item { Text(stringResource(R.string.protocol_vpn_only), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        state.message?.let { message -> item { Text(stringResource(message), style = MaterialTheme.typography.bodySmall) } }
        if (!cisco) {
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
                DashboardPanel {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileEmblem()
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(stringResource(R.string.protocol_openvpn), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(profile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    PremiumButton(onClick = { onAction(ProtocolAction.Connect(profile.guid)) }, enabled = canHandleProtocolAction(state, ProtocolAction.Connect(profile.guid)), modifier = Modifier.fillMaxWidth()) {
                        Icon(painterResource(R.drawable.ic_top_power), null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.protocol_connect_profile, profile.name), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            item {
                DashboardPanel {
                    if (state.profiles.isEmpty() && !state.busy) {
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
        } else item {
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
private fun ProtocolSelector(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = if (selected) listOf(Color(0xFFBC9441), Color(0xFFFFE5A0), Color(0xFFD7B361)) else listOf(Color.Transparent, Color.Transparent)
    Surface(modifier = modifier.selectable(selected = selected, role = Role.Tab, onClick = onClick), shape = CircleShape, color = Color.Transparent) {
        Box(Modifier.background(Brush.linearGradient(colors)).heightIn(min = 42.dp).padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
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
