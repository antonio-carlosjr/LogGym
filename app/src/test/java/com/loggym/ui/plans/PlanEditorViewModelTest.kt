package com.loggym.ui.plans

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loggym.MainDispatcherRule
import com.loggym.awaitFirst
import com.loggym.data.local.LogGymDatabase
import com.loggym.data.local.fixedClock
import com.loggym.data.local.inMemoryDatabase
import com.loggym.data.repository.ExerciseRepositoryImpl
import com.loggym.data.repository.PlanRepositoryImpl
import com.loggym.domain.model.InputRules
import com.loggym.domain.model.Plan
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlanEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: LogGymDatabase
    private lateinit var plans: PlanRepositoryImpl
    private lateinit var viewModel: PlanEditorViewModel
    private var planId = 0L

    @Before
    fun setUp() = runTest {
        database = inMemoryDatabase()
        plans = PlanRepositoryImpl(database, database.planDao(), database.exerciseDao(), fixedClock)
        planId = plans.createPlan("Meu plano")
        viewModel = PlanEditorViewModel(
            SavedStateHandle(mapOf("planId" to planId)),
            plans,
            ExerciseRepositoryImpl(database, database.exerciseDao()),
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `divisao sem nome e recusada e com nome e adicionada - RF-40 e RF-41`() = runTest {
        viewModel.openAddWorkout()
        viewModel.saveWorkoutName()
        assertTrue((viewModel.dialog.value as PlanEditorDialog.WorkoutName).showBlankError)

        viewModel.onWorkoutNameChange("Push")
        viewModel.saveWorkoutName()

        assertEquals(listOf("Push"), awaitPlan { it.workouts.isNotEmpty() }.workouts.map { it.name })
        viewModel.dialog.awaitFirst { it == null }
    }

    @Test
    fun `renomear plano e divisao - RF-25 e RF-42`() = runTest {
        plans.addWorkout(planId, "A")
        val workout = awaitPlan { it.workouts.isNotEmpty() }.workouts.single()

        viewModel.openRenamePlan()
        viewModel.onPlanNameChange("ABC")
        viewModel.saveRenamePlan()
        viewModel.openRenameWorkout(workout)
        viewModel.onWorkoutNameChange("Treino A")
        viewModel.saveWorkoutName()

        val plan = awaitPlan { it.name == "ABC" && it.workouts.single().name == "Treino A" }
        assertEquals("ABC", plan.name)
    }

    @Test
    fun `configuracao comeca vazia e exige valores validos - RF-49 e RF-143`() = runTest {
        val workout = workoutWithLibraryLoaded("Push")
        val supino = viewModel.library.value.first { it.name == "Supino reto com barra" }

        viewModel.openPickExercise(workout)
        viewModel.pickExercise(supino)
        val target = viewModel.dialog.value as PlanEditorDialog.Target
        assertEquals(TargetForm(), target.form)

        viewModel.saveTarget()
        assertEquals(TargetFormError.MISSING_SETS, (viewModel.dialog.value as PlanEditorDialog.Target).error)
    }

    @Test
    fun `adicionar exercicio com o padrao escolhido pelo usuario - RF-46 e RF-143`() = runTest {
        val workout = workoutWithLibraryLoaded("Push")
        val supino = viewModel.library.value.first { it.name == "Supino reto com barra" }

        viewModel.openPickExercise(workout)
        viewModel.pickExercise(supino)
        viewModel.updateTargetForm { it.withDefault() }
        viewModel.saveTarget()

        val added = awaitPlan { plan -> plan.workouts.single().exercises.isNotEmpty() }.workouts.single().exercises.single()
        assertEquals(supino.id, added.exercise.id)
        assertEquals(InputRules.DEFAULT_TARGET, added.target)
    }

    @Test
    fun `editar a configuracao de um exercicio da divisao - RF-49 a RF-57`() = runTest {
        val workout = workoutWithLibraryLoaded("Push")
        plans.addExerciseToWorkout(workout.id, viewModel.library.value.first().id, InputRules.DEFAULT_TARGET)
        val item = awaitPlan { it.workouts.single().exercises.isNotEmpty() }.workouts.single().exercises.single()

        viewModel.openEditTarget(workout, item)
        viewModel.updateTargetForm { it.copy(sets = "4", repMin = "6", repMax = "10").adjustRest(+6) }
        viewModel.saveTarget()

        val updated = awaitPlan { it.workouts.single().exercises.single().target.targetSets == 4 }
        val target = updated.workouts.single().exercises.single().target
        assertEquals(4, target.targetSets)
        assertEquals(6..10, target.repMin..target.repMax)
        assertEquals(180L, target.rest.seconds)
    }

    @Test
    fun `reordenar divisoes e exercicios - RF-44 e RF-48`() = runTest {
        plans.addWorkout(planId, "A")
        plans.addWorkout(planId, "B")
        val (first, second) = viewModel.library.awaitFirst { it.size >= 2 }
        val loaded = awaitPlan { it.workouts.size == 2 }
        plans.addExerciseToWorkout(loaded.workouts[0].id, first.id, InputRules.DEFAULT_TARGET)
        plans.addExerciseToWorkout(loaded.workouts[0].id, second.id, InputRules.DEFAULT_TARGET)
        val withExercises = awaitPlan { it.workouts[0].exercises.size == 2 }

        viewModel.moveWorkout(withExercises.workouts[1], -1)
        val reordered = awaitPlan { it.workouts.map { w -> w.name } == listOf("B", "A") }
        val workoutA = reordered.workouts[1]
        viewModel.moveExercise(workoutA, workoutA.exercises[0], +1)

        val final = awaitPlan { it.workouts[1].exercises.map { e -> e.exercise.id } == listOf(second.id, first.id) }
        assertEquals(listOf("B", "A"), final.workouts.map { it.name })
    }

    @Test
    fun `remover exercicio e excluir divisao com confirmacao - RF-43 e RF-47`() = runTest {
        val workout = workoutWithLibraryLoaded("Push")
        plans.addExerciseToWorkout(workout.id, viewModel.library.value.first().id, InputRules.DEFAULT_TARGET)
        val item = awaitPlan { it.workouts.single().exercises.isNotEmpty() }.workouts.single().exercises.single()

        viewModel.removeExercise(item)
        awaitPlan { it.workouts.single().exercises.isEmpty() }

        viewModel.requestDeleteWorkout(workout)
        viewModel.confirmDeleteWorkout()
        awaitPlan { it.workouts.isEmpty() }
        assertNull(viewModel.dialog.awaitFirst { it == null })
    }

    @Test
    fun `plano excluido fora do editor vira NotFound`() = runTest {
        viewModel.uiState.awaitFirst { it is PlanEditorUiState.Loaded }

        plans.deletePlan(planId)

        viewModel.uiState.awaitFirst { it == PlanEditorUiState.NotFound }
    }

    @Test
    fun `mover respeita os limites da lista`() {
        assertEquals(listOf("b", "a", "c"), listOf("a", "b", "c").moved(0, +1))
        assertNull(listOf("a", "b").moved(0, -1))
        assertNull(listOf("a", "b").moved(1, +1))
    }

    private suspend fun awaitPlan(predicate: (Plan) -> Boolean): Plan =
        (viewModel.uiState.awaitFirst { (it as? PlanEditorUiState.Loaded)?.plan?.let(predicate) == true }
            as PlanEditorUiState.Loaded).plan

    private suspend fun workoutWithLibraryLoaded(name: String) = run {
        plans.addWorkout(planId, name)
        viewModel.library.awaitFirst { it.isNotEmpty() }
        awaitPlan { it.workouts.isNotEmpty() }.workouts.single()
    }
}
