package com.loggym.ui.session

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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(onBack: () -> Unit, viewModel: SessionViewModel = hiltViewModel()) {
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
        state.message?.let { snackbar.showSnackbar(it); viewModel.messageShown() }
    }
    BackHandler(enabled = !state.exitReady) { viewModel.back() }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(state.session?.workoutName ?: "Treino") }, navigationIcon = {
                IconButton(onClick = viewModel::back, enabled = !state.busy) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar e salvar")
                }
            })
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (state.session != null) Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                    state.remainingSeconds?.let { seconds ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (seconds == 0L) "Descanso concluído" else
                                "Descanso: " + String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60),
                                modifier = Modifier.weight(1f))
                            TextButton(onClick = viewModel::skipRest, enabled = !state.busy) { Text("Pular") }
                        }
                    }
                    Text(when {
                        state.saving -> "Salvando…"
                        state.error != null -> "Falha ao salvar"
                        state.dirty -> "Alterações ainda não salvas"
                        else -> "Alterações salvas"
                    }, style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = viewModel::requestDiscard, enabled = !state.busy,
                            modifier = Modifier.weight(1f)) { Text("Descartar") }
                        Button(onClick = viewModel::requestFinish, enabled = !state.busy,
                            modifier = Modifier.weight(1f)) { Text("Finalizar treino") }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.isLoading) item { CircularProgressIndicator() }
            state.error?.let { error -> item {
                Text(error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = viewModel::retry, enabled = !state.busy) { Text("Tentar novamente") }
            } }
            val session = state.session
            if (!state.isLoading && session == null) item {
                Text("Nenhum treino em andamento")
                TextButton(onClick = viewModel::back, enabled = !state.busy) { Text("Voltar à home") }
            }
            if (session != null) {
                item {
                    Text(session.planName, style = MaterialTheme.typography.titleMedium)
                    Text("${session.completedSetCount} de ${session.completedSetCount + session.pendingSetCount} séries concluídas")
                }
                session.exercises.forEach { exercise ->
                    item(key = "exercise-${exercise.id}") {
                        Text(exercise.name, style = MaterialTheme.typography.titleLarge)
                        Text(exercise.target.summary() + " · descanso ${exercise.target.rest.seconds} s")
                        if (exercise.trackingType == TrackingType.WEIGHT_REPS)
                            Text("Carga em kg: registre o peso de um halter/lado, conforme o equipamento.",
                                style = MaterialTheme.typography.bodySmall)
                    }
                    items(exercise.sets, key = { "set-${it.id}" }) { set ->
                        val index = exercise.sets.indexOfFirst { it.id == set.id } + 1
                        val entry = state.entries[set.id] ?: SetEntry.from(set)
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Série $index", modifier = Modifier.weight(1f))
                                    Checkbox(checked = set.isCompleted, onCheckedChange = { viewModel.toggle(set.id) },
                                        enabled = !state.busy,
                                        modifier = Modifier.semantics { contentDescription = "Concluir série $index de ${exercise.name}" })
                                }
                                val enabled = !set.isCompleted && !state.busy
                                if (exercise.trackingType == TrackingType.TIME) {
                                    EntryField("Duração (s)", entry.duration, enabled, entry.error != null, KeyboardType.Number) {
                                        value -> viewModel.edit(set.id) { it.copy(duration = value) }
                                    }
                                } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    EntryField("Carga (kg)", entry.weight, enabled, entry.error != null, KeyboardType.Decimal,
                                        Modifier.weight(1f)) { value -> viewModel.edit(set.id) { it.copy(weight = value) } }
                                    EntryField("Repetições", entry.reps, enabled, entry.error != null, KeyboardType.Number,
                                        Modifier.weight(1f)) { value -> viewModel.edit(set.id) { it.copy(reps = value) } }
                                }
                                entry.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                                if (set.isCompleted && SetValues(set.weightKg, set.reps, set.durationSeconds).isIncomplete(exercise.trackingType))
                                    Text("Série marcada com campos incompletos.", color = MaterialTheme.colorScheme.error)
                                if (!set.isCompleted) TextButton(onClick = { viewModel.remove(set.id) }, enabled = !state.busy) {
                                    Text("Remover série $index")
                                }
                            }
                        }
                    }
                    item(key = "add-${exercise.id}") {
                        TextButton(onClick = { viewModel.add(exercise.id) }, enabled = !state.busy) { Text("Adicionar série") }
                    }
                }
            }
        }
    }
    state.dialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text(if (dialog == SessionDialog.DISCARD) "Descartar treino?" else "Finalizar treino?") },
            text = {
                if (dialog == SessionDialog.DISCARD) Text("O treino em andamento será apagado e não aparecerá no histórico.")
                else Column {
                    val pending = state.session?.pendingSetCount ?: 0
                    if (pending > 0) Text("$pending séries não concluídas serão excluídas.")
                    if (state.incompleteCount > 0) Text("${state.incompleteCount} séries serão salvas com campos vazios.")
                }
            },
            confirmButton = { TextButton(enabled = !state.busy, onClick = {
                if (dialog == SessionDialog.DISCARD) viewModel.confirmDiscard() else viewModel.confirmFinish()
            }) { Text(if (dialog == SessionDialog.DISCARD) "Descartar" else "Finalizar") } },
            dismissButton = { TextButton(onClick = viewModel::dismissDialog, enabled = !state.busy) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun EntryField(label: String, value: String, enabled: Boolean, isError: Boolean,
    keyboardType: KeyboardType, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, enabled = enabled, isError = isError,
        label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth())
}
