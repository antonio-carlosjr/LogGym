package com.loggym.ui.history

import androidx.compose.runtime.Composable
import com.loggym.ui.components.PlaceholderScreen

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    PlaceholderScreen(
        title = "Histórico",
        requirements = listOf(
            "RF-116 a RF-118: listar apenas sessões finalizadas",
            "RF-119: identificar sessões por data e treino realizado",
            "RF-120: abrir os detalhes de uma sessão",
        ),
        onBack = onBack,
    )
}
