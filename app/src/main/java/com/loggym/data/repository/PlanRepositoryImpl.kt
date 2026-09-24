package com.loggym.data.repository

import androidx.room.withTransaction
import com.loggym.data.local.LogGymDatabase
import com.loggym.data.local.dao.ExerciseDao
import com.loggym.data.local.dao.PlanDao
import com.loggym.data.local.entity.PlanEntity
import com.loggym.data.local.entity.WorkoutEntity
import com.loggym.data.local.entity.WorkoutExerciseEntity
import com.loggym.data.mapper.toDomain
import com.loggym.data.mapper.toSummary
import com.loggym.data.seed.PlanTemplates
import com.loggym.domain.model.ExerciseTarget
import com.loggym.domain.model.Plan
import com.loggym.domain.model.PlanSummary
import com.loggym.domain.model.PlanTemplate
import com.loggym.domain.repository.PlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

class PlanRepositoryImpl @Inject constructor(
    private val database: LogGymDatabase,
    private val planDao: PlanDao,
    private val exerciseDao: ExerciseDao,
    private val clock: Clock,
) : PlanRepository {

    override fun templates(): List<PlanTemplate> = PlanTemplates.all

    override fun observePlans(): Flow<List<PlanSummary>> =
        planDao.observePlans().map { plans -> plans.map { it.toSummary() } }

    override fun observeActivePlan(): Flow<Plan?> = planDao.observeActivePlan().map { it?.toDomain() }

    override fun observePlan(planId: Long): Flow<Plan?> = planDao.observePlan(planId).map { it?.toDomain() }

    override suspend fun createPlan(name: String): Long =
        planDao.insertPlan(PlanEntity(name = requireName(name), createdAt = clock.instant()))

    override suspend fun createFromTemplate(templateKey: String, activate: Boolean): Long =
        database.withTransaction {
            val template = requireNotNull(PlanTemplates.byKey(templateKey)) { "Template desconhecido: $templateKey" }
            val nativeKeys = template.workouts.flatMap { workout -> workout.exercises.map { it.nativeKey } }.distinct()
            val exerciseIdsByKey = exerciseDao.getByNativeKeys(nativeKeys)
                .associate { exercise -> exercise.nativeKey to exercise.id }

            val planId = planDao.insertPlan(
                PlanEntity(name = template.name, templateKey = template.key, createdAt = clock.instant()),
            )
            for ((workoutPosition, workout) in template.workouts.withIndex()) {
                val workoutId = planDao.insertWorkout(
                    WorkoutEntity(planId = planId, name = workout.name, position = workoutPosition),
                )
                for ((exercisePosition, exercise) in workout.exercises.withIndex()) {
                    val exerciseId = exerciseIdsByKey[exercise.nativeKey] ?: continue
                    planDao.insertWorkoutExercise(exercise.target.toEntity(workoutId, exerciseId, exercisePosition))
                }
            }
            if (activate) activatePlan(planId)
            planId
        }

    override suspend fun renamePlan(planId: Long, name: String) = planDao.renamePlan(planId, requireName(name))

    override suspend fun deletePlan(planId: Long) = planDao.deletePlan(planId)

    override suspend fun setActivePlan(planId: Long) = database.withTransaction { activatePlan(planId) }

    override suspend fun addWorkout(planId: Long, name: String): Long = database.withTransaction {
        planDao.insertWorkout(
            WorkoutEntity(planId = planId, name = requireName(name), position = planDao.nextWorkoutPosition(planId)),
        )
    }

    override suspend fun renameWorkout(workoutId: Long, name: String) =
        planDao.renameWorkout(workoutId, requireName(name))

    override suspend fun deleteWorkout(workoutId: Long) = planDao.deleteWorkout(workoutId)

    override suspend fun reorderWorkouts(orderedWorkoutIds: List<Long>) = database.withTransaction {
        orderedWorkoutIds.forEachIndexed { position, workoutId -> planDao.updateWorkoutPosition(workoutId, position) }
    }

    override suspend fun addExerciseToWorkout(workoutId: Long, exerciseId: Long, target: ExerciseTarget): Long =
        database.withTransaction {
            val position = planDao.nextWorkoutExercisePosition(workoutId)
            planDao.insertWorkoutExercise(target.toEntity(workoutId, exerciseId, position))
        }

    override suspend fun updateWorkoutExercise(workoutExerciseId: Long, target: ExerciseTarget) =
        planDao.updateWorkoutExerciseTarget(
            workoutExerciseId = workoutExerciseId,
            targetSets = target.targetSets,
            repMin = target.repMin,
            repMax = target.repMax,
            restSeconds = target.rest.seconds.toInt(),
        )

    override suspend fun removeExerciseFromWorkout(workoutExerciseId: Long) =
        planDao.deleteWorkoutExercise(workoutExerciseId)

    override suspend fun reorderWorkoutExercises(orderedWorkoutExerciseIds: List<Long>) = database.withTransaction {
        orderedWorkoutExerciseIds.forEachIndexed { position, id -> planDao.updateWorkoutExercisePosition(id, position) }
    }

    /** RF-28/RF-29: deve ser chamada dentro de uma transação. */
    private suspend fun activatePlan(planId: Long) {
        planDao.deactivateAll()
        planDao.activate(planId)
    }

    private fun ExerciseTarget.toEntity(workoutId: Long, exerciseId: Long, position: Int) = WorkoutExerciseEntity(
        workoutId = workoutId,
        exerciseId = exerciseId,
        position = position,
        targetSets = targetSets,
        repMin = repMin,
        repMax = repMax,
        restSeconds = rest.seconds.toInt(),
    )
}
