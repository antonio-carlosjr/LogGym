package com.loggym.e2e

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loggym.domain.backup.*
import com.loggym.ui.settings.BackupSection
import com.loggym.ui.theme.LogGymTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Contratos reais do SAF; registry controlado evita depender de um provider/idioma específico. */
@RunWith(AndroidJUnit4::class)
class BackupSettingsTest {
    @get:Rule val compose = createComposeRule()
    private val state = mutableStateOf(BackupState())
    private var mode: BackupMode? = null
    private var confirmed: String? = null
    private var cancelled = false
    private var delivered: Pair<String, String?>? = null
    private var imported: String? = null
    private var request = 0
    private var intent: Intent? = null
    private val registry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
            request = requestCode
            intent = contract.createIntent(ApplicationProvider.getApplicationContext<Context>(), input)
        }
    }
    @Before fun setup() {
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides object : ActivityResultRegistryOwner {
                override val activityResultRegistry = registry
            }) {
                LogGymTheme(darkTheme = true) { Column {
                    BackupSection(state.value, true, { mode = it }, { t, uri -> delivered = t to uri },
                        { imported = it }, { confirmed = it }, { cancelled = true })
                } }
            }
        }
    }
    @Test fun exportOffersTwoModesThenUsesCreateDocumentAndAcceptsCancellation() {
        compose.onNodeWithText("Exportar backup").performClick()
        compose.onNodeWithText("Completo: inclui planos, exercícios, configurações, histórico e treino em andamento.\n\nSem histórico: inclui planos, exercícios e configurações, sem sessões.").assertExists()
        compose.onNodeWithText("Este arquivo não está criptografado. Quem tiver acesso a ele poderá ler seus dados.").assertExists()
        compose.onNodeWithText("Sem histórico").performClick()
        assertEquals(BackupMode.WITHOUT_HISTORY, mode)
        compose.runOnIdle { state.value = BackupState(BackupStep.DESTINATION, "token", "teste.loggym") }
        compose.waitUntil(5000) { intent != null }
        assertEquals(Intent.ACTION_CREATE_DOCUMENT, intent!!.action)
        assertEquals("teste.loggym", intent!!.getStringExtra(Intent.EXTRA_TITLE))
        compose.runOnIdle { registry.dispatchResult(request, Activity.RESULT_CANCELED, null) }
        assertEquals("token" to null, delivered)
    }
    @Test fun informationIsAvailableOnDemandAndDoesNotStartAnOperation() {
        val explanation = hasText("Completo: inclui", substring = true)
        val warning = "Este arquivo não está criptografado. Quem tiver acesso a ele poderá ler seus dados."
        compose.onNode(explanation).assertDoesNotExist()
        compose.onNodeWithText(warning).assertDoesNotExist()
        compose.onNodeWithContentDescription("Sobre backup e restauração").performClick()
        compose.onNode(explanation).assertExists()
        compose.onNodeWithText(warning).assertExists()
        assertNull(mode); assertNull(confirmed); assertNull(intent)
        compose.onNodeWithText("Entendi").performClick()
        compose.onNode(explanation).assertDoesNotExist()
        compose.onNodeWithText(warning).assertDoesNotExist()
        compose.onNodeWithText("Exportar backup").assertIsEnabled()
    }
    @Test fun importUsesOpenDocumentAndDoesNotConfirmBeforeExplicitAction() {
        compose.onNodeWithText("Importar backup").performClick()
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, intent!!.action)
        compose.runOnIdle { registry.dispatchResult(request, Activity.RESULT_OK, Intent().setData(Uri.parse("content://provider/backup"))) }
        assertEquals("content://provider/backup", imported)
        compose.runOnIdle { state.value = BackupState(BackupStep.PREVIEW, "preview", summary = BackupSummary("preview", BackupMode.WITHOUT_HISTORY, 1000, 2, 1, 1, 0, 0, false)) }
        compose.onNodeWithText("O histórico e o treino em andamento atuais serão removidos.").assertExists()
        assertNull(confirmed)
        compose.onNodeWithText("Substituir dados").performClick()
        assertEquals("preview", confirmed)
    }
    @Test fun processingDisablesActionsAndPreviewCanBeCancelledWithoutMutation() {
        compose.runOnIdle { state.value = BackupState(BackupStep.WRITING) }
        compose.onNodeWithText("Exportar backup").assertIsNotEnabled()
        compose.onNodeWithText("Importar backup").assertIsNotEnabled()
        compose.runOnIdle { state.value = BackupState(BackupStep.PREVIEW, "p", summary = BackupSummary("p", BackupMode.FULL, 1000, 2, 1, 1, 3, 8, true)) }
        compose.onNodeWithText("Inclui treino em andamento.").assertExists()
        compose.onNodeWithText("Cancelar").performClick()
        assertTrue(cancelled); assertNull(confirmed)
    }
}
