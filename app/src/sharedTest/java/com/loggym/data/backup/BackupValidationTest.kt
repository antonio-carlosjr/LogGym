package com.loggym.data.backup

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loggym.data.local.*
import com.loggym.domain.backup.BackupMode
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupValidationTest {
    private lateinit var database: LogGymDatabase
    private val db get() = database.openHelper.writableDatabase
    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), LogGymDatabase::class.java)
            .addCallback(NativeExerciseSeedCallback()).build()
    }
    @After fun close() { database.close() }
    private fun header() = BackupHeader(mode = BackupMode.FULL, createdAt = 1000, catalog = BackupFormat.catalog,
        floors = BackupData.floors(db), preferences = BackupPreferences("system", true))
    @Test fun nativeCatalogAndEmptyPlansAreValid() = runBlocking {
        database.withTransaction { assertEquals(0L, BackupData.validate(db, header(), "test").plans) }
    }
    @Test fun rejectsUnknownOrAlteredNativeCatalog() = runBlocking {
        database.withTransaction {
            db.execSQL("UPDATE exercises SET name='Adulterado' WHERE id=1")
            assertTrue(runCatching { BackupData.validate(db, header(), "test") }.isFailure)
        }
    }
    @Test fun rejectsMultipleActivePlansAndInsufficientAllocator() = runBlocking {
        database.withTransaction {
            db.execSQL("INSERT INTO plans(id,name,is_active,created_at) VALUES (1,'A',1,1000),(2,'B',1,1000)")
            assertTrue(runCatching { BackupData.validate(db, header(), "test") }.isFailure)
            db.execSQL("UPDATE plans SET is_active=0 WHERE id=2")
            val bad = header().copy(floors = header().floors + ("plans" to 0L))
            assertTrue(runCatching { BackupData.validate(db, bad, "test") }.isFailure)
        }
    }
    @Test fun rejectsDuplicatePositionsButPermitsGaps() = runBlocking {
        database.withTransaction {
            db.execSQL("INSERT INTO plans(id,name,is_active,created_at) VALUES (1,'A',1,1000)")
            db.execSQL("INSERT INTO workouts(id,plan_id,name,position) VALUES (1,1,'A',3),(2,1,'B',9)")
            assertEquals(2L, BackupData.validate(db, header(), "test").workouts)
            db.execSQL("DROP TABLE validation_names")
            db.execSQL("UPDATE workouts SET position=3 WHERE id=2")
            assertTrue(runCatching { BackupData.validate(db, header(), "test") }.isFailure)
        }
    }
    @Test fun rejectsMissingColumnsWrongTypesAndUntrustedTableNames() = runBlocking {
        database.withTransaction {
            val source = BackupData.rows(db, BackupMode.FULL).first()
            assertTrue(runCatching { BackupData.insert(db, source.copy(values = kotlinx.serialization.json.JsonObject(source.values - "name"))) }.isFailure)
            assertTrue(runCatching { BackupData.insert(db, source.copy(table = "exercises; DROP TABLE plans")) }.isFailure)
            assertTrue(runCatching { BackupData.insert(db, source.copy(values = kotlinx.serialization.json.JsonObject(source.values + ("id" to kotlinx.serialization.json.JsonPrimitive("500"))))) }.isFailure)
        }
    }
}
