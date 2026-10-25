package com.loggym.ui.history

import com.loggym.MainDispatcherRule
import com.loggym.StubSessionRepository
import com.loggym.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import java.time.Instant

class ExerciseHistoryViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    @Test fun retriesFailedNextPageWithoutLosingEntriesAndDoesNotDuplicateRequests() = runTest {
        var calls = 0; var fail = true
        val cursor = ExerciseHistoryCursor(Instant.EPOCH, 10)
        val entry = ExerciseHistoryEntry(SessionSummary(10, "P", "A", Instant.EPOCH, Instant.EPOCH), emptyList())
        val repository = object : StubSessionRepository() {
            override suspend fun getExerciseHistoryPage(exerciseId: Long, trackingType: TrackingType, cursor: ExerciseHistoryCursor?): ExerciseHistoryPage {
                calls++
                if (cursor == null) return ExerciseHistoryPage(listOf(entry), ExerciseHistoryCursor(Instant.EPOCH, 10))
                if (fail) throw java.io.IOException()
                return ExerciseHistoryPage(emptyList(), null)
            }
        }
        val vm = ExerciseHistoryViewModel(repository)
        vm.open(42, TrackingType.WEIGHT_REPS); assertEquals(listOf(entry), vm.uiState.value.entries)
        vm.open(42, TrackingType.WEIGHT_REPS); assertEquals(1, calls)
        assertEquals(cursor, vm.uiState.value.nextCursor)
        vm.more(); assertTrue(vm.uiState.value.failed); assertEquals(listOf(entry), vm.uiState.value.entries)
        fail = false; vm.retry(); assertFalse(vm.uiState.value.failed); assertNull(vm.uiState.value.nextCursor)
        vm.more(); assertEquals(3, calls)
    }
    @Test fun oldQueryCannotOverwriteNewSelectionAndClosingClearsState() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = object : StubSessionRepository() {
            override suspend fun getExerciseHistoryPage(exerciseId: Long, trackingType: TrackingType, cursor: ExerciseHistoryCursor?): ExerciseHistoryPage {
                if (exerciseId == 1L) withContext(NonCancellable) { gate.await() }
                return ExerciseHistoryPage(listOf(ExerciseHistoryEntry(SessionSummary(exerciseId, "P", "A", Instant.EPOCH, Instant.EPOCH), emptyList())), null)
            }
        }
        val vm = ExerciseHistoryViewModel(repository)
        vm.open(1, TrackingType.WEIGHT_REPS); vm.more()
        vm.open(2, TrackingType.TIME); gate.complete(Unit)
        assertEquals(2L, vm.uiState.value.exerciseId)
        assertEquals(2L, vm.uiState.value.entries.single().session.id)
        vm.close(); assertNull(vm.uiState.value.exerciseId); assertTrue(vm.uiState.value.entries.isEmpty())
    }
}
