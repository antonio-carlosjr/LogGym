package com.loggym.ui.home

import com.loggym.R
import kotlinx.coroutines.flow.*
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
    val message: Int? = null,
    val isBusy: Boolean = false,
    val failed: Boolean = false,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    planRepository: PlanRepository,
    private val sessionRepository: SessionRepository,
    private val persistence: com.loggym.domain.backup.PersistenceCoordinator = com.loggym.domain.backup.PersistenceCoordinator(),
) : ViewModel() {
    private val persistenceContext = persistence.context()


    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<Int?>(null)

    private val refresh = MutableStateFlow(0)
    fun retry() { refresh.update { it + 1 } }

    val uiState: StateFlow<HomeUiState> = refresh.flatMapLatest { combine(
        planRepository.observeActivePlan(),
        sessionRepository.observeDraft(),
        message,
        busy,
    ) { plan, draft, message, busy ->
        HomeUiState(isLoading = false, activePlan = plan, draft = draft, message = message, isBusy = busy)
    }.onStart { emit(HomeUiState()) }.catch { emit(HomeUiState(isLoading = false, failed = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun startWorkout(workoutId: Long, onStarted: () -> Unit) {
        if (!persistence.canEdit || busy.value) return
        busy.value = true
        persistence.command(viewModelScope, persistenceContext) {
            try {
                when (sessionRepository.startSession(workoutId)) {
                    is StartSessionResult.Started -> onStarted()
                    is StartSessionResult.DraftAlreadyExists ->
                        message.value = R.string.draft_exists_warning
                    StartSessionResult.NoActivePlan ->
                        message.value = R.string.no_active_plan_warning
                    StartSessionResult.WorkoutNotInActivePlan ->
                        message.value = R.string.wrong_workout_warning
                }
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                message.value = R.string.session_start_error
            } finally { busy.value = false }
        }
    }

    fun discardDraft(sessionId: Long) {
        if (!persistence.canEdit || busy.value) return
        busy.value = true
        persistence.command(viewModelScope, persistenceContext) {
            try {
                check(sessionRepository.discardSession(sessionId)) { "O treino não está mais em andamento." }
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                message.value = R.string.session_discard_error
            } finally { busy.value = false }
        }
    }

    fun onMessageShown() {
        message.value = null
    }
}
