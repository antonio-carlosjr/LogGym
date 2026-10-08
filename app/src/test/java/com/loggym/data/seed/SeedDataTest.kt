package com.loggym.data.seed

import com.loggym.domain.logic.ExerciseNames
import com.loggym.domain.model.InputRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedDataTest {

    @Test
    fun `chaves dos exercicios nativos sao unicas`() {
        val keys = NativeExercises.all.map { it.key }

        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `templates referenciam apenas exercicios nativos existentes`() {
        val nativeKeys = NativeExercises.all.map { it.key }.toSet()
        val missing = PlanTemplates.all
            .flatMap { it.workouts }
            .flatMap { it.exercises }
            .map { it.nativeKey }
            .filterNot { it in nativeKeys }

        assertTrue("Chaves inexistentes: $missing", missing.isEmpty())
    }

    @Test
    fun `oferece as estruturas decididas - RF-36 e RF-144`() {
        assertEquals(setOf("full_body", "ppl", "upper_lower", "bro_split"), PlanTemplates.all.map { it.key }.toSet())
    }

    @Test
    fun `biblioteca nativa tem cerca de 60 exercicios - decisao 1_1`() {
        assertTrue(NativeExercises.all.size in 55..65)
    }

    @Test
    fun `todo nativo tem ao menos um grupo e descricao - RF-130 e RF-134`() {
        val invalid = NativeExercises.all.filter { it.muscleGroups.isEmpty() || it.description.isBlank() }

        assertTrue("Nativos incompletos: ${invalid.map { it.key }}", invalid.isEmpty())
    }

    @Test
    fun `nomes nativos sao unicos ignorando maiusculas e espacos - RF-132`() {
        val keys = NativeExercises.all.map { ExerciseNames.comparisonKey(it.name) }

        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `chaves nativas seguem o formato estavel - RF-148`() {
        val invalid = NativeExercises.all.map { it.key }.filterNot { it.matches(Regex("[a-z0-9_]+")) }

        assertTrue("Chaves inválidas: $invalid", invalid.isEmpty())
    }

    @Test
    fun `todo treino de template respeita 4 a 6 exercicios e 12 a 20 series - RF-144`() {
        val outOfRange = PlanTemplates.all.flatMap { template ->
            template.workouts
                .filterNot { workout ->
                    workout.exercises.size in InputRules.TEMPLATE_EXERCISES_PER_WORKOUT &&
                        workout.exercises.sumOf { it.target.targetSets } in InputRules.TEMPLATE_SETS_PER_WORKOUT
                }
                .map { "${template.key}/${it.name}" }
        }

        assertTrue("Treinos fora da regra: $outOfRange", outOfRange.isEmpty())
    }

    @Test
    fun `template nao repete exercicio dentro do mesmo treino`() {
        val repeated = PlanTemplates.all.flatMap { template ->
            template.workouts
                .filter { workout -> workout.exercises.map { it.nativeKey }.let { it.size != it.toSet().size } }
                .map { "${template.key}/${it.name}" }
        }

        assertTrue("Treinos com repetição: $repeated", repeated.isEmpty())
    }

    @Test
    fun `todo template tem ao menos uma divisao com exercicios - RF-39`() {
        PlanTemplates.all.forEach { template ->
            assertTrue(template.workouts.isNotEmpty())
            assertTrue(template.workouts.all { it.exercises.isNotEmpty() })
        }
    }
}
