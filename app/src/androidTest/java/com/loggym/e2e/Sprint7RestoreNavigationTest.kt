package com.loggym.e2e

import android.content.Context
import android.net.Uri
import android.view.KeyEvent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.loggym.MainActivity
import com.loggym.data.backup.BackupEngine
import com.loggym.data.local.LogGymDatabase
import com.loggym.domain.backup.*
import com.loggym.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.*
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class Sprint7RestoreNavigationTest {
    @Module @InstallIn(SingletonComponent::class)
    object EngineModule {
        @Provides @Singleton fun engine(@ApplicationContext context: Context, database: LogGymDatabase,
            preferences: UserPreferencesRepository, coordinator: PersistenceCoordinator, clock: Clock) =
            BackupEngine(context, database, preferences, coordinator, clock)
    }
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    @Inject lateinit var backup: BackupRepository
    @Inject lateinit var engine: BackupEngine
    @Inject lateinit var plans: PlanRepository
    @Inject lateinit var preferences: UserPreferencesRepository
    @Inject lateinit var coordinator: PersistenceCoordinator
    private lateinit var file: File
    @Before fun setup() {
        hilt.inject()
        file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "s7-navigation.loggym")
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
        runBlocking { plans.createPlan("Plano original S7") }
    }
    @After fun cleanup() { engine.checkpoint = {}; file.delete() }
    private fun tab(name: String) {
        compose.waitUntil(10000) { compose.onAllNodesWithTag("tab-$name").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("tab-$name").assertIsEnabled().performClick()
        compose.onNodeWithTag("tab-$name").assertIsSelected()
    }
    private fun stage(step: BackupStep) {
        compose.waitUntil(15000) { backup.state.value.step == step || backup.state.value.step == BackupStep.ERROR }
        assertEquals(step, backup.state.value.step)
    }
    private fun export(mode: BackupMode) {
        // Fora de Configurações para não lançar SAF: transferência controlada pelo teste.
        compose.runOnIdle { backup.prepareExport(mode) }; stage(BackupStep.DESTINATION)
        compose.runOnIdle { backup.writeExport(backup.state.value.token!!, Uri.fromFile(file).toString()) }
        stage(BackupStep.SUCCESS)
    }
    private fun populateSavedRoots() {
        tab("exercises")
        compose.onNode(hasSetTextAction()).performTextReplacement("contexto antigo")
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        tab("history"); tab("plans"); tab("settings"); tab("home")
    }
    private fun restore(mode: BackupMode, rollback: Boolean = false) {
        export(mode)
        populateSavedRoots()
        val id = runBlocking { plans.observePlans().first().single().id }
        runBlocking { plans.renamePlan(id, "Antes de restaurar S7") }
        if (rollback) engine.checkpoint = { if (it == "preferences_applied") error("falha controlada S7") }
        compose.runOnIdle { backup.validateImport(Uri.fromFile(file).toString()) }; stage(BackupStep.PREVIEW)
        compose.runOnIdle { backup.confirmImport(backup.state.value.token!!) }
        if (rollback) {
            compose.waitUntil(15000) { backup.state.value.step == BackupStep.ERROR }
            assertEquals(BackupFailure.ROLLED_BACK, backup.state.value.error)
        } else stage(BackupStep.SUCCESS)
        compose.waitUntil(10000) { compose.onAllNodesWithTag("tab-settings").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("tab-settings").assertIsSelected()
        assertEquals(1L, coordinator.generation.value)
        compose.activityRule.scenario.recreate()
        tab("exercises")
        compose.onNode(hasSetTextAction()).assert(SemanticsMatcher.expectValue(
            androidx.compose.ui.semantics.SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
        tab("history"); tab("plans")
        compose.waitUntil(10000) { compose.onAllNodesWithText(if (rollback) "Antes de restaurar S7" else "Plano original S7").fetchSemanticsNodes().isNotEmpty() }
        // ViewModels novos precisam admitir comandos na geração atual, não só apresentar dados.
        tab("settings")
        val before = runBlocking { preferences.settings.first().prefillEnabled }
        compose.onNodeWithText("Usar valores do último treino").performClick()
        compose.waitUntil(10000) { runBlocking { preferences.settings.first().prefillEnabled != before } }
        tab("home")
        runBlocking { coordinator.capture {} }
    }
    @Test fun fullRestoreClearsAllSavedRootsAndOldCommands() = restore(BackupMode.FULL)
    @Test fun restoreWithoutHistoryClearsAllSavedRootsAndOldCommands() = restore(BackupMode.WITHOUT_HISTORY)
    @Test fun rollbackAfterGenerationChangeAlsoClearsSavedRoots() = restore(BackupMode.FULL, rollback = true)
    @Test fun cancelPreviewPreservesSavedLibraryContextAndUnlocksTabs() {
        export(BackupMode.FULL)
        populateSavedRoots()
        tab("settings")
        compose.runOnIdle { backup.validateImport(Uri.fromFile(file).toString()) }; stage(BackupStep.PREVIEW)
        compose.onNodeWithTag("tab-exercises").assertIsNotEnabled()
        compose.onNodeWithText("Cancelar").performClick()
        compose.waitUntil(10000) { backup.state.value.step == BackupStep.IDLE }
        tab("exercises")
        compose.onNode(hasSetTextAction()).assertTextContains("contexto antigo")
        assertEquals(0L, coordinator.generation.value)
    }
}
