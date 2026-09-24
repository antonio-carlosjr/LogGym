package com.loggym.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    /** RF-08: o onboarding só aparece até ser concluído. */
    val onboardingCompleted: Flow<Boolean>

    suspend fun setOnboardingCompleted()
}
