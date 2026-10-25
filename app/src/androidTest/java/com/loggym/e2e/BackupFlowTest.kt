package com.loggym.e2e

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.loggym.MainActivity
import com.loggym.domain.backup.*
import com.loggym.domain.logic.SetValues
import com.loggym.domain.model.ExerciseTarget
import com.loggym.domain.repository.*
import dagger.hilt.android.testing.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.time.Duration
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BackupFlowTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 2) val compose = createAndroidComposeRule<MainActivity>()
    @Inject lateinit var plans: PlanRepository
    @Inject lateinit var exercises: ExerciseRepository
    @Inject lateinit var sessions: SessionRepository
    @Inject lateinit var backup: BackupRepository
    @Inject lateinit var coordinator: PersistenceCoordinator
    @Before fun setup() { hilt.inject() }
    @Test fun exportFlushRestoreResetsNavigationAndOldEditorsThenResumesDraft() {
        click("Pular por enquanto")
        runBlocking {
            val plan = plans.createPlan("Plano backup"); plans.setActivePlan(plan)
            val workout = plans.addWorkout(plan, "Divisão backup")
            val exercise = (exercises.createCustom(ExerciseDraft("Movimento backup")) as SaveExerciseResult.Saved).exerciseId
            plans.addExerciseToWorkout(workout, exercise, ExerciseTarget.Repetitions(2, 8, 12, Duration.ofSeconds(37)))
        }
        click("Iniciar")
        compose.onAllNodesWithText("Carga (kg)").onFirst().performTextInput("22,5")
        compose.onAllNodesWithText("Repetições").onFirst().performTextInput("0")
        compose.runOnIdle { backup.prepareExport(BackupMode.FULL) }
        compose.activityRule.scenario.recreate()
        waitStep(BackupStep.DESTINATION)
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "s5-flow.loggym")
        try {
            compose.runOnIdle { backup.writeExport(backup.state.value.token!!, Uri.fromFile(file).toString()) }
            waitStep(BackupStep.SUCCESS)
            val draft = runBlocking { sessions.observeDraft().first()!! }
            assertEquals(22.5, draft.exercises[0].sets[0].weightKg!!, 0.0)
            assertEquals(0, draft.exercises[0].sets[0].reps)
            runBlocking { sessions.updateSetValues(draft.exercises[0].sets[0].id, SetValues(55.0, 12)) }
            compose.runOnIdle { backup.validateImport(Uri.fromFile(file).toString()) }
            waitStep(BackupStep.PREVIEW)
            val token = backup.state.value.token!!
            compose.runOnIdle { backup.confirmImport(token); backup.confirmImport(token) }
            waitStep(BackupStep.SUCCESS)
            compose.waitUntil(10000) { compose.onAllNodesWithText("Backup e restauração").fetchSemanticsNodes().isNotEmpty() }
            assertEquals(draft, runBlocking { sessions.observeDraft().first() })
            assertEquals(1L, coordinator.generation.value)
            // Não pode haver participantes antigos tentando escrever com IDs/geração substituídos.
            runBlocking { coordinator.capture {} }
            compose.activityRule.scenario.recreate()
            click("Voltar"); click("Retomar")
            compose.onAllNodesWithText("22,5").onFirst().assertExists()
            assertEquals(draft, runBlocking { sessions.observeDraft().first() })
        } finally { file.delete() }
    }
    private fun waitStep(step: BackupStep) {
        compose.waitUntil(15000) { backup.state.value.step == step || backup.state.value.step == BackupStep.ERROR }
        assertEquals(step, backup.state.value.step)
    }
    private fun click(text: String) {
        val match = hasText(text) or hasContentDescription(text)
        compose.waitUntil(15000) {
            if (compose.onAllNodes(match).fetchSemanticsNodes().isEmpty()) {
                val scrolls = compose.onAllNodes(hasScrollAction())
                repeat(scrolls.fetchSemanticsNodes().size) { index -> runCatching { scrolls[index].performScrollToNode(match) } }
            }
            compose.onAllNodes(match and hasClickAction() and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        val node = compose.onAllNodes(match and hasClickAction() and isEnabled()).onFirst()
        runCatching { node.performScrollTo() }; node.performClick(); compose.waitForIdle()
    }
}
