package com.loggym.e2e

import android.view.KeyEvent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.loggym.MainActivity
import com.loggym.domain.backup.*
import com.loggym.domain.model.ThemeMode
import com.loggym.domain.model.TrackingType
import kotlinx.coroutines.flow.first
import com.loggym.domain.repository.*
import dagger.hilt.android.testing.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class Sprint7NavigationTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    @Inject lateinit var plans: PlanRepository
    @Inject lateinit var backup: BackupRepository
    @Inject lateinit var exercises: ExerciseRepository
    @Inject lateinit var preferences: UserPreferencesRepository
    @Before fun setup() {
        hilt.inject()
        compose.waitUntil(15000) {
            if (compose.onAllNodesWithText("Pular por enquanto").fetchSemanticsNodes().isEmpty()) {
                val lists = compose.onAllNodes(hasScrollAction())
                repeat(lists.fetchSemanticsNodes().size) { index ->
                    runCatching { lists[index].performScrollToNode(hasText("Pular por enquanto")) }
                }
            }
            compose.onAllNodesWithText("Pular por enquanto").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Pular por enquanto").performScrollTo().assertIsDisplayed().performClick()
        awaitTab("home")
    }
    private fun awaitTab(tab: String) {
        compose.waitUntil(10000) { compose.onAllNodesWithTag("tab-$tab").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun tab(tab: String) {
        awaitTab(tab)
        compose.onNodeWithTag("tab-$tab").performClick()
        compose.onNodeWithTag("tab-$tab").assertIsSelected()
    }
    private fun back() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }
    @Test fun rootsDoNotAccumulateAndInternalDestinationsHideTheBar() {
        runBlocking { plans.createPlan("Plano S7") }
        for (name in listOf("plans", "exercises", "history", "settings")) {
            tab(name); tab(name)
            back()
            compose.onNodeWithTag("tab-home").assertIsSelected()
        }
        tab("plans")
        compose.onNodeWithText("Plano S7").performClick()
        compose.onNodeWithTag("root-navigation").assertDoesNotExist()
        back()
        compose.onNodeWithTag("tab-plans").assertIsSelected()
        back()
        compose.onNodeWithTag("tab-home").assertIsSelected()
    }
    @Test fun libraryQueryFilterSurviveTabsBackAndRecreation() {
        tab("exercises")
        compose.onNode(hasSetTextAction()).performTextReplacement("supino")
        back() // primeiro fecha IME, sem sair da Biblioteca
        compose.onNodeWithTag("tab-exercises").assertIsSelected()
        compose.onNodeWithText("Peito").performClick()
        tab("history"); tab("exercises")
        compose.onNode(hasSetTextAction()).assertTextContains("supino")
        compose.onNodeWithText("Peito").assertIsSelected()
        compose.activityRule.scenario.recreate()
        awaitTab("exercises")
        compose.onNodeWithTag("tab-exercises").assertIsSelected()
        compose.onNode(hasSetTextAction()).assertTextContains("supino")
        compose.onNodeWithText("Peito").assertIsSelected()
        back()
        compose.onNodeWithTag("tab-home").assertIsSelected()
        tab("exercises")
        compose.onNode(hasSetTextAction()).assertTextContains("supino")
        compose.onNodeWithText("Peito").assertIsSelected()
    }
    @Test fun libraryScrollSurvivesTabSwitchAndActivityRecreation() {
        tab("exercises")
        fun loaded() = compose.waitUntil(10000) {
            compose.onAllNodesWithText("Nativo").fetchSemanticsNodes().isNotEmpty()
        }
        fun position() = compose.onAllNodes(hasScrollAction()).onLast().fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange].value()
        loaded()
        compose.onAllNodes(hasScrollAction()).onLast().performScrollToIndex(35)
        val expected = position()
        assertTrue(expected > 0f)
        tab("plans"); tab("exercises"); loaded()
        assertEquals(expected, position(), 1f)
        compose.activityRule.scenario.recreate()
        awaitTab("exercises"); loaded()
        assertEquals(expected, position(), 1f)
    }
    @Test fun libraryUsesSharedCreationAndPreservesQueryAfterCancelSaveAndRecreation() {
        runBlocking { preferences.setThemeMode(ThemeMode.DARK) }
        tab("exercises")
        compose.onNode(hasSetTextAction()).performClick().performTextReplacement("supino")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("root-navigation").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("tab-exercises").assertIsSelected()
        click("Novo exercício")
        compose.onNodeWithTag("root-navigation").assertDoesNotExist()
        compose.onNodeWithText("Nome").performTextInput("Cancelado S7")
        click("Cancelar")
        compose.onNodeWithTag("tab-exercises").assertIsSelected()
        compose.onNode(hasSetTextAction()).assertTextContains("supino")
        assertTrue(runBlocking { exercises.observeAll().first().none { it.name == "Cancelado S7" } })
        click("Novo exercício")
        compose.onNodeWithText("Nome").performTextInput("Prancha compartilhada S7")
        click("Tempo (segundos)")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Nome").assertTextContains("Prancha compartilhada S7")
        click("Salvar")
        awaitTab("exercises")
        compose.onNodeWithTag("tab-exercises").assertIsSelected()
        compose.onNode(hasSetTextAction()).assertTextContains("supino")
        val created = runBlocking { exercises.observeAll().first().filter { it.name == "Prancha compartilhada S7" } }
        assertEquals(1, created.size)
        assertEquals(TrackingType.TIME, created.single().trackingType)
        compose.onNode(hasSetTextAction()).performClick().performTextReplacement("")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("root-navigation").fetchSemanticsNodes().isNotEmpty() }
        runBlocking { preferences.setThemeMode(ThemeMode.LIGHT) }
        tab("home"); tab("exercises")
        compose.activityRule.scenario.recreate()
        awaitTab("exercises")
        compose.waitUntil(15000) {
            val lists = compose.onAllNodes(hasScrollAction())
            repeat(lists.fetchSemanticsNodes().size) { i ->
                runCatching { lists[i].performScrollToNode(hasText("Prancha compartilhada S7")) }
            }
            compose.onAllNodesWithText("Prancha compartilhada S7").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Prancha compartilhada S7").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).assert(SemanticsMatcher.expectValue(
            androidx.compose.ui.semantics.SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
    }

    @Test fun existingPlanKeepsPickerQueryOnCancelAndSearchesCreatedExerciseOnlyAfterSave() {
        val planId = runBlocking {
            preferences.setThemeMode(ThemeMode.LIGHT)
            plans.createPlan("Editar plano S7").also { plans.addWorkout(it, "Divisão S7") }
        }
        tab("plans"); click("Editar plano S7"); click("Adicionar exercício")
        compose.onNode(hasSetTextAction()).performTextReplacement("busca anterior")
        click("Cadastrar exercício")
        compose.onNodeWithTag("root-navigation").assertDoesNotExist()
        compose.onNodeWithText("Nome").performTextInput("Voltar sem salvar S7")
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNode(hasSetTextAction()).assertTextContains("busca anterior")
        assertTrue(runBlocking { exercises.observeAll().first().none { it.name == "Voltar sem salvar S7" } })
        click("Cadastrar exercício")
        compose.onNodeWithText("Nome").performTextInput("Carga compartilhada S7")
        click("Salvar")
        compose.waitUntil(10000) {
            compose.onAllNodes(hasSetTextAction() and hasText("Carga compartilhada S7")).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(runBlocking { plans.observePlan(planId).first()!!.workouts.single().exercises.isEmpty() })
        click("Carga compartilhada S7")
        click("Usar padrão (3 × 8–12, 1:30)")
        click("Salvar")
        compose.waitUntil(10000) {
            runBlocking { plans.observePlan(planId).first()!!.workouts.single().exercises.size == 1 }
        }
    }

    private fun click(text: String) {
        val match = (hasText(text) or hasContentDescription(text)) and !hasSetTextAction()
        compose.waitUntil(15000) {
            if (compose.onAllNodes(match and hasClickAction() and isEnabled()).fetchSemanticsNodes().isEmpty()) {
                val lists = compose.onAllNodes(hasScrollAction())
                repeat(lists.fetchSemanticsNodes().size) { i -> runCatching { lists[i].performScrollToNode(match) } }
            }
            compose.onAllNodes(match and hasClickAction() and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        val node = compose.onAllNodes(match and hasClickAction() and isEnabled()).onFirst()
        runCatching { node.performScrollTo() }
        node.assertIsDisplayed().performClick()
        compose.waitForIdle()
    }

    @Test fun backupDestinationLocksNavigationAndCancelPreservesContext() {
        tab("settings")
        compose.runOnIdle { backup.prepareExport(BackupMode.FULL) }
        compose.waitUntil(15000) { backup.state.value.step == BackupStep.DESTINATION || backup.state.value.step == BackupStep.ERROR }
        assertEquals(BackupStep.DESTINATION, backup.state.value.step)
        // Seletor SAF pode estar em outra janela; o estado do repositório é cancelado explicitamente.
        compose.runOnIdle { backup.writeExport(backup.state.value.token!!, null) }
        back()
        compose.waitForIdle()
        awaitTab("settings")
        compose.onNodeWithTag("tab-settings").assertIsSelected()
        tab("home")
    }
}
