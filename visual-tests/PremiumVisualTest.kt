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

    private fun render(protocols: Boolean = false, rtl: Boolean = false, light: Boolean = false, populated: Boolean = false, notice: SubscriptionRefreshNotice? = null) {
        val history = (0L..30L).map { t -> SessionTraffic(t, t * 10240L, t * t * 4096L, true) }
        val quota = SubscriptionUsageInfo(title = "iSpecialVPN", expire = System.currentTimeMillis() / 1000 + 7 * 86400, upload = 2L shl 30, download = 12L shl 30, total = 40L shl 30, trafficAvailable = true, quotaAvailable = true, dailyUsage = (2L downTo 0L).map { day -> com.v2ray.ang.dto.entities.SubscriptionUsageSample(System.currentTimeMillis() / 1000 - day * 86400, (14L - day) shl 30) })
        val state = if (populated) MainUiState(groups = listOf(GroupMapItem("plan", "import sub", quota)), selectedGuid = "a", selectedSubscriptionId = "plan",
            selectedServerName = "Germany · Frankfurt", isRunning = true, status = MainStatus.Connected, sessionTraffic = history.last(), trafficHistory = history) else MainUiState()
        val rows = if (populated) listOf("Germany · Frankfurt", "Turkey · Istanbul").mapIndexed { i, name ->
            ServerRowUiModel(if (i == 0) "a" else "b", ProfileItem(configType = EConfigType.VLESS), name, "", "VLESS / TLS", 34L + i * 15L, "")
        } else emptyList()
        compose.setContent {
            MaterialTheme(colorScheme = staticAppColorScheme(!light), typography = AppTypography) {
                CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    var tab by remember { mutableStateOf(if (protocols) DashboardTab.Protocols else DashboardTab.Overview) }
                    Scaffold(containerColor = MaterialTheme.colorScheme.background,
                        snackbarHost = { SubscriptionRefreshBanner(notice) },
                        topBar = { Column(Modifier.statusBarsPadding()) { DashboardHeader(if (tab == DashboardTab.Overview) ({}) else null, null) {} } },
                        bottomBar = { DashboardNavigation(tab) { tab = it } }) { padding ->
                        val modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
                        if (tab == DashboardTab.Protocols) ProtocolsScreen(ProtocolsState(busy = false, openVpnReady = true, ciscoReady = true, vpnMode = true,
                            profiles = if (populated) listOf(OpenVpnRow("profile", "Personal VPN")) else emptyList()), modifier) {}
                        else ISpeedOverview(state, rows, "", modifier, {}, {}, {}, {}, {}, {})
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
        File(target.parentFile, "$name.semantics.txt").writeText(compose.onRoot().printToString())
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun homeEnglish() { render(populated = true); compose.onNodeWithText("iSpecialVPN").assertIsDisplayed(); compose.onNodeWithText("Subscription quota").assertIsDisplayed(); compose.onNodeWithText("7 days remaining").assertIsDisplayed(); compose.onNodeWithText("import sub").assertDoesNotExist(); capture("home-en-dark") }
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
}
