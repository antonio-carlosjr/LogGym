package com.loggym.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loggym.domain.logic.SetValues
import com.loggym.domain.logic.WeightFormat
import com.loggym.domain.model.*
import com.loggym.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import javax.inject.Inject

/** Raw text stays local while Room emits previously committed values. */
data class SetEntry(
    val weight: String = "",
    val reps: String = "",
    val duration: String = "",
    val revision: Long = 0,
    val savedRevision: Long = 0,
    val error: String? = null,
) {
    val dirty: Boolean get() = revision != savedRevision
    fun values(type: TrackingType): SetValues {
        fun integer(text: String, positive: Boolean): Int? {
            if (text.isBlank()) return null
            return requireNotNull(text.toIntOrNull()?.takeIf { if (positive) it > 0 else it >= 0 }) {
                if (positive) "Informe segundos inteiros maiores que zero." else "Informe repetições inteiras não negativas."
            }
        }
        return if (type == TrackingType.TIME) SetValues(durationSeconds = integer(duration, true)) else {
            val kg = if (weight.isBlank()) null else requireNotNull(WeightFormat.parse(weight)?.takeIf { it.isFinite() }) {
                "Informe carga não negativa com até duas casas decimais."
            }
            SetValues(kg, integer(reps, false))
        }
    }
    companion object {
        fun from(set: SessionSet) = SetEntry(
            weight = set.weightKg?.let(WeightFormat::format).orEmpty(),
            reps = set.reps?.toString().orEmpty(), duration = set.durationSeconds?.toString().orEmpty(),
        )
    }
}

