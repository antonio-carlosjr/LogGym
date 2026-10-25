package com.loggym.e2e

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loggym.MainActivity
import com.loggym.domain.logic.SetValues
import com.loggym.domain.model.*
import com.loggym.domain.repository.*
import dagger.hilt.android.testing.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.Duration
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class Sprint6FlowTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 2) val compose = createAndroidComposeRule<MainActivity>()
    @Inject lateinit var plans: PlanRepository
    @Inject lateinit var exercises: ExerciseRepository
    @Inject lateinit var sessions: SessionRepository
    @Inject lateinit var preferences: UserPreferencesRepository
    @Before fun setup() {
        hilt.inject()
        waitFor("Pular por enquanto")
        runBlocking {
            val plan = plans.createPlan("Plano S6"); plans.setActivePlan(plan)
            val workout = plans.addWorkout(plan, "Misto S6")
            val weight = (exercises.createCustom(ExerciseDraft("Halter S6")) as SaveExerciseResult.Saved).exerciseId
            val time = (exercises.createCustom(ExerciseDraft("Prancha S6", trackingType = TrackingType.TIME)) as SaveExerciseResult.Saved).exerciseId
            plans.addExerciseToWorkout(workout, weight, ExerciseTarget.Repetitions(2, 8, 12, Duration.ofSeconds(37)))
            plans.addExerciseToWorkout(workout, time, ExerciseTarget.Timed(1, 30, Duration.ofSeconds(60)))
            sessions.startSession(workout)
            val old = sessions.observeDraft().first()!!
            sessions.saveSetAndSetCompleted(old.exercises[0].sets[0].id, SetValues(0.0, 8), true)
            sessions.saveSetAndSetCompleted(old.exercises[1].sets[0].id, SetValues(durationSeconds = 45), true)
            sessions.finishSession(old.id)
            sessions.startSession(workout)
        }
    }
    private fun draft() = runBlocking { sessions.observeDraft().first()!! }
    @Test fun optionalAutomationHistoryAndClearPreserveWorkoutAndRest() {
        click("Pular por enquanto"); click("Configurações")
        compose.onNodeWithText("Concluir séries automaticamente").assertIsOff()
        click("Concluir séries automaticamente"); click("Voltar"); click("Retomar")
        field("Repetições", 0).performImeAction()
        assertEquals(0, draft().completedSetCount)
        field("Carga (kg)", 0).performTextReplacement("22,5")
        field("Carga (kg)", 0).performImeAction()
        assertEquals(0, draft().completedSetCount)
        field("Repetições", 0).performImeAction()
        awaitCount(1)
        field("Carga (kg)", 1).performTextReplacement("30")
        field("Repetições", 1).performTextReplacement("10")
        val rest = draft().restTimer
        click("Ver histórico de Halter S6")
        waitFor("Histórico de Halter S6")
        compose.onNodeWithText("Carga: 0 kg").assertExists()
        compose.onNodeWithText("Repetições: 8").assertExists()
        compose.activityRule.scenario.recreate()
        waitFor("Histórico de Halter S6")
        compose.onNodeWithText("Carga: 0 kg").assertExists()
        click("Fechar histórico")
        compose.onNodeWithText("30").assertExists(); compose.onNodeWithText("10").assertExists()
        assertEquals(1, draft().completedSetCount); assertEquals(rest, draft().restTimer)
        field("Carga (kg)", 1).performTextReplacement("30,")
        click("Ver histórico de Halter S6"); waitFor("Histórico de Halter S6")
        Espresso.pressBack()
        compose.onNodeWithText("30,").assertExists()
        assertEquals(1, draft().completedSetCount); assertEquals(rest, draft().restTimer)
        compose.activityRule.scenario.recreate()
        field("Carga (kg)", 1).performTextReplacement("30,5")
        field("Repetições", 1).performClick()
        field("Duração (s)", 0).performClick()
        awaitCount(2)
        field("Duração (s)", 0).performImeAction()
        assertEquals(2, draft().completedSetCount)
        field("Duração (s)", 0).performTextReplacement("46")
        field("Duração (s)", 0).performImeAction()
        awaitCount(3)
        val beforeClear = draft()
        click("Limpar séries de Halter S6"); click("Limpar")
        compose.waitUntil(10000) { draft().exercises[0].sets.none { it.isCompleted } }
        assertEquals(beforeClear.restTimer, draft().restTimer)
        assertEquals(beforeClear.exercises[1], draft().exercises[1])
        compose.activityRule.scenario.recreate()
        field("Repetições", 0).performImeAction()
        assertEquals(1, draft().completedSetCount)
        field("Carga (kg)", 0).performTextReplacement("0")
        field("Repetições", 0).performTextReplacement("0")
        field("Repetições", 0).performImeAction(); awaitCount(2)
        click("Finalizar treino"); click("Finalizar")
        click("Histórico"); click("Misto S6")
        waitFor("Duração: 46 s")
        compose.onNodeWithText("Carga: 0 kg").assertExists()
    }
    private fun awaitCount(count: Int) { compose.waitUntil(10000) { draft().completedSetCount == count } }
    private fun field(label: String, index: Int): SemanticsNodeInteraction {
        val suffix = when (label) { "Carga (kg)" -> "weight"; "Repetições" -> "reps"; else -> "duration" }
        val exercise = draft().exercises.first { it.trackingType == if (suffix == "duration") TrackingType.TIME else TrackingType.WEIGHT_REPS }
        val tag = "set-${exercise.sets[index].id}-$suffix"
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag)
    }
    private fun match(text: String) = hasText(text) or hasContentDescription(text)
    private fun waitFor(text: String) {
        compose.waitUntil(15000) {
            if (compose.onAllNodes(match(text)).fetchSemanticsNodes().isEmpty()) {
                val scrolls = compose.onAllNodes(hasScrollAction())
                repeat(scrolls.fetchSemanticsNodes().size) { index -> runCatching { scrolls[index].performScrollToNode(match(text)) } }
            }
            compose.onAllNodes(match(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }
    private fun click(text: String) {
        waitFor(text)
        val node = compose.onAllNodes(match(text) and hasClickAction() and isEnabled()).onFirst()
        runCatching { node.performScrollTo() }; node.performClick(); compose.waitForIdle()
    }
}
