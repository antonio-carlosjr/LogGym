package com.loggym.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.loggym.ui.theme.LogGymSpacing
import com.loggym.ui.components.LoadingState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loggym.R
import com.loggym.domain.model.summary
import com.loggym.ui.util.formatSessionDate
import com.loggym.ui.util.sessionDurationSeconds

@Composable
fun HistoryDetailScreen(onBack: () -> Unit, viewModel: HistoryDetailViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(topBar = { HistoryTopBar(stringResource(R.string.history_detail_title), onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(LogGymSpacing.screen),
            verticalArrangement = Arrangement.spacedBy(LogGymSpacing.medium)) {
            when {
                state.isLoading -> item { LoadingState() }
                state.failed -> item { HistoryError(viewModel::retry) }
                state.session == null -> item { Text(stringResource(R.string.history_not_found)) }
            }
            state.session?.let { session ->
                item {
                    Text(session.workoutName, style = MaterialTheme.typography.headlineSmall)
                    Text(session.planName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(session.startedAt.formatSessionDate(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(historyDuration(sessionDurationSeconds(session.startedAt, session.finishedAt)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.history_read_only), style = MaterialTheme.typography.labelMedium)
                }
                session.exercises.forEach { exercise ->
                    item(key = "exercise-${exercise.id}") {
                        Text(exercise.name, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.session_prescription, exercise.target.summary(), exercise.target.rest.seconds))
                    }
                    exercise.sets.forEachIndexed { index, set ->
                        item(key = "set-${set.id}") {
                            Card(Modifier.fillMaxWidth(), colors = com.loggym.ui.components.LogGymCardDefaults.colors()) {
                                Column(Modifier.padding(LogGymSpacing.screen), verticalArrangement = Arrangement.spacedBy(LogGymSpacing.extraSmall)) {
                                    HistorySetContent(index + 1, set, exercise.trackingType)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
