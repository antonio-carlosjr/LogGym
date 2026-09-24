package com.loggym.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.loggym.data.local.dao.ExerciseDao
import com.loggym.data.local.dao.PlanDao
import com.loggym.data.local.dao.SessionDao
import com.loggym.data.local.entity.ExerciseEntity
import com.loggym.data.local.entity.PlanEntity
import com.loggym.data.local.entity.SessionEntity
import com.loggym.data.local.entity.SessionExerciseEntity
import com.loggym.data.local.entity.SessionSetEntity
import com.loggym.data.local.entity.WorkoutEntity
import com.loggym.data.local.entity.WorkoutExerciseEntity

@Database(
    entities = [
        ExerciseEntity::class,
        PlanEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        SessionEntity::class,
        SessionExerciseEntity::class,
        SessionSetEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class LogGymDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao

    abstract fun planDao(): PlanDao

    abstract fun sessionDao(): SessionDao

    companion object {
        const val NAME = "loggym.db"
    }
}
