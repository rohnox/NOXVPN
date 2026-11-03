package com.v2ray.ang.ui.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModelStore
import com.v2ray.ang.R
import com.v2ray.ang.dto.RealPingResult
import com.v2ray.ang.dto.TestServiceMessage
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.*
import java.io.IOException

// Test-only virtual-time Main dispatcher, pinned to the existing coroutines version.
// Reevaluate this opt-in when coroutines-test makes setMain/runCurrent/advanceTimeBy stable.
@OptIn(ExperimentalCoroutinesApi::class)
class AutoConnectViewModelTest {
    private class Fixture(scheduler: TestCoroutineScheduler) : AutoCloseable {
        val dispatcher = StandardTestDispatcher(scheduler)
        val events = MutableSharedFlow<MainServiceEvent>(extraBufferCapacity = 64)
        val profiles = linkedMapOf(
            "a" to ProfileItem(configType = EConfigType.VLESS, remarks = "A"),
            "b" to ProfileItem(configType = EConfigType.VLESS, remarks = "B"),
        )
        val writes = mutableListOf<String>()
        val sent = mutableListOf<Pair<TestServiceMessage, String>>()
        var selected: String? = null
        val source = mock<MainDataSource>()
        val store = ViewModelStore()
        val log = Mockito.mockStatic(Log::class.java)
        private val priority = LogUtil::class.java.getDeclaredField("cachedMinPriority").apply { isAccessible = true }
        private val previousPriority = priority.getInt(null)
        val vm: MainViewModel
        init {
            priority.setInt(null, Log.ERROR)
            Dispatchers.setMain(dispatcher)
            whenever(source.mainServiceEvent).thenReturn(events)
            whenever(source.getSelectedSubscriptionId()).thenReturn("")
            whenever(source.getSelectServer()).thenAnswer { selected }
            whenever(source.getSubscriptions()).thenReturn(emptyList())
            whenever(source.getServerGuidList(any())).thenAnswer { profiles.keys.toList() }
            whenever(source.decodeServerConfig(any())).thenAnswer { profiles[it.getArgument<String>(0)] }
            doAnswer { writes += it.getArgument<String>(0); selected = it.getArgument(0); null }
                .whenever(source).setSelectServer(any())
            doAnswer { sent += it.getArgument<TestServiceMessage>(0) to it.getArgument<String>(1); null }
                .whenever(source).sendMsg2TestService(any(), any())
            vm = MainViewModel(mock<Application>(), source, dispatcher, dispatcher)
            store.put("main", vm)
        }
        fun result(id: String, guid: String, delay: Long) { events.tryEmit(MainServiceEvent.MeasureConfigSuccess(RealPingResult(guid, delay), id)) }
        fun finish(id: String) { events.tryEmit(MainServiceEvent.MeasureConfigFinish(id)) }
        override fun close() {
            store.clear()
            priority.setInt(null, previousPriority)
            log.close()
            Dispatchers.resetMain()
        }
    }

