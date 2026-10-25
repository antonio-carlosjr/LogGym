package com.loggym.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.loggym.R
import com.loggym.domain.model.summary
import androidx.compose.ui.text.style.TextOverflow
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseHistorySheet(name: String, state: ExerciseHistoryUiState, onClose: () -> Unit,
    onMore: () -> Unit, onRetry: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(stringResource(R.string.exercise_history_title, name), Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, stringResource(R.string.exercise_history_close)) }
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.entries.isEmpty() && !state.loading && !state.failed) item { Text(stringResource(R.string.exercise_history_empty)) }
            state.entries.forEachIndexed { sessionIndex, entry ->
                if (sessionIndex > 0) item(key = "divider-${entry.session.id}") {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                item(key = "session-${entry.session.id}") {
                    Text(DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault())
                        .format(entry.session.startedAt), style = MaterialTheme.typography.titleMedium)
                    val context = listOf(entry.session.planName, entry.session.workoutName)
                        .map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" | ")
                    if (context.isNotEmpty()) Text(context, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                entry.occurrences.forEachIndexed { index, occurrence ->
                    item(key = "occurrence-${occurrence.id}") {
                        Text(occurrence.name, style = MaterialTheme.typography.titleMedium)
                        if (entry.occurrences.size > 1) Text(stringResource(R.string.exercise_history_occurrence, index + 1))
                        Text(stringResource(R.string.session_prescription, occurrence.target.summary(), occurrence.target.rest.seconds))
                    }
                    occurrence.sets.forEachIndexed { number, set ->
                        item(key = "set-${set.id}") { Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                HistorySetContent(number + 1, set, occurrence.trackingType)
                            }
                        } }
                    }
                }
            }
            if (state.loading) item { CircularProgressIndicator() }
            if (state.failed) item { HistoryError(onRetry) }
            else if (!state.loading && state.nextCursor != null) item {
                TextButton(onClick = onMore) { Text(stringResource(R.string.exercise_history_more)) }
            }
        }
    }
}
