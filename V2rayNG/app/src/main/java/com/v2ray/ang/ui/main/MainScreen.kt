package com.v2ray.ang.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.ui.compose.QRCodeDialog

internal enum class DashboardTab(val label: Int, val icon: Int) {
    Overview(R.string.top_overview, R.drawable.ic_top_home),
    Servers(R.string.top_servers, R.drawable.ic_top_servers),
    Plans(R.string.top_plans, R.drawable.ic_subscriptions_24dp),
    Protocols(R.string.protocol_tab, R.drawable.ic_openvpn_menu),
    Controls(R.string.top_controls, R.drawable.ic_settings_24dp)
}

@Composable
fun MainScreen(mainViewModel: MainViewModel, onAction: (MainAction) -> Unit, onNavigate: (MainDestination) -> Unit,
    protocolsContent: @Composable (Modifier) -> Unit,
    connectedProtocolKey: String? = null, anyProtocolConnected: Boolean = false, protocolStatusReady: Boolean = true) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val state by mainViewModel.uiState.collectAsStateWithLifecycle()
    val loading by mainViewModel.isLoading.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(if (connectedProtocolKey != null) DashboardTab.Protocols.name else DashboardTab.Overview.name) }
    var handledProtocol by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(connectedProtocolKey, anyProtocolConnected, protocolStatusReady) {
        if (protocolStatusReady) {
            if (connectedProtocolKey != null && connectedProtocolKey != handledProtocol) {
                selectedTab = DashboardTab.Protocols.name
                handledProtocol = connectedProtocolKey
            } else if (!anyProtocolConnected) handledProtocol = null
        }
    }
    val tab = dashboardTab(selectedTab)
    androidx.lifecycle.compose.LifecycleResumeEffect(tab, state.dashboardSubscriptionId) {
        onAction(MainAction.UsageVisibility(tab == DashboardTab.Overview))
        onPauseOrDispose { onAction(MainAction.UsageVisibility(false)) }
    }
    var importOpen by rememberSaveable { mutableStateOf(false) }
    var shareGuid by rememberSaveable { mutableStateOf<String?>(null) }
    var removeTarget by rememberSaveable(stateSaver = ServerDeleteTarget.Saver) { mutableStateOf<ServerDeleteTarget?>(null) }
    var deleteAll by rememberSaveable { mutableStateOf(false) }
    var deleteDuplicate by rememberSaveable { mutableStateOf(false) }
    var deleteInvalid by rememberSaveable { mutableStateOf(false) }
    val groupFlow = remember(state.selectedGroupId) { mainViewModel.serverGroupState(state.selectedGroupId) }
    val group by groupFlow.collectAsStateWithLifecycle()
    val shareRow = group.rows.firstOrNull { it.guid == shareGuid }
    LaunchedEffect(shareGuid, group.rows) { if (shareGuid != null && shareRow == null) shareGuid = null }
    BackHandler(enabled = tab != DashboardTab.Overview) { selectedTab = DashboardTab.Overview.name }

    MainDialogs(deleteAll, { deleteAll = false }, { deleteAll = false; onAction(MainAction.RemoveAllServers) },
        deleteDuplicate, { deleteDuplicate = false }, { deleteDuplicate = false; onAction(MainAction.RemoveDuplicateServers) },
        deleteInvalid, { deleteInvalid = false }, { deleteInvalid = false; onAction(MainAction.RemoveInvalidServers) },
        removeTarget, { removeTarget = null }, { guid -> removeTarget = null; onAction(MainAction.RemoveServer(guid)) })
    shareRow?.let { row ->
        ShareMethodDialog(row.guid, row.profile, true, { shareGuid = null }, onAction, { id, name ->
            if (state.confirmRemove) removeTarget = ServerDeleteTarget(id, name) else onAction(MainAction.RemoveServer(id))
        })
    }
    state.shareQRCodeBitmap?.let { QRCodeDialog(bitmap = it, onDismiss = { onAction(MainAction.DismissQRCodeDialog) }) }
    if (importOpen) DashboardImportDialog({ importOpen = false }) { action -> importOpen = false; onAction(action) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SubscriptionRefreshBanner(state.subscriptionRefresh) },
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                DashboardHeader(
                    onRefresh = if (tab == DashboardTab.Overview) ({ onAction(MainAction.UpdateGroupSubscription("")) }) else null,
                    onLanguageClick = { onNavigate(MainDestination.Settings) },
                    onTelegramClick = { uriHandler.openUri("https://t.me/iSpeedTopVPN") }
                )
                if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        },
        bottomBar = {
            Box(Modifier.fillMaxWidth()) {
                DashboardNavigation(tab) { selectedTab = it.name }
                Button(
                    onClick = {
                        if (state.selectedGuid == null) importOpen = true
                        else onAction(MainAction.ToggleService)
                    },
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = (-32).dp).size(64.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isRunning) androidx.compose.ui.graphics.Color(0xFF16A34A)
                            else MaterialTheme.colorScheme.primary,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 3.dp)
                ) {
                    Icon(
                        painter = painterResource(if (state.selectedGuid == null) R.drawable.ic_add_24dp else R.drawable.ic_top_power),
                        contentDescription = stringResource(
                            if (state.isRunning) R.string.top_disconnect
                            else if (state.selectedGuid == null) R.string.top_add else R.string.top_connect
                        ),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    ) { padding ->
        val content = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
        when (tab) {
            DashboardTab.Overview -> ISpeedOverview(state, group.rows, mainViewModel.formatStatus(state.status), content,
                onServers = { selectedTab = DashboardTab.Servers.name }, onPlans = { selectedTab = DashboardTab.Plans.name }, onImport = { importOpen = true },
                onAction = onAction, onNavigate = onNavigate, onMore = { shareGuid = it.guid })
            DashboardTab.Servers -> ISpeedServers(state, group.rows, content, onAction,
                onImport = { importOpen = true }, onMore = { shareGuid = it.guid })
            DashboardTab.Plans -> ISpeedPlans(state, content, onAction, onImport = { importOpen = true })
            DashboardTab.Protocols -> protocolsContent(content)
            DashboardTab.Controls -> ISpeedControls(content, onNavigate)
        }
    }
}

