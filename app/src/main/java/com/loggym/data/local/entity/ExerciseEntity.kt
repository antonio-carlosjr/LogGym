package com.loggym.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.loggym.domain.model.MuscleGroup

@Entity(
    tableName = "exercises",
    indices = [Index(value = ["native_key"], unique = true)],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** RF-130: um ou mais grupos, gravados pelas chaves estáveis. */
    @ColumnInfo(name = "muscle_groups") val muscleGroups: Set<MuscleGroup> = emptySet(),
    /** RF-134: fixa nos nativos, livre nos personalizados. */
    val description: String? = null,
    /** RF-133: observação do usuário, editável inclusive nos nativos. */
    @ColumnInfo(name = "user_note") val userNote: String? = null,
    @ColumnInfo(name = "is_custom") val isCustom: Boolean,
    /** Chave estável dos exercícios nativos (usada pelos templates); nula nos personalizados. */
    @ColumnInfo(name = "native_key") val nativeKey: String?,
)
