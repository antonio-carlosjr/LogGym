package com.loggym.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loggym.domain.model.PlanTemplate
import com.loggym.domain.repository.PlanRepository
import com.loggym.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val preferences: UserPreferencesRepository,
    private val persistence: com.loggym.domain.backup.PersistenceCoordinator = com.loggym.domain.backup.PersistenceCoordinator(),
) : ViewModel() {
    private val persistenceContext = persistence.context()

    val templates: List<PlanTemplate> = planRepository.templates()

    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    private val failure = MutableStateFlow(false)
    val failed = failure.asStateFlow()
    private var createdTemplateKey: String? = null

    /** RF-04/RF-37: cria a cópia local do template e já a ativa. */
    fun selectTemplate(templateKey: String, onDone: () -> Unit) = completeOnce(onDone) {
        if (createdTemplateKey != templateKey) {
            planRepository.createFromTemplate(templateKey, activate = true)
            createdTemplateKey = templateKey
        }
    }

    /** RF-05/RF-06: criar plano personalizado ou pular também encerram o onboarding. */
    fun complete(onDone: () -> Unit) = completeOnce(onDone) {}

    private fun completeOnce(onDone: () -> Unit, action: suspend () -> Unit) {
        if (_isWorking.value || !persistence.canEdit) return
        _isWorking.value = true
        persistence.command(viewModelScope, persistenceContext) {
            failure.value = false
            try {
                action()
                preferences.setOnboardingCompleted()
                onDone()
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                failure.value = true
            } finally { _isWorking.value = false }
        }
    }
}
