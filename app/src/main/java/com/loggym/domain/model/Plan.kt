package com.loggym.domain.model

/** Prescrição futura (seção 1): plano → divisões → exercícios configurados. */
data class Plan(
    val id: Long,
    val name: String,
    val isActive: Boolean,
    val workouts: List<Workout>,
)

data class PlanSummary(
    val id: Long,
    val name: String,
    val isActive: Boolean,
)

/** Divisão/treino executável dentro de um plano (seção 5). */
data class Workout(
    val id: Long,
    val planId: Long,
    val name: String,
    val position: Int,
    val exercises: List<WorkoutExercise>,
)

/** Prescrição de um exercício em uma divisão específica (seção 19, conceito 2). */
data class WorkoutExercise(
    val id: Long,
    val exercise: Exercise,
    val position: Int,
    val target: ExerciseTarget,
)
