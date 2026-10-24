package com.loggym.ui.onboarding

import androidx.compose.ui.res.stringResource
import com.loggym.R
import androidx.compose.ui.platform.LocalResources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OnboardingScreen(
    onPlanSelected: () -> Unit,
    onCreateCustomPlan: () -> Unit,
    onSkip: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val resources = LocalResources.current
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    val isWorking by viewModel.isWorking.collectAsStateWithLifecycle()

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (failed) item { Text(stringResource(R.string.operation_failed), color = MaterialTheme.colorScheme.error) }
            item {
                Text(resources.getString(R.string.ui_bem_vindo_ao_loggym), style = MaterialTheme.typography.headlineMedium)
                Text(
                    resources.getString(R.string.ui_escolha_um_plano_pronto_para_comecar_voce_podera_personaliza_lo_depois),
                    style = MaterialTheme.typography.bodyLarge,
                )
                // RF-147: aviso de saúde.
                Text(
                    resources.getString(R.string.ui_o_loggym_nao_substitui_a_orientacao_de_um_profissional_de_educacao_fisica_o),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(viewModel.templates, key = { it.key }) { template ->
                ElevatedCard(
                    onClick = { viewModel.selectTemplate(template.key, onPlanSelected) },
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(template.name, style = MaterialTheme.typography.titleMedium)
                        Text(template.description, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            template.workouts.joinToString(" · ") { it.name },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { viewModel.complete(onCreateCustomPlan) },
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(resources.getString(R.string.ui_criar_plano_personalizado))
                }
                TextButton(
                    onClick = { viewModel.complete(onSkip) },
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(resources.getString(R.string.ui_pular_por_enquanto))
                }
            }
        }
    }
}
