package com.loggym.domain.repository

import kotlinx.coroutines.flow.Flow
import com.loggym.domain.model.ThemeMode
import com.loggym.domain.model.UserSettings

interface UserPreferencesRepository {
    val settings: Flow<UserSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setPrefillEnabled(enabled: Boolean)
    /** RF-08: o onboarding só aparece até ser concluído. */
    val onboardingCompleted: Flow<Boolean>

    suspend fun setOnboardingCompleted()
}
