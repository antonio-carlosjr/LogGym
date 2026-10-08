package com.loggym.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.loggym.data.local.entity.ExerciseEntity
import com.loggym.domain.model.MuscleGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    suspend fun getById(exerciseId: Long): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE native_key IN (:keys)")
    suspend fun getByNativeKeys(keys: List<String>): List<ExerciseEntity>

    @Insert
    suspend fun insert(exercise: ExerciseEntity): Long

    /** O filtro `is_custom = 1` impede a edição de nativos (RF-18). */
    @Query(
        """
        UPDATE exercises SET name = :name, muscle_groups = :muscleGroups, description = :description
        WHERE id = :exerciseId AND is_custom = 1
        """,
    )
    suspend fun updateCustom(
        exerciseId: Long,
        name: String,
        muscleGroups: Set<MuscleGroup>,
        description: String?,
    ): Int

    /** RF-133: a observação é do usuário, então vale também para nativos. */
    @Query("UPDATE exercises SET user_note = :note WHERE id = :exerciseId")
    suspend fun updateUserNote(exerciseId: Long, note: String?): Int

    /** O filtro `is_custom = 1` impede a exclusão de nativos (RF-19). */
    @Query("DELETE FROM exercises WHERE id = :exerciseId AND is_custom = 1")
    suspend fun deleteCustom(exerciseId: Long): Int

    @Query("SELECT COUNT(*) FROM workout_exercises WHERE exercise_id = :exerciseId")
    suspend fun countPlanUsages(exerciseId: Long): Int
}
