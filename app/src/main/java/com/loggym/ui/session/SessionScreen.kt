package com.loggym.ui.session

import androidx.compose.runtime.Composable
import com.loggym.ui.components.PlaceholderScreen

@Composable
fun SessionScreen(onBack: () -> Unit) {
    PlaceholderScreen(
        title = "Treino",
        requirements = listOf(
            "RF-73 a RF-78: editar carga e repetições pré-preenchidas",
            "RF-79 a RF-82: marcar e desmarcar séries",
            "RF-83 a RF-85: adicionar e remover séries apenas nesta sessão",
            "RF-87 a RF-95: cronômetro de descanso baseado em timestamp",
            "RF-107 a RF-111: finalizar com confirmação se houver séries pendentes",
        ),
        onBack = onBack,
    )
}
