package com.loggym.ui.exercises

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loggym.data.local.LogGymDatabase
import com.loggym.data.local.fixedClock
import com.loggym.data.local.inMemoryDatabase
import com.loggym.data.repository.ExerciseRepositoryImpl
import com.loggym.data.repository.PlanRepositoryImpl
import com.loggym.domain.model.Exercise
import com.loggym.domain.model.InputRules
import com.loggym.domain.model.MuscleGroup
import com.loggym.domain.repository.ExerciseDraft
import com.loggym.domain.repository.SaveExerciseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ExerciseLibraryViewModelTest {

    private lateinit var database: LogGymDatabase
    private lateinit var viewModel: ExerciseLibraryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = inMemoryDatabase()
        viewModel = ExerciseLibraryViewModel(ExerciseRepositoryImpl(database, database.exerciseDao()))
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `criar exercicio valido fecha o editor e o exercicio aparece - RF-13`() = runTest {
        viewModel.openCreate()
        viewModel.updateEditor { it.copy(name = "Remada articulada", muscleGroups = setOf(MuscleGroup.BACK)) }
        viewModel.saveEditor()

        awaitDialog { it == null }
        val created = awaitExercises { list -> list.any { it.name == "Remada articulada" } }
            .single { it.name == "Remada articulada" }
        assertTrue(created.isCustom)
        assertEquals(setOf(MuscleGroup.BACK), created.muscleGroups)
    }

    @Test
    fun `nome duplicado mantem o editor aberto com erro - RF-132`() = runTest {
        viewModel.openCreate()
        viewModel.updateEditor { it.copy(name = "stiff") }
        viewModel.saveEditor()

        val dialog = awaitDialog { (it as? ExerciseDialog.Editor)?.error != null } as ExerciseDialog.Editor
        assertEquals(EditorError.DUPLICATE_NAME, dialog.error)
    }

    @Test
    fun `nativos nao abrem edicao nem exclusao - RF-18 e RF-19`() = runTest {
        val native = awaitExercises { it.isNotEmpty() }.first { !it.isCustom }

        viewModel.openEdit(native)
        viewModel.requestDelete(native)

        assertNull(viewModel.dialog.value)
    }

    @Test
    fun `exclusao avisa quantas divisoes usam o exercicio - RF-21`() = runTest {
        val repository = ExerciseRepositoryImpl(database, database.exerciseDao())
        val plans = PlanRepositoryImpl(database, database.planDao(), database.exerciseDao(), fixedClock)
        val id = (repository.createCustom(ExerciseDraft("Remada articulada"))
            as SaveExerciseResult.Saved).exerciseId
        val planId = plans.createPlan("Plano")
        val workoutA = plans.addWorkout(planId, "A")
        val workoutB = plans.addWorkout(planId, "B")
        plans.addExerciseToWorkout(workoutA, id, InputRules.DEFAULT_TARGET)
        plans.addExerciseToWorkout(workoutB, id, InputRules.DEFAULT_TARGET)
        val custom = awaitExercises { list -> list.any { it.id == id } }.single { it.id == id }

        viewModel.requestDelete(custom)
        val confirm = awaitDialog { it is ExerciseDialog.ConfirmDelete } as ExerciseDialog.ConfirmDelete
        assertEquals(2, confirm.planUsages)

        viewModel.confirmDelete()
        awaitDialog { it == null }
        awaitExercises { list -> list.none { it.id == id } }
    }

    private suspend fun awaitDialog(predicate: (ExerciseDialog?) -> Boolean): ExerciseDialog? =
        realTimeout { viewModel.dialog.first(predicate) }

    private suspend fun awaitExercises(predicate: (List<Exercise>) -> Boolean) =
        realTimeout { viewModel.allExercises.first(predicate) }

    /** O Room responde em threads próprias, então a espera usa tempo real, não o virtual do runTest. */
    private suspend fun <T> realTimeout(block: suspend () -> T): T =
        withContext(Dispatchers.Default.limitedParallelism(1)) { withTimeout(5_000) { block() } }
}
