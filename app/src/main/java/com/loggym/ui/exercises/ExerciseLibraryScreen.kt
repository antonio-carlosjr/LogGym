package com.loggym.ui.exercises

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import com.loggym.ui.navigation.RootNavigationLock

import com.loggym.ui.components.LoadError
import com.loggym.ui.components.OperationError
import com.loggym.R
import androidx.compose.ui.platform.LocalResources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.loggym.ui.theme.LogGymSpacing
import com.loggym.ui.components.LoadingState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loggym.domain.model.TrackingType
import com.loggym.domain.model.Exercise
import com.loggym.domain.model.InputRules
import com.loggym.domain.model.MuscleGroup
import com.loggym.ui.components.label
import com.loggym.ui.components.labels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(
    onBack: () -> Unit,
    onCreateExercise: () -> Unit,
    showBack: Boolean = true,
    viewModel: ExerciseLibraryViewModel = hiltViewModel(),
) {
    val resources = LocalResources.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val operationFailed by viewModel.operationFailed.collectAsStateWithLifecycle()
    val allExercises by viewModel.allExercises.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()

    val busy by viewModel.busy.collectAsStateWithLifecycle()
    RootNavigationLock(busy || dialog != null || operationFailed)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resources.getString(R.string.ui_exercicios)) },
                navigationIcon = {
                    if (showBack) IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = resources.getString(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateExercise,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(resources.getString(R.string.ui_novo_exercicio)) },
                // O FAB estendido limpa a semântica do rótulo; sem isto o leitor de tela não anuncia a ação.
                modifier = Modifier.semantics { contentDescription = resources.getString(R.string.ui_novo_exercicio) },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text(resources.getString(R.string.ui_buscar_exercicio)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            MuscleGroupFilterRow(selected = state.groupFilter, onSelect = viewModel::onGroupFilterChange)
            HorizontalDivider()
            if (operationFailed) OperationError(viewModel::dismissError)
            if (state.isLoading) com.loggym.ui.components.LoadingState(Modifier.padding(horizontal = LogGymSpacing.screen))
            if (state.failed) androidx.compose.foundation.layout.Box(Modifier.padding(horizontal = LogGymSpacing.screen)) { LoadError(viewModel::retry) }

            if (!state.isLoading && !state.failed && state.exercises.isEmpty()) {
                com.loggym.ui.components.EmptyState(resources.getString(R.string.ui_nenhum_exercicio_encontrado),
                    Modifier.padding(horizontal = LogGymSpacing.screen))
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                items(state.exercises, key = { it.id }) { exercise ->
                    ExerciseRow(exercise = exercise, onClick = { viewModel.openDetails(exercise) })
                }
            }
        }
    }

    when (val current = dialog) {
        is ExerciseDialog.Details -> allExercises.firstOrNull { it.id == current.exerciseId }?.let { exercise ->
            ExerciseDetailsDialog(
                exercise = exercise,
                onSaveNote = { note -> viewModel.saveNote(exercise.id, note) },
                onEdit = { viewModel.openEdit(exercise) },
                onDelete = { viewModel.requestDelete(exercise) },
                onDismiss = viewModel::dismissDialog,
            )
        }
        is ExerciseDialog.Editor -> ExerciseEditorDialog(
            editor = current,
            onChange = viewModel::updateEditor,
            onSave = viewModel::saveEditor,
            onDismiss = viewModel::dismissDialog,
        )
        is ExerciseDialog.ConfirmDelete -> ConfirmDeleteDialog(
            confirm = current,
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::dismissDialog,
        )
        null -> Unit
    }
}

@Composable
private fun MuscleGroupFilterRow(selected: MuscleGroup?, onSelect: (MuscleGroup?) -> Unit) {
    val resources = LocalResources.current
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(LogGymSpacing.small),
    ) {
        item {
            FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text(resources.getString(R.string.ui_todos)) })
        }
        items(MuscleGroup.entries) { group ->
            FilterChip(
                selected = selected == group,
                onClick = { onSelect(if (selected == group) null else group) },
                label = { Text(group.label()) },
            )
        }
    }
}

