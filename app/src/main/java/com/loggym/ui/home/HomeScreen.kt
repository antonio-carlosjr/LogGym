package com.loggym.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    onOpenExercises: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var draftToDiscard by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("LogGym") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.draft?.let { draft ->
                item(key = "draft") {
                    DraftCard(draft = draft, onResume = onOpenSession, onDiscard = { draftToDiscard = draft.id })
                }
            }

            val plan = state.activePlan
            if (!state.isLoading && plan == null) {
                item(key = "no-plan") { NoActivePlanCard(onOpenPlans = onOpenPlans) }
            }
            if (plan != null) {
                item(key = "plan-header") {
                    Column {
                        Text(plan.name, style = MaterialTheme.typography.titleLarge)
                        Text("Escolha a divisão de hoje", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                items(plan.workouts, key = { it.id }) { workout ->
                    WorkoutCard(
                        workout = workout,
                        canStart = state.draft == null,
                        onStart = { viewModel.startWorkout(workout.id, onOpenSession) },
                    )
                }
            }

            item(key = "navigation") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenPlans, modifier = Modifier.fillMaxWidth()) { Text("Planos") }
                    OutlinedButton(onClick = onOpenExercises, modifier = Modifier.fillMaxWidth()) { Text("Exercícios") }
                    OutlinedButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) { Text("Histórico") }
                }
            }
        }
    }

    draftToDiscard?.let { sessionId ->
        // RF-104: o descarte exige confirmação explícita.
        AlertDialog(
            onDismissRequest = { draftToDiscard = null },
            title = { Text("Descartar treino?") },
            text = { Text("O treino em andamento será apagado e não aparecerá no histórico.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.discardDraft(sessionId)
                    draftToDiscard = null
                }) { Text("Descartar") }
            },
            dismissButton = {
                TextButton(onClick = { draftToDiscard = null }) { Text("Cancelar") }
            },
        )
    }
}

/** RF-101 a RF-103: avisa sobre a sessão em andamento e permite retomar ou descartar. */
@Composable
private fun DraftCard(draft: Session, onResume: () -> Unit, onDiscard: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Treino em andamento", style = MaterialTheme.typography.titleMedium)
            Text("${draft.workoutName} · iniciado às ${draft.startedAt.formatTime()}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onResume) { Text("Retomar") }
                TextButton(onClick = onDiscard) { Text("Descartar") }
            }
        }
    }
}

/** RF-07/RF-34: sem plano ativo, direciona para seleção ou criação. */
@Composable
private fun NoActivePlanCard(onOpenPlans: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Nenhum plano ativo", style = MaterialTheme.typography.titleMedium)
            Text("Selecione ou crie um plano para começar a treinar.")
            Button(onClick = onOpenPlans) { Text("Ir para planos") }
        }
    }
}

@Composable
private fun WorkoutCard(workout: Workout, canStart: Boolean, onStart: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(workout.name, style = MaterialTheme.typography.titleMedium)
                Text("${workout.exercises.size} exercícios", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onStart, enabled = canStart && workout.exercises.isNotEmpty()) {
                Text("Iniciar")
            }
        }
    }
}
