package com.loggym.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.loggym.MainDispatcherRule
import com.loggym.awaitFirst
import com.loggym.domain.logic.SetValues
import com.loggym.domain.model.*
import com.loggym.domain.repository.SessionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import java.io.IOException
import java.time.Instant

class HistoryViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val models = ViewModelStore()
    private val repository = FakeRepository()
    private fun <T : ViewModel> retain(model: T): T = ViewModelProvider(models, object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <R : ViewModel> create(modelClass: Class<R>): R = model as R
    })[model.javaClass]
    @After fun close() { models.clear() }

    @Test fun emptyHistoryAndRetryAfterLoadFailure() = runTest {
        repository.fail = true
        val vm = retain(HistoryViewModel(repository))
        assertTrue(vm.uiState.awaitFirst { it.failed }.sessions.isEmpty())
        repository.fail = false
        vm.retry()
        val state = vm.uiState.awaitFirst { !it.isLoading && !it.failed }
        assertTrue(state.sessions.isEmpty())
        repository.history.value = listOf(SessionSummary(1, "Plano", "A", Instant.EPOCH, Instant.EPOCH.plusSeconds(60)))
        assertEquals(1, vm.uiState.awaitFirst { it.sessions.isNotEmpty() }.sessions.single().id)
    }

    @Test fun detailHidesMissingAndDraftSessionsAndExposesCompletedSnapshot() = runTest {
        val vm = retain(HistoryDetailViewModel(SavedStateHandle(mapOf("sessionId" to 7L)), repository))
        assertNull(vm.uiState.awaitFirst { !it.isLoading }.session)
        val draft = Session(7, SessionStatus.DRAFT, "Plano antigo", "A antiga", Instant.EPOCH, null, null, emptyList())
        repository.session.value = draft
        assertNull(vm.uiState.awaitFirst { !it.isLoading }.session)
        val completed = draft.copy(status = SessionStatus.COMPLETED, finishedAt = Instant.EPOCH.plusSeconds(90))
        repository.session.value = completed
        assertEquals(completed, vm.uiState.awaitFirst { it.session != null }.session)
        repository.session.value = null
        assertNull(vm.uiState.awaitFirst { !it.isLoading && it.session == null }.session)
    }

    @Test fun detailCanRetryWithoutExposingAnErrorAsMissingData() = runTest {
        repository.fail = true
        val vm = retain(HistoryDetailViewModel(SavedStateHandle(mapOf("sessionId" to 7L)), repository))
        assertTrue(vm.uiState.awaitFirst { it.failed }.failed)
        repository.fail = false
        vm.retry()
        assertFalse(vm.uiState.awaitFirst { !it.isLoading && !it.failed }.failed)
    }

    private class FakeRepository : SessionRepository {
        var fail = false
        val history = MutableStateFlow<List<SessionSummary>>(emptyList())
        val session = MutableStateFlow<Session?>(null)
        override fun observeHistory() = flow { if (fail) throw IOException(); emitAll(history) }
        override fun observeSession(sessionId: Long) = flow { if (fail) throw IOException(); emitAll(session) }
        override fun observeDraft(): Flow<Session?> = session
        override suspend fun startSession(workoutId: Long): StartSessionResult = error("Não utilizado")
        override suspend fun updateSetValues(setId: Long, values: SetValues) = Unit
        override suspend fun saveSetAndSetCompleted(setId: Long, values: SetValues, completed: Boolean) = Unit
        override suspend fun addSet(sessionExerciseId: Long) = Unit
        override suspend fun clearExerciseSets(sessionExerciseId: Long) = false
        override suspend fun removeSet(setId: Long) = false
        override suspend fun skipRest(sessionId: Long) = Unit
        override suspend fun finishSession(sessionId: Long): FinishSessionResult = error("Não utilizado")
        override suspend fun discardSession(sessionId: Long) = false
    }
}
