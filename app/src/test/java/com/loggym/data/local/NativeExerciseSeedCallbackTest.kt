package com.loggym.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loggym.data.seed.NativeExercises
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeExerciseSeedCallbackTest {

    @Test
    fun `primeira criacao do banco carrega todos os nativos - RF-01`() = runTest {
        val database = inMemoryDatabase()

        val exercises = database.exerciseDao().observeAll().first()

        assertEquals(NativeExercises.all.map { it.key }.toSet(), exercises.map { it.nativeKey }.toSet())
        assertTrue(exercises.none { it.isCustom })
        database.close()
    }

    @Test
    fun `carga inicial grava grupos e descricao de cada nativo - RF-130 e RF-134`() = runTest {
        val database = inMemoryDatabase()

        val byKey = database.exerciseDao().observeAll().first().associateBy { it.nativeKey }

        NativeExercises.all.forEach { native ->
            val stored = byKey.getValue(native.key)
            assertEquals(native.muscleGroups, stored.muscleGroups)
            assertEquals(native.description, stored.description)
        }
        database.close()
    }

    @Test
    fun `reabrir o banco nao duplica os nativos - RF-02`() = runTest {
        val name = "seed-reopen.db"
        testContext.deleteDatabase(name)

        val first = fileDatabase(name)
        val countAfterCreate = first.exerciseDao().observeAll().first().size
        first.close()

        val reopened = fileDatabase(name)
        val countAfterReopen = reopened.exerciseDao().observeAll().first().size
        reopened.close()

        assertEquals(NativeExercises.all.size, countAfterCreate)
        assertEquals(countAfterCreate, countAfterReopen)
        testContext.deleteDatabase(name)
    }
}
