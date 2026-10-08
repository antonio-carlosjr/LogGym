package com.loggym.e2e

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.loggym.MainActivity
import com.loggym.awaitFirst
import com.loggym.domain.repository.SessionRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import javax.inject.Inject

/**
 * Critério de pronto da Sprint 1 (docs/SPRINTS.md), dirigido pela interface:
 * um plano personalizado é criado do zero e iniciado pela home, e os nativos ficam protegidos.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp")
// Medição real de texto; no modo legado os textos têm largura quase zero e os layouts não estabilizam.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Sprint1FlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        // Os diálogos do Compose mantêm animações que, com o relógio automático, nunca deixam a UI ociosa
        // no Robolectric. O relógio passa a avançar manualmente.
        composeRule.mainClock.autoAdvance = false
    }

    /**
     * Fluxo pela interface sem diálogos com campo de texto: no Robolectric, um OutlinedTextField
     * dentro de AlertDialog nunca fica ocioso, mesmo em Material 3 puro. O fluxo com digitação
     * em diálogos (plano criado do zero) roda como teste instrumentado em androidTest.
     */
    @Test
    fun `plano pronto ativado pela area de planos e iniciado pela home`() {
        // Onboarding com aviso de saúde, pulando a escolha de plano (RF-03, RF-06, RF-147).
        waitForText("Bem-vindo ao LogGym")
        waitForText("O LogGym não substitui a orientação de um profissional de educação física ou de saúde.")
        click("Pular por enquanto")

        // Sem plano ativo, a home direciona para planos (RF-07).
        waitForText("Nenhum plano ativo")
        click("Ir para planos")

        // Plano pronto vira cópia local e o app pergunta se deve ativar (RF-37, RF-146).
        click("Usar um plano pronto")
        click("Full Body 3x")
        click("Ativar")
        waitForText("Plano ativo")

        // A home lista as divisões do plano ativo e inicia o treino (RF-58).
        click("Voltar")
        waitForText("Escolha a divisão de hoje")
        waitForText("Full Body C")
        click("Iniciar")

        // Rascunho com snapshot do plano e séries previstas, sem histórico para pré-preencher (RF-60, RF-61, RF-72).
        val draft = runBlocking { sessionRepository.observeDraft().awaitFirst { it != null } }!!
        assertEquals("Full Body 3x", draft.planName)
        assertEquals("Full Body A", draft.workoutName)
        assertEquals(5, draft.exercises.size)
        draft.exercises.forEach { exercise ->
            assertEquals(exercise.target.targetSets, exercise.sets.size)
            assertEquals(true, exercise.sets.all { it.weightKg == null && it.reps == null && !it.isCompleted })
        }
    }

    /** Espera o texto aparecer, rolando as listas quando ele está fora da área visível. */
    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.mainClock.advanceTimeByFrame()
            exists(text) || scrollTo(text)
        }
    }

    /** Texto visível ou descrição de acessibilidade (ícones e FABs). */
    private fun matching(text: String) = hasText(text) or hasContentDescription(text)

    private fun exists(text: String) = composeRule.onAllNodes(matching(text)).fetchSemanticsNodes().isNotEmpty()

    private fun scrollTo(text: String): Boolean {
        val scrollables = composeRule.onAllNodes(hasScrollAction())
        repeat(scrollables.fetchSemanticsNodes().size) { index ->
            runCatching { scrollables[index].performScrollToNode(matching(text)) }
        }
        return exists(text)
    }

    private fun click(text: String) {
        waitForText(text)
        composeRule.onAllNodes(matching(text)).onFirst().performClick()
        settle()
    }

    /** Avança o suficiente para as transições de tela e de diálogo terminarem. */
    private fun settle() {
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
    }
}
