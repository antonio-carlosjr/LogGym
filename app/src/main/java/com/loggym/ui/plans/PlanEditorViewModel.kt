package com.loggym.ui.plans

import androidx.lifecycle.SavedStateHandle
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

@HiltViewModel
class PlanEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val planRepository: PlanRepository,
    exerciseRepository: ExerciseRepository,
) : ViewModel() {

    /** Argumento da rota tipada `PlanEditorRoute(planId)`. */
    private val planId: Long = checkNotNull(savedStateHandle["planId"])

    val uiState: StateFlow<PlanEditorUiState> = planRepository.observePlan(planId)
        .map { plan -> plan?.let(PlanEditorUiState::Loaded) ?: PlanEditorUiState.NotFound }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanEditorUiState.Loading)

    val library: StateFlow<List<Exercise>> = exerciseRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _dialog = MutableStateFlow<PlanEditorDialog?>(null)
    val dialog: StateFlow<PlanEditorDialog?> = _dialog.asStateFlow()

    private val plan: Plan? get() = (uiState.value as? PlanEditorUiState.Loaded)?.plan

    fun dismissDialog() {
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
        viewModelScope.launch { planRepository.setActivePlan(planId) }
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
        viewModelScope.launch { planRepository.reorderWorkouts(reordered.map { it.id }) }
    }

    // Exercícios da divisão

    fun openPickExercise(workout: Workout) {
        _dialog.value = PlanEditorDialog.PickExercise(workout.id)
    }

    fun onPickQueryChange(query: String) {
        _dialog.update { if (it is PlanEditorDialog.PickExercise) it.copy(query = query) else it }
    }

    fun pickExercise(exercise: Exercise) {
        val pick = _dialog.value as? PlanEditorDialog.PickExercise ?: return
        _dialog.value = PlanEditorDialog.Target(
            workoutId = pick.workoutId,
            workoutExerciseId = null,
            exerciseId = exercise.id,
            exerciseName = exercise.name,
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
        viewModelScope.launch { planRepository.removeExerciseFromWorkout(item.id) }
    }

    /** RF-48. */
    fun moveExercise(workout: Workout, item: WorkoutExercise, offset: Int) {
        val reordered = workout.exercises.moved(workout.exercises.indexOfFirst { it.id == item.id }, offset) ?: return
        viewModelScope.launch { planRepository.reorderWorkoutExercises(reordered.map { it.id }) }
    }

    private fun launchAndClose(action: suspend () -> Unit) {
        viewModelScope.launch {
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
