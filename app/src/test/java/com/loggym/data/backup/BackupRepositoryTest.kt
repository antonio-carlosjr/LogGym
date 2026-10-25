package com.loggym.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loggym.MainDispatcherRule
import com.loggym.awaitFirst
import com.loggym.data.local.*
import com.loggym.FakeUserPreferences
import com.loggym.domain.backup.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.time.Clock

@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {
    @get:Rule val main = MainDispatcherRule()
    private lateinit var db: LogGymDatabase
    private lateinit var repository: BackupRepositoryImpl
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, LogGymDatabase::class.java).addCallback(NativeExerciseSeedCallback()).build()
        repository = BackupRepositoryImpl(context, BackupEngine(context, db, FakeUserPreferences(), PersistenceCoordinator(), Clock.systemUTC()))
    }
    @After fun close() { db.close() }
    @Test fun cancellingDestinationReleasesOperationAndDuplicateResultCannotWrite() = runBlocking {
        repository.prepareExport(BackupMode.WITHOUT_HISTORY)
        val ready = repository.state.awaitFirst { it.step == BackupStep.DESTINATION || it.step == BackupStep.ERROR }
        assertEquals(BackupStep.DESTINATION, ready.step)
        val token = ready.token!!
        repository.writeExport(token, null)
        val file = File(context.filesDir, "not-created.loggym")
        repository.writeExport(token, android.net.Uri.fromFile(file).toString())
        assertEquals(BackupStep.IDLE, repository.state.value.step); assertFalse(file.exists())
    }
    @Test fun invalidImportReportsFailureWithoutPublishingPreview() = runBlocking {
        val source = File(context.filesDir, "invalid.loggym").apply { writeText("invalid") }
        try {
            repository.validateImport(android.net.Uri.fromFile(source).toString())
            val result = repository.state.awaitFirst { it.step == BackupStep.ERROR }
            assertEquals(BackupFailure.OPERATION, result.error); assertNull(result.summary)
            assertTrue(source.exists())
        } finally { source.delete() }
    }
    @Test fun publicV1ReportsTypedIncompatibilityAndKeepsTheSourceAndLocalData() = runBlocking {
        val source = File(context.filesDir, "old-version.loggym")
        try {
            repository.prepareExport(BackupMode.FULL)
            val ready = repository.state.awaitFirst { it.step == BackupStep.DESTINATION || it.step == BackupStep.ERROR }
            assertEquals(BackupStep.DESTINATION, ready.step)
            repository.writeExport(ready.token!!, android.net.Uri.fromFile(source).toString())
            repository.state.awaitFirst { it.step == BackupStep.SUCCESS }
            source.writeBytes(rewriteBackupHeader(source.readBytes(), ::s5Header))
            val before = db.exerciseDao().getAll()
            repository.validateImport(android.net.Uri.fromFile(source).toString())
            assertEquals(BackupFailure.INCOMPATIBLE_VERSION, repository.state.awaitFirst { it.step == BackupStep.ERROR }.error)
            assertEquals(before, db.exerciseDao().getAll()); assertTrue(source.exists())
        } finally { source.delete() }
    }
}
