package com.loggym.data.backup

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loggym.data.local.*
import com.loggym.data.preferences.DataStoreUserPreferencesRepository
import com.loggym.data.repository.*
import com.loggym.domain.backup.*
import com.loggym.domain.model.*
import com.loggym.domain.repository.*
import com.loggym.domain.logic.SetValues
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.IOException
import java.time.*
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BackupEngineTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: LogGymDatabase
    private lateinit var preferences: DataStoreUserPreferencesRepository
    private lateinit var coordinator: PersistenceCoordinator
    private lateinit var engine: BackupEngine
    private lateinit var plans: PlanRepositoryImpl
    private lateinit var sessions: SessionRepositoryImpl
    private lateinit var exercises: ExerciseRepositoryImpl
    private lateinit var store: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    private val clock = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC)
    private val name = "backup-test-${UUID.randomUUID()}.db"
    private val tokens = mutableListOf<String>()
    private var preferenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private fun token() = UUID.randomUUID().toString().also { tokens.add(it) }
    @Before fun setup() = runBlocking {
        coordinator = PersistenceCoordinator()
        db = Room.databaseBuilder(context, LogGymDatabase::class.java, name).addCallback(NativeExerciseSeedCallback()).build()
        store = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(scope = preferenceScope) {
            java.io.File(context.filesDir, "$name.preferences_pb")
        }
        preferences = DataStoreUserPreferencesRepository(store, coordinator)
        preferences.replace(PersistedPreferences(UserSettings(), true))
        engine = BackupEngine(context, db, preferences, coordinator, clock)
        plans = PlanRepositoryImpl(db, db.planDao(), db.exerciseDao(), clock, coordinator)
        sessions = SessionRepositoryImpl(db, db.sessionDao(), db.planDao(), clock, preferences, coordinator)
        exercises = ExerciseRepositoryImpl(db, db.exerciseDao(), coordinator)
    }
    private suspend fun restart() {
        db.close()
        preferenceScope.coroutineContext[Job]!!.cancelAndJoin()
        preferenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        store = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(scope = preferenceScope) {
            java.io.File(context.filesDir, "$name.preferences_pb")
        }
        coordinator = PersistenceCoordinator()
        db = Room.databaseBuilder(context, LogGymDatabase::class.java, name).build()
        preferences = DataStoreUserPreferencesRepository(store, coordinator)
        engine = BackupEngine(context, db, preferences, coordinator, clock)
        engine.recover()
        plans = PlanRepositoryImpl(db, db.planDao(), db.exerciseDao(), clock, coordinator)
        sessions = SessionRepositoryImpl(db, db.sessionDao(), db.planDao(), clock, preferences, coordinator)
        exercises = ExerciseRepositoryImpl(db, db.exerciseDao(), coordinator)
    }
    @After fun close() {
        db.close(); context.deleteDatabase(name)
        preferenceScope.cancel(); java.io.File(context.filesDir, "$name.preferences_pb").delete()
        tokens.forEach { t -> listOf("import", "export", "undo").forEach { engine.file(t, it).delete() } }
    }
    private suspend fun fixture(): Long {
        val plan = plans.createPlan("Plano original"); plans.setActivePlan(plan)
        val workout = plans.addWorkout(plan, "Misto")
        val weight = (exercises.createCustom(ExerciseDraft("Halter teste")) as SaveExerciseResult.Saved).exerciseId
        val time = (exercises.createCustom(ExerciseDraft("Prancha teste", trackingType = TrackingType.TIME)) as SaveExerciseResult.Saved).exerciseId
        plans.addExerciseToWorkout(workout, weight, ExerciseTarget.Repetitions(2, 0, 10, Duration.ofSeconds(37)))
        plans.addExerciseToWorkout(workout, time, ExerciseTarget.Timed(2, 45, Duration.ofSeconds(60)))
        sessions.startSession(workout)
        val draft = sessions.observeDraft().first()!!
        sessions.saveSetAndSetCompleted(draft.exercises[0].sets[0].id, SetValues(0.0, 0), true)
        sessions.saveSetAndSetCompleted(draft.exercises[1].sets[0].id, SetValues(durationSeconds = 45), true)
        sessions.finishSession(draft.id)
        sessions.startSession(workout)
        val next = sessions.observeDraft().first()!!
        sessions.addSet(next.exercises[0].id)
        sessions.saveSetAndSetCompleted(next.exercises[0].sets[0].id, SetValues(22.5, null), true)
        preferences.setThemeMode(ThemeMode.DARK); preferences.setPrefillEnabled(false)
        return plan
    }
    private suspend fun exported(mode: BackupMode): String {
        val t = token(); engine.export(t, mode).copyTo(engine.file(t, "import"), overwrite = true); return t
    }
    private suspend fun summary(t: String) = engine.validate(engine.file(t, "import"), t)
    @Test fun fullReplacesOccupiedInstallationAndPreservesMixedDraftHistoryPreferencesAndExtras() = runBlocking {
        val original = fixture()
        val before = sessions.observeDraft().first()!!
        val t = exported(BackupMode.FULL)
        assertTrue(summary(t).hasDraft); assertEquals(1L, summary(t).sessions)
        plans.renamePlan(original, "Alterado"); preferences.setThemeMode(ThemeMode.LIGHT)
        assertTrue(engine.restore(t))
        assertEquals(before, sessions.observeDraft().first())
        assertEquals(ThemeMode.DARK, preferences.snapshot().settings.themeMode)
        assertFalse(preferences.snapshot().settings.prefillEnabled)
        assertEquals(1, sessions.observeHistory().first().size)
        assertTrue(db.planDao().observePlans().first().any { it.name == "Plano original" })
    }
    @Test fun withoutHistoryRemovesDestinationDraftAndHistoryButKeepsPlansAndPreferences() = runBlocking {
        fixture(); val t = exported(BackupMode.WITHOUT_HISTORY)
        assertEquals(0L, summary(t).sessions); assertFalse(summary(t).hasDraft); assertEquals(0L, summary(t).sets)
        assertTrue(engine.restore(t)); assertNull(sessions.observeDraft().first()); assertTrue(sessions.observeHistory().first().isEmpty())
        assertEquals(ThemeMode.DARK, preferences.snapshot().settings.themeMode)
    }
    @Test fun bothModalitiesRestoreIntoCleanBusinessTables() = runBlocking {
        fixture()
        for (mode in BackupMode.entries) {
            val t = exported(mode)
            db.withTransaction { BackupData.clearBusiness(db.openHelper.writableDatabase) }
            assertTrue(engine.restore(t))
            assertEquals(mode == BackupMode.FULL, sessions.observeDraft().first() != null)
        }
    }
    @Test fun invalidFileNeverChangesExistingData() = runBlocking {
        fixture(); val before = sessions.observeDraft().first(); val settings = preferences.snapshot()
        val t = exported(BackupMode.FULL); engine.file(t, "import").appendText("{}\n")
        assertTrue(runCatching { engine.restore(t) }.isFailure)
        assertEquals(before, sessions.observeDraft().first()); assertEquals(settings, preferences.snapshot())
    }
    @Test fun handledFailureAfterRoomAndAfterPreferencesRevertsBothStores() = runBlocking {
        fixture()
        for (point in listOf("undo_durable", "room_applied", "preferences_applied")) {
            val t = exported(BackupMode.FULL)
            preferences.setThemeMode(ThemeMode.LIGHT)
            val before = sessions.observeDraft().first(); val settings = preferences.snapshot()
            engine.checkpoint = { if (it == point) throw IOException("simulada") }
            assertFalse(engine.restore(t)); assertEquals(before, sessions.observeDraft().first()); assertEquals(settings, preferences.snapshot())
            assertEquals(PersistencePhase.READY, coordinator.phase.value)
            engine.checkpoint = {}
        }
    }
    private class ProcessDeath : Error()
    @Test fun processDeathAtEveryApplyBoundaryRecoversFromDurableJournal() = runBlocking {
        fixture()
        for (point in listOf("undo_durable", "room_applied", "preferences_applied", "committed")) {
            val t = exported(BackupMode.FULL)
            preferences.setThemeMode(ThemeMode.LIGHT)
            val before = preferences.snapshot()
            engine.checkpoint = { if (it == point) throw ProcessDeath() }
            assertTrue(runCatching { engine.restore(t) }.exceptionOrNull() is ProcessDeath)
            assertEquals(PersistencePhase.BLOCKED, coordinator.phase.value)
            restart()
            assertEquals(if (point == "committed") ThemeMode.DARK else before.settings.themeMode, preferences.snapshot().settings.themeMode)
            assertEquals(PersistencePhase.READY, coordinator.phase.value)
            engine.checkpoint = {}; preferences.setThemeMode(ThemeMode.DARK)
        }
    }
    @Test fun interruptedRollbackRepeatsAndCorruptUndoBlocksUntilRepaired() = runBlocking {
        fixture(); val t = exported(BackupMode.FULL)
        preferences.setThemeMode(ThemeMode.LIGHT)
        engine.checkpoint = { if (it == "preferences_applied") throw IOException(); if (it == "rollback_room") throw ProcessDeath() }
        assertTrue(runCatching { engine.restore(t) }.isFailure)
        val undo = engine.file(t, "undo"); val bytes = undo.readBytes(); undo.appendText("corrupt")
        val reopened = BackupEngine(context, db, preferences, coordinator, clock)
        assertTrue(runCatching { reopened.recover() }.isFailure); assertEquals(PersistencePhase.BLOCKED, coordinator.phase.value)
        undo.writeBytes(bytes); reopened.recover(); assertEquals(ThemeMode.LIGHT, preferences.snapshot().settings.themeMode)
    }
    @Test fun dataStoreFailureRevertsRoomAndInterruptedRollbackAfterPreferencesCanRetry() = runBlocking {
        fixture(); val t = exported(BackupMode.FULL)
        preferences.setThemeMode(ThemeMode.LIGHT)
        val before = sessions.observeDraft().first()
        val failing = object : UserPreferencesRepository by preferences {
            override suspend fun replace(snapshot: PersistedPreferences) {
                if (snapshot.settings.themeMode == ThemeMode.DARK) throw IOException("DataStore indisponível")
                preferences.replace(snapshot)
            }
        }
        val attempt = BackupEngine(context, db, failing, coordinator, clock)
        attempt.checkpoint = { if (it == "rollback_preferences") throw ProcessDeath() }
        assertTrue(runCatching { attempt.restore(t) }.isFailure)
        assertEquals(PersistencePhase.BLOCKED, coordinator.phase.value)
        restart()
        assertEquals(ThemeMode.LIGHT, preferences.snapshot().settings.themeMode)
        assertEquals(before, sessions.observeDraft().first())
    }
    @Test fun deletedIdentitiesWeakHistoryReferencesAndHigherDestinationAllocatorsSurvive() = runBlocking {
        val plan = fixture()
        val draft = sessions.observeDraft().first()!!
        val oldExercise = draft.exercises[0].exerciseId!!
        plans.deletePlan(plan); exercises.deleteCustom(oldExercise)
        val t = exported(BackupMode.FULL)
        db.withTransaction { db.openHelper.writableDatabase.execSQL("UPDATE sqlite_sequence SET seq=10000 WHERE name='exercises'") }
        assertTrue(engine.restore(t))
        assertEquals(draft, sessions.observeDraft().first())
        val next = exercises.createCustom(ExerciseDraft("Nova identidade")) as SaveExerciseResult.Saved
        assertTrue(next.exerciseId > 10000L)
        assertEquals(oldExercise, sessions.observeDraft().first()!!.exercises[0].exerciseId)
    }
    @Test fun checksumValidButSemanticallyInvalidFilesAreRejectedWithoutLiveMutation() = runBlocking {
        fixture(); val before = sessions.observeDraft().first()
        val t = exported(BackupMode.FULL)
        val rows = mutableListOf<BackupRow>()
        val header = engine.file(t, "import").inputStream().use { BackupFormat.read(it, header = {}, row = rows::add) }
        val changed = rows.map { row -> if (row.table != "session_sets") row else row.copy(values = kotlinx.serialization.json.JsonObject(row.values + ("reps" to kotlinx.serialization.json.JsonPrimitive(-1)))) }
        engine.file(t, "import").outputStream().use { BackupFormat.write(it, header, changed.asSequence()) }
        assertTrue(runCatching { summary(t) }.isFailure)
        assertEquals(before, sessions.observeDraft().first())
    }
    @Test fun previousJournalCannotBeReportedAsSuccessfulNewImport() = runBlocking {
        fixture(); val t = exported(BackupMode.FULL)
        preferences.setThemeMode(ThemeMode.LIGHT)
        val old = token()
        db.withTransaction {
            db.openHelper.writableDatabase.execSQL("INSERT INTO restore_journal(id,operation_id,undo_sha256,phase,protocol_version) VALUES (1,?,'old','COMMITTED',1)", arrayOf(old))
        }
        assertTrue(runCatching { engine.restore(t) }.isFailure)
        assertEquals(ThemeMode.LIGHT, preferences.snapshot().settings.themeMode)
        engine.recover()
        assertEquals(PersistencePhase.READY, coordinator.phase.value)
    }
}
