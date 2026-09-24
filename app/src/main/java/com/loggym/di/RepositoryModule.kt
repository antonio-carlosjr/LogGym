package com.loggym.di

import com.loggym.data.preferences.DataStoreUserPreferencesRepository
import com.loggym.data.repository.ExerciseRepositoryImpl
import com.loggym.data.repository.PlanRepositoryImpl
import com.loggym.data.repository.SessionRepositoryImpl
import com.loggym.domain.repository.ExerciseRepository
import com.loggym.domain.repository.PlanRepository
import com.loggym.domain.repository.SessionRepository
import com.loggym.domain.repository.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindExerciseRepository(impl: ExerciseRepositoryImpl): ExerciseRepository

    @Binds
    @Singleton
    abstract fun bindPlanRepository(impl: PlanRepositoryImpl): PlanRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(impl: DataStoreUserPreferencesRepository): UserPreferencesRepository
}
