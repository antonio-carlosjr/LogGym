package com.loggym.e2e

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.loggym.MainActivity
import com.loggym.domain.model.InputRules
import com.loggym.domain.repository.SessionRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import java.io.File
import javax.inject.Inject

/**
 * Critério de pronto da Sprint 1 (docs/SPRINTS.md), no dispositivo:
 * um plano personalizado é criado do zero e iniciado pela home, e os exercícios nativos
 * não podem ser editados nem excluídos.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class Sprint1DefinitionOfDoneTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    /** O onboarding depende do DataStore, que sobrevive entre execuções no dispositivo. */
    @get:Rule(order = 1)
    val freshPreferences = object : ExternalResource() {
        override fun before() {
            val filesDir = InstrumentationRegistry.getInstrumentation().targetContext.filesDir
            File(filesDir, "datastore").deleteRecursively()
        }
    }

    @get:Rule(order = 2)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Before
    fun setUp() = hiltRule.inject()

    @Test
    fun planoPersonalizadoCriadoDoZeroEIniciadoPelaHome() {
        // Onboarding com aviso de saúde (RF-03, RF-06, RF-147).
        waitFor("Bem-vindo ao LogGym")
        waitFor("O LogGym não substitui a orientação de um profissional de educação física ou de saúde.")
        click("Pular por enquanto")

        // Nativos: sem edição nem exclusão (RF-18, RF-19), com observação do usuário (RF-133).
        waitFor("Nenhum plano ativo")
        click("Exercícios")
        typeInto("stiff")
        click("Stiff")
        waitFor("Exercício nativo")
        waitFor("Minha observação")
        assertAbsent("Editar")
        assertAbsent("Excluir")
        click("Fechar")
        click("Voltar")

        // Sem plano ativo, a home direciona para planos (RF-07). Plano criado do zero (RF-23, RF-24).
        click("Ir para planos")
        click("Novo plano")
        typeInto("Meu ABC")
        click("Criar")

        // Editor: ativar, criar divisão e adicionar exercício com o padrão (RF-31, RF-41, RF-46, RF-143).
        click("Ativar este plano")
        waitFor("Plano ativo")
        click("Adicionar divisão")
        typeInto("Treino A")
        click("Salvar")
        waitFor("Treino A")
        click("Adicionar exercício")
        typeInto("agachamento livre")
        click("Agachamento livre")
        click("Usar padrão (3 × 8–12, 1:30)")
        click("Salvar")
        waitFor("3 × 8–12 · 1:30")

        // Home: inicia o treino da divisão criada (RF-58).
        click("Voltar")
        waitFor("Usar um plano pronto")
        click("Voltar")
        waitFor("Escolha a divisão de hoje")
        waitFor("Meu ABC")
        click("Iniciar")

        val draft = runBlocking { withTimeout(10_000) { sessionRepository.observeDraft().first { it != null } } }!!
        assertEquals("Meu ABC", draft.planName)
        assertEquals("Treino A", draft.workoutName)
        val exercise = draft.exercises.single()
        assertEquals("Agachamento livre", exercise.name)
        assertEquals(InputRules.DEFAULT_TARGET, exercise.target)
        assertEquals(InputRules.DEFAULT_TARGET.targetSets, exercise.sets.size)
    }

    /** Texto visível ou descrição de acessibilidade (ícones e FABs). */
    private fun matching(text: String) = hasText(text) or hasContentDescription(text)

    private fun exists(text: String) = composeRule.onAllNodes(matching(text)).fetchSemanticsNodes().isNotEmpty()

    /** Espera o elemento aparecer, rolando as listas quando ele está fora da área visível. */
    private fun waitFor(text: String) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            exists(text) || run {
                val scrollables = composeRule.onAllNodes(hasScrollAction())
                repeat(scrollables.fetchSemanticsNodes().size) { index ->
                    runCatching { scrollables[index].performScrollToNode(matching(text)) }
                }
                exists(text)
            }
        }
    }

    private fun click(text: String) {
        waitFor(text)
        composeRule.onAllNodes(matching(text)).onFirst().performClick()
        composeRule.waitForIdle()
    }

    private fun typeInto(text: String) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(hasSetTextAction()).onFirst().performTextInput(text)
        composeRule.waitForIdle()
    }

    private fun assertAbsent(text: String) {
        assertEquals("\"$text\" não deveria aparecer", 0, composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().size)
    }
}