internal fun dashboardTab(savedName: String): DashboardTab =
    DashboardTab.entries.firstOrNull { it.name == savedName } ?: DashboardTab.Overview

@Composable
internal fun DashboardImportDialog(onDismiss: () -> Unit, onAction: (MainAction) -> Unit) {
    var link by rememberSaveable { mutableStateOf("") }
    var methods by rememberSaveable { mutableStateOf(false) }
    Dialog(onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.94f).heightIn(max = 640.dp), shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.top_add), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.top_empty_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(link, { link = it }, label = { Text(stringResource(R.string.top_import_link)) }, modifier = Modifier.fillMaxWidth(), maxLines = 4)
                Button(onClick = { onAction(MainAction.ImportBatchConfig(link.trim())) }, enabled = link.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.top_import))
                }
                OutlinedButton(onClick = { onAction(MainAction.ImportClipboard) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.menu_item_import_config_clipboard)) }
                OutlinedButton(onClick = { onAction(MainAction.ImportQRcode) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.menu_item_import_config_qrcode)) }
                OutlinedButton(onClick = { onAction(MainAction.ImportConfigLocal) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.menu_item_import_config_local)) }
                TextButton(onClick = { methods = !methods }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.top_manual)) }
                if (methods) ImportMenuContent(onAction)
            }
        }
    }
}

@Composable
internal fun SubscriptionRefreshBanner(notice: SubscriptionRefreshNotice?) {
    if (notice == null) return
    val success = notice == SubscriptionRefreshNotice.Success
    Surface(Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite }, shape = RoundedCornerShape(28.dp),
        color = if (success) androidx.compose.ui.graphics.Color(0xFF28783A) else MaterialTheme.colorScheme.errorContainer,
        contentColor = if (success) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onErrorContainer) {
        Text(stringResource(when (notice) {
            SubscriptionRefreshNotice.Success -> R.string.top_sub_updated
            SubscriptionRefreshNotice.Partial -> R.string.top_sub_partial
            SubscriptionRefreshNotice.Failed -> R.string.top_sub_failed
        }), Modifier.padding(horizontal = 22.dp, vertical = 16.dp), style = MaterialTheme.typography.bodyMedium)
    }
}
