package com.loggym.data.preferences

import android.content.Context
import com.loggym.domain.backup.PersistenceCoordinator
import com.loggym.domain.repository.PersistedPreferences
import kotlinx.coroutines.flow.first
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.loggym.domain.model.ThemeMode
import com.loggym.domain.model.UserSettings
import androidx.datastore.preferences.preferencesDataStore
import com.loggym.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class DataStoreUserPreferencesRepository(
    private val store: DataStore<Preferences>,
    private val coordinator: PersistenceCoordinator = PersistenceCoordinator(),
) : UserPreferencesRepository {
    @Inject constructor(@ApplicationContext context: Context, coordinator: PersistenceCoordinator = PersistenceCoordinator()) : this(context.dataStore, coordinator)

    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
    private val themeKey = stringPreferencesKey("theme_mode")
    private val prefillKey = booleanPreferencesKey("prefill_enabled")

    override suspend fun snapshot(): PersistedPreferences {
        val data = store.data.first()
        return PersistedPreferences(UserSettings(ThemeMode.fromKey(data[themeKey]), data[prefillKey] ?: true), data[onboardingCompletedKey] ?: false)
    }
    override suspend fun replace(snapshot: PersistedPreferences) = coordinator.write {
        store.edit {
            it.clear()
            it[themeKey] = snapshot.settings.themeMode.key
            it[prefillKey] = snapshot.settings.prefillEnabled
            it[onboardingCompletedKey] = snapshot.onboardingCompleted
        }
        Unit
    }
    override val settings: Flow<UserSettings> = store.data.map {
        UserSettings(ThemeMode.fromKey(it[themeKey]), it[prefillKey] ?: true)
    }
    override suspend fun setThemeMode(mode: ThemeMode) = coordinator.write {
        store.edit { it[themeKey] = mode.key }; Unit
    }
    override suspend fun setPrefillEnabled(enabled: Boolean) = coordinator.write {
        store.edit { it[prefillKey] = enabled }; Unit
    }

    override val onboardingCompleted: Flow<Boolean> =
        store.data.map { preferences -> preferences[onboardingCompletedKey] ?: false }

    override suspend fun setOnboardingCompleted() = coordinator.write {
        store.edit { preferences -> preferences[onboardingCompletedKey] = true }; Unit
    }
}
