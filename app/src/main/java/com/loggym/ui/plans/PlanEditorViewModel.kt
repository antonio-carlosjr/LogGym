package com.loggym.ui.plans

import androidx.lifecycle.SavedStateHandle
import com.loggym.ui.util.UiOperation
import kotlinx.coroutines.flow.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loggym.domain.model.Exercise
import com.loggym.domain.model.Plan
import com.loggym.domain.model.Workout
import com.loggym.domain.model.WorkoutExercise
import com.loggym.domain.repository.ExerciseRepository
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

sealed interface PlanEditorUiState {
    data object Failed : PlanEditorUiState

    data object Loading : PlanEditorUiState

    /** O plano foi excluído enquanto o editor estava aberto. */
    data object NotFound : PlanEditorUiState

    data class Loaded(val plan: Plan) : PlanEditorUiState
}

sealed interface PlanEditorDialog {
    /** RF-25. */
    data class RenamePlan(val name: String, val showBlankError: Boolean = false) : PlanEditorDialog

    /** RF-41/RF-42: [workoutId] nulo cria uma nova divisão. */
    data class WorkoutName(
        val workoutId: Long?,
        val name: String = "",
        val showBlankError: Boolean = false,
    ) : PlanEditorDialog

    /** RF-43. */
    data class ConfirmDeleteWorkout(val workout: Workout) : PlanEditorDialog

    /** RF-46: escolha de exercício da biblioteca. */
    data class PickExercise(val workoutId: Long, val query: String = "") : PlanEditorDialog

