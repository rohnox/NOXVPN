package com.v2ray.ang.ui.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModelStore
import com.v2ray.ang.R
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.dto.entities.SubscriptionCache
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.SubscriptionUsageSample
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.*

// Existing coroutines-test virtual Main APIs; remove opt-in when those dispatcher APIs stabilize.
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardSubscriptionsTest {
    @Test fun countryFlagUsesOnlyKnownCountryCodes() {
        assertEquals("🇳🇱", countryFlag("NL"))
        assertEquals("🇩🇪", countryFlag("DE"))
        listOf(null, "", "ZZ", "nl", "NLD").forEach { assertEquals("", countryFlag(it)) }
    }
    @Test fun sharingResolvesSubscriptionByIdAndRejectsMissingOrBlankLinks() = runTest {
        Fixture(testScheduler).use { f ->
            runCurrent()
            f.items[1].subscription.url = "  https://example.invalid/sub/test  "
            f.vm.onAction(MainAction.ShareSubscription("two", false)); runCurrent()
            verify(f.source).copySubscriptionLink("https://example.invalid/sub/test")
            assertEquals(R.string.toast_success, f.vm.uiState.value.subscriptionShareMessage)
            f.vm.onAction(MainAction.ShareSubscription("missing", false)); runCurrent()
            assertEquals(R.string.toast_failure, f.vm.uiState.value.subscriptionShareMessage)
            f.vm.onAction(MainAction.ShareSubscription("one", false)); runCurrent()
            verify(f.source, times(1)).copySubscriptionLink(any())
            f.vm.onAction(MainAction.ShareSubscription("two", true)); runCurrent()
            verify(f.source).createSubscriptionQr("https://example.invalid/sub/test")
            assertEquals(R.string.toast_failure, f.vm.uiState.value.subscriptionShareMessage)
            val bitmap = mock<android.graphics.Bitmap>()
            whenever(f.source.createSubscriptionQr(any())).thenReturn(bitmap)
            f.vm.onAction(MainAction.ShareSubscription("two", true)); runCurrent()
            assertSame(bitmap, f.vm.uiState.value.shareQRCodeBitmap)
            f.vm.onAction(MainAction.DismissQRCodeDialog); runCurrent()
            assertNull(f.vm.uiState.value.shareQRCodeBitmap)
            doThrow(IllegalStateException("test failure")).whenever(f.source).copySubscriptionLink(any())
            f.vm.onAction(MainAction.ShareSubscription("two", false)); runCurrent()
            assertEquals(R.string.toast_failure, f.vm.uiState.value.subscriptionShareMessage)
        }
    }

    @Test fun countryEnrichesOnlyTheCurrentExitAddress() = runTest {
        Fixture(testScheduler).use { f ->
            runCurrent()
            whenever(f.source.fetchExitIp(10809)).thenReturn("203.0.113.7")
            val reply = kotlinx.coroutines.CompletableDeferred<String?>()
            doSuspendableAnswer { reply.await() }.whenever(f.source).fetchExitCountry("203.0.113.7", 10809)
            f.events.emit(MainServiceEvent.StateRunning); runCurrent()
            f.events.emit(MainServiceEvent.Traffic(com.v2ray.ang.dto.SessionTraffic(httpProxyPort = 10809))); runCurrent()
            assertEquals(ExitIpState.Address("203.0.113.7"), f.vm.uiState.value.exitIp)
            f.events.emit(MainServiceEvent.StateStopSuccess); runCurrent()
            reply.complete("NL"); runCurrent()
            assertEquals(ExitIpState.Off, f.vm.uiState.value.exitIp)
        }
    }
    @Test fun exitIpUsesDaemonPortAndClearsOnStop() = runTest {
        Fixture(testScheduler).use { f ->
            runCurrent()
            assertEquals(ExitIpState.Off, f.vm.uiState.value.exitIp)
            f.vm.onAction(MainAction.RefreshExitIp); runCurrent()
            verify(f.source, never()).fetchExitIp(any())
            whenever(f.source.fetchExitIp(10809)).thenReturn("203.0.113.7")
            f.events.emit(MainServiceEvent.StateRunning); runCurrent()
            f.events.emit(MainServiceEvent.Traffic(com.v2ray.ang.dto.SessionTraffic(httpProxyPort = 10809))); runCurrent()
            assertEquals(ExitIpState.Address("203.0.113.7"), f.vm.uiState.value.exitIp)
            f.events.emit(MainServiceEvent.StateNotRunning); runCurrent()
            assertEquals(ExitIpState.Off, f.vm.uiState.value.exitIp)
        }
    }

    @Test fun exitIpMissingPortAndLookupFailureAreUnavailableAndCanRetry() = runTest {
        Fixture(testScheduler).use { f ->
            runCurrent(); f.events.emit(MainServiceEvent.StateRunning); runCurrent()
            f.events.emit(MainServiceEvent.Traffic(com.v2ray.ang.dto.SessionTraffic())); runCurrent()
            assertEquals(ExitIpState.Unavailable, f.vm.uiState.value.exitIp)
            verify(f.source, never()).fetchExitIp(any())
            whenever(f.source.fetchExitIp(10809)).thenThrow(IllegalStateException("test failure"))
            f.events.emit(MainServiceEvent.Traffic(com.v2ray.ang.dto.SessionTraffic(httpProxyPort = 10809))); runCurrent()
            f.vm.onAction(MainAction.RefreshExitIp); runCurrent()
            assertEquals(ExitIpState.Unavailable, f.vm.uiState.value.exitIp)
            doReturn("203.0.113.8").whenever(f.source).fetchExitIp(10809)
            f.vm.onAction(MainAction.RefreshExitIp); runCurrent()
            assertEquals(ExitIpState.Address("203.0.113.8"), f.vm.uiState.value.exitIp)
        }
    }

    @Test fun exitIpInFlightLookupIsCancelledWhenDaemonStops() = runTest {
        Fixture(testScheduler).use { f ->
            runCurrent()
            val reply = kotlinx.coroutines.CompletableDeferred<String?>()
            whenever(f.source.fetchExitIp(10809)).thenAnswer { null }
            doSuspendableAnswer { reply.await() }.whenever(f.source).fetchExitIp(10809)
            f.events.emit(MainServiceEvent.StateRunning); runCurrent()
            f.events.emit(MainServiceEvent.Traffic(com.v2ray.ang.dto.SessionTraffic(httpProxyPort = 10809))); runCurrent()
            assertEquals(ExitIpState.Loading, f.vm.uiState.value.exitIp)
            f.events.emit(MainServiceEvent.StateStopSuccess); runCurrent()
            reply.complete("203.0.113.9"); runCurrent()
            assertEquals(ExitIpState.Off, f.vm.uiState.value.exitIp)
        }
    }
    private class Fixture(scheduler: TestCoroutineScheduler, initial: String? = null) : AutoCloseable {
        val dispatcher = StandardTestDispatcher(scheduler)
        val source = mock<MainDataSource>()
        val events = MutableSharedFlow<MainServiceEvent>(extraBufferCapacity = 16)
        val items = mutableListOf(SubscriptionCache("one", SubscriptionItem(remarks = "One")), SubscriptionCache("two", SubscriptionItem(remarks = "Two")))
        var persisted = initial
        var selectedServer: String? = null
        val store = ViewModelStore()
        val log = Mockito.mockStatic(Log::class.java)
        private val priority = LogUtil::class.java.getDeclaredField("cachedMinPriority").apply { isAccessible = true }
        private val before = priority.getInt(null)
        val vm: MainViewModel
        init {
            priority.setInt(null, Log.ERROR); Dispatchers.setMain(dispatcher)
            whenever(source.mainServiceEvent).thenReturn(events)
            whenever(source.getSelectedSubscriptionId()).thenReturn("")
            whenever(source.getSubscriptions()).thenAnswer { items.toList() }
            whenever(source.getSubscriptionItem(any())).thenAnswer { call -> items.firstOrNull { it.guid == call.getArgument<String>(0) }?.subscription }
            whenever(source.getServerGuidList(any())).thenReturn(emptyList())
            whenever(source.getDashboardSubscriptionId()).thenAnswer { persisted }
            doAnswer { persisted = it.getArgument(0); null }.whenever(source).setDashboardSubscriptionId(any())
            doAnswer { call -> items.removeAll { it.guid == call.getArgument<String>(0) }; null }.whenever(source).removeSubscription(any())
            whenever(source.getSelectServer()).thenAnswer { selectedServer }
            vm = MainViewModel(mock<Application>(), source, dispatcher, dispatcher)
            store.put("main", vm)
        }
        override fun close() { store.clear(); priority.setInt(null, before); log.close(); Dispatchers.resetMain() }
    }
    @Test fun preferenceWinsOverConnectedServerAndInvalidPreferenceFallsBack() {
        val groups = listOf(GroupMapItem("one", "One"), GroupMapItem("two", "Two"))
        assertEquals("two", resolveDashboardSubscription(groups, "two", "one"))
        assertEquals("one", resolveDashboardSubscription(groups, "missing", "one"))
        assertEquals("one", resolveDashboardSubscription(groups, null, "missing"))
        assertEquals("", resolveDashboardSubscription(emptyList(), "missing", "one"))
    }
    @Test fun selectingAnotherSubscriptionPersistsInfoChoiceWithoutChangingVpnServer() = runTest {
        Fixture(testScheduler).use { f ->
            runCurrent(); assertEquals("one", f.vm.uiState.value.dashboardSubscriptionId)
            f.vm.onAction(MainAction.SelectDashboardSubscription("two")); runCurrent()
            assertEquals("two", f.persisted)
            assertEquals("two", dashboardPlan(f.vm.uiState.value)!!.id)
            assertEquals("two", f.vm.uiState.value.selectedGroupId)
            verify(f.source, never()).setSelectServer(any())
            assertFalse(f.vm.uiState.value.isRunning)
            f.vm.onAction(MainAction.RefreshGroups); runCurrent()
            assertEquals("two", f.vm.uiState.value.dashboardSubscriptionId)
            f.vm.onAction(MainAction.SelectDashboardSubscription("missing")); runCurrent()
            assertEquals("two", f.persisted)
        }
    }
    @Test fun savedChoiceRestoresByIdOnFreshModel() = runTest {
        Fixture(testScheduler, "two").use { f -> runCurrent(); assertEquals("two", dashboardPlan(f.vm.uiState.value)!!.id) }
    }
    @Test fun deletingSelectedSubscriptionChoosesRemainingAndHandlesLastAndStaleIds() = runTest {
        Fixture(testScheduler, "two").use { f ->
            runCurrent(); f.vm.onAction(MainAction.DeleteSubscription("two")); runCurrent()
            assertEquals(listOf("one"), f.vm.uiState.value.groups.map { it.id })
            assertEquals("one", f.persisted)
            f.vm.onAction(MainAction.DeleteSubscription("one")); runCurrent()
            assertTrue(f.vm.uiState.value.groups.isEmpty()); assertEquals("", f.persisted)
            f.vm.onAction(MainAction.DeleteSubscription("missing")); runCurrent()
            verify(f.source, never()).removeSubscription("missing")
        }
    }
    @Test fun connectedSubscriptionCannotBeDeleted() = runTest {
        Fixture(testScheduler).use { f ->
            runCurrent(); f.selectedServer = "server"
            whenever(f.source.decodeServerConfig("server")).thenReturn(ProfileItem(configType = com.v2ray.ang.enums.EConfigType.VLESS, subscriptionId = "one"))
            f.events.emit(MainServiceEvent.StateRunning); runCurrent()
            f.vm.onAction(MainAction.DeleteSubscription("one")); runCurrent()
            verify(f.source, never()).removeSubscription(any())
            assertEquals(R.string.top_delete_connected, f.vm.uiState.value.dashboardMessage)
        }
    }
    @Test fun mutationFailuresKeepUiSelectionAndExposeFailure() = runTest {
        Fixture(testScheduler, "one").use { f ->
            runCurrent()
            doThrow(IllegalStateException("fixture")).whenever(f.source).setDashboardSubscriptionId("two")
            f.vm.onAction(MainAction.SelectDashboardSubscription("two")); runCurrent()
            assertEquals("one", f.vm.uiState.value.dashboardSubscriptionId)
            assertNotNull(f.vm.uiState.value.dashboardMessage)
            doThrow(IllegalStateException("fixture")).whenever(f.source).removeSubscription("one")
            f.vm.onAction(MainAction.DeleteSubscription("one")); runCurrent()
            assertTrue(f.vm.uiState.value.groups.any { it.id == "one" })
        }
    }
    @Test fun chartSelectionUsesRealTimestampsWithBoundsAndNoInventedDays() {
        val points = listOf(SubscriptionUsageSample(10, 100), SubscriptionUsageSample(30, 300))
        assertEquals(10L, nearestProviderDay(points, -1f))
        assertEquals(30L, nearestProviderDay(points, 2f))
        assertEquals(30L, nearestProviderDay(points, .9f))
        assertNull(nearestProviderDay(emptyList(), .5f))
        assertNull(nearestProviderDay(points, Float.NaN))
        assertEquals(10L, nearestProviderDay(points.take(1), .5f))
    }
}
