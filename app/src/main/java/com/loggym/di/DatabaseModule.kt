package com.loggym.di

import android.content.Context
import androidx.room.Room
import com.loggym.data.local.LogGymDatabase
import com.loggym.data.local.NativeExerciseSeedCallback
import com.loggym.data.local.dao.ExerciseDao
import com.loggym.data.local.dao.PlanDao
import com.loggym.data.local.dao.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LogGymDatabase =
        Room.databaseBuilder(context, LogGymDatabase::class.java, LogGymDatabase.NAME)
            .addCallback(NativeExerciseSeedCallback())
            .build()

    @Provides
    fun provideExerciseDao(database: LogGymDatabase): ExerciseDao = database.exerciseDao()

    @Provides
    fun providePlanDao(database: LogGymDatabase): PlanDao = database.planDao()

    @Provides
    fun provideSessionDao(database: LogGymDatabase): SessionDao = database.sessionDao()

    /** Injetado para permitir testes determinísticos de horários e cronômetro. */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()
}
