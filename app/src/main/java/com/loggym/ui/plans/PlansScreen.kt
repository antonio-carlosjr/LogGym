package com.loggym.ui.plans

import androidx.compose.runtime.Composable
import com.loggym.ui.components.PlaceholderScreen

@Composable
fun PlansScreen(onBack: () -> Unit) {
    PlaceholderScreen(
        title = "Planos",
        requirements = listOf(
            "RF-22/RF-30: listar planos indicando o ativo",
            "RF-23/RF-24: criar plano personalizado com nome",
            "RF-26/RF-27: excluir plano com confirmação",
            "RF-28/RF-29/RF-31: ativar plano desativando o anterior",
            "RF-35/RF-37: criar plano a partir de template",
        ),
        onBack = onBack,
    )
}
