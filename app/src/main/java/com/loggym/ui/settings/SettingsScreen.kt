package com.loggym.ui.settings

import com.loggym.ui.navigation.RootNavigationLock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.loggym.ui.theme.LogGymSpacing
import com.loggym.ui.components.LoadingState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loggym.R
import com.loggym.domain.model.ThemeMode
import com.loggym.ui.components.LoadError
import com.loggym.ui.components.OperationError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, showBack: Boolean = true, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val failed by viewModel.operationFailed.collectAsStateWithLifecycle()
    val backup by viewModel.backupState.collectAsStateWithLifecycle()
    var backupLocked by remember { mutableStateOf(false) }
    val blocked = busy || backup.busy || backup.step in setOf(com.loggym.domain.backup.BackupStep.PREVIEW, com.loggym.domain.backup.BackupStep.DESTINATION)
    RootNavigationLock(blocked || backupLocked || failed)
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }, navigationIcon = {
        if (showBack) IconButton(onClick = onBack, enabled = !blocked) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(LogGymSpacing.medium)) {
            if (failed) OperationError(viewModel::dismissError)
            if (state.failed) LoadError(viewModel::retry)
            else if (state.settings == null) LoadingState()
            state.settings?.let { settings ->
                com.loggym.ui.components.SectionHeading(stringResource(R.string.settings_appearance))
                Column(Modifier.selectableGroup()) {
                    ThemeMode.entries.forEach { mode ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .selectable(settings.themeMode == mode, enabled = !blocked, role = Role.RadioButton,
                                onClick = { viewModel.setTheme(mode) }).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = settings.themeMode == mode, onClick = null, enabled = !blocked)
                            Text(stringResource(when (mode) {
                                ThemeMode.SYSTEM -> R.string.theme_system
                                ThemeMode.LIGHT -> R.string.theme_light
                                ThemeMode.DARK -> R.string.theme_dark
                            }), Modifier.padding(start = 12.dp))
                        }
                    }
                }
                HorizontalDivider()
                com.loggym.ui.components.SectionHeading(stringResource(R.string.settings_training))
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(settings.prefillEnabled,
                    enabled = !blocked, role = Role.Switch, onValueChange = viewModel::setPrefill), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.prefill_label), Modifier.weight(1f))
                    Switch(checked = settings.prefillEnabled, onCheckedChange = null, enabled = !blocked)
                }
                Text(stringResource(R.string.prefill_help), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(settings.autoCompleteEnabled,
                    enabled = !blocked, role = Role.Switch, onValueChange = viewModel::setAutoComplete), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.auto_complete_label), Modifier.weight(1f))
                    Switch(checked = settings.autoCompleteEnabled, onCheckedChange = null, enabled = !blocked)
                }
                Text(stringResource(R.string.auto_complete_help), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                BackupSection(backup, !busy, viewModel::export, viewModel::writeExport, viewModel::importBackup,
                    viewModel::confirmImport, viewModel::cancelBackup, onNavigationLock = { backupLocked = it })
            }
        }
    }
}
