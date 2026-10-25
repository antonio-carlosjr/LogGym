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
    private val preferences = com.loggym.FakeUserPreferences()

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
        sessions = SessionRepositoryImpl(db, db.sessionDao(), db.planDao(), clock, preferences)
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
        preferences.setPrefillEnabled(false)
        assertEquals(next, draft())
        sessions.discardSession(next.id)
        val empty = start()
        assertTrue(empty.exercises.flatMap { it.sets }.all { it.weightKg == null && it.reps == null && it.durationSeconds == null && !it.isCompleted })
        preferences.setPrefillEnabled(true)
        assertEquals(empty, draft())
        sessions.discardSession(empty.id)
        assertEquals(45, start().exercises[1].sets[0].durationSeconds)
    }

    @Test fun preferenceReadFailureDoesNotCreatePartialDraft() = runBlocking {
        val broken = object : UserPreferencesRepository by preferences {
            override val settings = kotlinx.coroutines.flow.flow<com.loggym.domain.model.UserSettings> { throw java.io.IOException() }
        }
        val repository = SessionRepositoryImpl(db, db.sessionDao(), db.planDao(), clock, broken)
        assertTrue(runCatching { repository.startSession(workoutId) }.isFailure)
        assertNull(sessions.observeDraft().first())
        val existing = start()
        assertEquals(StartSessionResult.DraftAlreadyExists(existing.id), repository.startSession(workoutId))
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

    @Test fun completedMixedSnapshotSurvivesPrescriptionEditsAndSourceDeletion() = runBlocking {
        val draft = start()
        sessions.saveSetAndSetCompleted(draft.exercises[0].sets[0].id, SetValues(0.0, 0), true)
        sessions.saveSetAndSetCompleted(draft.exercises[0].sets[1].id, SetValues(), true)
        sessions.saveSetAndSetCompleted(draft.exercises[1].sets[0].id, SetValues(durationSeconds = 45), true)
        clock.now = clock.now.plusSeconds(125)
        sessions.finishSession(draft.id)
        val snapshot = sessions.observeSession(draft.id).first()!!
        val plan = plans.observePlan(planId).first()!!
        plans.renamePlan(planId, "Plano novo")
        plans.renameWorkout(workoutId, "Divisão nova")
        for (item in plan.workouts.single().exercises) {
            val target = if (item.exercise.trackingType == TrackingType.TIME)
                ExerciseTarget.Timed(4, 90, Duration.ZERO)
            else ExerciseTarget.Repetitions(4, 10, 20, Duration.ZERO)
            plans.updateWorkoutExercise(item.id, target)
            exercises.updateCustom(item.exercise.id, ExerciseDraft("Novo ${item.exercise.id}", trackingType = item.exercise.trackingType))
        }
        assertEquals(snapshot, sessions.observeSession(draft.id).first())
        plan.workouts.single().exercises.forEach { exercises.deleteCustom(it.exercise.id) }
        plans.deletePlan(planId)
        assertEquals(snapshot, sessions.observeSession(draft.id).first())
        assertEquals("Halter", snapshot.exercises[0].name)
        assertEquals(0.0, snapshot.exercises[0].sets[0].weightKg!!, 0.0)
        assertNull(snapshot.exercises[0].sets[1].weightKg)
        assertEquals(45, snapshot.exercises[1].sets.single().durationSeconds)
        assertEquals(30, snapshot.exercises[1].target.durationSeconds)
    }

    @Test fun historyExcludesDraftsAndDiscardsAndOrdersCompletedSessionsNewestFirst() = runBlocking {
        val first = start()
        sessions.saveSetAndSetCompleted(first.exercises[0].sets[0].id, SetValues(1.0, 1), true)
        sessions.finishSession(first.id)
        clock.now = clock.now.plusSeconds(60)
        val discarded = start()
        assertEquals(listOf(first.id), sessions.observeHistory().first().map { it.id })
        sessions.discardSession(discarded.id)
        assertEquals(listOf(first.id), sessions.observeHistory().first().map { it.id })
        val last = start()
        sessions.saveSetAndSetCompleted(last.exercises[1].sets[0].id, SetValues(durationSeconds = 45), true)
        sessions.finishSession(last.id)
        assertEquals(listOf(last.id, first.id), sessions.observeHistory().first().map { it.id })
    }

    @Test fun clearAffectsOnlySelectedOccurrenceAndPreservesRestPlanAndHistory() = runBlocking {
        val before = start()
        val weight = before.exercises[0]
        sessions.saveSetAndSetCompleted(weight.sets[0].id, SetValues(0.0, 0), true)
        sessions.addSet(weight.id)
        sessions.saveSetAndSetCompleted(before.exercises[1].sets[0].id, SetValues(durationSeconds = 45), true)
        val populated = draft()
        val plan = plans.observePlan(planId).first()
        assertTrue(sessions.clearExerciseSets(weight.id))
        assertTrue(sessions.clearExerciseSets(weight.id))
        val cleared = draft()
        assertEquals(populated.restTimer, cleared.restTimer)
        assertEquals(populated.exercises[0].sets.map { it.id }, cleared.exercises[0].sets.map { it.id })
        assertEquals(populated.exercises[1], cleared.exercises[1])
        assertTrue(cleared.exercises[0].sets.all { it.weightKg == null && it.reps == null && !it.isCompleted })
        assertEquals(plan, plans.observePlan(planId).first())
        sessions.finishSession(before.id)
        val completed = sessions.observeSession(before.id).first()
        assertFalse(sessions.clearExerciseSets(before.exercises[1].id))
        assertFalse(sessions.clearExerciseSets(Long.MAX_VALUE))
        assertEquals(completed, sessions.observeSession(before.id).first())
    }

    @Test fun failedClearRollsBackAllSets() = runBlocking {
        val before = start()
        val exercise = before.exercises[0]
        exercise.sets.forEach { sessions.updateSetValues(it.id, SetValues(12.5, 0)) }
        val snapshot = draft()
        db.openHelper.writableDatabase.execSQL("""CREATE TRIGGER fail_clear BEFORE UPDATE ON session_sets
            WHEN OLD.session_exercise_id = ${exercise.id} AND OLD.position = 1
            BEGIN SELECT RAISE(ABORT, 'clear failure'); END""")
        assertTrue(runCatching { sessions.clearExerciseSets(exercise.id) }.isFailure)
        assertEquals(snapshot, draft())
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_clear")
        assertTrue(sessions.clearExerciseSets(exercise.id))
    }

    @Test fun newPrescriptionsDoNotModifyAlreadyStartedSnapshots() = runBlocking {
        val snapshot = start()
        val prescription = plans.observePlan(planId).first()!!.workouts.single().exercises[0]
        plans.updateWorkoutExercise(prescription.id, ExerciseTarget.Repetitions(3, 0, 10, Duration.ofSeconds(37)))
        assertEquals(snapshot, draft())
        sessions.discardSession(snapshot.id)
        val next = start()
        assertEquals(0, next.exercises[0].target.repMin)
        assertEquals(37L, next.exercises[0].target.rest.seconds)
    }

    private class MutableClock(var now: Instant = Instant.parse("2026-10-22T12:00:00Z")) : Clock() {
        override fun instant() = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(now, zone)
    }
}