enum class SessionDialog { FINISH, DISCARD }
data class SessionUiState(
    val isLoading: Boolean = true,
    val session: Session? = null,
    val entries: Map<Long, SetEntry> = emptyMap(),
    val busy: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val dialog: SessionDialog? = null,
    val remainingSeconds: Long? = null,
    val exitReady: Boolean = false,
) {
    val dirty: Boolean get() = entries.values.any { it.dirty }
    val incompleteCount: Int get() = session?.exercises?.sumOf { exercise ->
        exercise.sets.count { set -> set.isCompleted &&
            SetValues(set.weightKg, set.reps, set.durationSeconds).isIncomplete(exercise.trackingType) }
    } ?: 0
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val repository: SessionRepository,
    private val clock: Clock,
) : ViewModel() {
    private val state = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = state.asStateFlow()
    private val mutex = Mutex()
    private val debounces = mutableMapOf<Long, Job>()
    private var ticker: Job? = null
    private var observing: Job? = null
    private var observationFailed = false

    init { observe() }

    private fun observe() {
        observationFailed = false
        observing?.cancel()
        observing = viewModelScope.launch {
            repository.observeDraft().catch { error ->
                observationFailed = true
                state.update { it.copy(isLoading = false, error = error.message ?: "Não foi possível abrir o treino.") }
            }.collect { session ->
                state.update { current ->
                    val entries = session?.exercises?.flatMap { it.sets }?.associate { set ->
                        set.id to (current.entries[set.id] ?: SetEntry.from(set))
                    }.orEmpty()
                    current.copy(isLoading = false, session = session, entries = entries)
                }
                tick()
            }
        }
    }

    fun setActive(active: Boolean) {
        ticker?.cancel()
        if (active) ticker = viewModelScope.launch {
            while (isActive) { tick(); delay(1_000) }
        } else {
            cancelDebounces()
            viewModelScope.launch { mutex.withLock { flush() } }
        }
    }

    private fun tick() {
        val remaining = state.value.session?.restTimer?.remaining(clock.instant())
        val seconds = remaining?.let { it.seconds + if (it.nano > 0) 1 else 0 }
        state.update { it.copy(remainingSeconds = seconds) }
    }

    fun edit(setId: Long, transform: (SetEntry) -> SetEntry) {
        if (state.value.busy || find(setId)?.second?.isCompleted != false) return
        val before = state.value.entries[setId] ?: return
        val next = transform(before).copy(revision = before.revision + 1, error = null)
        if (listOf(next.weight.substringBefore(',').substringBefore('.'), next.reps, next.duration)
                .any { text -> text.count(Char::isDigit) > 6 }) return
        state.update { it.copy(entries = it.entries + (setId to next)) }
        debounces.remove(setId)?.cancel()
        debounces[setId] = viewModelScope.launch {
            delay(500)
            // Do not cancel an in-flight database transaction when another character is typed.
            debounces.remove(setId)
            mutex.withLock { save(setId) }
        }
    }

    private fun find(id: Long): Pair<SessionExercise, SessionSet>? = state.value.session?.exercises
        ?.firstNotNullOfOrNull { exercise -> exercise.sets.firstOrNull { it.id == id }?.let { exercise to it } }

    private suspend fun save(id: Long): Boolean {
        val (exercise, set) = find(id) ?: return true
        val entry = state.value.entries[id] ?: return true
        if (!entry.dirty || set.isCompleted) return true
        val values = parse(id, entry, exercise.trackingType) ?: return false
        state.update { it.copy(saving = true) }
        return try {
            repository.updateSetValues(id, values)
            acknowledge(id, entry.revision)
            state.update { current -> if (!current.dirty) current.copy(error = null) else current }
            true
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            fail(error)
            false
        } finally { state.update { it.copy(saving = false) } }
    }

    private fun parse(id: Long, entry: SetEntry, type: TrackingType): SetValues? = try {
        entry.values(type)
    } catch (error: IllegalArgumentException) {
        state.update { current -> current.copy(entries = current.entries + (id to entry.copy(error = error.message))) }
        null
    }

    private fun acknowledge(id: Long, revision: Long) {
        state.update { current ->
            val entry = current.entries[id]
            if (entry == null) current else current.copy(entries = current.entries +
                (id to entry.copy(savedRevision = revision)))
        }
    }

    private suspend fun flush(): Boolean {
        var ok = true
        for (id in state.value.entries.keys.toList()) if (!save(id)) ok = false
        return ok
    }

    private fun cancelDebounces() { debounces.values.forEach { it.cancel() }; debounces.clear() }

    private fun operation(block: suspend () -> Unit) {
        if (state.value.busy || state.value.exitReady) return
        state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try { mutex.withLock { block() } }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                fail(error)
            } finally { state.update { it.copy(busy = false) } }
        }
    }

    private fun fail(error: Exception) { state.update { it.copy(error = error.message ?: "Falha ao salvar. Tente novamente.") } }

    fun toggle(id: Long) = operation {
        debounces.remove(id)?.cancel()
        val (exercise, set) = find(id) ?: return@operation
        val entry = state.value.entries[id] ?: return@operation
        val values = parse(id, entry, exercise.trackingType) ?: return@operation
        repository.saveSetAndSetCompleted(id, values, !set.isCompleted)
        acknowledge(id, entry.revision)
        // Reflect the successful commit immediately; a Room emission may arrive later.
        state.update { current -> current.copy(session = current.session?.copy(exercises = current.session.exercises.map { ex ->
            ex.copy(sets = ex.sets.map { item -> if (item.id != id) item else item.copy(
                weightKg = values.weightKg, reps = values.reps, durationSeconds = values.durationSeconds,
                isCompleted = !set.isCompleted,
            ) })
        }), message = if (!set.isCompleted && values.isIncomplete(exercise.trackingType))
            "Série marcada com campos incompletos." else null) }
    }

    fun add(exerciseId: Long) = operation {
        cancelDebounces()
        if (flush()) repository.addSet(exerciseId)
    }

    fun remove(id: Long) = operation {
        debounces.remove(id)?.cancel()
        if (repository.removeSet(id)) state.update { it.copy(entries = it.entries - id) }
    }

    fun skipRest() = operation { state.value.session?.let { repository.skipRest(it.id) } }
    fun back() = operation {
        cancelDebounces()
        if (flush()) state.update { it.copy(exitReady = true) }
    }
    fun retry() {
        if (state.value.session == null || observationFailed) { state.update { it.copy(isLoading = true, error = null) }; observe() }
        else operation { cancelDebounces(); flush() }
    }
    fun requestFinish() = operation {
        cancelDebounces()
        if (!flush()) return@operation
        val session = state.value.session ?: return@operation
        if (session.completedSetCount == 0) {
            state.update { it.copy(message = "Marque ao menos uma série antes de finalizar, ou descarte o treino.") }
        } else if (session.pendingSetCount > 0 || state.value.incompleteCount > 0) {
            state.update { it.copy(dialog = SessionDialog.FINISH) }
        } else finish()
    }
    private suspend fun finish() {
        val id = state.value.session?.id ?: return
        if (!flush()) return
        when (repository.finishSession(id)) {
            FinishSessionResult.Finished -> state.update { it.copy(exitReady = true, dialog = null) }
            FinishSessionResult.NoCompletedSets -> state.update { it.copy(message = "Marque ao menos uma série.", dialog = null) }
            FinishSessionResult.NotADraft -> error("O treino não está mais em andamento.")
        }
    }
    fun confirmFinish() = operation { cancelDebounces(); finish() }
    fun requestDiscard() { if (!state.value.busy) state.update { it.copy(dialog = SessionDialog.DISCARD) } }
    fun dismissDialog() { if (!state.value.busy) state.update { it.copy(dialog = null) } }
    fun confirmDiscard() = operation {
        cancelDebounces()
        val id = state.value.session?.id ?: return@operation
        check(repository.discardSession(id)) { "O treino não está mais em andamento." }
        state.update { it.copy(entries = emptyMap(), dialog = null, exitReady = true) }
    }
    fun messageShown() { state.update { it.copy(message = null) } }
}
