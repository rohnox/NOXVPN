package com.v2ray.ang.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.extension.toSpeedString
import com.v2ray.ang.extension.toTrafficString
import kotlin.math.max

@Composable
internal fun DashboardPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f))) {
        Column(Modifier.background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .3f), MaterialTheme.colorScheme.surfaceContainerLow))).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
internal fun DashboardTitle(title: String, action: String? = null, onClick: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (action != null) TextButton(onClick) { Text(action) }
    }
}

@Composable
internal fun ISpeedOverview(
    state: MainUiState, rows: List<ServerRowUiModel>, status: String, modifier: Modifier,
    onToggle: () -> Unit, onServers: () -> Unit, onImport: () -> Unit,
    onAction: (MainAction) -> Unit, onNavigate: (MainDestination) -> Unit, onMore: (ServerRowUiModel) -> Unit
) {
    val plan = state.groups.firstOrNull { it.id == state.selectedSubscriptionId && it.id.isNotEmpty() && it.id != AppConfig.DEFAULT_SUBSCRIPTION_ID }
        ?: state.groups.firstOrNull { it.id == state.selectedGroupId && it.id.isNotEmpty() && it.id != AppConfig.DEFAULT_SUBSCRIPTION_ID }
    LazyColumn(modifier, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ConnectionHero(state, status, onToggle, onServers) }
        item { TrafficPanel(state, onNavigate) }
        item { DashboardQuotaCard(plan?.usageInfo, plan?.remarks.orEmpty(), compact = true) {
            if (plan == null) onNavigate(MainDestination.Subscriptions) else onAction(MainAction.UpdateGroupSubscription(plan.id))
        } }
        item { DashboardTitle(stringResource(R.string.top_your_servers), stringResource(R.string.top_all_servers), onServers) }
        if (rows.isEmpty()) item { DashboardEmpty(onImport) }
        else item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.take(2).forEach { row -> androidx.compose.runtime.key(row.guid) {
                    DashboardMiniServer(row, row.guid == state.selectedGuid, Modifier.weight(1f),
                        { onAction(MainAction.SelectServer(row.guid)) }, { onMore(row) })
                } }
            }
        }
        item { AutoConnectPanel(state, onAction) }
    }
}

