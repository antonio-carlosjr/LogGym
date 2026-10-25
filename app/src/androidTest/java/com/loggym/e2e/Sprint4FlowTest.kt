package com.loggym.e2e

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loggym.MainActivity
import com.loggym.domain.model.ThemeMode
import com.loggym.domain.repository.*
import dagger.hilt.android.testing.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class Sprint4FlowTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 2) val compose = createAndroidComposeRule<MainActivity>()
    @Inject lateinit var plans: PlanRepository
    @Inject lateinit var sessions: SessionRepository
    @Inject lateinit var preferences: UserPreferencesRepository
    @Before fun setup() { hilt.inject() }

    @Test fun prescribeCreateClearRecoverAndDisableNextPrefill() {
        click("Criar plano personalizado")
        click("Novo plano"); type("Plano S4"); click("Criar")
        click("Ativar este plano")
        click("Adicionar divisão"); type("Misto S4"); click("Salvar")
        click("Adicionar exercício"); type("agachamento livre"); click("Agachamento livre")
        click("Usar padrão (3 × 8–12, 1:30)")
        click("Fixas")
        compose.onNodeWithText("Repetições").performTextReplacement("10")
        compose.onNodeWithText("Segundos").performTextReplacement("37")
        click("+15 s")
        compose.onNodeWithText("52").assertExists()
        click("−15 s")
        click("Salvar")
        waitFor("3 × 10 · 0:37")
        click("Adicionar exercício"); type("busca cancelada")
        click("Cadastrar exercício"); click("Cancelar")
        compose.onNodeWithText("busca cancelada").assertExists()
        click("Cadastrar exercício")
        compose.onNodeWithText("Nome").performTextInput("Prancha S4")
        click("Tempo (segundos)")
        compose.activityRule.scenario.recreate()
        waitFor("Prancha S4")
        click("Salvar")
        waitFor("Prancha S4")
        assertEquals(1, runBlocking { plans.observeActivePlan().first()!!.workouts.single().exercises.size })
        click("Prancha S4")
        click("Usar padrão (3 × 30 s, 1:30)")
        compose.onNodeWithText("Segundos").performTextReplacement("37")
        click("Salvar"); click("Voltar"); click("Voltar"); click("Iniciar")
        waitFor("Carga (kg)")
        compose.onAllNodesWithText("Carga (kg)").onFirst().performTextInput("0")
        compose.onAllNodesWithText("Repetições").onFirst().performTextInput("0")
        runBlocking { preferences.setThemeMode(ThemeMode.DARK) }
        compose.onAllNodesWithText("0").assertCountEquals(2)
        click("Concluir série 1 de Agachamento livre")
        waitFor("Duração (s)")
        compose.onAllNodesWithText("Duração (s)").onFirst().performTextInput("45")
        click("Concluir série 1 de Prancha S4")
        compose.waitUntil(10_000) {
            runBlocking { sessions.observeDraft().first()?.exercises?.sumOf { ex -> ex.sets.count { it.isCompleted } } == 2 }
        }
        val before = runBlocking { sessions.observeDraft().first()!! }
        click("Limpar séries de Agachamento livre"); click("Cancelar")
        assertEquals(before.exercises[0].sets, runBlocking { sessions.observeDraft().first()!!.exercises[0].sets })
        click("Limpar séries de Agachamento livre"); click("Limpar")
        compose.waitUntil(10_000) { runBlocking { sessions.observeDraft().first()!!.exercises[0].sets.none { it.isCompleted } } }
        val cleared = runBlocking { sessions.observeDraft().first()!! }
        assertEquals(before.restTimer, cleared.restTimer)
        assertEquals(before.exercises[1], cleared.exercises[1])
        assertEquals(before.exercises[0].sets.map { it.id }, cleared.exercises[0].sets.map { it.id })
        click("Voltar e salvar"); click("Retomar")
        assertEquals(cleared, runBlocking { sessions.observeDraft().first()!! })
        click("Finalizar treino"); click("Finalizar")
        click("Histórico"); click("Misto S4"); waitFor("Duração: 45 s")
        click("Voltar"); click("Voltar"); click("Configurações")
        click("Claro"); click("Usar valores do último treino")
        compose.waitUntil(10_000) { runBlocking { !preferences.settings.first().prefillEnabled } }
        click("Voltar"); click("Iniciar")
        compose.waitUntil(10_000) { runBlocking { sessions.observeDraft().first() != null } }
        val next = runBlocking { sessions.observeDraft().first()!! }
        assertTrue(next.exercises.flatMap { it.sets }.all { it.weightKg == null && it.reps == null && it.durationSeconds == null && !it.isCompleted })
    }

    private fun matching(text: String) = hasText(text) or hasContentDescription(text)
    private fun exists(text: String) = compose.onAllNodes(matching(text)).fetchSemanticsNodes().isNotEmpty()
    private fun waitFor(text: String) {
        compose.waitUntil(15_000) {
            exists(text) || run {
                val scrollables = compose.onAllNodes(hasScrollAction())
                repeat(scrollables.fetchSemanticsNodes().size) { index ->
                    runCatching { scrollables[index].performScrollToNode(matching(text)) }
                }
                exists(text)
            }
        }
    }
    private fun click(text: String) {
        waitFor(text)
        val target = matching(text) and !hasSetTextAction() and hasClickAction() and isEnabled()
        compose.waitUntil(10_000) { compose.onAllNodes(target).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(target).onFirst().performClick()
        compose.waitForIdle()
    }
    private fun type(text: String) { compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput(text); compose.waitForIdle() }
}
