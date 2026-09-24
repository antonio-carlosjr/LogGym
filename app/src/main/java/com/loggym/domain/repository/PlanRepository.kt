package com.loggym.domain.repository

import com.loggym.domain.model.ExerciseTarget
import com.loggym.domain.model.Plan
import com.loggym.domain.model.PlanSummary
import com.loggym.domain.model.PlanTemplate
import kotlinx.coroutines.flow.Flow

interface PlanRepository {
    fun templates(): List<PlanTemplate>

    fun observePlans(): Flow<List<PlanSummary>>

    fun observeActivePlan(): Flow<Plan?>

    fun observePlan(planId: Long): Flow<Plan?>

    suspend fun createPlan(name: String): Long

    /** RF-37: cria uma instância local e editável a partir do template. */
    suspend fun createFromTemplate(templateKey: String, activate: Boolean): Long

    suspend fun renamePlan(planId: Long, name: String)

    suspend fun deletePlan(planId: Long)

    /** RF-28/RF-29: desativa qualquer outro plano ativo. */
    suspend fun setActivePlan(planId: Long)

    suspend fun addWorkout(planId: Long, name: String): Long

    suspend fun renameWorkout(workoutId: Long, name: String)

    suspend fun deleteWorkout(workoutId: Long)

    suspend fun reorderWorkouts(orderedWorkoutIds: List<Long>)

    suspend fun addExerciseToWorkout(workoutId: Long, exerciseId: Long, target: ExerciseTarget): Long

    suspend fun updateWorkoutExercise(workoutExerciseId: Long, target: ExerciseTarget)

    suspend fun removeExerciseFromWorkout(workoutExerciseId: Long)

    suspend fun reorderWorkoutExercises(orderedWorkoutExerciseIds: List<Long>)
}
