package com.loggym.data.repository

import androidx.room.withTransaction
import com.loggym.data.local.LogGymDatabase
import com.loggym.data.local.dao.PlanDao
import com.loggym.data.local.dao.SessionDao
import com.loggym.data.local.entity.PlanEntity
import com.loggym.data.local.entity.SessionEntity
import com.loggym.data.local.entity.SessionExerciseEntity
import com.loggym.data.local.entity.SessionSetEntity
import com.loggym.data.local.relation.WorkoutWithExercises
import com.loggym.data.mapper.toDomain
import com.loggym.data.mapper.toSummary
import com.loggym.domain.logic.RestTimer
import com.loggym.domain.logic.SetPrefill
import com.loggym.domain.logic.SetValues
import com.loggym.domain.model.FinishSessionResult
import com.loggym.domain.model.Session
import com.loggym.domain.model.SessionStatus
import com.loggym.domain.model.SessionSummary
import com.loggym.domain.model.StartSessionResult
import com.loggym.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val database: LogGymDatabase,
    private val sessionDao: SessionDao,
    private val planDao: PlanDao,
    private val clock: Clock,
) : SessionRepository {

    override fun observeDraft(): Flow<Session?> = sessionDao.observeDraft().map { it?.toDomain() }

    override fun observeHistory(): Flow<List<SessionSummary>> =
        sessionDao.observeCompletedSessions().map { sessions -> sessions.map { it.toSummary() } }

    override fun observeSession(sessionId: Long): Flow<Session?> =
        sessionDao.observeSession(sessionId).map { it?.toDomain() }

    override suspend fun startSession(workoutId: Long): StartSessionResult = database.withTransaction {
        val draftId = sessionDao.getDraftId()
        val activePlan = planDao.getActivePlan()
        val workout = planDao.getWorkoutWithExercises(workoutId)
        when {
            draftId != null -> StartSessionResult.DraftAlreadyExists(draftId)
            activePlan == null -> StartSessionResult.NoActivePlan
            workout == null || workout.workout.planId != activePlan.id -> StartSessionResult.WorkoutNotInActivePlan
            else -> StartSessionResult.Started(createDraft(activePlan, workout))
        }
    }

    /** RF-59 a RF-61 e RF-65 a RF-72. Deve ser chamada dentro de uma transação. */
    private suspend fun createDraft(plan: PlanEntity, workout: WorkoutWithExercises): Long {
        val sessionId = sessionDao.insertSession(
            SessionEntity(
                status = SessionStatus.DRAFT,
                sourcePlanId = plan.id,
                planName = plan.name,
                sourceWorkoutId = workout.workout.id,
                workoutName = workout.workout.name,
                startedAt = clock.instant(),
            ),
        )
        workout.exercises
            .sortedBy { it.workoutExercise.position }
            .forEachIndexed { position, item ->
                val prescription = item.workoutExercise
                val sessionExerciseId = sessionDao.insertSessionExercise(
                    SessionExerciseEntity(
                        sessionId = sessionId,
                        exerciseId = item.exercise.id,
                        exerciseName = item.exercise.name,
                        muscleGroups = item.exercise.muscleGroups,
                        position = position,
                        targetSets = prescription.targetSets,
                        repMin = prescription.repMin,
                        repMax = prescription.repMax,
                        restSeconds = prescription.restSeconds,
                    ),
                )
                val previous = sessionDao.getLastCompletedSets(item.exercise.id)
                    .map { SetValues(weightKg = it.weightKg, reps = it.reps) }
                val sets = SetPrefill.build(prescription.targetSets, previous).mapIndexed { setPosition, values ->
                    SessionSetEntity(
                        sessionExerciseId = sessionExerciseId,
                        position = setPosition,
                        weightKg = values.weightKg,
                        reps = values.reps,
                        isCompleted = false,
                    )
                }
                sessionDao.insertSets(sets)
            }
        return sessionId
    }

    override suspend fun updateSetValues(setId: Long, weightKg: Double?, reps: Int?) {
        require(weightKg == null || weightKg >= 0.0) { "A carga não pode ser negativa (RF-75)." }
        require(reps == null || reps >= 0) { "Repetições devem ser um inteiro não negativo (RF-77)." }
        sessionDao.updateSetValues(setId, weightKg, reps)
    }

    override suspend fun setSetCompleted(setId: Long, completed: Boolean) = database.withTransaction {
        val updated = sessionDao.updateSetCompleted(setId, completed) > 0
        if (updated && completed) {
            val exercise = sessionDao.getExerciseOfSet(setId)
            if (exercise != null) {
                // RF-90/RF-91: um único cronômetro por sessão, reiniciado com o descanso da série recém-concluída.
                // Com descanso zero (RF-89) o cronômetro anterior é encerrado e nenhum novo é iniciado.
                val timer = RestTimer.start(clock.instant(), Duration.ofSeconds(exercise.restSeconds.toLong()))
                sessionDao.updateRest(exercise.sessionId, timer?.startedAt, timer?.endsAt)
            }
        }
    }

    override suspend fun addSet(sessionExerciseId: Long) = database.withTransaction {
        if (sessionDao.countDraftExercise(sessionExerciseId) == 0) return@withTransaction
        val lastSet = sessionDao.getLastSet(sessionExerciseId)
        sessionDao.insertSet(
            SessionSetEntity(
                sessionExerciseId = sessionExerciseId,
                position = (lastSet?.position ?: -1) + 1,
                weightKg = lastSet?.weightKg,
                reps = lastSet?.reps,
                isCompleted = false,
            ),
        )
        Unit
    }

    override suspend fun removeSet(setId: Long): Boolean = sessionDao.deleteUncompletedSet(setId) > 0

    override suspend fun skipRest(sessionId: Long) = sessionDao.updateRest(sessionId, startedAt = null, endsAt = null)

    override suspend fun finishSession(sessionId: Long): FinishSessionResult = database.withTransaction {
        when {
            sessionDao.countDraft(sessionId) == 0 -> FinishSessionResult.NotADraft
            sessionDao.countCompletedSets(sessionId) == 0 -> FinishSessionResult.NoCompletedSets
            else -> {
                sessionDao.deleteUncompletedSets(sessionId)
                sessionDao.deleteExercisesWithoutSets(sessionId)
                sessionDao.markCompleted(sessionId, clock.instant())
                FinishSessionResult.Finished
            }
        }
    }

    override suspend fun discardSession(sessionId: Long): Boolean = sessionDao.deleteDraft(sessionId) > 0
}
