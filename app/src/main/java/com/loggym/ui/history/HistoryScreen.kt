package com.loggym.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.loggym.ui.theme.LogGymSpacing
import com.loggym.ui.components.LoadingState
import com.loggym.ui.components.EmptyState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loggym.R
import com.loggym.ui.util.formatSessionDate
import com.loggym.ui.util.sessionDurationSeconds

@Composable
fun HistoryScreen(onBack: () -> Unit, onOpenSession: (Long) -> Unit, viewModel: HistoryViewModel = hiltViewModel(), showBack: Boolean = true) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(topBar = { HistoryTopBar(stringResource(R.string.history_title), onBack, showBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(LogGymSpacing.screen),
            verticalArrangement = Arrangement.spacedBy(LogGymSpacing.medium)) {
            when {
                state.isLoading -> item { LoadingState() }
                state.failed -> item { HistoryError(viewModel::retry) }
                state.sessions.isEmpty() -> item { EmptyState(stringResource(R.string.history_empty)) }
            }
            items(state.sessions, key = { it.id }) { session ->
                ElevatedCard(onClick = { onOpenSession(session.id) }, modifier = Modifier.fillMaxWidth(),
                    colors = com.loggym.ui.components.LogGymCardDefaults.colors(), elevation = com.loggym.ui.components.LogGymCardDefaults.elevation()) {
                    Column(Modifier.padding(LogGymSpacing.screen), verticalArrangement = Arrangement.spacedBy(LogGymSpacing.extraSmall)) {
                        Text(session.workoutName, style = MaterialTheme.typography.titleMedium)
                        Text(session.planName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(session.startedAt.formatSessionDate(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(historyDuration(sessionDurationSeconds(session.startedAt, session.finishedAt)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryTopBar(title: String, onBack: () -> Unit, showBack: Boolean = true) {
    TopAppBar(title = { Text(title, Modifier.semantics { heading() }) }, navigationIcon = {
        if (showBack) IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
        }
    })
}

@Composable
internal fun HistoryError(onRetry: () -> Unit) {
    com.loggym.ui.components.LoadError(onRetry, stringResource(R.string.history_load_error))
}

@Composable
internal fun historyDuration(seconds: Long?): String = if (seconds == null) {
    stringResource(R.string.history_duration_unknown)
} else stringResource(R.string.history_duration, seconds / 60, seconds % 60)
