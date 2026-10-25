package com.loggym.ui.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loggym.domain.model.MuscleGroup
import com.loggym.domain.model.TrackingType
import com.loggym.domain.repository.*
import com.loggym.ui.util.UiOperation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class ExerciseCreateViewModel @Inject constructor(
    private val savedState: SavedStateHandle, private val repository: ExerciseRepository,
    private val persistence: com.loggym.domain.backup.PersistenceCoordinator = com.loggym.domain.backup.PersistenceCoordinator(),
) : ViewModel() {
    private val operations = UiOperation(viewModelScope, persistence)
    val busy = operations.busy
    val failed = operations.failed
    fun dismissError() = operations.dismissError()
    private val state = MutableStateFlow(ExerciseDialog.Editor(
        editingId = null, name = savedState["name"] ?: "", description = savedState["description"] ?: "",
        trackingType = TrackingType.entries.firstOrNull { it.key == savedState.get<String>("type") } ?: TrackingType.WEIGHT_REPS,
        muscleGroups = savedState.get<ArrayList<String>>("groups").orEmpty().mapNotNull(MuscleGroup::fromKey).toSet(),
    ))
    val editor = state.asStateFlow()
    val createdId = savedState.getStateFlow<Long?>("createdId", null)
    fun update(transform: (ExerciseDialog.Editor) -> ExerciseDialog.Editor) {
        if (busy.value || createdId.value != null) return
        val next = transform(state.value).copy(error = null)
        state.value = next
        savedState["name"] = next.name
        savedState["description"] = next.description
        savedState["type"] = next.trackingType.key
        savedState["groups"] = ArrayList(next.muscleGroups.map { it.key })
    }
    fun save() {
        if (createdId.value != null) return
        val form = state.value
        operations.launch {
            when (val result = repository.createCustom(ExerciseDraft(form.name, form.muscleGroups, form.description, form.trackingType))) {
                is SaveExerciseResult.Saved -> savedState["createdId"] = result.exerciseId
                else -> state.value = form.copy(error = result.editorError())
            }
        }
    }
}
