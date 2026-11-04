package com.v2ray.ang.ui.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModelStore
import com.v2ray.ang.dto.SubscriptionUpdateResult
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.*

// Virtual-time dispatcher testing; remove opt-in when coroutines-test dispatcher APIs become stable.
@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionEntryViewModelTest {
    private class Fixture(scheduler: TestCoroutineScheduler) : AutoCloseable {
        val dispatcher = StandardTestDispatcher(scheduler)
        val source = mock<MainDataSource>()
        val store = ViewModelStore()
        var now = 0L
        val log = Mockito.mockStatic(Log::class.java)
        private val priority = LogUtil::class.java.getDeclaredField("cachedMinPriority").apply { isAccessible = true }
        private val previous = priority.getInt(null)
        val vm: MainViewModel
        init {
            priority.setInt(null, Log.ERROR)
            Dispatchers.setMain(dispatcher)
            whenever(source.mainServiceEvent).thenReturn(MutableSharedFlow())
            whenever(source.getSelectedSubscriptionId()).thenReturn("")
            whenever(source.getSubscriptions()).thenReturn(emptyList())
            whenever(source.getServerGuidList(any())).thenReturn(emptyList())
            whenever(source.updateConfigViaSubAll()).thenReturn(SubscriptionUpdateResult())
            vm = MainViewModel(mock<Application>(), source, dispatcher, dispatcher, { now })
            store.put("main", vm)
        }
        override fun close() {
            store.clear()
            priority.setInt(null, previous)
            log.close()
            Dispatchers.resetMain()
        }
    }
    @Test fun entryFetchesAllAndCoalescesManualConcurrentRefresh() = runTest {
        Fixture(testScheduler).use { f ->
            runCurrent()
            f.vm.onAction(MainAction.AppEntered)
            f.vm.onAction(MainAction.UpdateGroupSubscription(""))
            f.vm.onAction(MainAction.AppEntered)
            runCurrent()
            verify(f.source, times(1)).updateConfigViaSubAll()
            assertFalse(f.vm.isLoading.value)
            f.now = 60_000
            f.vm.onAction(MainAction.AppEntered)
            runCurrent()
            verify(f.source, times(2)).updateConfigViaSubAll()
        }
    }
    @Test fun failedUpdateCanRetryWithoutFakeDataOrSelection() = runTest {
        Fixture(testScheduler).use { f ->
            whenever(f.source.updateConfigViaSubAll()).thenReturn(SubscriptionUpdateResult(failureCount = 1), SubscriptionUpdateResult(successCount = 1))
            runCurrent()
            f.vm.onAction(MainAction.AppEntered); runCurrent()
            assertEquals(SubscriptionRefreshNotice.Failed, f.vm.uiState.value.subscriptionRefresh)
            assertTrue(f.vm.uiState.value.groups.isEmpty())
            assertNull(f.vm.uiState.value.selectedGuid)
            f.now = 60_000
            f.vm.onAction(MainAction.AppEntered); runCurrent()
            verify(f.source, times(2)).updateConfigViaSubAll()
            assertEquals(SubscriptionRefreshNotice.Success, f.vm.uiState.value.subscriptionRefresh)
            assertFalse(f.vm.uiState.value.isRunning)
        }
    }
    @Test fun exceptionReleasesRefreshForNextEntry() = runTest {
        Fixture(testScheduler).use { f ->
            whenever(f.source.updateConfigViaSubAll()).thenThrow(IllegalStateException("test failure")).thenReturn(SubscriptionUpdateResult())
            runCurrent()
            f.vm.onAction(MainAction.AppEntered); runCurrent()
            f.now = 60_000
            f.vm.onAction(MainAction.AppEntered); runCurrent()
            verify(f.source, times(2)).updateConfigViaSubAll()
            assertFalse(f.vm.isLoading.value)
        }
    }
    @Test fun successfulNoticeExpiresAndOlderTimerCannotClearNewerResult() = runTest {
        Fixture(testScheduler).use { f ->
            whenever(f.source.updateConfigViaSubAll()).thenReturn(SubscriptionUpdateResult(successCount = 1))
            runCurrent(); f.vm.onAction(MainAction.AppEntered); runCurrent()
            assertEquals(SubscriptionRefreshNotice.Success, f.vm.uiState.value.subscriptionRefresh)
            advanceTimeBy(3000); f.now = 60_000
            f.vm.onAction(MainAction.AppEntered); runCurrent()
            advanceTimeBy(3000); runCurrent()
            assertEquals(SubscriptionRefreshNotice.Success, f.vm.uiState.value.subscriptionRefresh)
            advanceTimeBy(3000); runCurrent()
            assertNull(f.vm.uiState.value.subscriptionRefresh)
        }
    }
}
