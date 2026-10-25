package com.loggym.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loggym.domain.model.ThemeMode
import com.loggym.domain.model.UserSettings
import com.loggym.domain.repository.UserPreferencesRepository
import com.loggym.ui.components.LoadError
import com.loggym.ui.navigation.*
import com.loggym.ui.theme.LogGymTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class AppUiState(val startDestination: Any? = null, val settings: UserSettings? = null, val failed: Boolean = false)

@HiltViewModel
class AppViewModel @Inject constructor(private val preferences: UserPreferencesRepository) : ViewModel() {
    private val state = MutableStateFlow(AppUiState())
    val uiState = state.asStateFlow()
    private var loading: Job? = null
    init { retry() }
    fun retry() {
        loading?.cancel()
        loading = viewModelScope.launch {
            try {
                val destination = state.value.startDestination ?: if (preferences.onboardingCompleted.first()) HomeRoute else OnboardingRoute
                preferences.settings.collect { settings -> state.value = AppUiState(destination, settings) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                state.update { it.copy(failed = true) }
            }
        }
    }
}

@Composable
fun LogGymApp(onThemeApplied: (Boolean) -> Unit = {}, viewModel: AppViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dark = (state.settings?.themeMode ?: ThemeMode.SYSTEM).isDark(isSystemInDarkTheme())
    SideEffect { onThemeApplied(dark) }
    LogGymTheme(darkTheme = dark) {
        Column(Modifier.fillMaxSize()) {
            if (state.failed) LoadError(viewModel::retry)
            val destination = state.startDestination
            if (destination != null) Box(Modifier.weight(1f)) { LogGymNavHost(startDestination = destination) }
        }
    }
}
