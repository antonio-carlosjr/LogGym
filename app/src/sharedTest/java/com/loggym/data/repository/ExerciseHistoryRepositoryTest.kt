package com.loggym.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loggym.data.local.*
import com.loggym.data.local.entity.*
import com.loggym.domain.model.*
import com.loggym.domain.repository.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.*

@RunWith(AndroidJUnit4::class)
class ExerciseHistoryRepositoryTest {
    private lateinit var db: LogGymDatabase
    private lateinit var repository: SessionRepositoryImpl
    private val date = Instant.parse("2026-01-01T12:00:00Z")
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), LogGymDatabase::class.java).build()
        repository = SessionRepositoryImpl(db, db.sessionDao(), db.planDao(), Clock.systemUTC(), com.loggym.FakeUserPreferences())
    }
    @After fun close() { db.close() }
    private suspend fun record(exerciseId: Long, type: TrackingType = TrackingType.WEIGHT_REPS,
        status: SessionStatus = SessionStatus.COMPLETED, occurrences: Int = 1, at: Instant = date): Long {
        val dao = db.sessionDao()
        val session = dao.insertSession(SessionEntity(status = status, sourcePlanId = null, planName = "Plano antigo",
            sourceWorkoutId = null, workoutName = "Divisão antiga", startedAt = at,
            finishedAt = if (status == SessionStatus.COMPLETED) at.plusSeconds(90) else null))
        repeat(occurrences) { index ->
            val ex = dao.insertSessionExercise(SessionExerciseEntity(sessionId = session, exerciseId = exerciseId,
                exerciseName = "Mesmo nome", muscleGroups = emptySet(), position = index * 3,
                targetSets = 2, repMin = if (type == TrackingType.TIME) null else 0,
                repMax = if (type == TrackingType.TIME) null else 10, trackingType = type,
                targetDurationSeconds = if (type == TrackingType.TIME) 45 else null, restSeconds = 37))
            dao.insertSet(SessionSetEntity(sessionExerciseId = ex, position = 0, weightKg = if (type == TrackingType.TIME) null else 0.0,
                reps = if (type == TrackingType.TIME) null else 0, durationSeconds = if (type == TrackingType.TIME) 45 else null, isCompleted = true))
            dao.insertSet(SessionSetEntity(sessionExerciseId = ex, position = 4, weightKg = null, reps = null, isCompleted = true))
        }
        return session
    }
    @Test fun paginationKeepsAllOccurrencesOfEachSessionTogetherAndUsesIdsToBreakDateTies() = runBlocking {
        val ids = (1..31).map { record(42, occurrences = 2) }
        val first = repository.getExerciseHistoryPage(42, TrackingType.WEIGHT_REPS, null)
        assertEquals(ids.reversed().take(25), first.entries.map { it.session.id })
        assertTrue(first.entries.all { it.occurrences.size == 2 })
        val second = repository.getExerciseHistoryPage(42, TrackingType.WEIGHT_REPS, first.nextCursor)
        assertEquals(ids.reversed().drop(25), second.entries.map { it.session.id }); assertNull(second.nextCursor)
        assertEquals(31, (first.entries + second.entries).map { it.session.id }.distinct().size)
    }
    @Test fun onlyOriginalIdentityAndCompatibleTypeInCompletedSessionsAreIncluded() = runBlocking {
        val weight = record(42)
        record(43); record(42, TrackingType.TIME); record(42, status = SessionStatus.DRAFT)
        val page = repository.getExerciseHistoryPage(42, TrackingType.WEIGHT_REPS, null)
        assertEquals(listOf(weight), page.entries.map { it.session.id })
        val occurrence = page.entries.single().occurrences.single()
        assertEquals(0.0, occurrence.sets[0].weightKg!!, 0.0); assertEquals(0, occurrence.sets[0].reps)
        assertNull(occurrence.sets[1].weightKg); assertNull(occurrence.sets[1].reps)
        val timed = repository.getExerciseHistoryPage(42, TrackingType.TIME, null).entries.single().occurrences.single()
        assertEquals(45, timed.sets[0].durationSeconds); assertNull(timed.sets[1].durationSeconds)
    }
    @Test fun renamedAndDeletedLibraryEntryDoesNotChangeHistoricalSnapshots() = runBlocking {
        val exercises = ExerciseRepositoryImpl(db, db.exerciseDao())
        val id = (exercises.createCustom(ExerciseDraft("Nome atual")) as SaveExerciseResult.Saved).exerciseId
        record(id)
        val before = repository.getExerciseHistoryPage(id, TrackingType.WEIGHT_REPS, null)
        exercises.updateCustom(id, ExerciseDraft("Renomeado")); exercises.deleteCustom(id)
        assertEquals(before, repository.getExerciseHistoryPage(id, TrackingType.WEIGHT_REPS, null))
        assertEquals("Mesmo nome", before.entries.single().occurrences.single().name)
        assertEquals(37L, before.entries.single().occurrences.single().target.rest.seconds)
    }
    @Test fun orderingUsesDateBeforeIdAndMissingIdentityNeverFallsBackToNames() = runBlocking {
        val newer = record(42, at = date.plusSeconds(1)); record(42)
        assertEquals(newer, repository.getExerciseHistoryPage(42, TrackingType.WEIGHT_REPS, null).entries.first().session.id)
        assertTrue(repository.getExerciseHistoryPage(44, TrackingType.WEIGHT_REPS, null).entries.isEmpty())
    }
}
