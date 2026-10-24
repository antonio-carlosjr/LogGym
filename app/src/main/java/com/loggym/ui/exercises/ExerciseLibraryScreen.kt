package com.loggym.ui.exercises

import com.loggym.ui.components.LoadError
import com.loggym.ui.components.OperationError
import com.loggym.R
import androidx.compose.ui.platform.LocalResources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
    viewModel: ExerciseLibraryViewModel = hiltViewModel(),
) {
    val resources = LocalResources.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val operationFailed by viewModel.operationFailed.collectAsStateWithLifecycle()
    val allExercises by viewModel.allExercises.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resources.getString(R.string.ui_exercicios)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = resources.getString(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::openCreate,
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
            if (state.failed) LoadError(viewModel::retry)

            if (!state.isLoading && !state.failed && state.exercises.isEmpty()) {
                Text(
                    resources.getString(R.string.ui_nenhum_exercicio_encontrado),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
        headlineContent = { Text(exercise.name) },
        supportingContent = {
            val groups = exercise.muscleGroups.labels()
            if (groups.isNotEmpty()) Text(groups)
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = { value -> onChange { it.copy(name = value) } },
                    label = { Text(resources.getString(R.string.ui_nome)) },
                    singleLine = true,
                    isError = editor.error != null,
                    supportingText = {
                        when (editor.error) {
                            EditorError.BLANK_NAME -> Text(resources.getString(R.string.ui_informe_um_nome))
                            EditorError.DUPLICATE_NAME -> Text(resources.getString(R.string.ui_ja_existe_um_exercicio_com_esse_nome))
                            EditorError.NOT_EDITABLE -> Text(resources.getString(R.string.ui_este_exercicio_nao_pode_ser_editado))
                            null -> Text(resources.getString(R.string.ui_ex_movimento_variacao_equipamento))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(resources.getString(R.string.ui_tipo_de_registro_fixo_apos_criar))
                TrackingType.entries.forEach { type ->
                    FilterChip(
                        selected = editor.trackingType == type,
                        enabled = editor.editingId == null,
                        onClick = { onChange { it.copy(trackingType = type) } },
                        label = { Text(if (type == TrackingType.TIME) resources.getString(R.string.ui_tempo_segundos) else resources.getString(R.string.ui_carga_e_repeticoes)) },
                    )
                }
                Text(resources.getString(R.string.ui_grupos_musculares_opcional), style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    MuscleGroup.entries.forEach { group ->
                        val selected = group in editor.muscleGroups
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onChange {
                                    it.copy(muscleGroups = if (selected) it.muscleGroups - group else it.muscleGroups + group)
                                }
                            },
                            label = { Text(group.label()) },
                        )
                    }
                }
                OutlinedTextField(
                    value = editor.description,
                    onValueChange = { value -> onChange { it.copy(description = value) } },
                    label = { Text(resources.getString(R.string.ui_descricao_ou_instrucao_opcional)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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
