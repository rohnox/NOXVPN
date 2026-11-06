package com.v2ray.ang.ui.visual

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.dto.SessionTraffic
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.SubscriptionUsageInfo
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.ui.compose.staticAppColorScheme
import com.v2ray.ang.ui.compose.AppTypography
import com.v2ray.ang.ui.main.*
import com.v2ray.ang.ui.protocols.*
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Render-only fixtures: no endpoint, persisted data, ViewModel or native VPN initialization. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35], qualifiers = "en-w393dp-h852dp-xhdpi")
class PremiumVisualTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var inputModes: InputModeManager
    private val commands = mutableListOf<MainAction>()
    private val destinations = mutableListOf<MainDestination>()

    private fun render(protocols: Boolean = false, rtl: Boolean = false, light: Boolean = false, populated: Boolean = false, notice: SubscriptionRefreshNotice? = null, plans: Boolean = false, exitIp: ExitIpState = ExitIpState.Address("198.51.100.24"), appUpdateFixture: com.v2ray.ang.dto.CheckUpdateResult? = null) {
        val visualIp = if (exitIp is ExitIpState.Address && exitIp.countryCode != null) exitIp.copy(flagBitmap = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) { com.v2ray.ang.util.CountryFlagImages.load(org.robolectric.RuntimeEnvironment.getApplication(), exitIp.countryCode.orEmpty()) }) else exitIp
        val history = (0L..30L).map { t -> SessionTraffic(t, t * 10240L, t * t * 4096L, true) }
        val quota = SubscriptionUsageInfo(title = "iSpecialVPN", expire = System.currentTimeMillis() / 1000 + 7 * 86400, upload = 2L shl 30, download = 12L shl 30, total = 40L shl 30, trafficAvailable = true, quotaAvailable = true, providerUsageUpdatedAt = System.currentTimeMillis() / 1000, providerUsage = (2L downTo 0L).map { day -> com.v2ray.ang.dto.entities.SubscriptionUsageSample(System.currentTimeMillis() / 1000 - day * 86400, (3L - day) * 104857600L) }, dailyUsage = (2L downTo 0L).map { day -> com.v2ray.ang.dto.entities.SubscriptionUsageSample(System.currentTimeMillis() / 1000 - day * 86400, (14L - day) shl 30) })
        val state = if (populated) MainUiState(appUpdate = appUpdateFixture, groups = if (plans) listOf(GroupMapItem("plan", "iSpecialVPN", quota), GroupMapItem("family", "Family plan", quota.copy(title = "Family plan", upload = 1L shl 30, download = 6L shl 30, total = 30L shl 30, expire = System.currentTimeMillis() / 1000 + 18 * 86400)), GroupMapItem("work", "Work plan", quota.copy(title = "Work plan", upload = 1L shl 30, download = 3L shl 30, total = 20L shl 30, expire = System.currentTimeMillis() / 1000 + 9 * 86400))) else listOf(GroupMapItem("plan", "import sub", quota)), dashboardSubscriptionId = if (plans) "family" else "", selectedGuid = "a", selectedSubscriptionId = "plan",
            selectedServerName = "Germany · Frankfurt", isRunning = true, exitIp = visualIp, status = MainStatus.Connected, sessionTraffic = history.last(), trafficHistory = history) else MainUiState()
        val rows = if (populated) listOf("Germany · Frankfurt", "Turkey · Istanbul").mapIndexed { i, name ->
            ServerRowUiModel(if (i == 0) "a" else "b", ProfileItem(configType = EConfigType.VLESS), name, "", "VLESS / TLS", 34L + i * 15L, "")
        } else emptyList()
        compose.setContent {
            val modes = LocalInputModeManager.current
            SideEffect { inputModes = modes }
            MaterialTheme(colorScheme = staticAppColorScheme(!light), typography = AppTypography) {
                CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    var tab by remember { mutableStateOf(if (plans) DashboardTab.Plans else if (protocols) DashboardTab.Protocols else DashboardTab.Overview) }
                    var displayed by remember { mutableStateOf(state) }
                    var importOpen by remember { mutableStateOf(false) }
                    val action: (MainAction) -> Unit = { command ->
                        commands.add(command)
                        when (command) {
                            MainAction.DismissAppUpdate -> displayed = displayed.copy(appUpdate = null)
                            is MainAction.SelectDashboardSubscription -> displayed = displayed.copy(dashboardSubscriptionId = command.id)
                            is MainAction.DeleteSubscription -> {
                                val remaining = displayed.groups.filterNot { it.id == command.id }
                                displayed = displayed.copy(groups = remaining, dashboardSubscriptionId = resolveDashboardSubscription(remaining, displayed.dashboardSubscriptionId, displayed.selectedSubscriptionId))
                            }
                            else -> Unit
                        }
                    }
                    if (importOpen) DashboardImportDialog({ importOpen = false }) { command -> importOpen = false; action(command) }
                    Scaffold(containerColor = MaterialTheme.colorScheme.background,
                        snackbarHost = { SubscriptionRefreshBanner(notice) },
                        topBar = { Column(Modifier.statusBarsPadding()) { DashboardHeader(if (tab == DashboardTab.Overview) ({ action(MainAction.UpdateGroupSubscription("")) }) else null) } },
                        bottomBar = { DashboardNavigation(tab) { tab = it } }) { padding ->
                        val modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
                        if (tab == DashboardTab.Protocols) ProtocolsScreen(ProtocolsState(busy = false, openVpnReady = true, ciscoReady = true, vpnMode = true,
                            profiles = if (populated) listOf(OpenVpnRow("profile", "Personal VPN")) else emptyList()), modifier) {}
                        else if (tab == DashboardTab.Plans) ISpeedPlans(displayed, modifier, action, onImport = { importOpen = true })
                        else if (tab == DashboardTab.Controls) ISpeedControls(modifier, {})
                        else if (tab == DashboardTab.Servers) ISpeedServers(displayed, rows, modifier, action, { importOpen = true }, {})
                        else ISpeedOverview(displayed, rows, "", modifier, {}, {}, { importOpen = true }, action, { destinations.add(it) }, {})
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun capture(name: String) {
        // Robolectric 4.17 / API35 native graphics can capture only the dirty region after a click.
        // Remove this repaint when both interaction snapshots render the full window without it.
        compose.runOnIdle {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).forEach {
                it.window.decorView.requestLayout()
                it.window.decorView.invalidate()
            }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        val target = File(System.getProperty("ispeed.visual.output"), "$name.png")
        checkNotNull(target.parentFile).mkdirs()
        val root = compose.onAllNodes(isRoot()).onLast()
        File(target.parentFile, "$name.semantics.txt").writeText(root.printToString())
        // Robolectric 4.17/API35 captureToImage reads the activity pixels for a dialog window.
        // Keep dialog semantics only until separate-window bitmap capture is supported correctly.
        if (compose.onAllNodes(isRoot()).fetchSemanticsNodes().size > 1) return
        root.captureToImage().asAndroidBitmap().let { bitmap ->
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun homeEnglish() { render(populated = true); compose.onNodeWithText("iSpecialVPN").assertIsDisplayed(); compose.onNodeWithText("Subscription quota").assertIsDisplayed(); compose.onNodeWithText("Total 30-day usage").assertIsDisplayed(); compose.onNodeWithText("600.00 MB").assertIsDisplayed(); compose.onNodeWithText("7 days remaining").assertExists(); compose.onNodeWithText("import sub").assertDoesNotExist(); capture("home-en-dark") }
    @Test fun homeEmpty() { render(); capture("home-en-empty") }
    @Test @Config(qualifiers = "fa-rIR-w393dp-h852dp-xhdpi")
    fun homePersian() { render(rtl = true, populated = true); compose.onNodeWithText("سهمیه مصرف").assertIsDisplayed(); capture("home-fa-dark") }
    @Test fun protocolEmpty() { render(protocols = true); compose.onNodeWithText("Import .ovpn file").assertIsDisplayed(); capture("protocol-empty-en") }
    @Test fun protocolProfile() { render(protocols = true, populated = true); capture("protocol-profile-en") }
    @Test fun ciscoSelector() { render(protocols = true); compose.onNodeWithText("Cisco", useUnmergedTree = true).performClick(); capture("cisco-en") }
    @Test fun lightHome() { render(light = true, populated = true); capture("home-en-light") }
    @Test fun bottomNavigationOpensProtocolScreen() {
        render()
        compose.onNodeWithText("VPN").performClick()
        compose.onNodeWithText("VPN").assertIsSelected()
        compose.onNodeWithText("Import .ovpn file").assertIsDisplayed()
        compose.onNodeWithText("Disconnect OpenVPN").assertDoesNotExist()
        capture("protocol-after-navigation")
    }
    @Test fun successfulSubscriptionUpdateBanner() {
        render(populated = true, notice = SubscriptionRefreshNotice.Success)
        compose.onNodeWithText("Subscription updated successfully").assertIsDisplayed()
        capture("subscription-updated")
    }
    @Test fun failedSubscriptionUpdateHasNoSuccessMessage() {
        render(notice = SubscriptionRefreshNotice.Failed)
        compose.onNodeWithText("Subscription update failed. Check your connection.").assertIsDisplayed()
        compose.onNodeWithText("Subscription updated successfully").assertDoesNotExist()
        capture("subscription-update-failed")
    }

    @Test fun subscriptionDesignThreeEnglish() { render(plans = true, populated = true); compose.onNodeWithText("Selected").assertIsDisplayed(); capture("subscriptions-en") }
    @Test fun subscriptionsEmpty() { render(plans = true); compose.onNodeWithText("No subscription usage available").assertIsDisplayed(); capture("subscriptions-empty") }
    @Test fun subscriptionsLight() { render(plans = true, populated = true, light = true); compose.onNodeWithText("Selected").assertIsDisplayed(); capture("subscriptions-light") }
    @Test fun keyboardSelectsSubscription() {
        render(plans = true, populated = true)
        compose.runOnIdle { inputModes.requestInputMode(InputMode.Keyboard) }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Work plan") and hasClickAction())
        val row = compose.onNode(hasText("Work plan") and hasClickAction()).performScrollTo()
        row.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        row.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        row.assertIsSelected()
    }
    @Test fun dpadSelectsSubscription() {
        render(plans = true, populated = true)
        compose.runOnIdle { inputModes.requestInputMode(InputMode.Keyboard) }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Work plan") and hasClickAction())
        val row = compose.onNode(hasText("Work plan") and hasClickAction()).performScrollTo()
        row.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        row.assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        row.assertIsSelected()
    }
    @Test @Config(qualifiers = "fa-rIR-w393dp-h852dp-xhdpi")
    fun subscriptionDesignThreePersian() { render(plans = true, populated = true, rtl = true); capture("subscriptions-fa") }
    @Test fun selectingSubscriptionChangesHomeInformation() {
        render(plans = true, populated = true)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Work plan") and hasClickAction())
        compose.onNode(hasText("Work plan") and hasClickAction()).performScrollTo().performClick().assertIsSelected()
        capture("subscriptions-work-selected")
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithText("Work plan").assertIsDisplayed()
        compose.onNodeWithText("4.0 GB").performScrollTo().assertIsDisplayed()
        capture("home-work-selected")
    }
    @Test fun deleteSubscriptionFromSameTabRequiresConfirmation() {
        render(plans = true, populated = true)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription("Delete subscription Family plan"))
        compose.onAllNodesWithContentDescription("Delete subscription Family plan").onFirst().performClick()
        compose.onNodeWithText("Delete subscription “Family plan” and its configurations?").assertIsDisplayed()
        capture("subscription-delete-confirmation")
        compose.onAllNodesWithText("Delete").onLast().performClick()
        compose.onAllNodesWithText("Family plan").assertCountEquals(0)
    }
    @Test fun tappingChartShowsActualDailyUsage() {
        render(populated = true)
        compose.onNode(hasContentDescription("Combined subscription usage", substring = true)).performTouchInput { click(center) }
        compose.onNode(hasText("200.00 MB", substring = true)).assertIsDisplayed()
        capture("daily-usage-selected")
        compose.onNodeWithText("Choose day").performClick()
        compose.onNode(hasText("300.00 MB", substring = true)).performClick()
        compose.onNode(hasText("300.00 MB", substring = true)).assertIsDisplayed()
    }
    @Test fun homeHasEnglishUpdateButtonAndNoHeaderIcons() {
        render(populated = true)
        compose.onNodeWithText("Update Subscription").assertIsDisplayed().performClick()
        org.junit.Assert.assertEquals(MainAction.UpdateGroupSubscription(""), commands.last())
        compose.onNodeWithText("آپدیت اشتراک").assertDoesNotExist()
        compose.onNodeWithContentDescription("Add configuration").assertDoesNotExist()
        capture("home-update-en")
    }
    @Test @Config(qualifiers = "fa-rIR-w393dp-h852dp-xhdpi")
    fun homeHasPersianUpdateButtonOnly() {
        render(populated = true, rtl = true)
        compose.onNodeWithText("آپدیت اشتراک").assertIsDisplayed()
        compose.onNodeWithText("Update Subscription").assertDoesNotExist()
        capture("home-update-fa")
    }
    @Test fun addSubscriptionUsesImportDialogAndTrimsInput() {
        render(plans = true, populated = true)
        compose.onNodeWithText("Manage").assertDoesNotExist()
        compose.onNodeWithText("Update Sub").assertIsDisplayed()
        compose.onNodeWithText("Add subscription").performClick()
        compose.onNodeWithText("Add configuration").assertIsDisplayed()
        compose.onNodeWithText("Import").assertIsNotEnabled()
        compose.onNodeWithText("Configuration or subscription link").performTextInput("  https://example.invalid/sub/test-only  ")
        compose.onNodeWithText("Import").performScrollTo().performClick()
        org.junit.Assert.assertEquals(MainAction.ImportBatchConfig("https://example.invalid/sub/test-only"), commands.last())
    }
    @Test fun subscriptionDialogKeepsClipboardQrAndFileActions() {
        render(plans = true, populated = true)
        listOf("Import from Clipboard" to MainAction.ImportClipboard, "Import from QR code" to MainAction.ImportQRcode, "Import from a local file" to MainAction.ImportConfigLocal).forEach { (label, expected) ->
            compose.onNodeWithText("Add subscription").performClick()
            compose.onNodeWithText(label).performScrollTo().performClick()
            org.junit.Assert.assertEquals(expected, commands.last())
        }
    }
    @Test fun updateButtonSupportsEnter() {
        render(populated = true)
        compose.runOnIdle { inputModes.requestInputMode(InputMode.Keyboard) }
        val button = compose.onNodeWithText("Update Subscription")
        button.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        button.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        org.junit.Assert.assertEquals(MainAction.UpdateGroupSubscription(""), commands.last())
    }
    @Test fun subscriptionsHaveNoManageFooter() {
        render(plans = true, populated = true)
        compose.onNodeWithText("Manage").assertDoesNotExist()
        compose.onNodeWithText("Refresh").assertDoesNotExist()
        capture("subscriptions-clean-en")
    }
    @Test fun serverAndSettingsHeaderHaveNoImportOrOverflowIcons() {
        render(populated = true)
        compose.onNodeWithText("Servers").performClick()
        compose.onNodeWithContentDescription("Add configuration").assertDoesNotExist()
        compose.onNodeWithText("Update Subscription").assertDoesNotExist()
        capture("servers-clean-en")
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithContentDescription("Add configuration").assertDoesNotExist()
        capture("settings-clean-en")
    }

    @Test fun realDaemonSnapshotClockTicksBetweenThreeSecondSamples() {
        val timestamp = android.os.SystemClock.elapsedRealtime().coerceAtLeast(1)
        val snapshot = SessionTraffic(20, 100, 200, true, timestamp)
        compose.setContent {
            MaterialTheme {
                val seconds = rememberSessionSeconds(snapshot, true)
                Text(seconds?.toString() ?: "Unavailable")
            }
        }
        compose.onNodeWithText("20").assertExists()
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofSeconds(1))
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
        compose.onNodeWithText("21").assertExists()
    }

    @Test fun selectedSpecialVpnLogoAndMonochromeIcon() {
        compose.setContent {
            MaterialTheme(colorScheme = staticAppColorScheme(true), typography = AppTypography) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(32.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        Text("Special VPN", style = MaterialTheme.typography.headlineMedium)
                        androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(com.v2ray.ang.R.drawable.ispeed_logo), null, Modifier.size(260.dp))
                        Icon(androidx.compose.ui.res.painterResource(com.v2ray.ang.R.drawable.ic_ispeed_notification), null, Modifier.size(32.dp))
                    }
                }
            }
        }
        capture("special-vpn-branding")
    }

    @Test fun exitIpIsDisplayedAndRefreshUsesTheActionContract() {
        render(populated = true)
        compose.onNodeWithText("198.51.100.24").assertIsDisplayed()
        compose.onNodeWithContentDescription("Refresh exit IP").performClick()
        org.junit.Assert.assertEquals(MainAction.RefreshExitIp, commands.last())
        capture("home-exit-ip")
    }

    @Test fun exitIpLoadingDoesNotOfferConcurrentRefresh() {
        render(populated = true, exitIp = ExitIpState.Loading)
        compose.onNodeWithText("Checking…").assertIsDisplayed()
        compose.onNodeWithContentDescription("Refresh exit IP").assertDoesNotExist()
    }

    @Test fun exitIpUnavailableOffersRetry() {
        render(populated = true, exitIp = ExitIpState.Unavailable)
        compose.onNodeWithText("Unavailable").assertIsDisplayed()
        compose.onNodeWithContentDescription("Refresh exit IP").assertIsDisplayed()
    }

    @Test fun exitIpIsHiddenWhileDisconnected() {
        render()
        compose.onNodeWithText("Exit IP").assertDoesNotExist()
    }

    @Test fun countryIsLocalizedBesideExitIp() {
        render(populated = true, exitIp = ExitIpState.Address("198.51.100.24", "NL"))
        compose.onNodeWithText("Netherlands").assertIsDisplayed()
        capture("home-exit-country")
    }

    @Test @Config(qualifiers = "fa-rIR-w393dp-h852dp-xhdpi")
    fun countryBadgeAndWatermarkInPersian() {
        render(populated = true, rtl = true, exitIp = ExitIpState.Address("198.51.100.24", "NL"))
        compose.onNodeWithText("هلند").assertIsDisplayed()
        capture("home-country-fa")
    }

    @Test fun countryBadgeAndWatermarkInLightTheme() {
        render(populated = true, light = true, exitIp = ExitIpState.Address("198.51.100.24", "NL"))
        compose.onNodeWithText("Netherlands").assertIsDisplayed()
        capture("home-country-light")
    }

    @Test fun subscriptionEditAndShareUseTheChosenId() {
        render(plans = true, populated = true)
        compose.onAllNodesWithContentDescription("Edit").onFirst().performScrollTo().performClick()
        org.junit.Assert.assertEquals(MainAction.EditSubscription("plan"), commands.last())
        compose.onAllNodesWithContentDescription("Share subscription link").onFirst().performScrollTo().performClick()
        compose.onNodeWithText("QR code").performClick()
        org.junit.Assert.assertEquals(MainAction.ShareSubscription("plan", true), commands.last())
        compose.onAllNodesWithContentDescription("Share subscription link").onFirst().performScrollTo().performClick()
        compose.onNodeWithText("Export to clipboard").performClick()
        org.junit.Assert.assertEquals(MainAction.ShareSubscription("plan", false), commands.last())
        capture("subscriptions-edit-share")
    }


    @Test fun slimUpdateBannerOpensUpdaterAndDismisses() {
        render(populated = true, appUpdateFixture = com.v2ray.ang.dto.CheckUpdateResult(true, "1.6.3"))
        compose.onNodeWithText("New version: 1.6.3").assertIsDisplayed()
        capture("home-update-banner-en")
        compose.onNodeWithText("Update").performClick()
        org.junit.Assert.assertEquals(MainDestination.CheckUpdate, destinations.last())
        compose.onNodeWithContentDescription("Dismiss update notice").performClick()
        org.junit.Assert.assertEquals(MainAction.DismissAppUpdate, commands.last())
        compose.onNodeWithText("New version: 1.6.3").assertDoesNotExist()
    }

    @Test @Config(qualifiers = "fa-rIR-w393dp-h852dp-xhdpi") fun slimUpdateBannerPersian() {
        render(populated = true, rtl = true, appUpdateFixture = com.v2ray.ang.dto.CheckUpdateResult(true, "1.6.3"))
        compose.onNodeWithText("آپدیت").assertIsDisplayed()
        capture("home-update-banner-fa")
    }

    @Test fun serverDelayStatesKeepUntestedSeparateFromFailed() {
        compose.setContent { MaterialTheme(colorScheme = staticAppColorScheme(true)) { Column { ServerDelay(-1); ServerDelay(0); ServerDelay(42) } } }
        compose.onNodeWithText("Unreachable").assertIsDisplayed()
        compose.onNodeWithText("Not tested").assertIsDisplayed()
        compose.onNodeWithText("42 ms").assertIsDisplayed()
        capture("server-delay-states")
    }
    @Test fun actualControlsScreenHasNoConfigurationToolsAndKeepsManualUpdater() {
        val model = org.mockito.kotlin.mock<MainViewModel>()
        org.mockito.kotlin.whenever(model.uiState).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(MainUiState()))
        org.mockito.kotlin.whenever(model.isLoading).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(false))
        org.mockito.kotlin.whenever(model.serverGroupState(org.mockito.kotlin.any())).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(ServerGroupUiState()))
        org.mockito.kotlin.whenever(model.formatStatus(org.mockito.kotlin.any())).thenReturn("")
        compose.setContent { MaterialTheme(colorScheme = staticAppColorScheme(true), typography = AppTypography) { MainScreen(model, {}, { destinations.add(it) }, {}) } }
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Check for update").performScrollTo().performClick()
        org.junit.Assert.assertEquals(MainDestination.CheckUpdate, destinations.last())
        val footer = org.robolectric.RuntimeEnvironment.getApplication().getString(com.v2ray.ang.R.string.ispeed_attribution)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(footer))
        compose.onNodeWithText("Configuration tools").assertDoesNotExist()
        capture("controls-without-configuration-tools")
    }
}
