package com.loggym.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class ExerciseTargetTest {

    @Test(expected = IllegalArgumentException::class)
    fun `meta de series precisa ser positiva - RF-50`() {
        ExerciseTarget.Repetitions(targetSets = 0, repMin = 6, repMax = 10, rest = Duration.ofSeconds(60))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `repeticao minima nao pode exceder a maxima - RF-52`() {
        ExerciseTarget.Repetitions(targetSets = 3, repMin = 12, repMax = 8, rest = Duration.ofSeconds(60))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `repeticoes precisam ser positivas - RF-53`() {
        ExerciseTarget.Repetitions(targetSets = 3, repMin = 0, repMax = 0, rest = Duration.ofSeconds(60))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `descanso acima de 600 s e invalido - RF-142`() {
        ExerciseTarget.Repetitions(targetSets = 3, repMin = 6, repMax = 10, rest = Duration.ofSeconds(615))
    }

    @Test
    fun `descanso manual nao precisa ser multiplo de 15 - RF-142`() {
        assertEquals(37L, ExerciseTarget.Repetitions(3, 6, 10, Duration.ofSeconds(37)).rest.seconds)
    }

    @Test
    fun `aceita os limites de descanso e series sem teto - RF-141 e RF-142`() {
        ExerciseTarget.Repetitions(targetSets = 3, repMin = 6, repMax = 10, rest = Duration.ofSeconds(600))
        ExerciseTarget.Repetitions(targetSets = 50, repMin = 100, repMax = 300, rest = Duration.ZERO)
    }

    @Test
    fun `padrao opcional e 3 series de 8 a 12 com 90 s - RF-143`() {
        assertEquals(ExerciseTarget.Repetitions(3, 8, 12, Duration.ofSeconds(90)), InputRules.DEFAULT_TARGET)
    }

    @Test
    fun `descanso zero desativa o cronometro - RF-56`() {
        assertFalse(ExerciseTarget.Repetitions(3, 6, 10, Duration.ZERO).isRestTimerEnabled)
        assertTrue(ExerciseTarget.Repetitions(3, 6, 10, Duration.ofSeconds(180)).isRestTimerEnabled)
    }

    @Test fun `minimo zero e repeticoes fixas preservam os valores`() {
        assertEquals("3 × 0–12", ExerciseTarget.Repetitions(3, 0, 12, Duration.ZERO).summary())
        assertEquals("3 × 10", ExerciseTarget.Repetitions(3, 10, 10, Duration.ZERO).summary())
    }
}
