package com.loggym.ui.history

import androidx.compose.runtime.Composable
import com.loggym.ui.components.PlaceholderScreen

@Composable
fun HistoryDetailScreen(sessionId: Long, onBack: () -> Unit) {
    PlaceholderScreen(
        title = "Sessão #$sessionId",
        requirements = listOf(
            "RF-121/RF-122: exercícios e séries realizadas com carga e repetições",
            "RF-123 a RF-127: exibir os snapshots da época da sessão",
            "RF-128: somente leitura",
        ),
        onBack = onBack,
    )
}
