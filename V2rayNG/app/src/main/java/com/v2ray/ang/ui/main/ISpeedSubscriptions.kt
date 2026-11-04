package com.v2ray.ang.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.dto.entities.SubscriptionUsageInfo
import com.v2ray.ang.extension.toTrafficString

@Composable
internal fun ISpeedPlans(state: MainUiState, modifier: Modifier, onAction: (MainAction) -> Unit, onNavigate: (MainDestination) -> Unit) {
    val plans = state.groups.filter { it.id.isNotEmpty() && it.id != AppConfig.DEFAULT_SUBSCRIPTION_ID }
    val selected = dashboardPlan(state)
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var focusAfterDelete by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedFocus = remember { FocusRequester() }
    val list = rememberLazyListState()
    val ids = plans.map { it.id }
    LaunchedEffect(ids, state.dashboardSubscriptionId, focusAfterDelete) {
        if (pendingDelete != null && pendingDelete !in ids) pendingDelete = null
        val removed = focusAfterDelete ?: return@LaunchedEffect
        if (removed !in ids) {
            val index = plans.indexOfFirst { it.id == selected?.id }
            if (index >= 0) {
                list.animateScrollToItem(index + 3)
                selectedFocus.requestFocus()
            }
            focusAfterDelete = null
        }
    }
    val target = plans.firstOrNull { it.id == pendingDelete }
    if (target != null) AlertDialog(onDismissRequest = { pendingDelete = null },
        title = { Text(stringResource(R.string.top_delete_subscription)) },
        text = { Text(stringResource(R.string.top_delete_subscription_confirm, target.remarks)) },
        confirmButton = { TextButton(onClick = {
            pendingDelete = null
            focusAfterDelete = target.id
            onAction(MainAction.DeleteSubscription(target.id))
        }) { Text(stringResource(R.string.top_delete_short), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(android.R.string.cancel)) } })
    LazyColumn(modifier, state = list, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            DashboardTitle(stringResource(R.string.top_plans), stringResource(R.string.top_add_subscription)) { onNavigate(MainDestination.AddSubscription) }
            state.dashboardMessage?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
        item {
            if (selected != null) SelectedPlanHero(selected,
                { onAction(MainAction.UpdateGroupSubscription(selected.id)) }, { pendingDelete = selected.id })
            else DashboardPanel {
                Text(stringResource(R.string.top_no_plan), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.top_no_plan_hint), style = MaterialTheme.typography.bodyMedium)
                PremiumButton(onClick = { onNavigate(MainDestination.AddSubscription) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.top_add_subscription)) }
            }
        }
        item { Text(stringResource(R.string.top_your_subscriptions), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(plans, key = { it.id }) { plan ->
            PlanChoiceCard(plan, plan.id == selected?.id, selectedFocus,
                { onAction(MainAction.SelectDashboardSubscription(plan.id)) },
                { onAction(MainAction.UpdateGroupSubscription(plan.id)) }, { pendingDelete = plan.id })
        }
        item { TextButton(onClick = { onNavigate(MainDestination.Subscriptions) }) { Text(stringResource(R.string.top_manage)) } }
    }
}

@Composable
private fun planDays(info: SubscriptionUsageInfo?): String {
    val now by produceState(System.currentTimeMillis() / 1000, info?.expire) {
        while (true) { value = System.currentTimeMillis() / 1000; kotlinx.coroutines.delay(60_000) }
    }
    return when (val days = subscriptionDaysRemaining(info?.expire, now)) {
        null -> stringResource(R.string.ispeed_unavailable)
        0 -> stringResource(R.string.top_plan_expired)
        else -> pluralStringResource(R.plurals.top_days_remaining, days, days)
    }
}

@Composable
private fun SelectedPlanHero(plan: GroupMapItem, onRefresh: () -> Unit, onDelete: () -> Unit) {
    val info = plan.usageInfo
    val known = info != null && hasSubscriptionTraffic(info)
    val limited = info != null && hasSubscriptionQuota(info) && info.total > 0
    val fraction = if (known && limited) (info.used.toDouble() / info.total).toFloat().coerceIn(0f, 1f) else 0f
    Surface(shape = RoundedCornerShape(26.dp), border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)) {
        Column(Modifier.background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f), MaterialTheme.colorScheme.surfaceContainerLow))).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(plan.remarks, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(painterResource(R.drawable.ic_fab_check), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.top_subscription_selected), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(planDays(info), style = MaterialTheme.typography.bodyMedium)
                }
                Box(Modifier.size(106.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { fraction }, Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant, strokeWidth = 9.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (known) info.used.toTrafficString() else stringResource(R.string.ispeed_unavailable), style = MaterialTheme.typography.labelLarge.copy(textDirection = TextDirection.Ltr), fontWeight = FontWeight.Bold)
                        Text(if (limited) info.total.toTrafficString() else if (info != null && hasSubscriptionQuota(info)) stringResource(R.string.top_unlimited) else stringResource(R.string.ispeed_unavailable), style = MaterialTheme.typography.labelSmall.copy(textDirection = TextDirection.Ltr))
                    }
                }
            }
            if (known && limited) Text(stringResource(R.string.top_remaining) + ": " + (info.total - info.used).coerceAtLeast(0).toTrafficString(), style = MaterialTheme.typography.bodyMedium)
            PlanActions(plan.remarks, onRefresh, onDelete)
        }
    }
}

@Composable
private fun PlanChoiceCard(plan: GroupMapItem, active: Boolean, selectedFocus: FocusRequester, onSelect: () -> Unit, onRefresh: () -> Unit, onDelete: () -> Unit) {
    val refreshLabel = stringResource(R.string.top_refresh_named, plan.remarks)
    val deleteLabel = stringResource(R.string.top_delete_named, plan.remarks)
    Surface(shape = RoundedCornerShape(20.dp), color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .35f) else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
            Row(Modifier.weight(1f).then(if (active) Modifier.focusRequester(selectedFocus) else Modifier)
                .clickable(role = Role.RadioButton, onClick = onSelect).semantics(mergeDescendants = true) { selected = active }
                .padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(26.dp).background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape), contentAlignment = Alignment.Center) {
                    if (active) Icon(painterResource(R.drawable.ic_fab_check), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(plan.remarks, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(if (plan.usageInfo != null && hasSubscriptionTraffic(plan.usageInfo)) stringResource(R.string.top_used) + ": " + plan.usageInfo.used.toTrafficString() else stringResource(R.string.ispeed_unavailable), style = MaterialTheme.typography.labelMedium)
                    Text(planDays(plan.usageInfo), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onRefresh) { Icon(painterResource(R.drawable.ic_refresh_24dp), refreshLabel, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) }
            IconButton(onDelete) { Icon(painterResource(R.drawable.ic_delete_24dp), deleteLabel, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun PlanActions(name: String, onRefresh: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val refresh = stringResource(R.string.top_refresh_named, name)
    val delete = stringResource(R.string.top_delete_named, name)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onRefresh, Modifier.weight(1f).semantics { contentDescription = refresh }) {
            Icon(painterResource(R.drawable.ic_refresh_24dp), null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.top_refresh))
        }
        TextButton(onClick = onDelete, Modifier.weight(1f).semantics { contentDescription = delete }) {
            Icon(painterResource(R.drawable.ic_delete_24dp), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.top_delete_short), color = MaterialTheme.colorScheme.error)
        }
    }
}
