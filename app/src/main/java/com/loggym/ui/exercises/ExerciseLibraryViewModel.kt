package com.loggym.ui.exercises

import com.loggym.ui.util.UiOperation
import kotlinx.coroutines.flow.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loggym.domain.model.TrackingType
import com.loggym.domain.model.Exercise
import com.loggym.domain.model.InputRules
import com.loggym.domain.model.MuscleGroup
import com.loggym.domain.repository.ExerciseDraft
import com.loggym.domain.repository.ExerciseRepository
import com.loggym.domain.repository.SaveExerciseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseLibraryUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val groupFilter: MuscleGroup? = null,
    val exercises: List<Exercise> = emptyList(),
    val failed: Boolean = false,
)

/** Diálogo aberto na tela; no máximo um por vez. */
sealed interface ExerciseDialog {
    data class Details(val exerciseId: Long) : ExerciseDialog

    data class Editor(
        val editingId: Long?,
        val name: String = "",
        val muscleGroups: Set<MuscleGroup> = emptySet(),
        val description: String = "",
        val error: EditorError? = null,
        val trackingType: TrackingType = TrackingType.WEIGHT_REPS,
    ) : ExerciseDialog

    data class ConfirmDelete(val exercise: Exercise, val planUsages: Int) : ExerciseDialog
}

enum class EditorError { BLANK_NAME, DUPLICATE_NAME, NOT_EDITABLE }

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExerciseLibraryViewModel @Inject constructor(
    private val repository: ExerciseRepository,
    private val persistence: com.loggym.domain.backup.PersistenceCoordinator = com.loggym.domain.backup.PersistenceCoordinator(),
) : ViewModel() {
    private val operations = UiOperation(viewModelScope, persistence)
    val busy = operations.busy
    val operationFailed = operations.failed
    fun dismissError() = operations.dismissError()


    private val query = MutableStateFlow("")
    private val groupFilter = MutableStateFlow<MuscleGroup?>(null)

    private val refresh = MutableStateFlow(0)
    private val source: StateFlow<ExerciseLibraryUiState> = refresh.flatMapLatest {
        repository.observeAll().map { ExerciseLibraryUiState(isLoading = false, exercises = it) }
            .onStart { emit(ExerciseLibraryUiState()) }
            .catch { emit(ExerciseLibraryUiState(isLoading = false, failed = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseLibraryUiState())

    val allExercises = source.map { it.exercises }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<ExerciseLibraryUiState> = combine(source, query, groupFilter) { source, query, group ->
        source.copy(query = query, groupFilter = group, exercises = filterExercises(source.exercises, query, group))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseLibraryUiState())
    fun retry() { refresh.update { it + 1 } }

    private val _dialog = MutableStateFlow<ExerciseDialog?>(null)
    val dialog: StateFlow<ExerciseDialog?> = _dialog.asStateFlow()

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onGroupFilterChange(group: MuscleGroup?) {
        groupFilter.value = group
    }

    fun openDetails(exercise: Exercise) {
        _dialog.value = ExerciseDialog.Details(exercise.id)
    }

    /** RF-13. */
    fun openCreate() {
        _dialog.value = ExerciseDialog.Editor(editingId = null)
    }

    /** RF-15/RF-18: só personalizados chegam aqui. */
    fun openEdit(exercise: Exercise) {
        if (!exercise.isCustom) return
        _dialog.value = ExerciseDialog.Editor(
            editingId = exercise.id,
            name = exercise.name,
            muscleGroups = exercise.muscleGroups,
            description = exercise.description.orEmpty(),
            trackingType = exercise.trackingType,
        )
    }

    fun updateEditor(transform: (ExerciseDialog.Editor) -> ExerciseDialog.Editor) {
        _dialog.update { current ->
            if (current is ExerciseDialog.Editor) transform(current).copy(error = null) else current
        }
    }

    fun saveEditor() {
        val editor = _dialog.value as? ExerciseDialog.Editor ?: return
        operations.launch {
            val draft = ExerciseDraft(editor.name, editor.muscleGroups, editor.description, editor.trackingType)
            val result = editor.editingId
                ?.let { repository.updateCustom(it, draft) }
                ?: repository.createCustom(draft)
            if (result is SaveExerciseResult.Saved) _dialog.value = null
            else result.editorError()?.let(::setEditorError)
        }
    }

    /** RF-133. */
    fun saveNote(exerciseId: Long, note: String) {
        operations.launch {
            repository.updateUserNote(exerciseId, note.take(InputRules.USER_NOTE_MAX_LENGTH))
        }
    }

    /** RF-17/RF-21: consulta o uso em planos antes de pedir confirmação. */
    fun requestDelete(exercise: Exercise) {
        if (!exercise.isCustom) return
        operations.launch {
            _dialog.value = ExerciseDialog.ConfirmDelete(exercise, repository.countPlanUsages(exercise.id))
        }
    }

    fun confirmDelete() {
        val confirm = _dialog.value as? ExerciseDialog.ConfirmDelete ?: return
        operations.launch {
            repository.deleteCustom(confirm.exercise.id)
            _dialog.value = null
        }
    }

    fun dismissDialog() {
        _dialog.value = null
    }

    private fun setEditorError(error: EditorError) {
        _dialog.update { current -> if (current is ExerciseDialog.Editor) current.copy(error = error) else current }
    }
}
