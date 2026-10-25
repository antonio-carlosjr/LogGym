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
import com.loggym.domain.backup.*
import androidx.activity.compose.BackHandler
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.loggym.R

data class AppUiState(val startDestination: Any? = null, val settings: UserSettings? = null, val failed: Boolean = false)

@HiltViewModel
class AppViewModel @Inject constructor(private val preferences: UserPreferencesRepository,
    private val backup: BackupRepository? = null,
    val persistence: PersistenceCoordinator = PersistenceCoordinator(),
) : ViewModel() {
    private val state = MutableStateFlow(AppUiState())
    val uiState = state.asStateFlow()
    private var loading: Job? = null
    private val settingsAfterRestore = MutableStateFlow(false)
    val openSettings = settingsAfterRestore.asStateFlow()
    fun consumedSettings() { settingsAfterRestore.value = false }
    init { retry() }
    fun retry() {
        loading?.cancel()
        loading = viewModelScope.launch {
            try {
                backup?.recover()
                val destination = state.value.startDestination ?: if (preferences.onboardingCompleted.first()) HomeRoute else OnboardingRoute
                preferences.settings.collect { settings -> state.value = AppUiState(destination, settings) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                state.update { it.copy(failed = true) }
            }
        }
    }
    init {
        viewModelScope.launch {
            persistence.generation.drop(1).collect { settingsAfterRestore.value = true }
        }
    }
}

@Composable
fun LogGymApp(onThemeApplied: (Boolean) -> Unit = {}, viewModel: AppViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase by viewModel.persistence.phase.collectAsStateWithLifecycle()
    val generation by viewModel.persistence.generation.collectAsStateWithLifecycle()
    val settingsAfterRestore by viewModel.openSettings.collectAsStateWithLifecycle()
    val dark = (state.settings?.themeMode ?: ThemeMode.SYSTEM).isDark(isSystemInDarkTheme())
    SideEffect { onThemeApplied(dark) }
    LogGymTheme(darkTheme = dark) {
        BackHandler(phase in setOf(PersistencePhase.RESTORING, PersistencePhase.RECOVERING, PersistencePhase.BLOCKED)) {}
        Column(Modifier.fillMaxSize()) {
            if (state.failed || phase == PersistencePhase.BLOCKED) LoadError(viewModel::retry)
            if (phase in setOf(PersistencePhase.RESTORING, PersistencePhase.RECOVERING)) {
                CircularProgressIndicator()
                Text(stringResource(if (phase == PersistencePhase.RESTORING) R.string.backup_restoring else R.string.backup_recovering))
            }
            val destination = state.startDestination
            if (destination != null && phase !in setOf(PersistencePhase.BLOCKED, PersistencePhase.RECOVERING, PersistencePhase.RESTORING)) {
                key(generation) { Box(Modifier.weight(1f)) {
                    val controller = androidx.navigation.compose.rememberNavController()
                    val navigationGeneration = generation
                    DisposableEffect(controller) {
                        onDispose {
                            // Libera também os stores das rotas antigas e seus participantes de flush.
                            // Rotação sem troca de geração conserva a pilha normalmente.
                            if (viewModel.persistence.generation.value != navigationGeneration) {
                                controller.popBackStack(controller.graph.id, inclusive = true)
                            }
                        }
                    }
                    LogGymNavHost(startDestination = if (generation > 0) HomeRoute else destination,
                        navController = controller,
                        openSettings = settingsAfterRestore, onSettingsOpened = viewModel::consumedSettings)
                } }
            }
        }
    }
}
