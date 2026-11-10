package com.v2ray.ang.ui.main

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.input.pointer.pointerInput
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
    onServers: () -> Unit, onPlans: () -> Unit, onImport: () -> Unit,
    onAction: (MainAction) -> Unit, onNavigate: (MainDestination) -> Unit, onMore: (ServerRowUiModel) -> Unit
) {
    val plan = dashboardPlan(state)
    LazyColumn(modifier, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.appUpdate?.takeIf { it.hasUpdate }?.let { update ->
            item(key = "app-update") {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)) {
                    Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_check_update_24dp), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.top_app_update, update.latestVersion.orEmpty()), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                        Button({ onNavigate(MainDestination.CheckUpdate) }, modifier = Modifier.heightIn(min = 36.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) { Text(stringResource(R.string.top_app_update_action), style = MaterialTheme.typography.labelMedium) }
                        IconButton({ onAction(MainAction.DismissAppUpdate) }) {
                            Icon(painterResource(R.drawable.ic_app_update_close), stringResource(R.string.top_dismiss_app_update), Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
        item { ConnectionHero(state, status, onServers) { onAction(MainAction.RefreshExitIp) } }
        item { TrafficPanel(plan?.usageInfo, plan?.id) { if (plan == null) onNavigate(MainDestination.Subscriptions) else onAction(MainAction.UpdateGroupSubscription(plan.id)) } }
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
private fun ConnectionHero(state: MainUiState, status: String, onServers: () -> Unit, onRefreshExitIp: () -> Unit) {
    val sessionSeconds = rememberSessionSeconds(state.sessionTraffic, state.isRunning)
    val activeColor = if (MaterialTheme.colorScheme.surface.luminance() > .5f) Color(0xFF167B47) else Color(0xFF5BD68B)
    Surface(shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .36f))) {
        Box(Modifier.background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerLow)))) {
            val flag = if (state.isRunning) (state.exitIp as? ExitIpState.Address)?.flagBitmap else null
            if (flag != null) {
                val light = MaterialTheme.colorScheme.surface.luminance() > .5f
                Image(flag.asImageBitmap(), null, Modifier.matchParentSize().alpha(if (light) .23f else .38f), contentScale = ContentScale.Crop)
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (light) .25f else .12f),
                    MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = if (light) .5f else .65f)))))
            }
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
                }
                if (state.isRunning) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.top_exit_ip), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    androidx.compose.foundation.text.selection.SelectionContainer(Modifier.weight(1f)) {
                        Text(when (val value = state.exitIp) {
                            is ExitIpState.Address -> value.value
                            ExitIpState.Unavailable -> stringResource(R.string.top_exit_ip_unavailable)
                            else -> stringResource(R.string.top_exit_ip_loading)
                        }, style = MaterialTheme.typography.labelMedium.copy(textDirection = TextDirection.Ltr), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    (state.exitIp as? ExitIpState.Address)?.let { address ->
                        val code = address.countryCode
                        val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
                        if (code == null) Text(stringResource(R.string.ispeed_unavailable), style = MaterialTheme.typography.labelSmall)
                        else Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = .4f))) {
                            Row(Modifier.widthIn(max = 120.dp).padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(countryFlag(code), modifier = Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.bodyMedium)
                                Text(java.util.Locale.Builder().setRegion(code).build().getDisplayCountry(locale),
                                    modifier = Modifier.weight(1f, fill = false), style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (state.exitIp == ExitIpState.Loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else IconButton(onRefreshExitIp) { Icon(painterResource(R.drawable.ic_refresh_24dp), stringResource(R.string.top_exit_ip_refresh), Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroMetric(stringResource(R.string.top_session), sessionSeconds?.let { formatSessionDuration(it) } ?: stringResource(R.string.ispeed_unavailable), Modifier.weight(1f))
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
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.labelLarge.copy(textDirection = TextDirection.Ltr), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TrafficPanel(info: com.v2ray.ang.dto.entities.SubscriptionUsageInfo?, subscriptionId: String?, onRefresh: () -> Unit) {
    val nowSeconds by androidx.compose.runtime.produceState(System.currentTimeMillis() / 1000) {
        while (true) {
            value = System.currentTimeMillis() / 1000
            kotlinx.coroutines.delay(60_000)
        }
    }
    val remote = info?.providerUsage
    val local = remember(info?.dailyUsage, nowSeconds) { com.v2ray.ang.handler.SubscriptionInfoParser.recentUsage(info, nowSeconds) }
    val points = remote ?: local
    var selectedDay by rememberSaveable(subscriptionId) { mutableStateOf<Long?>(null) }
    var dayMenu by remember { mutableStateOf(false) }
    LaunchedEffect(points) { if (points.none { it.timestampSeconds == selectedDay }) selectedDay = null }
    val chooseDay = stringResource(R.string.top_choose_day)
    val known = info != null && hasSubscriptionTraffic(info)
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val used = if (remote != null) com.v2ray.ang.handler.PasarGuardUsage.total(remote)?.let { formatHistoryTraffic(it, locale) } ?: stringResource(R.string.ispeed_unavailable)
        else if (known) info.used.toTrafficString() else stringResource(R.string.ispeed_unavailable)
    val graphDescription = stringResource(R.string.top_provider_chart_a11y, used)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f)
    val color = MaterialTheme.colorScheme.primary
    val dates = remember(points, locale) {
        val formatter = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, locale).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        points.map { formatter.format(java.util.Date(it.timestampSeconds * 1000)) }
    }
    DashboardPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.top_traffic), Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Icon(painterResource(R.drawable.ic_top_chart), null, tint = color)
        }
        Text(stringResource(if (remote != null) R.string.top_remote_history else R.string.top_provider_history), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (remote != null) info?.providerUsageUpdatedAt?.takeIf { it in 1..Long.MAX_VALUE / 1000 }?.let { updated ->
            val time = remember(updated, locale) { java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT, locale).format(java.util.Date(updated * 1000)) }
            Text(stringResource(R.string.top_history_updated, time), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TrafficLegend(stringResource(if (remote != null) R.string.top_monthly_usage else R.string.top_combined_usage), used, color)
        if (points.isEmpty()) {
            Text(stringResource(if (remote != null) R.string.top_remote_history_empty else R.string.top_provider_history_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onRefresh) { Text(stringResource(R.string.top_refresh)) }
        } else {
            Canvas(Modifier.fillMaxWidth().height(84.dp).semantics {
                contentDescription = graphDescription
                if (remote != null) onClick(label = chooseDay) { dayMenu = true; true }
            }.pointerInput(points, remote) {
                if (remote != null) detectTapGestures { position ->
                    selectedDay = nearestProviderDay(points, position.x / size.width.coerceAtLeast(1))
                }
            }) {
                repeat(4) { i -> val y = size.height * i / 3; drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx()) }
                val ceiling = points.maxOf { it.usedBytes }.coerceAtLeast(1L).toDouble()
                val start = points.first().timestampSeconds
                val range = (points.last().timestampSeconds - start).coerceAtLeast(1L)
                val path = Path()
                points.forEachIndexed { index, point ->
                    val x = (point.timestampSeconds - start).toFloat() / range * size.width
                    val y = size.height - (point.usedBytes / ceiling * (size.height - 12.dp.toPx())).toFloat()
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    drawCircle(color, 3.dp.toPx(), Offset(x, y))
                    if (point.timestampSeconds == selectedDay) {
                        drawLine(color.copy(alpha = .6f), Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                        drawCircle(color, 5.dp.toPx(), Offset(x, y))
                    }
                }
                drawPath(path, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(dates.first(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dates.last(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (remote != null) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val selected = points.firstOrNull { it.timestampSeconds == selectedDay }
                Text(if (selected == null) stringResource(R.string.top_day_hint) else stringResource(R.string.top_day_value,
                    dates[points.indexOf(selected)], formatHistoryTraffic(selected.usedBytes, locale)), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                Box {
                    TextButton(onClick = { dayMenu = true }) { Text(chooseDay, style = MaterialTheme.typography.labelSmall) }
                    DropdownMenu(expanded = dayMenu, onDismissRequest = { dayMenu = false }, modifier = Modifier.heightIn(max = 280.dp)) {
                        points.forEachIndexed { index, point -> key(point.timestampSeconds) {
                            DropdownMenuItem(text = { Text(dates[index] + " · " + formatHistoryTraffic(point.usedBytes, locale)) },
                                onClick = { selectedDay = point.timestampSeconds; dayMenu = false })
                        } }
                    }
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
