package com.loggym.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.loggym.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class DataStoreUserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : UserPreferencesRepository {

    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")

    override val onboardingCompleted: Flow<Boolean> =
        context.dataStore.data.map { preferences -> preferences[onboardingCompletedKey] ?: false }

    override suspend fun setOnboardingCompleted() {
        context.dataStore.edit { preferences -> preferences[onboardingCompletedKey] = true }
    }
}
