package com.loggym.domain.model

/**
 * Definição reutilizável de um movimento (seção 19, conceito 1).
 * Não carrega metas de séries/repetições: isso pertence a [WorkoutExercise].
 */
data class Exercise(
    val id: Long,
    val name: String,
    val muscleGroup: String?,
    /** RF-12: distingue exercícios personalizados dos nativos. */
    val isCustom: Boolean,
)
