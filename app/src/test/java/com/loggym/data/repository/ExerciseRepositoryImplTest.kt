package com.loggym.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loggym.data.local.LogGymDatabase
import com.loggym.data.local.inMemoryDatabase
import com.loggym.domain.model.MuscleGroup
import com.loggym.domain.repository.ExerciseDraft
import com.loggym.domain.repository.SaveExerciseResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseRepositoryImplTest {

    private lateinit var database: LogGymDatabase
    private lateinit var repository: ExerciseRepositoryImpl

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        repository = ExerciseRepositoryImpl(database, database.exerciseDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `cria personalizado com nome limpo, grupos e descricao - RF-13 e RF-130`() = runTest {
        val result = repository.createCustom(
            ExerciseDraft("  Remada   cavalinho ", setOf(MuscleGroup.BACK, MuscleGroup.BICEPS), "  Peito apoiado  "),
        )

        val id = (result as SaveExerciseResult.Saved).exerciseId
        val saved = repository.observeAll().first().single { it.id == id }
        assertEquals("Remada cavalinho", saved.name)
        assertEquals(setOf(MuscleGroup.BACK, MuscleGroup.BICEPS), saved.muscleGroups)
        assertEquals("Peito apoiado", saved.description)
        assertTrue(saved.isCustom)
    }

    @Test
    fun `nome em branco e recusado - RF-14`() = runTest {
        assertEquals(SaveExerciseResult.BlankName, repository.createCustom(ExerciseDraft("   ")))
    }

    @Test
    fun `nome igual a um nativo e recusado ignorando maiusculas e espacos - RF-132`() = runTest {
        val result = repository.createCustom(ExerciseDraft("  SUPINO reto   com barra"))

        assertEquals(SaveExerciseResult.DuplicateName, result)
    }

    @Test
    fun `nome igual a outro personalizado e recusado - RF-132`() = runTest {
        repository.createCustom(ExerciseDraft("Remada cavalinho"))
        val other = repository.createCustom(ExerciseDraft("Outro exercício")) as SaveExerciseResult.Saved

        assertEquals(SaveExerciseResult.DuplicateName, repository.createCustom(ExerciseDraft("remada cavalinho")))
        assertEquals(
            SaveExerciseResult.DuplicateName,
            repository.updateCustom(other.exerciseId, ExerciseDraft("Remada Cavalinho")),
        )
    }

    @Test
    fun `editar mantendo o proprio nome e permitido`() = runTest {
        val id = (repository.createCustom(ExerciseDraft("Remada cavalinho")) as SaveExerciseResult.Saved).exerciseId

        val result = repository.updateCustom(id, ExerciseDraft("remada cavalinho", setOf(MuscleGroup.BACK)))

        assertEquals(SaveExerciseResult.Saved(id), result)
    }

    @Test
    fun `nativo nao pode ser editado pelo repositorio - RF-18`() = runTest {
        val native = database.exerciseDao().getByNativeKeys(listOf("stiff")).single()

        assertEquals(SaveExerciseResult.NotEditable, repository.updateCustom(native.id, ExerciseDraft("Stiff 2")))
    }

    @Test
    fun `observacao vale para nativos e branco remove - RF-133`() = runTest {
        val native = database.exerciseDao().getByNativeKeys(listOf("stiff")).single()

        repository.updateUserNote(native.id, " Banco 3, pegada pronada ")
        assertEquals("Banco 3, pegada pronada", database.exerciseDao().getById(native.id)?.userNote)

        repository.updateUserNote(native.id, "   ")
        assertNull(database.exerciseDao().getById(native.id)?.userNote)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `observacao acima de 80 caracteres e recusada - RF-133`() = runTest {
        val native = database.exerciseDao().getByNativeKeys(listOf("stiff")).single()

        repository.updateUserNote(native.id, "x".repeat(81))
    }
}
