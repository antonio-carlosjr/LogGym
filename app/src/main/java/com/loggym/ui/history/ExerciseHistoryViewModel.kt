package com.loggym.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loggym.domain.model.*
import com.loggym.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ExerciseHistoryUiState(val exerciseId: Long? = null, val type: TrackingType? = null,
    val entries: List<ExerciseHistoryEntry> = emptyList(), val loading: Boolean = false,
    val failed: Boolean = false, val nextCursor: ExerciseHistoryCursor? = null)

@HiltViewModel
class ExerciseHistoryViewModel @Inject constructor(private val repository: SessionRepository) : ViewModel() {
    private val state = MutableStateFlow(ExerciseHistoryUiState())
    val uiState = state.asStateFlow()
    private var loading: Job? = null
    private var request = 0L
    fun open(id: Long, type: TrackingType) {
        if (state.value.exerciseId == id && state.value.type == type) return
        close()
        state.value = ExerciseHistoryUiState(exerciseId = id, type = type)
        load()
    }
    fun close() { request++; loading?.cancel(); loading = null; state.value = ExerciseHistoryUiState() }
    fun retry() = load()
    fun more() { if (state.value.nextCursor != null) load() }
    private fun load() {
        val current = state.value
        val id = current.exerciseId ?: return
        val type = current.type ?: return
        if (current.loading || (current.entries.isNotEmpty() && current.nextCursor == null)) return
        val ticket = ++request
        state.update { it.copy(loading = true, failed = false) }
        loading = viewModelScope.launch {
            try {
                val page = repository.getExerciseHistoryPage(id, type, current.nextCursor)
                if (ticket == request) state.update { it.copy(entries = current.entries + page.entries,
                    loading = false, nextCursor = page.nextCursor) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (ticket == request) state.update { it.copy(loading = false, failed = true) }
            }
        }
    }
}
