package com.loggym.ui.plans

import androidx.compose.runtime.Composable
import com.loggym.ui.components.PlaceholderScreen

@Composable
fun PlanEditorScreen(planId: Long, onBack: () -> Unit) {
    PlaceholderScreen(
        title = "Editar plano #$planId",
        requirements = listOf(
            "RF-25: renomear o plano",
            "RF-39 a RF-44: adicionar, renomear, excluir e reordenar divisões",
            "RF-45 a RF-48: adicionar, remover e reordenar exercícios da divisão",
            "RF-49 a RF-57: configurar séries, faixa de repetições e descanso por exercício",
        ),
        onBack = onBack,
    )
}
