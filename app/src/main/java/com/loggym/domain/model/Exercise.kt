package com.loggym.domain.model

/**
 * Definição reutilizável de um movimento (seção 19, conceito 1).
 * Não carrega metas de séries/repetições: isso pertence a [WorkoutExercise].
 */
data class Exercise(
    val id: Long,
    val name: String,
    val muscleGroups: Set<MuscleGroup>,
    val description: String?,
    /** RF-133: observação curta do usuário. */
    val userNote: String?,
    /** RF-12: distingue exercícios personalizados dos nativos. */
    val isCustom: Boolean,
)
