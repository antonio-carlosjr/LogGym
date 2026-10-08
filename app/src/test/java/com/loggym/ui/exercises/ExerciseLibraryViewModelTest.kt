package com.loggym.ui.exercises

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loggym.MainDispatcherRule
import com.loggym.awaitFirst
import com.loggym.data.local.LogGymDatabase
import com.loggym.data.local.fixedClock
import com.loggym.data.local.inMemoryDatabase
import com.loggym.data.repository.ExerciseRepositoryImpl
import com.loggym.data.repository.PlanRepositoryImpl
import com.loggym.domain.model.InputRules
import com.loggym.domain.model.MuscleGroup
import com.loggym.domain.repository.ExerciseDraft
import com.loggym.domain.repository.SaveExerciseResult
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
class ExerciseLibraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: LogGymDatabase
    private lateinit var repository: ExerciseRepositoryImpl
    private lateinit var viewModel: ExerciseLibraryViewModel

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        repository = ExerciseRepositoryImpl(database, database.exerciseDao())
        viewModel = ExerciseLibraryViewModel(repository)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `criar exercicio valido fecha o editor e o exercicio aparece - RF-13`() = runTest {
        viewModel.openCreate()
        viewModel.updateEditor { it.copy(name = "Remada articulada", muscleGroups = setOf(MuscleGroup.BACK)) }
        viewModel.saveEditor()

        viewModel.dialog.awaitFirst { it == null }
        val created = viewModel.allExercises
            .awaitFirst { list -> list.any { it.name == "Remada articulada" } }
            .single { it.name == "Remada articulada" }
        assertTrue(created.isCustom)
        assertEquals(setOf(MuscleGroup.BACK), created.muscleGroups)
    }

    @Test
    fun `nome duplicado mantem o editor aberto com erro - RF-132`() = runTest {
        viewModel.openCreate()
        viewModel.updateEditor { it.copy(name = "stiff") }
        viewModel.saveEditor()

        val dialog = viewModel.dialog.awaitFirst { (it as? ExerciseDialog.Editor)?.error != null }
        assertEquals(EditorError.DUPLICATE_NAME, (dialog as ExerciseDialog.Editor).error)
    }

    @Test
    fun `nativos nao abrem edicao nem exclusao - RF-18 e RF-19`() = runTest {
        val native = viewModel.allExercises.awaitFirst { it.isNotEmpty() }.first { !it.isCustom }

        viewModel.openEdit(native)
        viewModel.requestDelete(native)

        assertNull(viewModel.dialog.value)
    }

    @Test
    fun `exclusao avisa quantas divisoes usam o exercicio - RF-21`() = runTest {
        val plans = PlanRepositoryImpl(database, database.planDao(), database.exerciseDao(), fixedClock)
        val id = (repository.createCustom(ExerciseDraft("Remada articulada")) as SaveExerciseResult.Saved).exerciseId
        val planId = plans.createPlan("Plano")
        plans.addExerciseToWorkout(plans.addWorkout(planId, "A"), id, InputRules.DEFAULT_TARGET)
        plans.addExerciseToWorkout(plans.addWorkout(planId, "B"), id, InputRules.DEFAULT_TARGET)
        val custom = viewModel.allExercises.awaitFirst { list -> list.any { it.id == id } }.single { it.id == id }

        viewModel.requestDelete(custom)
        val confirm = viewModel.dialog.awaitFirst { it is ExerciseDialog.ConfirmDelete }
        assertEquals(2, (confirm as ExerciseDialog.ConfirmDelete).planUsages)

        viewModel.confirmDelete()
        viewModel.dialog.awaitFirst { it == null }
        viewModel.allExercises.awaitFirst { list -> list.none { it.id == id } }
    }
}
