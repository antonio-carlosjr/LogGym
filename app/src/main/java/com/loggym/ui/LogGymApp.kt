package com.loggym.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.loggym.domain.repository.UserPreferencesRepository
import com.loggym.ui.navigation.HomeRoute
import com.loggym.ui.navigation.LogGymNavHost
import com.loggym.ui.navigation.OnboardingRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    preferences: UserPreferencesRepository,
) : ViewModel() {

    /** Lido uma única vez: o destino inicial não deve mudar enquanto o NavHost está montado (RF-03, RF-08). */
    val startDestination: StateFlow<Any?> = flow {
        emit(if (preferences.onboardingCompleted.first()) HomeRoute else OnboardingRoute)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

@Composable
fun LogGymApp(viewModel: AppViewModel = hiltViewModel()) {
    val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()

    when (val destination = startDestination) {
        null -> Box(Modifier.fillMaxSize())
        else -> LogGymNavHost(startDestination = destination)
    }
}
