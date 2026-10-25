package com.loggym.ui.settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import com.loggym.R
import com.loggym.domain.backup.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun BackupSection(state: BackupState, enabled: Boolean, export: (BackupMode) -> Unit,
    write: (String, String?) -> Unit, import: (String) -> Unit, confirm: (String) -> Unit, cancel: () -> Unit) {
    var chooseMode by rememberSaveable { mutableStateOf(false) }
    var destinationToken by rememberSaveable { mutableStateOf<String?>(null) }
    var importPicker by rememberSaveable { mutableStateOf(false) }
    val output = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        destinationToken?.let { write(it, uri?.toString()) }; destinationToken = null
    }
    val input = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        importPicker = false; uri?.let { import(it.toString()) }
    }
    LaunchedEffect(state.step, state.token) {
        if (state.step == BackupStep.DESTINATION && state.token != null && destinationToken != state.token) {
            destinationToken = state.token
            output.launch(requireNotNull(state.filename))
        }
    }
    val locked = state.busy || state.step in setOf(BackupStep.DESTINATION, BackupStep.PREVIEW) || importPicker
    BackHandler(locked) { if (!state.busy && !importPicker && state.step != BackupStep.DESTINATION) cancel() }
    HorizontalDivider()
    Text(stringResource(R.string.backup_title), Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.backup_help))
    Text(stringResource(R.string.backup_plaintext), color = MaterialTheme.colorScheme.onSurfaceVariant)
    Button(onClick = { chooseMode = true }, enabled = enabled && !locked) { Text(stringResource(R.string.backup_export)) }
    OutlinedButton(onClick = { importPicker = true; input.launch(arrayOf("*/*")) }, enabled = enabled && !locked) { Text(stringResource(R.string.backup_import)) }
    val feedback = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
    if (state.busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(stringResource(R.string.backup_working), feedback) }
    if (state.step == BackupStep.SUCCESS) Text(stringResource(R.string.backup_success), feedback)
    if (state.step == BackupStep.ERROR) Text(stringResource(if (state.error == BackupFailure.ROLLED_BACK)
        R.string.backup_rolled_back else R.string.backup_failed), feedback, color = MaterialTheme.colorScheme.error)
    if (chooseMode) AlertDialog(onDismissRequest = { chooseMode = false },
        title = { Text(stringResource(R.string.backup_export)) },
        text = { Text(stringResource(R.string.backup_help)) },
        confirmButton = { TextButton(onClick = { chooseMode = false; export(BackupMode.FULL) }) { Text(stringResource(R.string.backup_full)) } },
        dismissButton = { TextButton(onClick = { chooseMode = false; export(BackupMode.WITHOUT_HISTORY) }) { Text(stringResource(R.string.backup_without_history)) } })
    state.summary?.takeIf { state.step == BackupStep.PREVIEW }?.let { summary ->
        AlertDialog(onDismissRequest = cancel,
            title = { Text(stringResource(R.string.backup_restore_confirm)) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(if (summary.mode == BackupMode.FULL) R.string.backup_full else R.string.backup_without_history))
                Text(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(summary.createdAt)))
                Text(stringResource(R.string.backup_summary, summary.customExercises, summary.plans, summary.workouts, summary.sessions, summary.sets))
                Text(stringResource(if (summary.hasDraft) R.string.backup_has_draft else R.string.backup_no_draft))
                Text(stringResource(R.string.backup_replace_warning))
                if (summary.mode == BackupMode.WITHOUT_HISTORY) Text(stringResource(R.string.backup_remove_sessions))
            } },
            confirmButton = { TextButton(onClick = { confirm(summary.token) }) { Text(stringResource(R.string.backup_replace)) } },
            dismissButton = { TextButton(onClick = cancel) { Text(stringResource(R.string.ui_cancelar)) } })
    }
}
