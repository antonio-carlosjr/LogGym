package com.loggym.ui.exercises

import com.loggym.domain.model.Exercise
import com.loggym.domain.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseFilterTest {

    private val supino = exercise(1, "Supino reto com barra", MuscleGroup.CHEST, MuscleGroup.TRICEPS)
    private val triceps = exercise(2, "Tríceps na polia", MuscleGroup.TRICEPS)
    private val stiff = exercise(3, "Stiff", MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES)
    private val all = listOf(supino, triceps, stiff)

    @Test
    fun `sem filtros retorna todos`() {
        assertEquals(all, filterExercises(all, query = "  ", group = null))
    }

    @Test
    fun `busca ignora acentos e maiusculas`() {
        assertEquals(listOf(triceps), filterExercises(all, query = "TRICEPS", group = null))
        assertEquals(listOf(supino), filterExercises(all, query = "reto", group = null))
    }

    @Test
    fun `filtro por grupo inclui exercicios com qualquer grupo correspondente - P2`() {
        assertEquals(listOf(supino, triceps), filterExercises(all, query = "", group = MuscleGroup.TRICEPS))
    }

    @Test
    fun `busca e grupo combinados`() {
        assertEquals(listOf(triceps), filterExercises(all, query = "polia", group = MuscleGroup.TRICEPS))
        assertEquals(emptyList<Exercise>(), filterExercises(all, query = "stiff", group = MuscleGroup.CHEST))
    }

    private fun exercise(id: Long, name: String, vararg groups: MuscleGroup) = Exercise(
        id = id,
        name = name,
        muscleGroups = groups.toSet(),
        description = null,
        userNote = null,
        isCustom = false,
    )
}
