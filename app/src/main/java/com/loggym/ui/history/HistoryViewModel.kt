package com.loggym.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loggym.domain.model.Session
import com.loggym.domain.model.SessionStatus
import com.loggym.domain.model.SessionSummary
import com.loggym.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class HistoryUiState(val isLoading: Boolean = true, val sessions: List<SessionSummary> = emptyList(), val failed: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(private val repository: SessionRepository) : ViewModel() {
    private val refresh = MutableStateFlow(0)
    val uiState: StateFlow<HistoryUiState> = refresh.flatMapLatest {
        repository.observeHistory().map { HistoryUiState(isLoading = false, sessions = it) }
            .onStart { emit(HistoryUiState()) }
            .catch { emit(HistoryUiState(isLoading = false, failed = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())
    fun retry() { refresh.update { it + 1 } }
}

data class HistoryDetailUiState(val isLoading: Boolean = true, val session: Session? = null, val failed: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SessionRepository,
) : ViewModel() {
    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])
    private val refresh = MutableStateFlow(0)
    val uiState: StateFlow<HistoryDetailUiState> = refresh.flatMapLatest {
        repository.observeSession(sessionId).map { session ->
            // Uma rota restaurada não pode expor um rascunho como histórico (RF-117).
            HistoryDetailUiState(isLoading = false, session = session?.takeIf { it.status == SessionStatus.COMPLETED })
        }.onStart { emit(HistoryDetailUiState()) }
            .catch { emit(HistoryDetailUiState(isLoading = false, failed = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryDetailUiState())
    fun retry() { refresh.update { it + 1 } }
}
