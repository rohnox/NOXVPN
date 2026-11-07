package com.v2ray.ang.ui.protocols

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.v2ray.ang.ui.compose.staticAppColorScheme
import com.v2ray.ang.ui.compose.AppTypography
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35], qualifiers = "en-w393dp-h852dp-xhdpi")
class ProtocolSubscriptionsVisualTest {
    @get:Rule val compose = createComposeRule()
    private val commands = mutableListOf<ProtocolAction>()
    private lateinit var inputModes: InputModeManager
    @Test fun connectedTabDefaultsOnceAllowsBrowsingAndRestoresManualChoiceByProtocol() {
        val state = mutableStateOf(ProtocolsState(busy = false, openVpnReady = true, ciscoReady = true, ciscoActiveId = "cisco-id"))
        val restoration = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        restoration.setContent { MaterialTheme { ProtocolsScreen(state.value, Modifier.fillMaxSize(), {}) } }
        protocolTab("Cisco").assertIsSelected()
        protocolTab("WireGuard").performClick().assertIsSelected()
        compose.runOnIdle { state.value = state.value.copy(received = 42) }
        protocolTab("WireGuard").assertIsSelected()
        restoration.emulateSavedInstanceStateRestore()
        protocolTab("WireGuard").assertIsSelected()
        compose.runOnIdle { state.value = state.value.copy(ciscoActiveId = null) }
        compose.waitForIdle()
        compose.runOnIdle { state.value = state.value.copy(connected = true, activeProfileId = "open-id") }
        protocolTab("OpenVPN").assertIsSelected()
    }
    private fun fixture(light: Boolean = false, rtl: Boolean = false, editor: Boolean = false, empty: Boolean = false) {
        val profiles = listOf(SubscriptionProfile("ovpn-fixture", "Germany", ProfileProtocol.OpenVPN), SubscriptionProfile("cisco-fixture", "Sweden", ProfileProtocol.Cisco), SubscriptionProfile("wg-fixture", "Netherlands", ProfileProtocol.WireGuard))
        val record = ProtocolSubscription("plan", "Premium Plan", "https://example.invalid/sub/token/demo", 100, profiles, ProtocolSubscriptionInfo("demo-user", 1073741824L, 53687091200L, false, 1893542400L, false, null))
        val state = ProtocolsState(busy = false, openVpnReady = true, ciscoReady = true, vpnMode = true, subscriptions = if (empty) emptyList() else listOf(record), selectedSubscriptionId = "plan", status = com.v2ray.ang.R.string.protocol_state_stopped,
            editor = if (editor) SubscriptionEditor(title = "Example", link = "https://example.invalid/sub/token/demo") else null)
        compose.setContent {
            inputModes = LocalInputModeManager.current
            MaterialTheme(colorScheme = staticAppColorScheme(!light), typography = AppTypography) {
                CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    Surface { ProtocolsScreen(state, Modifier.fillMaxSize(), commands::add) }
                }
            }
        }
    }
    private fun protocolTab(name: String) = compose.onNode(hasText(name, substring = false) and SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Tab))
    private fun capture(name: String) {
        if (compose.onAllNodes(isRoot()).fetchSemanticsNodes().size > 1) return
        val root = compose.onRoot(useUnmergedTree = true)
        val target = File(System.getProperty("ispeed.visual.output"), "$name.png"); target.parentFile.mkdirs()
        File(target.parentFile, "$name.semantics.txt").writeText(root.printToString())
        if (compose.onAllNodes(isRoot()).fetchSemanticsNodes().size == 1) root.captureToImage().asAndroidBitmap().let { bitmap -> target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }
    @Test fun darkWireguardCardConnectAndUpdateDispatchStableIds() {
        fixture()
        compose.onNodeWithText("Username: demo-user").assertIsDisplayed()
        compose.onNodeWithText("Remaining").assertIsDisplayed()
        protocolTab("WireGuard").performClick().assertIsSelected()
        compose.onNodeWithText("Netherlands").assertIsDisplayed()
        compose.onNodeWithText("Germany").assertDoesNotExist()
        compose.onNodeWithText("Connect", substring = false).performScrollTo().performClick()
        assertEquals(ProtocolAction.ConnectManaged("wg-fixture"), commands.last())
        compose.onNodeWithText("Update Sub").performClick()
        assertEquals(ProtocolAction.RefreshSubscription("plan"), commands.last())
        capture("vpn-subscription-wireguard-en-dark")
    }
    @Test fun lightCiscoAndKeyboardConnect() {
        fixture(light = true)
        compose.onNodeWithText("Cisco", substring = false).performClick()
        compose.onNodeWithText("Sweden").assertIsDisplayed()
        compose.runOnIdle { inputModes.requestInputMode(InputMode.Keyboard) }
        val button = compose.onNodeWithText("Connect", substring = false)
        button.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        button.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        assertEquals(ProtocolAction.ConnectManaged("cisco-fixture"), commands.last())
        capture("vpn-subscription-cisco-en-light")
    }
    @Test @Config(qualifiers = "fa-rIR-w393dp-h852dp-xhdpi") fun persianRtlAndDpadProtocolSelection() {
        fixture(rtl = true)
        compose.runOnIdle { inputModes.requestInputMode(InputMode.Keyboard) }
        val tab = protocolTab("WireGuard")
        tab.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        tab.assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        tab.assertIsSelected()
        compose.onNodeWithText("Netherlands").assertIsDisplayed()
        capture("vpn-subscription-wireguard-fa-dark")
    }
    @Test fun emptyAndAddAction() {
        fixture(empty = true)
        compose.onNodeWithText("Add subscription").performClick()
        assertEquals(ProtocolAction.EditSubscription(null), commands.last())
        protocolTab("WireGuard").performClick()
        compose.onNodeWithText("Import .conf file").assertExists()
        capture("vpn-subscription-empty-en")
    }
    @Test fun editorSupportsFieldsClipboardSaveAndDismiss() {
        fixture(editor = true)
        compose.onNodeWithText("Import from Clipboard").performClick()
        assertEquals(ProtocolAction.PasteEditor, commands.last())
        compose.onNodeWithText("Subscription name").performTextReplacement("Changed")
        assertEquals(ProtocolAction.EditorTitle("Changed"), commands.last())
        compose.onAllNodesWithText("Add subscription").onLast().performClick()
        assertEquals(ProtocolAction.SaveEditor, commands.last())
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(ProtocolAction.CancelEditor, commands.last())
        capture("vpn-subscription-editor-en")
    }
}
