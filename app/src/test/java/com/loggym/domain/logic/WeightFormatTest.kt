package com.loggym.domain.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeightFormatTest {

    @Test
    fun `aceita virgula ou ponto com ate duas casas - RF-140`() {
        assertEquals(22.5, WeightFormat.parse("22,5")!!, 0.0)
        assertEquals(22.5, WeightFormat.parse(" 22.5 ")!!, 0.0)
        assertEquals(1.25, WeightFormat.parse("1,25")!!, 0.0)
        assertEquals(0.0, WeightFormat.parse("0")!!, 0.0)
    }

    @Test
    fun `aceita carga sem limite superior - RF-141`() {
        assertEquals(1_500.0, WeightFormat.parse("1500")!!, 0.0)
    }

    @Test
    fun `rejeita texto invalido, negativo ou com mais de duas casas`() {
        listOf("", "abc", "-5", "1,234", "1e3", "2,", ",5").forEach { input ->
            assertNull("Deveria rejeitar '$input'", WeightFormat.parse(input))
        }
    }

    @Test
    fun `exibe em pt-BR sem zeros a direita - RF-140`() {
        assertEquals("22,5", WeightFormat.format(22.5))
        assertEquals("100", WeightFormat.format(100.0))
        assertEquals("0", WeightFormat.format(0.0))
        assertEquals("1,25", WeightFormat.format(1.25))
    }
}
