package com.loggym.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.loggym.data.local.entity.PlanEntity
import com.loggym.data.local.entity.WorkoutEntity
import com.loggym.data.local.entity.WorkoutExerciseEntity
import com.loggym.data.local.relation.PlanWithWorkouts
import com.loggym.data.local.relation.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Query("SELECT * FROM plans ORDER BY is_active DESC, name COLLATE NOCASE")
    fun observePlans(): Flow<List<PlanEntity>>

    @Transaction
    @Query("SELECT * FROM plans WHERE is_active = 1 LIMIT 1")
    fun observeActivePlan(): Flow<PlanWithWorkouts?>

    @Query("SELECT * FROM plans WHERE is_active = 1 LIMIT 1")
    suspend fun getActivePlan(): PlanEntity?

    @Query("SELECT name FROM plans")
    suspend fun getPlanNames(): List<String>

    @Transaction
    @Query("SELECT * FROM plans WHERE id = :planId")
    fun observePlan(planId: Long): Flow<PlanWithWorkouts?>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    suspend fun getWorkoutWithExercises(workoutId: Long): WorkoutWithExercises?

    @Insert
    suspend fun insertPlan(plan: PlanEntity): Long

    @Query("UPDATE plans SET name = :name WHERE id = :planId")
    suspend fun renamePlan(planId: Long, name: String)

    @Query("DELETE FROM plans WHERE id = :planId")
    suspend fun deletePlan(planId: Long)

    @Query("UPDATE plans SET is_active = 0 WHERE is_active = 1")
    suspend fun deactivateAll()

    @Query("UPDATE plans SET is_active = 1 WHERE id = :planId")
    suspend fun activate(planId: Long): Int

    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Query("UPDATE workouts SET name = :name WHERE id = :workoutId")
    suspend fun renameWorkout(workoutId: Long, name: String)

    @Query("DELETE FROM workouts WHERE id = :workoutId")
    suspend fun deleteWorkout(workoutId: Long)

    @Query("UPDATE workouts SET position = :position WHERE id = :workoutId")
    suspend fun updateWorkoutPosition(workoutId: Long, position: Int)

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM workouts WHERE plan_id = :planId")
    suspend fun nextWorkoutPosition(planId: Long): Int

    @Insert
    suspend fun insertWorkoutExercise(workoutExercise: WorkoutExerciseEntity): Long

    @Query(
        """
        UPDATE workout_exercises
        SET target_sets = :targetSets, rep_min = :repMin, rep_max = :repMax, rest_seconds = :restSeconds
        WHERE id = :workoutExerciseId
        """,
    )
    suspend fun updateWorkoutExerciseTarget(
        workoutExerciseId: Long,
        targetSets: Int,
        repMin: Int,
        repMax: Int,
        restSeconds: Int,
    )

    @Query("DELETE FROM workout_exercises WHERE id = :workoutExerciseId")
    suspend fun deleteWorkoutExercise(workoutExerciseId: Long)

    @Query("UPDATE workout_exercises SET position = :position WHERE id = :workoutExerciseId")
    suspend fun updateWorkoutExercisePosition(workoutExerciseId: Long, position: Int)

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM workout_exercises WHERE workout_id = :workoutId")
    suspend fun nextWorkoutExercisePosition(workoutId: Long): Int
}
