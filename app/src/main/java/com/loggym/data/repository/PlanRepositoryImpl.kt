package com.loggym.data.repository

import com.loggym.domain.backup.PersistenceCoordinator
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
    private val coordinator: PersistenceCoordinator = PersistenceCoordinator(),
) : PlanRepository {

    override fun templates(): List<PlanTemplate> = PlanTemplates.all

    override fun observePlans(): Flow<List<PlanSummary>> =
        planDao.observePlans().map { plans -> plans.map { it.toSummary() } }

    override fun observeActivePlan(): Flow<Plan?> = planDao.observeActivePlan().map { it?.toDomain() }

    override fun observePlan(planId: Long): Flow<Plan?> = planDao.observePlan(planId).map { it?.toDomain() }

    override suspend fun createPlan(name: String): Long = coordinator.write {
        planDao.insertPlan(PlanEntity(name = requireName(name), createdAt = clock.instant()))
    }

    override suspend fun createFromTemplate(templateKey: String, activate: Boolean): Long = coordinator.write {
        database.withTransaction {
            val template = requireNotNull(PlanTemplates.byKey(templateKey)) { "Template desconhecido: $templateKey" }
            val nativeKeys = template.workouts.flatMap { workout -> workout.exercises.map { it.nativeKey } }.distinct()
            val exerciseIdsByKey = exerciseDao.getByNativeKeys(nativeKeys)
                .associate { exercise -> exercise.nativeKey to exercise.id }

            val planId = planDao.insertPlan(
                PlanEntity(name = uniquePlanName(template.name), templateKey = template.key, createdAt = clock.instant()),
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
    }

    override suspend fun renamePlan(planId: Long, name: String) = coordinator.write { planDao.renamePlan(planId, requireName(name))
    }

    override suspend fun deletePlan(planId: Long) = coordinator.write { planDao.deletePlan(planId)
    }

    override suspend fun setActivePlan(planId: Long) = coordinator.write { database.withTransaction { activatePlan(planId) }
    }

    override suspend fun addWorkout(planId: Long, name: String): Long = coordinator.write { database.withTransaction {
        planDao.insertWorkout(
            WorkoutEntity(planId = planId, name = requireName(name), position = planDao.nextWorkoutPosition(planId)),
        )
    }
    }

    override suspend fun renameWorkout(workoutId: Long, name: String) = coordinator.write {
        planDao.renameWorkout(workoutId, requireName(name))
    }

    override suspend fun deleteWorkout(workoutId: Long) = coordinator.write { planDao.deleteWorkout(workoutId)
    }

    override suspend fun reorderWorkouts(orderedWorkoutIds: List<Long>) = coordinator.write { database.withTransaction {
        orderedWorkoutIds.forEachIndexed { position, workoutId -> planDao.updateWorkoutPosition(workoutId, position) }
    }
    }

    override suspend fun addExerciseToWorkout(workoutId: Long, exerciseId: Long, target: ExerciseTarget): Long = coordinator.write {
        database.withTransaction {
            require(exerciseDao.getById(exerciseId)?.trackingType == target.trackingType)
            val position = planDao.nextWorkoutExercisePosition(workoutId)
            planDao.insertWorkoutExercise(target.toEntity(workoutId, exerciseId, position))
        }
    }

    override suspend fun updateWorkoutExercise(workoutExerciseId: Long, target: ExerciseTarget) = coordinator.write { database.withTransaction {
        val item = requireNotNull(planDao.getWorkoutExercise(workoutExerciseId))
        require(exerciseDao.getById(item.exerciseId)?.trackingType == target.trackingType)
        planDao.updateWorkoutExerciseTarget(
            workoutExerciseId = workoutExerciseId,
            targetSets = target.targetSets,
            repMin = target.repMin,
            repMax = target.repMax,
            restSeconds = target.rest.seconds.toInt(),
            durationSeconds = target.durationSeconds,
        )

    }
    }

    override suspend fun removeExerciseFromWorkout(workoutExerciseId: Long) = coordinator.write {
        planDao.deleteWorkoutExercise(workoutExerciseId)
    }

    override suspend fun reorderWorkoutExercises(orderedWorkoutExerciseIds: List<Long>) = coordinator.write { database.withTransaction {
        orderedWorkoutExerciseIds.forEachIndexed { position, id -> planDao.updateWorkoutExercisePosition(id, position) }
    }
    }

    /** RF-145: "Nome", "Nome (2)", "Nome (3)"… para distinguir instâncias do mesmo template. */
    private suspend fun uniquePlanName(baseName: String): String {
        val taken = planDao.getPlanNames().toSet()
        if (baseName !in taken) return baseName
        return generateSequence(2) { it + 1 }.map { "$baseName ($it)" }.first { it !in taken }
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
        targetDurationSeconds = durationSeconds,
    )
}
