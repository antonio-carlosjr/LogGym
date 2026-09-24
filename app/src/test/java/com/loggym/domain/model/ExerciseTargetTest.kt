package com.loggym.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class ExerciseTargetTest {

    @Test(expected = IllegalArgumentException::class)
    fun `meta de series precisa ser positiva - RF-50`() {
        ExerciseTarget(targetSets = 0, repMin = 6, repMax = 10, rest = Duration.ofSeconds(60))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `repeticao minima nao pode exceder a maxima - RF-52`() {
        ExerciseTarget(targetSets = 3, repMin = 12, repMax = 8, rest = Duration.ofSeconds(60))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `repeticoes precisam ser positivas - RF-53`() {
        ExerciseTarget(targetSets = 3, repMin = 0, repMax = 8, rest = Duration.ofSeconds(60))
    }

    @Test
    fun `descanso zero desativa o cronometro - RF-56`() {
        assertFalse(ExerciseTarget(3, 6, 10, Duration.ZERO).isRestTimerEnabled)
        assertTrue(ExerciseTarget(3, 6, 10, Duration.ofSeconds(180)).isRestTimerEnabled)
    }
}
