package com.loggym.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loggym.data.local.*
import com.loggym.domain.logic.SetValues
import com.loggym.domain.model.*
import com.loggym.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.*

@RunWith(AndroidJUnit4::class)
class SessionRepositoryTest {
    private lateinit var db: LogGymDatabase
    private lateinit var sessions: SessionRepositoryImpl
    private lateinit var plans: PlanRepositoryImpl
    private lateinit var exercises: ExerciseRepositoryImpl
    private val clock = MutableClock()
    private var workoutId = 0L
    private var planId = 0L

    @Before fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LogGymDatabase::class.java).build()
        repositories()
        planId = plans.createPlan("Plano")
        plans.setActivePlan(planId)
        workoutId = plans.addWorkout(planId, "Treino A")
        val weight = exercises.createCustom(ExerciseDraft("Halter")) as SaveExerciseResult.Saved
        val time = exercises.createCustom(ExerciseDraft("Prancha", trackingType = TrackingType.TIME)) as SaveExerciseResult.Saved
        plans.addExerciseToWorkout(workoutId, weight.exerciseId, ExerciseTarget.Repetitions(2, 8, 12, Duration.ofSeconds(30)))
        plans.addExerciseToWorkout(workoutId, time.exerciseId, ExerciseTarget.Timed(2, 30, Duration.ofSeconds(60)))
        Unit
    }
    private fun repositories() {
        plans = PlanRepositoryImpl(db, db.planDao(), db.exerciseDao(), clock)
        exercises = ExerciseRepositoryImpl(db, db.exerciseDao())
        sessions = SessionRepositoryImpl(db, db.sessionDao(), db.planDao(), clock)
    }
    @After fun close() { db.close() }
    private suspend fun start(): Session {
        assertTrue(sessions.startSession(workoutId) is StartSessionResult.Started)
        return sessions.observeDraft().first()!!
    }
    private suspend fun draft() = sessions.observeDraft().first()!!

    @Test fun mixedSessionFinishesAndPrefillsOnlyCompletedValues() = runBlocking {
        val before = start()
        val weight = before.exercises[0].sets.first()
        val timed = before.exercises[1].sets.first()
        sessions.saveSetAndSetCompleted(weight.id, SetValues(22.5, 0), true)
        sessions.saveSetAndSetCompleted(timed.id, SetValues(durationSeconds = 45), true)
        assertEquals(clock.instant().plusSeconds(60), draft().restTimer!!.endsAt)
        assertEquals(FinishSessionResult.Finished, sessions.finishSession(before.id))
        assertNull(sessions.observeDraft().first())
        val completed = sessions.observeSession(before.id).first()!!
        assertEquals(SessionStatus.COMPLETED, completed.status)
        assertEquals(2, completed.completedSetCount)
        assertEquals(clock.instant(), completed.finishedAt)
        assertNull(completed.restTimer)
        val next = start()
        assertEquals(22.5, next.exercises[0].sets[0].weightKg!!, 0.0)
        assertEquals(0, next.exercises[0].sets[0].reps)
        assertEquals(45, next.exercises[1].sets[0].durationSeconds)
        assertNull(next.exercises[1].sets[1].durationSeconds)
        assertFalse(next.exercises[1].sets[0].isCompleted)
    }

    @Test fun incompleteSetsStayNullAndEmptySessionsCannotFinish() = runBlocking {
        val session = start()
        assertEquals(FinishSessionResult.NoCompletedSets, sessions.finishSession(session.id))
        sessions.saveSetAndSetCompleted(session.exercises[1].sets[0].id, SetValues(), true)
        assertEquals(FinishSessionResult.Finished, sessions.finishSession(session.id))
        val completed = sessions.observeSession(session.id).first()!!
        assertEquals(1, completed.exercises.size)
        assertNull(completed.exercises.single().sets.single().durationSeconds)
        assertTrue(completed.exercises.single().sets.single().isCompleted)
    }

    @Test fun snapshotAndExtraSetsDoNotChangePlanAndDiscardIsNotHistory() = runBlocking {
        val session = start()
        val exercise = session.exercises[1]
        sessions.updateSetValues(exercise.sets.last().id, SetValues(durationSeconds = 40))
        sessions.addSet(exercise.id)
        assertEquals(40, draft().exercises[1].sets.last().durationSeconds)
        assertEquals(2, plans.observePlan(planId).first()!!.workouts.single().exercises[1].target.targetSets)
        sessions.saveSetAndSetCompleted(exercise.sets[0].id, SetValues(), true)
        assertFalse(sessions.removeSet(exercise.sets[0].id))
        assertTrue(sessions.removeSet(exercise.sets[1].id))
        plans.renamePlan(planId, "Renomeado")
        plans.deletePlan(planId)
        assertEquals("Plano", draft().planName)
        assertEquals(TrackingType.TIME, draft().exercises[1].trackingType)
        assertTrue(sessions.discardSession(session.id))
        assertTrue(sessions.observeHistory().first().isEmpty())
        assertNull(sessions.observeDraft().first())
    }

    @Test fun duplicateCheckDoesNotRestartAndUncheckKeepsRest() = runBlocking {
        val session = start()
        val set = session.exercises[0].sets[0]
        sessions.saveSetAndSetCompleted(set.id, SetValues(0.0, 8), true)
        val timer = draft().restTimer
        clock.now = clock.now.plusSeconds(10)
        sessions.saveSetAndSetCompleted(set.id, SetValues(0.0, 8), true)
        assertEquals(timer, draft().restTimer)
        sessions.saveSetAndSetCompleted(set.id, SetValues(0.0, 8), false)
        assertEquals(timer, draft().restTimer)
        sessions.skipRest(session.id)
        assertNull(draft().restTimer)
        val prescribed = plans.observePlan(planId).first()!!.workouts.single().exercises.first()
        plans.updateWorkoutExercise(prescribed.id, ExerciseTarget.Repetitions(2, 8, 12, Duration.ZERO))
        sessions.discardSession(session.id)
        val next = start()
        sessions.saveSetAndSetCompleted(next.exercises[1].sets[0].id, SetValues(), true)
        sessions.saveSetAndSetCompleted(next.exercises[0].sets[0].id, SetValues(), true)
        assertNull(draft().restTimer)
    }

    @Test fun failingRestWriteRollsBackValuesAndCheck() = runBlocking {
        val session = start()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_rest BEFORE UPDATE OF rest_started_at ON sessions BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
        try {
            sessions.saveSetAndSetCompleted(session.exercises[0].sets[0].id, SetValues(20.0, 8), true)
            fail("Expected rollback")
        } catch (_: android.database.SQLException) { }
        val set = draft().exercises[0].sets[0]
        assertNull(set.weightKg)
        assertFalse(set.isCompleted)
        assertNull(draft().restTimer)
    }

    @Test fun failingFinishRollsBackDeletionsAndStatus() = runBlocking {
        val session = start()
        sessions.saveSetAndSetCompleted(session.exercises[0].sets[0].id, SetValues(), true)
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_finish BEFORE UPDATE OF status ON sessions BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
        try { sessions.finishSession(session.id); fail("Expected rollback") }
        catch (_: android.database.SQLException) { }
        assertEquals(2, draft().exercises.size)
        assertEquals(4, draft().exercises.sumOf { it.sets.size })
        assertTrue(sessions.observeHistory().first().isEmpty())
    }

    @Test fun concurrentStartsCreateOneDraft() = runBlocking {
        val results = coroutineScope {
            listOf(async(Dispatchers.Default) { sessions.startSession(workoutId) },
                async(Dispatchers.Default) { sessions.startSession(workoutId) }).awaitAll()
        }
        assertEquals(1, results.count { it is StartSessionResult.Started })
        assertEquals(1, results.count { it is StartSessionResult.DraftAlreadyExists })
    }

    @Test fun rejectsInvalidValuesAndTypeChangesAndProtectsCompletedSessions() = runBlocking {
        val session = start()
        val exercise = session.exercises[1]
        try { sessions.updateSetValues(exercise.sets[0].id, SetValues(durationSeconds = 0)); fail() }
        catch (_: IllegalArgumentException) { }
        try { sessions.updateSetValues(exercise.sets[0].id, SetValues(1.0, 1)); fail() }
        catch (_: IllegalArgumentException) { }
        try { sessions.updateSetValues(session.exercises[0].sets[0].id, SetValues(Double.NaN, 1)); fail() }
        catch (_: IllegalArgumentException) { }
        assertEquals(SaveExerciseResult.NotEditable, exercises.updateCustom(exercise.exerciseId!!, ExerciseDraft("Prancha")))
        sessions.saveSetAndSetCompleted(exercise.sets[0].id, SetValues(durationSeconds = 30), true)
        sessions.finishSession(session.id)
        try { sessions.updateSetValues(exercise.sets[0].id, SetValues(durationSeconds = 99)); fail() }
        catch (_: IllegalStateException) { }
        assertFalse(sessions.removeSet(exercise.sets[0].id))
        assertEquals(30, sessions.observeSession(session.id).first()!!.exercises.single().sets.single().durationSeconds)
    }

    @Test fun fileDatabaseSurvivesClosingAndReopening() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "sprint2-reopen-test.db"
        context.deleteDatabase(name)
        db.close()
        db = Room.databaseBuilder(context, LogGymDatabase::class.java, name).addMigrations(MIGRATION_1_2).build()
        repositories()
        try {
            planId = plans.createPlan("Persistido")
            plans.setActivePlan(planId)
            workoutId = plans.addWorkout(planId, "A")
            val id = (exercises.createCustom(ExerciseDraft("Tempo", trackingType = TrackingType.TIME)) as SaveExerciseResult.Saved).exerciseId
            plans.addExerciseToWorkout(workoutId, id, ExerciseTarget.Timed(2, 30, Duration.ofSeconds(60)))
            val session = start()
            sessions.saveSetAndSetCompleted(session.exercises[0].sets[0].id, SetValues(durationSeconds = 50), true)
            val timer = draft().restTimer
            db.close()
            db = Room.databaseBuilder(context, LogGymDatabase::class.java, name).addMigrations(MIGRATION_1_2).build()
            repositories()
            assertEquals(session.id, draft().id)
            assertEquals(50, draft().exercises[0].sets[0].durationSeconds)
            assertEquals(timer, draft().restTimer)
            assertEquals(SessionStatus.DRAFT, draft().status)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    private class MutableClock(var now: Instant = Instant.parse("2026-10-22T12:00:00Z")) : Clock() {
        override fun instant() = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(now, zone)
    }
}
