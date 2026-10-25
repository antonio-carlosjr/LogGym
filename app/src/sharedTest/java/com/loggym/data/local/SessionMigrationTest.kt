package com.loggym.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loggym.data.repository.SessionRepositoryImpl
import com.loggym.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock

@RunWith(AndroidJUnit4::class)
class SessionMigrationTest {
    @Test fun migrationPreservesV1DraftHistoryAndForeignKeys() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "sprint2-migration-test.db"
        context.deleteDatabase(name)
        val schemaPath = "com.loggym.data.local.LogGymDatabase/1.json"
        val stream = try { androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.assets.open(schemaPath) } catch (_: java.io.IOException) {
            requireNotNull(javaClass.classLoader!!.getResourceAsStream(schemaPath))
        }
        val schema = stream.bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(
                object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val entities = schema.getJSONArray("entities")
                        for (i in 0 until entities.length()) {
                            val entity = entities.getJSONObject(i)
                            val table = entity.getString("tableName")
                            db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                            val indices = entity.getJSONArray("indices")
                            for (j in 0 until indices.length()) db.execSQL(indices.getJSONObject(j)
                                .getString("createSql").replace("\${TABLE_NAME}", table))
                        }
                        val setup = schema.getJSONArray("setupQueries")
                        for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                },
            ).build(),
        )
        try {
            helper.writableDatabase.apply {
                execSQL("INSERT INTO exercises (id,name,muscle_groups,is_custom) VALUES (1,'Halter','',1)")
                execSQL("INSERT INTO plans (id,name,is_active,created_at) VALUES (1,'Plano',1,1000)")
                execSQL("INSERT INTO workouts (id,plan_id,name,position) VALUES (1,1,'A',0)")
                execSQL("INSERT INTO workout_exercises (id,workout_id,exercise_id,position,target_sets,rep_min,rep_max,rest_seconds) VALUES (1,1,1,0,3,8,12,90)")
                execSQL("INSERT INTO sessions (id,status,source_plan_id,plan_name,source_workout_id,workout_name,started_at,finished_at,rest_started_at,rest_ends_at) VALUES (1,'DRAFT',1,'Plano',1,'A',1000,NULL,1000,91000), (2,'COMPLETED',1,'Plano',1,'A',1000,2000,NULL,NULL)")
                execSQL("INSERT INTO session_exercises (id,session_id,exercise_id,exercise_name,muscle_groups,position,target_sets,rep_min,rep_max,rest_seconds) VALUES (1,1,1,'Halter','',0,3,8,12,90), (2,2,1,'Halter','',0,3,8,12,90)")
                execSQL("INSERT INTO session_sets (id,session_exercise_id,position,weight_kg,reps,is_completed) VALUES (1,1,0,22.5,8,1), (2,2,0,25,10,1)")
            }
        } finally { helper.close() }
        val db = Room.databaseBuilder(context, LogGymDatabase::class.java, name).addMigrations(MIGRATION_1_2).build()
        try {
            val repository = SessionRepositoryImpl(db, db.sessionDao(), db.planDao(), Clock.systemUTC(), com.loggym.FakeUserPreferences())
            val draft = repository.observeDraft().first()!!
            assertEquals(1L, draft.id)
            assertEquals(22.5, draft.exercises.single().sets.single().weightKg!!, 0.0)
            assertEquals(TrackingType.WEIGHT_REPS, draft.exercises.single().trackingType)
            assertEquals(91000L, draft.restTimer!!.endsAt.toEpochMilli())
            val history = repository.observeSession(2).first()!!
            assertEquals(SessionStatus.COMPLETED, history.status)
            assertEquals(25.0, history.exercises.single().sets.single().weightKg!!, 0.0)
            assertEquals(2000L, history.finishedAt!!.toEpochMilli())
            assertEquals(3, db.planDao().getWorkoutWithExercises(1)!!.exercises.single().workoutExercise.targetSets)
            db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
