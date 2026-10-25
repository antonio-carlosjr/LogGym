package com.loggym.e2e

import android.content.Context
import androidx.room.Room
import com.loggym.data.local.LogGymDatabase
import com.loggym.data.local.NativeExerciseSeedCallback
import com.loggym.data.local.dao.ExerciseDao
import com.loggym.data.local.dao.PlanDao
import com.loggym.data.local.dao.SessionDao
import com.loggym.di.DatabaseModule
import com.loggym.di.RepositoryModule
import com.loggym.data.repository.*
import com.loggym.domain.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.time.Clock
import javax.inject.Singleton

/** Mesmo grafo do app, com banco em memória e a mesma carga inicial. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class, RepositoryModule::class])
object TestDatabaseModule {

    @Provides @Singleton
    fun provideBackup(impl: com.loggym.data.backup.BackupRepositoryImpl): com.loggym.domain.backup.BackupRepository = impl

    @Provides @Singleton
    fun provideExercises(impl: ExerciseRepositoryImpl): ExerciseRepository = impl

    @Provides @Singleton
    fun providePlans(impl: PlanRepositoryImpl): PlanRepository = impl

    @Provides @Singleton
    fun provideSessions(impl: SessionRepositoryImpl): SessionRepository = impl

    // Estado próprio de cada componente de teste: não apaga preferências do app real.
    @Provides @Singleton
    fun providePreferences(): UserPreferencesRepository = com.loggym.FakeUserPreferences()

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
