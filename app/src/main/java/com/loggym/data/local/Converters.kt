package com.loggym.data.local

import androidx.room.TypeConverter
import com.loggym.domain.model.MuscleGroup
import java.time.Instant

class Converters {
    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun muscleGroupsToKeys(groups: Set<MuscleGroup>): String = encodeMuscleGroups(groups)

    @TypeConverter
    fun keysToMuscleGroups(keys: String): Set<MuscleGroup> =
        keys.split(',').mapNotNull { MuscleGroup.fromKey(it.trim()) }.toSet()

    companion object {
        /** Ordem estável para que o mesmo conjunto gere sempre o mesmo texto. */
        fun encodeMuscleGroups(groups: Set<MuscleGroup>): String =
            groups.sortedBy { it.ordinal }.joinToString(",") { it.key }
    }
}
