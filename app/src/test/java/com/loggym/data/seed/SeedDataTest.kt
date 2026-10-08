package com.loggym.data.seed

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
    fun `todo template tem ao menos uma divisao com exercicios - RF-39`() {
        PlanTemplates.all.forEach { template ->
            assertTrue(template.workouts.isNotEmpty())
            assertTrue(template.workouts.all { it.exercises.isNotEmpty() })
        }
    }
}
