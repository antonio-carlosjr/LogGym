package com.loggym.ui

import com.loggym.FakeUserPreferences
import com.loggym.MainDispatcherRule
import com.loggym.awaitFirst
import com.loggym.domain.model.ThemeMode
import com.loggym.ui.navigation.OnboardingRoute
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*

class AppViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    @Test fun settingsChangesDoNotResetTheInitialDestination() = runTest {
        val preferences = FakeUserPreferences()
        val vm = AppViewModel(preferences)
        assertEquals(OnboardingRoute, vm.uiState.awaitFirst { it.startDestination != null }.startDestination)
        preferences.setOnboardingCompleted()
        preferences.setThemeMode(ThemeMode.DARK)
        val changed = vm.uiState.awaitFirst { it.settings?.themeMode == ThemeMode.DARK }
        assertEquals(OnboardingRoute, changed.startDestination)
    }
}
