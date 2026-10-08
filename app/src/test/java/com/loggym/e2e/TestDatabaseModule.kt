package com.loggym.e2e

import android.content.Context
import androidx.room.Room
import com.loggym.data.local.LogGymDatabase
import com.loggym.data.local.NativeExerciseSeedCallback
import com.loggym.data.local.dao.ExerciseDao
import com.loggym.data.local.dao.PlanDao
import com.loggym.data.local.dao.SessionDao
import com.loggym.di.DatabaseModule
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.time.Clock
import javax.inject.Singleton

/** Mesmo grafo do app, com banco em memória e a mesma carga inicial. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object TestDatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LogGymDatabase =
        Room.inMemoryDatabaseBuilder(context, LogGymDatabase::class.java)
            .addCallback(NativeExerciseSeedCallback())
            .build()

    @Provides
    fun provideExerciseDao(database: LogGymDatabase): ExerciseDao = database.exerciseDao()

    @Provides
    fun providePlanDao(database: LogGymDatabase): PlanDao = database.planDao()

    @Provides
    fun provideSessionDao(database: LogGymDatabase): SessionDao = database.sessionDao()

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()
}
