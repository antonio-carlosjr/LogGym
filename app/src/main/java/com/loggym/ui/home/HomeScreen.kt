package com.loggym.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import com.loggym.ui.navigation.RootNavigationLock

import com.loggym.ui.components.LoadError
import com.loggym.R
import androidx.compose.ui.platform.LocalResources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loggym.ui.theme.LogGymSpacing
import com.loggym.ui.components.LoadingState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loggym.domain.model.Session
import com.loggym.domain.model.Workout
import com.loggym.ui.util.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSession: () -> Unit,
    onOpenPlans: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val resources = LocalResources.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var draftToDiscard by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(resources.getString(message))
            viewModel.onMessageShown()
        }
    }

    RootNavigationLock(state.isBusy || draftToDiscard != null)
    Scaffold(
        topBar = { TopAppBar(title = { com.loggym.ui.components.LogGymHomeTitle() }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(LogGymSpacing.screen),
            verticalArrangement = Arrangement.spacedBy(LogGymSpacing.medium),
        ) {
            if (state.isLoading) item { com.loggym.ui.components.LoadingState() }
            if (state.failed) item { LoadError(viewModel::retry) }
            state.draft?.let { draft ->
                item(key = "draft") {
                    DraftCard(draft = draft, enabled = !state.isBusy, onResume = onOpenSession, onDiscard = { draftToDiscard = draft.id })
                }
            }

            val plan = state.activePlan
            if (!state.isLoading && !state.failed && plan == null) {
                item(key = "no-plan") { NoActivePlanCard(onOpenPlans = onOpenPlans) }
            }
            if (plan != null) {
                item(key = "plan-header") {
                    Column {
                        Text(plan.name, style = MaterialTheme.typography.titleLarge)
                        Text(resources.getString(R.string.ui_escolha_a_divisao_de_hoje), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(plan.workouts, key = { it.id }) { workout ->
                    WorkoutCard(
                        workout = workout,
                        canStart = state.draft == null && !state.isBusy,
                        onStart = { viewModel.startWorkout(workout.id, onOpenSession) },
                    )
                }
            }


        }
    }

    draftToDiscard?.let { sessionId ->
        // RF-104: o descarte exige confirmação explícita.
        AlertDialog(
            onDismissRequest = { draftToDiscard = null },
            title = { Text(resources.getString(R.string.ui_descartar_treino)) },
            text = { Text(resources.getString(R.string.ui_o_treino_em_andamento_sera_apagado_e_nao_aparecera_no_historico)) },
            confirmButton = {
                TextButton(enabled = !state.isBusy, onClick = {
                    viewModel.discardDraft(sessionId)
                    draftToDiscard = null
                }) { Text(resources.getString(R.string.ui_descartar)) }
            },
            dismissButton = {
                TextButton(onClick = { draftToDiscard = null }) { Text(resources.getString(R.string.ui_cancelar)) }
            },
        )
    }
}

/** RF-101 a RF-103: avisa sobre a sessão em andamento e permite retomar ou descartar. */
@Composable
private fun DraftCard(draft: Session, enabled: Boolean, onResume: () -> Unit, onDiscard: () -> Unit) {
    val resources = LocalResources.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(LogGymSpacing.screen), verticalArrangement = Arrangement.spacedBy(LogGymSpacing.small)) {
            Text(resources.getString(R.string.ui_treino_em_andamento), style = MaterialTheme.typography.titleMedium)
            Text(resources.getString(R.string.ui_1_s_iniciado_as_2_s, draft.workoutName, draft.startedAt.formatTime()),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(LogGymSpacing.small), verticalArrangement = Arrangement.spacedBy(LogGymSpacing.extraSmall)) {
                Button(onClick = onResume, enabled = enabled) { Text(resources.getString(R.string.ui_retomar)) }
                TextButton(onClick = onDiscard, enabled = enabled,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Text(resources.getString(R.string.ui_descartar))
                }
            }
        }
    }
}

/** RF-07/RF-34: sem plano ativo, direciona para seleção ou criação. */
@Composable
private fun NoActivePlanCard(onOpenPlans: () -> Unit) {
    val resources = LocalResources.current
    Card(modifier = Modifier.fillMaxWidth(), colors = com.loggym.ui.components.LogGymCardDefaults.colors()) {
        Column(Modifier.padding(LogGymSpacing.screen), verticalArrangement = Arrangement.spacedBy(LogGymSpacing.small)) {
            Text(resources.getString(R.string.ui_nenhum_plano_ativo), style = MaterialTheme.typography.titleMedium)
            Text(resources.getString(R.string.ui_selecione_ou_crie_um_plano_para_comecar_a_treinar))
            Button(onClick = onOpenPlans) { Text(resources.getString(R.string.ui_ir_para_planos)) }
        }
    }
}

@Composable
private fun WorkoutCard(workout: Workout, canStart: Boolean, onStart: () -> Unit) {
    val resources = LocalResources.current
    Card(modifier = Modifier.fillMaxWidth(), colors = com.loggym.ui.components.LogGymCardDefaults.colors()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(workout.name, style = MaterialTheme.typography.titleMedium)
                Text(resources.getQuantityString(R.plurals.exercise_count, workout.exercises.size, workout.exercises.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onStart, enabled = canStart && workout.exercises.isNotEmpty()) {
                Text(resources.getString(R.string.ui_iniciar))
            }
        }
    }
}
