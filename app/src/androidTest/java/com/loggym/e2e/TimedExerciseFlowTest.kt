package com.loggym.e2e

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loggym.data.local.LogGymDatabase
import com.loggym.data.repository.*
import com.loggym.domain.model.*
import com.loggym.ui.exercises.*
import com.loggym.ui.plans.*
import com.loggym.ui.session.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.Clock

@RunWith(AndroidJUnit4::class)
class TimedExerciseFlowTest {
    private val models = ViewModelStore()
    private var modelIndex = 0
    private fun <T : ViewModel> retain(model: T): T {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <R : ViewModel> create(modelClass: Class<R>): R = model as R
        }
        return ViewModelProvider(models, factory)["model-${modelIndex++}", model.javaClass]
    }
    @get:Rule val compose = createComposeRule()
    private lateinit var db: LogGymDatabase
    @After fun close() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { models.clear() }
        if (::db.isInitialized) db.close()
    }

    @Test fun createTimedExercisePrescribeAndExecuteThroughInterface() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LogGymDatabase::class.java).build()
        val clock = Clock.systemUTC()
        val exercises = ExerciseRepositoryImpl(db, db.exerciseDao())
        val plans = PlanRepositoryImpl(db, db.planDao(), db.exerciseDao(), clock)
        val sessions = SessionRepositoryImpl(db, db.sessionDao(), db.planDao(), clock, com.loggym.FakeUserPreferences())
        val (plan, workout) = runBlocking {
            val id = plans.createPlan("Plano S2")
            plans.setActivePlan(id)
            id to plans.addWorkout(id, "Tempo A")
        }
        val libraryVm = retain(ExerciseLibraryViewModel(exercises, savedState = androidx.lifecycle.SavedStateHandle()))
        val editorVm = retain(PlanEditorViewModel(SavedStateHandle(mapOf("planId" to plan)), plans, exercises))
        var sessionVm: SessionViewModel? = null
        var finished = false
        val historyVm = retain(com.loggym.ui.history.ExerciseHistoryViewModel(sessions))
        compose.setContent {
            var stage by remember { mutableStateOf(0) }
            MaterialTheme {
                when (stage) {
                    0 -> ExerciseLibraryScreen(onBack = { stage = 1 }, viewModel = libraryVm)
                    1 -> PlanEditorScreen(onBack = {
                        runBlocking { sessions.startSession(workout) }
                        sessionVm = retain(SessionViewModel(sessions, clock))
                        stage = 2
                    }, onCreateExercise = { error("Cadastro contextual pertence ao Sprint4FlowTest") }, viewModel = editorVm)
                    2 -> SessionScreen(onBack = { finished = true; stage = 3 }, viewModel = sessionVm!!, historyViewModel = historyVm)
                }
            }
        }
        click("Novo exercício")
        compose.onNodeWithText("Nome").performTextInput("Prancha S2")
        click("Tempo (segundos)")
        click("Salvar")
        compose.waitUntil(10_000) { runBlocking { exercises.observeAll().first().isNotEmpty() } }
        assertEquals(TrackingType.TIME, runBlocking { exercises.observeAll().first().single().trackingType })
        compose.onNodeWithContentDescription("Voltar").performClick()
        click("Adicionar exercício")
        click("Prancha S2")
        click("Usar padrão (3 × 30 s, 1:30)")
        click("Salvar")
        compose.waitUntil(10_000) { runBlocking { plans.observePlan(plan).first()!!.workouts.single().exercises.isNotEmpty() } }
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Duração (s)").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Duração (s)").onFirst().performTextInput("40")
        click("Concluir série 1 de Prancha S2")
        compose.waitUntil(10_000) { runBlocking { sessions.observeDraft().first()?.completedSetCount == 1 } }
        click("Finalizar treino")
        click("Finalizar")
        compose.waitUntil(10_000) { finished }
        runBlocking {
            val history = sessions.observeHistory().first().single()
            val session = sessions.observeSession(history.id).first()!!
            assertEquals(40, session.exercises.single().sets.single().durationSeconds)
            assertNull(sessions.observeDraft().first())
        }
    }
    private fun click(text: String) {
        val target = (hasText(text) or hasContentDescription(text)) and !hasSetTextAction() and hasClickAction() and isEnabled()
        compose.waitUntil(10_000) { compose.onAllNodes(target).fetchSemanticsNodes().isNotEmpty() }
        val node = compose.onAllNodes(target).onFirst()
        runCatching { node.performScrollTo() }
        node.assertIsDisplayed().performClick()
        compose.waitForIdle()
    }
}