@Composable
private fun AutoConnectPanel(state: MainUiState, onAction: (MainAction) -> Unit) {
    DashboardPanel {
        PremiumButton(onClick = { onAction(if (state.isAutoSelecting) MainAction.CancelAutoConnect else MainAction.AutoConnect) }, modifier = Modifier.fillMaxWidth()) {
            Icon(painterResource(R.drawable.ic_top_chart), null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if (state.isAutoSelecting) R.string.top_auto_cancel else R.string.top_auto_connect))
        }
        Text(stringResource(R.string.top_auto_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.isAutoSelecting) {
            if (state.autoTotal > 0) {
                LinearProgressIndicator(progress = { (state.autoCompleted.toFloat() / state.autoTotal).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.top_auto_progress, state.autoCompleted, state.autoTotal), style = MaterialTheme.typography.bodySmall)
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.top_auto_preparing), style = MaterialTheme.typography.bodySmall)
            }
        }
        state.autoMessage?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun ConnectionHero(state: MainUiState, status: String, onToggle: () -> Unit, onServers: () -> Unit) {
    val activeColor = if (MaterialTheme.colorScheme.surface.luminance() > .5f) Color(0xFF167B47) else Color(0xFF5BD68B)
    Surface(shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .36f))) {
        Box(Modifier.background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerLow)))) {
            GoldWave(Modifier.fillMaxWidth().height(105.dp))
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.top_heading), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(7.dp).background(if (state.isRunning) activeColor else MaterialTheme.colorScheme.onSurfaceVariant, CircleShape))
                            Text(stringResource(if (state.isRunning) R.string.top_online else R.string.top_offline), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                                color = if (state.isRunning) activeColor else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton(onServers, contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 36.dp)) {
                            Text(state.selectedServerName.ifBlank { stringResource(R.string.top_choose) }, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(82.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .1f), CircleShape).padding(5.dp)) {
                            Button(onClick = onToggle, modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFFFFE8AD), Color(0xFFE4BD65), Color(0xFFAC812C))), CircleShape),
                                shape = CircleShape, border = BorderStroke(1.dp, Color(0xFFFFE9B2)), contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color(0xFF2A210D))) {
                                Icon(painterResource(if (state.selectedGuid == null) R.drawable.ic_add_24dp else R.drawable.ic_top_power),
                                    stringResource(if (state.isRunning) R.string.top_disconnect else if (state.selectedGuid == null) R.string.top_add else R.string.top_connect), Modifier.size(32.dp))
                            }
                        }
                        Text(stringResource(if (state.isRunning) R.string.top_disconnect else if (state.selectedGuid == null) R.string.top_add else R.string.top_connect), style = MaterialTheme.typography.labelMedium)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroMetric(stringResource(R.string.top_session), state.sessionTraffic?.let { formatSessionDuration(it.elapsedSeconds) } ?: stringResource(R.string.ispeed_unavailable), Modifier.weight(1f))
                    HeroMetric(stringResource(R.string.top_upload), state.sessionTraffic?.takeIf { it.available }?.uploaded?.toTrafficString() ?: stringResource(R.string.ispeed_unavailable), Modifier.weight(1f))
                    HeroMetric(stringResource(R.string.top_download), state.sessionTraffic?.takeIf { it.available }?.downloaded?.toTrafficString() ?: stringResource(R.string.ispeed_unavailable), Modifier.weight(1f))
                }
                if (state.status !is MainStatus.Disconnected && state.status !is MainStatus.Connected) {
                    Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun HeroMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.labelLarge.copy(textDirection = TextDirection.Ltr), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TrafficPanel(state: MainUiState, onNavigate: (MainDestination) -> Unit) {
    val rates = remember(state.trafficHistory) { trafficRates(state.trafficHistory) }
    val latest = rates.lastOrNull()
    val unavailable = stringResource(R.string.ispeed_unavailable)
    val up = latest?.up?.toLong()?.toSpeedString() ?: unavailable
    val down = latest?.down?.toLong()?.toSpeedString() ?: unavailable
    val graphDescription = if (latest == null) stringResource(R.string.top_chart_empty) else stringResource(R.string.top_chart_a11y, up, down)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f)
    val downloadColor = MaterialTheme.colorScheme.primary
    val uploadColor = MaterialTheme.colorScheme.secondary
    DashboardPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.top_traffic), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
            Icon(painterResource(R.drawable.ic_top_chart), null, tint = MaterialTheme.colorScheme.primary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TrafficLegend(stringResource(R.string.top_upload), up, uploadColor)
            TrafficLegend(stringResource(R.string.top_download), down, downloadColor)
        }
        if (rates.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.top_chart_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { onNavigate(MainDestination.Settings) }) { Text(stringResource(R.string.top_stats_settings)) }
        } else {
            Canvas(Modifier.fillMaxWidth().height(84.dp).semantics { contentDescription = graphDescription }) {
                repeat(4) { index -> val y = size.height * index / 3; drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx()) }
                val maxValue = rates.maxOf { max(it.up, it.down) }.coerceAtLeast(1.0)
                val start = rates.first().seconds
                val range = (rates.last().seconds - start).coerceAtLeast(1L)
                fun plot(upload: Boolean, color: Color) {
                    val path = Path()
                    rates.forEachIndexed { i, point ->
                        val x = (point.seconds - start).toFloat() / range * size.width
                        val y = size.height - ((if (upload) point.up else point.down) / maxValue * (size.height - 12.dp.toPx())).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        if (rates.size == 1) drawCircle(color, 3.dp.toPx(), Offset(x, y))
                    }
                    if (!upload && rates.size > 1) {
                        val area = Path().apply {
                            addPath(path)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = .22f), color.copy(alpha = 0f))))
                    }
                    drawPath(path, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
                }
                plot(false, downloadColor); plot(true, uploadColor)
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatSessionDuration(rates.first().seconds), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatSessionDuration(rates.last().seconds), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            }
        }
    }
}

@Composable
private fun TrafficLegend(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(5.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium.copy(textDirection = TextDirection.Ltr))
    }
}