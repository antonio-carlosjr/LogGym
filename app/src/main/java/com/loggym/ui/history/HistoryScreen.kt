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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loggym.R
import com.loggym.ui.util.formatSessionDate
import com.loggym.ui.util.sessionDurationSeconds

@Composable
fun HistoryScreen(onBack: () -> Unit, onOpenSession: (Long) -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(topBar = { HistoryTopBar(stringResource(R.string.history_title), onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                state.isLoading -> item { CircularProgressIndicator() }
                state.failed -> item { HistoryError(viewModel::retry) }
                state.sessions.isEmpty() -> item { Text(stringResource(R.string.history_empty)) }
            }
            items(state.sessions, key = { it.id }) { session ->
                ElevatedCard(onClick = { onOpenSession(session.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(session.workoutName, style = MaterialTheme.typography.titleMedium)
                        Text(session.planName)
                        Text(session.startedAt.formatSessionDate())
                        Text(historyDuration(sessionDurationSeconds(session.startedAt, session.finishedAt)))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(title = { Text(title, Modifier.semantics { heading() }) }, navigationIcon = {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
        }
    })
}

@Composable
internal fun HistoryError(onRetry: () -> Unit) {
    Column {
        Text(stringResource(R.string.history_load_error), color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
internal fun historyDuration(seconds: Long?): String = if (seconds == null) {
    stringResource(R.string.history_duration_unknown)
} else stringResource(R.string.history_duration, seconds / 60, seconds % 60)
