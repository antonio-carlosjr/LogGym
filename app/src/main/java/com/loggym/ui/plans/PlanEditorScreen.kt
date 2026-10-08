package com.loggym.ui.plans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loggym.domain.model.Exercise
import com.loggym.domain.model.Plan
import com.loggym.domain.model.Workout
import com.loggym.domain.model.WorkoutExercise
import com.loggym.ui.components.labels
import com.loggym.ui.exercises.filterExercises
import com.loggym.ui.util.formatRest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditorScreen(
    onBack: () -> Unit,
    viewModel: PlanEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()
    val library by viewModel.library.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state == PlanEditorUiState.NotFound) onBack()
    }
    val plan = (state as? PlanEditorUiState.Loaded)?.plan

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plan?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (plan != null) {
                        IconButton(onClick = viewModel::openRenamePlan) {
                            Icon(Icons.Filled.Edit, contentDescription = "Renomear plano")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (plan == null) {
            Box(Modifier.padding(padding))
            return@Scaffold
        }
        PlanContent(
            plan = plan,
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }

    when (val current = dialog) {
        is PlanEditorDialog.RenamePlan -> NameDialog(
            title = "Renomear plano",
            label = "Nome do plano",
            name = current.name,
            showBlankError = current.showBlankError,
            onNameChange = viewModel::onPlanNameChange,
            onConfirm = viewModel::saveRenamePlan,
            onDismiss = viewModel::dismissDialog,
        )
        is PlanEditorDialog.WorkoutName -> NameDialog(
            title = if (current.workoutId == null) "Nova divisão" else "Renomear divisão",
            label = "Nome da divisão (ex.: Push, Treino A)",
            name = current.name,
            showBlankError = current.showBlankError,
            onNameChange = viewModel::onWorkoutNameChange,
            onConfirm = viewModel::saveWorkoutName,
            onDismiss = viewModel::dismissDialog,
        )
        is PlanEditorDialog.ConfirmDeleteWorkout -> AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text("Excluir \"${current.workout.name}\"?") },
            text = { Text("A divisão e a configuração dos exercícios dela serão removidas. O histórico não muda.") },
            confirmButton = { TextButton(onClick = viewModel::confirmDeleteWorkout) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = viewModel::dismissDialog) { Text("Cancelar") } },
        )
        is PlanEditorDialog.PickExercise -> {
            val alreadyAdded = plan?.workouts?.firstOrNull { it.id == current.workoutId }
                ?.exercises?.map { it.exercise.id }?.toSet().orEmpty()
            PickExerciseDialog(
                query = current.query,
                exercises = filterExercises(library, current.query, group = null).filterNot { it.id in alreadyAdded },
                onQueryChange = viewModel::onPickQueryChange,
                onPick = viewModel::pickExercise,
                onDismiss = viewModel::dismissDialog,
            )
        }
        is PlanEditorDialog.Target -> TargetDialog(
            dialog = current,
            onChange = viewModel::updateTargetForm,
            onSave = viewModel::saveTarget,
            onDismiss = viewModel::dismissDialog,
        )
        null -> Unit
    }
}

