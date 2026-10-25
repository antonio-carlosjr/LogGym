package com.loggym.e2e

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.loggym.MainActivity
import com.loggym.domain.model.TrackingType
import com.loggym.domain.repository.*
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import javax.inject.Inject

/** Fluxo real de navegação e Room: prescrever, executar parcialmente e consultar snapshots. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class Sprint3FlowTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 2) val compose = createAndroidComposeRule<MainActivity>()
    @Inject lateinit var exercises: ExerciseRepository
    @Inject lateinit var sessions: SessionRepository

    @Before fun setup() {
        hilt.inject()
        // Cadastro por tempo já é coberto pelo TimedExerciseFlowTest; aqui reutilizamos a biblioteca.
        runBlocking { exercises.createCustom(ExerciseDraft("Prancha S3", trackingType = TrackingType.TIME)) }
    }

    @Test fun onboardingMixedPartialSessionHistoryAndReadOnlyDetail() {
        click("Criar plano personalizado")
        click("Voltar")
        click("Histórico")
        waitFor("Você ainda não finalizou nenhum treino.")
        click("Voltar")
        click("Planos")
        click("Novo plano")
        type("Meu plano S3")
        click("Criar")
        click("Ativar este plano")
        click("Adicionar divisão")
        type("Treino misto S3")
        click("Salvar")
        click("Adicionar exercício")
        type("agachamento livre")
        click("Agachamento livre")
        click("Usar padrão (3 × 8–12, 1:30)")
        click("Salvar")
        click("Adicionar exercício")
        type("Prancha")
        click("Prancha S3")
        click("Usar padrão (3 × 30 s, 1:30)")
        click("Salvar")
        click("Voltar")
        click("Voltar")
        click("Iniciar")
        waitFor("Carga (kg)")
        compose.onAllNodesWithText("Carga (kg)").onFirst().performTextInput("0")
        compose.onAllNodesWithText("Repetições").onFirst().performTextInput("0")
        click("Concluir série 1 de Agachamento livre")
        waitFor("Duração (s)")
        compose.onAllNodesWithText("Duração (s)").onFirst().performTextInput("45")
        click("Concluir série 1 de Prancha S3")
        click("Concluir série 2 de Prancha S3")
        compose.waitUntil(10_000) { runBlocking { sessions.observeDraft().first()?.completedSetCount == 3 } }
        click("Finalizar treino")
        waitFor("3 séries não concluídas serão excluídas.")
        waitFor("1 série será salva com campos vazios.")
        click("Cancelar")
        assertNotNull(runBlocking { sessions.observeDraft().first() })
        click("Finalizar treino")
        click("Finalizar")
        click("Histórico")
        click("Treino misto S3")
        waitFor("Registro somente leitura")
        waitFor("Carga: 0 kg")
        waitFor("Repetições: 0")
        waitFor("Duração: 45 s")
        waitFor("Duração: não informada")
        waitFor("3 × 30 s · descanso 90 s")
        assertEquals(0, compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        val session = runBlocking { sessions.observeSession(sessions.observeHistory().first().single().id).first()!! }
        assertEquals(3, session.completedSetCount)
        assertEquals(2, session.exercises[1].sets.size)
        assertNull(runBlocking { sessions.observeDraft().first() })
        val output = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "s3-preview")
        output.mkdirs()
        File(output, "history-detail.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
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
        val node = compose.onAllNodes(target).onFirst()
        runCatching { node.performScrollTo() }
        node.assertIsDisplayed().performClick()
        compose.waitForIdle()
    }
    private fun type(text: String) { compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput(text); compose.waitForIdle() }
}
