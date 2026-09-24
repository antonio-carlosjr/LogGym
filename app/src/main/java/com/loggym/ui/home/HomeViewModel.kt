package com.loggym.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loggym.domain.model.Plan
import com.loggym.domain.model.Session
import com.loggym.domain.model.StartSessionResult
import com.loggym.domain.repository.PlanRepository
import com.loggym.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val activePlan: Plan? = null,
    val draft: Session? = null,
    val message: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    planRepository: PlanRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        planRepository.observeActivePlan(),
        sessionRepository.observeDraft(),
        message,
    ) { plan, draft, message ->
        HomeUiState(isLoading = false, activePlan = plan, draft = draft, message = message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun startWorkout(workoutId: Long, onStarted: () -> Unit) {
        viewModelScope.launch {
            when (sessionRepository.startSession(workoutId)) {
                is StartSessionResult.Started -> onStarted()
                is StartSessionResult.DraftAlreadyExists ->
                    message.value = "Já existe um treino em andamento. Retome ou descarte antes de iniciar outro."
                StartSessionResult.NoActivePlan ->
                    message.value = "Nenhum plano ativo. Selecione ou crie um plano."
                StartSessionResult.WorkoutNotInActivePlan ->
                    message.value = "Esta divisão não pertence ao plano ativo."
            }
        }
    }

    fun discardDraft(sessionId: Long) {
        viewModelScope.launch { sessionRepository.discardSession(sessionId) }
    }

    fun onMessageShown() {
        message.value = null
    }
}
