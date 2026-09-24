package com.loggym.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercises",
    indices = [Index(value = ["native_key"], unique = true)],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "muscle_group") val muscleGroup: String?,
    @ColumnInfo(name = "is_custom") val isCustom: Boolean,
    /** Chave estável dos exercícios nativos (usada pelos templates); nula nos personalizados. */
    @ColumnInfo(name = "native_key") val nativeKey: String?,
)
