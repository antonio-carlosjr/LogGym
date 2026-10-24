package com.loggym.e2e

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.loggym.ui.theme.LogGymTheme
import java.io.File
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loggym.data.local.LogGymDatabase
import com.loggym.data.repository.*
import com.loggym.domain.model.*
import com.loggym.domain.repository.*
import com.loggym.ui.session.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.*

/** Exercises the actual screen with Room; history UI belongs to Sprint 3. */
@RunWith(AndroidJUnit4::class)
class SessionScreenTest {
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
    private lateinit var sessions: SessionRepositoryImpl
    private lateinit var vm: SessionViewModel
    private val clock = Clock.fixed(Instant.parse("2026-10-22T12:00:00Z"), ZoneOffset.UTC)
    private var left = false
    private val dark = mutableStateOf(false)
    private val dynamic = mutableStateOf(false)
    private val fontScale = mutableStateOf(1f)

    @Before fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LogGymDatabase::class.java).build()
        val exercises = ExerciseRepositoryImpl(db, db.exerciseDao())
        val plans = PlanRepositoryImpl(db, db.planDao(), db.exerciseDao(), clock)
        sessions = SessionRepositoryImpl(db, db.sessionDao(), db.planDao(), clock)
        val plan = plans.createPlan("Plano misto")
        plans.setActivePlan(plan)
        val workout = plans.addWorkout(plan, "Treino A")
        val id = (exercises.createCustom(ExerciseDraft("Prancha", trackingType = TrackingType.TIME)) as SaveExerciseResult.Saved).exerciseId
        plans.addExerciseToWorkout(workout, id, ExerciseTarget.Timed(2, 30, Duration.ofSeconds(60)))
        sessions.startSession(workout)
        vm = retain(SessionViewModel(sessions, clock))
        compose.setContent {
            var open by remember { mutableStateOf(true) }
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale.value)) {
            LogGymTheme(darkTheme = dark.value, dynamicColor = dynamic.value) {
                if (open) SessionScreen(onBack = { left = true; open = false }, viewModel = vm)
                else Button(onClick = { vm = retain(SessionViewModel(sessions, clock)); open = true }) { Text("Retomar teste") }
            }
            }
        }
    }
    @After fun close() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { models.clear() }
        db.close()
    }
    private fun waitForScreen() = compose.waitUntil(10_000) {
        compose.onAllNodesWithText("Duração (s)").fetchSemanticsNodes().isNotEmpty()
    }
    private fun click(text: String) { compose.onNodeWithText(text).performClick() }
    private fun scrollToSet(index: Int) {
        // A LazyColumn pode ainda não ter composto a série fora da tela.
        compose.onNode(hasScrollAction()).performScrollToNode(
            hasContentDescription("Concluir série $index de Prancha"),
        )
    }
    private fun completeSet(index: Int) {
        scrollToSet(index)
        compose.onNodeWithContentDescription("Concluir série $index de Prancha").performClick()
        compose.waitUntil(10_000) {
            val state = vm.uiState.value
            !state.busy && state.session?.exercises?.single()?.sets?.get(index - 1)?.isCompleted == true
        }
    }

    @Test fun timedSetAndIncompleteSetFinishWithWarningAndNullPreserved() {
        waitForScreen()
        compose.onAllNodesWithText("Duração (s)")[0].performTextInput("45")
        completeSet(1)
        compose.onAllNodesWithText("Duração (s)").onFirst().assertIsNotEnabled()
        completeSet(2)
        val warning = hasText("Série marcada com campos incompletos.") and hasAnyAncestor(hasScrollAction())
        compose.onNode(hasScrollAction()).performScrollToNode(warning)
        compose.onNode(warning).assertIsDisplayed()
        click("Finalizar treino")
        compose.onNodeWithText("1 série será salva com campos vazios.").assertIsDisplayed()
        click("Finalizar")
        compose.waitUntil(10_000) { left }
        runBlocking {
            val history = sessions.observeHistory().first()
            assertEquals(1, history.size)
            val sets = sessions.observeSession(history.single().id).first()!!.exercises.single().sets
            assertEquals(45, sets[0].durationSeconds)
            assertNull(sets[1].durationSeconds)
            assertNull(sessions.observeDraft().first())
        }
    }
    @Test fun backFlushesAndResumeRestoresDraftThenDiscardRequiresConfirmation() {
        waitForScreen()
        compose.onAllNodesWithText("Duração (s)")[0].performTextInput("60")
        compose.onNodeWithContentDescription("Voltar e salvar").performClick()
        compose.waitUntil(10_000) { left }
        assertEquals(60, runBlocking { sessions.observeDraft().first()!!.exercises.single().sets[0].durationSeconds })
        click("Retomar teste")
        waitForScreen()
        compose.onNodeWithText("60").assertExists()
        click("Descartar")
        click("Cancelar")
        assertNotNull(runBlocking { sessions.observeDraft().first() })
        click("Descartar")
        compose.onAllNodesWithText("Descartar").onLast().performClick()
        compose.waitUntil(10_000) { runBlocking { sessions.observeDraft().first() == null } }
        assertTrue(runBlocking { sessions.observeHistory().first().isEmpty() })
    }

    @Test fun restAndCompletedStateRemainAccessibleAcrossThemesAndLargeText() {
        waitForScreen()
        completeSet(1)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = File(context.getExternalFilesDir(null), "s3-preview").apply { mkdirs() }
        for (mode in listOf("light", "dark", "dynamic-large")) {
            compose.runOnIdle {
                dark.value = mode == "dark"
                dynamic.value = mode == "dynamic-large"
                fontScale.value = if (mode == "dynamic-large") 2f else 1f
            }
            compose.onNodeWithTag("rest-banner").assertIsDisplayed()
            compose.onNodeWithText("Pular").assertIsDisplayed()
            scrollToSet(2)
            compose.onNodeWithContentDescription("Concluir série 2 de Prancha").assertIsDisplayed()
            compose.onNodeWithTag("rest-banner").assertIsDisplayed()
            val bounds = compose.onNodeWithText("Pular").fetchSemanticsNode().boundsInRoot
            val minimum = 48 * context.resources.displayMetrics.density - 1
            assertTrue("Pular deve ter alvo de toque de 48 dp", bounds.height >= minimum)
            File(output, "session-$mode.png").outputStream().use {
                compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        click("Pular")
        compose.onNodeWithTag("rest-banner").assertDoesNotExist()
    }
}
