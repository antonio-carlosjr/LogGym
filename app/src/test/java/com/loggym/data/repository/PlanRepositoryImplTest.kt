package com.loggym.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loggym.data.local.LogGymDatabase
import com.loggym.data.local.fixedClock
import com.loggym.data.local.inMemoryDatabase
import com.loggym.data.seed.PlanTemplates
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
class PlanRepositoryImplTest {

    private lateinit var database: LogGymDatabase
    private lateinit var repository: PlanRepositoryImpl

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        repository = PlanRepositoryImpl(database, database.planDao(), database.exerciseDao(), fixedClock)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `ativar um plano desativa o anterior - RF-28 e RF-29`() = runTest {
        val first = repository.createPlan("Plano A")
        val second = repository.createPlan("Plano B")

        repository.setActivePlan(first)
        repository.setActivePlan(second)

        val active = repository.observePlans().first().filter { it.isActive }
        assertEquals(listOf(second), active.map { it.id })
    }

    @Test
    fun `excluir o plano ativo remove divisoes e deixa o app sem plano ativo - RF-33`() = runTest {
        val planId = repository.createFromTemplate("ppl", activate = true)
        val workoutIds = repository.observePlan(planId).first()!!.workouts.map { it.id }

        repository.deletePlan(planId)

        assertNull(repository.observeActivePlan().first())
        assertNull(database.planDao().getActivePlan())
        assertTrue(workoutIds.all { database.planDao().getWorkoutWithExercises(it) == null })
    }

    @Test
    fun `template gera instancia local completa e ativa - RF-37`() = runTest {
        val template = PlanTemplates.byKey("upper_lower")!!

        val planId = repository.createFromTemplate(template.key, activate = true)

        val plan = repository.observeActivePlan().first()!!
        assertEquals(planId, plan.id)
        assertEquals(template.workouts.map { it.name }, plan.workouts.map { it.name })
        assertEquals(
            template.workouts.map { workout -> workout.exercises.map { it.target } },
            plan.workouts.map { workout -> workout.exercises.map { it.target } },
        )
    }

    @Test
    fun `instancias do mesmo template sao independentes - RF-38`() = runTest {
        val first = repository.createFromTemplate("ppl", activate = false)
        val second = repository.createFromTemplate("ppl", activate = false)

        repository.renamePlan(first, "Meu PPL")
        repository.deleteWorkout(repository.observePlan(first).first()!!.workouts.first().id)

        val untouched = repository.observePlan(second).first()!!
        assertEquals("${PlanTemplates.byKey("ppl")!!.name} (2)", untouched.name)
        assertEquals(3, untouched.workouts.size)
        assertTrue(repository.observePlans().first().none { it.isActive })
    }

    @Test
    fun `mesmo template repetido recebe sufixo numerado - RF-145`() = runTest {
        val name = PlanTemplates.byKey("full_body")!!.name

        val ids = List(3) { repository.createFromTemplate("full_body", activate = false) }

        val names = ids.map { repository.observePlan(it).first()!!.name }
        assertEquals(listOf(name, "$name (2)", "$name (3)"), names)
    }

    @Test
    fun `divisoes e exercicios seguem a ordem definida - RF-44 e RF-48`() = runTest {
        val planId = repository.createFromTemplate("ppl", activate = false)
        val workouts = repository.observePlan(planId).first()!!.workouts

        repository.reorderWorkouts(workouts.map { it.id }.reversed())
        val pushExercises = workouts.first().exercises
        repository.reorderWorkoutExercises(pushExercises.map { it.id }.reversed())

        val reordered = repository.observePlan(planId).first()!!
        assertEquals(workouts.map { it.name }.reversed(), reordered.workouts.map { it.name })
        assertEquals(
            pushExercises.map { it.exercise.name }.reversed(),
            reordered.workouts.last().exercises.map { it.exercise.name },
        )
    }
}
