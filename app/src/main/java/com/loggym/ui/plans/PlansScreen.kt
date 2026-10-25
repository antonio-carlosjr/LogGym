package com.loggym.ui.plans

import com.loggym.ui.navigation.RootNavigationLock

import com.loggym.ui.components.LoadError
import com.loggym.ui.components.OperationError
import com.loggym.R
import androidx.compose.ui.platform.LocalResources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.loggym.ui.theme.LogGymSpacing
import com.loggym.ui.components.LoadingState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loggym.domain.model.PlanSummary
import com.loggym.domain.model.PlanTemplate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(
    onBack: () -> Unit,
    showBack: Boolean = true,
    onOpenPlan: (Long) -> Unit,
    viewModel: PlansViewModel = hiltViewModel(),
) {
    val resources = LocalResources.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val operationFailed by viewModel.operationFailed.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()

    val busy by viewModel.busy.collectAsStateWithLifecycle()
    RootNavigationLock(busy || dialog != null || operationFailed)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resources.getString(R.string.ui_planos)) },
                navigationIcon = {
                    if (showBack) IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = resources.getString(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::openCreatePlan,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(resources.getString(R.string.ui_novo_plano)) },
                // O FAB estendido limpa a semântica do rótulo; sem isto o leitor de tela não anuncia a ação.
                modifier = Modifier.semantics { contentDescription = resources.getString(R.string.ui_novo_plano) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(LogGymSpacing.small),
        ) {
            if (operationFailed) item { OperationError(viewModel::dismissError) }
            if (state.isLoading) item { com.loggym.ui.components.LoadingState() }
            if (state.failed) item { LoadError(viewModel::retry) }
            item(key = "templates") {
                OutlinedButton(onClick = viewModel::openTemplates, modifier = Modifier.fillMaxWidth()) {
                    Text(resources.getString(R.string.ui_usar_um_plano_pronto))
                }
            }
            if (!state.isLoading && !state.failed && state.plans.isEmpty()) {
                item(key = "empty") {
                    Text(
                        resources.getString(R.string.ui_voce_ainda_nao_tem_planos_crie_um_do_zero_ou_use_um_plano_pronto),
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
            title = { Text(resources.getString(R.string.ui_plano_criado)) },
            text = { Text(resources.getString(R.string.ui_ativar_agora_o_plano_ativo_e_o_usado_para_iniciar_treinos)) },
            confirmButton = { TextButton(onClick = { viewModel.activate(current.planId) }) { Text(resources.getString(R.string.ui_ativar)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissDialog) { Text(resources.getString(R.string.ui_agora_nao)) } },
        )
        is PlansDialog.ConfirmDelete -> AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text(resources.getString(R.string.ui_excluir_1_s, current.plan.name)) },
            text = {
                val activeWarning = if (current.plan.isActive) {
                    resources.getString(R.string.ui_este_e_o_plano_ativo_voce_ficara_sem_plano_ativo_ate_escolher_outro)
                } else {
                    ""
                }
                Text(resources.getString(R.string.ui_1_sos_treinos_ja_registrados_no_historico_nao_mudam, activeWarning))
            },
            confirmButton = { TextButton(onClick = viewModel::confirmDelete) { Text(resources.getString(R.string.ui_excluir)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissDialog) { Text(resources.getString(R.string.ui_cancelar)) } },
        )
        null -> Unit
    }
}

/** RF-30: o plano ativo é destacado. */
@Composable
private fun PlanCard(plan: PlanSummary, onOpen: () -> Unit, onActivate: () -> Unit, onDelete: () -> Unit) {
    val resources = LocalResources.current
    var menuOpen by remember { mutableStateOf(false) }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(plan.name) },
            supportingContent = { if (plan.isActive) Text(resources.getString(R.string.ui_plano_ativo)) },
            colors = if (plan.isActive) {
                ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            } else {
                ListItemDefaults.colors()
            },
            trailingContent = {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = resources.getString(R.string.ui_acoes_do_plano_1_s, plan.name))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (!plan.isActive) {
                        DropdownMenuItem(
                            text = { Text(resources.getString(R.string.ui_ativar)) },
                            onClick = {
                                menuOpen = false
                                onActivate()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(resources.getString(R.string.ui_excluir)) },
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
    val resources = LocalResources.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(resources.getString(R.string.ui_novo_plano)) },
        text = {
            OutlinedTextField(
                value = dialog.name,
                onValueChange = onNameChange,
                label = { Text(resources.getString(R.string.ui_nome_do_plano)) },
                singleLine = true,
                isError = dialog.showBlankError,
                supportingText = { if (dialog.showBlankError) Text(resources.getString(R.string.ui_informe_um_nome)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(resources.getString(R.string.ui_criar)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(resources.getString(R.string.ui_cancelar)) } },
    )
}

@Composable
private fun ChooseTemplateDialog(
    templates: List<PlanTemplate>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val resources = LocalResources.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(resources.getString(R.string.ui_planos_prontos)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(LogGymSpacing.small),
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
        dismissButton = { TextButton(onClick = onDismiss) { Text(resources.getString(R.string.ui_cancelar)) } },
    )
}