    /** RF-49 a RF-57: [workoutExerciseId] nulo adiciona o exercício à divisão. */
    data class Target(
        val workoutId: Long,
        val workoutExerciseId: Long?,
        val exerciseId: Long,
        val exerciseName: String,
        val form: TargetForm = TargetForm(),
        val error: TargetFormError? = null,
    ) : PlanEditorDialog
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlanEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val planRepository: PlanRepository,
    exerciseRepository: ExerciseRepository,
) : ViewModel() {
    private val operations = UiOperation(viewModelScope)
    val busy = operations.busy
    val operationFailed = operations.failed
    fun dismissError() = operations.dismissError()


    /** Argumento da rota tipada `PlanEditorRoute(planId)`. */
    private val planId: Long = checkNotNull(savedStateHandle["planId"])

    private val refresh = MutableStateFlow(0)
    private val libraryFailure = MutableStateFlow(false)
    val libraryFailed = libraryFailure.asStateFlow()
    val uiState: StateFlow<PlanEditorUiState> = refresh.flatMapLatest {
        planRepository.observePlan(planId)
            .map { plan -> plan?.let(PlanEditorUiState::Loaded) ?: PlanEditorUiState.NotFound as PlanEditorUiState }
            .onStart { emit(PlanEditorUiState.Loading) }
            .catch { emit(PlanEditorUiState.Failed) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanEditorUiState.Loading)

    val library: StateFlow<List<Exercise>> = refresh.flatMapLatest {
        exerciseRepository.observeAll().onEach(::resolveCreated).onStart { libraryFailure.value = false }
            .catch { libraryFailure.value = true; emit(emptyList()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun retry() { refresh.update { it + 1 } }

    private val _dialog = MutableStateFlow<PlanEditorDialog?>(
        savedStateHandle.get<Long>("pickerWorkout")?.takeIf { savedStateHandle.get<Boolean>("pickerSuspended") != true }
            ?.let { PlanEditorDialog.PickExercise(it, savedStateHandle["pickerQuery"] ?: "") })
    val dialog: StateFlow<PlanEditorDialog?> = _dialog.asStateFlow()

    private val plan: Plan? get() = (uiState.value as? PlanEditorUiState.Loaded)?.plan

    fun dismissDialog() {
        savedStateHandle.remove<Long>("pickerWorkout")
        savedStateHandle.remove<Long>("pickerCreated")
        _dialog.value = null
    }

    // Plano

    fun openRenamePlan() {
        plan?.let { _dialog.value = PlanEditorDialog.RenamePlan(it.name) }
    }

    fun onPlanNameChange(name: String) {
        _dialog.update { if (it is PlanEditorDialog.RenamePlan) it.copy(name = name, showBlankError = false) else it }
    }

    fun saveRenamePlan() {
        val rename = _dialog.value as? PlanEditorDialog.RenamePlan ?: return
        if (rename.name.isBlank()) {
            _dialog.value = rename.copy(showBlankError = true)
            return
        }
        launchAndClose { planRepository.renamePlan(planId, rename.name) }
    }

    /** RF-31. */
    fun activatePlan() {
        operations.launch { planRepository.setActivePlan(planId) }
    }

    // Divisões

    fun openAddWorkout() {
        _dialog.value = PlanEditorDialog.WorkoutName(workoutId = null)
    }

    fun openRenameWorkout(workout: Workout) {
        _dialog.value = PlanEditorDialog.WorkoutName(workoutId = workout.id, name = workout.name)
    }

    fun onWorkoutNameChange(name: String) {
        _dialog.update { if (it is PlanEditorDialog.WorkoutName) it.copy(name = name, showBlankError = false) else it }
    }

    fun saveWorkoutName() {
        val dialog = _dialog.value as? PlanEditorDialog.WorkoutName ?: return
        if (dialog.name.isBlank()) {
            _dialog.value = dialog.copy(showBlankError = true)
            return
        }
        launchAndClose {
            if (dialog.workoutId == null) {
                planRepository.addWorkout(planId, dialog.name)
            } else {
                planRepository.renameWorkout(dialog.workoutId, dialog.name)
            }
        }
    }

    fun requestDeleteWorkout(workout: Workout) {
        _dialog.value = PlanEditorDialog.ConfirmDeleteWorkout(workout)
    }

    fun confirmDeleteWorkout() {
        val confirm = _dialog.value as? PlanEditorDialog.ConfirmDeleteWorkout ?: return
        launchAndClose { planRepository.deleteWorkout(confirm.workout.id) }
    }

    /** RF-44. */
    fun moveWorkout(workout: Workout, offset: Int) {
        val workouts = plan?.workouts ?: return
        val reordered = workouts.moved(workouts.indexOfFirst { it.id == workout.id }, offset) ?: return
        operations.launch { planRepository.reorderWorkouts(reordered.map { it.id }) }
    }

    // Exercícios da divisão

    fun openPickExercise(workout: Workout) {
        savedStateHandle["pickerWorkout"] = workout.id
        savedStateHandle["pickerQuery"] = ""
        rememberPickerPosition(0, 0)
        _dialog.value = PlanEditorDialog.PickExercise(workout.id)
    }

    fun onPickQueryChange(query: String) {
        savedStateHandle["pickerQuery"] = query
        rememberPickerPosition(0, 0)
        _dialog.update { if (it is PlanEditorDialog.PickExercise) it.copy(query = query) else it }
    }

    val pickerIndex: Int get() = savedStateHandle["pickerIndex"] ?: 0
    val pickerOffset: Int get() = savedStateHandle["pickerOffset"] ?: 0
    fun rememberPickerPosition(index: Int, offset: Int) {
        savedStateHandle["pickerIndex"] = index
        savedStateHandle["pickerOffset"] = offset
    }
    fun suspendPicker() {
        savedStateHandle["pickerSuspended"] = true
        _dialog.value = null
    }
    fun resumePicker(createdId: Long?) {
        if (savedStateHandle.get<Boolean>("pickerSuspended") != true) return
        val workout = savedStateHandle.get<Long>("pickerWorkout") ?: return
        savedStateHandle["pickerSuspended"] = false
        _dialog.value = PlanEditorDialog.PickExercise(workout, savedStateHandle["pickerQuery"] ?: "")
        if (createdId != null) {
            savedStateHandle["pickerCreated"] = createdId
            resolveCreated(library.value)
        }
    }
    private fun resolveCreated(exercises: List<Exercise>) {
        val id = savedStateHandle.get<Long>("pickerCreated") ?: return
        val exercise = exercises.firstOrNull { it.id == id } ?: return
        val workout = savedStateHandle.get<Long>("pickerWorkout") ?: return
        savedStateHandle["pickerQuery"] = exercise.name
        rememberPickerPosition(0, 0)
        _dialog.value = PlanEditorDialog.PickExercise(workout, exercise.name)
        savedStateHandle.remove<Long>("pickerCreated")
    }

    fun pickExercise(exercise: Exercise) {
        val pick = _dialog.value as? PlanEditorDialog.PickExercise ?: return
        savedStateHandle.remove<Long>("pickerWorkout")
        _dialog.value = PlanEditorDialog.Target(
            workoutId = pick.workoutId,
            workoutExerciseId = null,
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            form = TargetForm(trackingType = exercise.trackingType),
        )
    }

    fun openEditTarget(workout: Workout, item: WorkoutExercise) {
        _dialog.value = PlanEditorDialog.Target(
            workoutId = workout.id,
            workoutExerciseId = item.id,
            exerciseId = item.exercise.id,
            exerciseName = item.exercise.name,
            form = TargetForm.from(item.target),
        )
    }

    fun updateTargetForm(transform: (TargetForm) -> TargetForm) {
        _dialog.update { if (it is PlanEditorDialog.Target) it.copy(form = transform(it.form), error = null) else it }
    }

    fun saveTarget() {
        val dialog = _dialog.value as? PlanEditorDialog.Target ?: return
        when (val result = dialog.form.validate()) {
            is TargetFormResult.Invalid -> _dialog.value = dialog.copy(error = result.error)
            is TargetFormResult.Valid -> launchAndClose {
                if (dialog.workoutExerciseId == null) {
                    planRepository.addExerciseToWorkout(dialog.workoutId, dialog.exerciseId, result.target)
                } else {
                    planRepository.updateWorkoutExercise(dialog.workoutExerciseId, result.target)
                }
            }
        }
    }

    /** RF-47. */
    fun removeExercise(item: WorkoutExercise) {
        operations.launch { planRepository.removeExerciseFromWorkout(item.id) }
    }

    /** RF-48. */
    fun moveExercise(workout: Workout, item: WorkoutExercise, offset: Int) {
        val reordered = workout.exercises.moved(workout.exercises.indexOfFirst { it.id == item.id }, offset) ?: return
        operations.launch { planRepository.reorderWorkoutExercises(reordered.map { it.id }) }
    }

    private fun launchAndClose(action: suspend () -> Unit) {
        operations.launch {
            action()
            _dialog.value = null
        }
    }
}

/** Move o item em [index] por [offset] posições; nulo se sair dos limites. */
internal fun <T> List<T>.moved(index: Int, offset: Int): List<T>? {
    val target = index + offset
    if (index !in indices || target !in indices) return null
    return toMutableList().apply { add(target, removeAt(index)) }
}
