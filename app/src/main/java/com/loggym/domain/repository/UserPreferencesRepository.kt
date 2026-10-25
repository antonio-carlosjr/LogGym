package com.loggym.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.loggym.domain.model.ThemeMode
import com.loggym.domain.model.UserSettings

interface UserPreferencesRepository {
    suspend fun snapshot(): PersistedPreferences = PersistedPreferences(settings.first(), onboardingCompleted.first())
    suspend fun replace(snapshot: PersistedPreferences) { error("Substituição de preferências não implementada") }
    val settings: Flow<UserSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setPrefillEnabled(enabled: Boolean)
    suspend fun setAutoCompleteEnabled(enabled: Boolean)
    /** RF-08: o onboarding só aparece até ser concluído. */
    val onboardingCompleted: Flow<Boolean>

    suspend fun setOnboardingCompleted()
}

data class PersistedPreferences(val settings: UserSettings, val onboardingCompleted: Boolean)
