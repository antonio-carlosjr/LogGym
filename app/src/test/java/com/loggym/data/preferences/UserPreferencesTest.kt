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
    @Test fun existingLocalPreferencesAreNotReplacedWhenAutomaticKeyIsMissing() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
        val file = java.io.File(context.filesDir, "legacy-${java.util.UUID.randomUUID()}.preferences_pb")
        val store = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(scope = scope) { file }
        try {
            store.updateData { data -> data.toMutablePreferences().apply {
                this[androidx.datastore.preferences.core.stringPreferencesKey("theme_mode")] = "dark"
                this[androidx.datastore.preferences.core.booleanPreferencesKey("prefill_enabled")] = false
                this[androidx.datastore.preferences.core.booleanPreferencesKey("onboarding_completed")] = true
            } }
            val repository = DataStoreUserPreferencesRepository(store)
            assertEquals(com.loggym.domain.repository.PersistedPreferences(
                com.loggym.domain.model.UserSettings(com.loggym.domain.model.ThemeMode.DARK, false, false), true), repository.snapshot())
            assertFalse(store.data.first().contains(androidx.datastore.preferences.core.booleanPreferencesKey("auto_complete_enabled")))
        } finally { scope.coroutineContext[kotlinx.coroutines.Job]!!.cancel(); file.delete() }
    }
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
