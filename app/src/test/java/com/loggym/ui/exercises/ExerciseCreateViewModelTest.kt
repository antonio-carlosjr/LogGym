package com.loggym.ui.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loggym.MainDispatcherRule
import com.loggym.awaitFirst
import com.loggym.data.local.*
import com.loggym.data.repository.ExerciseRepositoryImpl
import com.loggym.domain.model.TrackingType
import com.loggym.domain.repository.*
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseCreateViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private lateinit var db: LogGymDatabase
    @Before fun setup() { db = inMemoryDatabase() }
    @After fun close() { db.close() }
    @Test fun formSurvivesRecreationAndFailureThenCreatesOnlyOnce() = runTest {
        val state = SavedStateHandle()
        val repository = ExerciseRepositoryImpl(db, db.exerciseDao())
        var fail = true
        val flaky = object : ExerciseRepository by repository {
            override suspend fun createCustom(draft: ExerciseDraft): SaveExerciseResult {
                if (fail) throw java.io.IOException()
                return repository.createCustom(draft)
            }
        }
        val initial = ExerciseCreateViewModel(state, flaky)
        initial.update { it.copy(name = "Prancha S4", description = "Descrição", trackingType = TrackingType.TIME) }
        val vm = ExerciseCreateViewModel(state, flaky)
        assertEquals(initial.editor.value, vm.editor.value)
        vm.save()
        vm.failed.awaitFirst { it }
        vm.busy.awaitFirst { !it }
        assertEquals("Prancha S4", vm.editor.value.name)
        fail = false
        vm.save()
        val id = vm.createdId.awaitFirst { it != null }
        vm.busy.awaitFirst { !it }
        vm.save()
        assertEquals(id, ExerciseCreateViewModel(state, flaky).createdId.value)
        assertEquals(1, repository.observeAll().awaitFirst { list -> list.any { it.name == "Prancha S4" } }.count { it.name == "Prancha S4" })
    }
}
