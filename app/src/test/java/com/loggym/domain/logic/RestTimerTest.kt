package com.loggym.domain.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class RestTimerTest {

    private val start = Instant.parse("2026-01-01T10:00:00Z")

    @Test
    fun `descanso zero nao inicia cronometro - RF-89`() {
        assertNull(RestTimer.start(start, Duration.ZERO))
    }

    @Test
    fun `tempo restante e reconstruido a partir do instante final - RF-94`() {
        val timer = RestTimer.start(start, Duration.ofSeconds(180))!!

        assertEquals(Duration.ofSeconds(120), timer.remaining(start.plusSeconds(60)))
        assertFalse(timer.isFinished(start.plusSeconds(60)))
    }

    @Test
    fun `horario final ja passado conta como concluido - RF-95`() {
        val timer = RestTimer.start(start, Duration.ofSeconds(90))!!
        val later = start.plusSeconds(3_600)

        assertEquals(Duration.ZERO, timer.remaining(later))
        assertTrue(timer.isFinished(later))
    }
}
