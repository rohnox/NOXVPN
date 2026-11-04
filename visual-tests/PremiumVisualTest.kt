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

    private fun render(protocols: Boolean = false, rtl: Boolean = false, light: Boolean = false, populated: Boolean = false, notice: SubscriptionRefreshNotice? = null, plans: Boolean = false) {
        val history = (0L..30L).map { t -> SessionTraffic(t, t * 10240L, t * t * 4096L, true) }
        val quota = SubscriptionUsageInfo(title = "iSpecialVPN", expire = System.currentTimeMillis() / 1000 + 7 * 86400, upload = 2L shl 30, download = 12L shl 30, total = 40L shl 30, trafficAvailable = true, quotaAvailable = true, providerUsageUpdatedAt = System.currentTimeMillis() / 1000, providerUsage = (2L downTo 0L).map { day -> com.v2ray.ang.dto.entities.SubscriptionUsageSample(System.currentTimeMillis() / 1000 - day * 86400, (3L - day) * 104857600L) }, dailyUsage = (2L downTo 0L).map { day -> com.v2ray.ang.dto.entities.SubscriptionUsageSample(System.currentTimeMillis() / 1000 - day * 86400, (14L - day) shl 30) })
        val state = if (populated) MainUiState(groups = if (plans) listOf(GroupMapItem("plan", "iSpecialVPN", quota), GroupMapItem("family", "Family plan", quota.copy(title = "Family plan", upload = 1L shl 30, download = 6L shl 30, total = 30L shl 30, expire = System.currentTimeMillis() / 1000 + 18 * 86400)), GroupMapItem("work", "Work plan", quota.copy(title = "Work plan", upload = 1L shl 30, download = 3L shl 30, total = 20L shl 30, expire = System.currentTimeMillis() / 1000 + 9 * 86400))) else listOf(GroupMapItem("plan", "import sub", quota)), dashboardSubscriptionId = if (plans) "family" else "", selectedGuid = "a", selectedSubscriptionId = "plan",
            selectedServerName = "Germany · Frankfurt", isRunning = true, status = MainStatus.Connected, sessionTraffic = history.last(), trafficHistory = history) else MainUiState()
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
                    val action: (MainAction) -> Unit = { command ->
                        when (command) {
                            is MainAction.SelectDashboardSubscription -> displayed = displayed.copy(dashboardSubscriptionId = command.id)
                            is MainAction.DeleteSubscription -> {
                                val remaining = displayed.groups.filterNot { it.id == command.id }
                                displayed = displayed.copy(groups = remaining, dashboardSubscriptionId = resolveDashboardSubscription(remaining, displayed.dashboardSubscriptionId, displayed.selectedSubscriptionId))
                            }
                            else -> Unit
                        }
                    }
                    Scaffold(containerColor = MaterialTheme.colorScheme.background,
                        snackbarHost = { SubscriptionRefreshBanner(notice) },
                        topBar = { Column(Modifier.statusBarsPadding()) { DashboardHeader(if (tab == DashboardTab.Overview) ({}) else null, null) {} } },
                        bottomBar = { DashboardNavigation(tab) { tab = it } }) { padding ->
                        val modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
                        if (tab == DashboardTab.Protocols) ProtocolsScreen(ProtocolsState(busy = false, openVpnReady = true, ciscoReady = true, vpnMode = true,
                            profiles = if (populated) listOf(OpenVpnRow("profile", "Personal VPN")) else emptyList()), modifier) {}
                        else if (tab == DashboardTab.Plans) ISpeedPlans(displayed, modifier, action) {}
                        else ISpeedOverview(displayed, rows, "", modifier, {}, {}, {}, action, {}, {})
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
        compose.onNodeWithText("Open/Cisco").performClick()
        compose.onNodeWithText("Open/Cisco").assertIsSelected()
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
        val row = compose.onNode(hasText("Work plan") and hasClickAction()).performScrollTo()
        row.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        row.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        row.assertIsSelected()
    }
    @Test fun dpadSelectsSubscription() {
        render(plans = true, populated = true)
        compose.runOnIdle { inputModes.requestInputMode(InputMode.Keyboard) }
        val row = compose.onNode(hasText("Work plan") and hasClickAction()).performScrollTo()
        row.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        row.assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        row.assertIsSelected()
    }
    @Test @Config(qualifiers = "fa-rIR-w393dp-h852dp-xhdpi")
    fun subscriptionDesignThreePersian() { render(plans = true, populated = true, rtl = true); capture("subscriptions-fa") }
    @Test fun selectingSubscriptionChangesHomeInformation() {
        render(plans = true, populated = true)
        compose.onNode(hasText("Work plan") and hasClickAction()).performScrollTo().performClick().assertIsSelected()
        capture("subscriptions-work-selected")
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithText("Work plan").assertIsDisplayed()
        compose.onNodeWithText("4.0 GB").assertIsDisplayed()
        capture("home-work-selected")
    }
    @Test fun deleteSubscriptionFromSameTabRequiresConfirmation() {
        render(plans = true, populated = true)
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
}
