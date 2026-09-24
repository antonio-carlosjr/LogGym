package com.loggym.domain.model

/**
 * Modelo de plano pré-definido do aplicativo (seção 4.2).
 * Nunca é persistido diretamente: selecioná-lo cria uma cópia local editável (RF-37, RF-38).
 */
data class PlanTemplate(
    val key: String,
    val name: String,
    val description: String,
    val workouts: List<WorkoutTemplate>,
)

data class WorkoutTemplate(
    val name: String,
    val exercises: List<ExerciseTemplate>,
)

/** Referencia exercícios nativos pela chave estável, não pelo id do banco. */
data class ExerciseTemplate(
    val nativeKey: String,
    val target: ExerciseTarget,
)
