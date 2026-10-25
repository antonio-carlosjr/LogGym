package com.loggym.data.backup

import android.content.Context
import android.util.AtomicFile
import androidx.room.Room
import androidx.room.withTransaction
import com.loggym.data.local.LogGymDatabase
import com.loggym.domain.backup.*
import com.loggym.domain.model.ThemeMode
import com.loggym.domain.model.UserSettings
import com.loggym.domain.repository.PersistedPreferences
import com.loggym.domain.repository.UserPreferencesRepository
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** Apenas arquivos privados gerados localmente são usados pelo journal. Nunca caminhos do backup. */
class BackupEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: LogGymDatabase,
    private val preferences: UserPreferencesRepository,
    private val coordinator: PersistenceCoordinator,
    private val clock: Clock,
) {
    private var bootstrapped = false
    internal var checkpoint: (String) -> Unit = {}
    private val directory get() = File(context.noBackupFilesDir, "loggym-backup").apply { check(isDirectory || mkdirs()) }
    internal fun file(token: String, suffix: String): File {
        require(UUID.fromString(token).toString() == token)
        require(suffix in setOf("export", "import", "undo"))
        return File(directory, "$token.$suffix")
    }
    private val db get() = database.openHelper.writableDatabase
    internal fun hash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    private suspend fun snapshot(file: File, mode: BackupMode, undo: Boolean = false) {
        database.withTransaction {
            val settings = preferences.snapshot()
            val header = BackupHeader(magic = if (undo) "LOGGYM_UNDO" else "LOGGYM_BACKUP", mode = mode,
                createdAt = clock.millis(), catalog = BackupFormat.catalog, floors = BackupData.floors(db),
                preferences = BackupPreferences(settings.settings.themeMode.key, settings.settings.prefillEnabled),
                previousOnboarding = if (undo) settings.onboardingCompleted else null)
            val atomic = AtomicFile(file)
            val stream = atomic.startWrite()
            try {
                BackupFormat.write(stream, header, BackupData.rows(db, mode))
                stream.fd.sync()
                atomic.finishWrite(stream)
            } catch (error: Throwable) { atomic.failWrite(stream); throw error }
        }
    }
    suspend fun export(token: String, mode: BackupMode): File = coordinator.capture {
        val destination = file(token, "export")
        try {
            snapshot(destination, mode)
            // A mesma validação usada na importação precede a publicação do arquivo.
            validate(destination, token)
            destination
        } catch (error: Throwable) { destination.delete(); throw error }
    }
    suspend fun validate(source: File, token: String): BackupSummary {
        val name = "validation-${UUID.randomUUID()}.db"
        val staging = Room.databaseBuilder(context, LogGymDatabase::class.java, name).build()
        try {
            return staging.withTransaction {
                val sql = staging.openHelper.writableDatabase
                val header = source.inputStream().use { input ->
                    BackupFormat.read(input, header = {}, row = { BackupData.insert(sql, it) })
                }
                BackupData.validate(sql, header, token)
            }
        } finally { staging.close(); context.deleteDatabase(name) }
    }
    private fun BackupHeader.persisted(undo: Boolean) = PersistedPreferences(
        UserSettings(ThemeMode.fromKey(preferences.theme), preferences.prefillEnabled),
        if (undo) requireNotNull(previousOnboarding) else true)

    /** Retorna false após reversão bem-sucedida; falha de recuperação mantém a aplicação bloqueada. */
    suspend fun restore(token: String): Boolean {
        val source = file(token, "import")
        validate(source, token)
        return coordinator.restore {
            val undo = file(token, "undo")
            try {
                snapshot(undo, BackupMode.FULL, undo = true)
                val undoHash = hash(undo)
                checkpoint("undo_durable")
                val header = database.withTransaction {
                    check(journal() == null) { "Recuperação anterior pendente" }
                    val previous = BackupData.floors(db)
                    BackupData.clearBusiness(db)
                    val imported = source.inputStream().use { input ->
                        BackupFormat.read(input, header = {}, row = { BackupData.insert(db, it) })
                    }
                    BackupData.restoreFloors(db, imported.floors, previous)
                    db.execSQL("INSERT INTO restore_journal(id,operation_id,undo_sha256,phase,protocol_version) VALUES (1,?,?,?,1)",
                        arrayOf(token, undoHash, "APPLYING"))
                    imported
                }
                checkpoint("room_applied")
                preferences.replace(header.persisted(false))
                check(preferences.snapshot() == header.persisted(false))
                checkpoint("preferences_applied")
                database.withTransaction { db.execSQL("UPDATE restore_journal SET phase='COMMITTED' WHERE id=1") }
                checkpoint("committed")
                cleanup()
                true
            } catch (error: Exception) {
                // Cancelamento também deve concluir a reversão antes de liberar o estado normal.
                withContext(NonCancellable) {
                    val entry = journal()
                    if (entry?.phase == "APPLYING") rollback(entry)
                    if (entry?.phase == "COMMITTED") { cleanup(); true }
                    else { cleanup(); undo.delete(); false }
                }
            }
        }
    }
    private data class Journal(val token: String, val hash: String, val phase: String)
    private fun journal(): Journal? = db.query("SELECT * FROM restore_journal WHERE id=1").use { c ->
        if (!c.moveToFirst()) null else {
            check(c.getInt(c.getColumnIndexOrThrow("protocol_version")) == 1)
            Journal(c.getString(c.getColumnIndexOrThrow("operation_id")), c.getString(c.getColumnIndexOrThrow("undo_sha256")), c.getString(c.getColumnIndexOrThrow("phase")))
        }
    }
    private suspend fun rollback(entry: Journal) {
        val previous = file(entry.token, "undo")
        check(previous.isFile && hash(previous) == entry.hash) { "Cópia de recuperação indisponível" }
        val header = database.withTransaction {
            val floors = BackupData.floors(db)
            BackupData.clearBusiness(db)
            val old = FileInputStream(previous).use { input ->
                BackupFormat.read(input, internal = true, header = {}, row = { BackupData.insert(db, it) })
            }
            BackupData.restoreFloors(db, old.floors, floors)
            old
        }
        checkpoint("rollback_room")
        preferences.replace(header.persisted(true))
        check(preferences.snapshot() == header.persisted(true))
        checkpoint("rollback_preferences")
        database.withTransaction { db.execSQL("UPDATE restore_journal SET phase='ROLLED_BACK' WHERE id=1") }
        cleanup()
    }
    private suspend fun cleanup() {
        val entry = journal() ?: return
        check(entry.phase in setOf("COMMITTED", "ROLLED_BACK"))
        // O estado final é durável; a falta do undo após esse ponto é segura.
        if (file(entry.token, "undo").exists() && !file(entry.token, "undo").delete()) return
        file(entry.token, "import").delete()
        database.withTransaction { db.execSQL("DELETE FROM restore_journal WHERE id=1") }
    }
    suspend fun recover() = coordinator.recover {
        journal()?.let { entry -> when (entry.phase) {
            "APPLYING" -> rollback(entry)
            "COMMITTED", "ROLLED_BACK" -> cleanup()
            else -> error("Estado de recuperação desconhecido")
        } }
        if (!bootstrapped && journal() == null) {
            // Sem journal, nenhum arquivo antigo pode participar de uma recuperação.
            // A transferência externa é reiniciada após morte do processo, nunca retomada.
            directory.listFiles()?.forEach { candidate ->
                val base = candidate.name.substringBefore('.')
                if (runCatching { UUID.fromString(base).toString() == base }.getOrDefault(false)) candidate.delete()
            }
        }
        bootstrapped = true
    }
}
