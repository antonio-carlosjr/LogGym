package com.loggym

import com.loggym.domain.model.ThemeMode
import com.loggym.domain.model.UserSettings
import com.loggym.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow

open class FakeUserPreferences : UserPreferencesRepository {
    override suspend fun replace(snapshot: com.loggym.domain.repository.PersistedPreferences) {
        settings.value = snapshot.settings
        onboardingCompleted.value = snapshot.onboardingCompleted
    }
    override val settings = MutableStateFlow(UserSettings())
    override val onboardingCompleted = MutableStateFlow(false)
    override suspend fun setThemeMode(mode: ThemeMode) { settings.value = settings.value.copy(themeMode = mode) }
    override suspend fun setPrefillEnabled(enabled: Boolean) { settings.value = settings.value.copy(prefillEnabled = enabled) }
    override suspend fun setOnboardingCompleted() { onboardingCompleted.value = true }
}
