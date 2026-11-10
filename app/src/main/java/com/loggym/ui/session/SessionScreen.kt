package com.loggym.ui.session

import androidx.compose.ui.platform.LocalResources
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import com.loggym.ui.components.CompactNumberField
import com.loggym.ui.util.formatRest
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.loggym.ui.history.ExerciseHistoryViewModel
import com.loggym.ui.history.ExerciseHistorySheet
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loggym.domain.model.*
import com.loggym.R
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(onBack: () -> Unit, viewModel: SessionViewModel = hiltViewModel(),
    historyViewModel: ExerciseHistoryViewModel = hiltViewModel()) {
    val resources = LocalResources.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val history by historyViewModel.uiState.collectAsStateWithLifecycle()
    var historyId by rememberSaveable { mutableStateOf<Long?>(null) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val selectedHistory = state.session?.exercises?.firstOrNull { it.id == historyId }
    val closeHistory = {
        viewModel.setHistoryOpen(false); historyViewModel.close(); historyId = null
    }
    LaunchedEffect(historyId, selectedHistory?.exerciseId, selectedHistory?.trackingType) {
        if (historyId != null && !state.isLoading && selectedHistory == null) { closeHistory(); return@LaunchedEffect }
        viewModel.setHistoryOpen(historyId != null)
        selectedHistory?.exerciseId?.let { historyViewModel.open(it, selectedHistory.trackingType) }
    }
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
    var sessionMenu by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            Column {
            TopAppBar(title = {
                Column {
                    Text(state.session?.workoutName ?: resources.getString(R.string.ui_treino),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    state.session?.let { session ->
                        val total = session.completedSetCount + session.pendingSetCount
                        Text("${session.planName} · " + resources.getString(R.string.session_progress, session.completedSetCount, total),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }, navigationIcon = {
                IconButton(onClick = viewModel::back, enabled = !state.busy) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = resources.getString(R.string.ui_voltar_e_salvar))
                }
            }, actions = {
                if (state.session != null) Box {
                    IconButton(onClick = { sessionMenu = true }, enabled = !state.busy) {
                        Icon(Icons.Filled.MoreVert, resources.getString(R.string.session_options))
                    }
                    DropdownMenu(expanded = sessionMenu, onDismissRequest = { sessionMenu = false }) {
                        DropdownMenuItem(text = { Text(resources.getString(R.string.discard_session_menu)) },
                            onClick = { sessionMenu = false; viewModel.requestDiscard() })
                    }
                }
            })
            state.session?.let { session ->
                val total = session.completedSetCount + session.pendingSetCount
                if (total > 0) LinearProgressIndicator(progress = { session.completedSetCount.toFloat() / total },
                    modifier = Modifier.fillMaxWidth().clearAndSetSemantics {}, drawStopIndicator = {})
            }
            state.remainingSeconds?.let { seconds ->
                RestBanner(seconds, !state.busy, viewModel::skipRest)
            }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (state.session != null) Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // Estado só aparece quando exige atenção; o salvamento normal é silencioso.
                    when {
                        state.error != null -> TextButton(onClick = viewModel::retry, enabled = !state.busy,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                            Text(resources.getString(R.string.session_save_retry))
                        }
                        state.saving -> Text(resources.getString(R.string.ui_salvando), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = viewModel::requestFinish, enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(resources.getString(R.string.ui_finalizar_treino)) }
                }
            }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),
            contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            if (state.isLoading) item { CircularProgressIndicator() }
            state.error?.let { error -> item {
                Text(resources.getString(error), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = viewModel::retry, enabled = !state.busy) { Text(resources.getString(R.string.retry)) }
            } }
            state.preferenceError?.let { error -> item {
                Text(stringResource(error), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = viewModel::retryPreference) { Text(stringResource(R.string.retry)) }
            } }
            val session = state.session
            if (!state.isLoading && session == null) item {
                Text(resources.getString(R.string.ui_nenhum_treino_em_andamento))
                TextButton(onClick = viewModel::back, enabled = !state.busy) { Text(resources.getString(R.string.ui_voltar_a_home)) }
            }
            if (session != null) {
                session.exercises.forEachIndexed { exerciseIndex, exercise ->
                    item(key = "exercise-${exercise.id}") {
                        var exerciseMenu by remember { mutableStateOf(false) }
                        Surface(Modifier.fillMaxWidth().padding(top = if (exerciseIndex == 0) 0.dp else 12.dp)
                            .testTag("exercise-header-${exercise.id}"),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)) {
                        Column(Modifier.padding(top = 8.dp)) {
                        Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(exercise.name, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
                                Text(resources.getString(R.string.session_prescription, exercise.target.summary(),
                                    formatRest(exercise.target.rest.seconds)),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (exercise.exerciseId != null) IconButton(onClick = {
                                viewModel.setHistoryOpen(true)
                                focus.clearFocus(); keyboard?.hide(); historyId = exercise.id
                            }, enabled = !state.busy) {
                                Icon(painterResource(R.drawable.ic_history), stringResource(R.string.exercise_history_open, exercise.name))
                            }
                            Box {
                                IconButton(onClick = { exerciseMenu = true }, enabled = !state.busy) {
                                    Icon(Icons.Filled.MoreVert, stringResource(R.string.exercise_options, exercise.name))
                                }
                                DropdownMenu(expanded = exerciseMenu, onDismissRequest = { exerciseMenu = false }) {
                                    DropdownMenuItem(text = { Text(stringResource(R.string.clear_exercise_menu)) },
                                        leadingIcon = { Icon(painterResource(R.drawable.ic_eraser), null) },
                                        onClick = { exerciseMenu = false; viewModel.requestClearExercise(exercise.id) })
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        SetTableHeader(exercise.trackingType == TrackingType.TIME)
                        }
                        }
                    }
                    val lastPendingId = exercise.sets.lastOrNull { !it.isCompleted }?.id
                    items(exercise.sets, key = { "set-${it.id}" }) { set ->
                        val index = exercise.sets.indexOfFirst { it.id == set.id } + 1
                        val entry = state.entries[set.id] ?: SetEntry.from(set)
                        val enabled = !set.isCompleted && !state.busy && set.id !in state.autoCompleting
                        val focused = Modifier.onFocusChanged { if (it.isFocused) viewModel.fieldFocused(set.id) }
                        val done = { viewModel.finishEditing(set.id); focus.clearFocus(); keyboard?.hide(); Unit }
                        val next = { focus.moveFocus(FocusDirection.Next); Unit }
                        // Último campo pendente do exercício usa Concluído; os demais seguem para a próxima série.
                        val isLast = set.id == lastPendingId
                        val lastAction = if (isLast) ImeAction.Done else ImeAction.Next
                        val lastHandler = if (isLast) done else next
                        SessionSetRow(exercise, set, index, entry, state.busy, set.id in state.autoCompleting,
                            onToggle = { viewModel.toggle(set.id) }, onRemove = { viewModel.remove(set.id) }) {
                            if (exercise.trackingType == TrackingType.TIME) {
                                CompactNumberField(entry.duration, { value -> viewModel.edit(set.id) { it.copy(duration = value) } },
                                    stringResource(R.string.set_duration_field, index), focused.testTag("set-${set.id}-duration"),
                                    enabled, entry.error != null, KeyboardType.Number, lastAction, lastHandler)
                            } else {
                                CompactNumberField(entry.weight, { value -> viewModel.edit(set.id) { it.copy(weight = value) } },
                                    stringResource(R.string.set_weight_field, index), focused.testTag("set-${set.id}-weight"),
                                    enabled, entry.error != null, KeyboardType.Decimal, ImeAction.Next, next)
                                CompactNumberField(entry.reps, { value -> viewModel.edit(set.id) { it.copy(reps = value) } },
                                    stringResource(R.string.set_reps_field, index), focused.testTag("set-${set.id}-reps"),
                                    enabled, entry.error != null, KeyboardType.Number, lastAction, lastHandler)
                            }
                        }
                    }
                    item(key = "add-${exercise.id}") {
                        Surface(Modifier.fillMaxWidth().testTag("exercise-footer-${exercise.id}"),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)) {
                            Row(Modifier.padding(horizontal = 4.dp)) {
                                TextButton(onClick = { viewModel.add(exercise.id) }, enabled = !state.busy) {
                                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(resources.getString(R.string.ui_adicionar_serie))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (historyId != null && selectedHistory != null) {
        key(selectedHistory.id) { ExerciseHistorySheet(selectedHistory.name, history, closeHistory, historyViewModel::more, historyViewModel::retry) }
    }
    state.dialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text(when (dialog) {
                SessionDialog.CLEAR_EXERCISE -> resources.getString(R.string.clear_exercise_named,
                    state.session?.exercises?.firstOrNull { it.id == state.clearExerciseId }?.name.orEmpty())
                SessionDialog.DISCARD -> resources.getString(R.string.ui_descartar_treino)
                SessionDialog.FINISH -> resources.getString(R.string.ui_finalizar_treino_2)
            }) },
            text = {
                if (dialog == SessionDialog.CLEAR_EXERCISE) Column {
                    Text(stringResource(R.string.clear_exercise_help))
                    state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                }
                else if (dialog == SessionDialog.DISCARD) Text(resources.getString(R.string.ui_o_treino_em_andamento_sera_apagado_e_nao_aparecera_no_historico))
                else Column {
                    val pending = state.session?.pendingSetCount ?: 0
                    if (pending > 0) Text(resources.getQuantityString(R.plurals.pending_sets, pending, pending))
                    if (state.incompleteCount > 0) Text(resources.getQuantityString(R.plurals.incomplete_sets, state.incompleteCount, state.incompleteCount))
                }
            },
            confirmButton = { TextButton(enabled = !state.busy, onClick = {
                when (dialog) {
                    SessionDialog.CLEAR_EXERCISE -> viewModel.confirmClearExercise()
                    SessionDialog.DISCARD -> viewModel.confirmDiscard()
                    SessionDialog.FINISH -> viewModel.confirmFinish()
                }
            }) { Text(resources.getString(when (dialog) {
                SessionDialog.CLEAR_EXERCISE -> R.string.clear_action
                SessionDialog.DISCARD -> R.string.ui_descartar
                SessionDialog.FINISH -> R.string.ui_finalizar
            })) } },
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
