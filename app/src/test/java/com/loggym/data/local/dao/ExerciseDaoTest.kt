package com.loggym.data.local.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loggym.data.local.LogGymDatabase
import com.loggym.data.local.entity.ExerciseEntity
import com.loggym.data.local.entity.PlanEntity
import com.loggym.data.local.entity.WorkoutEntity
import com.loggym.data.local.entity.WorkoutExerciseEntity
import com.loggym.data.local.fixedClock
import com.loggym.data.local.inMemoryDatabase
import com.loggym.domain.model.MuscleGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseDaoTest {

    private lateinit var database: LogGymDatabase
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var planDao: PlanDao

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        exerciseDao = database.exerciseDao()
        planDao = database.planDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `exercicio nativo nao pode ser editado - RF-18`() = runTest {
        val native = exerciseDao.getByNativeKeys(listOf("supino_reto")).single()

        val updated = exerciseDao.updateCustom(native.id, "Outro nome", emptySet(), null)

        assertEquals(0, updated)
        assertEquals(native.name, exerciseDao.getByNativeKeys(listOf("supino_reto")).single().name)
    }

    @Test
    fun `exercicio nativo nao pode ser excluido - RF-19`() = runTest {
        val native = exerciseDao.getByNativeKeys(listOf("supino_reto")).single()

        val deleted = exerciseDao.deleteCustom(native.id)

        assertEquals(0, deleted)
        assertNotNull(findById(native.id))
    }

    @Test
    fun `exercicio personalizado pode ser editado e excluido - RF-15 e RF-16`() = runTest {
        val id = exerciseDao.insert(customExercise("Supino na Smith"))

        assertEquals(1, exerciseDao.updateCustom(id, "Supino na máquina Smith", setOf(MuscleGroup.CHEST), null))
        assertEquals("Supino na máquina Smith", findById(id)?.name)

        assertEquals(1, exerciseDao.deleteCustom(id))
        assertNull(findById(id))
    }

    @Test
    fun `excluir personalizado remove apenas suas prescricoes nos planos - RF-21`() = runTest {
        val customId = exerciseDao.insert(customExercise("Remada cavalinho"))
        val nativeId = exerciseDao.getByNativeKeys(listOf("puxada_frontal")).single().id
        val planId = planDao.insertPlan(PlanEntity(name = "Plano", createdAt = fixedClock.instant()))
        val workoutId = planDao.insertWorkout(WorkoutEntity(planId = planId, name = "Pull", position = 0))
        planDao.insertWorkoutExercise(prescription(workoutId, customId, position = 0))
        planDao.insertWorkoutExercise(prescription(workoutId, nativeId, position = 1))

        assertEquals(1, exerciseDao.countPlanUsages(customId))
        exerciseDao.deleteCustom(customId)

        val workout = planDao.getWorkoutWithExercises(workoutId)
        assertNotNull(workout)
        assertEquals(listOf(nativeId), workout!!.exercises.map { it.exercise.id })
        assertEquals(0, exerciseDao.countPlanUsages(customId))
    }

    private suspend fun findById(id: Long): ExerciseEntity? =
        exerciseDao.observeAll().first().firstOrNull { it.id == id }

    private fun customExercise(name: String) =
        ExerciseEntity(name = name, isCustom = true, nativeKey = null)

    private fun prescription(workoutId: Long, exerciseId: Long, position: Int) = WorkoutExerciseEntity(
        workoutId = workoutId,
        exerciseId = exerciseId,
        position = position,
        targetSets = 3,
        repMin = 8,
        repMax = 12,
        restSeconds = 90,
    )
}
