package com.loggym.domain.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseNamesTest {

    @Test
    fun `remove espacos nas pontas e repetidos`() {
        assertEquals("Supino reto com barra", ExerciseNames.clean("  Supino   reto com  barra "))
    }

    @Test
    fun `chave de comparacao ignora maiusculas e espacos - RF-132`() {
        assertEquals(
            ExerciseNames.comparisonKey("Elevação Pélvica"),
            ExerciseNames.comparisonKey("  elevação   pélvica"),
        )
    }
}
