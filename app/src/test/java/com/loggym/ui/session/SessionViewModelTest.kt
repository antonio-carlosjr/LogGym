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
    private val persistence = com.loggym.domain.backup.PersistenceCoordinator()
    private val preferences = com.loggym.FakeUserPreferences()
    private var failPreferenceRead = false
    private val preferenceSource = object : com.loggym.domain.repository.UserPreferencesRepository by preferences {
        override val settings: Flow<UserSettings> get() = flow {
            if (failPreferenceRead) throw java.io.IOException()
            emitAll(preferences.settings)
        }
    }

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        recreateModel()
    }
    private fun recreateModel() {
        store.clear()
        vm = ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SessionViewModel(repository, clock, persistence, preferenceSource) as T
        })[SessionViewModel::class.java]
    }
    @After fun close() { store.clear(); Dispatchers.resetMain() }

    @Test fun defaultOffAndPrefillOrIdenticalCallbackNeverMakesASetEligible() = runTest(dispatcher) {
        repository.changeSet(10) { it.copy(weightKg = 0.0, reps = 8) }; recreateModel(); runCurrent()
        assertFalse(vm.uiState.value.autoCompleteEnabled)
        vm.edit(10) { it.copy(weight = "25") }; vm.finishEditing(10); runCurrent()
        assertEquals(0, repository.completionCalls)
        preferences.setAutoCompleteEnabled(true); runCurrent()
        vm.finishEditing(10); runCurrent(); assertEquals(0, repository.completionCalls)
        val revision = vm.uiState.value.entries[10L]!!.revision
        vm.edit(10) { it.copy(weight = it.weight) }; vm.finishEditing(10); runCurrent()
        assertEquals(revision, vm.uiState.value.entries[10L]!!.revision)
        assertEquals(0, repository.completionCalls)
    }
    @Test fun oneEditedFieldAfterDebounceCompletesOnceAndManualUncheckRequiresNewEdit() = runTest(dispatcher) {
        repository.changeSet(10) { it.copy(weightKg = 0.0, reps = 0) }; recreateModel(); runCurrent()
        preferences.setAutoCompleteEnabled(true); runCurrent()
        vm.edit(10) { it.copy(weight = "22,5") }; advanceTimeBy(500); runCurrent()
        assertFalse(vm.uiState.value.dirty)
        vm.finishEditing(10); vm.finishEditing(10); runCurrent(); vm.finishEditing(10); runCurrent()
        assertEquals(1, repository.completionCalls)
        assertEquals(SetValues(22.5, 0), repository.checkedValues)
        vm.toggle(10); runCurrent()
        vm.fieldFocused(10); vm.fieldFocused(11); vm.finishEditing(10); runCurrent()
        assertEquals(2, repository.completionCalls)
        assertFalse(repository.draft.value!!.exercises[0].sets[0].isCompleted)
        vm.edit(10) { it.copy(weight = "25") }; vm.finishEditing(10); runCurrent()
        assertEquals(3, repository.completionCalls)
    }
    @Test fun transferToOtherSeriesKeepsDestinationEditableWhileSourceCommits() = runTest(dispatcher) {
        runCurrent(); preferences.setAutoCompleteEnabled(true); runCurrent()
        vm.fieldFocused(10); vm.edit(10) { it.copy(weight = "0", reps = "0") }; vm.fieldFocused(10)
        runCurrent(); assertEquals(0, repository.completionCalls)
        val gate = CompletableDeferred<Unit>(); repository.checkGate = gate
        vm.fieldFocused(11); runCurrent()
        assertEquals(setOf(10L), vm.uiState.value.autoCompleting); assertFalse(vm.uiState.value.busy)
        vm.edit(11) { it.copy(duration = "45") }
        assertEquals("45", vm.uiState.value.entries[11L]!!.duration)
        gate.complete(Unit); runCurrent(); vm.finishEditing(11); runCurrent()
        assertTrue(repository.draft.value!!.exercises.all { it.sets.single().isCompleted })
        assertEquals(2, repository.completionCalls)
    }
    @Test fun missingFieldsAndInvalidDurationDoNotCompleteButManualIncompleteCheckStillWorks() = runTest(dispatcher) {
        runCurrent(); preferences.setAutoCompleteEnabled(true); runCurrent()
        vm.edit(10) { it.copy(weight = "0") }; vm.finishEditing(10)
        vm.edit(11) { it.copy(duration = "0") }; vm.finishEditing(11); runCurrent()
        assertEquals(0, repository.completionCalls); assertNotNull(vm.uiState.value.entries[11L]!!.error)
        vm.edit(11) { it.copy(duration = "45") }; vm.finishEditing(11); runCurrent()
        assertEquals(1, repository.completionCalls)
        vm.toggle(10); runCurrent(); assertTrue(repository.draft.value!!.exercises[0].sets[0].isCompleted)
        assertNotNull(vm.uiState.value.message)
    }
    @Test fun historyAndLifecycleAndBackupOnlySaveValuesWithoutCompleting() = runTest(dispatcher) {
        runCurrent(); preferences.setAutoCompleteEnabled(true); runCurrent()
        vm.fieldFocused(10); vm.edit(10) { it.copy(weight = "25", reps = "8") }
        vm.setHistoryOpen(true); vm.finishEditing(10); vm.fieldFocused(11); runCurrent()
        assertEquals(0, repository.completionCalls)
        vm.setHistoryOpen(false); vm.fieldFocused(11); runCurrent(); assertEquals(0, repository.completionCalls)
        vm.setActive(false); runCurrent()
        persistence.capture { assertEquals(SetValues(25.0, 8), repository.updates[10L]) }
        assertEquals(0, repository.completionCalls)
        vm.finishEditing(10); runCurrent(); assertEquals(1, repository.completionCalls)
    }
    @Test fun clearInvalidatesAutomaticEventWaitingBehindOlderSave() = runTest(dispatcher) {
        runCurrent(); preferences.setAutoCompleteEnabled(true); runCurrent()
        val gate = CompletableDeferred<Unit>(); repository.saveGate = gate
        vm.edit(10) { it.copy(weight = "20", reps = "8") }; advanceTimeBy(500); runCurrent()
        vm.edit(10) { it.copy(weight = "25") }; vm.finishEditing(10); runCurrent()
        vm.requestClearExercise(1); vm.confirmClearExercise(); runCurrent()
        gate.complete(Unit); runCurrent(); advanceTimeBy(1000); runCurrent()
        assertEquals(0, repository.completionCalls)
        assertNull(repository.draft.value!!.exercises[0].sets[0].weightKg)
        assertEquals("", vm.uiState.value.entries[10L]!!.weight)
    }
    @Test fun changingPreferenceInvalidatesQueuedIntentAndDoesNotCheckExistingValues() = runTest(dispatcher) {
        runCurrent(); preferences.setAutoCompleteEnabled(true); runCurrent()
        val gate = CompletableDeferred<Unit>(); repository.saveGate = gate
        vm.edit(10) { it.copy(weight = "20", reps = "8") }; advanceTimeBy(500); runCurrent()
        vm.edit(10) { it.copy(weight = "25") }; vm.finishEditing(10); runCurrent()
        preferences.setAutoCompleteEnabled(false); runCurrent(); gate.complete(Unit); runCurrent()
        assertEquals(0, repository.completionCalls)
        preferences.setAutoCompleteEnabled(true); runCurrent(); vm.finishEditing(10); runCurrent()
        assertEquals(0, repository.completionCalls)
    }
    @Test fun failedAutomaticWriteKeepsBuffersAndCanBeCheckedManually() = runTest(dispatcher) {
        runCurrent(); preferences.setAutoCompleteEnabled(true); runCurrent()
        vm.edit(10) { it.copy(weight = "25", reps = "8") }; repository.failWrites = true
        vm.finishEditing(10); runCurrent(); vm.finishEditing(10); runCurrent()
        assertEquals(1, repository.completionCalls); assertEquals("25", vm.uiState.value.entries[10L]!!.weight)
        assertFalse(repository.draft.value!!.exercises[0].sets[0].isCompleted)
        assertNotNull(vm.uiState.value.error); assertTrue(vm.uiState.value.autoCompleting.isEmpty())
        repository.failWrites = false; vm.toggle(10); runCurrent()
        assertTrue(repository.draft.value!!.exercises[0].sets[0].isCompleted)
    }
    @Test fun preferenceReadFailureDisablesAutomationAndCanRetryWithoutLosingManualControls() = runTest(dispatcher) {
        failPreferenceRead = true; recreateModel(); runCurrent()
        assertNotNull(vm.uiState.value.preferenceError); assertFalse(vm.uiState.value.autoCompleteEnabled)
        vm.edit(10) { it.copy(weight = "0", reps = "0") }; vm.finishEditing(10); runCurrent()
        assertEquals(0, repository.completionCalls)
        failPreferenceRead = false; preferences.setAutoCompleteEnabled(true); vm.retryPreference(); runCurrent()
        vm.finishEditing(10); runCurrent(); assertEquals(0, repository.completionCalls)
        vm.toggle(10); runCurrent(); assertEquals(1, repository.completionCalls)
    }
    @Test fun staleViewModelCannotSubmitAutomaticWriteAfterGenerationChange() = runTest(dispatcher) {
        runCurrent(); preferences.setAutoCompleteEnabled(true); runCurrent()
        vm.edit(10) { it.copy(weight = "25", reps = "8") }
        persistence.restore {}
        vm.finishEditing(10); runCurrent()
        assertEquals(0, repository.completionCalls); assertTrue(vm.uiState.value.autoCompleting.isEmpty())
    }

    @Test fun exportBeforeDebounceFlushesZeroTimeAndNullWithoutCheckingOrChangingRest() = runTest(dispatcher) {
        runCurrent()
        val timer = RestTimer(clock.instant(), clock.instant().plusSeconds(37))
        repository.draft.value = repository.draft.value!!.copy(restTimer = timer); runCurrent()
        vm.edit(10) { it.copy(weight = "0", reps = "0") }
        vm.edit(11) { it.copy(duration = "45") }
        persistence.capture {
            assertEquals(SetValues(0.0, 0), repository.updates[10L])
            assertEquals(SetValues(durationSeconds = 45), repository.updates[11L])
            vm.edit(10) { it.copy(weight = "99") }
            assertEquals("0", vm.uiState.value.entries[10L]!!.weight)
        }
        assertEquals(0L, testScheduler.currentTime)
        assertEquals(timer, repository.draft.value!!.restTimer)
        assertNull(repository.checkedValues); assertFalse(vm.uiState.value.busy)
        vm.edit(10) { it.copy(weight = "", reps = "") }
        persistence.capture { assertEquals(SetValues(), repository.updates[10L]) }
    }
    @Test fun exportWaitsForOldWriteThenFlushesLatestRevision() = runTest(dispatcher) {
        runCurrent(); val gate = CompletableDeferred<Unit>(); repository.saveGate = gate
        vm.edit(10) { it.copy(weight = "20", reps = "8") }; advanceTimeBy(500); runCurrent()
        vm.edit(10) { it.copy(weight = "25") }
        var captured = false
        val exporting = launch { persistence.capture { assertEquals(25.0, repository.updates[10L]!!.weightKg!!, 0.0); captured = true } }
        runCurrent(); assertFalse(captured); gate.complete(Unit); exporting.join()
        assertTrue(captured); assertFalse(vm.uiState.value.dirty)
    }
    @Test fun invalidOrFailedFlushBlocksSnapshotAndPreservesBuffersForRetry() = runTest(dispatcher) {
        runCurrent(); vm.edit(10) { it.copy(weight = "invalid") }
        var captured = false
        assertTrue(runCatching { persistence.capture { captured = true } }.isFailure)
        assertFalse(captured); assertEquals("invalid", vm.uiState.value.entries[10L]!!.weight)
        assertFalse(vm.uiState.value.busy)
        vm.edit(10) { it.copy(weight = "12,5") }; repository.failWrites = true
        assertTrue(runCatching { persistence.capture { captured = true } }.isFailure); assertTrue(vm.uiState.value.dirty)
        repository.failWrites = false; persistence.capture { captured = true }; assertTrue(captured)
    }
    @Test fun exportDrainsAlreadyAcceptedClearWithoutRestoringOldValues() = runTest(dispatcher) {
        runCurrent(); vm.edit(10) { it.copy(weight = "invalid") }
        vm.requestClearExercise(1); vm.confirmClearExercise()
        persistence.capture {
            assertNull(repository.draft.value!!.exercises[0].sets[0].weightKg)
            assertEquals("", vm.uiState.value.entries[10L]!!.weight)
        }
        advanceTimeBy(1000); runCurrent(); assertFalse(vm.uiState.value.dirty)
    }

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

    @Test fun restNoticeHidesFiveSecondsAfterEndAndOldNoticeDoesNotReappear() = runTest(dispatcher) {
        runCurrent()
        val now = clock.instant()
        repository.draft.value = repository.draft.value!!.copy(restTimer = RestTimer(now.minusSeconds(60), now.minusMillis(4999)))
        runCurrent()
        assertEquals(0L, vm.uiState.value.remainingSeconds)
        val expired = RestTimer(now.minusSeconds(60), now.minusSeconds(5))
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
        clock.now = clock.now.plusSeconds(4)
        advanceTimeBy(4_000); runCurrent()
        assertEquals(0L, vm.uiState.value.remainingSeconds)
        clock.now = clock.now.plusSeconds(1)
        advanceTimeBy(1_000); runCurrent()
        assertNull(vm.uiState.value.remainingSeconds)
        assertEquals(timer, repository.draft.value!!.restTimer)
        vm.setActive(false); runCurrent()
    }

    @Test fun clearCancelsDebounceAndInvalidInputWithoutTouchingOtherExercise() = runTest(dispatcher) {
        runCurrent()
        vm.edit(10) { it.copy(weight = "invalid") }
        vm.edit(11) { it.copy(duration = "45") }
        vm.requestClearExercise(1)
        vm.confirmClearExercise(); runCurrent()
        advanceTimeBy(1_000); runCurrent()
        assertEquals("", vm.uiState.value.entries[10L]!!.weight)
        assertFalse(vm.uiState.value.entries[10L]!!.dirty)
        assertFalse(repository.updates.containsKey(10))
        assertEquals(45, repository.updates[11L]!!.durationSeconds)
        assertNull(vm.uiState.value.dialog)
    }

    @Test fun clearWaitsForInFlightSaveAndDropsOldObservation() = runTest(dispatcher) {
        runCurrent()
        val gate = CompletableDeferred<Unit>()
        val observation = CompletableDeferred<Unit>()
        repository.saveGate = gate
        repository.observationGate = observation
        vm.edit(10) { it.copy(weight = "25", reps = "8") }
        advanceTimeBy(500); runCurrent()
        vm.requestClearExercise(1)
        vm.confirmClearExercise(); runCurrent()
        assertTrue(vm.uiState.value.busy)
        gate.complete(Unit); runCurrent()
        observation.complete(Unit); runCurrent()
        assertNull(repository.draft.value!!.exercises[0].sets[0].weightKg)
        assertNull(vm.uiState.value.session!!.exercises[0].sets[0].weightKg)
        assertEquals("", vm.uiState.value.entries[10L]!!.weight)
        assertFalse(vm.uiState.value.dirty)
    }

    @Test fun failedClearPreservesBuffersAndConfirmationThenCanRetry() = runTest(dispatcher) {
        runCurrent()
        vm.edit(10) { it.copy(weight = "12,5", reps = "0") }
        repository.failClear = true
        vm.requestClearExercise(1)
        vm.confirmClearExercise(); runCurrent()
        assertEquals("12,5", vm.uiState.value.entries[10L]!!.weight)
        assertEquals(SessionDialog.CLEAR_EXERCISE, vm.uiState.value.dialog)
        assertNotNull(vm.uiState.value.error)
        repository.failClear = false
        vm.confirmClearExercise(); runCurrent()
        assertEquals("", vm.uiState.value.entries[10L]!!.weight)
        assertNull(vm.uiState.value.dialog)
    }

    private class MutableClock(var now: Instant = Instant.parse("2026-10-22T12:00:00Z")) : Clock() {
        override fun instant() = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(now, zone)
    }

    private class FakeRepository : SessionRepository {
        override suspend fun getExerciseHistoryPage(exerciseId: Long, trackingType: TrackingType, cursor: ExerciseHistoryCursor?) = ExerciseHistoryPage(emptyList(), null)
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
        var observationGate: CompletableDeferred<Unit>? = null
        var failClear = false
        var completionCalls = 0
        var checkGate: CompletableDeferred<Unit>? = null
        override fun observeDraft(): Flow<Session?> = draft.map { session ->
            if (session?.exercises?.firstOrNull()?.sets?.firstOrNull()?.weightKg != null) observationGate?.await()
            session
        }
        override suspend fun clearExerciseSets(sessionExerciseId: Long): Boolean {
            check(!failClear)
            val session = draft.value ?: return false
            draft.value = session.copy(exercises = session.exercises.map { ex ->
                if (ex.id != sessionExerciseId) ex else ex.copy(sets = ex.sets.map {
                    it.copy(weightKg = null, reps = null, durationSeconds = null, isCompleted = false)
                })
            })
            return true
        }
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
            completionCalls++
            checkGate?.await()
            check(!failWrites)
            checkedValues = values
            changeSet(setId) { it.copy(weightKg = values.weightKg, reps = values.reps,
                durationSeconds = values.durationSeconds, isCompleted = completed) }
        }
        fun changeSet(id: Long, transform: (SessionSet) -> SessionSet) {
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
