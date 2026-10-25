package com.loggym.data.backup

import android.content.Context
import androidx.core.net.toUri
import android.provider.DocumentsContract
import com.loggym.domain.backup.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Trabalho em primeiro plano com duração da aplicação: rotação não cancela a transferência. */
@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: BackupEngine,
) : BackupRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(BackupState())
    override val state = mutableState.asStateFlow()
    private var work: Job? = null
    override suspend fun recover() { withContext(Dispatchers.IO) { engine.recover() } }
    private fun available() = state.value.step in setOf(BackupStep.IDLE, BackupStep.SUCCESS, BackupStep.ERROR)
    private fun launch(step: BackupStep, token: String, action: suspend () -> Unit) {
        mutableState.value = BackupState(step, token)
        work = scope.launch {
            try { withContext(Dispatchers.IO) { action() } }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) {
                engine.file(token, "export").delete()
                engine.file(token, "import").delete()
                mutableState.value = BackupState(BackupStep.ERROR, error = BackupFailure.OPERATION)
            }
        }
    }
    override fun prepareExport(mode: BackupMode) {
        if (!available()) return
        val token = UUID.randomUUID().toString()
        launch(BackupStep.PREPARING, token) {
            engine.export(token, mode)
            mutableState.value = BackupState(BackupStep.DESTINATION, token, "LogGym-${if (mode == BackupMode.FULL) "completo" else "sem-historico"}-$token.loggym")
        }
    }
    override fun writeExport(token: String, uri: String?) {
        if (state.value.step != BackupStep.DESTINATION || state.value.token != token) return
        if (uri == null) { cancel(); return }
        launch(BackupStep.WRITING, token) {
            val file = engine.file(token, "export")
            try {
                context.contentResolver.openOutputStream(uri.toUri(), "w").let { requireNotNull(it) }.use { output ->
                    file.inputStream().use { it.copyTo(output) }; output.flush()
                }
                mutableState.value = BackupState(BackupStep.SUCCESS)
            } catch (error: Exception) {
                runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri.toUri()) }
                throw error
            } finally { file.delete() }
        }
    }
    override fun validateImport(uri: String) {
        if (!available()) return
        val token = UUID.randomUUID().toString()
        launch(BackupStep.VALIDATING, token) {
            val target = engine.file(token, "import")
            try {
                requireNotNull(context.contentResolver.openInputStream(uri.toUri())).use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(8192); var size = 0L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val n = input.read(buffer); if (n < 0) break
                            size += n; require(size <= BackupFormat.MAX_BYTES)
                            output.write(buffer, 0, n)
                        }
                    }
                }
                val summary = engine.validate(target, token)
                mutableState.value = BackupState(BackupStep.PREVIEW, token, summary = summary)
            } catch (error: Throwable) { target.delete(); throw error }
        }
    }
    override fun confirmImport(token: String) {
        if (state.value.step != BackupStep.PREVIEW || state.value.token != token) return
        launch(BackupStep.RESTORING, token) {
            val restored = engine.restore(token)
            mutableState.value = if (restored) BackupState(BackupStep.SUCCESS)
                else BackupState(BackupStep.ERROR, error = BackupFailure.ROLLED_BACK)
            engine.file(token, "import").delete()
        }
    }
    override fun cancel() {
        if (state.value.busy) return
        state.value.token?.let { token ->
            engine.file(token, "export").delete(); engine.file(token, "import").delete()
        }
        mutableState.value = BackupState()
    }
}
