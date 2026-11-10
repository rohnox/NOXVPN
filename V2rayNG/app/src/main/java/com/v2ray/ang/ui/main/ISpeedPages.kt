package com.v2ray.ang.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.SubscriptionUsageInfo
import com.v2ray.ang.extension.toTrafficString
import java.text.DateFormat
import java.util.Date

@Composable
internal fun DashboardEmpty(onImport: () -> Unit) {
    DashboardPanel {
        Icon(painterResource(R.drawable.ic_top_servers), null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.top_empty_servers), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.top_empty_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onImport, Modifier.fillMaxWidth()) { Text(stringResource(R.string.top_add)) }
    }
}

@Composable
internal fun DashboardServerCard(row: ServerRowUiModel, isSelected: Boolean, onSelect: () -> Unit, onMore: () -> Unit, compact: Boolean = false) {
    Surface(onClick = onSelect, modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { selected = isSelected },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = .5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f))) {
        if (compact) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(if (isSelected) R.drawable.ic_fab_check else R.drawable.ic_top_servers), null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.weight(1f))
                    IconButton(onMore) { Icon(painterResource(R.drawable.ic_more_vert_24dp), stringResource(R.string.top_more)) }
                }
                Text(row.remarks, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(row.typeDescription, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                ServerDelay(row.testDelayMillis)
            }
        } else Row(Modifier.padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(painterResource(if (isSelected) R.drawable.ic_fab_check else R.drawable.ic_top_servers), null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(row.remarks, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(row.typeDescription, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                ServerDelay(row.testDelayMillis)
            }
            IconButton(onMore) { Icon(painterResource(R.drawable.ic_more_vert_24dp), stringResource(R.string.top_more)) }
        }
    }
}

@Composable
internal fun DashboardMiniServer(row: ServerRowUiModel, isSelected: Boolean, modifier: Modifier, onSelect: () -> Unit, onMore: () -> Unit) {
    Surface(onClick = onSelect, modifier = modifier.semantics(mergeDescendants = true) { selected = isSelected }, shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = .75f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))) {
        Row(Modifier.padding(start = 12.dp, end = 2.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(row.remarks, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                ServerDelay(row.testDelayMillis)
            }
            IconButton(onMore, Modifier.size(40.dp)) { Icon(painterResource(R.drawable.ic_more_vert_24dp), stringResource(R.string.top_more), Modifier.size(18.dp)) }
        }
    }
}

@Composable
internal fun ISpeedServers(state: MainUiState, rows: List<ServerRowUiModel>, modifier: Modifier, onAction: (MainAction) -> Unit, onImport: () -> Unit, onMore: (ServerRowUiModel) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val grid = rememberLazyGridState()
    LaunchedEffect(query) { onAction(MainAction.Search(query)) }
    LaunchedEffect(state.locateTarget, rows) {
        val target = state.locateTarget ?: return@LaunchedEffect
        if (target.groupId != state.selectedGroupId) return@LaunchedEffect
        val index = rows.indexOfFirst { it.guid == target.serverGuid }
        if (index >= 0) { grid.animateScrollToItem(index + 3); onAction(MainAction.LocateHandled) }
    }
    LazyVerticalGrid(columns = GridCells.Fixed(if (state.doubleColumnDisplay) 2 else 1), state = grid,
        modifier = modifier.imePadding(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) { DashboardTitle(stringResource(R.string.top_your_servers), stringResource(R.string.top_add), onImport) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = true,
                label = { Text(stringResource(R.string.top_search)) }, leadingIcon = { Icon(painterResource(R.drawable.ic_search_24dp), null) })
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.groups.forEach { group -> key(group.id) {
                        FilterChip(selected = group.id == state.selectedGroupId, onClick = { onAction(MainAction.SelectGroup(group.id)) }, label = { Text(group.remarks) })
                    } }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onAction(if (state.isTesting) MainAction.CancelTesting else MainAction.TestRealAllServers) }, modifier = Modifier.weight(1f)) { Text(stringResource(if (state.isTesting) R.string.acc_stop else R.string.top_test)) }
                    TextButton(onClick = { onAction(MainAction.SortByTestResults) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.title_sort_by_test_results)) }
                }
            }
        }
        if (rows.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            if (query.isBlank()) DashboardEmpty(onImport) else DashboardPanel { Text(stringResource(R.string.top_no_matches)) }
        }
        items(rows, key = { it.guid }) { row -> DashboardServerCard(row, row.guid == state.selectedGuid, { onAction(MainAction.SelectServer(row.guid)) }, { onMore(row) }, compact = state.doubleColumnDisplay) }
    }
}

