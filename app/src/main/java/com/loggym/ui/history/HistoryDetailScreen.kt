package com.loggym.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.loggym.domain.logic.WeightFormat
import com.loggym.domain.model.TrackingType
import com.loggym.domain.model.summary
import com.loggym.ui.util.formatSessionDate
import com.loggym.ui.util.sessionDurationSeconds

@Composable
fun HistoryDetailScreen(onBack: () -> Unit, viewModel: HistoryDetailViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(topBar = { HistoryTopBar(stringResource(R.string.history_detail_title), onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                state.isLoading -> item { CircularProgressIndicator() }
                state.failed -> item { HistoryError(viewModel::retry) }
                state.session == null -> item { Text(stringResource(R.string.history_not_found)) }
            }
            state.session?.let { session ->
                item {
                    Text(session.workoutName, style = MaterialTheme.typography.headlineSmall)
                    Text(session.planName)
                    Text(session.startedAt.formatSessionDate())
                    Text(historyDuration(sessionDurationSeconds(session.startedAt, session.finishedAt)))
                    Text(stringResource(R.string.history_read_only), style = MaterialTheme.typography.labelMedium)
                }
                session.exercises.forEach { exercise ->
                    item(key = "exercise-${exercise.id}") {
                        Text(exercise.name, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.session_prescription, exercise.target.summary(), exercise.target.rest.seconds))
                    }
                    exercise.sets.forEachIndexed { index, set ->
                        item(key = "set-${set.id}") {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(stringResource(R.string.set_number, index + 1), style = MaterialTheme.typography.titleSmall)
                                    if (exercise.trackingType == TrackingType.TIME) {
                                        Text(if (set.durationSeconds == null) stringResource(R.string.history_set_duration_missing)
                                            else stringResource(R.string.history_set_duration, set.durationSeconds.toString()))
                                    } else {
                                        Text(if (set.weightKg == null) stringResource(R.string.history_set_weight_missing)
                                            else stringResource(R.string.history_set_weight, WeightFormat.format(set.weightKg)))
                                        Text(if (set.reps == null) stringResource(R.string.history_set_reps_missing)
                                            else stringResource(R.string.history_set_reps, set.reps.toString()))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
