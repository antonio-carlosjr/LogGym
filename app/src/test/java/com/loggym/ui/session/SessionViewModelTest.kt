package com.loggym.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.loggym.domain.logic.RestTimer
import com.loggym.domain.logic.SetValues
import com.loggym.domain.model.*
import com.loggym.domain.repository.SessionRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.time.*

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeRepository()
    private val clock = MutableClock()
    private val store = ViewModelStore()
    private lateinit var vm: SessionViewModel

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        vm = ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SessionViewModel(repository, clock) as T
        })[SessionViewModel::class.java]
    }
    @After fun close() { store.clear(); Dispatchers.resetMain() }

    @Test fun debounceIsPerSetAndCheckFlushesLatestValue() = runTest(dispatcher) {
        runCurrent()
        vm.edit(10) { it.copy(weight = "22,5", reps = "8") }
        advanceTimeBy(300)
        vm.edit(11) { it.copy(duration = "45") }
        advanceTimeBy(200); runCurrent()
        assertEquals(SetValues(22.5, 8), repository.updates[10L])
        assertFalse(repository.updates.containsKey(11L))
        vm.toggle(11); runCurrent()
        assertEquals(SetValues(durationSeconds = 45), repository.checkedValues)
        assertTrue(vm.uiState.value.session!!.exercises[1].sets[0].isCompleted)
        advanceTimeBy(1_000); runCurrent()
        assertFalse(repository.updates.containsKey(11L))
    }
    @Test fun staleRoomEmissionDoesNotReplaceDirtyText() = runTest(dispatcher) {
        runCurrent()
        vm.edit(10) { it.copy(weight = "23,") }
        repository.draft.value = repository.draft.value!!.copy(planName = "Atualizado")
        runCurrent()
        assertEquals("23,", vm.uiState.value.entries[10L]!!.weight)
        advanceTimeBy(500); runCurrent()
        assertNotNull(vm.uiState.value.entries[10L]!!.error)
        assertTrue(repository.updates.isEmpty())
        vm.back(); runCurrent()
        assertFalse(vm.uiState.value.exitReady)
    }
    @Test fun blankCheckWarnsAndPreservesNullWhileZeroIsValid() = runTest(dispatcher) {
        runCurrent()
        vm.toggle(11); runCurrent()
        assertEquals(SetValues(), repository.checkedValues)
        assertEquals(com.loggym.R.string.incomplete_set_warning, vm.uiState.value.message)
        assertEquals(1, vm.uiState.value.incompleteCount)
        vm.edit(10) { it.copy(weight = "0", reps = "0") }
        vm.toggle(10); runCurrent()
        assertEquals(SetValues(0.0, 0), repository.checkedValues)
    }
    @Test fun invalidDurationBlocksCheck() = runTest(dispatcher) {
        runCurrent()
        vm.edit(11) { it.copy(duration = "0") }
        vm.toggle(11); runCurrent()
        assertNull(repository.checkedValues)
        assertNotNull(vm.uiState.value.entries[11L]!!.error)
    }
    @Test fun failedWriteKeepsBufferAndRetryPersistsIt() = runTest(dispatcher) {
        runCurrent()
        repository.failWrites = true
        vm.edit(10) { it.copy(weight = "25", reps = "10") }
        advanceTimeBy(500); runCurrent()
        assertTrue(vm.uiState.value.dirty)
        assertNotNull(vm.uiState.value.error)
        repository.failWrites = false
        vm.retry(); runCurrent()
        assertFalse(vm.uiState.value.dirty)
        assertEquals(SetValues(25.0, 10), repository.updates[10L])
    }
    @Test fun removeAndDiscardCancelPendingDebounces() = runTest(dispatcher) {
        runCurrent()
        vm.edit(10) { it.copy(weight = "50") }
        vm.remove(10); runCurrent()
        advanceTimeBy(600); runCurrent()
        assertTrue(repository.updates.isEmpty())
        vm.edit(11) { it.copy(duration = "30") }
        vm.requestDiscard(); vm.confirmDiscard(); runCurrent()
        advanceTimeBy(600); runCurrent()
        assertTrue(repository.updates.isEmpty())
        assertTrue(vm.uiState.value.exitReady)
        assertNull(repository.draft.value)
    }
    @Test fun finishConfirmsIncompleteSetsAndOnlyExitsAfterCommit() = runTest(dispatcher) {
        runCurrent()
        vm.requestFinish(); runCurrent()
        assertFalse(vm.uiState.value.exitReady)
        assertNull(vm.uiState.value.dialog)
        vm.toggle(10); runCurrent()
        vm.requestFinish(); runCurrent()
        assertEquals(SessionDialog.FINISH, vm.uiState.value.dialog)
        repository.failFinish = true
        vm.confirmFinish(); runCurrent()
        assertFalse(vm.uiState.value.exitReady)
        assertNotNull(repository.draft.value)
        repository.failFinish = false
        vm.confirmFinish(); runCurrent()
        assertTrue(vm.uiState.value.exitReady)
    }
    @Test fun backgroundFlushesImmediatelyAndTimerUsesTimestamp() = runTest(dispatcher) {
        runCurrent()
        repository.draft.value = repository.draft.value!!.copy(restTimer = RestTimer(
            clock.instant(), clock.instant().plusMillis(1_500)))
        runCurrent()
        assertEquals(2L, vm.uiState.value.remainingSeconds)
        vm.edit(11) { it.copy(duration = "60") }
        vm.setActive(false); runCurrent()
        assertEquals(SetValues(durationSeconds = 60), repository.updates[11L])
        repository.draft.value = repository.draft.value!!.copy(restTimer = RestTimer(
            clock.instant().minusSeconds(10), clock.instant().minusSeconds(1)))
        runCurrent()
        assertEquals(0L, vm.uiState.value.remainingSeconds)
    }
    @Test fun editsDuringAnInFlightSaveRemainDirtyUntilLatestRevisionCommits() = runTest(dispatcher) {
        runCurrent()
        val gate = CompletableDeferred<Unit>()
        repository.saveGate = gate
        vm.edit(10) { it.copy(weight = "20") }
        advanceTimeBy(500); runCurrent()
        vm.edit(10) { it.copy(weight = "25") }
        gate.complete(Unit); runCurrent()
        assertEquals("25", vm.uiState.value.entries[10L]!!.weight)
        assertTrue(vm.uiState.value.entries[10L]!!.dirty)
        advanceTimeBy(500); runCurrent()
        assertEquals(25.0, repository.updates[10L]!!.weightKg!!, 0.0)
        assertFalse(vm.uiState.value.dirty)
    }

    @Test fun restNoticeHidesThreeSecondsAfterEndAndOldNoticeDoesNotReappear() = runTest(dispatcher) {
        runCurrent()
        val now = clock.instant()
        repository.draft.value = repository.draft.value!!.copy(restTimer = RestTimer(now.minusSeconds(60), now.minusMillis(2999)))
        runCurrent()
        assertEquals(0L, vm.uiState.value.remainingSeconds)
        val expired = RestTimer(now.minusSeconds(60), now.minusSeconds(3))
        repository.draft.value = repository.draft.value!!.copy(restTimer = expired)
        runCurrent()
        assertNull(vm.uiState.value.remainingSeconds)
        vm.setActive(true); runCurrent()
        assertNull(vm.uiState.value.remainingSeconds)
        assertEquals(expired, repository.draft.value!!.restTimer)
        repository.draft.value = repository.draft.value!!.copy(restTimer = RestTimer(now, now.plusSeconds(30)))
        runCurrent()
        assertEquals(30L, vm.uiState.value.remainingSeconds)
        vm.setActive(false); runCurrent()
    }

    @Test fun tickerHidesNoticeWithoutWritingOrReceivingAnotherDatabaseEmission() = runTest(dispatcher) {
        runCurrent()
        val timer = RestTimer(clock.instant(), clock.instant().plusSeconds(1))
        repository.draft.value = repository.draft.value!!.copy(restTimer = timer)
        vm.setActive(true); runCurrent()
        assertEquals(1L, vm.uiState.value.remainingSeconds)
        clock.now = clock.now.plusSeconds(1)
        advanceTimeBy(1_000); runCurrent()
        assertEquals(0L, vm.uiState.value.remainingSeconds)
        clock.now = clock.now.plusSeconds(2)
        advanceTimeBy(2_000); runCurrent()
        assertEquals(0L, vm.uiState.value.remainingSeconds)
        clock.now = clock.now.plusSeconds(1)
        advanceTimeBy(1_000); runCurrent()
        assertNull(vm.uiState.value.remainingSeconds)
        assertEquals(timer, repository.draft.value!!.restTimer)
        vm.setActive(false); runCurrent()
    }

    private class MutableClock(var now: Instant = Instant.parse("2026-10-22T12:00:00Z")) : Clock() {
        override fun instant() = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(now, zone)
    }

    private class FakeRepository : SessionRepository {
        val draft = MutableStateFlow<Session?>(Session(1, SessionStatus.DRAFT, "Plano", "A", Instant.EPOCH,
            null, null, listOf(
                SessionExercise(1, 1, "Halter", emptySet(), 0, ExerciseTarget.Repetitions(1, 8, 12, Duration.ofSeconds(30)),
                    listOf(SessionSet(10, 0, null, null, false))),
                SessionExercise(2, 2, "Prancha", emptySet(), 1, ExerciseTarget.Timed(1, 30, Duration.ofSeconds(30)),
                    listOf(SessionSet(11, 0, null, null, false))),
            )))
        val updates = mutableMapOf<Long, SetValues>()
        var checkedValues: SetValues? = null
        var failWrites = false
        var failFinish = false
        var saveGate: CompletableDeferred<Unit>? = null
        override fun observeDraft(): Flow<Session?> = draft
        override fun observeHistory(): Flow<List<SessionSummary>> = flowOf(emptyList())
        override fun observeSession(sessionId: Long): Flow<Session?> = draft
        override suspend fun startSession(workoutId: Long): StartSessionResult = StartSessionResult.DraftAlreadyExists(1)
        override suspend fun updateSetValues(setId: Long, values: SetValues) {
            saveGate?.await()
            check(!failWrites) { "Falha de gravação" }
            updates[setId] = values
            changeSet(setId) { it.copy(weightKg = values.weightKg, reps = values.reps, durationSeconds = values.durationSeconds) }
        }
        override suspend fun saveSetAndSetCompleted(setId: Long, values: SetValues, completed: Boolean) {
            check(!failWrites)
            checkedValues = values
            changeSet(setId) { it.copy(weightKg = values.weightKg, reps = values.reps,
                durationSeconds = values.durationSeconds, isCompleted = completed) }
        }
        private fun changeSet(id: Long, transform: (SessionSet) -> SessionSet) {
            draft.value = draft.value?.let { session -> session.copy(exercises = session.exercises.map { ex ->
                ex.copy(sets = ex.sets.map { if (it.id == id) transform(it) else it })
            }) }
        }
        override suspend fun addSet(sessionExerciseId: Long) = Unit
        override suspend fun removeSet(setId: Long): Boolean {
            draft.value = draft.value!!.copy(exercises = draft.value!!.exercises.map { ex -> ex.copy(sets = ex.sets.filterNot { it.id == setId }) })
            return true
        }
        override suspend fun skipRest(sessionId: Long) { draft.value = draft.value?.copy(restTimer = null) }
        override suspend fun finishSession(sessionId: Long): FinishSessionResult {
            check(!failFinish) { "Falha de finalização" }
            draft.value = null
            return FinishSessionResult.Finished
        }
        override suspend fun discardSession(sessionId: Long): Boolean { draft.value = null; return true }
    }
}
