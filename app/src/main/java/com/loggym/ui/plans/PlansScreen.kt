package com.loggym.ui.plans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import com.loggym.domain.model.PlanSummary
import com.loggym.domain.model.PlanTemplate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(
    onBack: () -> Unit,
    onOpenPlan: (Long) -> Unit,
    viewModel: PlansViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Planos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::openCreatePlan,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Novo plano") },
                // O FAB estendido limpa a semântica do rótulo; sem isto o leitor de tela não anuncia a ação.
                modifier = Modifier.semantics { contentDescription = "Novo plano" },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "templates") {
                OutlinedButton(onClick = viewModel::openTemplates, modifier = Modifier.fillMaxWidth()) {
                    Text("Usar um plano pronto")
                }
            }
            if (!state.isLoading && state.plans.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "Você ainda não tem planos. Crie um do zero ou use um plano pronto.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
            items(state.plans, key = { it.id }) { plan ->
                PlanCard(
                    plan = plan,
                    onOpen = { onOpenPlan(plan.id) },
                    onActivate = { viewModel.activate(plan.id) },
                    onDelete = { viewModel.requestDelete(plan) },
                )
            }
        }
    }

    when (val current = dialog) {
        is PlansDialog.CreatePlan -> CreatePlanDialog(
            dialog = current,
            onNameChange = viewModel::onPlanNameChange,
            onConfirm = { viewModel.createPlan(onCreated = onOpenPlan) },
            onDismiss = viewModel::dismissDialog,
        )
        PlansDialog.ChooseTemplate -> ChooseTemplateDialog(
            templates = viewModel.templates,
            onSelect = viewModel::createFromTemplate,
            onDismiss = viewModel::dismissDialog,
        )
        is PlansDialog.AskActivate -> AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text("Plano criado") },
            text = { Text("Ativar agora? O plano ativo é o usado para iniciar treinos.") },
            confirmButton = { TextButton(onClick = { viewModel.activate(current.planId) }) { Text("Ativar") } },
            dismissButton = { TextButton(onClick = viewModel::dismissDialog) { Text("Agora não") } },
        )
        is PlansDialog.ConfirmDelete -> AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text("Excluir \"${current.plan.name}\"?") },
            text = {
                val activeWarning = if (current.plan.isActive) {
                    "Este é o plano ativo: você ficará sem plano ativo até escolher outro. "
                } else {
                    ""
                }
                Text("${activeWarning}Os treinos já registrados no histórico não mudam.")
            },
            confirmButton = { TextButton(onClick = viewModel::confirmDelete) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = viewModel::dismissDialog) { Text("Cancelar") } },
        )
        null -> Unit
    }
}

/** RF-30: o plano ativo é destacado. */
@Composable
private fun PlanCard(plan: PlanSummary, onOpen: () -> Unit, onActivate: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(plan.name) },
            supportingContent = { if (plan.isActive) Text("Plano ativo") },
            colors = if (plan.isActive) {
                ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            } else {
                ListItemDefaults.colors()
            },
            trailingContent = {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Ações do plano ${plan.name}")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (!plan.isActive) {
                        DropdownMenuItem(
                            text = { Text("Ativar") },
                            onClick = {
                                menuOpen = false
                                onActivate()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Excluir") },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            },
            modifier = Modifier.clickable(onClick = onOpen),
        )
    }
}

@Composable
private fun CreatePlanDialog(
    dialog: PlansDialog.CreatePlan,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo plano") },
        text = {
            OutlinedTextField(
                value = dialog.name,
                onValueChange = onNameChange,
                label = { Text("Nome do plano") },
                singleLine = true,
                isError = dialog.showBlankError,
                supportingText = { if (dialog.showBlankError) Text("Informe um nome.") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Criar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun ChooseTemplateDialog(
    templates: List<PlanTemplate>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Planos prontos") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                templates.forEach { template ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(template.key) }
                            .padding(vertical = 8.dp),
                    ) {
                        Text(template.name, style = MaterialTheme.typography.titleMedium)
                        Text(template.description, style = MaterialTheme.typography.bodySmall)
                        Text(
                            template.workouts.joinToString(" · ") { it.name },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
