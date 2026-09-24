package com.loggym.domain.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class SetPrefillTest {

    @Test
    fun `sem historico todas as series comecam vazias - RF-72`() {
        val result = SetPrefill.build(targetSets = 3, previous = emptyList())

        assertEquals(List(3) { SetValues.EMPTY }, result)
    }

    @Test
    fun `menos series historicas completa com vazias - RF-70`() {
        val previous = listOf(SetValues(80.0, 8))

        val result = SetPrefill.build(targetSets = 3, previous = previous)

        assertEquals(listOf(SetValues(80.0, 8), SetValues.EMPTY, SetValues.EMPTY), result)
    }

    @Test
    fun `mais series historicas preenche apenas as atuais - RF-71`() {
        val previous = listOf(SetValues(80.0, 8), SetValues(80.0, 7), SetValues(75.0, 9), SetValues(70.0, 10))

        val result = SetPrefill.build(targetSets = 2, previous = previous)

        assertEquals(listOf(SetValues(80.0, 8), SetValues(80.0, 7)), result)
    }
}
