package com.loggym.ui.onboarding

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
    val isWorking by viewModel.isWorking.collectAsStateWithLifecycle()

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Bem-vindo ao LogGym", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Escolha um plano pronto para começar. Você poderá personalizá-lo depois.",
                    style = MaterialTheme.typography.bodyLarge,
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
                    Text("Criar plano personalizado")
                }
                TextButton(
                    onClick = { viewModel.complete(onSkip) },
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Pular por enquanto")
                }
            }
        }
    }
}