/** RF-12: indica se o exercício é personalizado. */
@Composable
private fun ExerciseRow(exercise: Exercise, onClick: () -> Unit) {
    val resources = LocalResources.current
    ListItem(
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        headlineContent = { Text(exercise.name) },
        supportingContent = {
            val groups = exercise.muscleGroups.labels()
            if (groups.isNotEmpty()) Text(groups, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingContent = {
            Text(
                if (exercise.isCustom) resources.getString(R.string.ui_personalizado) else resources.getString(R.string.ui_nativo),
                style = MaterialTheme.typography.labelSmall,
                color = if (exercise.isCustom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun ExerciseDetailsDialog(
    exercise: Exercise,
    onSaveNote: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val resources = LocalResources.current
    var note by remember(exercise.id, exercise.userNote) { mutableStateOf(exercise.userNote.orEmpty()) }
    val noteChanged = note.trim() != exercise.userNote.orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exercise.name) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(LogGymSpacing.medium),
            ) {
                Text(
                    if (exercise.isCustom) resources.getString(R.string.ui_exercicio_personalizado) else resources.getString(R.string.ui_exercicio_nativo),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                val groups = exercise.muscleGroups.labels()
                if (groups.isNotEmpty()) Text(groups, style = MaterialTheme.typography.bodyMedium)
                Text(if (exercise.trackingType == TrackingType.TIME) resources.getString(R.string.ui_tempo_segundos) else resources.getString(R.string.ui_carga_e_repeticoes))
                exercise.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                // RF-133: observação do usuário, inclusive em nativos.
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(InputRules.USER_NOTE_MAX_LENGTH) },
                    label = { Text(resources.getString(R.string.ui_minha_observacao)) },
                    supportingText = { Text("${note.length}/${InputRules.USER_NOTE_MAX_LENGTH}") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (noteChanged) {
                    TextButton(onClick = { onSaveNote(note) }) { Text(resources.getString(R.string.ui_salvar_observacao)) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(resources.getString(R.string.ui_fechar)) } },
        dismissButton = {
            // RF-18/RF-19: nativos não podem ser editados nem excluídos.
            if (exercise.isCustom) {
                Row {
                    TextButton(onClick = onDelete) { Text(resources.getString(R.string.ui_excluir)) }
                    TextButton(onClick = onEdit) { Text(resources.getString(R.string.ui_editar)) }
                }
            }
        },
    )
}

@Composable
private fun ExerciseEditorDialog(
    editor: ExerciseDialog.Editor,
    onChange: ((ExerciseDialog.Editor) -> ExerciseDialog.Editor) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val resources = LocalResources.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editor.editingId == null) resources.getString(R.string.ui_novo_exercicio) else resources.getString(R.string.ui_editar_exercicio)) },
        text = {
            ExerciseEditorFields(editor, onChange)
        },
        confirmButton = { TextButton(onClick = onSave) { Text(resources.getString(R.string.ui_salvar)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(resources.getString(R.string.ui_cancelar)) } },
    )
}

/** RF-17 e RF-21: confirmação, com aviso quando o exercício está em planos. */
@Composable
private fun ConfirmDeleteDialog(
    confirm: ExerciseDialog.ConfirmDelete,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val resources = LocalResources.current
    val usage = if (confirm.planUsages == 0) "" else resources.getQuantityString(
        R.plurals.plan_usages, confirm.planUsages, confirm.planUsages)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(resources.getString(R.string.ui_excluir_1_s, confirm.exercise.name)) },
        text = { Text(resources.getString(R.string.ui_1_sos_treinos_ja_registrados_no_historico_nao_mudam, usage)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(resources.getString(R.string.ui_excluir)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(resources.getString(R.string.ui_cancelar)) } },
    )
}
