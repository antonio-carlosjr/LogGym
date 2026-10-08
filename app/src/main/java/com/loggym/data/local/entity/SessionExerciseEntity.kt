package com.loggym.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.loggym.domain.model.MuscleGroup

/**
 * Snapshot do exercício e da prescrição no momento da sessão (RF-61, RF-129).
 * [exerciseId] é uma referência fraca, sem chave estrangeira: serve para buscar o
 * histórico pela identidade do exercício (RF-68) e sobrevive à sua exclusão (RF-125).
 */
@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["session_id"]), Index(value = ["exercise_id"])],
)
data class SessionExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long?,
    @ColumnInfo(name = "exercise_name") val exerciseName: String,
    @ColumnInfo(name = "muscle_groups") val muscleGroups: Set<MuscleGroup>,
    val position: Int,
    @ColumnInfo(name = "target_sets") val targetSets: Int,
    @ColumnInfo(name = "rep_min") val repMin: Int,
    @ColumnInfo(name = "rep_max") val repMax: Int,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int,
)
