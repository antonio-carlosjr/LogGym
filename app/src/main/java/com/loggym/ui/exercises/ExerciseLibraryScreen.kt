package com.loggym.ui.exercises

import androidx.compose.runtime.Composable
import com.loggym.ui.components.PlaceholderScreen

@Composable
fun ExerciseLibraryScreen(onBack: () -> Unit) {
    PlaceholderScreen(
        title = "Exercícios",
        requirements = listOf(
            "RF-09 a RF-12: listar nativos e personalizados, com distinção visual",
            "RF-13/RF-14: criar exercício personalizado com nome obrigatório",
            "RF-15/RF-18: editar apenas personalizados",
            "RF-16/RF-17/RF-19: excluir apenas personalizados, com confirmação",
            "RF-21: avisar quando o exercício é usado em planos antes de excluir",
        ),
        onBack = onBack,
    )
}