@Composable
internal fun DashboardQuotaCard(info: SubscriptionUsageInfo?, title: String, compact: Boolean = false, onRefresh: () -> Unit) {
    val absent = stringResource(R.string.ispeed_unavailable)
    val trafficKnown = info != null && hasSubscriptionTraffic(info)
    val quotaKnown = info != null && hasSubscriptionQuota(info)
    val fraction = if (trafficKnown && quotaKnown && info.total > 0) (info.used.toDouble() / info.total).toFloat().coerceIn(0f, 1f) else null
    val locale = LocalConfiguration.current.locales[0]
    val nowSeconds by produceState(System.currentTimeMillis() / 1000, info?.expire) {
        while (true) {
            value = System.currentTimeMillis() / 1000
            kotlinx.coroutines.delay(60_000)
        }
    }
    val days = subscriptionDaysRemaining(info?.expire, nowSeconds)
    val expiry = when (days) {
        null -> absent
        0 -> stringResource(R.string.top_plan_expired)
        else -> pluralStringResource(R.plurals.top_days_remaining, days, days)
    }
    val displayTitle = subscriptionTitle(info, title)
    val track = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f)
    val ink = if (fraction != null && fraction >= .9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    DashboardPanel {
        if (compact) {
            Text(stringResource(R.string.top_plan_quota), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(displayTitle.ifBlank { stringResource(R.string.top_plan_quota) }, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(onRefresh, Modifier.size(32.dp)) { Icon(painterResource(R.drawable.ic_refresh_24dp), stringResource(if (info == null) R.string.top_manage else R.string.top_refresh), Modifier.size(18.dp)) }
            }
            if (info == null) {
                Text(stringResource(R.string.top_no_plan), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CompactQuota(stringResource(R.string.top_used), if (trafficKnown) info.used.toTrafficString() else absent, Modifier.weight(1f))
                    CompactQuota(stringResource(R.string.top_total), if (!quotaKnown) absent else if (info.total <= 0) stringResource(R.string.top_unlimited) else info.total.toTrafficString(), Modifier.weight(1f))
                    CompactQuota(stringResource(R.string.top_remaining), if (trafficKnown && quotaKnown && info.total > 0) (info.total - info.used).coerceAtLeast(0).toTrafficString() else absent, Modifier.weight(1f))
                }
                if (fraction != null) LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().height(6.dp), color = ink, trackColor = track, gapSize = 0.dp, drawStopIndicator = {})
                if (days != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.top_validity), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(expiry, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
        DashboardTitle(displayTitle.ifBlank { stringResource(R.string.top_plan_quota) }, stringResource(R.string.top_refresh), onRefresh)
        Text(stringResource(R.string.top_plan_source), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (info == null) {
            Text(stringResource(R.string.top_no_plan), fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.top_no_plan_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            if (fraction != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.top_used), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(java.text.NumberFormat.getPercentInstance(locale).format(fraction.toDouble()), fontWeight = FontWeight.Bold, color = ink)
                }
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().height(8.dp), color = ink, trackColor = track, gapSize = 0.dp, drawStopIndicator = {})
            }
            QuotaValue(stringResource(R.string.top_used), if (trafficKnown) info.used.toTrafficString() else absent)
            QuotaValue(stringResource(R.string.top_total), if (!quotaKnown) absent else if (info.total <= 0) stringResource(R.string.top_unlimited) else info.total.toTrafficString())
            QuotaValue(stringResource(R.string.top_remaining), if (trafficKnown && quotaKnown && info.total > 0) (info.total - info.used).coerceAtLeast(0).toTrafficString() else absent)
            HorizontalDivider(color = track)
            QuotaValue(stringResource(R.string.top_validity), expiry)
            if (!trafficKnown) Text(stringResource(R.string.top_plan_missing), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        }
    }
}

@Composable
private fun CompactQuota(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge.copy(textDirection = TextDirection.Ltr), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
@Composable
private fun QuotaValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun ISpeedControls(modifier: Modifier, onNavigate: (MainDestination) -> Unit) {
    val routes = listOf(
        MainDestination.Settings, MainDestination.Routing, MainDestination.PerAppProxy,
        MainDestination.Subscriptions, MainDestination.UserAssets, MainDestination.BackupRestore,
        MainDestination.Logcat, MainDestination.CheckUpdate, MainDestination.About
    )
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 148.dp),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .38f))
                ) {
                    Box {
                        GoldWave(Modifier.fillMaxWidth().height(116.dp))
                        Row(
                            Modifier.fillMaxWidth().padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Image(
                                painterResource(R.drawable.nox_logo_mark),
                                contentDescription = "NOX",
                                modifier = Modifier.size(58.dp)
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("NOX", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    stringResource(R.string.top_tools_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Text(
                    stringResource(R.string.top_settings_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
                Text(
                    stringResource(R.string.top_controls),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
        }
        items(routes, key = { it.name }) { route ->
            Surface(
                onClick = { onNavigate(route) },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .38f)),
                modifier = Modifier.fillMaxWidth().heightIn(min = 116.dp)
            ) {
                Column(
                    Modifier.fillMaxSize().padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(38.dp).background(
                                MaterialTheme.colorScheme.primary.copy(alpha = .12f),
                                RoundedCornerShape(12.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painterResource(route.iconRes),
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(
                            painterResource(R.drawable.ic_expand_more_24dp),
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        stringResource(route.labelRes),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                stringResource(R.string.ispeed_attribution),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
    }
}

@Composable
internal fun ServerDelay(delayMillis: Long) {
    val failed = delayMillis < 0
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (failed) Icon(painterResource(R.drawable.ic_app_update_close), null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.error)
        Text(when { delayMillis > 0 -> stringResource(R.string.top_latency, delayMillis)
            failed -> stringResource(R.string.top_server_unreachable)
            else -> stringResource(R.string.top_untested) }, style = MaterialTheme.typography.labelSmall,
            color = if (failed) MaterialTheme.colorScheme.error else if (delayMillis > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
