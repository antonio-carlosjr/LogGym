package com.loggym.e2e

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.loggym.MainActivity
import com.loggym.data.local.LogGymDatabase
import com.loggym.domain.logic.SetValues
import com.loggym.domain.model.*
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

/** Same fixture/navigation before and after S8. Captures are opt-in artifacts, never golden assertions. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class Sprint8VisualEvidenceTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    @Inject lateinit var plans: PlanRepository
    @Inject lateinit var exercises: ExerciseRepository
    @Inject lateinit var sessions: SessionRepository
    @Inject lateinit var preferences: UserPreferencesRepository
    @Inject lateinit var database: LogGymDatabase
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private var theme = "light"
    private lateinit var planName: String

    @Before fun setup() { hilt.inject(); waitFor("Bem-vindo ao LogGym") }
    @Test fun lightScreensPreserveMixedWorkoutAndNavigation() = inspect(ThemeMode.LIGHT)
    @Test fun darkScreensPreserveMixedWorkoutAndNavigation() = inspect(ThemeMode.DARK)
    private fun inspect(mode: ThemeMode) {
        theme = if (mode == ThemeMode.DARK) "dark" else "light"
        runBlocking { preferences.setThemeMode(mode) }
        compose.waitForIdle(); capture("01-onboarding")
        planName = "Full Body A5"
        runBlocking {
            val plan = plans.createPlan(planName); plans.setActivePlan(plan)
            val workout = plans.addWorkout(plan, "Força e estabilidade")
            val weight = (exercises.createCustom(ExerciseDraft("Supino com halteres A5")) as SaveExerciseResult.Saved).exerciseId
            val time = (exercises.createCustom(ExerciseDraft("Prancha abdominal A5", trackingType = TrackingType.TIME)) as SaveExerciseResult.Saved).exerciseId
            plans.addExerciseToWorkout(workout, weight, ExerciseTarget.Repetitions(3, 8, 12, Duration.ofSeconds(37)))
            plans.addExerciseToWorkout(workout, time, ExerciseTarget.Timed(2, 45, Duration.ofSeconds(60)))
            sessions.startSession(workout)
            val old = sessions.observeDraft().first()!!
            sessions.saveSetAndSetCompleted(old.exercises[0].sets[0].id, SetValues(22.5, 10), true)
            sessions.saveSetAndSetCompleted(old.exercises[1].sets[0].id, SetValues(durationSeconds = 45), true)
            sessions.finishSession(old.id)
            sessions.startSession(workout)
            val draft = sessions.observeDraft().first()!!
            sessions.saveSetAndSetCompleted(draft.exercises[0].sets[0].id, SetValues(0.0, 0), true)
            sessions.saveSetAndSetCompleted(draft.exercises[0].sets[2].id, SetValues(), true)
            sessions.skipRest(draft.id)
            // Stable historical date and Home time; business persistence is unchanged.
            database.openHelper.writableDatabase.execSQL("UPDATE sessions SET started_at = 1793102400000, finished_at = CASE WHEN status = 'COMPLETED' THEN 1793104200000 ELSE NULL END")
        }
        click("Pular por enquanto"); waitFor("Treino em andamento"); capture("02-home")
        tab("plans"); waitFor(planName); capture("03-plans")
        click(planName); waitFor("Adicionar divisão"); capture("04-plan-editor"); back()
        tab("exercises"); waitFor("Novo exercício"); capture("05-exercises")
        click("Novo exercício"); waitFor("Nome"); capture("06-exercise-create"); click("Cancelar")
        tab("history"); waitFor("Força e estabilidade"); capture("07-history")
        click("Força e estabilidade"); waitFor("Registro somente leitura"); capture("08-history-detail"); back()
        tab("settings"); waitFor("Aparência"); capture("09-settings")
        scrollTo("Backup e restauração"); capture("10-backup")
        click("Sobre backup e restauração"); capture("11-backup-info"); click("Entendi")
        click("Exportar backup"); capture("12-export-dialog"); back()
        tab("home"); click("Retomar"); waitFor("Carga (kg)")
        val draft = runBlocking { sessions.observeDraft().first()!! }
        runBlocking { sessions.saveSetAndSetCompleted(draft.exercises[0].sets[0].id, SetValues(0.0, 0), false)
            sessions.saveSetAndSetCompleted(draft.exercises[0].sets[0].id, SetValues(0.0, 0), true) }
        waitFor("Pular"); capture("13-session")
        val rest = runBlocking { sessions.observeDraft().first()!!.restTimer }
        click("Ver histórico de Supino com halteres A5"); waitFor("Histórico de Supino com halteres A5")
        capture("14-exercise-history"); click("Fechar histórico")
        assertEquals(rest, runBlocking { sessions.observeDraft().first()!!.restTimer })
        compose.onNodeWithTag("set-${draft.exercises[0].sets[1].id}-weight").performTextReplacement("-1")
        waitFor("Informe carga não negativa com até duas casas decimais."); capture("15-invalid-input")
        assertEquals(2, runBlocking { sessions.observeDraft().first()!!.completedSetCount })
    }
    private fun capture(name: String) {
        val phase = InstrumentationRegistry.getArguments().getString("evidencePhase") ?: return
        require(phase in setOf("before", "after"))
        compose.waitForIdle()
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "s8-evidence/$phase/$theme").apply { mkdirs() }
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(dir, "$name.png").outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        File(dir, "metadata.txt").writeText("phase=$phase\ntheme=$theme\nAPI=${android.os.Build.VERSION.SDK_INT}\npixels=${bitmap.width}x${bitmap.height}\nfontScale=${instrumentation.targetContext.resources.configuration.fontScale}\nfixture=Full Body A5; historical start=1793102400000; active rest approximately 37s\n")
        bitmap.recycle()
    }
    private fun tab(name: String) {
        compose.waitUntil(15000) { compose.onAllNodesWithTag("tab-$name").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("tab-$name").assertIsEnabled().performClick()
        compose.onNodeWithTag("tab-$name").assertIsSelected(); compose.waitForIdle()
    }
    private fun back() { instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK); compose.waitForIdle() }
    private fun matching(text: String) = hasText(text) or hasContentDescription(text)
    private fun scrollTo(text: String) {
        val lists = compose.onAllNodes(hasScrollAction())
        repeat(lists.fetchSemanticsNodes().size) { i -> runCatching { lists[i].performScrollToNode(matching(text)) } }
    }
    private fun waitFor(text: String) {
        compose.waitUntil(15000) {
            if (compose.onAllNodes(matching(text)).fetchSemanticsNodes().isEmpty()) scrollTo(text)
            compose.onAllNodes(matching(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }
    private fun click(text: String) {
        waitFor(text)
        val node = compose.onAllNodes(matching(text) and hasClickAction() and isEnabled() and !hasSetTextAction()).onFirst()
        runCatching { node.performScrollTo() }; node.assertIsDisplayed().performClick(); compose.waitForIdle()
    }
}
