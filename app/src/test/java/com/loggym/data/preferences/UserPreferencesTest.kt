package com.loggym.data.preferences

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserPreferencesTest {
    @Test fun onboardingFlagIsPersistedAndReadByNewRepositoryInstance() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val first = DataStoreUserPreferencesRepository(context)
        assertFalse(first.onboardingCompleted.first())
        assertEquals(com.loggym.domain.model.UserSettings(), first.settings.first())
        assertFalse(first.settings.first().autoCompleteEnabled)
        first.setAutoCompleteEnabled(true)
        first.setThemeMode(com.loggym.domain.model.ThemeMode.DARK)
        first.setPrefillEnabled(false)
        first.setOnboardingCompleted()
        val reopened = DataStoreUserPreferencesRepository(context)
        assertTrue(reopened.onboardingCompleted.first())
        assertEquals(com.loggym.domain.model.ThemeMode.DARK, reopened.settings.first().themeMode)
        assertFalse(reopened.settings.first().prefillEnabled)
        assertTrue(reopened.settings.first().autoCompleteEnabled)
    }
}
