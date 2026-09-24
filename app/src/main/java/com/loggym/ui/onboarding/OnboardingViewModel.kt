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
) : ViewModel() {

    val templates: List<PlanTemplate> = planRepository.templates()

    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    /** RF-04/RF-37: cria a cópia local do template e já a ativa. */
    fun selectTemplate(templateKey: String, onDone: () -> Unit) = completeOnce(onDone) {
        planRepository.createFromTemplate(templateKey, activate = true)
    }

    /** RF-05/RF-06: criar plano personalizado ou pular também encerram o onboarding. */
    fun complete(onDone: () -> Unit) = completeOnce(onDone) {}

    private fun completeOnce(onDone: () -> Unit, action: suspend () -> Unit) {
        if (_isWorking.value) return
        _isWorking.value = true
        viewModelScope.launch {
            action()
            preferences.setOnboardingCompleted()
            onDone()
        }
    }
}
