package com.loggym.ui.session

import androidx.compose.ui.platform.LocalResources
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loggym.domain.logic.SetValues
import com.loggym.domain.model.*
import com.loggym.R
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(onBack: () -> Unit, viewModel: SessionViewModel = hiltViewModel()) {
    val resources = LocalResources.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) viewModel.setActive(true)
            if (event == Lifecycle.Event.ON_STOP) viewModel.setActive(false)
        }
        owner.lifecycle.addObserver(observer)
        viewModel.setActive(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        onDispose { owner.lifecycle.removeObserver(observer); viewModel.setActive(false) }
    }
    LaunchedEffect(state.exitReady) { if (state.exitReady) onBack() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(resources.getString(it)); viewModel.messageShown() }
    }
    BackHandler(enabled = !state.exitReady) { viewModel.back() }
    Scaffold(
        topBar = {
            Column {
            TopAppBar(title = { Text(state.session?.workoutName ?: resources.getString(R.string.ui_treino)) }, navigationIcon = {
                IconButton(onClick = viewModel::back, enabled = !state.busy) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = resources.getString(R.string.ui_voltar_e_salvar))
                }
            })
            state.remainingSeconds?.let { seconds ->
                RestBanner(seconds, !state.busy, viewModel::skipRest)
            }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (state.session != null) Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                    Text(when {
                        state.saving -> resources.getString(R.string.ui_salvando)
                        state.error != null -> resources.getString(R.string.ui_falha_ao_salvar)
                        state.dirty -> resources.getString(R.string.ui_alteracoes_ainda_nao_salvas)
                        else -> resources.getString(R.string.ui_alteracoes_salvas)
                    }, style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = viewModel::requestDiscard, enabled = !state.busy,
                            modifier = Modifier.weight(1f)) { Text(resources.getString(R.string.ui_descartar)) }
                        Button(onClick = viewModel::requestFinish, enabled = !state.busy,
                            modifier = Modifier.weight(1f)) { Text(resources.getString(R.string.ui_finalizar_treino)) }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.isLoading) item { CircularProgressIndicator() }
            state.error?.let { error -> item {
                Text(resources.getString(error), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = viewModel::retry, enabled = !state.busy) { Text(resources.getString(R.string.retry)) }
            } }
            val session = state.session
            if (!state.isLoading && session == null) item {
                Text(resources.getString(R.string.ui_nenhum_treino_em_andamento))
                TextButton(onClick = viewModel::back, enabled = !state.busy) { Text(resources.getString(R.string.ui_voltar_a_home)) }
            }
            if (session != null) {
                item {
                    Text(session.planName, style = MaterialTheme.typography.titleMedium)
                    Text(resources.getQuantityString(R.plurals.completed_sets, session.completedSetCount + session.pendingSetCount, session.completedSetCount, session.completedSetCount + session.pendingSetCount))
                }
                session.exercises.forEach { exercise ->
                    item(key = "exercise-${exercise.id}") {
                        Text(exercise.name, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                        Text(resources.getString(R.string.session_prescription, exercise.target.summary(), exercise.target.rest.seconds))
                        if (exercise.trackingType == TrackingType.WEIGHT_REPS)
                            Text(resources.getString(R.string.ui_carga_em_kg_registre_o_peso_de_um_halter_lado_conforme_o_equipamento),
                                style = MaterialTheme.typography.bodySmall)
                    }
                    items(exercise.sets, key = { "set-${it.id}" }) { set ->
                        val index = exercise.sets.indexOfFirst { it.id == set.id } + 1
                        val entry = state.entries[set.id] ?: SetEntry.from(set)
                        val completionLabel = stringResource(if (set.isCompleted) R.string.set_completed else R.string.set_pending)
                        Card(Modifier.fillMaxWidth().semantics { isTraversalGroup = true },
                            colors = CardDefaults.cardColors(containerColor = if (set.isCompleted)
                                MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(resources.getString(R.string.ui_serie_1_s, index), modifier = Modifier.weight(1f))
                                    Text(completionLabel, style = MaterialTheme.typography.labelMedium)
                                    Checkbox(checked = set.isCompleted, onCheckedChange = { viewModel.toggle(set.id) },
                                        enabled = !state.busy,
                                        modifier = Modifier.semantics {
                                            contentDescription = resources.getString(R.string.ui_concluir_serie_1_s_de_2_s, index, exercise.name)
                                            stateDescription = completionLabel
                                        })
                                }
                                val enabled = !set.isCompleted && !state.busy
                                if (exercise.trackingType == TrackingType.TIME) {
                                    EntryField(resources.getString(R.string.ui_duracao_s), entry.duration, enabled, entry.error != null, KeyboardType.Number) {
                                        value -> viewModel.edit(set.id) { it.copy(duration = value) }
                                    }
                                } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    EntryField(resources.getString(R.string.ui_carga_kg), entry.weight, enabled, entry.error != null, KeyboardType.Decimal,
                                        Modifier.weight(1f)) { value -> viewModel.edit(set.id) { it.copy(weight = value) } }
                                    EntryField(resources.getString(R.string.ui_repeticoes), entry.reps, enabled, entry.error != null, KeyboardType.Number,
                                        Modifier.weight(1f)) { value -> viewModel.edit(set.id) { it.copy(reps = value) } }
                                }
                                entry.error?.let { Text(resources.getString(it), color = MaterialTheme.colorScheme.error) }
                                if (set.isCompleted && SetValues(set.weightKg, set.reps, set.durationSeconds).isIncomplete(exercise.trackingType))
                                    Text(resources.getString(R.string.ui_serie_marcada_com_campos_incompletos), color = MaterialTheme.colorScheme.error)
                                if (!set.isCompleted) TextButton(onClick = { viewModel.remove(set.id) }, enabled = !state.busy) {
                                    Text(resources.getString(R.string.ui_remover_serie_1_s, index))
                                }
                            }
                        }
                    }
                    item(key = "add-${exercise.id}") {
                        TextButton(onClick = { viewModel.add(exercise.id) }, enabled = !state.busy) { Text(resources.getString(R.string.ui_adicionar_serie)) }
                    }
                }
            }
        }
    }
    state.dialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text(if (dialog == SessionDialog.DISCARD) resources.getString(R.string.ui_descartar_treino) else resources.getString(R.string.ui_finalizar_treino_2)) },
            text = {
                if (dialog == SessionDialog.DISCARD) Text(resources.getString(R.string.ui_o_treino_em_andamento_sera_apagado_e_nao_aparecera_no_historico))
                else Column {
                    val pending = state.session?.pendingSetCount ?: 0
                    if (pending > 0) Text(resources.getQuantityString(R.plurals.pending_sets, pending, pending))
                    if (state.incompleteCount > 0) Text(resources.getQuantityString(R.plurals.incomplete_sets, state.incompleteCount, state.incompleteCount))
                }
            },
            confirmButton = { TextButton(enabled = !state.busy, onClick = {
                if (dialog == SessionDialog.DISCARD) viewModel.confirmDiscard() else viewModel.confirmFinish()
            }) { Text(if (dialog == SessionDialog.DISCARD) resources.getString(R.string.ui_descartar) else resources.getString(R.string.ui_finalizar)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissDialog, enabled = !state.busy) { Text(resources.getString(R.string.ui_cancelar)) } },
        )
    }
}

@Composable
private fun RestBanner(seconds: Long, enabled: Boolean, onSkip: () -> Unit) {
    val resources = LocalResources.current
    val description = stringResource(R.string.rest_remaining, seconds / 60, seconds % 60)
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().testTag("rest-banner")) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {
                if (seconds > 0) contentDescription = description
            }) {
                if (seconds == 0L) Text(stringResource(R.string.rest_finished), style = MaterialTheme.typography.titleLarge)
                else {
                    Text(stringResource(R.string.rest_title), style = MaterialTheme.typography.labelMedium)
                    Text(String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60),
                        style = MaterialTheme.typography.headlineSmall)
                }
            }
            TextButton(onClick = onSkip, enabled = enabled,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                Text(stringResource(R.string.rest_skip))
            }
        }
    }
}

@Composable
private fun EntryField(label: String, value: String, enabled: Boolean, isError: Boolean,
    keyboardType: KeyboardType, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    val resources = LocalResources.current
    OutlinedTextField(value = value, onValueChange = onChange, enabled = enabled, isError = isError,
        label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = modifier.fillMaxWidth())
}
