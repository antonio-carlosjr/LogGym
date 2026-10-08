package com.loggym.ui.exercises

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
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val allExercises by viewModel.allExercises.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercícios") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::openCreate,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Novo exercício") },
                // O FAB estendido limpa a semântica do rótulo; sem isto o leitor de tela não anuncia a ação.
                modifier = Modifier.semantics { contentDescription = "Novo exercício" },
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
                placeholder = { Text("Buscar exercício") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            MuscleGroupFilterRow(selected = state.groupFilter, onSelect = viewModel::onGroupFilterChange)
            HorizontalDivider()

            if (!state.isLoading && state.exercises.isEmpty()) {
                Text(
                    "Nenhum exercício encontrado.",
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
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("Todos") })
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
    ListItem(
        headlineContent = { Text(exercise.name) },
        supportingContent = {
            val groups = exercise.muscleGroups.labels()
            if (groups.isNotEmpty()) Text(groups)
        },
        trailingContent = {
            Text(
                if (exercise.isCustom) "Personalizado" else "Nativo",
                style = MaterialTheme.typography.labelSmall,
                color = if (exercise.isCustom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
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
                    if (exercise.isCustom) "Exercício personalizado" else "Exercício nativo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                val groups = exercise.muscleGroups.labels()
                if (groups.isNotEmpty()) Text(groups, style = MaterialTheme.typography.bodyMedium)
                exercise.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                // RF-133: observação do usuário, inclusive em nativos.
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(InputRules.USER_NOTE_MAX_LENGTH) },
                    label = { Text("Minha observação") },
                    supportingText = { Text("${note.length}/${InputRules.USER_NOTE_MAX_LENGTH}") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (noteChanged) {
                    TextButton(onClick = { onSaveNote(note) }) { Text("Salvar observação") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
        dismissButton = {
            // RF-18/RF-19: nativos não podem ser editados nem excluídos.
            if (exercise.isCustom) {
                Row {
                    TextButton(onClick = onDelete) { Text("Excluir") }
                    TextButton(onClick = onEdit) { Text("Editar") }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editor.editingId == null) "Novo exercício" else "Editar exercício") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = { value -> onChange { it.copy(name = value) } },
                    label = { Text("Nome") },
                    singleLine = true,
                    isError = editor.error != null,
                    supportingText = {
                        when (editor.error) {
                            EditorError.BLANK_NAME -> Text("Informe um nome.")
                            EditorError.DUPLICATE_NAME -> Text("Já existe um exercício com esse nome.")
                            EditorError.NOT_EDITABLE -> Text("Este exercício não pode ser editado.")
                            null -> Text("Ex.: Movimento + variação + equipamento")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Grupos musculares (opcional)", style = MaterialTheme.typography.labelLarge)
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
                    label = { Text("Descrição ou instrução (opcional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** RF-17 e RF-21: confirmação, com aviso quando o exercício está em planos. */
@Composable
private fun ConfirmDeleteDialog(
    confirm: ExerciseDialog.ConfirmDelete,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val usage = when (confirm.planUsages) {
        0 -> ""
        1 -> "Ele está em 1 divisão de plano e será removido dela. "
        else -> "Ele está em ${confirm.planUsages} divisões de planos e será removido delas. "
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir \"${confirm.exercise.name}\"?") },
        text = { Text("${usage}Os treinos já registrados no histórico não mudam.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Excluir") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