@Composable
private fun PlanContent(plan: Plan, viewModel: PlanEditorViewModel, modifier: Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "status") {
            if (plan.isActive) {
                Text("Plano ativo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            } else {
                OutlinedButton(onClick = viewModel::activatePlan) { Text("Ativar este plano") }
            }
        }
        if (plan.workouts.isEmpty()) {
            item(key = "empty") {
                Text(
                    "Este plano ainda não tem divisões. Adicione a primeira, como \"Push\" ou \"Treino A\".",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        items(plan.workouts, key = { it.id }) { workout ->
            WorkoutCard(
                workout = workout,
                isFirst = workout.id == plan.workouts.first().id,
                isLast = workout.id == plan.workouts.last().id,
                viewModel = viewModel,
            )
        }
        item(key = "add-workout") {
            OutlinedButton(onClick = viewModel::openAddWorkout, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Adicionar divisão", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun WorkoutCard(workout: Workout, isFirst: Boolean, isLast: Boolean, viewModel: PlanEditorViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(workout.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.moveWorkout(workout, -1) }, enabled = !isFirst) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Mover ${workout.name} para cima")
            }
            IconButton(onClick = { viewModel.moveWorkout(workout, +1) }, enabled = !isLast) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Mover ${workout.name} para baixo")
            }
            IconButton(onClick = { viewModel.openRenameWorkout(workout) }) {
                Icon(Icons.Filled.Edit, contentDescription = "Renomear ${workout.name}")
            }
            IconButton(onClick = { viewModel.requestDeleteWorkout(workout) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Excluir ${workout.name}")
            }
        }
        HorizontalDivider()
        workout.exercises.forEachIndexed { index, item ->
            WorkoutExerciseRow(
                item = item,
                isFirst = index == 0,
                isLast = index == workout.exercises.lastIndex,
                onEdit = { viewModel.openEditTarget(workout, item) },
                onMoveUp = { viewModel.moveExercise(workout, item, -1) },
                onMoveDown = { viewModel.moveExercise(workout, item, +1) },
                onRemove = { viewModel.removeExercise(item) },
            )
        }
        TextButton(
            onClick = { viewModel.openPickExercise(workout) },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("Adicionar exercício", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun WorkoutExerciseRow(
    item: WorkoutExercise,
    isFirst: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val target = item.target
    ListItem(
        headlineContent = { Text(item.exercise.name) },
        supportingContent = {
            Text("${target.targetSets} × ${target.repMin}–${target.repMax} · ${formatRest(target.rest.seconds)}")
        },
        trailingContent = {
            Row {
                IconButton(onClick = onMoveUp, enabled = !isFirst) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Mover ${item.exercise.name} para cima")
                }
                IconButton(onClick = onMoveDown, enabled = !isLast) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Mover ${item.exercise.name} para baixo")
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remover ${item.exercise.name}")
                }
            }
        },
        modifier = Modifier.clickable(onClick = onEdit),
    )
}

@Composable
private fun NameDialog(
    title: String,
    label: String,
    name: String,
    showBlankError: Boolean,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(label) },
                singleLine = true,
                isError = showBlankError,
                supportingText = { if (showBlankError) Text("Informe um nome.") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun PickExerciseDialog(
    query: String,
    exercises: List<Exercise>,
    onQueryChange: (String) -> Unit,
    onPick: (Exercise) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar exercício") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Buscar exercício") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(exercises, key = { it.id }) { exercise ->
                        ListItem(
                            headlineContent = { Text(exercise.name) },
                            supportingContent = {
                                val groups = exercise.muscleGroups.labels()
                                if (groups.isNotEmpty()) Text(groups)
                            },
                            modifier = Modifier.clickable { onPick(exercise) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun TargetDialog(
    dialog: PlanEditorDialog.Target,
    onChange: ((TargetForm) -> TargetForm) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val form = dialog.form
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialog.exerciseName) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NumberField("Séries", form.sets) { value -> onChange { it.copy(sets = value) } }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Rep. mín.", form.repMin, Modifier.weight(1f)) { value -> onChange { it.copy(repMin = value) } }
                    NumberField("Rep. máx.", form.repMax, Modifier.weight(1f)) { value -> onChange { it.copy(repMax = value) } }
                }
                Text("Descanso", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onChange { it.adjustRest(-1) } }) { Text("−15 s") }
                    Text(
                        formatRest(form.restSeconds),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                    )
                    OutlinedButton(onClick = { onChange { it.adjustRest(+1) } }) { Text("+15 s") }
                }
                dialog.error?.let { error ->
                    Text(error.message(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                // RF-143: o padrão só entra por escolha do usuário.
                AssistChip(onClick = { onChange { it.withDefault() } }, label = { Text("Usar padrão (3 × 8–12, 1:30)") })
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(TargetForm.digitsOnly(it)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth(),
    )
}

private fun TargetFormError.message(): String = when (this) {
    TargetFormError.MISSING_SETS -> "Informe a quantidade de séries."
    TargetFormError.INVALID_SETS -> "As séries devem ser maiores que zero."
    TargetFormError.MISSING_REPS -> "Informe a faixa de repetições."
    TargetFormError.INVALID_REPS -> "As repetições devem ser maiores que zero."
    TargetFormError.MIN_GREATER_THAN_MAX -> "A repetição mínima não pode ser maior que a máxima."
}