    @Test fun `empty input never starts a test or fake connection`() = runTest {
        Fixture(testScheduler).use { f ->
            f.profiles.clear(); runCurrent()
            f.vm.onAction(MainAction.AutoConnect); runCurrent()
            assertTrue(f.sent.isEmpty()); assertTrue(f.writes.isEmpty())
            assertFalse(f.vm.uiState.value.isAutoSelecting)
            assertEquals(R.string.top_auto_empty, f.vm.uiState.value.autoMessage)
            assertFalse(f.vm.uiState.value.isRunning)
        }
    }
    @Test fun `fresh real probes persist fastest choice before single activation`() = runTest {
        Fixture(testScheduler).use { f ->
            runCurrent(); f.selected = "b"
            f.vm.onAction(MainAction.AutoConnect); runCurrent()
            val (message, id) = f.sent.single()
            assertFalse(message.onlyTcp)
            assertEquals(listOf("a", "b"), message.serverGuids)
            f.result(id, "a", 200); f.result(id, "b", 25); f.finish(id); runCurrent()
            assertEquals(listOf("b"), f.writes)
            val pending = f.vm.uiState.value.serverActivation!!
            assertEquals("b", pending.guid); assertTrue(pending.startIfStopped)
            assertFalse(f.vm.uiState.value.isRunning)
            assertNotNull(f.vm.consumeServerActivation(pending.generation))
            assertNull(f.vm.consumeServerActivation(pending.generation))
            assertFalse(f.vm.uiState.value.isRunning)
        }
    }
    @Test fun `failed tests never use an old cached ping or activation`() = runTest {
        Fixture(testScheduler).use { f ->
            runCurrent(); f.vm.onAction(MainAction.AutoConnect); runCurrent()
            val id = f.sent.single().second
            f.result(id, "a", -1); f.result(id, "b", 0); f.finish(id); runCurrent()
            assertTrue(f.writes.isEmpty()); assertNull(f.vm.uiState.value.serverActivation)
            assertEquals(R.string.top_auto_no_response, f.vm.uiState.value.autoMessage)
        }
    }
    @Test fun `manual selection cancels automatic intent and ignores late replies`() = runTest {
        Fixture(testScheduler).use { f ->
            runCurrent(); f.vm.onAction(MainAction.AutoConnect); runCurrent()
            val id = f.sent.single().second
            f.vm.onAction(MainAction.SelectServer("a")); runCurrent()
            f.result(id, "b", 1); f.finish(id); runCurrent()
            assertEquals(listOf("a"), f.writes)
            assertEquals("a", f.vm.uiState.value.selectedGuid)
            assertFalse(f.vm.uiState.value.serverActivation!!.startIfStopped)
            assertFalse(f.vm.uiState.value.isAutoSelecting)
        }
    }
    @Test fun `cancel or leaving screen invalidates pending and future activation`() = runTest {
        Fixture(testScheduler).use { f ->
            runCurrent(); f.vm.onAction(MainAction.AutoConnect); runCurrent()
            val id = f.sent.single().second
            f.result(id, "b", 10); f.finish(id); runCurrent()
            val pending = f.vm.uiState.value.serverActivation!!
            f.vm.cancelAutoConnect(); runCurrent()
            assertNull(f.vm.consumeServerActivation(pending.generation))
            assertNull(f.vm.uiState.value.serverActivation)
            assertFalse(f.vm.uiState.value.isRunning)
        }
    }
    @Test fun `removed or edited configuration is revalidated before selection`() = runTest {
        Fixture(testScheduler).use { f ->
            runCurrent(); f.vm.onAction(MainAction.AutoConnect); runCurrent()
            val id = f.sent.single().second
            f.result(id, "a", 100); f.result(id, "b", 5)
            f.profiles["b"] = f.profiles.getValue("b").copy(serverPort = "9999")
            f.finish(id); runCurrent()
            assertEquals(listOf("a"), f.writes)
            f.vm.cancelAutoConnect()
        }
    }
    @Test fun `deadline chooses completed responses and cancels outstanding probes`() = runTest {
        Fixture(testScheduler).use { f ->
            runCurrent(); f.vm.onAction(MainAction.AutoConnect); runCurrent()
            val id = f.sent.single().second
            f.result(id, "a", 90); runCurrent()
            advanceTimeBy(120_000); runCurrent()
            assertEquals(listOf("a"), f.writes)
            verify(f.source, times(2)).cancelAllPing()
            f.result(id, "b", 1); f.finish(id); runCurrent()
            assertEquals(listOf("a"), f.writes)
            f.vm.cancelAutoConnect()
        }
    }
    @Test fun `no response before deadline remains disconnected`() = runTest {
        Fixture(testScheduler).use { f ->
            runCurrent(); f.vm.onAction(MainAction.AutoConnect); runCurrent()
            advanceTimeBy(120_000); runCurrent()
            assertTrue(f.writes.isEmpty()); assertFalse(f.vm.uiState.value.isAutoSelecting)
            assertEquals(R.string.top_auto_no_response, f.vm.uiState.value.autoMessage)
        }
    }
    @Test fun `repository failure does not pretend to connect`() = runTest {
        Fixture(testScheduler).use { f ->
            runCurrent(); whenever(f.source.getServerGuidList(any())).thenAnswer { throw IOException("storage unavailable") }
            f.vm.onAction(MainAction.AutoConnect); runCurrent()
            assertTrue(f.sent.isEmpty()); assertTrue(f.writes.isEmpty())
            assertFalse(f.vm.uiState.value.isAutoSelecting)
            assertEquals(R.string.top_auto_error, f.vm.uiState.value.autoMessage)
        }
    }
    @Test fun `same manual server has no restart and later power action cancels activation`() = runTest {
        Fixture(testScheduler).use { f ->
            runCurrent(); f.selected = "a"
            f.vm.updateSelectedGuid("a"); runCurrent()
            assertNull(f.vm.uiState.value.serverActivation)
            f.vm.updateSelectedGuid("b"); runCurrent()
            val pending = f.vm.uiState.value.serverActivation!!
            f.vm.prepareUserAction(MainAction.ToggleService)
            assertNull(f.vm.consumeServerActivation(pending.generation))
        }
    }
    @Test fun `external daemon stop cancels pending automatic connection`() = runTest {
        Fixture(testScheduler).use { f ->
            runCurrent(); f.vm.onAction(MainAction.AutoConnect); runCurrent()
            val id = f.sent.single().second
            f.events.tryEmit(MainServiceEvent.StateStopSuccess); runCurrent()
            f.result(id, "b", 1); f.finish(id); runCurrent()
            assertFalse(f.vm.uiState.value.isAutoSelecting)
            assertNull(f.vm.uiState.value.serverActivation)
            assertTrue(f.writes.isEmpty())
        }
    }
    @Test fun `probing preserves an existing daemon connection until activation`() = runTest {
        Fixture(testScheduler).use { f ->
            runCurrent(); f.events.tryEmit(MainServiceEvent.StateRunning); runCurrent()
            f.vm.onAction(MainAction.AutoConnect); runCurrent()
            assertTrue(f.vm.uiState.value.isRunning)
            verify(f.source, never()).sendMsg2Service(any(), any())
            f.vm.cancelAutoConnect()
            assertTrue(f.vm.uiState.value.isRunning)
        }
    }
    @Test fun `unconfirmed selection persistence cannot request a connection`() = runTest {
        Fixture(testScheduler).use { f ->
            runCurrent()
            doAnswer { null }.whenever(f.source).setSelectServer(any())
            f.vm.onAction(MainAction.AutoConnect); runCurrent()
            val id = f.sent.single().second
            f.result(id, "a", 50); f.finish(id); runCurrent()
            assertNull(f.vm.uiState.value.serverActivation)
            assertFalse(f.vm.uiState.value.isAutoSelecting)
            assertFalse(f.vm.uiState.value.isRunning)
            assertEquals(R.string.top_auto_error, f.vm.uiState.value.autoMessage)
        }
    }
    @Test fun `changed UI selection cannot leave a ready automatic intent waiting indefinitely`() = runTest {
        Fixture(testScheduler).use { f ->
            runCurrent(); f.vm.onAction(MainAction.AutoConnect); runCurrent()
            val id = f.sent.single().second
            f.result(id, "b", 20); f.finish(id); runCurrent()
            val pending = f.vm.uiState.value.serverActivation!!
            f.selected = "a"; f.vm.refreshSelectedGuid(); runCurrent()
            assertNull(f.vm.consumeServerActivation(pending.generation))
            assertNull(f.vm.uiState.value.serverActivation)
            assertFalse(f.vm.uiState.value.isAutoSelecting)
            assertFalse(f.vm.uiState.value.isRunning)
        }
    }
}
