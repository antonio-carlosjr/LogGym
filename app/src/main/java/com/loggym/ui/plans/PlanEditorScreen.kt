package com.loggym.ui.plans

import com.loggym.ui.components.LoadError
import com.loggym.ui.components.OperationError
import com.loggym.R
import androidx.compose.ui.platform.LocalResources
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
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loggym.domain.model.summary
import com.loggym.domain.model.TrackingType
import com.loggym.domain.model.Exercise
import com.loggym.domain.model.Plan
import com.loggym.domain.model.Workout
import com.loggym.domain.model.WorkoutExercise
import com.loggym.ui.components.labels
import com.loggym.ui.exercises.filterExercises
import com.loggym.ui.util.restLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditorScreen(
    onBack: () -> Unit,
    viewModel: PlanEditorViewModel = hiltViewModel(),
) {
    val resources = LocalResources.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val operationFailed by viewModel.operationFailed.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()
    val libraryFailed by viewModel.libraryFailed.collectAsStateWithLifecycle()
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = resources.getString(R.string.back))
                    }
                },
                actions = {
                    if (plan != null) {
                        IconButton(onClick = viewModel::openRenamePlan) {
                            Icon(Icons.Filled.Edit, contentDescription = resources.getString(R.string.ui_renomear_plano))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (plan == null) {
            Box(Modifier.padding(padding)) {
                if (state == PlanEditorUiState.Failed) LoadError(viewModel::retry)
            }
            return@Scaffold
        }
        Column(Modifier.padding(padding)) {
            if (operationFailed) OperationError(viewModel::dismissError)
            PlanContent(plan = plan, viewModel = viewModel, modifier = Modifier.fillMaxSize())
        }
    }

    when (val current = dialog) {
        is PlanEditorDialog.RenamePlan -> NameDialog(
            title = resources.getString(R.string.ui_renomear_plano),
            label = resources.getString(R.string.ui_nome_do_plano),
            name = current.name,
            showBlankError = current.showBlankError,
            onNameChange = viewModel::onPlanNameChange,
            onConfirm = viewModel::saveRenamePlan,
            onDismiss = viewModel::dismissDialog,
        )
        is PlanEditorDialog.WorkoutName -> NameDialog(
            title = if (current.workoutId == null) resources.getString(R.string.ui_nova_divisao) else resources.getString(R.string.ui_renomear_divisao),
            label = resources.getString(R.string.ui_nome_da_divisao_ex_push_treino_a),
            name = current.name,
            showBlankError = current.showBlankError,
            onNameChange = viewModel::onWorkoutNameChange,
            onConfirm = viewModel::saveWorkoutName,
            onDismiss = viewModel::dismissDialog,
        )
        is PlanEditorDialog.ConfirmDeleteWorkout -> AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text(resources.getString(R.string.ui_excluir_1_s, current.workout.name)) },
            text = { Text(resources.getString(R.string.ui_a_divisao_e_a_configuracao_dos_exercicios_dela_serao_removidas_o_historico_)) },
            confirmButton = { TextButton(onClick = viewModel::confirmDeleteWorkout) { Text(resources.getString(R.string.ui_excluir)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissDialog) { Text(resources.getString(R.string.ui_cancelar)) } },
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
                failed = libraryFailed,
                onRetry = viewModel::retry,
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
    val resources = LocalResources.current
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "status") {
            if (plan.isActive) {
                Text(resources.getString(R.string.ui_plano_ativo), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            } else {
                OutlinedButton(onClick = viewModel::activatePlan) { Text(resources.getString(R.string.ui_ativar_este_plano)) }
            }
        }
        if (plan.workouts.isEmpty()) {
            item(key = "empty") {
                Text(
                    resources.getString(R.string.ui_este_plano_ainda_nao_tem_divisoes_adicione_a_primeira_como_push_ou_treino_a),
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
                Text(resources.getString(R.string.ui_adicionar_divisao), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun WorkoutCard(workout: Workout, isFirst: Boolean, isLast: Boolean, viewModel: PlanEditorViewModel) {
    val resources = LocalResources.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(workout.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.moveWorkout(workout, -1) }, enabled = !isFirst) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = resources.getString(R.string.ui_mover_1_s_para_cima, workout.name))
            }
            IconButton(onClick = { viewModel.moveWorkout(workout, +1) }, enabled = !isLast) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = resources.getString(R.string.ui_mover_1_s_para_baixo, workout.name))
            }
            IconButton(onClick = { viewModel.openRenameWorkout(workout) }) {
                Icon(Icons.Filled.Edit, contentDescription = resources.getString(R.string.ui_renomear_1_s, workout.name))
            }
            IconButton(onClick = { viewModel.requestDeleteWorkout(workout) }) {
                Icon(Icons.Filled.Delete, contentDescription = resources.getString(R.string.ui_excluir_1_s_2, workout.name))
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
            Text(resources.getString(R.string.ui_adicionar_exercicio), modifier = Modifier.padding(start = 8.dp))
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
    val resources = LocalResources.current
    val target = item.target
    ListItem(
        headlineContent = { Text(item.exercise.name) },
        supportingContent = {
            Text("${target.summary()} · ${restLabel(target.rest.seconds)}")
        },
        trailingContent = {
            Row {
                IconButton(onClick = onMoveUp, enabled = !isFirst) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = resources.getString(R.string.ui_mover_1_s_para_cima, item.exercise.name))
                }
                IconButton(onClick = onMoveDown, enabled = !isLast) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = resources.getString(R.string.ui_mover_1_s_para_baixo, item.exercise.name))
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = resources.getString(R.string.ui_remover_1_s, item.exercise.name))
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
    val resources = LocalResources.current
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
                supportingText = { if (showBlankError) Text(resources.getString(R.string.ui_informe_um_nome)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(resources.getString(R.string.ui_salvar)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(resources.getString(R.string.ui_cancelar)) } },
    )
}

@Composable
private fun PickExerciseDialog(
    query: String,
    exercises: List<Exercise>,
    onQueryChange: (String) -> Unit,
    onPick: (Exercise) -> Unit,
    onDismiss: () -> Unit,
    failed: Boolean,
    onRetry: () -> Unit,
) {
    val resources = LocalResources.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(resources.getString(R.string.ui_adicionar_exercicio)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text(resources.getString(R.string.ui_buscar_exercicio)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (failed) LoadError(onRetry)
                else if (exercises.isEmpty()) Text(resources.getString(R.string.ui_nenhum_exercicio_encontrado))
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(exercises, key = { it.id }) { exercise ->
                        ListItem(
                            headlineContent = { Text(exercise.name) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
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
        dismissButton = { TextButton(onClick = onDismiss) { Text(resources.getString(R.string.ui_cancelar)) } },
    )
}

@Composable
private fun TargetDialog(
    dialog: PlanEditorDialog.Target,
    onChange: ((TargetForm) -> TargetForm) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val resources = LocalResources.current
    val form = dialog.form
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialog.exerciseName) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NumberField(resources.getString(R.string.ui_series), form.sets) { value -> onChange { it.copy(sets = value) } }
                if (form.trackingType == TrackingType.TIME) {
                    NumberField(resources.getString(R.string.ui_duracao_por_serie_s), form.durationSeconds, maxDigits = 6) { value -> onChange { it.copy(durationSeconds = value) } }
                } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(resources.getString(R.string.ui_rep_min), form.repMin, Modifier.weight(1f)) { value -> onChange { it.copy(repMin = value) } }
                    NumberField(resources.getString(R.string.ui_rep_max), form.repMax, Modifier.weight(1f)) { value -> onChange { it.copy(repMax = value) } }
                }
                Text(resources.getString(R.string.rest_title), style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onChange { it.adjustRest(-1) } }, enabled = form.restValue != null) { Text(resources.getString(R.string.ui_15_s)) }
                    OutlinedTextField(
                        value = form.restSeconds,
                        onValueChange = { value -> onChange { it.copy(restSeconds = value) } },
                        label = { Text(resources.getString(R.string.rest_seconds)) },
                        singleLine = true,
                        isError = form.restValue == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                    )
                    OutlinedButton(onClick = { onChange { it.adjustRest(+1) } }, enabled = form.restValue != null) { Text(resources.getString(R.string.ui_15_s_2)) }
                }
                dialog.error?.let { error ->
                    Text(error.message(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                // RF-143: o padrão só entra por escolha do usuário.
                AssistChip(onClick = { onChange { it.withDefault() } }, label = { Text(if (form.trackingType == TrackingType.TIME) resources.getString(R.string.ui_usar_padrao_3_30_s_1_30) else resources.getString(R.string.ui_usar_padrao_3_812_1_30)) })
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text(resources.getString(R.string.ui_salvar)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(resources.getString(R.string.ui_cancelar)) } },
    )
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier = Modifier, maxDigits: Int = TargetForm.MAX_DIGITS, onChange: (String) -> Unit) {
    val resources = LocalResources.current
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(maxDigits)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun TargetFormError.message(): String {
    val resources = LocalResources.current
    return when (this) {
    TargetFormError.INVALID_REST -> resources.getString(R.string.invalid_rest)
    TargetFormError.INVALID_DURATION -> resources.getString(R.string.ui_informe_uma_duracao_em_segundos_maior_que_zero)
    TargetFormError.MISSING_SETS -> resources.getString(R.string.ui_informe_a_quantidade_de_series)
    TargetFormError.INVALID_SETS -> resources.getString(R.string.ui_as_series_devem_ser_maiores_que_zero)
    TargetFormError.MISSING_REPS -> resources.getString(R.string.ui_informe_a_faixa_de_repeticoes)
    TargetFormError.INVALID_REPS -> resources.getString(R.string.ui_as_repeticoes_devem_ser_maiores_que_zero)
    TargetFormError.MIN_GREATER_THAN_MAX -> resources.getString(R.string.ui_a_repeticao_minima_nao_pode_ser_maior_que_a_maxima)
}

}
