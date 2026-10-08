package com.loggym.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loggym.domain.model.PlanSummary
import com.loggym.domain.model.PlanTemplate
import com.loggym.domain.repository.PlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlansUiState(
    val isLoading: Boolean = true,
    val plans: List<PlanSummary> = emptyList(),
)

sealed interface PlansDialog {
    /** RF-23/RF-24. */
    data class CreatePlan(val name: String = "", val showBlankError: Boolean = false) : PlansDialog

    /** RF-35/RF-37. */
    data object ChooseTemplate : PlansDialog

    /** RF-146: plano criado a partir de template fora do onboarding. */
    data class AskActivate(val planId: Long) : PlansDialog

    /** RF-27/RF-33. */
    data class ConfirmDelete(val plan: PlanSummary) : PlansDialog
}

@HiltViewModel
class PlansViewModel @Inject constructor(
    private val repository: PlanRepository,
) : ViewModel() {

    val templates: List<PlanTemplate> = repository.templates()

    val uiState: StateFlow<PlansUiState> = repository.observePlans()
        .map { PlansUiState(isLoading = false, plans = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlansUiState())

    private val _dialog = MutableStateFlow<PlansDialog?>(null)
    val dialog: StateFlow<PlansDialog?> = _dialog.asStateFlow()

    fun openCreatePlan() {
        _dialog.value = PlansDialog.CreatePlan()
    }

    fun onPlanNameChange(name: String) {
        _dialog.update { if (it is PlansDialog.CreatePlan) it.copy(name = name, showBlankError = false) else it }
    }

    /** Cria o plano vazio e abre o editor para adicionar as divisões (RF-39). */
    fun createPlan(onCreated: (Long) -> Unit) {
        val create = _dialog.value as? PlansDialog.CreatePlan ?: return
        if (create.name.isBlank()) {
            _dialog.value = create.copy(showBlankError = true)
            return
        }
        viewModelScope.launch {
            val planId = repository.createPlan(create.name)
            _dialog.value = null
            onCreated(planId)
        }
    }

    fun openTemplates() {
        _dialog.value = PlansDialog.ChooseTemplate
    }

    fun createFromTemplate(templateKey: String) {
        viewModelScope.launch {
            val planId = repository.createFromTemplate(templateKey, activate = false)
            _dialog.value = PlansDialog.AskActivate(planId)
        }
    }

    /** RF-28/RF-29/RF-31: ativar desativa o plano anterior. */
    fun activate(planId: Long) {
        viewModelScope.launch {
            repository.setActivePlan(planId)
            if ((_dialog.value as? PlansDialog.AskActivate)?.planId == planId) _dialog.value = null
        }
    }

    fun requestDelete(plan: PlanSummary) {
        _dialog.value = PlansDialog.ConfirmDelete(plan)
    }

    fun confirmDelete() {
        val confirm = _dialog.value as? PlansDialog.ConfirmDelete ?: return
        viewModelScope.launch {
            repository.deletePlan(confirm.plan.id)
            _dialog.value = null
        }
    }

    fun dismissDialog() {
        _dialog.value = null
    }
}
